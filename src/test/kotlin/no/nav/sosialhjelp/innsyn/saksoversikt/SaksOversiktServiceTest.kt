package no.nav.sosialhjelp.innsyn.saksoversikt

import io.mockk.every
import io.mockk.mockk
import no.finn.unleash.Unleash
import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.innsyn.app.featuretoggle.FAGSYSTEM_MED_INNSYN_I_PAPIRSOKNADER
import no.nav.sosialhjelp.innsyn.digisosapi.FiksClient
import no.nav.sosialhjelp.innsyn.digisossak.oppgaver.OppgaveService
import no.nav.sosialhjelp.innsyn.saksoversikt.soknadapi.SoknadApiClient
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.KILDE_INNSYN_API
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.KILDE_SOKNAD_API
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Date

internal class SaksOversiktServiceTest {

    private val fiksClient: FiksClient = mockk()
    private val soknadApiClient: SoknadApiClient = mockk()
    private val unleashClient: Unleash = mockk()
    private val oppgaveService: OppgaveService = mockk()

    private val saksOversiktService = SaksOversiktService(fiksClient, soknadApiClient, unleashClient, oppgaveService)

    private val digisosSak1: DigisosSak = mockk()
    private val digisosSak2: DigisosSak = mockk()

    @BeforeEach
    internal fun setUp() {
        every { digisosSak1.fiksDigisosId } returns "123"
        every { digisosSak1.sistEndret } returns 0L
        every { digisosSak1.kommunenummer } returns "0301"

        every { digisosSak2.fiksDigisosId } returns "456"
        every { digisosSak2.sistEndret } returns 1000L
        every { digisosSak2.kommunenummer } returns "0301"

        every { unleashClient.isEnabled(FAGSYSTEM_MED_INNSYN_I_PAPIRSOKNADER, false) } returns false
    }

    @Test
    internal fun `skal mappe fra DigisosSak til SaksListeResponse`() {
        every { fiksClient.hentAlleDigisosSaker(any()) } returns listOf(digisosSak1, digisosSak2)
        every { soknadApiClient.getSvarUtSoknader(any()) } returns emptyList()

        val alleSaker = saksOversiktService.hentAlleSaker("token")

        assertThat(alleSaker).hasSize(2)
        assertThat(alleSaker[0].fiksDigisosId).isEqualTo("123")
        assertThat(alleSaker[0].soknadTittel).isEqualTo("Søknad om økonomisk sosialhjelp")
        assertThat(alleSaker[0].kilde).isEqualTo(KILDE_INNSYN_API)
        assertThat(alleSaker[0].url).isNull()
        assertThat(alleSaker[1].fiksDigisosId).isEqualTo("456")
        assertThat(alleSaker[1].soknadTittel).isEqualTo("Søknad om økonomisk sosialhjelp")
        assertThat(alleSaker[1].kilde).isEqualTo(KILDE_INNSYN_API)
        assertThat(alleSaker[1].url).isNull()
    }

    @Test
    internal fun `skal hente SaksListeResponse fra SoknadApiClient`() {
        every { fiksClient.hentAlleDigisosSaker(any()) } returns emptyList()
        every { soknadApiClient.getSvarUtSoknader(any()) } returns listOf(
            SaksListeResponse(
                fiksDigisosId = null,
                soknadTittel = "Tittel",
                sistOppdatert = Date(),
                kilde = KILDE_SOKNAD_API,
                url = "someUrl"
            )
        )

        val alleSaker = saksOversiktService.hentAlleSaker("token")

        assertThat(alleSaker).hasSize(1)
        assertThat(alleSaker[0].fiksDigisosId).isNull()
        assertThat(alleSaker[0].soknadTittel).isEqualTo("Tittel")
        assertThat(alleSaker[0].kilde).isEqualTo(KILDE_SOKNAD_API)
        assertThat(alleSaker[0].url).isEqualTo("someUrl")
    }
}
