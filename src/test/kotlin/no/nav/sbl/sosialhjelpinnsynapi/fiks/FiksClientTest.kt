package no.nav.sbl.sosialhjelpinnsynapi.fiks

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.domain.KommuneInfo
import no.nav.sbl.sosialhjelpinnsynapi.responses.ok_digisossak_response
import no.nav.sbl.sosialhjelpinnsynapi.typeRef
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate
import org.springframework.web.multipart.MultipartFile

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

        val result = fiksClient.hentInformasjonOmKommuneErPaakoblet("1234")

        assertThat(result).isNotNull
    }

    @Test
    fun `POST ny ettersendelse`() {
        val response: ResponseEntity<String> = mockk()
        every { response.statusCode } returns HttpStatus.OK

        every { restTemplate.exchange(any<String>(), HttpMethod.POST, any(), String::class.java) } returns response

        val file: MultipartFile = mockk()
        every { file.originalFilename } returns "filnavn.pdf"
        every { file.contentType } returns "application/pdf"
        every { file.size } returns 42
        every { file.bytes } returns "fil".toByteArray()

        assertThatCode { fiksClient.lastOppNyEttersendelse(file, kommunenummer, id, "token") }.doesNotThrowAnyException()
    }
}