package no.nav.sosialhjelp.innsyn.integrasjonstest

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
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
import no.nav.sosialhjelp.innsyn.saksoversikt.soknadapi.SoknadApiClient
import no.nav.sosialhjelp.innsyn.testutils.IntegrasjonstestStubber
import no.nav.sosialhjelp.innsyn.testutils.MockOauth2ServerUtils
import no.nav.sosialhjelp.innsyn.utils.objectMapper
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
class SaksOversiktItest {

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

    @MockkBean
    lateinit var soknadApiClient: SoknadApiClient

    private val navEnhet: NavEnhet = mockk()

    var token: String = ""

    @BeforeEach
    fun setUp() {
        token = mockLogin.hentLevel4SelvbetjeningToken()
    }

    @Test
    fun `skal hente liste med saker`() {

        val digisosSakOk = objectMapper.readValue(ok_digisossak_response, DigisosSak::class.java)
        every { fiksClient.hentAlleDigisosSaker(any()) } returns listOf(digisosSakOk)

        every { soknadApiClient.getSvarUtSoknader(any()) } returns emptyList()

        webClient
            .get()
            .uri("/api/v1/innsyn/saker")
            .accept(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            .exchange()
            .expectStatus().isOk

        verify(exactly = 1) { fiksClient.hentAlleDigisosSaker(any()) }
    }

    @Test
    fun `skal hente saksdetaljer for sak`() {

        val digisosSakOk = objectMapper.readValue(ok_digisossak_response, DigisosSak::class.java)
        val soker = objectMapper.readValue(ok_komplett_jsondigisossoker_response, JsonDigisosSoker::class.java)
        val soknad = JsonSoknad()

        every { fiksClient.hentDigisosSak(any(), any(), any()) } returns digisosSakOk
        every { kommuneService.hentKommuneInfo(any(), any()) } returns IntegrasjonstestStubber.lagKommuneInfoStub()
        every { kommuneService.erInnsynDeaktivertForKommune(any(), any()) } returns false
        every { fiksClient.hentDokument(any(), any(), JsonDigisosSoker::class.java, any(), any()) } returns soker
        every { norgClient.hentNavEnhet(any()) } returns navEnhet
        every { navEnhet.navn } returns "testNavKontor"
        every { fiksClient.hentDokument(any(), any(), JsonSoknad::class.java, any(), any()) } returns soknad

        webClient
            .get()
            .uri("/api/v1/innsyn/saksDetaljer?id=1234")
            .accept(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            .exchange()
            .expectStatus().isOk

        verify(exactly = 2) { fiksClient.hentDigisosSak(any(), any(), any()) }
        verify(exactly = 1) { fiksClient.hentDokument(any(), any(), JsonSoknad::class.java, any(), any()) }
        verify(exactly = 2) { fiksClient.hentDokument(any(), any(), JsonDigisosSoker::class.java, any(), any()) }
    }

    @Test
    fun `skal returnere bad request dersom id requestparameter ikke er lagt til for saksDetaljer endepunkt`() {

        webClient
            .get()
            .uri("/api/v1/innsyn/saksDetaljer")
            .accept(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            .exchange()
            .expectStatus().isBadRequest
    }
}
