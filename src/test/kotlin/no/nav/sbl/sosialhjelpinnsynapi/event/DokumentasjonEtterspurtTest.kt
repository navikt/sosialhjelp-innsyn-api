package no.nav.sbl.sosialhjelpinnsynapi.event

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import no.finn.unleash.Unleash
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sbl.soknadsosialhjelp.soknad.JsonSoknad
import no.nav.sbl.sosialhjelpinnsynapi.client.norg.NorgClient
import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.domain.NavEnhet
import no.nav.sbl.sosialhjelpinnsynapi.domain.SoknadsStatus
import no.nav.sbl.sosialhjelpinnsynapi.service.innsyn.InnsynService
import no.nav.sbl.sosialhjelpinnsynapi.service.vedlegg.InternalVedlegg
import no.nav.sbl.sosialhjelpinnsynapi.service.vedlegg.VEDLEGG_KREVES_STATUS
import no.nav.sbl.sosialhjelpinnsynapi.service.vedlegg.VedleggService
import no.nav.sbl.sosialhjelpinnsynapi.utils.toLocalDateTime
import no.nav.sbl.sosialhjelpinnsynapi.utils.unixToLocalDateTime
import no.nav.sosialhjelp.api.fiks.DigisosSak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

internal class DokumentasjonEtterspurtTest {

    private val clientProperties: ClientProperties = mockk(relaxed = true)
    private val innsynService: InnsynService = mockk()
    private val vedleggService: VedleggService = mockk()
    private val norgClient: NorgClient = mockk()
    private val unleashClient: Unleash = mockk()

    private val service = EventService(clientProperties, innsynService, vedleggService, norgClient, unleashClient)

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
        every { innsynService.hentOriginalSoknad(any(), any(), any()) } returns mockJsonSoknad
        every { norgClient.hentNavEnhet(enhetsnr) } returns mockNavEnhet

        resetHendelser()
    }

    @Test
    fun `dokumentliste er satt OG vedtaksbrev er satt - skal gi oppgaver og historikk`() {
        every { innsynService.hentJsonDigisosSoker(any(), any(), any()) } returns
                JsonDigisosSoker()
                        .withAvsender(avsender)
                        .withVersion("123")
                        .withHendelser(listOf(
                                SOKNADS_STATUS_MOTTATT.withHendelsestidspunkt(tidspunkt_1),
                                SOKNADS_STATUS_UNDERBEHANDLING.withHendelsestidspunkt(tidspunkt_2),
                                DOKUMENTASJONETTERSPURT.withHendelsestidspunkt(tidspunkt_3)
                        ))

        val model = service.createModel(mockDigisosSak, "token")

        assertThat(model).isNotNull
        assertThat(model.status).isEqualTo(SoknadsStatus.UNDER_BEHANDLING)
        assertThat(model.saker).isEmpty()
        assertThat(model.oppgaver).hasSize(1)
        assertThat(model.historikk).hasSize(4)

        val oppgave = model.oppgaver.last()
        assertThat(oppgave.tittel).isEqualTo(dokumenttype)
        assertThat(oppgave.tilleggsinfo).isEqualTo(tilleggsinfo)
        assertThat(oppgave.innsendelsesfrist).isEqualTo(innsendelsesfrist.toLocalDateTime())
        assertThat(oppgave.erFraInnsyn).isEqualTo(true)

        val hendelse = model.historikk.last()
        assertThat(hendelse.tidspunkt).isEqualTo(tidspunkt_3.toLocalDateTime())
        assertThat(hendelse.tittel).contains("Du må sende dokumentasjon")
        assertThat(hendelse.url?.link).contains("/dokumentlager/nedlasting/niva4/$dokumentlagerId_1")
    }

    @Test
    internal fun `dokumentliste er satt OG forvaltningsbrev mangler - skal gi oppgaver men ikke historikk`() {
        every { innsynService.hentJsonDigisosSoker(any(), any(), any()) } returns
                JsonDigisosSoker()
                        .withAvsender(avsender)
                        .withVersion("123")
                        .withHendelser(listOf(
                                SOKNADS_STATUS_MOTTATT.withHendelsestidspunkt(tidspunkt_1),
                                SOKNADS_STATUS_UNDERBEHANDLING.withHendelsestidspunkt(tidspunkt_2),
                                DOKUMENTASJONETTERSPURT_UTEN_FORVALTNINGSBREV.withHendelsestidspunkt(tidspunkt_3)
                        ))

        val model = service.createModel(mockDigisosSak, "token")

        assertThat(model).isNotNull
        assertThat(model.status).isEqualTo(SoknadsStatus.UNDER_BEHANDLING)
        assertThat(model.saker).isEmpty()
        assertThat(model.oppgaver).hasSize(1)
        assertThat(model.historikk).hasSize(3)

        val oppgave = model.oppgaver.last()
        assertThat(oppgave.tittel).isEqualTo(dokumenttype)
        assertThat(oppgave.tilleggsinfo).isEqualTo(tilleggsinfo)
        assertThat(oppgave.innsendelsesfrist).isEqualTo(innsendelsesfrist.toLocalDateTime())
        assertThat(oppgave.erFraInnsyn).isEqualTo(true)
    }

    @Test
    fun `dokumentliste er tom OG forvaltningsbrev er satt - skal verken gi oppgaver eller historikk`() {
        every { innsynService.hentJsonDigisosSoker(any(), any(), any()) } returns
                JsonDigisosSoker()
                        .withAvsender(avsender)
                        .withVersion("123")
                        .withHendelser(listOf(
                                SOKNADS_STATUS_MOTTATT.withHendelsestidspunkt(tidspunkt_1),
                                SOKNADS_STATUS_UNDERBEHANDLING.withHendelsestidspunkt(tidspunkt_2),
                                DOKUMENTASJONETTERSPURT_TOM_DOKUMENT_LISTE.withHendelsestidspunkt(tidspunkt_3)
                        ))

        val model = service.createModel(mockDigisosSak, "token")

        assertThat(model).isNotNull
        assertThat(model.status).isEqualTo(SoknadsStatus.UNDER_BEHANDLING)
        assertThat(model.saker).isEmpty()
        assertThat(model.oppgaver).hasSize(0)
        assertThat(model.historikk).hasSize(3)
    }

    @Test
    fun `oppgaver skal hentes fra soknaden dersom det ikke finnes dokumentasjonEtterspurt`() {
        every { innsynService.hentJsonDigisosSoker(any(), any(), any()) } returns
                JsonDigisosSoker()
                        .withAvsender(avsender)
                        .withVersion("123")
                        .withHendelser(listOf(
                                SOKNADS_STATUS_MOTTATT.withHendelsestidspunkt(tidspunkt_1),
                                SOKNADS_STATUS_UNDERBEHANDLING.withHendelsestidspunkt(tidspunkt_2)
                        ))
        every { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any(), any(), any()) } returns
                listOf(InternalVedlegg(vedleggKrevesDokumenttype, vedleggKrevesTilleggsinfo, emptyList(), unixToLocalDateTime(tidspunkt_soknad)))

        val model = service.createModel(mockDigisosSak, "token")

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
        assertThat(hendelse.tittel).contains("Søknaden er under behandling")
        assertThat(hendelse.url).isNull()
    }

    @Test
    internal fun `dokumentasjonEtterspurt overstyrer gjenstaende vedleggskrav fra soknad`() {
        every { innsynService.hentJsonDigisosSoker(any(), any(), any()) } returns
                JsonDigisosSoker()
                        .withAvsender(avsender)
                        .withVersion("123")
                        .withHendelser(listOf(
                                SOKNADS_STATUS_MOTTATT.withHendelsestidspunkt(tidspunkt_1),
                                SOKNADS_STATUS_UNDERBEHANDLING.withHendelsestidspunkt(tidspunkt_2),
                                DOKUMENTASJONETTERSPURT.withHendelsestidspunkt(tidspunkt_3)
                        ))
        every { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any(), any(), any()) } returns
                listOf(InternalVedlegg(vedleggKrevesDokumenttype, vedleggKrevesTilleggsinfo, emptyList(), unixToLocalDateTime(tidspunkt_soknad)))

        val model = service.createModel(mockDigisosSak, "token")

        assertThat(model).isNotNull
        assertThat(model.status).isEqualTo(SoknadsStatus.UNDER_BEHANDLING)
        assertThat(model.saker).isEmpty()
        assertThat(model.oppgaver).hasSize(1)
        assertThat(model.historikk).hasSize(4)

        val oppgave = model.oppgaver.last()
        assertThat(oppgave.tittel).isEqualTo(dokumenttype)
        assertThat(oppgave.tilleggsinfo).isEqualTo(tilleggsinfo)
        assertThat(oppgave.innsendelsesfrist).isEqualTo(innsendelsesfrist.toLocalDateTime())
        assertThat(oppgave.erFraInnsyn).isEqualTo(true)
    }

    @Test
    internal fun `ny dokumentasjonEtterspurt uten oppgaver skal overstyre og gi hendelse i historikk`() {
        every { innsynService.hentJsonDigisosSoker(any(), any(), any()) } returns
                JsonDigisosSoker()
                        .withAvsender(avsender)
                        .withVersion("123")
                        .withHendelser(listOf(
                                SOKNADS_STATUS_MOTTATT.withHendelsestidspunkt(tidspunkt_1),
                                SOKNADS_STATUS_UNDERBEHANDLING.withHendelsestidspunkt(tidspunkt_2),
                                DOKUMENTASJONETTERSPURT.withHendelsestidspunkt(tidspunkt_3),
                                DOKUMENTASJONETTERSPURT_TOM_DOKUMENT_LISTE.withHendelsestidspunkt(tidspunkt_4)
                        ))
        every { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any(), any(), any()) } returns
                listOf(InternalVedlegg(vedleggKrevesDokumenttype, vedleggKrevesTilleggsinfo, emptyList(), unixToLocalDateTime(tidspunkt_soknad)))

        val model = service.createModel(mockDigisosSak, "token")

        assertThat(model).isNotNull
        assertThat(model.status).isEqualTo(SoknadsStatus.UNDER_BEHANDLING)
        assertThat(model.saker).isEmpty()
        assertThat(model.oppgaver).isEmpty()
        assertThat(model.historikk).hasSize(5)

        val hendelse = model.historikk.last()
        assertThat(hendelse.tittel).isEqualTo("Vi har sett på dokumentene dine og vil gi beskjed om vi trenger mer fra deg.")
        assertThat(hendelse.tidspunkt).isEqualTo(tidspunkt_4.toLocalDateTime())
    }

    @Test
    internal fun `ny dokumentasjonEtterspurt uten oppgaver skal ikke gi hendelse i historikk ved soknadstatus ferdigbehandlet`() {
        every { innsynService.hentJsonDigisosSoker(any(), any(), any()) } returns
                JsonDigisosSoker()
                        .withAvsender(avsender)
                        .withVersion("123")
                        .withHendelser(listOf(
                                SOKNADS_STATUS_MOTTATT.withHendelsestidspunkt(tidspunkt_1),
                                SOKNADS_STATUS_FERDIGBEHANDLET.withHendelsestidspunkt(tidspunkt_2),
                                DOKUMENTASJONETTERSPURT.withHendelsestidspunkt(tidspunkt_3),
                                DOKUMENTASJONETTERSPURT_TOM_DOKUMENT_LISTE.withHendelsestidspunkt(tidspunkt_4)
                        ))
        every { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any(), any(), any()) } returns
                listOf(InternalVedlegg(vedleggKrevesDokumenttype, vedleggKrevesTilleggsinfo, emptyList(), unixToLocalDateTime(tidspunkt_soknad)))

        val model = service.createModel(mockDigisosSak, "token")

        assertThat(model).isNotNull
        assertThat(model.status).isEqualTo(SoknadsStatus.FERDIGBEHANDLET)
        assertThat(model.saker).isEmpty()
        assertThat(model.oppgaver).isEmpty()
        assertThat(model.historikk).hasSize(4)

        val hendelse = model.historikk.last()
        assertThat(hendelse.tidspunkt).isEqualTo(tidspunkt_3.toLocalDateTime())
    }

    @Test
    internal fun `ny dokumentasjonEtterspurt uten oppgaver skal ikke gi hendelse i historikk ved soknadstatus behandles_ikke`() {
        every { innsynService.hentJsonDigisosSoker(any(), any(), any()) } returns
                JsonDigisosSoker()
                        .withAvsender(avsender)
                        .withVersion("123")
                        .withHendelser(listOf(
                                SOKNADS_STATUS_MOTTATT.withHendelsestidspunkt(tidspunkt_1),
                                SOKNADS_STATUS_BEHANDLES_IKKE.withHendelsestidspunkt(tidspunkt_2),
                                DOKUMENTASJONETTERSPURT.withHendelsestidspunkt(tidspunkt_3),
                                DOKUMENTASJONETTERSPURT_TOM_DOKUMENT_LISTE.withHendelsestidspunkt(tidspunkt_4)
                        ))
        every { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any(), any(), any()) } returns
                listOf(InternalVedlegg(vedleggKrevesDokumenttype, vedleggKrevesTilleggsinfo, emptyList(), unixToLocalDateTime(tidspunkt_soknad)))

        val model = service.createModel(mockDigisosSak, "token")

        assertThat(model).isNotNull
        assertThat(model.status).isEqualTo(SoknadsStatus.BEHANDLES_IKKE)
        assertThat(model.saker).isEmpty()
        assertThat(model.oppgaver).isEmpty()
        assertThat(model.historikk).hasSize(4)

        val hendelse = model.historikk.last()
        assertThat(hendelse.tidspunkt).isEqualTo(tidspunkt_3.toLocalDateTime())
    }

    @Test
    internal fun `skal vise oppgaver fra dokumentasjonEtterspurt selv om soknad er over 30 dager gammel`() {
        val nowMinus31Days = ZonedDateTime.now().minusDays(31)
        val tidspunktSendt31dagerSiden = nowMinus31Days.toEpochSecond() * 1000L
        val tidspunktMottatt = nowMinus31Days.minusMinutes(5).format(DateTimeFormatter.ISO_DATE_TIME)
        val tidspunktUnderBehandling = nowMinus31Days.minusMinutes(4).format(DateTimeFormatter.ISO_DATE_TIME)
        val tidspunktDokumentasjonEtterspurt = nowMinus31Days.minusMinutes(3).format(DateTimeFormatter.ISO_DATE_TIME)

        every { mockDigisosSak.originalSoknadNAV?.timestampSendt } returns tidspunktSendt31dagerSiden
        every { innsynService.hentJsonDigisosSoker(any(), any(), any()) } returns
                JsonDigisosSoker()
                        .withAvsender(avsender)
                        .withVersion("123")
                        .withHendelser(listOf(
                                SOKNADS_STATUS_MOTTATT.withHendelsestidspunkt(tidspunktMottatt),
                                SOKNADS_STATUS_UNDERBEHANDLING.withHendelsestidspunkt(tidspunktUnderBehandling),
                                DOKUMENTASJONETTERSPURT.withHendelsestidspunkt(tidspunktDokumentasjonEtterspurt)
                        ))

        val model = service.createModel(mockDigisosSak, "token")

        assertThat(model).isNotNull
        assertThat(model.oppgaver).hasSize(1)

        val oppgave = model.oppgaver.last()
        assertThat(oppgave.tittel).isEqualTo(dokumenttype)
        assertThat(oppgave.tilleggsinfo).isEqualTo(tilleggsinfo)
        assertThat(oppgave.innsendelsesfrist).isEqualTo(innsendelsesfrist.toLocalDateTime())
        assertThat(oppgave.erFraInnsyn).isEqualTo(true)
    }
}