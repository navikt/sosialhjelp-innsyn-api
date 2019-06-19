package no.nav.sbl.sosialhjelpinnsynapi.fiks

import io.mockk.every
import io.mockk.mockk
import no.nav.sbl.sosialhjelpinnsynapi.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.domain.KommuneInfo
import no.nav.sbl.sosialhjelpinnsynapi.responses.ok_digisossak_response
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate

internal class FiksClientTest {

    val clientProperties: ClientProperties = mockk(relaxed = true)
    val restTemplate: RestTemplate = mockk()

    val fiksClient = FiksClient(clientProperties, restTemplate)

    @Test
    fun `GET eksakt 1 DigisosSak`() {
        val mockResponse = mockk<ResponseEntity<String>>()

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
        val mockResponse = mockk<ResponseEntity<List<String>>>()

        every { mockResponse.statusCode.is2xxSuccessful } returns true
        every { mockResponse.body } returns listOf(ok_digisossak_response, ok_digisossak_response)

        every {
            restTemplate.exchange(
                    any<String>(),
                    any(),
                    null,
                    any<ParameterizedTypeReference<List<String>>>())
        } returns mockResponse

        val result = fiksClient.hentAlleDigisosSaker()

        assertNotNull(result)
        assertEquals(2, result.size)
    }

    @Test
    fun `GET KommuneInfo for kommunenummer`() {
        val mockResponse = mockk<ResponseEntity<KommuneInfo>>()
        val mockKommuneInfo = mockk<KommuneInfo>()

        every { mockResponse.statusCode.is2xxSuccessful } returns true
        every { mockResponse.body } returns mockKommuneInfo

        every {
            restTemplate.getForEntity(
                    any<String>(),
                    KommuneInfo::class.java)
        } returns mockResponse

        val result = fiksClient.hentInformasjonOmKommuneErPaakoblet("1234")

        assertNotNull(result)
    }
}