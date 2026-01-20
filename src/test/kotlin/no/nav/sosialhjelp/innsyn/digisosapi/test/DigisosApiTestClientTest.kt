package no.nav.sosialhjelp.innsyn.digisosapi.test

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sosialhjelp.innsyn.app.texas.TexasClient
import no.nav.sosialhjelp.innsyn.app.token.Token
import no.nav.sosialhjelp.innsyn.digisosapi.test.dto.DigisosApiWrapper
import no.nav.sosialhjelp.innsyn.digisosapi.test.dto.SakWrapper
import no.nav.sosialhjelp.innsyn.responses.ok_komplett_jsondigisossoker_response
import no.nav.sosialhjelp.innsyn.utils.sosialhjelpJsonMapper
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient
import kotlin.time.Duration.Companion.seconds

internal class DigisosApiTestClientTest {
    private val mockWebServer = MockWebServer()

    @AfterEach
    internal fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `Post digisos sak til mock`() =
        runTest(timeout = 5.seconds) {
            val fiksWebClient = WebClient.create(mockWebServer.url("/").toString())
            val digisosApiWebClient = WebClient.create(mockWebServer.url("/").toString())
            val texasClient: TexasClient = mockk()

            val digisosApiTestClient = DigisosApiTestClientImpl(fiksWebClient, digisosApiWebClient, texasClient)

            coEvery { texasClient.getMaskinportenToken() } returns Token("token")

            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(202)
                    .setBody("ok"),
            )

            val jsonDigisosSoker =
                sosialhjelpJsonMapper.readValue(ok_komplett_jsondigisossoker_response, JsonDigisosSoker::class.java)

            digisosApiTestClient.oppdaterDigisosSak("123123", DigisosApiWrapper(SakWrapper(jsonDigisosSoker), ""))
        }
}
