package no.nav.sosialhjelp.innsyn.client.digisosapi

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sosialhjelp.innsyn.client.fiks.FiksClientImpl
import no.nav.sosialhjelp.innsyn.config.ClientProperties
import no.nav.sosialhjelp.innsyn.domain.DigisosApiWrapper
import no.nav.sosialhjelp.innsyn.domain.SakWrapper
import no.nav.sosialhjelp.innsyn.responses.ok_komplett_jsondigisossoker_response
import no.nav.sosialhjelp.innsyn.service.idporten.IdPortenService
import org.junit.jupiter.api.Test
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate


internal class DigisosApiClientTest {
    private val clientProperties: ClientProperties = mockk(relaxed = true)

    @Test
    fun `Post digisos sak til mock`() {
        val restTemplate: RestTemplate = mockk()
        val idPortenService: IdPortenService = mockk()
        val fiksClientImpl: FiksClientImpl = mockk()

        val digisosApiClient = DigisosApiClientImpl(clientProperties, restTemplate, idPortenService, fiksClientImpl)

        val mockResponse: ResponseEntity<String> = mockk()
        every { mockResponse.body } returns ok_komplett_jsondigisossoker_response
        coEvery { idPortenService.getToken().token } returns "Token"
        every {
            restTemplate.exchange(
                    any<String>(),
                    any(),
                    any(),
                    String::class.java)
        } returns mockResponse

        digisosApiClient.oppdaterDigisosSak("123123", DigisosApiWrapper(SakWrapper(JsonDigisosSoker()), ""))
    }
}