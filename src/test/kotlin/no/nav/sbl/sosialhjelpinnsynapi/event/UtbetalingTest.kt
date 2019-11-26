package no.nav.sbl.sosialhjelpinnsynapi.event

import io.mockk.every
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sbl.sosialhjelpinnsynapi.domain.SoknadsStatus
import no.nav.sbl.sosialhjelpinnsynapi.domain.UtbetalingsStatus
import no.nav.sbl.sosialhjelpinnsynapi.saksstatus.DEFAULT_TITTEL
import no.nav.sbl.sosialhjelpinnsynapi.vedlegg.VEDLEGG_KREVES_STATUS
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class UtbetalingTest : BaseEventTest() {

    @Test
    fun `utbetaling ETTER vedtakFattet og saksStatus`() {
        every { innsynService.hentJsonDigisosSoker(any(), any(), any()) } returns
                JsonDigisosSoker()
                        .withAvsender(avsender)
                        .withVersion("123")
                        .withHendelser(listOf(
                                SOKNADS_STATUS_MOTTATT.withHendelsestidspunkt(tidspunkt_1),
                                SOKNADS_STATUS_UNDERBEHANDLING.withHendelsestidspunkt(tidspunkt_2),
                                SAK1_SAKS_STATUS_UNDERBEHANDLING.withHendelsestidspunkt(tidspunkt_3),
                                SAK1_VEDTAK_FATTET_INNVILGET.withHendelsestidspunkt(tidspunkt_4),
                                SOKNADS_STATUS_FERDIGBEHANDLET.withHendelsestidspunkt(tidspunkt_5),
                                UTBETALING.withHendelsestidspunkt(tidspunkt_6)
                        ))
        every { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any(), any(), any()) } returns emptyList()

        val model = service.createModel(mockDigisosSak, "token")

        assertThat(model).isNotNull
        assertThat(model.status).isEqualTo(SoknadsStatus.FERDIGBEHANDLET)
        assertThat(model.saker).hasSize(1)
        assertThat(model.historikk).hasSize(5)

        assertThat(model.saker[0].tittel).isEqualTo(tittel_1) // tittel for sak fra saksstatus-hendelse

        assertThat(model.saker[0].utbetalinger).hasSize(1)
        val utbetaling = model.saker[0].utbetalinger[0]
        assertThat(utbetaling.referanse).isEqualTo(utbetaling_ref_1)
        assertThat(utbetaling.status).isEqualTo(UtbetalingsStatus.UTBETALT)
        assertThat(utbetaling.belop).isEqualTo("1234.56")
        assertThat(utbetaling.beskrivelse).isEqualTo(tittel_1)
        assertThat(utbetaling.posteringsDato).isEqualTo(LocalDate.of(2019, 12, 31))
        assertThat(utbetaling.utbetalingsDato).isEqualTo(LocalDate.of(2019, 12, 24))
        assertThat(utbetaling.fom).isNull()
        assertThat(utbetaling.tom).isNull()
        assertThat(utbetaling.mottaker).isEqualTo("fnr")
        assertThat(utbetaling.utbetalingsform).isEqualTo("pose med krølla femtilapper")
        assertThat(utbetaling.vilkar).isEmpty()
        assertThat(utbetaling.dokumentasjonkrav).isEmpty()
    }

    @Test
    fun `utbetaling UTEN vedtakFattet`() {
        every { innsynService.hentJsonDigisosSoker(any(), any(), any()) } returns
                JsonDigisosSoker()
                        .withAvsender(avsender)
                        .withVersion("123")
                        .withHendelser(listOf(
                                SOKNADS_STATUS_MOTTATT.withHendelsestidspunkt(tidspunkt_1),
                                SOKNADS_STATUS_UNDERBEHANDLING.withHendelsestidspunkt(tidspunkt_2),
                                UTBETALING.withHendelsestidspunkt(tidspunkt_3)
                        ))
        every { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any(), any(), any()) } returns emptyList()

        val model = service.createModel(mockDigisosSak, "token")

        assertThat(model).isNotNull
        assertThat(model.status).isEqualTo(SoknadsStatus.UNDER_BEHANDLING)
        assertThat(model.saker).hasSize(1)
        assertThat(model.historikk).hasSize(3)

        assertThat(model.saker[0].tittel).isEqualTo(DEFAULT_TITTEL) // default tittel for sak som settes i dersom hverken saksStatus eller vedtakfattet er mottatt
        assertThat(model.saker[0].utbetalinger).hasSize(1)
        val utbetaling = model.saker[0].utbetalinger[0]
        assertThat(utbetaling.belop).isEqualTo("1234.56")
    }

}