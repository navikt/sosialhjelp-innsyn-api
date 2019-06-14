package no.nav.sbl.sosialhjelpinnsynapi.fiks

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import no.nav.sbl.sosialhjelpinnsynapi.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.domain.KommuneInfo
import no.nav.sbl.sosialhjelpinnsynapi.responses.ok_digisossak_response
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate

@ExtendWith(MockKExtension::class)
internal class FiksClientTest {

    @MockK(relaxed = true)
    lateinit var clientProperties: ClientProperties

    @MockK
    lateinit var restTemplate: RestTemplate

    @InjectMockKs
    lateinit var fiksclient: FiksClient

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

        val result = fiksclient.hentDigisosSak("123")

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

        val result = fiksclient.hentAlleDigisosSaker()

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

        val result = fiksclient.hentInformasjonOmKommuneErPaakoblet("1234")

        assertNotNull(result)
    }
}