package no.nav.sbl.sosialhjelpinnsynapi.fiks

import io.mockk.*
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedleggSpesifikasjon
import no.nav.sbl.sosialhjelpinnsynapi.client.fiks.FiksEttersendelseClientImpl
import no.nav.sbl.sosialhjelpinnsynapi.client.fiks.FiksRetryProperties
import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.redis.RedisStore
import no.nav.sbl.sosialhjelpinnsynapi.responses.ok_digisossak_response
import no.nav.sbl.sosialhjelpinnsynapi.service.pdf.EttersendelsePdfGenerator
import no.nav.sbl.sosialhjelpinnsynapi.service.vedlegg.FilForOpplasting
import no.nav.sbl.sosialhjelpinnsynapi.service.vedlegg.KrypteringService
import org.apache.http.client.methods.CloseableHttpResponse
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestTemplate
import java.io.InputStream

internal class FiksEttersendelseClientTest {

    private val clientProperties: ClientProperties = mockk(relaxed = true)
    private val restTemplate: RestTemplate = mockk()
    private val redisStore: RedisStore = mockk()
    private val retryProperties: FiksRetryProperties = mockk()
    private val ettersendelsePdfGenerator: EttersendelsePdfGenerator = mockk()
    private val krypteringService: KrypteringService = mockk()
    private val fiksEttersendelseClient = FiksEttersendelseClientImpl(clientProperties, krypteringService, restTemplate)

    private val id = "123"

    @BeforeEach
    fun init() {
        clearAllMocks()

        every { redisStore.get(any()) } returns null
        every { redisStore.set(any(), any(), any()) } returns "OK"

        every { retryProperties.attempts } returns 2
        every { retryProperties.initialDelay } returns 5
        every { retryProperties.maxDelay } returns 10
    }

    @Test
    fun `POST ny ettersendelse`() {
        val fil1: InputStream = mockk()
        val fil2: InputStream = mockk()
        every { fil1.readAllBytes() } returns "test-fil".toByteArray()
        every { fil2.readAllBytes() } returns "div".toByteArray()

        val ettersendelsPdf = ByteArray(1)
        every { ettersendelsePdfGenerator.generate(any(), any() ) } returns ettersendelsPdf
        every { krypteringService.krypter(any(), any(), any(), any()) } returns fil1

        val mockDigisosSakResponse: ResponseEntity<String> = mockk()
        every { mockDigisosSakResponse.body } returns ok_digisossak_response
        every { restTemplate.exchange(any(), HttpMethod.GET, any(), String::class.java, id) } returns mockDigisosSakResponse

        val slot = slot<HttpEntity<LinkedMultiValueMap<String, Any>>>()
        val mockFiksResponse: ResponseEntity<String> = mockk()
        every { mockFiksResponse.statusCodeValue } returns 202
        every { restTemplate.exchange(any<String>(), HttpMethod.POST, capture(slot), String::class.java) } returns mockFiksResponse

        val files = listOf(FilForOpplasting("filnavn0", "image/png", 1L, fil1),
                FilForOpplasting("filnavn1", "image/jpg", 1L, fil2))

        assertThatCode { fiksEttersendelseClient.lastOppNyEttersendelse(files, JsonVedleggSpesifikasjon(), id, "behandlingsId", "0103", "token") }.doesNotThrowAnyException()

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


    @Disabled
    @Test
    fun `POST ny ettersendelse med apache client`() {
        val fil1: InputStream = mockk()
        val fil2: InputStream = mockk()
        every { fil1.readAllBytes() } returns "test-fil".toByteArray()
        every { fil2.readAllBytes() } returns "div".toByteArray()

        val ettersendelsPdf = ByteArray(1)
        every { ettersendelsePdfGenerator.generate(any(), any() ) } returns ettersendelsPdf
        every { krypteringService.krypter(any(), any(), any(), any()) } returns fil1

        val mockDigisosSakResponse: ResponseEntity<String> = mockk()
        every { mockDigisosSakResponse.body } returns ok_digisossak_response

        val slot = slot<HttpEntity<LinkedMultiValueMap<String, Any>>>()
        val mockFiksResponse: ResponseEntity<String> = mockk()
        val mockPostEttersendelseResponse: CloseableHttpResponse = mockk()
        every { mockFiksResponse.statusCodeValue } returns 202
        every { mockPostEttersendelseResponse.statusLine.statusCode } returns 202
        every { restTemplate.exchange(any(), HttpMethod.POST, capture(slot), String::class.java, any()) } returns mockFiksResponse
        every { fiksEttersendelseClient.postEttersendlse(any(), any()) } returns mockPostEttersendelseResponse

        val files = listOf(FilForOpplasting("filnavn0", "image/png", 1L, fil1),
                FilForOpplasting("filnavn1", "image/jpg", 1L, fil2))

        assertThatCode { fiksEttersendelseClient.lastOppNyEttersendelse(files, JsonVedleggSpesifikasjon(), id, "behandlingsId", "0103", "token") }
                .doesNotThrowAnyException()

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