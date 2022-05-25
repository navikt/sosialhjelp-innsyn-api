package no.nav.sosialhjelp.innsyn.client.digisosapi

import io.mockk.coEvery
import io.mockk.mockk
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sosialhjelp.innsyn.client.fiks.FiksClientImpl
import no.nav.sosialhjelp.innsyn.client.maskinporten.MaskinportenClient
import no.nav.sosialhjelp.innsyn.domain.DigisosApiWrapper
import no.nav.sosialhjelp.innsyn.domain.SakWrapper
import no.nav.sosialhjelp.innsyn.responses.ok_komplett_jsondigisossoker_response
import no.nav.sosialhjelp.innsyn.utils.objectMapper
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient

internal class DigisosApiClientTest {

    @Test
    fun `Post digisos sak til mock`() {
        val mockWebServer = MockWebServer()
        val fiksWebClient = WebClient.create(mockWebServer.url("/").toString())
        val digisosApiWebClient = WebClient.create(mockWebServer.url("/").toString())
        val maskinportenClient: MaskinportenClient = mockk()
        val fiksClientImpl: FiksClientImpl = mockk()

        val digisosApiClient = DigisosApiClientImpl(fiksWebClient, digisosApiWebClient, maskinportenClient, fiksClientImpl)

        coEvery { maskinportenClient.getToken() } returns "Token"

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(202)
                .setBody("ok")
        )

        val jsonDigisosSoker =
            objectMapper.readValue(ok_komplett_jsondigisossoker_response, JsonDigisosSoker::class.java)

        digisosApiClient.oppdaterDigisosSak("123123", DigisosApiWrapper(SakWrapper(jsonDigisosSoker), ""))
    }
}
