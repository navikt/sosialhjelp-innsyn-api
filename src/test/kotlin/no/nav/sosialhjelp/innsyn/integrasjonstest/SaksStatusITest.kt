package no.nav.sosialhjelp.innsyn.integrasjonstest

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.junit5.MockKExtension
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sbl.soknadsosialhjelp.soknad.JsonSoknad
import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.api.fiks.KommuneInfo
import no.nav.sosialhjelp.innsyn.TestApplication
import no.nav.sosialhjelp.innsyn.digisosapi.FiksClientImpl
import no.nav.sosialhjelp.innsyn.kommuneinfo.KommuneService
import no.nav.sosialhjelp.innsyn.navenhet.NavEnhet
import no.nav.sosialhjelp.innsyn.navenhet.NorgClient
import no.nav.sosialhjelp.innsyn.responses.ok_digisossak_response
import no.nav.sosialhjelp.innsyn.responses.ok_komplett_jsondigisossoker_response
import no.nav.sosialhjelp.innsyn.utils.MockOauth2ServerUtils
import no.nav.sosialhjelp.innsyn.utils.objectMapper
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.reactive.server.WebTestClient

@ContextConfiguration(classes = [PdlIntegrationTestConfig::class])
@SpringBootTest(classes = [TestApplication::class], webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles(profiles = ["mock-redis", "test", "local_unleash"])
@ExtendWith(MockKExtension::class)
internal class SaksStatusITest {

    @Autowired
    lateinit var mockLogin: MockOauth2ServerUtils

    @Autowired
    private lateinit var webClient: WebTestClient

    @MockkBean
    lateinit var navEnhet: NavEnhet

    @MockkBean
    lateinit var soknad: JsonSoknad

    @MockkBean
    lateinit var fiksClient: FiksClientImpl

    @MockkBean
    lateinit var kommuneService: KommuneService

    @MockkBean
    lateinit var norgClient: NorgClient

    var token: String = ""

    @BeforeEach
    fun setUp() {
        token = mockLogin.hentLevel14SelvbetjeningToken()
    }

    @AfterEach
    fun tearDown() {
    }

    @Test
    fun `Skal hente saksstatus for fiksDigisoID`() {

        val digisosSakOk = objectMapper.readValue(ok_digisossak_response, DigisosSak::class.java)
        val soknad = JsonSoknad()
        val soker = objectMapper.readValue(ok_komplett_jsondigisossoker_response, JsonDigisosSoker::class.java)

        every { fiksClient.hentDigisosSak(any(), any(), any()) } returns digisosSakOk
        every { fiksClient.hentDokument(any(), any(), JsonSoknad::class.java, any()) } returns soknad
        every { fiksClient.hentDokument(any(), any(), JsonDigisosSoker::class.java, any()) } returns soker
        every { kommuneService.hentKommuneInfo(any(), any()) } returns lagKommuneInfoStub()
        every { kommuneService.erInnsynDeaktivertForKommune(any(), any()) } returns false
        every { norgClient.hentNavEnhet(any()) } returns navEnhet
        every { navEnhet.navn } returns "testNavKontor"

        webClient
            .get()
            .uri("/api/v1/innsyn/1234/saksStatus")
            .accept(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            .exchange()
            .expectStatus().isOk
    }

    private fun lagKommuneInfoStub(): KommuneInfo {
        val kommuneInfo = KommuneInfo(
            "1001",
            true,
            true,
            false,
            false,
            null,
            true,
            "Tester"
        )
        return kommuneInfo
    }
}
