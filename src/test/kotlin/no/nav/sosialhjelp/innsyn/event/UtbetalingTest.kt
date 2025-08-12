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
import no.nav.sosialhjelp.innsyn.domain.SoknadsStatus
import no.nav.sosialhjelp.innsyn.domain.UtbetalingsStatus
import no.nav.sosialhjelp.innsyn.navenhet.NavEnhet
import no.nav.sosialhjelp.innsyn.navenhet.NorgClient
import no.nav.sosialhjelp.innsyn.vedlegg.VEDLEGG_KREVES_STATUS
import no.nav.sosialhjelp.innsyn.vedlegg.VedleggService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds

internal class UtbetalingTest {
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
    fun `utbetaling ETTER vedtakFattet og saksStatus`() =
        runTest(timeout = 5.seconds) {
            coEvery { innsynService.hentJsonDigisosSoker(any()) } returns
                JsonDigisosSoker()
                    .withAvsender(avsender)
                    .withVersion("123")
                    .withHendelser(
                        listOf(
                            SOKNADS_STATUS_MOTTATT.withHendelsestidspunkt(tidspunkt_1),
                            SOKNADS_STATUS_UNDERBEHANDLING.withHendelsestidspunkt(tidspunkt_2),
                            SAK1_SAKS_STATUS_UNDERBEHANDLING.withHendelsestidspunkt(tidspunkt_3),
                            SAK1_VEDTAK_FATTET_INNVILGET.withHendelsestidspunkt(tidspunkt_4),
                            SOKNADS_STATUS_FERDIGBEHANDLET.withHendelsestidspunkt(tidspunkt_5),
                            UTBETALING.withHendelsestidspunkt(tidspunkt_6),
                        ),
                    )
            coEvery { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any()) } returns emptyList()

            val model = service.createModel(mockDigisosSak)

            assertThat(model).isNotNull
            assertThat(model.status).isEqualTo(SoknadsStatus.FERDIGBEHANDLET)
            assertThat(model.saker).hasSize(1)
            assertThat(model.historikk).hasSize(6)

            // tittel for sak fra saksstatus-hendelse
            assertThat(model.saker[0].tittel).isEqualTo(TITTEL_1)

            assertThat(model.saker[0].utbetalinger).hasSize(1)
            val utbetaling = model.saker[0].utbetalinger[0]
            assertThat(utbetaling.referanse).isEqualTo(UTBETALING_REF_1)
            assertThat(utbetaling.status).isEqualTo(UtbetalingsStatus.UTBETALT)
            assertThat(utbetaling.belop).isEqualTo("1234.56")
            assertThat(utbetaling.beskrivelse).isEqualTo(TITTEL_1)
            assertThat(utbetaling.forfallsDato).isEqualTo("2019-12-31")
            assertThat(utbetaling.utbetalingsDato).isEqualTo("2019-12-24")
            assertThat(utbetaling.fom).isEqualTo("2019-12-01")
            assertThat(utbetaling.tom).isEqualTo("2019-12-31")
            assertThat(utbetaling.mottaker).isEqualTo("fnr")
            assertThat(utbetaling.kontonummer).isNull()
            assertThat(utbetaling.utbetalingsmetode).isEqualTo("pose med krølla femtilapper")
            assertThat(utbetaling.vilkar).isEmpty()
            assertThat(utbetaling.dokumentasjonkrav).isEmpty()
        }

    @Test
    fun `utbetaling UTEN vedtakFattet`() =
        runTest(timeout = 5.seconds) {
            coEvery { innsynService.hentJsonDigisosSoker(any()) } returns
                JsonDigisosSoker()
                    .withAvsender(avsender)
                    .withVersion("123")
                    .withHendelser(
                        listOf(
                            SOKNADS_STATUS_MOTTATT.withHendelsestidspunkt(tidspunkt_1),
                            SOKNADS_STATUS_UNDERBEHANDLING.withHendelsestidspunkt(tidspunkt_2),
                            UTBETALING_BANKOVERFORING.withHendelsestidspunkt(tidspunkt_3),
                        ),
                    )
            coEvery { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any()) } returns emptyList()

            val model = service.createModel(mockDigisosSak)

            assertThat(model).isNotNull
            assertThat(model.status).isEqualTo(SoknadsStatus.UNDER_BEHANDLING)

            val utbetaling = model.utbetalinger[0]
            assertThat(utbetaling.belop).isEqualTo("1234.56")
        }

    @Test
    fun `utbetaling kontonummer settes kun hvis annenMottaker er false`() =
        runTest(timeout = 5.seconds) {
            coEvery { innsynService.hentJsonDigisosSoker(any()) } returns
                JsonDigisosSoker()
                    .withAvsender(avsender)
                    .withVersion("123")
                    .withHendelser(
                        listOf(
                            SOKNADS_STATUS_MOTTATT.withHendelsestidspunkt(tidspunkt_1),
                            SOKNADS_STATUS_UNDERBEHANDLING.withHendelsestidspunkt(tidspunkt_2),
                            UTBETALING_BANKOVERFORING_ANNEN_MOTTAKER.withHendelsestidspunkt(tidspunkt_3),
                        ),
                    )
            coEvery { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any()) } returns emptyList()

            val model = service.createModel(mockDigisosSak)

            assertThat(model).isNotNull
            assertThat(model.status).isEqualTo(SoknadsStatus.UNDER_BEHANDLING)

            assertThat(model.utbetalinger[0].belop).isEqualTo("1234.56")
            assertThat(model.utbetalinger[0].kontonummer).isNull()
            assertThat(model.utbetalinger[0].mottaker).isEqualTo("utleier")
        }
}
