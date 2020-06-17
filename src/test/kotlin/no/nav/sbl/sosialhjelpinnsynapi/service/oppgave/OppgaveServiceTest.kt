package no.nav.sbl.sosialhjelpinnsynapi.service.oppgave

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.sbl.sosialhjelpinnsynapi.client.fiks.FiksClient
import no.nav.sbl.sosialhjelpinnsynapi.domain.InternalDigisosSoker
import no.nav.sbl.sosialhjelpinnsynapi.domain.Oppgave
import no.nav.sbl.sosialhjelpinnsynapi.event.EventService
import no.nav.sbl.sosialhjelpinnsynapi.service.vedlegg.InternalVedlegg
import no.nav.sbl.sosialhjelpinnsynapi.service.vedlegg.VedleggService
import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.api.fiks.EttersendtInfoNAV
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

internal class OppgaveServiceTest {

    private val eventService: EventService = mockk()
    private val vedleggService: VedleggService = mockk()
    private val fiksClient: FiksClient = mockk()
    private val service = OppgaveService(eventService, vedleggService, fiksClient)

    private val mockDigisosSak: DigisosSak = mockk()
    private val mockEttersendtInfoNAV: EttersendtInfoNAV = mockk()

    private val type = "brukskonto"
    private val tillegg = "fraarm"
    private val type2 = "sparekonto"
    private val tillegg2 = "sparegris"
    private val type3 = "bsu"
    private val tillegg3 = "bes svare umiddelbart"
    private val type4 = "pengebinge"
    private val tillegg4 = "Onkel Skrue penger"
    private val tidspunktForKrav = LocalDateTime.now().minusDays(5)
    private val tidspunktFoerKrav = LocalDateTime.now().minusDays(7)
    private val tidspunktEtterKrav = LocalDateTime.now().minusDays(3)
    private val frist = LocalDateTime.now()
    private val frist2 = LocalDateTime.now().plusDays(1)
    private val frist3 = LocalDateTime.now().plusDays(2)
    private val frist4 = LocalDateTime.now().plusDays(3)

    private val token = "token"

    @BeforeEach
    fun init() {
        clearAllMocks()
        every { fiksClient.hentDigisosSak(any(), any(), any()) } returns mockDigisosSak
        every { mockDigisosSak.ettersendtInfoNAV } returns mockEttersendtInfoNAV
    }

    @Test
    fun `Should return emptylist`() {
        val model = InternalDigisosSoker()

        every { eventService.createModel(any(), any()) } returns model

        val oppgaver = service.hentOppgaver("123", token)

        assertThat(oppgaver).isNotNull
        assertThat(oppgaver).isEmpty()
    }

    @Test
    fun `Should return oppgave`() {
        val model = InternalDigisosSoker()
        model.oppgaver.add(Oppgave("oppgaveId1", type, tillegg, frist, tidspunktForKrav, true))

        every { eventService.createModel(any(), any()) } returns model
        every { vedleggService.hentEttersendteVedlegg(any(), any(), any()) } returns emptyList()

        val responseList = service.hentOppgaver("123", token)

        assertThat(responseList).isNotNull
        assertThat(responseList[0].innsendelsesfrist).isEqualTo(frist.toLocalDate())
        assertThat(responseList[0].oppgaveElementer).hasSize(1)
        assertThat(responseList[0].oppgaveElementer[0].dokumenttype).isEqualTo(type)
        assertThat(responseList[0].oppgaveElementer[0].tilleggsinformasjon).isEqualTo(tillegg)
        assertThat(responseList[0].oppgaveElementer[0].erFraInnsyn).isTrue()
    }

    @Test
    fun `Should return oppgave without tilleggsinformasjon`() {
        val model = InternalDigisosSoker()
        model.oppgaver.add(Oppgave("oppgaveId1", type, null, frist, tidspunktForKrav, true))

        every { eventService.createModel(any(), any()) } returns model
        every { vedleggService.hentEttersendteVedlegg(any(), any(), any()) } returns emptyList()

        val responseList = service.hentOppgaver("123", token)

        assertThat(responseList).isNotNull
        assertThat(responseList[0].innsendelsesfrist).isEqualTo(frist.toLocalDate())
        assertThat(responseList[0].oppgaveElementer).hasSize(1)
        assertThat(responseList[0].oppgaveElementer[0].dokumenttype).isEqualTo(type)
        assertThat(responseList[0].oppgaveElementer[0].tilleggsinformasjon).isNull()
        assertThat(responseList[0].oppgaveElementer[0].erFraInnsyn).isTrue()
    }

    @Test
    fun `Should return list of oppgaver sorted by frist`() {
        val model = InternalDigisosSoker()
        model.oppgaver.addAll(listOf(
                Oppgave("oppgaveId1", type, tillegg, frist, tidspunktForKrav, true),
                Oppgave("oppgaveId2", type3, tillegg3, frist3, tidspunktForKrav, true),
                Oppgave("oppgaveId3", type4, tillegg4, frist4, tidspunktForKrav, true),
                Oppgave("oppgaveId4", type2, tillegg2, frist2, tidspunktForKrav, true)))

        every { eventService.createModel(any(), any()) } returns model
        every { vedleggService.hentEttersendteVedlegg(any(), any(), any()) } returns emptyList()

        val responseList = service.hentOppgaver("123", token)

        assertThat(responseList).isNotNull
        assertThat(responseList.size == 4)
        assertThat(responseList[0].innsendelsesfrist).isEqualTo(frist.toLocalDate())
        assertThat(responseList[0].oppgaveElementer).hasSize(1)
        assertThat(responseList[0].oppgaveElementer[0].dokumenttype).isEqualTo(type)
        assertThat(responseList[0].oppgaveElementer[0].tilleggsinformasjon).isEqualTo(tillegg)

        assertThat(responseList[1].innsendelsesfrist).isEqualTo(frist2.toLocalDate())
        assertThat(responseList[1].oppgaveElementer).hasSize(1)
        assertThat(responseList[1].oppgaveElementer[0].dokumenttype).isEqualTo(type2)
        assertThat(responseList[1].oppgaveElementer[0].tilleggsinformasjon).isEqualTo(tillegg2)

        assertThat(responseList[2].innsendelsesfrist).isEqualTo(frist3.toLocalDate())
        assertThat(responseList[2].oppgaveElementer).hasSize(1)
        assertThat(responseList[2].oppgaveElementer[0].dokumenttype).isEqualTo(type3)
        assertThat(responseList[2].oppgaveElementer[0].tilleggsinformasjon).isEqualTo(tillegg3)

        assertThat(responseList[3].innsendelsesfrist).isEqualTo(frist4.toLocalDate())
        assertThat(responseList[3].oppgaveElementer).hasSize(1)
        assertThat(responseList[3].oppgaveElementer[0].dokumenttype).isEqualTo(type4)
        assertThat(responseList[3].oppgaveElementer[0].tilleggsinformasjon).isEqualTo(tillegg4)
    }

    @Test
    fun `Skal filtrere ut oppgaver der brukeren har lastet opp filer av samme type etter kravet ble gitt`() {
        val model = InternalDigisosSoker()
        model.oppgaver.addAll(listOf(
                Oppgave("oppgaveId1", type, tillegg, frist, tidspunktForKrav, true),
                Oppgave("oppgaveId2", type2, null, frist2, tidspunktForKrav, true),
                Oppgave("oppgaveId3", type3, tillegg3, frist3, tidspunktForKrav, true)))

        every { eventService.createModel(any(), any()) } returns model
        every { vedleggService.hentEttersendteVedlegg(any(), any(), any()) } returns listOf(
                InternalVedlegg(type, tillegg, emptyList(), tidspunktEtterKrav),
                InternalVedlegg(type2, null, emptyList(), tidspunktEtterKrav),
                InternalVedlegg(type3, tillegg3, emptyList(), tidspunktFoerKrav),
                InternalVedlegg(type3, null, emptyList(), tidspunktEtterKrav))

        val responseList = service.hentOppgaver("123", token)

        assertThat(responseList).isNotNull
        assertThat(responseList.size == 1)

        assertThat(responseList[0].innsendelsesfrist).isEqualTo(frist3.toLocalDate())
        assertThat(responseList[0].oppgaveElementer).hasSize(1)
        assertThat(responseList[0].oppgaveElementer[0].dokumenttype).isEqualTo(type3)
        assertThat(responseList[0].oppgaveElementer[0].tilleggsinformasjon).isEqualTo(tillegg3)
    }
}