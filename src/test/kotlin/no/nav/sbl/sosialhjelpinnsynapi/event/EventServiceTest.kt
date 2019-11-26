package no.nav.sbl.sosialhjelpinnsynapi.event

import io.mockk.every
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sbl.sosialhjelpinnsynapi.domain.SaksStatus
import no.nav.sbl.sosialhjelpinnsynapi.domain.SoknadsStatus
import no.nav.sbl.sosialhjelpinnsynapi.domain.UtfallVedtak
import no.nav.sbl.sosialhjelpinnsynapi.saksstatus.DEFAULT_TITTEL
import no.nav.sbl.sosialhjelpinnsynapi.toLocalDateTime
import no.nav.sbl.sosialhjelpinnsynapi.vedlegg.VEDLEGG_KREVES_STATUS
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test


internal class EventServiceTest : BaseEventTest() {

/* Test-caser:
 [x] ingen innsyn, ingen sendt soknad
 [x] ingen innsyn, sendt soknad -> status SENDT
 [x] status mottatt
 [x] status under behandling
 [x] status ferdig behandlet
 [x] saksStatus uten vedtakFattet
 [x] saksStatus før vedtakFattet
 [x] vedtakFattet uten saksStatus
 [x] vedtakFattet før saksStatus
 [x] saksStatus med 2 vedtakFattet
 [x] dokumentasjonEtterspurt
 [x] dokumentasjonEtterspurt med tom dokumentliste
 [x] ingen dokumentasjonEtterspurt-hendelser
 [x] forelopigSvar
 [ ] forelopigSvar - flere caser?
 [x] utbetaling
 [?] utbetaling - flere caser?
 [x] dokumentasjonskrav
 [x] vilkår
 [x] tildeltNavKontor - OK response fra Norg
 [x] tildeltNavKontor - generell melding ved Norg-feil
 [ ] rammevedtak
 ...
 [ ] komplett case
*/

    @Test
    fun `ingen innsyn OG ingen soknad, men med sendTidspunkt`() {
        every { innsynService.hentJsonDigisosSoker(any(), any(), any()) } returns null
        every { innsynService.hentOriginalSoknad(any(), any(), any()) } returns null
        every { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any(), any(), any()) } returns emptyList()

        val model = service.createModel(mockDigisosSak, "token")

        assertThat(model).isNotNull
        assertThat(model.status).isEqualTo(SoknadsStatus.SENDT)
        assertThat(model.historikk).hasSize(0)
    }

    @Test
    fun `ingen innsyn `() {
        every { mockDigisosSak.digisosSoker } returns null
        every { innsynService.hentJsonDigisosSoker(any(), null, any()) } returns null
        every { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any(), any(), any()) } returns emptyList()

        val model = service.createModel(mockDigisosSak, "token")

        assertThat(model).isNotNull
        assertThat(model.historikk).hasSize(1)
    }

    @Nested
    inner class SaksStatusVedtakFattet {

        @Test
        fun `saksStatus UTEN vedtakFattet`() {
            every { innsynService.hentJsonDigisosSoker(any(), any(), any()) } returns
                    JsonDigisosSoker()
                            .withAvsender(avsender)
                            .withVersion("123")
                            .withHendelser(listOf(
                                    SOKNADS_STATUS_MOTTATT.withHendelsestidspunkt(tidspunkt_1),
                                    SOKNADS_STATUS_UNDERBEHANDLING.withHendelsestidspunkt(tidspunkt_2),
                                    SAK1_SAKS_STATUS_UNDERBEHANDLING.withHendelsestidspunkt(tidspunkt_3)
                            ))
            every { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any(), any(), any()) } returns emptyList()

            val model = service.createModel(mockDigisosSak, "token")

            assertThat(model).isNotNull
            assertThat(model.status).isEqualTo(SoknadsStatus.UNDER_BEHANDLING)
            assertThat(model.saker).hasSize(1)
            assertThat(model.historikk).hasSize(3)

            val sak = model.saker.last()
            assertThat(sak.saksStatus).isEqualTo(SaksStatus.UNDER_BEHANDLING)
            assertThat(sak.referanse).isEqualTo(referanse_1)
            assertThat(sak.tittel).isEqualTo(tittel_1)
            assertThat(sak.vedtak).isEmpty()
            assertThat(sak.utbetalinger).isEmpty()

            val hendelse = model.historikk.last()
            assertThat(hendelse.tidspunkt).isEqualTo(toLocalDateTime(tidspunkt_2))
            assertThat(hendelse.tittel).contains("Søknaden er under behandling")
        }

        @Test
        fun `saksStatus UTEN tittel eller status`() {
            every { innsynService.hentJsonDigisosSoker(any(), any(), any()) } returns
                    JsonDigisosSoker()
                            .withAvsender(avsender)
                            .withVersion("123")
                            .withHendelser(listOf(
                                    SOKNADS_STATUS_MOTTATT.withHendelsestidspunkt(tidspunkt_1),
                                    SOKNADS_STATUS_UNDERBEHANDLING.withHendelsestidspunkt(tidspunkt_2),
                                    SAK1_UTEN_SAKS_STATUS_ELLER_TITTEL.withHendelsestidspunkt(tidspunkt_3)
                            ))
            every { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any(), any(), any()) } returns emptyList()

            val model = service.createModel(mockDigisosSak, "token")

            assertThat(model).isNotNull
            assertThat(model.status).isEqualTo(SoknadsStatus.UNDER_BEHANDLING)
            assertThat(model.saker).hasSize(1)
            assertThat(model.historikk).hasSize(3)

            val sak = model.saker.last()
            assertThat(sak.saksStatus).isEqualTo(SaksStatus.UNDER_BEHANDLING)
            assertThat(sak.referanse).isEqualTo(referanse_1)
            assertThat(sak.tittel).isNull()
            assertThat(sak.vedtak).isEmpty()
            assertThat(sak.utbetalinger).isEmpty()

            val hendelse = model.historikk.last()
            assertThat(hendelse.tidspunkt).isEqualTo(toLocalDateTime(tidspunkt_2))
            assertThat(hendelse.tittel).contains("Søknaden er under behandling")
        }

        @Test
        fun `saksStatus FOR vedtakFattet`() {
            every { innsynService.hentJsonDigisosSoker(any(), any(), any()) } returns
                    JsonDigisosSoker()
                            .withAvsender(avsender)
                            .withVersion("123")
                            .withHendelser(listOf(
                                    SOKNADS_STATUS_MOTTATT.withHendelsestidspunkt(tidspunkt_1),
                                    SOKNADS_STATUS_UNDERBEHANDLING.withHendelsestidspunkt(tidspunkt_2),
                                    SAK1_SAKS_STATUS_UNDERBEHANDLING.withHendelsestidspunkt(tidspunkt_3),
                                    SAK1_VEDTAK_FATTET_INNVILGET.withHendelsestidspunkt(tidspunkt_4)
                            ))
            every { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any(), any(), any()) } returns emptyList()

            val model = service.createModel(mockDigisosSak, "token")

            assertThat(model).isNotNull
            assertThat(model.status).isEqualTo(SoknadsStatus.UNDER_BEHANDLING)
            assertThat(model.saker).hasSize(1)
            assertThat(model.historikk).hasSize(4)

            val sak = model.saker.last()
            assertThat(sak.saksStatus).isEqualTo(SaksStatus.UNDER_BEHANDLING)
            assertThat(sak.referanse).isEqualTo(referanse_1)
            assertThat(sak.tittel).isEqualTo(tittel_1)
            assertThat(sak.vedtak).hasSize(1)
            assertThat(sak.utbetalinger).isEmpty()

            val vedtak = sak.vedtak.last()
            assertThat(vedtak.utfall).isEqualTo(UtfallVedtak.INNVILGET)
            assertThat(vedtak.vedtaksFilUrl).contains("/dokumentlager/nedlasting/$dokumentlagerId_1")

            val hendelse = model.historikk.last()
            assertThat(hendelse.tidspunkt).isEqualTo(toLocalDateTime(tidspunkt_4))
            assertThat(hendelse.tittel).contains("$tittel_1 er ferdig behandlet")
            assertThat(hendelse.url).contains("/dokumentlager/nedlasting/$dokumentlagerId_1")
        }

        @Test
        fun `vedtakFattet UTEN saksStatus`() {
            every { innsynService.hentJsonDigisosSoker(any(), any(), any()) } returns
                    JsonDigisosSoker()
                            .withAvsender(avsender)
                            .withVersion("123")
                            .withHendelser(listOf(
                                    SOKNADS_STATUS_MOTTATT.withHendelsestidspunkt(tidspunkt_1),
                                    SOKNADS_STATUS_UNDERBEHANDLING.withHendelsestidspunkt(tidspunkt_2),
                                    SAK1_VEDTAK_FATTET_INNVILGET.withHendelsestidspunkt(tidspunkt_3)
                            ))
            every { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any(), any(), any()) } returns emptyList()

            val model = service.createModel(mockDigisosSak, "token")

            assertThat(model).isNotNull
            assertThat(model.status).isEqualTo(SoknadsStatus.UNDER_BEHANDLING)
            assertThat(model.saker).hasSize(1)
            assertThat(model.historikk).hasSize(4)

            val sak = model.saker.last()
            assertThat(sak.saksStatus).isEqualTo(SaksStatus.UNDER_BEHANDLING)
            assertThat(sak.referanse).isEqualTo(referanse_1)
            assertThat(sak.tittel).isEqualTo(DEFAULT_TITTEL)
            assertThat(sak.vedtak).hasSize(1)
            assertThat(sak.utbetalinger).isEmpty()

            val vedtak = sak.vedtak.last()
            assertThat(vedtak.utfall).isEqualTo(UtfallVedtak.INNVILGET)
            assertThat(vedtak.vedtaksFilUrl).contains("/dokumentlager/nedlasting/$dokumentlagerId_1")

            val hendelse = model.historikk.last()
            assertThat(hendelse.tidspunkt).isEqualTo(toLocalDateTime(tidspunkt_3))
            assertThat(hendelse.tittel).contains("$DEFAULT_TITTEL er ferdig behandlet")
            assertThat(hendelse.url).contains("/dokumentlager/nedlasting/$dokumentlagerId_1")
        }

        @Test
        fun `vedtakFattet FOR saksStatus`() {
            every { innsynService.hentJsonDigisosSoker(any(), any(), any()) } returns
                    JsonDigisosSoker()
                            .withAvsender(avsender)
                            .withVersion("123")
                            .withHendelser(listOf(
                                    SOKNADS_STATUS_MOTTATT.withHendelsestidspunkt(tidspunkt_1),
                                    SOKNADS_STATUS_UNDERBEHANDLING.withHendelsestidspunkt(tidspunkt_2),
                                    SAK1_VEDTAK_FATTET_INNVILGET.withHendelsestidspunkt(tidspunkt_3),
                                    SAK1_SAKS_STATUS_UNDERBEHANDLING.withHendelsestidspunkt(tidspunkt_4)
                            ))
            every { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any(), any(), any()) } returns emptyList()

            val model = service.createModel(mockDigisosSak, "token")

            assertThat(model).isNotNull
            assertThat(model.status).isEqualTo(SoknadsStatus.UNDER_BEHANDLING)
            assertThat(model.saker).hasSize(1)
            assertThat(model.historikk).hasSize(4)

            val sak = model.saker.last()
            assertThat(sak.saksStatus).isEqualTo(SaksStatus.UNDER_BEHANDLING)
            assertThat(sak.referanse).isEqualTo(referanse_1)
            assertThat(sak.tittel)
                    .isEqualTo(tittel_1)
                    .isNotEqualTo(DEFAULT_TITTEL)
            assertThat(sak.vedtak).hasSize(1)

            val vedtak = sak.vedtak.last()
            assertThat(vedtak.utfall).isEqualTo(UtfallVedtak.INNVILGET)
            assertThat(vedtak.vedtaksFilUrl).contains("/dokumentlager/nedlasting/$dokumentlagerId_1")

            val hendelse = model.historikk.last()
            assertThat(hendelse.tidspunkt).isEqualTo(toLocalDateTime(tidspunkt_3))
            assertThat(hendelse.tittel).contains("$DEFAULT_TITTEL er ferdig behandlet")
            assertThat(hendelse.url).contains("/dokumentlager/nedlasting/$dokumentlagerId_1")
        }

        @Test
        fun `saksStatus med 2 vedtakFattet`() {
            every { innsynService.hentJsonDigisosSoker(any(), any(), any()) } returns
                    JsonDigisosSoker()
                            .withAvsender(avsender)
                            .withVersion("123")
                            .withHendelser(listOf(
                                    SOKNADS_STATUS_MOTTATT.withHendelsestidspunkt(tidspunkt_1),
                                    SOKNADS_STATUS_UNDERBEHANDLING.withHendelsestidspunkt(tidspunkt_2),
                                    SAK1_SAKS_STATUS_UNDERBEHANDLING.withHendelsestidspunkt(tidspunkt_3),
                                    SAK1_VEDTAK_FATTET_INNVILGET.withHendelsestidspunkt(tidspunkt_4),
                                    SAK1_VEDTAK_FATTET_AVSLATT.withHendelsestidspunkt(tidspunkt_5)
                            ))
            every { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any(), any(), any()) } returns emptyList()

            val model = service.createModel(mockDigisosSak, "token")

            assertThat(model).isNotNull
            assertThat(model.status).isEqualTo(SoknadsStatus.UNDER_BEHANDLING)
            assertThat(model.saker).hasSize(1)
            assertThat(model.historikk).hasSize(5)

            val sak = model.saker.last()
            assertThat(sak.saksStatus).isEqualTo(SaksStatus.UNDER_BEHANDLING)
            assertThat(sak.referanse).isEqualTo(referanse_1)
            assertThat(sak.tittel).isEqualTo(tittel_1)
            assertThat(sak.vedtak).hasSize(2)

            val vedtak = sak.vedtak[0]
            assertThat(vedtak.utfall).isEqualTo(UtfallVedtak.INNVILGET)
            assertThat(vedtak.vedtaksFilUrl).contains("/dokumentlager/nedlasting/$dokumentlagerId_1")

            val vedtak2 = sak.vedtak[1]
            assertThat(vedtak2.utfall).isEqualTo(UtfallVedtak.AVSLATT)
            assertThat(vedtak2.vedtaksFilUrl).contains("/dokumentlager/nedlasting/$dokumentlagerId_2")
        }

        @Test
        fun `saksStatus uten tittel eller status med vedtakFattet uten utfall`() {
            every { innsynService.hentJsonDigisosSoker(any(), any(), any()) } returns
                    JsonDigisosSoker()
                            .withAvsender(avsender)
                            .withVersion("123")
                            .withHendelser(listOf(
                                    SOKNADS_STATUS_MOTTATT.withHendelsestidspunkt(tidspunkt_1),
                                    SOKNADS_STATUS_UNDERBEHANDLING.withHendelsestidspunkt(tidspunkt_2),
                                    SAK1_UTEN_SAKS_STATUS_ELLER_TITTEL.withHendelsestidspunkt(tidspunkt_3),
                                    SAK1_VEDTAK_FATTET_UTEN_UTFALL.withHendelsestidspunkt(tidspunkt_4)
                            ))
            every { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any(), any(), any()) } returns emptyList()

            val model = service.createModel(mockDigisosSak, "token")

            assertThat(model).isNotNull
            assertThat(model.status).isEqualTo(SoknadsStatus.UNDER_BEHANDLING)
            assertThat(model.saker).hasSize(1)
            assertThat(model.historikk).hasSize(4)

            val sak = model.saker.last()
            assertThat(sak.saksStatus).isEqualTo(SaksStatus.UNDER_BEHANDLING)
            assertThat(sak.referanse).isEqualTo(referanse_1)
            assertThat(sak.tittel).isNull()
            assertThat(sak.vedtak).hasSize(1)

            val vedtak = sak.vedtak[0]
            assertThat(vedtak.utfall).isNull()
            assertThat(vedtak.vedtaksFilUrl).contains("/dokumentlager/nedlasting/$dokumentlagerId_1")

            val hendelse = model.historikk.last()
            assertThat(hendelse.tidspunkt).isEqualTo(toLocalDateTime(tidspunkt_4))
            assertThat(hendelse.tittel).contains("$DEFAULT_TITTEL er ferdig behandlet")
            assertThat(hendelse.url).contains("/dokumentlager/nedlasting/$dokumentlagerId_1")
        }
    }

    @Test
    fun `forelopigSvar skal gi historikk`() {
        every { innsynService.hentJsonDigisosSoker(any(), any(), any()) } returns
                JsonDigisosSoker()
                        .withAvsender(avsender)
                        .withVersion("123")
                        .withHendelser(listOf(
                                SOKNADS_STATUS_MOTTATT.withHendelsestidspunkt(tidspunkt_1),
                                SOKNADS_STATUS_UNDERBEHANDLING.withHendelsestidspunkt(tidspunkt_2),
                                FORELOPIGSVAR.withHendelsestidspunkt(tidspunkt_3)
                        ))
        every { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any(), any(), any()) } returns emptyList()

        val model = service.createModel(mockDigisosSak, "token")

        assertThat(model).isNotNull
        assertThat(model.status).isEqualTo(SoknadsStatus.UNDER_BEHANDLING)
        assertThat(model.saker).isEmpty()
        assertThat(model.historikk).hasSize(4)

        val hendelse = model.historikk.last()
        assertThat(hendelse.tidspunkt).isEqualTo(toLocalDateTime(tidspunkt_3))
        assertThat(hendelse.tittel).contains("Du har fått et brev om saksbehandlingstiden for søknaden din")
        assertThat(hendelse.url).contains("/forsendelse/$svarUtId/$svarUtNr")
    }
}