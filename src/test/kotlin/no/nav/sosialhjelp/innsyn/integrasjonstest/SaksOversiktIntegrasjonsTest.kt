package no.nav.sosialhjelp.innsyn.integrasjonstest

import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sbl.soknadsosialhjelp.soknad.JsonSoknad
import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.innsyn.digisosapi.FiksClientImpl
import no.nav.sosialhjelp.innsyn.kommuneinfo.KommuneService
import no.nav.sosialhjelp.innsyn.navenhet.NavEnhet
import no.nav.sosialhjelp.innsyn.navenhet.NorgClient
import no.nav.sosialhjelp.innsyn.responses.ok_digisossak_response
import no.nav.sosialhjelp.innsyn.responses.ok_komplett_jsondigisossoker_response
import no.nav.sosialhjelp.innsyn.saksoversikt.SaksListeResponse
import no.nav.sosialhjelp.innsyn.testutils.IntegrasjonstestStubber
import no.nav.sosialhjelp.innsyn.utils.objectMapper
import org.junit.jupiter.api.Test
import org.springframework.security.test.context.support.WithMockUser

class SaksOversiktIntegrasjonsTest : AbstractIntegrationTest() {
    @MockkBean
    lateinit var fiksClient: FiksClientImpl

    @MockkBean
    lateinit var kommuneService: KommuneService

    @MockkBean
    lateinit var norgClient: NorgClient

    private val navEnhet: NavEnhet = mockk()

    @Test
    @WithMockUser
    fun `skal hente liste med saker`() {
        val digisosSakOk = objectMapper.readValue(ok_digisossak_response, DigisosSak::class.java)
        coEvery { fiksClient.hentAlleDigisosSaker() } returns listOf(digisosSakOk)

        doGet(uri = "/api/v1/innsyn/saker")
            .expectBodyList(SaksListeResponse::class.java)
            .hasSize(1)

        coVerify(exactly = 1) { fiksClient.hentAlleDigisosSaker() }
    }

    @Test
    @WithMockUser("123")
    fun `skal hente saksdetaljer for sak`() {
        val digisosSakOk = objectMapper.readValue(ok_digisossak_response, DigisosSak::class.java)
        val soker = objectMapper.readValue(ok_komplett_jsondigisossoker_response, JsonDigisosSoker::class.java)
        val soknad = JsonSoknad()

        coEvery { fiksClient.hentDigisosSak(any()) } returns digisosSakOk
        coEvery { kommuneService.hentKommuneInfo(any()) } returns IntegrasjonstestStubber.lagKommuneInfoStub()
        coEvery { kommuneService.erInnsynDeaktivertForKommune(any()) } returns false
        coEvery { fiksClient.hentDokument(any(), any(), JsonDigisosSoker::class.java, any()) } returns soker
        coEvery { norgClient.hentNavEnhet(any()) } returns navEnhet
        every { navEnhet.navn } returns "testNavKontor"
        coEvery { fiksClient.hentDokument(any(), any(), JsonSoknad::class.java, any()) } returns soknad

        doGet("/api/v1/innsyn/sak/1234/detaljer")

        coVerify(exactly = 2) { fiksClient.hentDigisosSak(any()) }
        coVerify(exactly = 1) { fiksClient.hentDokument(any(), any(), JsonSoknad::class.java, any()) }
        coVerify(exactly = 2) { fiksClient.hentDokument(any(), any(), JsonDigisosSoker::class.java, any()) }
    }
}
