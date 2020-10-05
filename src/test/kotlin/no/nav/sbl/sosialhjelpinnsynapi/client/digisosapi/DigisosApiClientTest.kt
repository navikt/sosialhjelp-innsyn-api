package no.nav.sbl.sosialhjelpinnsynapi.client.digisosapi

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sbl.sosialhjelpinnsynapi.client.fiks.FiksClientImpl
import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.domain.DigisosApiWrapper
import no.nav.sbl.sosialhjelpinnsynapi.domain.SakWrapper
import no.nav.sbl.sosialhjelpinnsynapi.responses.ok_komplett_jsondigisossoker_response
import no.nav.sbl.sosialhjelpinnsynapi.service.idporten.IdPortenService
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