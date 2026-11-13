package no.nav.sosialhjelp.innsyn.klage.fiks

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import no.nav.sosialhjelp.innsyn.app.texas.TexasClient
import no.nav.sosialhjelp.innsyn.app.token.Token
import no.nav.sosialhjelp.innsyn.utils.sosialhjelpJsonMapper
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.http.codec.json.JacksonJsonDecoder
import org.springframework.web.reactive.function.client.WebClient
import java.util.UUID

internal class FiksKlageClientTest {
    private val mockWebServer = MockWebServer()
    private val texasClient: TexasClient = mockk()

    @AfterEach
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `henter klagestatuser med Maskinporten-token`() =
        runTest {
            coEvery { texasClient.getMaskinportenToken() } returns Token("maskinporten-token")
            mockWebServer.enqueue(
                MockResponse()
                    .setHeader("Content-Type", "application/json")
                    .setBody(
                        """
                        {
                          "statusListe": [
                            {
                              "digisosId": "11111111-1111-1111-1111-111111111111",
                              "klageId": "22222222-2222-2222-2222-222222222222",
                              "levertFagsystem": true,
                              "tidspunkt": 1794564000000,
                              "kommunenr": "0301"
                            }
                          ]
                        }
                        """.trimIndent(),
                    ),
            )
            val client = FiksKlageClientImpl(fiksWebClient(), texasClient)

            val statuser = client.hentKlageStatus(30, "sporings-id")

            assertThat(statuser).containsExactly(
                KlageForsendelseStatus(
                    digisosId = UUID.fromString("11111111-1111-1111-1111-111111111111"),
                    klageId = UUID.fromString("22222222-2222-2222-2222-222222222222"),
                    levertFagsystem = true,
                    tidspunkt = 1794564000000L,
                    kommunenr = "0301",
                ),
            )

            val request = mockWebServer.takeRequest()
            assertThat(request.method).isEqualTo("GET")
            assertThat(request.requestUrl?.encodedPath).isEqualTo("/digisos/klage/api/v1/status")
            assertThat(request.requestUrl?.queryParameter("antallDager")).isEqualTo("30")
            assertThat(request.requestUrl?.queryParameter("sporingsId")).isEqualTo("sporings-id")
            assertThat(request.getHeader("Authorization")).isEqualTo("Bearer maskinporten-token")
        }

    private fun fiksWebClient(): WebClient =
        WebClient
            .builder()
            .baseUrl(mockWebServer.url("/").toString())
            .codecs {
                it.defaultCodecs().jacksonJsonDecoder(JacksonJsonDecoder(sosialhjelpJsonMapper))
            }.build()
}
