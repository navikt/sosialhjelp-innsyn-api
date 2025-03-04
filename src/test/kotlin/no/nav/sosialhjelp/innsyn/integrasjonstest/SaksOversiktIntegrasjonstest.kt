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
import no.nav.sosialhjelp.innsyn.saksoversikt.SaksListeResponse
import no.nav.sosialhjelp.innsyn.testutils.IntegrasjonstestStubber
import no.nav.sosialhjelp.innsyn.testutils.MockOauth2ServerUtils
import no.nav.sosialhjelp.innsyn.utils.objectMapper
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest(classes = [TestApplication::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(profiles = ["mock-redis", "test"])
@ExtendWith(MockKExtension::class)
class SaksOversiktIntegrasjonstest {
    @Autowired
    private lateinit var webClient: WebTestClient

    @Autowired
    lateinit var mockLogin: MockOauth2ServerUtils

    @MockkBean
    lateinit var fiksClient: FiksClientImpl

    @MockkBean
    lateinit var kommuneService: KommuneService

    @MockkBean
    lateinit var norgClient: NorgClient

    private val navEnhet: NavEnhet = mockk()

    var token: String = ""

    @BeforeEach
    fun setUp() {
        mockLogin.init()
        token = mockLogin.hentLevel4SelvbetjeningToken()
    }

    @AfterEach
    fun cleanup() {
        mockLogin.cleanup()
    }

    @Test
    @WithMockUser
    fun `skal hente liste med saker`() {
        val digisosSakOk = objectMapper.readValue(ok_digisossak_response, DigisosSak::class.java)
        coEvery { fiksClient.hentAlleDigisosSaker(any()) } returns listOf(digisosSakOk)

        webClient
            .get()
            .uri("/api/v1/innsyn/saker")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk
            .expectBodyList(SaksListeResponse::class.java)
            .hasSize(1)

        coVerify(exactly = 1) { fiksClient.hentAlleDigisosSaker(any()) }
    }

    @Test
    @WithMockUser("123")
    fun `skal hente saksdetaljer for sak`() {
        val digisosSakOk = objectMapper.readValue(ok_digisossak_response, DigisosSak::class.java)
        val soker = objectMapper.readValue(ok_komplett_jsondigisossoker_response, JsonDigisosSoker::class.java)
        val soknad = JsonSoknad()

        coEvery { fiksClient.hentDigisosSak(any(), any()) } returns digisosSakOk
        coEvery { kommuneService.hentKommuneInfo(any(), any()) } returns IntegrasjonstestStubber.lagKommuneInfoStub()
        coEvery { kommuneService.erInnsynDeaktivertForKommune(any(), any()) } returns false
        coEvery { fiksClient.hentDokument(any(), any(), JsonDigisosSoker::class.java, any(), any()) } returns soker
        coEvery { norgClient.hentNavEnhet(any()) } returns navEnhet
        every { navEnhet.navn } returns "testNavKontor"
        coEvery { fiksClient.hentDokument(any(), any(), JsonSoknad::class.java, any(), any()) } returns soknad

        webClient
            .get()
            .uri("/api/v1/innsyn/sak/1234/detaljer")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk

        coVerify(exactly = 2) { fiksClient.hentDigisosSak(any(), any()) }
        coVerify(exactly = 1) { fiksClient.hentDokument(any(), any(), JsonSoknad::class.java, any(), any()) }
        coVerify(exactly = 2) { fiksClient.hentDokument(any(), any(), JsonDigisosSoker::class.java, any(), any()) }
    }
}
