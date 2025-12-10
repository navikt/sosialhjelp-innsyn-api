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
import no.nav.sosialhjelp.innsyn.testutils.IntegrasjonstestStubber
import no.nav.sosialhjelp.innsyn.utils.objectMapper
import org.junit.jupiter.api.Test
import org.springframework.security.test.context.support.WithMockUser

internal class SaksStatusIntegrasjonsTest : AbstractIntegrationTest() {
    private val navEnhet: NavEnhet = mockk()

    @MockkBean
    lateinit var fiksService: FiksService

    @MockkBean
    lateinit var kommuneService: KommuneService

    @MockkBean
    lateinit var norgClient: NorgClient

    @Test
    @WithMockUser("123")
    fun `Skal hente saksstatus for fiksDigisoID`() {
        val digisosSakOk = objectMapper.readValue(ok_digisossak_response, DigisosSak::class.java)
        val soknad = JsonSoknad()
        val soker = objectMapper.readValue(ok_komplett_jsondigisossoker_response, JsonDigisosSoker::class.java)

        coEvery { fiksService.getSoknad(any()) } returns digisosSakOk
        coEvery { fiksService.getDocument(any(), any(), JsonSoknad::class.java, any()) } returns soknad
        coEvery { fiksService.getDocument(any(), any(), JsonDigisosSoker::class.java, any()) } returns soker
        coEvery { kommuneService.hentKommuneInfo(any()) } returns IntegrasjonstestStubber.lagKommuneInfoStub()
        coEvery { kommuneService.erInnsynDeaktivertForKommune(any()) } returns false
        coEvery { norgClient.hentNavEnhet(any()) } returns navEnhet
        every { navEnhet.navn } returns "testNavKontor"

        doGet("/api/v1/innsyn/1234/saksStatus", emptyList())

        coVerify(exactly = 1) { fiksService.getSoknad(any()) }
        coVerify(exactly = 1) { fiksService.getDocument(any(), any(), JsonSoknad::class.java, any()) }
        coVerify(exactly = 1) { fiksService.getDocument(any(), any(), JsonDigisosSoker::class.java, any()) }
    }
}
