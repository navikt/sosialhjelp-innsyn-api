package no.nav.sosialhjelp.innsyn.event

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import no.finn.unleash.Unleash
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sbl.soknadsosialhjelp.soknad.JsonSoknad
import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.innsyn.client.unleash.DOKUMENTASJONKRAV
import no.nav.sosialhjelp.innsyn.client.unleash.DOKUMENTASJONKRAV_ENABLED
import no.nav.sosialhjelp.innsyn.config.ClientProperties
import no.nav.sosialhjelp.innsyn.domain.Oppgavestatus
import no.nav.sosialhjelp.innsyn.domain.SoknadsStatus
import no.nav.sosialhjelp.innsyn.navenhet.NavEnhet
import no.nav.sosialhjelp.innsyn.navenhet.NorgClient
import no.nav.sosialhjelp.innsyn.service.innsyn.InnsynService
import no.nav.sosialhjelp.innsyn.utils.toLocalDateTime
import no.nav.sosialhjelp.innsyn.vedlegg.VEDLEGG_KREVES_STATUS
import no.nav.sosialhjelp.innsyn.vedlegg.VedleggService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class DokumentasjonkravTest {

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

    private val hendelsetekst = "Dokumentasjonskravene dine er oppdatert, les mer i vedtaket."

    @BeforeEach
    fun init() {
        clearAllMocks()
        every { mockDigisosSak.fiksDigisosId } returns "123"
        every { mockDigisosSak.digisosSoker?.metadata } returns "some id"
        every { mockDigisosSak.originalSoknadNAV?.metadata } returns "some other id"
        every { mockDigisosSak.originalSoknadNAV?.timestampSendt } returns tidspunkt_soknad
        every { mockDigisosSak.originalSoknadNAV?.navEksternRefId } returns null
        every { mockJsonSoknad.mottaker.navEnhetsnavn } returns soknadsmottaker
        every { mockJsonSoknad.mottaker.enhetsnummer } returns enhetsnr
        every { mockDigisosSak.ettersendtInfoNAV } returns null
        every { innsynService.hentOriginalSoknad(any(), any()) } returns mockJsonSoknad
        every { norgClient.hentNavEnhet(enhetsnr) } returns mockNavEnhet
        every { mockDigisosSak.originalSoknadNAV?.soknadDokument?.dokumentlagerDokumentId } returns null

        every { unleashClient.isEnabled(DOKUMENTASJONKRAV_ENABLED, false) } returns true
        every { unleashClient.isEnabled(DOKUMENTASJONKRAV, false) } returns true

        resetHendelser()
    }

    @Test
    fun `dokumentasjonkrav ETTER utbetaling`() {
        every { innsynService.hentJsonDigisosSoker(any(), any()) } returns
            JsonDigisosSoker()
                .withAvsender(avsender)
                .withVersion("123")
                .withHendelser(
                    listOf(
                        SOKNADS_STATUS_MOTTATT.withHendelsestidspunkt(tidspunkt_1),
                        SOKNADS_STATUS_UNDERBEHANDLING.withHendelsestidspunkt(tidspunkt_2),
                        SAK1_SAKS_STATUS_UNDERBEHANDLING.withHendelsestidspunkt(tidspunkt_3),
                        SOKNADS_STATUS_FERDIGBEHANDLET.withHendelsestidspunkt(tidspunkt_4),
                        UTBETALING.withHendelsestidspunkt(tidspunkt_5),
                        DOKUMENTASJONKRAV_OPPFYLT.withHendelsestidspunkt(tidspunkt_6)
                    )
                )
        every { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any(), any()) } returns emptyList()

        val model = service.createModel(mockDigisosSak, "token")

        assertThat(model).isNotNull
        assertThat(model.status).isEqualTo(SoknadsStatus.FERDIGBEHANDLET)
        assertThat(model.saker).hasSize(1)
        assertThat(model.historikk).hasSize(6)

        assertThat(model.saker[0].utbetalinger).hasSize(1)
        val utbetaling = model.saker[0].utbetalinger[0]
        assertThat(utbetaling.dokumentasjonkrav).hasSize(1)
        assertThat(utbetaling.dokumentasjonkrav[0].referanse).isEqualTo(dokumentasjonkrav_ref_1)
        assertThat(utbetaling.dokumentasjonkrav[0].beskrivelse).isEqualTo("beskrivelse")
        assertThat(utbetaling.dokumentasjonkrav[0].getOppgaveStatus()).isEqualTo(Oppgavestatus.RELEVANT)

        val hendelse = model.historikk.last()
        assertThat(hendelse.tittel).isEqualTo(hendelsetekst)
        assertThat(hendelse.tidspunkt).isEqualTo(tidspunkt_6.toLocalDateTime())
    }

    @Test
    fun `dokumentasjonkrav UTEN utbetaling - skal ikke legge til dokumentasjonkrav eller historikk`() {
        every { innsynService.hentJsonDigisosSoker(any(), any()) } returns
            JsonDigisosSoker()
                .withAvsender(avsender)
                .withVersion("123")
                .withHendelser(
                    listOf(
                        SOKNADS_STATUS_MOTTATT.withHendelsestidspunkt(tidspunkt_1),
                        SOKNADS_STATUS_UNDERBEHANDLING.withHendelsestidspunkt(tidspunkt_2),
                        DOKUMENTASJONKRAV_OPPFYLT.withHendelsestidspunkt(tidspunkt_3)
                    )
                )
        every { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any(), any()) } returns emptyList()

        val model = service.createModel(mockDigisosSak, "token")

        assertThat(model).isNotNull
        assertThat(model.status).isEqualTo(SoknadsStatus.UNDER_BEHANDLING)
        assertThat(model.saker).hasSize(0)
        assertThat(model.historikk).hasSize(3)

        val hendelse = model.historikk.last()
        assertThat(hendelse.tittel).isNotEqualTo(hendelsetekst)
    }

    @Test
    fun `dokumentasjonkrav ETTER utbetaling UTEN saksreferanse`() {
        every { innsynService.hentJsonDigisosSoker(any(), any()) } returns
            JsonDigisosSoker()
                .withAvsender(avsender)
                .withVersion("123")
                .withHendelser(
                    listOf(
                        SOKNADS_STATUS_MOTTATT.withHendelsestidspunkt(tidspunkt_1),
                        SOKNADS_STATUS_UNDERBEHANDLING.withHendelsestidspunkt(tidspunkt_2),
                        SOKNADS_STATUS_FERDIGBEHANDLET.withHendelsestidspunkt(tidspunkt_3),
                        UTBETALING.withHendelsestidspunkt(tidspunkt_4),
                        DOKUMENTASJONKRAV_OPPFYLT.withHendelsestidspunkt(tidspunkt_5)
                    )
                )
        every { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any(), any()) } returns emptyList()

        val model = service.createModel(mockDigisosSak, "token")

        assertThat(model).isNotNull
        assertThat(model.status).isEqualTo(SoknadsStatus.FERDIGBEHANDLET)
        assertThat(model.saker).hasSize(0)
        assertThat(model.historikk).hasSize(5)

        val hendelse = model.historikk.last()
        assertThat(hendelse.tittel).isEqualTo(hendelsetekst)
        assertThat(hendelse.tidspunkt).isEqualTo(tidspunkt_5.toLocalDateTime())
    }

    @Test
    fun `dokumentasjonkrav samme dokumentasjonkravreferanse to ganger`() {
        every { innsynService.hentJsonDigisosSoker(any(), any()) } returns
            JsonDigisosSoker()
                .withAvsender(avsender)
                .withVersion("123")
                .withHendelser(
                    listOf(
                        SOKNADS_STATUS_MOTTATT.withHendelsestidspunkt(tidspunkt_1),
                        SOKNADS_STATUS_UNDERBEHANDLING.withHendelsestidspunkt(tidspunkt_2),
                        SOKNADS_STATUS_FERDIGBEHANDLET.withHendelsestidspunkt(tidspunkt_3),
                        UTBETALING.withHendelsestidspunkt(tidspunkt_4),
                        DOKUMENTASJONKRAV_OPPFYLT.withHendelsestidspunkt(tidspunkt_5),
                        DOKUMENTASJONKRAV_OPPFYLT.withHendelsestidspunkt(tidspunkt_6)
                    )
                )
        every { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any(), any()) } returns emptyList()

        val model = service.createModel(mockDigisosSak, "token")

        assertThat(model).isNotNull
        assertThat(model.status).isEqualTo(SoknadsStatus.FERDIGBEHANDLET)
        assertThat(model.saker).hasSize(0)
        assertThat(model.historikk).hasSize(6)

        val hendelse = model.historikk.last()
        assertThat(hendelse.tittel).isEqualTo(hendelsetekst)
        assertThat(hendelse.tidspunkt).isEqualTo(tidspunkt_6.toLocalDateTime())
    }

    @Test
    fun `dokumentasjonkrav FOR utbetaling - skal ikke gi noen dokumentasjonkrav`() {
        every { innsynService.hentJsonDigisosSoker(any(), any()) } returns
            JsonDigisosSoker()
                .withAvsender(avsender)
                .withVersion("123")
                .withHendelser(
                    listOf(
                        SOKNADS_STATUS_MOTTATT.withHendelsestidspunkt(tidspunkt_1),
                        SOKNADS_STATUS_UNDERBEHANDLING.withHendelsestidspunkt(tidspunkt_2),
                        SAK1_SAKS_STATUS_UNDERBEHANDLING.withHendelsestidspunkt(tidspunkt_3),
                        SOKNADS_STATUS_FERDIGBEHANDLET.withHendelsestidspunkt(tidspunkt_4),
                        DOKUMENTASJONKRAV_OPPFYLT.withHendelsestidspunkt(tidspunkt_5),
                        UTBETALING.withHendelsestidspunkt(tidspunkt_6)
                    )
                )
        every { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any(), any()) } returns emptyList()

        val model = service.createModel(mockDigisosSak, "token")

        assertThat(model).isNotNull
        assertThat(model.saker).hasSize(1)
        assertThat(model.historikk).hasSize(5)

        assertThat(model.saker[0].utbetalinger).hasSize(1)
        val utbetaling = model.saker[0].utbetalinger[0]
        assertThat(utbetaling.dokumentasjonkrav).hasSize(0)
    }

    @Test
    fun `dokumentasjonkrav og utbetaling har identiske hendelsestidspunkt`() {
        every { innsynService.hentJsonDigisosSoker(any(), any()) } returns
            JsonDigisosSoker()
                .withAvsender(avsender)
                .withVersion("123")
                .withHendelser(
                    listOf(
                        SOKNADS_STATUS_MOTTATT.withHendelsestidspunkt(tidspunkt_1),
                        SOKNADS_STATUS_UNDERBEHANDLING.withHendelsestidspunkt(tidspunkt_2),
                        SAK1_SAKS_STATUS_UNDERBEHANDLING.withHendelsestidspunkt(tidspunkt_3),
                        SOKNADS_STATUS_FERDIGBEHANDLET.withHendelsestidspunkt(tidspunkt_4),
                        DOKUMENTASJONKRAV_OPPFYLT.withHendelsestidspunkt(tidspunkt_5),
                        UTBETALING.withHendelsestidspunkt(tidspunkt_5)
                    )
                )
        every { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any(), any()) } returns emptyList()

        val model = service.createModel(mockDigisosSak, "token")

        assertThat(model).isNotNull
        assertThat(model.saker).hasSize(1)
        assertThat(model.historikk).hasSize(6)

        assertThat(model.saker[0].utbetalinger).hasSize(1)
        val utbetaling = model.saker[0].utbetalinger[0]
        assertThat(utbetaling.dokumentasjonkrav).hasSize(1)
        assertThat(utbetaling.dokumentasjonkrav[0].referanse).isEqualTo(dokumentasjonkrav_ref_1)
    }
}
