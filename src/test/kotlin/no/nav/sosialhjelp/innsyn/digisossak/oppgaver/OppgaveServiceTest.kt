package no.nav.sosialhjelp.innsyn.digisossak.oppgaver

import io.micrometer.core.instrument.MeterRegistry
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedlegg
import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.api.fiks.DokumentInfo
import no.nav.sosialhjelp.api.fiks.EttersendtInfoNAV
import no.nav.sosialhjelp.innsyn.app.ClientProperties
import no.nav.sosialhjelp.innsyn.digisosapi.FiksService
import no.nav.sosialhjelp.innsyn.domain.Dokumentasjonkrav
import no.nav.sosialhjelp.innsyn.domain.Fagsystem
import no.nav.sosialhjelp.innsyn.domain.Hendelse
import no.nav.sosialhjelp.innsyn.domain.HendelseTekstType
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
import kotlin.time.Duration.Companion.seconds

internal class OppgaveServiceTest {
    private val eventService: EventService = mockk()
    private val vedleggService: VedleggService = mockk()
    private val fiksService: FiksService = mockk()
    private val clientProperties: ClientProperties = mockk()
    private val meterRegistry: MeterRegistry = mockk(relaxed = true)
    private val service = OppgaveService(eventService, vedleggService, fiksService, clientProperties, meterRegistry)

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

    @BeforeEach
    fun init() {
        clearAllMocks()
        coEvery { fiksService.getSoknad(any()) } returns mockDigisosSak
        every { mockDigisosSak.ettersendtInfoNAV } returns mockEttersendtInfoNAV
        every { clientProperties.vilkarDokkravFagsystemVersjoner } returns listOf("socio;10.1.16", "mock-alt;1.0.0")
    }

    @Test
    fun `Should return vilkar with tittel`() =
        runTest(timeout = 5.seconds) {
            val model = InternalDigisosSoker()
            val tittel = "VILKAR1"
            val beskrivelse = "mer vilkarer2"
            model.vilkar.addAll(
                listOf(
                    Vilkar(
                        "vilkar1",
                        tittel,
                        "mer vilkarer1",
                        Oppgavestatus.RELEVANT,
                        null,
                        LocalDateTime.now(),
                        LocalDateTime.now(),
                        null,
                    ),
                    Vilkar("vilkar2", null, beskrivelse, Oppgavestatus.RELEVANT, null, LocalDateTime.now(), LocalDateTime.now(), null),
                    Vilkar("vilkar3", "", null, Oppgavestatus.RELEVANT, null, LocalDateTime.now(), LocalDateTime.now(), null),
                ),
            )
            coEvery { eventService.createModel(any()) } returns model

            val responseList = service.getVilkar("123")

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
    fun `Should not return Vilkar with status ANNULLERT`() =
        runTest(timeout = 5.seconds) {
            val model = InternalDigisosSoker()
            model.vilkar.addAll(
                listOf(
                    Vilkar("vilkar1", "tittel", null, Oppgavestatus.ANNULLERT, null, LocalDateTime.now(), LocalDateTime.now(), null),
                    Vilkar("vilkar2", "tittel", null, Oppgavestatus.RELEVANT, null, LocalDateTime.now(), LocalDateTime.now(), null),
                ),
            )
            coEvery { eventService.createModel(any()) } returns model

            val responseList = service.getVilkar("123")

            assertThat(responseList).isNotNull
            assertThat(responseList.size == 1)
            assertThat(responseList).hasSize(1)
            assertThat(responseList[0].status).isEqualTo(Oppgavestatus.RELEVANT)
        }

    @Test
    fun `Should not return Dokumentasjonkrav with status ANNULERT or LEVERT_TIDLIGERE`() =
        runTest(timeout = 5.seconds) {
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
                        LocalDate.now(),
                        null,
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
                        LocalDate.now(),
                        null,
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
                        LocalDate.now(),
                        null,
                    ),
                ),
            )
            coEvery { eventService.createModel(any()) } returns model
            coEvery { vedleggService.hentEttersendteVedlegg(any(), any<InternalDigisosSoker>()) } returns emptyList()

            val responseList = service.getDokumentasjonkrav("123")

            assertThat(responseList).isNotNull
            assertThat(responseList.size == 1)
            assertThat(responseList[0].dokumentasjonkravElementer).hasSize(1)
            assertThat(responseList[0].dokumentasjonkravElementer[0].status).isEqualTo(Oppgavestatus.RELEVANT)
        }

    @Test
    fun `Should return dokumentasjonkrav with tittel and filter out empty dokumentasjonkrav`() =
        runTest(timeout = 5.seconds) {
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
                        LocalDate.now(),
                        null,
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
                        LocalDate.now(),
                        null,
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
                        LocalDate.now(),
                        null,
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
                        LocalDate.now(),
                        null,
                    ),
                ),
            )
            coEvery { eventService.createModel(any()) } returns model
            coEvery { vedleggService.hentEttersendteVedlegg(any(), any<InternalDigisosSoker>()) } returns emptyList()

            val responseList = service.getDokumentasjonkrav("123")

            assertThat(responseList).isNotNull
            assertThat(responseList.size == 1)
            assertThat(responseList[0].dokumentasjonkravElementer).hasSize(2)
            assertThat(responseList[0].dokumentasjonkravElementer[0].tittel).isNotNull
            assertThat(responseList[0].dokumentasjonkravElementer[1].beskrivelse).isNull()
            assertThat(responseList[0].dokumentasjonkravElementer[1].tittel).isNotNull
            assertThat(responseList[0].dokumentasjonkravElementer[1].dokumentasjonkravReferanse).isEqualTo("dokumentasjonkrav2")
        }

    @Test
    fun `Should not include statuses OPPFYLT or IKKE_OPPFYLT for Dokumentasjonkrav`() =
        runTest(timeout = 5.seconds) {
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
                        LocalDate.now(),
                        null,
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
                        null,
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
                        null,
                    ),
                ),
            )
            coEvery { eventService.createModel(any()) } returns model
            coEvery { vedleggService.hentEttersendteVedlegg(any(), any<InternalDigisosSoker>()) } returns emptyList()

            val responseList = service.getDokumentasjonkrav("123")

            assertThat(responseList).isNotNull
            assertThat(responseList.size == 1)
            assertThat(responseList[0].dokumentasjonkravElementer).hasSize(1)
            assertThat(responseList[0].dokumentasjonkravElementer[0].status).isEqualTo(Oppgavestatus.RELEVANT)
        }

    @Test
    fun `Should not include statuses OPPFYLT or IKKE_OPPFYLT for Vilkar`() =
        runTest(timeout = 5.seconds) {
            val model = InternalDigisosSoker()
            model.vilkar.addAll(
                listOf(
                    Vilkar("vilkar1", "tittel", null, Oppgavestatus.OPPFYLT, null, LocalDateTime.now(), LocalDateTime.now(), null),
                    Vilkar("vilkar2", "tittel", null, Oppgavestatus.IKKE_OPPFYLT, null, LocalDateTime.now(), LocalDateTime.now(), null),
                    Vilkar("vilkar3", "tittel", null, Oppgavestatus.RELEVANT, null, LocalDateTime.now(), LocalDateTime.now(), null),
                ),
            )
            coEvery { eventService.createModel(any()) } returns model
            coEvery { vedleggService.hentEttersendteVedlegg(any(), any<InternalDigisosSoker>()) } returns emptyList()

            val responseList = service.getVilkar("123")

            assertThat(responseList).isNotNull
            assertThat(responseList.size == 1)
            assertThat(responseList).hasSize(1)
            assertThat(responseList[0].status).isEqualTo(Oppgavestatus.RELEVANT)
        }

    @Test
    fun `Should sort Dokumentasjonkrav by date and group by date`() =
        runTest(timeout = 5.seconds) {
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
                        frist2.toLocalDate(),
                        null,
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
                        frist2.toLocalDate(),
                        null,
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
                        frist.toLocalDate(),
                        null,
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
                        frist3.toLocalDate(),
                        null,
                    ),
                ),
            )
            coEvery { eventService.createModel(any()) } returns model
            coEvery { vedleggService.hentEttersendteVedlegg(any(), any<InternalDigisosSoker>()) } returns emptyList()

            val responseList = service.getDokumentasjonkrav("123")

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
    fun `Should group Dokumentasjonkrav without frist together`() =
        runTest(timeout = 5.seconds) {
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
                        frist.toLocalDate(),
                        null,
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
                        null,
                        null,
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
                        frist2.toLocalDate(),
                        null,
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
                        null,
                        null,
                    ),
                ),
            )
            coEvery { eventService.createModel(any()) } returns model
            coEvery { vedleggService.hentEttersendteVedlegg(any(), any<InternalDigisosSoker>()) } returns emptyList()

            val responseList = service.getDokumentasjonkrav("123")

            assertThat(responseList).isNotNull
            assertThat(responseList.size == 3)

            assertThat(responseList[2].dokumentasjonkravElementer).hasSize(2)

            assertThat(responseList[2].frist).isNull()
        }

    @Test
    fun `should return true if vedlegg for dokumentasjonkrav already uploaded`() =
        runTest(timeout = 5.seconds) {
            val model = InternalDigisosSoker()
            model.dokumentasjonkrav =
                mutableListOf(
                    Dokumentasjonkrav(
                        dokumentasjonkravId,
                        JsonVedlegg.HendelseType.DOKUMENTASJONKRAV,
                        "dokumentasjonkrav1",
                        "tittel",
                        null,
                        Oppgavestatus.RELEVANT,
                        null,
                        LocalDateTime.now(),
                        LocalDate.now(),
                        null,
                    ),
                )
            coEvery { eventService.createModel(any()) } returns model
            coEvery { vedleggService.hentEttersendteVedlegg(any(), any<InternalDigisosSoker>()) } returns
                listOf(
                    InternalVedlegg(
                        dokumenttype,
                        null,
                        JsonVedlegg.HendelseType.DOKUMENTASJONKRAV,
                        "dokumentasjonkrav1",
                        mutableListOf(dok),
                        tidspunkt,
                        null,
                    ),
                )

            val response = service.getHarLevertDokumentasjonkrav("123")

            assertThat(response).isTrue
        }

    @Test
    fun `should return true if fagsystemversjon equals client properties versjons`() =
        runTest(timeout = 5.seconds) {
            val model = InternalDigisosSoker()
            model.fagsystem = Fagsystem("socio", "10.1.16")
            coEvery { eventService.createModel(any()) } returns model

            var response = service.getFagsystemHarVilkarOgDokumentasjonkrav("123")

            assertThat(response).isTrue

            model.fagsystem = Fagsystem("mock-alt", "1.0-MOCKVERSJON")

            response = service.getFagsystemHarVilkarOgDokumentasjonkrav("123")

            assertThat(response).isTrue
        }

    @Test
    @Suppress("ktlint:standard:max-line-length")
    fun `should return false if fagsystemversjon is older client properties versjons or if fagsystem name is not configured for that version`() =
        runTest(timeout = 5.seconds) {
            val model = InternalDigisosSoker()
            model.fagsystem = Fagsystem("mock-alt", "0.0.3:0")
            coEvery { eventService.createModel(any()) } returns model

            var response = service.getFagsystemHarVilkarOgDokumentasjonkrav("123")

            assertThat(response).isFalse

            model.fagsystem = Fagsystem("annet system", "1.0.0:MOCKVERSJON")

            response = service.getFagsystemHarVilkarOgDokumentasjonkrav("123")

            assertThat(response).isFalse
        }

    @Test
    fun `should skip incorrect config for fagsystemversjon and still return true when wanted version is correctly configured in list`() =
        runTest(timeout = 5.seconds) {
            val model = InternalDigisosSoker()
            model.fagsystem = Fagsystem("mock-alt", "1.0.1:MOCKVERSJON")
            coEvery { eventService.createModel(any()) } returns model
            every {
                clientProperties.vilkarDokkravFagsystemVersjoner
            } returns listOf("ugyldigFormatertFagsystemConfig--0.1.1", "mock-alt;1.0.0:MOCKVERSJON")

            val response = service.getFagsystemHarVilkarOgDokumentasjonkrav("123")

            assertThat(response).isTrue
        }

    @Test
    fun `should return true if fagsystemversjon is newer than configured for that system`() =
        runTest(timeout = 5.seconds) {
            val model = InternalDigisosSoker()
            model.fagsystem = Fagsystem("mock-alt", "1.0.1")
            coEvery { eventService.createModel(any()) } returns model

            val response = service.getFagsystemHarVilkarOgDokumentasjonkrav("123")

            assertThat(response).isTrue
        }

    @Test
    fun `should return false if fagsystemversjon of another fagsystem is newer than configured for another fagsystem`() =
        runTest(timeout = 5.seconds) {
            val model = InternalDigisosSoker()
            model.fagsystem = Fagsystem("socio", "1.2.0:MOCKVERSJON")
            coEvery { eventService.createModel(any()) } returns model

            val response = service.getFagsystemHarVilkarOgDokumentasjonkrav("123")

            assertThat(response).isFalse
        }

    @Test
    fun `should return true if soknad har mottat-status og ikke har hatt SENDT-status`() =
        runTest(timeout = 5.seconds) {
            val model = InternalDigisosSoker()
            model.status = SoknadsStatus.MOTTATT
            model.historikk.add(Hendelse(HendelseTekstType.SOKNAD_UNDER_BEHANDLING, LocalDateTime.now(), null))
            coEvery { eventService.createModel(any()) } returns model

            val sakHarStatusMottattOgIkkeHattSendt = service.sakHarStatusMottattOgIkkeHattSendt("123")
            assertThat(sakHarStatusMottattOgIkkeHattSendt).isTrue
        }

    @Test
    fun `should return false if soknad har mottat-status og har hatt SENDT-status`() =
        runTest(timeout = 5.seconds) {
            val model = InternalDigisosSoker()
            model.status = SoknadsStatus.MOTTATT
            model.historikk.add(Hendelse(HendelseTekstType.SOKNAD_VIDERESENDT_MED_NORG_ENHET, LocalDateTime.now(), null))
            model.historikk.add(Hendelse(HendelseTekstType.SOKNAD_SEND_TIL_KONTOR, LocalDateTime.now(), null))
            coEvery { eventService.createModel(any()) } returns model

            val sakHarStatusMottattOgIkkeHattSendt = service.sakHarStatusMottattOgIkkeHattSendt("123")
            assertThat(sakHarStatusMottattOgIkkeHattSendt).isFalse
        }
}
