package no.nav.sosialhjelp.innsyn.client.fiks

import com.fasterxml.jackson.module.kotlin.readValue
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedleggSpesifikasjon
import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.api.fiks.exceptions.FiksClientException
import no.nav.sosialhjelp.api.fiks.exceptions.FiksServerException
import no.nav.sosialhjelp.innsyn.config.ClientProperties
import no.nav.sosialhjelp.innsyn.redis.RedisService
import no.nav.sosialhjelp.innsyn.responses.ok_digisossak_response
import no.nav.sosialhjelp.innsyn.responses.ok_minimal_jsondigisossoker_response
import no.nav.sosialhjelp.innsyn.service.pdf.EttersendelsePdfGenerator
import no.nav.sosialhjelp.innsyn.service.vedlegg.FilForOpplasting
import no.nav.sosialhjelp.innsyn.service.vedlegg.KrypteringService
import no.nav.sosialhjelp.innsyn.utils.objectMapper
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import org.springframework.web.reactive.function.client.toEntity
import java.io.InputStream

internal class FiksClientTest {

    private val mockWebServer = MockWebServer()

    private val clientProperties: ClientProperties = mockk(relaxed = true)
    private val fiksWebClient = WebClient.create(mockWebServer.url("/").toString())
    private val redisService: RedisService = mockk()
    private val retryProperties: FiksRetryProperties = mockk()
    private val ettersendelsePdfGenerator: EttersendelsePdfGenerator = mockk()
    private val krypteringService: KrypteringService = mockk()
    private val fiksClient = FiksClientImpl(clientProperties, fiksWebClient, retryProperties, redisService)

    private val id = "123"

    @BeforeEach
    fun init() {
        clearAllMocks()
        mockWebServer.start()

        every { redisService.get(any(), any()) } returns null
        every { redisService.put(any(), any(), any()) } just Runs
        every { redisService.defaultTimeToLiveSeconds } returns 1

        every { retryProperties.attempts } returns 2
        every { retryProperties.initialDelay } returns 5
        every { retryProperties.maxDelay } returns 10
    }

    @AfterEach
    internal fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `GET eksakt 1 DigisosSak`() {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(ok_digisossak_response)
        )

        val result = fiksClient.hentDigisosSak(id, "Token", false)

        assertThat(result).isNotNull
    }

    @Test
    fun `GET digisosSak fra cache`() {
        val digisosSak = objectMapper.readValue<DigisosSak>(ok_digisossak_response)
        every { redisService.get(id, DigisosSak::class.java) } returns digisosSak

        val result2 = fiksClient.hentDigisosSak(id, "Token", true)

        assertThat(result2).isNotNull

        verify(exactly = 0) { redisService.put(any(), any(), any()) }
    }

    @Test
    fun `GET digisosSak fra cache etter put`() {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(ok_digisossak_response)
        )

        val result1 = fiksClient.hentDigisosSak(id, "Token", true)

        assertThat(result1).isNotNull
        verify(exactly = 1) { redisService.put(any(), any(), any()) }
        verify(exactly = 1) { redisService.get(any(), DigisosSak::class.java) }

        val digisosSak: DigisosSak = objectMapper.readValue(ok_digisossak_response)
        every { redisService.get(id, DigisosSak::class.java) } returns digisosSak

        val result = fiksClient.hentDigisosSak(id, "Token", true)

        assertThat(result).isNotNull

        verify(exactly = 1) { redisService.put(any(), any(), any()) }
        verify(exactly = 2) { redisService.get(any(), any()) }
    }

    @Test
    fun `GET DigisosSak feiler hvis Fiks gir 500`() {
        every { retryProperties.attempts } returns 1

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(500)
        )

        assertThatExceptionOfType(FiksServerException::class.java)
            .isThrownBy { fiksClient.hentDigisosSak(id, "Token", true) }
    }

    @Test
    fun `GET alle DigisosSaker skal bruke retry hvis Fiks gir 5xx-feil`() {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(500)
        )
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(500)
        )

        assertThatExceptionOfType(FiksServerException::class.java).isThrownBy { fiksClient.hentAlleDigisosSaker("Token") }
        assertThat(mockWebServer.requestCount).isEqualTo(2)
    }

    @Test
    fun `GET alle DigisosSaker skal ikke bruke retry hvis Fiks gir 4xx-feil`() {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(400)
        )

        assertThatExceptionOfType(FiksClientException::class.java).isThrownBy { fiksClient.hentAlleDigisosSaker("Token") }

        assertThat(mockWebServer.requestCount).isEqualTo(1)
    }

    @Test
    fun `GET alle DigisosSaker`() {
        val digisosSakOk = objectMapper.readValue(ok_digisossak_response, DigisosSak::class.java)

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(objectMapper.writeValueAsString(listOf(digisosSakOk, digisosSakOk)))
        )

        val result = fiksClient.hentAlleDigisosSaker("Token")

        assertThat(result).isNotNull
        assertThat(result).hasSize(2)
    }

    @Test
    fun `GET dokument`() {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(ok_minimal_jsondigisossoker_response)
        )

        val result = fiksClient.hentDokument(id, "dokumentlagerId", JsonDigisosSoker::class.java, "Token")

        assertThat(result).isNotNull
    }

    @Test
    fun `GET dokument fra cache`() {
        val jsonDigisosSoker = objectMapper.readValue<JsonDigisosSoker>(ok_minimal_jsondigisossoker_response)
        every { redisService.get(any(), JsonDigisosSoker::class.java) } returns jsonDigisosSoker

        val result2 = fiksClient.hentDokument(id, "dokumentlagerId", JsonDigisosSoker::class.java, "Token")

        assertThat(result2).isNotNull

        verify(exactly = 0) { redisService.put(any(), any(), any()) }
    }

    @Test
    fun `GET dokument fra cache etter put`() {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(ok_minimal_jsondigisossoker_response)
        )

        val result1 = fiksClient.hentDokument(id, "dokumentlagerId", JsonDigisosSoker::class.java, "Token")

        assertThat(result1).isNotNull
        verify(exactly = 1) { redisService.put(any(), any(), any()) }
        verify(exactly = 1) { redisService.get(any(), JsonDigisosSoker::class.java) }

        val jsonDigisosSoker = objectMapper.readValue<JsonDigisosSoker>(ok_minimal_jsondigisossoker_response)
        every { redisService.get(any(), JsonDigisosSoker::class.java) } returns jsonDigisosSoker

        val result = fiksClient.hentDokument(id, "dokumentlagerId", JsonDigisosSoker::class.java, "Token")

        assertThat(result).isNotNull

        verify(exactly = 1) { redisService.put(any(), any(), any()) }
        verify(exactly = 2) { redisService.get(any(), JsonDigisosSoker::class.java) }
    }

    @Test
    fun `GET dokument - get fra cache returnerer feil type`() {
        // cache returnerer jsonsoknad, men vi forventer jsondigisossoker
        every { redisService.get(any(), JsonDigisosSoker::class.java) } returns null

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(ok_minimal_jsondigisossoker_response)
        )

        val result2 = fiksClient.hentDokument(id, "dokumentlagerId", JsonDigisosSoker::class.java, "Token")

        assertThat(result2).isNotNull

        verify(exactly = 1) { redisService.put(any(), any(), any()) }
    }

    @Test // fikk ikke mockWebServer til å funke her uten å skjønner hvorfor (InputStream-relatert), så gikk for "klassisk" mockk stil
    fun `POST ny ettersendelse`() {
        val webClient: WebClient = mockk()
        val clientForPost = FiksClientImpl(clientProperties, webClient, retryProperties, redisService)

        val fil1: InputStream = mockk()
        val fil2: InputStream = mockk()
        every { fil1.readAllBytes() } returns "test-fil".toByteArray()
        every { fil2.readAllBytes() } returns "div".toByteArray()

        val ettersendelsPdf = ByteArray(1)
        every { ettersendelsePdfGenerator.generate(any(), any()) } returns ettersendelsPdf
        every { krypteringService.krypter(any(), any(), any()) } returns fil1

        val files = listOf(FilForOpplasting("filnavn0", "image/png", 1L, fil1),
            FilForOpplasting("filnavn1", "image/jpg", 1L, fil2))

        every {
            webClient.get()
                .uri(any(), any<String>())
                .headers(any())
                .retrieve()
                .onStatus(any(), any())
                .onStatus(any(), any())
                .onStatus(any(), any())
                .bodyToMono<DigisosSak>()
                .block()
        } returns objectMapper.readValue(ok_digisossak_response, DigisosSak::class.java)

        every {
            webClient.post()
                .uri(any(), any<String>(), any<String>(), any<String>())
                .headers(any())
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(any())
                .retrieve()
                .onStatus(any(), any())
                .onStatus(any(), any())
                .toEntity<String>()
                .block()
        } returns ResponseEntity<String>(HttpStatus.ACCEPTED)

        assertThatCode {
            clientForPost.lastOppNyEttersendelse(files,
                JsonVedleggSpesifikasjon(),
                id,
                "token")
        }.doesNotThrowAnyException()
    }

    @Test
    internal fun `should produce body for upload`() {
        val fil1: InputStream = mockk()
        val fil2: InputStream = mockk()
        every { fil1.readAllBytes() } returns "test-fil".toByteArray()
        every { fil2.readAllBytes() } returns "div".toByteArray()

        val files = listOf(FilForOpplasting("filnavn0", "image/png", 1L, fil1),
            FilForOpplasting("filnavn1", "image/jpg", 1L, fil2))
        val body = fiksClient.createBodyForUpload(JsonVedleggSpesifikasjon(), files)

        assertThat(body.size == 5)
        assertThat(body.keys.contains("vedlegg.json"))
        assertThat(body.keys.contains("vedleggSpesifikasjon:0"))
        assertThat(body.keys.contains("dokument:0"))
        assertThat(body.keys.contains("vedleggSpesifikasjon:1"))
        assertThat(body.keys.contains("dokument:1"))
        assertThat(body["dokument:0"].toString().contains("InputStream resource"))
        assertThat(body["dokument:1"].toString().contains("InputStream resource"))
        assertThat(body["vedlegg.json"].toString().contains("text/plain;charset=UTF-8"))
        assertThat(body["vedleggSpesifikasjon:0"].toString().contains("text/plain;charset=UTF-8"))
        assertThat(body["vedleggSpesifikasjon:1"].toString().contains("text/plain;charset=UTF-8"))
    }
}