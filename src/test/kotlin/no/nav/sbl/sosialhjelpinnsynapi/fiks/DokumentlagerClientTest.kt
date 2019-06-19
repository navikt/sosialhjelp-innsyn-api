package no.nav.sbl.sosialhjelpinnsynapi.fiks

import io.mockk.every
import io.mockk.mockk
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sbl.sosialhjelpinnsynapi.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.responses.ok_komplett_jsondigisossoker_response
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate

internal class DokumentlagerClientTest {

    val clientProperties: ClientProperties = mockk(relaxed = true)
    val restTemplate: RestTemplate = mockk()

    val dokumentlagerClient = DokumentlagerClient(clientProperties, restTemplate)

    @Test
    fun `GET JsonDigisosSoker fra dokumentlager`() {
        val mockResponse = mockk<ResponseEntity<String>>()

        every { mockResponse.statusCode.is2xxSuccessful } returns true
        every { mockResponse.body } returns ok_komplett_jsondigisossoker_response

        every {
            restTemplate.getForEntity(
                    any<String>(),
                    String::class.java)
        } returns mockResponse

        val jsonDigisosSoker = dokumentlagerClient.hentDokument("123", JsonDigisosSoker::class.java) as JsonDigisosSoker

        assertNotNull(jsonDigisosSoker)
        assertEquals("Testsystemet", jsonDigisosSoker.avsender.systemnavn)
    }
}