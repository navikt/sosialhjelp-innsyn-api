package no.nav.sbl.sosialhjelpinnsynapi.fiks

import io.mockk.*
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedleggSpesifikasjon
import no.nav.sbl.sosialhjelpinnsynapi.common.FiksException
import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.domain.DigisosSak
import no.nav.sbl.sosialhjelpinnsynapi.domain.KommuneInfo
import no.nav.sbl.sosialhjelpinnsynapi.idporten.IdPortenService
import no.nav.sbl.sosialhjelpinnsynapi.responses.ok_digisossak_response
import no.nav.sbl.sosialhjelpinnsynapi.typeRef
import no.nav.sbl.sosialhjelpinnsynapi.utils.objectmapper
import no.nav.sbl.sosialhjelpinnsynapi.vedlegg.FilForOpplasting
import org.assertj.core.api.Assertions.*
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
    private val idPortenService: IdPortenService = mockk()

    private val fiksClient = FiksClientImpl(clientProperties, restTemplate, idPortenService)

    private val id = "123"

    @BeforeEach
    fun init() {
        clearMocks(restTemplate)
    }

    @Test
    fun `GET eksakt 1 DigisosSak`() {
        val mockResponse: ResponseEntity<String> = mockk()

        every { mockResponse.statusCode.is2xxSuccessful } returns true
        every { mockResponse.body } returns ok_digisossak_response

        every {
            restTemplate.exchange(
                    any<String>(),
                    any(),
                    any(),
                    String::class.java)
        } returns mockResponse

        val result = fiksClient.hentDigisosSak(id, "Token")

        assertThat(result).isNotNull
    }

    @Test
    fun `GET DigisosSak feiler hvis Fiks gir 500`() {
        val mockResponse: ResponseEntity<String> = mockk()

        every { mockResponse.statusCode.is2xxSuccessful } returns true
        every { mockResponse.body } returns ok_digisossak_response

        every {
            restTemplate.exchange(
                    any<String>(),
                    any(),
                    any(),
                    String::class.java)
        } throws HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "some error")

        assertThatExceptionOfType(FiksException::class.java).isThrownBy { fiksClient.hentDigisosSak(id, "Token") }
    }

    @Test
    fun `GET alle DigisosSaker`() {
        val mockListResponse: ResponseEntity<List<DigisosSak>> = mockk()
        val digisosSakOk = objectmapper.readValue(ok_digisossak_response, DigisosSak::class.java)
        every { mockListResponse.statusCode.is2xxSuccessful } returns true
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
    fun `GET KommuneInfo for kommunenummer`() {
        val mockKommuneResponse: ResponseEntity<KommuneInfo> = mockk()
        val mockKommuneInfo: KommuneInfo = mockk()

        every { mockKommuneResponse.statusCode.is2xxSuccessful } returns true
        every { mockKommuneResponse.body } returns mockKommuneInfo

        coEvery { idPortenService.requestToken().token } returns "token"

        every {
            restTemplate.exchange(
                    any<String>(),
                    HttpMethod.GET,
                    any(),
                    KommuneInfo::class.java)
        } returns mockKommuneResponse

        val result = fiksClient.hentKommuneInfo("1234")

        assertThat(result).isNotNull
    }

    @Test
    fun `GET KommuneInfo feiler hvis kommuneInfo gir 404`() {
        val mockKommuneResponse: ResponseEntity<KommuneInfo> = mockk()
        val mockKommuneInfo: KommuneInfo = mockk()

        every { mockKommuneResponse.statusCode.is2xxSuccessful } returns true
        every { mockKommuneResponse.body } returns mockKommuneInfo

        coEvery { idPortenService.requestToken().token } returns "token"

        every {
            restTemplate.exchange(
                    any<String>(),
                    HttpMethod.GET,
                    any(),
                    KommuneInfo::class.java)
        } throws HttpClientErrorException(HttpStatus.NOT_FOUND, "not found")

        assertThatExceptionOfType(FiksException::class.java).isThrownBy { fiksClient.hentKommuneInfo("1234") }
    }

    @Test
    fun `POST ny ettersendelse`() {
        val fil1: InputStream = mockk()
        val fil2: InputStream = mockk()

        every { fil1.readAllBytes() } returns "test-fil".toByteArray()
        every { fil2.readAllBytes() } returns "div".toByteArray()

        val mockDigisosSakResponse: ResponseEntity<String> = mockk()
        every { mockDigisosSakResponse.statusCode.is2xxSuccessful } returns true
        every { mockDigisosSakResponse.body } returns ok_digisossak_response
        every { restTemplate.exchange(any<String>(), HttpMethod.GET, any(), String::class.java) } returns mockDigisosSakResponse

        val slot = slot<HttpEntity<LinkedMultiValueMap<String, Any>>>()
        val mockFiksResponse: ResponseEntity<String> = mockk()
        every { mockFiksResponse.statusCode.is2xxSuccessful } returns true
        every { restTemplate.exchange(any<String>(), HttpMethod.POST, capture(slot), String::class.java) } returns mockFiksResponse

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