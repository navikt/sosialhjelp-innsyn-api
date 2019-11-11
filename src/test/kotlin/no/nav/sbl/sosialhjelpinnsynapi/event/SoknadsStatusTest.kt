package no.nav.sbl.sosialhjelpinnsynapi.event

import io.mockk.every
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sbl.sosialhjelpinnsynapi.domain.SoknadsStatus
import no.nav.sbl.sosialhjelpinnsynapi.toLocalDateTime
import no.nav.sbl.sosialhjelpinnsynapi.unixToLocalDateTime
import no.nav.sbl.sosialhjelpinnsynapi.vedlegg.VEDLEGG_KREVES_STATUS
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class SoknadsStatusTest : BaseEventTest() {

    @Test
    fun `soknadsStatus SENDT`() {
        every { innsynService.hentJsonDigisosSoker(any(), any(), any()) } returns null
        every { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any(), any(), any()) } returns emptyList()

        val model = service.createModel(mockDigisosSak, "token")

        assertThat(model).isNotNull
        assertThat(model.status).isEqualTo(SoknadsStatus.SENDT)
        assertThat(model.historikk).hasSize(1)

        val hendelse = model.historikk.last()
        assertThat(hendelse.tidspunkt).isEqualTo(unixToLocalDateTime(tidspunkt_soknad))
        assertThat(hendelse.tittel).contains("Søknaden med vedlegg er sendt til")
    }

    @Test
    fun `soknadsStatus MOTTATT`() {
        every { innsynService.hentJsonDigisosSoker(any(), any(), any()) } returns
                JsonDigisosSoker()
                        .withAvsender(avsender)
                        .withVersion("123")
                        .withHendelser(listOf(
                                SOKNADS_STATUS_MOTTATT.withHendelsestidspunkt(tidspunkt_1)
                        ))
        every { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any(), any(), any()) } returns emptyList()

        val model = service.createModel(mockDigisosSak, "token")

        assertThat(model).isNotNull
        assertThat(model.status).isEqualTo(SoknadsStatus.MOTTATT)
        assertThat(model.historikk).hasSize(2)

        val hendelse = model.historikk.last()
        assertThat(hendelse.tidspunkt).isEqualTo(toLocalDateTime(tidspunkt_1))
        assertThat(hendelse.tittel).contains("Søknaden med vedlegg er mottatt hos ")
    }

    @Test
    fun `soknadsStatus MOTTATT papirsoknad`() {
        every { mockJsonSoknad.mottaker } returns null
        every { innsynService.hentJsonDigisosSoker(any(), any(), any()) } returns
                JsonDigisosSoker()
                        .withAvsender(avsender)
                        .withVersion("123")
                        .withHendelser(listOf(
                                SOKNADS_STATUS_MOTTATT.withHendelsestidspunkt(tidspunkt_1)
                        ))
        every { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any(), any(), any()) } returns emptyList()

        val model = service.createModel(mockDigisosSak, "token")

        assertThat(model).isNotNull
        assertThat(model.status).isEqualTo(SoknadsStatus.MOTTATT)
        assertThat(model.historikk).hasSize(1)

        val hendelse = model.historikk.last()
        assertThat(hendelse.tidspunkt).isEqualTo(toLocalDateTime(tidspunkt_1))
        assertThat(hendelse.tittel).contains("Søknaden med vedlegg er mottatt")
    }

    @Test
    fun `soknadsStatus UNDER_BEHANDLING`() {
        every { innsynService.hentJsonDigisosSoker(any(), any(), any()) } returns
                JsonDigisosSoker()
                        .withAvsender(avsender)
                        .withVersion("123")
                        .withHendelser(listOf(
                                SOKNADS_STATUS_MOTTATT.withHendelsestidspunkt(tidspunkt_1),
                                SOKNADS_STATUS_UNDERBEHANDLING.withHendelsestidspunkt(tidspunkt_2)
                        ))
        every { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any(), any(), any()) } returns emptyList()

        val model = service.createModel(mockDigisosSak, "token")

        assertThat(model).isNotNull
        assertThat(model.status).isEqualTo(SoknadsStatus.UNDER_BEHANDLING)
        assertThat(model.saker).isEmpty()
        assertThat(model.historikk).hasSize(3)

        val hendelse = model.historikk.last()
        assertThat(hendelse.tidspunkt).isEqualTo(toLocalDateTime(tidspunkt_2))
        assertThat(hendelse.tittel).contains("Søknaden er under behandling")
    }

    @Test
    fun `soknadsStatus FERDIGBEHANDLET`() {
        every { innsynService.hentJsonDigisosSoker(any(), any(), any()) } returns
                JsonDigisosSoker()
                        .withAvsender(avsender)
                        .withVersion("123")
                        .withHendelser(listOf(
                                SOKNADS_STATUS_MOTTATT.withHendelsestidspunkt(tidspunkt_1),
                                SOKNADS_STATUS_UNDERBEHANDLING.withHendelsestidspunkt(tidspunkt_2),
                                SOKNADS_STATUS_FERDIGBEHANDLET.withHendelsestidspunkt(tidspunkt_3)
                        ))
        every { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any(), any(), any()) } returns emptyList()

        val model = service.createModel(mockDigisosSak, "token")

        assertThat(model).isNotNull
        assertThat(model.status).isEqualTo(SoknadsStatus.FERDIGBEHANDLET)
        assertThat(model.saker).isEmpty()
        assertThat(model.historikk).hasSize(4)

        val hendelse = model.historikk.last()
        assertThat(hendelse.tidspunkt).isEqualTo(toLocalDateTime(tidspunkt_3))
        assertThat(hendelse.tittel).contains("Søknaden er ferdig behandlet")
    }
}