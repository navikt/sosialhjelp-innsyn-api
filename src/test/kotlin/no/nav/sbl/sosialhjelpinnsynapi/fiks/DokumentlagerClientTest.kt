package no.nav.sbl.sosialhjelpinnsynapi.fiks

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import no.nav.sbl.sosialhjelpinnsynapi.ClientProperties
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate

@ExtendWith(MockKExtension::class)
internal class DokumentlagerClientTest {

    @MockK(relaxed = true)
    lateinit var clientProperties: ClientProperties

    @MockK
    lateinit var restTemplate: RestTemplate

    @InjectMockKs
    lateinit var dokumentlagerClient: DokumentlagerClient

    @Test
    fun `GET dokument fra dokumentlager`() {
        val mockResponse = mockk<ResponseEntity<String>>()

        every { mockResponse.statusCode.is2xxSuccessful } returns true
        every { mockResponse.body } returns "ok"

        every {
            restTemplate.getForEntity(
                    any<String>(),
                    String::class.java)
        } returns mockResponse

        val result = dokumentlagerClient.hentDokument("123")

        assertNotNull(result)
    }
}