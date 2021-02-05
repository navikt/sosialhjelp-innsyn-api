package no.nav.sbl.sosialhjelpinnsynapi.event

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.finn.unleash.Unleash
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sbl.soknadsosialhjelp.soknad.JsonSoknad
import no.nav.sbl.sosialhjelpinnsynapi.client.norg.NorgClient
import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.domain.Hendelse
import no.nav.sbl.sosialhjelpinnsynapi.domain.NavEnhet
import no.nav.sbl.sosialhjelpinnsynapi.domain.SaksStatus
import no.nav.sbl.sosialhjelpinnsynapi.domain.SoknadsStatus
import no.nav.sbl.sosialhjelpinnsynapi.domain.UtfallVedtak
import no.nav.sbl.sosialhjelpinnsynapi.service.innsyn.InnsynService
import no.nav.sbl.sosialhjelpinnsynapi.service.saksstatus.DEFAULT_TITTEL
import no.nav.sbl.sosialhjelpinnsynapi.service.vedlegg.InternalVedlegg
import no.nav.sbl.sosialhjelpinnsynapi.service.vedlegg.VEDLEGG_KREVES_STATUS
import no.nav.sbl.sosialhjelpinnsynapi.service.vedlegg.VedleggService
import no.nav.sbl.sosialhjelpinnsynapi.utils.toLocalDateTime
import no.nav.sosialhjelp.api.fiks.DigisosSak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.ZonedDateTime


internal class EventServiceTest {

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
 [x] dokumentasjonkrav
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
            assertThat(model.historikk).hasSize(4)

            val sak = model.saker.last()
            assertThat(sak.saksStatus).isEqualTo(SaksStatus.UNDER_BEHANDLING)
            assertThat(sak.referanse).isEqualTo(referanse_1)
            assertThat(sak.tittel).isEqualTo(tittel_1)
            assertThat(sak.vedtak).isEmpty()
            assertThat(sak.utbetalinger).isEmpty()

            val hendelse = model.historikk.last()
            assertThat(hendelse.tidspunkt).isEqualTo(tidspunkt_3.toLocalDateTime())
            assertThat(hendelse.tittel).contains("${tittel_1.capitalize()} er under behandling")
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
            assertThat(model.historikk).hasSize(4)

            val sak = model.saker.last()
            assertThat(sak.saksStatus).isEqualTo(SaksStatus.UNDER_BEHANDLING)
            assertThat(sak.referanse).isEqualTo(referanse_1)
            assertThat(sak.tittel).isNull()
            assertThat(sak.vedtak).isEmpty()
            assertThat(sak.utbetalinger).isEmpty()

            val hendelse = model.historikk.last()
            assertThat(hendelse.tidspunkt).isEqualTo(tidspunkt_3.toLocalDateTime())
            assertThat(hendelse.tittel).contains("Saken din er under behandling")
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
            assertThat(model.historikk).hasSize(5)

            val sak = model.saker.last()
            assertThat(sak.saksStatus).isEqualTo(SaksStatus.UNDER_BEHANDLING)
            assertThat(sak.referanse).isEqualTo(referanse_1)
            assertThat(sak.tittel).isEqualTo(tittel_1)
            assertThat(sak.vedtak).hasSize(1)
            assertThat(sak.utbetalinger).isEmpty()

            val vedtak = sak.vedtak.last()
            assertThat(vedtak.utfall).isEqualTo(UtfallVedtak.INNVILGET)
            assertThat(vedtak.vedtaksFilUrl).contains("/dokumentlager/nedlasting/niva4/$dokumentlagerId_1")

            val hendelse = model.historikk.last()
            assertThat(hendelse.tidspunkt).isEqualTo(tidspunkt_4.toLocalDateTime())
            assertThat(hendelse.tittel).contains("$tittel_1 er ferdig behandlet")
            assertThat(hendelse.url?.link).contains("/dokumentlager/nedlasting/niva4/$dokumentlagerId_1")
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
            assertThat(vedtak.vedtaksFilUrl).contains("/dokumentlager/nedlasting/niva4/$dokumentlagerId_1")

            val hendelse = model.historikk.last()
            assertThat(hendelse.tidspunkt).isEqualTo(tidspunkt_3.toLocalDateTime())
            assertThat(hendelse.tittel).contains("$DEFAULT_TITTEL er ferdig behandlet")
            assertThat(hendelse.url?.link).contains("/dokumentlager/nedlasting/niva4/$dokumentlagerId_1")
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
            assertThat(vedtak.vedtaksFilUrl).contains("/dokumentlager/nedlasting/niva4/$dokumentlagerId_1")

            val hendelse = model.historikk.last()
            assertThat(hendelse.tidspunkt).isEqualTo(tidspunkt_3.toLocalDateTime())
            assertThat(hendelse.tittel).contains("$DEFAULT_TITTEL er ferdig behandlet")
            assertThat(hendelse.url?.link).contains("/dokumentlager/nedlasting/niva4/$dokumentlagerId_1")
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
            assertThat(model.historikk).hasSize(6)

            val sak = model.saker.last()
            assertThat(sak.saksStatus).isEqualTo(SaksStatus.UNDER_BEHANDLING)
            assertThat(sak.referanse).isEqualTo(referanse_1)
            assertThat(sak.tittel).isEqualTo(tittel_1)
            assertThat(sak.vedtak).hasSize(2)

            val vedtak = sak.vedtak[0]
            assertThat(vedtak.utfall).isEqualTo(UtfallVedtak.INNVILGET)
            assertThat(vedtak.vedtaksFilUrl).contains("/dokumentlager/nedlasting/niva4/$dokumentlagerId_1")

            val vedtak2 = sak.vedtak[1]
            assertThat(vedtak2.utfall).isEqualTo(UtfallVedtak.AVSLATT)
            assertThat(vedtak2.vedtaksFilUrl).contains("/dokumentlager/nedlasting/niva4/$dokumentlagerId_2")
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
            assertThat(model.historikk).hasSize(5)

            val sak = model.saker.last()
            assertThat(sak.saksStatus).isEqualTo(SaksStatus.UNDER_BEHANDLING)
            assertThat(sak.referanse).isEqualTo(referanse_1)
            assertThat(sak.tittel).isNull()
            assertThat(sak.vedtak).hasSize(1)

            val vedtak = sak.vedtak[0]
            assertThat(vedtak.utfall).isNull()
            assertThat(vedtak.vedtaksFilUrl).contains("/dokumentlager/nedlasting/niva4/$dokumentlagerId_1")

            val hendelse = model.historikk.last()
            assertThat(hendelse.tidspunkt).isEqualTo(tidspunkt_4.toLocalDateTime())
            assertThat(hendelse.tittel).contains("$DEFAULT_TITTEL er ferdig behandlet")
            assertThat(hendelse.url?.link).contains("/dokumentlager/nedlasting/niva4/$dokumentlagerId_1")
        }

        @Test
        internal fun `saksStatus ikke_innsyn`() {
            every { innsynService.hentJsonDigisosSoker(any(), any(), any()) } returns
                    JsonDigisosSoker()
                            .withAvsender(avsender)
                            .withVersion("123")
                            .withHendelser(listOf(
                                    SOKNADS_STATUS_MOTTATT.withHendelsestidspunkt(tidspunkt_1),
                                    SOKNADS_STATUS_UNDERBEHANDLING.withHendelsestidspunkt(tidspunkt_2),
                                    SAK1_SAKS_STATUS_IKKEINNSYN.withHendelsestidspunkt(tidspunkt_3)
                            ))
            every { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any(), any(), any()) } returns emptyList()

            val model = service.createModel(mockDigisosSak, "token")

            assertThat(model).isNotNull
            assertThat(model.status).isEqualTo(SoknadsStatus.UNDER_BEHANDLING)
            assertThat(model.saker).hasSize(1)
            assertThat(model.historikk).hasSize(4)

            val sak = model.saker.last()
            assertThat(sak.saksStatus).isEqualTo(SaksStatus.IKKE_INNSYN)
            assertThat(sak.referanse).isEqualTo(referanse_1)
            assertThat(sak.tittel).isEqualTo(tittel_1)

            val hendelse = model.historikk.last()
            assertThat(hendelse.tidspunkt).isEqualTo(tidspunkt_3.toLocalDateTime())
            assertThat(hendelse.tittel).contains("Vi kan ikke vise behandlingsstatus for $tittel_1 digitalt.")
        }

        @Test
        internal fun `saksStatus endres fra under_behandling til ikke_innsyn`() {
            every { innsynService.hentJsonDigisosSoker(any(), any(), any()) } returns
                    JsonDigisosSoker()
                            .withAvsender(avsender)
                            .withVersion("123")
                            .withHendelser(listOf(
                                    SOKNADS_STATUS_MOTTATT.withHendelsestidspunkt(tidspunkt_1),
                                    SOKNADS_STATUS_UNDERBEHANDLING.withHendelsestidspunkt(tidspunkt_2),
                                    SAK1_SAKS_STATUS_UNDERBEHANDLING.withHendelsestidspunkt(tidspunkt_3),
                                    SAK1_SAKS_STATUS_IKKEINNSYN.withHendelsestidspunkt(tidspunkt_4)
                            ))
            every { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any(), any(), any()) } returns emptyList()

            val model = service.createModel(mockDigisosSak, "token")

            assertThat(model).isNotNull
            assertThat(model.status).isEqualTo(SoknadsStatus.UNDER_BEHANDLING)
            assertThat(model.saker).hasSize(1)
            assertThat(model.historikk).hasSize(5)

            val sak = model.saker.last()
            assertThat(sak.saksStatus).isEqualTo(SaksStatus.IKKE_INNSYN)
            assertThat(sak.referanse).isEqualTo(referanse_1)
            assertThat(sak.tittel).isEqualTo(tittel_1)

            val hendelse = model.historikk.last()
            assertThat(hendelse.tidspunkt).isEqualTo(tidspunkt_4.toLocalDateTime())
            assertThat(hendelse.tittel).contains("Vi kan ikke vise behandlingsstatus for $tittel_1 digitalt.")
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
        assertThat(hendelse.tidspunkt).isEqualTo(tidspunkt_3.toLocalDateTime())
        assertThat(hendelse.tittel).contains("Du har fått et brev om saksbehandlingstiden for søknaden din")
        assertThat(hendelse.url?.link).contains("/forsendelse/$svarUtId/$svarUtNr")
    }

    @Test
    fun `At soknad sendt hendelse blir lagt til og at linkTekst er Vis soknaden`() {
        every { innsynService.hentJsonDigisosSoker(any(), any(), any()) } returns
                JsonDigisosSoker()
                        .withAvsender(avsender)
                        .withVersion("123")
        every { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any(), any(), any()) } returns emptyList()
        every { mockDigisosSak.originalSoknadNAV?.soknadDokument?.dokumentlagerDokumentId } returns "asdf"

        val model = service.createModel(mockDigisosSak, "token")
        assertThat(model).isNotNull
        val hendelse: Hendelse = model.historikk[0]
        assertThat(hendelse).isNotNull
        assertThat(hendelse.tittel).contains("Søknaden med vedlegg er sendt til The Office")
        assertThat(hendelse.url?.linkTekst).isEqualTo("Vis søknaden")
    }

    @Test
    fun `At soknad sendt hendelse blir lagt til selv om soknad pdf ikke eksisterer`() {
        every { innsynService.hentJsonDigisosSoker(any(), any(), any()) } returns
                JsonDigisosSoker()
                        .withAvsender(avsender)
                        .withVersion("123")
        every { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any(), any(), any()) } returns emptyList()
        every { mockDigisosSak.originalSoknadNAV?.soknadDokument?.dokumentlagerDokumentId } returns null

        val model = service.createModel(mockDigisosSak, "token")
        assertThat(model).isNotNull
        val hendelse = model.historikk[0]
        assertThat(hendelse).isNotNull
        assertThat(hendelse.tittel).contains("Søknaden med vedlegg er sendt til The Office")
        assertThat(hendelse.url).isNull()
    }

    @Test
    fun `skal legge til dokumentasjonkrav fra søknaden`() {
        every { innsynService.hentJsonDigisosSoker(any(), any(), any()) } returns
                JsonDigisosSoker()
                        .withAvsender(avsender)
                        .withVersion("123")
                        .withHendelser(listOf(
                                SOKNADS_STATUS_MOTTATT.withHendelsestidspunkt(tidspunkt_1)
                        ))
        every { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any(), any(), any()) } returns listOf(
                InternalVedlegg(
                        type = "statsborgerskap",
                        tilleggsinfo = "dokumentasjon",
                        dokumentInfoList = emptyList(),
                        tidspunktLastetOpp = LocalDateTime.now()
                )
        )

        val model = service.createModel(mockDigisosSak, "token")
        assertThat(model).isNotNull
        assertThat(model.oppgaver).hasSize(1)
        verify(exactly = 1) { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any(), any(), any()) }

        val saksoversiktModel = service.createSaksoversiktModel(mockDigisosSak, "token")
        assertThat(saksoversiktModel).isNotNull
        assertThat(saksoversiktModel.oppgaver).hasSize(1)
        verify(exactly = 2) { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any(), any(), any()) }
    }

    @Test
    fun `skal ikke vise krav fra soknaden hvis soknaden ble sendt for mer enn 30 dager siden`() {
        val tidspunktSendt31dagerSiden = ZonedDateTime.now().minusDays(31).toEpochSecond() * 1000L
        every { mockDigisosSak.originalSoknadNAV?.timestampSendt } returns tidspunktSendt31dagerSiden
        every { innsynService.hentJsonDigisosSoker(any(), any(), any()) } returns
                JsonDigisosSoker()
                        .withAvsender(avsender)
                        .withVersion("123")
                        .withHendelser(listOf(
                                SOKNADS_STATUS_MOTTATT.withHendelsestidspunkt(tidspunkt_1)
                        ))
        every { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any(), any(), any()) } returns listOf(
                InternalVedlegg(
                        type = "statsborgerskap",
                        tilleggsinfo = "dokumentasjon",
                        dokumentInfoList = emptyList(),
                        tidspunktLastetOpp = LocalDateTime.now()
                )
        )

        val model = service.createModel(mockDigisosSak, "token")
        assertThat(model).isNotNull
        assertThat(model.oppgaver).hasSize(0)
        verify(exactly = 0) { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any(), any(), any()) }

        val saksoversiktModel = service.createSaksoversiktModel(mockDigisosSak, "token")
        assertThat(saksoversiktModel).isNotNull
        assertThat(saksoversiktModel.oppgaver).hasSize(0)
        verify(exactly = 0) { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any(), any(), any()) }
    }

}