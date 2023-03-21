package no.nav.sosialhjelp.innsyn.digisossak.oppgaver

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedlegg
import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.api.fiks.DokumentInfo
import no.nav.sosialhjelp.api.fiks.EttersendtInfoNAV
import no.nav.sosialhjelp.innsyn.app.ClientProperties
import no.nav.sosialhjelp.innsyn.digisosapi.FiksClient
import no.nav.sosialhjelp.innsyn.domain.Dokumentasjonkrav
import no.nav.sosialhjelp.innsyn.domain.Fagsystem
import no.nav.sosialhjelp.innsyn.domain.Hendelse
import no.nav.sosialhjelp.innsyn.domain.InternalDigisosSoker
import no.nav.sosialhjelp.innsyn.domain.Oppgave
import no.nav.sosialhjelp.innsyn.domain.Oppgavestatus
import no.nav.sosialhjelp.innsyn.domain.SoknadsStatus
import no.nav.sosialhjelp.innsyn.domain.Vilkar
import no.nav.sosialhjelp.innsyn.event.EventService
import no.nav.sosialhjelp.innsyn.vedlegg.InternalVedlegg
import no.nav.sosialhjelp.innsyn.vedlegg.VedleggService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import no.nav.sosialhjelp.innsyn.domain.HendelseTekstType

internal class OppgaveServiceTest {

    private val eventService: EventService = mockk()
    private val vedleggService: VedleggService = mockk()
    private val fiksClient: FiksClient = mockk()
    private val clientProperties: ClientProperties = mockk()
    private val service = OppgaveService(eventService, vedleggService, fiksClient, clientProperties)

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

    private val dokumentasjonkravId = "068e5c6516019eec95f19dd4fd78045aa25b634849538440ba49f7050cdbe4ce"
    private val dokumentasjonkravId2 = "74234e98afe7498fb5daf1f36ac2d78acc339464f950703b8c019892f982b90b"

    private val dokumenttype = "tittel"
    private val dok = DokumentInfo("tittel 1", dokumentasjonkravId, 11)
    private val tidspunkt = LocalDateTime.now().plusHours(9)

    private val token = "token"

    @BeforeEach
    fun init() {
        clearAllMocks()
        every { fiksClient.hentDigisosSak(any(), any(), any()) } returns mockDigisosSak
        every { mockDigisosSak.ettersendtInfoNAV } returns mockEttersendtInfoNAV
        every { clientProperties.vilkarDokkravFagsystemVersjoner } returns listOf("socio;10.1.16", "mock-alt;1.0.0:MOCKVERSJON")
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
        model.oppgaver.add(Oppgave("oppgaveId1", type, tillegg, null, null, frist, tidspunktForKrav, true))

        every { eventService.createModel(any(), any()) } returns model
        every { vedleggService.hentEttersendteVedlegg(any(), any(), any()) } returns emptyList()

        val responseList = service.hentOppgaver("123", token)

        assertThat(responseList).isNotNull
        assertThat(responseList[0].innsendelsesfrist).isEqualTo(frist.toLocalDate())
        assertThat(responseList[0].oppgaveElementer).hasSize(1)
        assertThat(responseList[0].oppgaveElementer[0].dokumenttype).isEqualTo(type)
        assertThat(responseList[0].oppgaveElementer[0].tilleggsinformasjon).isEqualTo(tillegg)
        assertThat(responseList[0].oppgaveElementer[0].erFraInnsyn).isTrue
    }

    @Test
    fun `Should return oppgave without tilleggsinformasjon`() {
        val model = InternalDigisosSoker()
        model.oppgaver.add(Oppgave("oppgaveId1", type, null, null, null, frist, tidspunktForKrav, true))

        every { eventService.createModel(any(), any()) } returns model
        every { vedleggService.hentEttersendteVedlegg(any(), any(), any()) } returns emptyList()

        val responseList = service.hentOppgaver("123", token)

        assertThat(responseList).isNotNull
        assertThat(responseList[0].innsendelsesfrist).isEqualTo(frist.toLocalDate())
        assertThat(responseList[0].oppgaveElementer).hasSize(1)
        assertThat(responseList[0].oppgaveElementer[0].dokumenttype).isEqualTo(type)
        assertThat(responseList[0].oppgaveElementer[0].tilleggsinformasjon).isNull()
        assertThat(responseList[0].oppgaveElementer[0].erFraInnsyn).isTrue
    }

    @Test
    fun `Should return list of oppgaver sorted by frist`() {
        val model = InternalDigisosSoker()
        model.oppgaver.addAll(
            listOf(
                Oppgave("oppgaveId1", type, tillegg, null, null, frist, tidspunktForKrav, true),
                Oppgave("oppgaveId2", type3, tillegg3, null, null, frist3, tidspunktForKrav, true),
                Oppgave("oppgaveId3", type4, tillegg4, null, null, frist4, tidspunktForKrav, true),
                Oppgave("oppgaveId4", type2, tillegg2, null, null, frist2, tidspunktForKrav, true)
            )
        )

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
        model.oppgaver.addAll(
            listOf(
                Oppgave("oppgaveId1", type, tillegg, null, null, frist, tidspunktForKrav, true),
                Oppgave("oppgaveId2", type2, null, null, null, frist2, tidspunktForKrav, true),
                Oppgave("oppgaveId3", type3, tillegg3, null, null, frist3, tidspunktForKrav, true)
            )
        )

        every { eventService.createModel(any(), any()) } returns model
        every { vedleggService.hentEttersendteVedlegg(any(), any(), any()) } returns listOf(
            InternalVedlegg(type, tillegg, null, null, mutableListOf(), tidspunktEtterKrav, null),
            InternalVedlegg(type2, null, null, null, mutableListOf(), tidspunktEtterKrav, null),
            InternalVedlegg(type3, tillegg3, null, null, mutableListOf(), tidspunktFoerKrav, null),
            InternalVedlegg(type3, null, null, null, mutableListOf(), tidspunktEtterKrav, null)
        )

        val responseList = service.hentOppgaver("123", token)

        assertThat(responseList).isNotNull
        assertThat(responseList.size == 1)

        assertThat(responseList[0].innsendelsesfrist).isEqualTo(frist3.toLocalDate())
        assertThat(responseList[0].oppgaveElementer).hasSize(1)
        assertThat(responseList[0].oppgaveElementer[0].dokumenttype).isEqualTo(type3)
        assertThat(responseList[0].oppgaveElementer[0].tilleggsinformasjon).isEqualTo(tillegg3)
    }

    @Test
    fun `Should not return oppgaver when soknad is ferdig behandla`() {
        val model = InternalDigisosSoker()
        model.status = SoknadsStatus.FERDIGBEHANDLET
        model.oppgaver.add(Oppgave("oppgaveId1", type, null, null, null, frist, tidspunktForKrav, true))

        every { eventService.createModel(any(), any()) } returns model
        every { vedleggService.hentEttersendteVedlegg(any(), any(), any()) } returns emptyList()

        val responseList = service.hentOppgaver("123", token)

        assertThat(responseList).isEmpty()
    }

    @Test
    fun `Should return vilkar with tittel`() {
        val model = InternalDigisosSoker()
        val tittel = "VILKAR1"
        val beskrivelse = "mer vilkarer2"
        model.vilkar.addAll(
            listOf(
                Vilkar("vilkar1", tittel, "mer vilkarer1", Oppgavestatus.RELEVANT, null, LocalDateTime.now(), LocalDateTime.now()),
                Vilkar("vilkar2", null, beskrivelse, Oppgavestatus.RELEVANT, null, LocalDateTime.now(), LocalDateTime.now()),
                Vilkar("vilkar3", "", null, Oppgavestatus.RELEVANT, null, LocalDateTime.now(), LocalDateTime.now())
            )
        )
        every { eventService.createModel(any(), any()) } returns model

        val responseList = service.getVilkar("123", token)

        assertThat(responseList).isNotNull
        assertThat(responseList).hasSize(2)
        assertThat(responseList[0].tittel).isNotNull
        assertThat(responseList[0].tittel).isEqualTo(tittel)
        assertThat(responseList[1].tittel).isNotNull
        assertThat(responseList[1].tittel).isEqualTo(beskrivelse)
        assertThat(responseList[1].beskrivelse).isNull()
        assertThat(responseList[1].vilkarReferanse).isEqualTo("vilkar2")
    }

    @Test
    fun `Should not return Vilkar with status ANNULLERT`() {
        val model = InternalDigisosSoker()
        model.vilkar.addAll(
            listOf(
                Vilkar("vilkar1", "tittel", null, Oppgavestatus.ANNULLERT, null, LocalDateTime.now(), LocalDateTime.now()),
                Vilkar("vilkar2", "tittel", null, Oppgavestatus.RELEVANT, null, LocalDateTime.now(), LocalDateTime.now()),
            )
        )
        every { eventService.createModel(any(), any()) } returns model

        val responseList = service.getVilkar("123", token)

        assertThat(responseList).isNotNull
        assertThat(responseList.size == 1)
        assertThat(responseList).hasSize(1)
        assertThat(responseList[0].status).isEqualTo(Oppgavestatus.RELEVANT)
    }

    @Test
    fun `Should not return Dokumentasjonkrav with status ANNULERT or LEVERT_TIDLIGERE`() {
        val model = InternalDigisosSoker()
        model.dokumentasjonkrav.addAll(
            listOf(
                Dokumentasjonkrav(
                    dokumentasjonkravId,
                    JsonVedlegg.HendelseType.DOKUMENTASJONKRAV,
                    "dokumentasjonkrav1",
                    "tittel",
                    null,
                    Oppgavestatus.ANNULLERT,
                    null,
                    LocalDateTime.now(),
                    LocalDate.now()
                ),
                Dokumentasjonkrav(
                    dokumentasjonkravId,
                    JsonVedlegg.HendelseType.DOKUMENTASJONKRAV,
                    "dokumentasjonkrav1",
                    "tittel",
                    null,
                    Oppgavestatus.LEVERT_TIDLIGERE,
                    null,
                    LocalDateTime.now(),
                    LocalDate.now()
                ),
                Dokumentasjonkrav(
                    dokumentasjonkravId,
                    JsonVedlegg.HendelseType.DOKUMENTASJONKRAV,
                    "dokumentasjonkrav2",
                    "tittel",
                    null,
                    Oppgavestatus.RELEVANT,
                    null,
                    LocalDateTime.now(),
                    LocalDate.now()
                )
            )
        )
        every { eventService.createModel(any(), any()) } returns model
        every { vedleggService.hentEttersendteVedlegg(any(), any(), any()) } returns emptyList()

        val responseList = service.getDokumentasjonkrav("123", token)

        assertThat(responseList).isNotNull
        assertThat(responseList.size == 1)
        assertThat(responseList[0].dokumentasjonkravElementer).hasSize(1)
        assertThat(responseList[0].dokumentasjonkravElementer[0].status).isEqualTo(Oppgavestatus.RELEVANT)
    }

    @Test
    fun `Should return dokumentasjonkrav with tittel and filter out empty dokumentasjonkrav`() {
        val model = InternalDigisosSoker()
        model.dokumentasjonkrav.addAll(
            listOf(
                Dokumentasjonkrav(
                    dokumentasjonkravId,
                    JsonVedlegg.HendelseType.DOKUMENTASJONKRAV,
                    "dokumentasjonkrav1",
                    "tittel",
                    "beskrivelse1",
                    Oppgavestatus.RELEVANT,
                    null,
                    LocalDateTime.now(),
                    LocalDate.now()
                ),
                Dokumentasjonkrav(
                    dokumentasjonkravId,
                    JsonVedlegg.HendelseType.DOKUMENTASJONKRAV,
                    "dokumentasjonkrav2",
                    null,
                    "beskrivelse2",
                    Oppgavestatus.RELEVANT,
                    null,
                    LocalDateTime.now(),
                    LocalDate.now()
                ),
                Dokumentasjonkrav(
                    dokumentasjonkravId,
                    JsonVedlegg.HendelseType.DOKUMENTASJONKRAV,
                    "dokumentasjonkrav3",
                    "",
                    null,
                    Oppgavestatus.RELEVANT,
                    null,
                    LocalDateTime.now(),
                    LocalDate.now()
                ),
                Dokumentasjonkrav(
                    dokumentasjonkravId,
                    JsonVedlegg.HendelseType.DOKUMENTASJONKRAV,
                    "dokumentasjonkrav4",
                    null,
                    " ",
                    Oppgavestatus.RELEVANT,
                    null,
                    LocalDateTime.now(),
                    LocalDate.now()
                )
            )
        )
        every { eventService.createModel(any(), any()) } returns model
        every { vedleggService.hentEttersendteVedlegg(any(), any(), any()) } returns emptyList()

        val responseList = service.getDokumentasjonkrav("123", token)

        assertThat(responseList).isNotNull
        assertThat(responseList.size == 1)
        assertThat(responseList[0].dokumentasjonkravElementer).hasSize(2)
        assertThat(responseList[0].dokumentasjonkravElementer[0].tittel).isNotNull
        assertThat(responseList[0].dokumentasjonkravElementer[1].beskrivelse).isNull()
        assertThat(responseList[0].dokumentasjonkravElementer[1].tittel).isNotNull
        assertThat(responseList[0].dokumentasjonkravElementer[1].dokumentasjonkravReferanse).isEqualTo("dokumentasjonkrav2")
    }

    @Test
    fun `Should not include statuses OPPFYLT or IKKE_OPPFYLT for Dokumentasjonkrav`() {
        val model = InternalDigisosSoker()
        model.dokumentasjonkrav.addAll(
            listOf(
                Dokumentasjonkrav(
                    dokumentasjonkravId,
                    JsonVedlegg.HendelseType.DOKUMENTASJONKRAV,
                    "dokumentasjonkrav1",
                    "tittel",
                    "beskrivelse1",
                    Oppgavestatus.OPPFYLT,
                    null,
                    LocalDateTime.now(),
                    LocalDate.now()
                ),
                Dokumentasjonkrav(
                    dokumentasjonkravId,
                    JsonVedlegg.HendelseType.DOKUMENTASJONKRAV,
                    "dokumentasjonkrav2",
                    null,
                    "beskrivelse2",
                    Oppgavestatus.IKKE_OPPFYLT,
                    null,
                    LocalDateTime.now(),
                    LocalDate.now(),
                ),
                Dokumentasjonkrav(
                    dokumentasjonkravId,
                    JsonVedlegg.HendelseType.DOKUMENTASJONKRAV,
                    "dokumentasjonkrav4",
                    null,
                    "beskrivelse",
                    Oppgavestatus.RELEVANT,
                    null,
                    LocalDateTime.now(),
                    LocalDate.now(),
                )
            )
        )
        every { eventService.createModel(any(), any()) } returns model
        every { vedleggService.hentEttersendteVedlegg(any(), any(), any()) } returns emptyList()

        val responseList = service.getDokumentasjonkrav("123", token)

        assertThat(responseList).isNotNull
        assertThat(responseList.size == 1)
        assertThat(responseList[0].dokumentasjonkravElementer).hasSize(1)
        assertThat(responseList[0].dokumentasjonkravElementer[0].status).isEqualTo(Oppgavestatus.RELEVANT)
    }

    @Test
    fun `Should not include statuses OPPFYLT or IKKE_OPPFYLT for Vilkar`() {
        val model = InternalDigisosSoker()
        model.vilkar.addAll(
            listOf(
                Vilkar("vilkar1", "tittel", null, Oppgavestatus.OPPFYLT, null, LocalDateTime.now(), LocalDateTime.now()),
                Vilkar("vilkar2", "tittel", null, Oppgavestatus.IKKE_OPPFYLT, null, LocalDateTime.now(), LocalDateTime.now()),
                Vilkar("vilkar3", "tittel", null, Oppgavestatus.RELEVANT, null, LocalDateTime.now(), LocalDateTime.now()),

            )
        )
        every { eventService.createModel(any(), any()) } returns model
        every { vedleggService.hentEttersendteVedlegg(any(), any(), any()) } returns emptyList()

        val responseList = service.getVilkar("123", token)

        assertThat(responseList).isNotNull
        assertThat(responseList.size == 1)
        assertThat(responseList).hasSize(1)
        assertThat(responseList[0].status).isEqualTo(Oppgavestatus.RELEVANT)
    }

    @Test
    fun `Should sort Dokumentasjonkrav by date and group by date`() {
        val model = InternalDigisosSoker()

        model.dokumentasjonkrav.addAll(
            listOf(
                Dokumentasjonkrav(
                    dokumentasjonkravId,
                    JsonVedlegg.HendelseType.DOKUMENTASJONKRAV,
                    "dokumentasjonkrav1",
                    "tittel",
                    "",
                    Oppgavestatus.RELEVANT,
                    null,
                    LocalDateTime.now(),
                    frist2.toLocalDate()
                ),
                Dokumentasjonkrav(
                    dokumentasjonkravId,
                    JsonVedlegg.HendelseType.DOKUMENTASJONKRAV,
                    "dokumentasjonkrav2",
                    "tittel",
                    "",
                    Oppgavestatus.RELEVANT,
                    null,
                    LocalDateTime.now(),
                    frist2.toLocalDate()
                ),
                Dokumentasjonkrav(
                    dokumentasjonkravId,
                    JsonVedlegg.HendelseType.DOKUMENTASJONKRAV,
                    "dokumentasjonkrav3",
                    "tittel",
                    "",
                    Oppgavestatus.RELEVANT,
                    null,
                    LocalDateTime.now(),
                    frist.toLocalDate()
                ),
                Dokumentasjonkrav(
                    dokumentasjonkravId,
                    JsonVedlegg.HendelseType.DOKUMENTASJONKRAV,
                    "dokumentasjonkrav4",
                    "tittel",
                    "",
                    Oppgavestatus.RELEVANT,
                    null,
                    LocalDateTime.now(),
                    frist3.toLocalDate()
                ),
            )
        )
        every { eventService.createModel(any(), any()) } returns model
        every { vedleggService.hentEttersendteVedlegg(any(), any(), any()) } returns emptyList()

        val responseList = service.getDokumentasjonkrav("123", token)

        assertThat(responseList).isNotNull
        assertThat(responseList.size == 3)

        assertThat(responseList[0].dokumentasjonkravElementer).hasSize(1)
        assertThat(responseList[1].dokumentasjonkravElementer).hasSize(2)
        assertThat(responseList[2].dokumentasjonkravElementer).hasSize(1)

        assertThat(responseList[0].frist).isEqualTo(frist.toLocalDate())
        assertThat(responseList[1].frist).isEqualTo(frist2.toLocalDate())
        assertThat(responseList[1].frist).isEqualTo(frist2.toLocalDate())
        assertThat(responseList[2].frist).isEqualTo(frist3.toLocalDate())

        assertThat(responseList[0].frist).isBefore(responseList[1].frist)
    }

    @Test
    fun `Should group Dokumentasjonkrav without frist together`() {
        val model = InternalDigisosSoker()

        model.dokumentasjonkrav.addAll(
            listOf(
                Dokumentasjonkrav(
                    dokumentasjonkravId,
                    JsonVedlegg.HendelseType.DOKUMENTASJONKRAV,
                    "dokumentasjonkrav1",
                    "tittel",
                    "",
                    Oppgavestatus.RELEVANT,
                    null,
                    LocalDateTime.now(),
                    frist.toLocalDate()
                ),
                Dokumentasjonkrav(
                    dokumentasjonkravId,
                    JsonVedlegg.HendelseType.DOKUMENTASJONKRAV,
                    "dokumentasjonkrav3",
                    "tittel",
                    "",
                    Oppgavestatus.RELEVANT,
                    null,
                    LocalDateTime.now(),
                    null
                ),
                Dokumentasjonkrav(
                    dokumentasjonkravId,
                    JsonVedlegg.HendelseType.DOKUMENTASJONKRAV,
                    "dokumentasjonkrav2",
                    "tittel",
                    "",
                    Oppgavestatus.RELEVANT,
                    null,
                    LocalDateTime.now(),
                    frist2.toLocalDate()
                ),
                Dokumentasjonkrav(
                    dokumentasjonkravId,
                    JsonVedlegg.HendelseType.DOKUMENTASJONKRAV,
                    "dokumentasjonkrav4",
                    "tittel",
                    "",
                    Oppgavestatus.RELEVANT,
                    null,
                    LocalDateTime.now(),
                    null
                ),
            )
        )
        every { eventService.createModel(any(), any()) } returns model
        every { vedleggService.hentEttersendteVedlegg(any(), any(), any()) } returns emptyList()

        val responseList = service.getDokumentasjonkrav("123", token)

        assertThat(responseList).isNotNull
        assertThat(responseList.size == 3)

        assertThat(responseList[2].dokumentasjonkravElementer).hasSize(2)

        assertThat(responseList[2].frist).isNull()
    }

    @Test
    fun `Should only return Dokumentasjonkrav with the same frist`() {
        val model = InternalDigisosSoker()

        model.dokumentasjonkrav.addAll(
            listOf(
                Dokumentasjonkrav(
                    dokumentasjonkravId,
                    JsonVedlegg.HendelseType.DOKUMENTASJONKRAV,
                    "dokumentasjonkrav1",
                    "tittel",
                    "",
                    Oppgavestatus.RELEVANT,
                    null,
                    LocalDateTime.now(),
                    frist.toLocalDate()
                ),
                Dokumentasjonkrav(
                    dokumentasjonkravId2,
                    JsonVedlegg.HendelseType.DOKUMENTASJONKRAV,
                    "dokumentasjonkrav3",
                    "tittel",
                    "",
                    Oppgavestatus.RELEVANT,
                    null,
                    LocalDateTime.now(),
                    null
                ),
                Dokumentasjonkrav(
                    dokumentasjonkravId,
                    JsonVedlegg.HendelseType.DOKUMENTASJONKRAV,
                    "dokumentasjonkrav2",
                    "tittel",
                    "",
                    Oppgavestatus.RELEVANT,
                    null,
                    LocalDateTime.now(),
                    frist2.toLocalDate()
                ),
                Dokumentasjonkrav(
                    dokumentasjonkravId2,
                    JsonVedlegg.HendelseType.DOKUMENTASJONKRAV,
                    "dokumentasjonkrav4",
                    "tittel",
                    "",
                    Oppgavestatus.RELEVANT,
                    null,
                    LocalDateTime.now(),
                    null
                ),
            )
        )
        every { eventService.createModel(any(), any()) } returns model
        every { vedleggService.hentEttersendteVedlegg(any(), any(), any()) } returns emptyList()

        val responseList = service.getDokumentasjonkravMedId("123", dokumentasjonkravId2, token)

        assertThat(responseList).isNotNull
        assertThat(responseList.size == 1)

        assertThat(responseList[0].dokumentasjonkravElementer).hasSize(2)

        assertThat(responseList[0].frist).isNull()
        assertThat(responseList[0].dokumentasjonkravId).isEqualTo(dokumentasjonkravId2)
    }

    @Test
    fun `should return true if vedlegg for dokumentasjonkrav already uploaded`() {
        val model = InternalDigisosSoker()
        model.dokumentasjonkrav = mutableListOf(
            Dokumentasjonkrav(
                dokumentasjonkravId,
                JsonVedlegg.HendelseType.DOKUMENTASJONKRAV,
                "dokumentasjonkrav1",
                "tittel",
                null,
                Oppgavestatus.RELEVANT,
                null,
                LocalDateTime.now(),
                LocalDate.now()
            )
        )
        every { eventService.createModel(any(), any()) } returns model
        every { vedleggService.hentEttersendteVedlegg(any(), any(), any()) } returns listOf(
            InternalVedlegg(dokumenttype, null, JsonVedlegg.HendelseType.DOKUMENTASJONKRAV, "dokumentasjonkrav1", mutableListOf(dok), tidspunkt, null)
        )

        val response = service.getHarLevertDokumentasjonkrav("123", token)

        assertThat(response).isTrue
    }

    @Test
    fun `should return true if fagsystemversjon equals client properties versjons`() {
        val model = InternalDigisosSoker()
        model.fagsystem = Fagsystem("socio", "10.1.16")
        every { eventService.createModel(any(), any()) } returns model

        var response = service.getFagsystemHarVilkarOgDokumentasjonkrav("123", token)

        assertThat(response).isTrue

        model.fagsystem = Fagsystem("mock-alt", "1.0-MOCKVERSJON")

        response = service.getFagsystemHarVilkarOgDokumentasjonkrav("123", token)

        assertThat(response).isTrue
    }

    @Test
    fun `should return false if fagsystemversjon is older client properties versjons or if fagsystem name is not configured for that version`() {
        val model = InternalDigisosSoker()
        model.fagsystem = Fagsystem("mock-alt", "0.0.3:0")
        every { eventService.createModel(any(), any()) } returns model

        var response = service.getFagsystemHarVilkarOgDokumentasjonkrav("123", token)

        assertThat(response).isFalse

        model.fagsystem = Fagsystem("annet system", "1.0.0:MOCKVERSJON")

        response = service.getFagsystemHarVilkarOgDokumentasjonkrav("123", token)

        assertThat(response).isFalse
    }

    @Test
    fun `should skip incorrect config for fagsystemversjon and still return true when wanted version is correctly configured in list`() {
        val model = InternalDigisosSoker()
        model.fagsystem = Fagsystem("mock-alt", "1.0.1:MOCKVERSJON")
        every { eventService.createModel(any(), any()) } returns model
        every { clientProperties.vilkarDokkravFagsystemVersjoner } returns listOf("ugyldigFormatertFagsystemConfig--0.1.1", "mock-alt;1.0.0:MOCKVERSJON")

        val response = service.getFagsystemHarVilkarOgDokumentasjonkrav("123", token)

        assertThat(response).isTrue
    }

    @Test
    fun `should return true if fagsystemversjon is newer than configured for that system`() {
        val model = InternalDigisosSoker()
        model.fagsystem = Fagsystem("mock-alt", "1.0.0.3")
        every { eventService.createModel(any(), any()) } returns model

        val response = service.getFagsystemHarVilkarOgDokumentasjonkrav("123", token)

        assertThat(response).isTrue
    }

    @Test
    fun `should return false if fagsystemversjon of another fagsystem is newer than configured for another fagsystem`() {
        val model = InternalDigisosSoker()
        model.fagsystem = Fagsystem("socio", "1.2.0:MOCKVERSJON")
        every { eventService.createModel(any(), any()) } returns model

        val response = service.getFagsystemHarVilkarOgDokumentasjonkrav("123", token)

        assertThat(response).isFalse
    }

    @Test
    fun `should return true if soknad har mottat-status og ikke har hatt SENDT-status`() {
        val model = InternalDigisosSoker()
        model.status = SoknadsStatus.MOTTATT
        model.historikk.add(Hendelse(HendelseTekstType.SOKNAD_SEND_TIL_KONTOR, LocalDateTime.now(), null))
        every { eventService.createModel(any(), any()) } returns model

        val sakHarStatusMottattOgIkkeHattSendt = service.sakHarStatusMottattOgIkkeHattSendt("123", token)
        assertThat(sakHarStatusMottattOgIkkeHattSendt).isTrue
    }

    @Test
    fun `should return false if soknad har mottat-status og har hatt SENDT-status`() {
        val model = InternalDigisosSoker()
        model.status = SoknadsStatus.MOTTATT
        model.historikk.add(Hendelse(HendelseTekstType.SOKNAD_VIDERESENDT, LocalDateTime.now(), null))
        model.historikk.add(Hendelse(HendelseTekstType.SOKNAD_SEND_TIL_KONTOR, LocalDateTime.now(), null))
        every { eventService.createModel(any(), any()) } returns model

        val sakHarStatusMottattOgIkkeHattSendt = service.sakHarStatusMottattOgIkkeHattSendt("123", token)
        assertThat(sakHarStatusMottattOgIkkeHattSendt).isFalse
    }
}
