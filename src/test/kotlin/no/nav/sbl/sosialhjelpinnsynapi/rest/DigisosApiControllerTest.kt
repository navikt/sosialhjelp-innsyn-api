package no.nav.sbl.sosialhjelpinnsynapi.rest

import io.mockk.*
import no.nav.sbl.sosialhjelpinnsynapi.digisosapi.DigisosApiService
import no.nav.sbl.sosialhjelpinnsynapi.domain.DigisosSak
import no.nav.sbl.sosialhjelpinnsynapi.domain.InternalDigisosSoker
import no.nav.sbl.sosialhjelpinnsynapi.domain.Sak
import no.nav.sbl.sosialhjelpinnsynapi.domain.SoknadsStatus.MOTTATT
import no.nav.sbl.sosialhjelpinnsynapi.domain.SoknadsStatus.UNDER_BEHANDLING
import no.nav.sbl.sosialhjelpinnsynapi.event.EventService
import no.nav.sbl.sosialhjelpinnsynapi.fiks.FiksClient
import no.nav.sbl.sosialhjelpinnsynapi.oppgave.OppgaveService
import no.nav.sbl.sosialhjelpinnsynapi.utils.IntegrationUtils.KILDE_INNSYN_API
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class DigisosApiControllerTest {

    private val digisosApiService: DigisosApiService = mockk()
    private val fiksClient: FiksClient = mockk()
    private val eventService: EventService = mockk()
    private val oppgaveService: OppgaveService = mockk()

    private val controller = DigisosApiController(digisosApiService, fiksClient, eventService, oppgaveService)

    private val digisosSak1: DigisosSak = mockk()
    private val digisosSak2: DigisosSak = mockk()

    private val model1: InternalDigisosSoker = mockk()
    private val model2: InternalDigisosSoker = mockk()

    private val sak1: Sak = mockk()
    private val sak2: Sak = mockk()

    @BeforeEach
    internal fun setUp() {
        clearMocks(digisosApiService, fiksClient, eventService, oppgaveService)

        every { digisosSak1.fiksDigisosId } returns "123"
        every { digisosSak1.sistEndret } returns 0L
        every { digisosSak1.digisosSoker } returns null

        every { digisosSak2.fiksDigisosId } returns "456"
        every { digisosSak2.sistEndret } returns 1000L
        every { digisosSak2.digisosSoker } returns mockk()

        every { eventService.createModel(digisosSak1, any()) } returns model1
        every { eventService.createModel(digisosSak2, any()) } returns model2

        every { oppgaveService.hentOppgaver("123", any()).size } returns 2
        every { oppgaveService.hentOppgaver("456", any()).size } returns 1
    }

    @Test
    fun `skal mappe fra DigisosSak til SakResponse`() {
        every { fiksClient.hentAlleDigisosSaker(any()) } returns listOf(digisosSak1, digisosSak2)

        every { model1.status } returns MOTTATT
        every { model2.status } returns UNDER_BEHANDLING

        every { model1.oppgaver.isEmpty() } returns false
        every { model2.oppgaver.isEmpty() } returns false

        every { sak1.tittel } returns "Livsopphold"
        every { sak2.tittel } returns "Strøm"

        every { model2.saker } returns mutableListOf(sak1, sak2)

        val response = controller.hentAlleSaker("token")
        val saker = response.body

        assertThat(saker).isNotNull
        assertThat(saker).hasSize(2)

        if (saker != null && saker.size == 2) {
            val first = saker[0]
            assertThat(first.soknadTittel).isEqualTo("Søknad om økonomisk sosialhjelp")
            assertThat(first.antallNyeOppgaver).isEqualTo(2)
            assertThat(first.kilde).isEqualTo(KILDE_INNSYN_API)

            val second = saker[1]
            assertThat(second.soknadTittel).contains("Livsopphold", "Strøm")
            assertThat(second.status).isEqualTo("$UNDER_BEHANDLING")
            assertThat(second.antallNyeOppgaver).isEqualTo(1)
            assertThat(second.kilde).isEqualTo(KILDE_INNSYN_API)
        }
    }

    @Test
    fun `hvis model ikke har noen oppgaver, skal ikke oppgaveService kalles`() {
        every { fiksClient.hentAlleDigisosSaker(any()) } returns listOf(digisosSak1)

        every { model1.status } returns MOTTATT
        every { model1.oppgaver.isEmpty() } returns true

        val response = controller.hentAlleSaker("token")
        val saker = response.body

        assertThat(saker).isNotNull
        assertThat(saker).hasSize(1)

        verify { oppgaveService wasNot Called }

        if (saker != null && saker.size == 1) {
            val first = saker[0]
            assertThat(first.antallNyeOppgaver).isEqualTo(null)
        }
    }

}