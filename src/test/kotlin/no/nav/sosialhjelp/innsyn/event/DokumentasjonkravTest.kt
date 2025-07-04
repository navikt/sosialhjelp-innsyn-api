package no.nav.sosialhjelp.innsyn.event

import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sbl.soknadsosialhjelp.soknad.JsonSoknad
import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.innsyn.app.ClientProperties
import no.nav.sosialhjelp.innsyn.app.token.Token
import no.nav.sosialhjelp.innsyn.domain.HendelseTekstType
import no.nav.sosialhjelp.innsyn.domain.Oppgavestatus
import no.nav.sosialhjelp.innsyn.domain.SoknadsStatus
import no.nav.sosialhjelp.innsyn.navenhet.NavEnhet
import no.nav.sosialhjelp.innsyn.navenhet.NorgClient
import no.nav.sosialhjelp.innsyn.utils.toLocalDateTime
import no.nav.sosialhjelp.innsyn.vedlegg.VEDLEGG_KREVES_STATUS
import no.nav.sosialhjelp.innsyn.vedlegg.VedleggService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds

internal class DokumentasjonkravTest {
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

    private val hendelsetekst = HendelseTekstType.DOKUMENTASJONKRAV

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
        coEvery { innsynService.hentOriginalSoknad(any(), any()) } returns mockJsonSoknad
        coEvery { norgClient.hentNavEnhet(enhetsnr) } returns mockNavEnhet
        every { mockDigisosSak.originalSoknadNAV?.soknadDokument?.dokumentlagerDokumentId } returns null

        resetHendelser()
    }

    @Test
    fun `dokumentasjonkrav ETTER utbetaling`() =
        runTest(timeout = 5.seconds) {
            coEvery { innsynService.hentJsonDigisosSoker(any(), any()) } returns
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
                            DOKUMENTASJONKRAV_OPPFYLT.withHendelsestidspunkt(tidspunkt_6),
                        ),
                    )
            coEvery { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any(), any()) } returns emptyList()

            val model = service.createModel(mockDigisosSak, Token("token"))

            assertThat(model).isNotNull
            assertThat(model.status).isEqualTo(SoknadsStatus.FERDIGBEHANDLET)
            assertThat(model.saker).hasSize(1)
            assertThat(model.historikk).hasSize(6)

            assertThat(model.saker[0].utbetalinger).hasSize(1)
            val utbetaling = model.saker[0].utbetalinger[0]
            assertThat(utbetaling.dokumentasjonkrav).hasSize(1)
            assertThat(utbetaling.dokumentasjonkrav[0].referanse).isEqualTo(DOKUMENTASJONKRAV_REF_1)
            assertThat(utbetaling.dokumentasjonkrav[0].beskrivelse).isEqualTo("beskrivelse")
            assertThat(utbetaling.dokumentasjonkrav[0].getOppgaveStatus()).isEqualTo(Oppgavestatus.RELEVANT)

            val hendelse = model.historikk.last()
            assertThat(hendelse.hendelseType).isEqualTo(hendelsetekst)
            assertThat(hendelse.tidspunkt).isEqualTo(tidspunkt_6.toLocalDateTime())
        }

    @Test
    fun `dokumentasjonkrav UTEN utbetaling - skal ikke legge til dokumentasjonkrav eller historikk`() =
        runTest(timeout = 5.seconds) {
            coEvery { innsynService.hentJsonDigisosSoker(any(), any()) } returns
                JsonDigisosSoker()
                    .withAvsender(avsender)
                    .withVersion("123")
                    .withHendelser(
                        listOf(
                            SOKNADS_STATUS_MOTTATT.withHendelsestidspunkt(tidspunkt_1),
                            SOKNADS_STATUS_UNDERBEHANDLING.withHendelsestidspunkt(tidspunkt_2),
                            DOKUMENTASJONKRAV_OPPFYLT.withHendelsestidspunkt(tidspunkt_3),
                        ),
                    )
            coEvery { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any(), any()) } returns emptyList()

            val model = service.createModel(mockDigisosSak, Token("token"))

            assertThat(model).isNotNull
            assertThat(model.status).isEqualTo(SoknadsStatus.UNDER_BEHANDLING)
            assertThat(model.saker).hasSize(0)
            assertThat(model.historikk).hasSize(3)

            val hendelse = model.historikk.last()
            assertThat(hendelse.hendelseType).isNotEqualTo(hendelsetekst)
        }

    @Test
    fun `dokumentasjonkrav ETTER utbetaling UTEN saksreferanse`() =
        runTest(timeout = 5.seconds) {
            coEvery { innsynService.hentJsonDigisosSoker(any(), any()) } returns
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
                        ),
                    )
            coEvery { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any(), any()) } returns emptyList()

            val model = service.createModel(mockDigisosSak, Token("token"))

            assertThat(model).isNotNull
            assertThat(model.status).isEqualTo(SoknadsStatus.FERDIGBEHANDLET)
            assertThat(model.saker).hasSize(0)
            assertThat(model.historikk).hasSize(5)

            val hendelse = model.historikk.last()
            assertThat(hendelse.hendelseType).isEqualTo(hendelsetekst)
            assertThat(hendelse.tidspunkt).isEqualTo(tidspunkt_5.toLocalDateTime())
        }

    @Test
    fun `dokumentasjonkrav samme dokumentasjonkravreferanse to ganger`() =
        runTest(timeout = 5.seconds) {
            coEvery { innsynService.hentJsonDigisosSoker(any(), any()) } returns
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
                            DOKUMENTASJONKRAV_OPPFYLT.withHendelsestidspunkt(tidspunkt_6),
                        ),
                    )
            coEvery { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any(), any()) } returns emptyList()

            val model = service.createModel(mockDigisosSak, Token("token"))

            assertThat(model).isNotNull
            assertThat(model.status).isEqualTo(SoknadsStatus.FERDIGBEHANDLET)
            assertThat(model.saker).hasSize(0)
            assertThat(model.historikk).hasSize(6)

            val hendelse = model.historikk.last()
            assertThat(hendelse.hendelseType).isEqualTo(hendelsetekst)
            assertThat(hendelse.tidspunkt).isEqualTo(tidspunkt_6.toLocalDateTime())
        }

    @Test
    fun `dokumentasjonkrav FOR utbetaling - skal ikke gi noen dokumentasjonkrav`() =
        runTest(timeout = 5.seconds) {
            coEvery { innsynService.hentJsonDigisosSoker(any(), any()) } returns
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
                            UTBETALING.withHendelsestidspunkt(tidspunkt_6),
                        ),
                    )
            coEvery { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any(), any()) } returns emptyList()

            val model = service.createModel(mockDigisosSak, Token("token"))

            assertThat(model).isNotNull
            assertThat(model.saker).hasSize(1)
            assertThat(model.historikk).hasSize(5)

            assertThat(model.saker[0].utbetalinger).hasSize(1)
            val utbetaling = model.saker[0].utbetalinger[0]
            assertThat(utbetaling.dokumentasjonkrav).hasSize(0)
        }

    @Test
    fun `dokumentasjonkrav og utbetaling har identiske hendelsestidspunkt`() =
        runTest(timeout = 5.seconds) {
            coEvery { innsynService.hentJsonDigisosSoker(any(), any()) } returns
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
                            UTBETALING.withHendelsestidspunkt(tidspunkt_5),
                        ),
                    )
            coEvery { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any(), any()) } returns emptyList()

            val model = service.createModel(mockDigisosSak, Token("token"))

            assertThat(model).isNotNull
            assertThat(model.saker).hasSize(1)
            assertThat(model.historikk).hasSize(6)

            assertThat(model.saker[0].utbetalinger).hasSize(1)
            val utbetaling = model.saker[0].utbetalinger[0]
            assertThat(utbetaling.dokumentasjonkrav).hasSize(1)
            assertThat(utbetaling.dokumentasjonkrav[0].referanse).isEqualTo(DOKUMENTASJONKRAV_REF_1)
        }
}
