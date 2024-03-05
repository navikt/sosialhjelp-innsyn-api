package no.nav.sosialhjelp.innsyn.integrasjonstest

import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sbl.soknadsosialhjelp.soknad.JsonSoknad
import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.innsyn.TestApplication
import no.nav.sosialhjelp.innsyn.digisosapi.FiksClientImpl
import no.nav.sosialhjelp.innsyn.kommuneinfo.KommuneService
import no.nav.sosialhjelp.innsyn.navenhet.NavEnhet
import no.nav.sosialhjelp.innsyn.navenhet.NorgClient
import no.nav.sosialhjelp.innsyn.responses.ok_digisossak_response
import no.nav.sosialhjelp.innsyn.responses.ok_komplett_jsondigisossoker_response
import no.nav.sosialhjelp.innsyn.testutils.IntegrasjonstestStubber
import no.nav.sosialhjelp.innsyn.testutils.MockOauth2ServerUtils
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
@SpringBootTest(classes = [TestApplication::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(profiles = ["mock-redis", "test", "local_unleash"])
@ExtendWith(MockKExtension::class)
internal class SaksStatusIntegrasjonsTest {
    @Autowired
    lateinit var mockLogin: MockOauth2ServerUtils

    @Autowired
    private lateinit var webClient: WebTestClient

    private val navEnhet: NavEnhet = mockk()

    @MockkBean
    lateinit var fiksClient: FiksClientImpl

    @MockkBean
    lateinit var kommuneService: KommuneService

    @MockkBean
    lateinit var norgClient: NorgClient

    var token: String = ""

    @BeforeEach
    fun setUp() {
        token = mockLogin.hentLevel4SelvbetjeningToken()
    }

    @AfterEach
    fun tearDown() {
    }

    @Test
    fun `Skal hente saksstatus for fiksDigisoID`() {
        val digisosSakOk = objectMapper.readValue(ok_digisossak_response, DigisosSak::class.java)
        val soknad = JsonSoknad()
        val soker = objectMapper.readValue(ok_komplett_jsondigisossoker_response, JsonDigisosSoker::class.java)

        coEvery { fiksClient.hentDigisosSak(any(), any(), any()) } returns digisosSakOk
        coEvery { fiksClient.hentDokument(any(), any(), JsonSoknad::class.java, any(), any()) } returns soknad
        coEvery { fiksClient.hentDokument(any(), any(), JsonDigisosSoker::class.java, any(), any()) } returns soker
        coEvery { kommuneService.hentKommuneInfo(any(), any()) } returns IntegrasjonstestStubber.lagKommuneInfoStub()
        coEvery { kommuneService.erInnsynDeaktivertForKommune(any(), any()) } returns false
        coEvery { norgClient.hentNavEnhet(any()) } returns navEnhet
        every { navEnhet.navn } returns "testNavKontor"

        webClient
            .get()
            .uri("/api/v1/innsyn/1234/saksStatus")
            .accept(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            .exchange()
            .expectStatus().isOk

        coVerify(exactly = 1) { fiksClient.hentDigisosSak(any(), any(), any()) }
        coVerify(exactly = 1) { fiksClient.hentDokument(any(), any(), JsonSoknad::class.java, any(), any()) }
        coVerify(exactly = 1) { fiksClient.hentDokument(any(), any(), JsonDigisosSoker::class.java, any(), any()) }
    }
}
