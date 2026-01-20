package no.nav.sosialhjelp.innsyn.integrasjonstest

import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sbl.soknadsosialhjelp.soknad.JsonSoknad
import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.innsyn.digisosapi.FiksService
import no.nav.sosialhjelp.innsyn.kommuneinfo.KommuneService
import no.nav.sosialhjelp.innsyn.navenhet.NavEnhet
import no.nav.sosialhjelp.innsyn.navenhet.NorgClient
import no.nav.sosialhjelp.innsyn.responses.ok_digisossak_response
import no.nav.sosialhjelp.innsyn.responses.ok_komplett_jsondigisossoker_response
import no.nav.sosialhjelp.innsyn.saksoversikt.SaksListeResponse
import no.nav.sosialhjelp.innsyn.testutils.IntegrasjonstestStubber
import no.nav.sosialhjelp.innsyn.utils.sosialhjelpJsonMapper
import org.junit.jupiter.api.Test
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.reactive.server.expectBodyList

class SaksOversiktIntegrasjonsTest : AbstractIntegrationTest() {
    @MockkBean
    lateinit var fiksService: FiksService

    @MockkBean
    lateinit var kommuneService: KommuneService

    @MockkBean
    lateinit var norgClient: NorgClient

    private val navEnhet: NavEnhet = mockk()

    @Test
    @WithMockUser
    fun `skal hente liste med saker`() {
        val digisosSakOk = sosialhjelpJsonMapper.readValue(ok_digisossak_response, DigisosSak::class.java)
        coEvery { fiksService.getAllSoknader() } returns listOf(digisosSakOk)

        doGet(uri = "/api/v1/innsyn/saker", emptyList())
            .expectBodyList<SaksListeResponse>()
            .hasSize(1)

        coVerify(exactly = 1) { fiksService.getAllSoknader() }
    }

    @Test
    @WithMockUser("123")
    fun `skal hente saksdetaljer for sak`() {
        val digisosSakOk = sosialhjelpJsonMapper.readValue(ok_digisossak_response, DigisosSak::class.java)
        val soker = sosialhjelpJsonMapper.readValue(ok_komplett_jsondigisossoker_response, JsonDigisosSoker::class.java)
        val soknad = JsonSoknad()

        coEvery { fiksService.getSoknad(any()) } returns digisosSakOk
        coEvery { kommuneService.hentKommuneInfo(any()) } returns IntegrasjonstestStubber.lagKommuneInfoStub()
        coEvery { kommuneService.erInnsynDeaktivertForKommune(any()) } returns false
        coEvery { fiksService.getDocument(any(), any(), JsonDigisosSoker::class.java, any()) } returns soker
        coEvery { norgClient.hentNavEnhet(any()) } returns navEnhet
        every { navEnhet.navn } returns "testNavKontor"
        coEvery { fiksService.getDocument(any(), any(), JsonSoknad::class.java, any()) } returns soknad

        doGet("/api/v1/innsyn/sak/1234/detaljer", emptyList())
    }
}
