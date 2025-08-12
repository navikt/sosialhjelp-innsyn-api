package no.nav.sosialhjelp.innsyn.event

import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sbl.soknadsosialhjelp.soknad.JsonSoknad
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedlegg
import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.innsyn.app.ClientProperties
import no.nav.sosialhjelp.innsyn.domain.HendelseTekstType
import no.nav.sosialhjelp.innsyn.domain.SoknadsStatus
import no.nav.sosialhjelp.innsyn.navenhet.NavEnhet
import no.nav.sosialhjelp.innsyn.navenhet.NorgClient
import no.nav.sosialhjelp.innsyn.utils.toLocalDateTime
import no.nav.sosialhjelp.innsyn.utils.unixToLocalDateTime
import no.nav.sosialhjelp.innsyn.vedlegg.InternalVedlegg
import no.nav.sosialhjelp.innsyn.vedlegg.VEDLEGG_KREVES_STATUS
import no.nav.sosialhjelp.innsyn.vedlegg.VedleggService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.time.Duration.Companion.seconds

internal class DokumentasjonEtterspurtTest {
    private val clientProperties: ClientProperties = mockk(relaxed = true)
    private val innsynService: InnsynService = mockk()
    private val vedleggService: VedleggService = mockk()
    private val norgClient: NorgClient = mockk()

    private val service = EventService(clientProperties, innsynService, vedleggService, norgClient)

    private val mockDigisosSak: DigisosSak = mockk()
    private val mockJsonSoknad: JsonSoknad = mockk()
    private val mockNavEnhet: NavEnhet = mockk()

    private val soknadsmottaker = "The Office"
    private val enhetsnr = "2317"

    private val vedleggKrevesDokumenttype = "faktura"
    private val vedleggKrevesTilleggsinfo = "strom"

    @BeforeEach
    fun init() {
        clearAllMocks()
        every { mockDigisosSak.fiksDigisosId } returns "123"
        every { mockDigisosSak.digisosSoker?.metadata } returns "some id"
        every { mockDigisosSak.originalSoknadNAV?.metadata } returns "some other id"
        every { mockDigisosSak.originalSoknadNAV?.timestampSendt } returns tidspunkt_soknad
        every { mockDigisosSak.originalSoknadNAV?.navEksternRefId } returns null
        every { mockDigisosSak.originalSoknadNAV?.soknadDokument?.dokumentlagerDokumentId } returns null
        every { mockJsonSoknad.mottaker.navEnhetsnavn } returns soknadsmottaker
        every { mockJsonSoknad.mottaker.enhetsnummer } returns enhetsnr
        every { mockDigisosSak.ettersendtInfoNAV } returns null
        coEvery { innsynService.hentOriginalSoknad(any()) } returns mockJsonSoknad
        coEvery { norgClient.hentNavEnhet(enhetsnr) } returns mockNavEnhet

        resetHendelser()
    }

    @Test
    fun `dokumentliste er satt OG vedtaksbrev er satt - skal gi oppgaver og historikk`() =
        runTest(timeout = 5.seconds) {
            coEvery { innsynService.hentJsonDigisosSoker(any()) } returns
                JsonDigisosSoker()
                    .withAvsender(avsender)
                    .withVersion("123")
                    .withHendelser(
                        listOf(
                            SOKNADS_STATUS_MOTTATT.withHendelsestidspunkt(tidspunkt_1),
                            SOKNADS_STATUS_UNDERBEHANDLING.withHendelsestidspunkt(tidspunkt_2),
                            DOKUMENTASJONETTERSPURT.withHendelsestidspunkt(tidspunkt_3),
                        ),
                    )

            val model = service.createModel(mockDigisosSak)

            assertThat(model).isNotNull
            assertThat(model.status).isEqualTo(SoknadsStatus.UNDER_BEHANDLING)
            assertThat(model.saker).isEmpty()
            assertThat(model.oppgaver).hasSize(1)
            assertThat(model.historikk).hasSize(4)

            val oppgave = model.oppgaver.last()
            assertThat(oppgave.tittel).isEqualTo(DOKUMENTTYPE)
            assertThat(oppgave.tilleggsinfo).isEqualTo(TILLEGGSINFO)
            assertThat(oppgave.innsendelsesfrist).isEqualTo(innsendelsesfrist.toLocalDateTime())
            assertThat(oppgave.erFraInnsyn).isEqualTo(true)

            val hendelse = model.historikk.last()
            assertThat(hendelse.tidspunkt).isEqualTo(tidspunkt_3.toLocalDateTime())
            assertThat(hendelse.hendelseType).isEqualTo(HendelseTekstType.ETTERSPOR_MER_DOKUMENTASJON)
            assertThat(hendelse.url?.link).contains("/dokumentlager/nedlasting/niva4/$DOKUMENTLAGERID_1")
        }

    @Test
    internal fun `dokumentliste er satt OG forvaltningsbrev mangler - skal gi oppgaver men ikke historikk`() =
        runTest(timeout = 5.seconds) {
            coEvery { innsynService.hentJsonDigisosSoker(any()) } returns
                JsonDigisosSoker()
                    .withAvsender(avsender)
                    .withVersion("123")
                    .withHendelser(
                        listOf(
                            SOKNADS_STATUS_MOTTATT.withHendelsestidspunkt(tidspunkt_1),
                            SOKNADS_STATUS_UNDERBEHANDLING.withHendelsestidspunkt(tidspunkt_2),
                            DOKUMENTASJONETTERSPURT_UTEN_FORVALTNINGSBREV.withHendelsestidspunkt(tidspunkt_3),
                        ),
                    )

            val model = service.createModel(mockDigisosSak)

            assertThat(model).isNotNull
            assertThat(model.status).isEqualTo(SoknadsStatus.UNDER_BEHANDLING)
            assertThat(model.saker).isEmpty()
            assertThat(model.oppgaver).hasSize(1)
            assertThat(model.historikk).hasSize(3)

            val oppgave = model.oppgaver.last()
            assertThat(oppgave.tittel).isEqualTo(DOKUMENTTYPE)
            assertThat(oppgave.tilleggsinfo).isEqualTo(TILLEGGSINFO)
            assertThat(oppgave.innsendelsesfrist).isEqualTo(innsendelsesfrist.toLocalDateTime())
            assertThat(oppgave.erFraInnsyn).isEqualTo(true)
        }

    @Test
    fun `dokumentliste er tom OG forvaltningsbrev er satt - skal verken gi oppgaver eller historikk`() =
        runTest(timeout = 5.seconds) {
            coEvery { innsynService.hentJsonDigisosSoker(any()) } returns
                JsonDigisosSoker()
                    .withAvsender(avsender)
                    .withVersion("123")
                    .withHendelser(
                        listOf(
                            SOKNADS_STATUS_MOTTATT.withHendelsestidspunkt(tidspunkt_1),
                            SOKNADS_STATUS_UNDERBEHANDLING.withHendelsestidspunkt(tidspunkt_2),
                            DOKUMENTASJONETTERSPURT_TOM_DOKUMENT_LISTE.withHendelsestidspunkt(tidspunkt_3),
                        ),
                    )

            val model = service.createModel(mockDigisosSak)

            assertThat(model).isNotNull
            assertThat(model.status).isEqualTo(SoknadsStatus.UNDER_BEHANDLING)
            assertThat(model.saker).isEmpty()
            assertThat(model.oppgaver).hasSize(0)
            assertThat(model.historikk).hasSize(3)
        }

    @Test
    fun `oppgaver skal hentes fra soknaden dersom det ikke finnes dokumentasjonEtterspurt`() =
        runTest(timeout = 5.seconds) {
            coEvery { innsynService.hentJsonDigisosSoker(any()) } returns
                JsonDigisosSoker()
                    .withAvsender(avsender)
                    .withVersion("123")
                    .withHendelser(
                        listOf(
                            SOKNADS_STATUS_MOTTATT.withHendelsestidspunkt(tidspunkt_1),
                            SOKNADS_STATUS_UNDERBEHANDLING.withHendelsestidspunkt(tidspunkt_2),
                        ),
                    )
            coEvery { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any()) } returns
                listOf(
                    InternalVedlegg(
                        vedleggKrevesDokumenttype,
                        vedleggKrevesTilleggsinfo,
                        null,
                        null,
                        mutableListOf(),
                        unixToLocalDateTime(tidspunkt_soknad),
                        null,
                    ),
                )

            val model = service.createModel(mockDigisosSak)

            assertThat(model).isNotNull
            assertThat(model.status).isEqualTo(SoknadsStatus.UNDER_BEHANDLING)
            assertThat(model.saker).isEmpty()
            assertThat(model.oppgaver).hasSize(1)
            assertThat(model.historikk).hasSize(3)

            val oppgave = model.oppgaver.last()
            assertThat(oppgave.tittel).isEqualTo(vedleggKrevesDokumenttype)
            assertThat(oppgave.tilleggsinfo).isEqualTo(vedleggKrevesTilleggsinfo)
            assertThat(oppgave.innsendelsesfrist).isNull()
            assertThat(oppgave.erFraInnsyn).isEqualTo(false)

            val hendelse = model.historikk.last()
            assertThat(hendelse.tidspunkt).isEqualTo(tidspunkt_2.toLocalDateTime())
            assertThat(hendelse.hendelseType).isEqualTo(HendelseTekstType.SOKNAD_UNDER_BEHANDLING)
            assertThat(hendelse.url).isNull()
        }

    @Test
    fun `oppgaver som hentes fra soknad skal ha hendelseType og HendelseReferanse om de er satt i soknaden`() =
        runTest(timeout = 5.seconds) {
            val hendelseReferanse = "1234"
            coEvery { innsynService.hentJsonDigisosSoker(any()) } returns null
            coEvery { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any()) } returns
                listOf(
                    InternalVedlegg(
                        vedleggKrevesDokumenttype,
                        vedleggKrevesTilleggsinfo,
                        JsonVedlegg.HendelseType.SOKNAD,
                        hendelseReferanse,
                        mutableListOf(),
                        unixToLocalDateTime(tidspunkt_soknad),
                        null,
                    ),
                )

            val model = service.createModel(mockDigisosSak)

            val oppgave = model.oppgaver.last()
            assertThat(oppgave.tilleggsinfo).isEqualTo(vedleggKrevesTilleggsinfo)
            assertThat(oppgave.hendelsetype).isEqualTo(JsonVedlegg.HendelseType.SOKNAD)
            assertThat(oppgave.hendelsereferanse).isEqualTo(hendelseReferanse)
        }

    @Test
    fun `oppgaver som hentes fra soknad skal ha hendelseType men ikke hendelsereferanse om det ikke er satt i soknaden`() =
        runTest(timeout = 5.seconds) {
            coEvery { innsynService.hentJsonDigisosSoker(any()) } returns null
            coEvery { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any()) } returns
                listOf(
                    InternalVedlegg(
                        vedleggKrevesDokumenttype,
                        vedleggKrevesTilleggsinfo,
                        null,
                        null,
                        mutableListOf(),
                        unixToLocalDateTime(tidspunkt_soknad),
                        null,
                    ),
                )

            val model = service.createModel(mockDigisosSak)

            val oppgave = model.oppgaver.last()
            assertThat(oppgave.hendelsetype).isEqualTo(JsonVedlegg.HendelseType.SOKNAD)
            assertThat(oppgave.hendelsereferanse).isNull()
        }

    @Test
    internal fun `dokumentasjonEtterspurt overstyrer gjenstaende vedleggskrav fra soknad`() =
        runTest(timeout = 5.seconds) {
            coEvery { innsynService.hentJsonDigisosSoker(any()) } returns
                JsonDigisosSoker()
                    .withAvsender(avsender)
                    .withVersion("123")
                    .withHendelser(
                        listOf(
                            SOKNADS_STATUS_MOTTATT.withHendelsestidspunkt(tidspunkt_1),
                            SOKNADS_STATUS_UNDERBEHANDLING.withHendelsestidspunkt(tidspunkt_2),
                            DOKUMENTASJONETTERSPURT.withHendelsestidspunkt(tidspunkt_3),
                        ),
                    )
            coEvery { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any()) } returns
                listOf(
                    InternalVedlegg(
                        vedleggKrevesDokumenttype,
                        vedleggKrevesTilleggsinfo,
                        null,
                        null,
                        mutableListOf(),
                        unixToLocalDateTime(tidspunkt_soknad),
                        null,
                    ),
                )

            val model = service.createModel(mockDigisosSak)

            assertThat(model).isNotNull
            assertThat(model.status).isEqualTo(SoknadsStatus.UNDER_BEHANDLING)
            assertThat(model.saker).isEmpty()
            assertThat(model.oppgaver).hasSize(1)
            assertThat(model.historikk).hasSize(4)

            val oppgave = model.oppgaver.last()
            assertThat(oppgave.tittel).isEqualTo(DOKUMENTTYPE)
            assertThat(oppgave.tilleggsinfo).isEqualTo(TILLEGGSINFO)
            assertThat(oppgave.innsendelsesfrist).isEqualTo(innsendelsesfrist.toLocalDateTime())
            assertThat(oppgave.erFraInnsyn).isEqualTo(true)
        }

    @Test
    internal fun `ny dokumentasjonEtterspurt uten oppgaver skal overstyre og gi hendelse i historikk`() =
        runTest(timeout = 5.seconds) {
            coEvery { innsynService.hentJsonDigisosSoker(any()) } returns
                JsonDigisosSoker()
                    .withAvsender(avsender)
                    .withVersion("123")
                    .withHendelser(
                        listOf(
                            SOKNADS_STATUS_MOTTATT.withHendelsestidspunkt(tidspunkt_1),
                            SOKNADS_STATUS_UNDERBEHANDLING.withHendelsestidspunkt(tidspunkt_2),
                            DOKUMENTASJONETTERSPURT.withHendelsestidspunkt(tidspunkt_3),
                            DOKUMENTASJONETTERSPURT_TOM_DOKUMENT_LISTE.withHendelsestidspunkt(tidspunkt_4),
                        ),
                    )
            coEvery { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any()) } returns
                listOf(
                    InternalVedlegg(
                        vedleggKrevesDokumenttype,
                        vedleggKrevesTilleggsinfo,
                        null,
                        null,
                        mutableListOf(),
                        unixToLocalDateTime(tidspunkt_soknad),
                        null,
                    ),
                )

            val model = service.createModel(mockDigisosSak)

            assertThat(model).isNotNull
            assertThat(model.status).isEqualTo(SoknadsStatus.UNDER_BEHANDLING)
            assertThat(model.saker).isEmpty()
            assertThat(model.oppgaver).isEmpty()
            assertThat(model.historikk).hasSize(5)

            val hendelse = model.historikk.last()
            assertThat(hendelse.hendelseType).isEqualTo(HendelseTekstType.ETTERSPOR_IKKE_MER_DOKUMENTASJON)
            assertThat(hendelse.tidspunkt).isEqualTo(tidspunkt_4.toLocalDateTime())
        }

    @Test
    internal fun `ny dokumentasjonEtterspurt uten oppgaver skal ikke gi hendelse i historikk ved soknadstatus ferdigbehandlet`() =
        runTest(timeout = 5.seconds) {
            coEvery { innsynService.hentJsonDigisosSoker(any()) } returns
                JsonDigisosSoker()
                    .withAvsender(avsender)
                    .withVersion("123")
                    .withHendelser(
                        listOf(
                            SOKNADS_STATUS_MOTTATT.withHendelsestidspunkt(tidspunkt_1),
                            SOKNADS_STATUS_FERDIGBEHANDLET.withHendelsestidspunkt(tidspunkt_2),
                            DOKUMENTASJONETTERSPURT.withHendelsestidspunkt(tidspunkt_3),
                            DOKUMENTASJONETTERSPURT_TOM_DOKUMENT_LISTE.withHendelsestidspunkt(tidspunkt_4),
                        ),
                    )
            coEvery { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any()) } returns
                listOf(
                    InternalVedlegg(
                        vedleggKrevesDokumenttype,
                        vedleggKrevesTilleggsinfo,
                        null,
                        null,
                        mutableListOf(),
                        unixToLocalDateTime(tidspunkt_soknad),
                        null,
                    ),
                )

            val model = service.createModel(mockDigisosSak)

            assertThat(model).isNotNull
            assertThat(model.status).isEqualTo(SoknadsStatus.FERDIGBEHANDLET)
            assertThat(model.saker).isEmpty()
            assertThat(model.oppgaver).isEmpty()
            assertThat(model.historikk).hasSize(4)

            val hendelse = model.historikk.last()
            assertThat(hendelse.tidspunkt).isEqualTo(tidspunkt_3.toLocalDateTime())
        }

    @Test
    internal fun `ny dokumentasjonEtterspurt uten oppgaver skal ikke gi hendelse i historikk ved soknadstatus behandles_ikke`() =
        runTest(timeout = 5.seconds) {
            coEvery { innsynService.hentJsonDigisosSoker(any()) } returns
                JsonDigisosSoker()
                    .withAvsender(avsender)
                    .withVersion("123")
                    .withHendelser(
                        listOf(
                            SOKNADS_STATUS_MOTTATT.withHendelsestidspunkt(tidspunkt_1),
                            SOKNADS_STATUS_BEHANDLES_IKKE.withHendelsestidspunkt(tidspunkt_2),
                            DOKUMENTASJONETTERSPURT.withHendelsestidspunkt(tidspunkt_3),
                            DOKUMENTASJONETTERSPURT_TOM_DOKUMENT_LISTE.withHendelsestidspunkt(tidspunkt_4),
                        ),
                    )
            coEvery { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any()) } returns
                listOf(
                    InternalVedlegg(
                        vedleggKrevesDokumenttype,
                        vedleggKrevesTilleggsinfo,
                        null,
                        null,
                        mutableListOf(),
                        unixToLocalDateTime(tidspunkt_soknad),
                        null,
                    ),
                )

            val model = service.createModel(mockDigisosSak)

            assertThat(model).isNotNull
            assertThat(model.status).isEqualTo(SoknadsStatus.BEHANDLES_IKKE)
            assertThat(model.saker).isEmpty()
            assertThat(model.oppgaver).isEmpty()
            assertThat(model.historikk).hasSize(4)

            val hendelse = model.historikk.last()
            assertThat(hendelse.tidspunkt).isEqualTo(tidspunkt_3.toLocalDateTime())
        }

    @Test
    internal fun `skal vise oppgaver fra dokumentasjonEtterspurt selv om soknad er over 30 dager gammel`() =
        runTest(timeout = 5.seconds) {
            val nowMinus31Days = ZonedDateTime.now().minusDays(31)
            val tidspunktSendt31dagerSiden = nowMinus31Days.toEpochSecond() * 1000L
            val tidspunktMottatt = nowMinus31Days.minusMinutes(5).format(DateTimeFormatter.ISO_DATE_TIME)
            val tidspunktUnderBehandling = nowMinus31Days.minusMinutes(4).format(DateTimeFormatter.ISO_DATE_TIME)
            val tidspunktDokumentasjonEtterspurt = nowMinus31Days.minusMinutes(3).format(DateTimeFormatter.ISO_DATE_TIME)

            every { mockDigisosSak.originalSoknadNAV?.timestampSendt } returns tidspunktSendt31dagerSiden
            coEvery { innsynService.hentJsonDigisosSoker(any()) } returns
                JsonDigisosSoker()
                    .withAvsender(avsender)
                    .withVersion("123")
                    .withHendelser(
                        listOf(
                            SOKNADS_STATUS_MOTTATT.withHendelsestidspunkt(tidspunktMottatt),
                            SOKNADS_STATUS_UNDERBEHANDLING.withHendelsestidspunkt(tidspunktUnderBehandling),
                            DOKUMENTASJONETTERSPURT.withHendelsestidspunkt(tidspunktDokumentasjonEtterspurt),
                        ),
                    )

            val model = service.createModel(mockDigisosSak)

            assertThat(model).isNotNull
            assertThat(model.oppgaver).hasSize(1)

            val oppgave = model.oppgaver.last()
            assertThat(oppgave.tittel).isEqualTo(DOKUMENTTYPE)
            assertThat(oppgave.tilleggsinfo).isEqualTo(TILLEGGSINFO)
            assertThat(oppgave.innsendelsesfrist).isEqualTo(innsendelsesfrist.toLocalDateTime())
            assertThat(oppgave.erFraInnsyn).isEqualTo(true)
        }
}
