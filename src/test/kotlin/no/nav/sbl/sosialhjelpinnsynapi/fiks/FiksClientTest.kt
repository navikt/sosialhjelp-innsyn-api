package no.nav.sbl.sosialhjelpinnsynapi.fiks

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedleggSpesifikasjon
import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.domain.KommuneInfo
import no.nav.sbl.sosialhjelpinnsynapi.responses.ok_digisossak_response
import no.nav.sbl.sosialhjelpinnsynapi.typeRef
import no.nav.sbl.sosialhjelpinnsynapi.vedlegg.FilForOpplasting
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestTemplate


internal class FiksClientTest {

    private val clientProperties: ClientProperties = mockk(relaxed = true)
    private val restTemplate: RestTemplate = mockk()

    private val fiksClient = FiksClientImpl(clientProperties, restTemplate)

    private val id = "123"
    private val kommunenummer = "1337"
    private val navEksternRefId = "42"

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
    fun `GET alle DigisosSaker`() {
        val mockListResponse: ResponseEntity<List<String>> = mockk()
        every { mockListResponse.statusCode.is2xxSuccessful } returns true
        every { mockListResponse.body } returns listOf(ok_digisossak_response, ok_digisossak_response)

        every {
            restTemplate.exchange(
                    any<String>(),
                    any(),
                    any(),
                    typeRef<List<String>>())
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

        every {
            restTemplate.getForEntity(
                    any<String>(),
                    KommuneInfo::class.java)
        } returns mockKommuneResponse

        val result = fiksClient.hentKommuneInfo("1234")

        assertThat(result).isNotNull
    }

    @Test
    fun `POST ny ettersendelse`() {
        val mockDigisosSakResponse: ResponseEntity<String> = mockk()
        every { mockDigisosSakResponse.statusCode.is2xxSuccessful } returns true
        every { mockDigisosSakResponse.body } returns ok_digisossak_response
        every { restTemplate.exchange(any<String>(), HttpMethod.GET, any(), String::class.java) } returns mockDigisosSakResponse

        val slot = slot<HttpEntity<LinkedMultiValueMap<String, Any>>>()
        val mockFiksResponse: ResponseEntity<String> = mockk()
        every { mockFiksResponse.statusCode.is2xxSuccessful } returns true
        every { restTemplate.exchange(any<String>(), HttpMethod.POST, capture(slot), String::class.java) } returns mockFiksResponse

        val files = listOf(FilForOpplasting("filnavn0", "image/png", 1L, mockk()),
                FilForOpplasting("filnavn1", "image/jpg", 1L, mockk()))

        Assertions.assertThatCode { fiksClient.lastOppNyEttersendelse(files, JsonVedleggSpesifikasjon(), id, "token") }.doesNotThrowAnyException()

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