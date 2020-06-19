package no.nav.sbl.sosialhjelpinnsynapi.client.fiks

import com.fasterxml.jackson.module.kotlin.readValue
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedleggSpesifikasjon
import no.nav.sbl.sosialhjelpinnsynapi.common.FiksClientException
import no.nav.sbl.sosialhjelpinnsynapi.common.FiksServerException
import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.redis.RedisService
import no.nav.sbl.sosialhjelpinnsynapi.responses.ok_digisossak_response
import no.nav.sbl.sosialhjelpinnsynapi.responses.ok_minimal_jsondigisossoker_response
import no.nav.sbl.sosialhjelpinnsynapi.service.pdf.EttersendelsePdfGenerator
import no.nav.sbl.sosialhjelpinnsynapi.service.vedlegg.FilForOpplasting
import no.nav.sbl.sosialhjelpinnsynapi.service.vedlegg.KrypteringService
import no.nav.sbl.sosialhjelpinnsynapi.utils.objectMapper
import no.nav.sbl.sosialhjelpinnsynapi.utils.typeRef
import no.nav.sosialhjelp.api.fiks.DigisosSak
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestTemplate
import java.io.InputStream

internal class FiksClientTest {

    private val clientProperties: ClientProperties = mockk(relaxed = true)
    private val restTemplate: RestTemplate = mockk()
    private val redisService: RedisService = mockk()
    private val retryProperties: FiksRetryProperties = mockk()
    private val ettersendelsePdfGenerator: EttersendelsePdfGenerator = mockk()
    private val krypteringService: KrypteringService = mockk()
    private val fiksClient = FiksClientImpl(clientProperties, restTemplate, retryProperties, redisService)

    private val id = "123"

    @BeforeEach
    fun init() {
        clearAllMocks()

        every { redisService.get(any(), any()) } returns null
        every { redisService.put(any(), any()) } just Runs

        every { retryProperties.attempts } returns 2
        every { retryProperties.initialDelay } returns 5
        every { retryProperties.maxDelay } returns 10
    }

    @Test
    fun `GET eksakt 1 DigisosSak`() {
        val mockResponse: ResponseEntity<String> = mockk()
        every { mockResponse.body } returns ok_digisossak_response
        every {
            restTemplate.exchange(
                    any(),
                    any(),
                    any(),
                    String::class.java,
                    id)
        } returns mockResponse

        val result = fiksClient.hentDigisosSak(id, "Token", false)

        assertThat(result).isNotNull
    }

    @Test
    fun `GET digisosSak fra cache`() {
        val digisosSak = objectMapper.readValue<DigisosSak>(ok_digisossak_response)
        every { redisService.get(id, DigisosSak::class.java) } returns digisosSak

        val result2 = fiksClient.hentDigisosSak(id, "Token", true)

        assertThat(result2).isNotNull

        verify(exactly = 0) { redisService.put(any(), any()) }
    }

    @Test
    fun `GET digisosSak fra cache etter put`() {
        val mockResponse: ResponseEntity<String> = mockk()
        every { mockResponse.body } returns ok_digisossak_response
        every {
            restTemplate.exchange(
                    any(),
                    any(),
                    any(),
                    String::class.java,
                    id)
        } returns mockResponse

        val result1 = fiksClient.hentDigisosSak(id, "Token", true)

        assertThat(result1).isNotNull
        verify(exactly = 1) { redisService.put(any(), any()) }
        verify(exactly = 1) { redisService.get(any(), DigisosSak::class.java) }

        val digisosSak: DigisosSak = objectMapper.readValue<DigisosSak>(ok_digisossak_response)
        every { redisService.get(id, DigisosSak::class.java) } returns digisosSak

        val result = fiksClient.hentDigisosSak(id, "Token", true)

        assertThat(result).isNotNull

        verify(exactly = 1) { redisService.put(any(), any()) }
        verify(exactly = 2) { redisService.get(any(), any()) }
    }

    @Test
    fun `GET DigisosSak feiler hvis Fiks gir 500`() {
        every {
            restTemplate.exchange(
                    any(),
                    any(),
                    any(),
                    String::class.java,
                    id)
        } throws HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "some error")
        assertThatExceptionOfType(FiksServerException::class.java)
                .isThrownBy { fiksClient.hentDigisosSak(id, "Token", true) }

    }

    @Test
    fun `GET alle DigisosSaker skal bruke retry hvis Fiks gir 5xx-feil`() {
        every {
            restTemplate.exchange(
                    any<String>(),
                    any(),
                    any(),
                    typeRef<List<DigisosSak>>())
        } throws HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "some error")

        assertThatExceptionOfType(FiksServerException::class.java).isThrownBy { fiksClient.hentAlleDigisosSaker("Token") }

        verify(atLeast = 2) { restTemplate.exchange(any<String>(), any(), any(), typeRef<List<DigisosSak>>()) }
    }

    @Test
    fun `GET alle DigisosSaker skal ikke bruke retry hvis Fiks gir 4xx-feil`() {
        every {
            restTemplate.exchange(
                    any<String>(),
                    any(),
                    any(),
                    typeRef<List<DigisosSak>>())
        } throws HttpClientErrorException(HttpStatus.BAD_REQUEST, "some error")

        assertThatExceptionOfType(FiksClientException::class.java).isThrownBy { fiksClient.hentAlleDigisosSaker("Token") }

        verify(exactly = 1) { restTemplate.exchange(any<String>(), any(), any(), typeRef<List<DigisosSak>>()) }
    }

    @Test
    fun `GET alle DigisosSaker`() {
        val mockListResponse: ResponseEntity<List<DigisosSak>> = mockk()
        val digisosSakOk = objectMapper.readValue(ok_digisossak_response, DigisosSak::class.java)
        every { mockListResponse.body } returns listOf(digisosSakOk, digisosSakOk)
        every {
            restTemplate.exchange(
                    any<String>(),
                    any(),
                    any(),
                    typeRef<List<DigisosSak>>())
        } returns mockListResponse

        val result = fiksClient.hentAlleDigisosSaker("Token")

        assertThat(result).isNotNull
        assertThat(result).hasSize(2)
    }

    @Test
    fun `GET dokument`() {
        val mockResponse: ResponseEntity<String> = mockk()
        every { mockResponse.body } returns ok_minimal_jsondigisossoker_response
        every {
            restTemplate.exchange(
                    any(),
                    any(),
                    any(),
                    String::class.java,
                    any())
        } returns mockResponse

        val result = fiksClient.hentDokument(id, "dokumentlagerId", JsonDigisosSoker::class.java, "Token")

        assertThat(result).isNotNull
    }

    @Test
    fun `GET dokument fra cache`() {
        val jsonDigisosSoker = objectMapper.readValue<JsonDigisosSoker>(ok_minimal_jsondigisossoker_response)
        every { redisService.get(any(), JsonDigisosSoker::class.java) } returns jsonDigisosSoker

        val result2 = fiksClient.hentDokument(id, "dokumentlagerId", JsonDigisosSoker::class.java, "Token")

        assertThat(result2).isNotNull

        verify(exactly = 0) { redisService.put(any(), any()) }
    }

    @Test
    fun `GET dokument fra cache etter put`() {
        val mockResponse: ResponseEntity<String> = mockk()
        every { mockResponse.body } returns ok_digisossak_response
        every {
            restTemplate.exchange(
                    any(),
                    any(),
                    any(),
                    String::class.java,
                    any())
        } returns mockResponse

        val result1 = fiksClient.hentDokument(id, "dokumentlagerId", JsonDigisosSoker::class.java, "Token")

        assertThat(result1).isNotNull
        verify(exactly = 1) { redisService.put(any(), any()) }
        verify(exactly = 1) { redisService.get(any(), JsonDigisosSoker::class.java) }

        val jsonDigisosSoker = objectMapper.readValue<JsonDigisosSoker>(ok_minimal_jsondigisossoker_response)
        every { redisService.get(any(), JsonDigisosSoker::class.java) } returns jsonDigisosSoker

        val result = fiksClient.hentDokument(id, "dokumentlagerId", JsonDigisosSoker::class.java, "Token")

        assertThat(result).isNotNull

        verify(exactly = 1) { redisService.put(any(), any()) }
        verify(exactly = 2) { redisService.get(any(), JsonDigisosSoker::class.java) }
    }

    @Test
    fun `GET dokument - get fra cache returnerer feil type`() {
        // cache returnerer jsonsoknad, men vi forventer jsondigisossoker
        every { redisService.get(any(), JsonDigisosSoker::class.java) } returns null

        val mockResponse: ResponseEntity<String> = mockk()
        every { mockResponse.body } returns ok_minimal_jsondigisossoker_response
        every {
            restTemplate.exchange(
                    any(),
                    any(),
                    any(),
                    String::class.java,
                    any())
        } returns mockResponse

        val result2 = fiksClient.hentDokument(id, "dokumentlagerId", JsonDigisosSoker::class.java, "Token")

        assertThat(result2).isNotNull

        verify(exactly = 1) { redisService.put(any(), any()) }
    }

    @Test
    fun `POST ny ettersendelse`() {
        val fil1: InputStream = mockk()
        val fil2: InputStream = mockk()
        every { fil1.readAllBytes() } returns "test-fil".toByteArray()
        every { fil2.readAllBytes() } returns "div".toByteArray()

        val ettersendelsPdf = ByteArray(1)
        every { ettersendelsePdfGenerator.generate(any(), any()) } returns ettersendelsPdf
        every { krypteringService.krypter(any(), any(), any(), any()) } returns fil1

        val mockDigisosSakResponse: ResponseEntity<String> = mockk()
        every { mockDigisosSakResponse.body } returns ok_digisossak_response
        every { restTemplate.exchange(any(), HttpMethod.GET, any(), String::class.java, id) } returns mockDigisosSakResponse

        val slot = slot<HttpEntity<LinkedMultiValueMap<String, Any>>>()
        val mockFiksResponse: ResponseEntity<String> = mockk()
        every { mockFiksResponse.statusCodeValue } returns 202
        every { restTemplate.exchange(any(), HttpMethod.POST, capture(slot), String::class.java, any()) } returns mockFiksResponse

        val files = listOf(FilForOpplasting("filnavn0", "image/png", 1L, fil1),
                FilForOpplasting("filnavn1", "image/jpg", 1L, fil2))

        assertThatCode { fiksClient.lastOppNyEttersendelse(files, JsonVedleggSpesifikasjon(), id, "token") }.doesNotThrowAnyException()

        val httpEntity = slot.captured

        assertThat(httpEntity.body!!.size == 5)
        assertThat(httpEntity.headers["Content-Type"]!![0] == "multipart/form-data")
        assertThat(httpEntity.body!!.keys.contains("vedlegg.json"))
        assertThat(httpEntity.body!!.keys.contains("vedleggSpesifikasjon:0"))
        assertThat(httpEntity.body!!.keys.contains("dokument:0"))
        assertThat(httpEntity.body!!.keys.contains("vedleggSpesifikasjon:1"))
        assertThat(httpEntity.body!!.keys.contains("dokument:1"))
        assertThat(httpEntity.body!!["dokument:0"].toString().contains("InputStream resource"))
        assertThat(httpEntity.body!!["dokument:1"].toString().contains("InputStream resource"))
        assertThat(httpEntity.body!!["vedlegg.json"].toString().contains("text/plain;charset=UTF-8"))
        assertThat(httpEntity.body!!["vedleggSpesifikasjon:0"].toString().contains("text/plain;charset=UTF-8"))
        assertThat(httpEntity.body!!["vedleggSpesifikasjon:1"].toString().contains("text/plain;charset=UTF-8"))
    }
}