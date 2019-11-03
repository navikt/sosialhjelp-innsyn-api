package no.nav.sbl.sosialhjelpinnsynapi.digisosapi

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.fiks.FiksClientImpl
import no.nav.sbl.sosialhjelpinnsynapi.idporten.AccessToken
import no.nav.sbl.sosialhjelpinnsynapi.idporten.IdPortenService
import no.nav.sbl.sosialhjelpinnsynapi.responses.ok_komplett_jsondigisossoker_response
import no.nav.sbl.sosialhjelpinnsynapi.utils.DigisosApiWrapper
import no.nav.sbl.sosialhjelpinnsynapi.utils.SakWrapper
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
        every { mockResponse.statusCode.is2xxSuccessful } returns true
        every { mockResponse.body } returns ok_komplett_jsondigisossoker_response
        coEvery { idPortenService.requestToken() } returns (AccessToken("Token"))
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