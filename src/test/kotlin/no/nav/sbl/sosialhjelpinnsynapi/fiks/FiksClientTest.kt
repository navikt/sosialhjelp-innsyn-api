package no.nav.sbl.sosialhjelpinnsynapi.fiks

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.sbl.sosialhjelpinnsynapi.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.domain.KommuneInfo
import no.nav.sbl.sosialhjelpinnsynapi.responses.ok_digisossak_response
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate

internal class FiksClientTest {

    val clientProperties: ClientProperties = mockk(relaxed = true)
    val restTemplate: RestTemplate = mockk()

    val fiksClient = FiksClientImpl(clientProperties, restTemplate)

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
            restTemplate.getForEntity(
                    any<String>(),
                    String::class.java)
        } returns mockResponse

        val result = fiksClient.hentDigisosSak("123")

        assertNotNull(result)
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
                    null,
                    any<ParameterizedTypeReference<List<String>>>())
        } returns mockListResponse

        val result = fiksClient.hentAlleDigisosSaker()

        assertNotNull(result)
        assertEquals(2, result.size)
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

        assertNotNull(result)
    }
}