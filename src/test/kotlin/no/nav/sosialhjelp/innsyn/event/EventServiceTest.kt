package no.nav.sosialhjelp.innsyn.event

import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.finn.unleash.Unleash
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonUtbetaling
import no.nav.sbl.soknadsosialhjelp.soknad.JsonSoknad
import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.innsyn.app.ClientProperties
import no.nav.sosialhjelp.innsyn.digisossak.saksstatus.SaksStatusService.Companion.DEFAULT_SAK_TITTEL
import no.nav.sosialhjelp.innsyn.domain.Hendelse
import no.nav.sosialhjelp.innsyn.domain.HendelseTekstType
import no.nav.sosialhjelp.innsyn.domain.InternalDigisosSoker
import no.nav.sosialhjelp.innsyn.domain.SaksStatus
import no.nav.sosialhjelp.innsyn.domain.SoknadsStatus
import no.nav.sosialhjelp.innsyn.domain.Utbetaling
import no.nav.sosialhjelp.innsyn.domain.UtbetalingsStatus
import no.nav.sosialhjelp.innsyn.domain.UtfallVedtak
import no.nav.sosialhjelp.innsyn.navenhet.NavEnhet
import no.nav.sosialhjelp.innsyn.navenhet.NorgClient
import no.nav.sosialhjelp.innsyn.utils.toLocalDateTime
import no.nav.sosialhjelp.innsyn.vedlegg.InternalVedlegg
import no.nav.sosialhjelp.innsyn.vedlegg.VEDLEGG_KREVES_STATUS
import no.nav.sosialhjelp.innsyn.vedlegg.VedleggService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

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

    private val jsonDigisosSoker: JsonDigisosSoker = mockk()
    private val model: InternalDigisosSoker = mockk()
    private val digisosSak: DigisosSak = mockk()
    private val log: Logger = mockk()

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
        every { innsynService.hentOriginalSoknad(any(), any()) } returns mockJsonSoknad
        every { norgClient.hentNavEnhet(enhetsnr) } returns mockNavEnhet
        every { digisosSak.fiksDigisosId } returns "fiksDigisosId"
        every { digisosSak.kommunenummer } returns "1234"

        resetHendelser()
    }

/* Test-caser:
 [x] ingen innsyn, ingen sendt soknad
 [x] ingen innsyn, sendt soknad -> status SENDT
 [x] status mottatt
 [x] status under behandling
 [x] status ferdigbehandlet
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
        every { innsynService.hentJsonDigisosSoker(any(), any()) } returns null
        every { innsynService.hentOriginalSoknad(any(), any()) } returns null
        every { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any(), any()) } returns emptyList()

        val model = service.createModel(mockDigisosSak, "token")

        assertThat(model).isNotNull
        assertThat(model.status).isEqualTo(SoknadsStatus.SENDT)
        assertThat(model.historikk).hasSize(0)
    }

    @Test
    fun `ingen innsyn `() {
        every { mockDigisosSak.digisosSoker } returns null
        every { innsynService.hentJsonDigisosSoker(any(), "token") } returns null
        every { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any(), any()) } returns emptyList()

        val model = service.createModel(mockDigisosSak, "token")

        assertThat(model).isNotNull
        assertThat(model.historikk).hasSize(1)
    }

    @Nested
    inner class SaksStatusVedtakFattet {

        @Test
        fun `saksStatus UTEN vedtakFattet`() {
            every { innsynService.hentJsonDigisosSoker(any(), any()) } returns
                JsonDigisosSoker()
                    .withAvsender(avsender)
                    .withVersion("123")
                    .withHendelser(
                        listOf(
                            SOKNADS_STATUS_MOTTATT.withHendelsestidspunkt(tidspunkt_1),
                            SOKNADS_STATUS_UNDERBEHANDLING.withHendelsestidspunkt(tidspunkt_2),
                            SAK1_SAKS_STATUS_UNDERBEHANDLING.withHendelsestidspunkt(tidspunkt_3)
                        )
                    )
            every { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any(), any()) } returns emptyList()

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
            assertThat(hendelse.hendelseType).isEqualTo(HendelseTekstType.SAK_UNDER_BEHANDLING_MED_TITTEL)
            assertThat(hendelse.tekstArgument).isEqualTo(tittel_1)
        }

        @Test
        fun `saksStatus UTEN tittel eller status`() {
            every { innsynService.hentJsonDigisosSoker(any(), any()) } returns
                JsonDigisosSoker()
                    .withAvsender(avsender)
                    .withVersion("123")
                    .withHendelser(
                        listOf(
                            SOKNADS_STATUS_MOTTATT.withHendelsestidspunkt(tidspunkt_1),
                            SOKNADS_STATUS_UNDERBEHANDLING.withHendelsestidspunkt(tidspunkt_2),
                            SAK1_UTEN_SAKS_STATUS_ELLER_TITTEL.withHendelsestidspunkt(tidspunkt_3)
                        )
                    )
            every { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any(), any()) } returns emptyList()

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
            assertThat(hendelse.hendelseType).isEqualTo(HendelseTekstType.SAK_UNDER_BEHANDLING_UTEN_TITTEL)
            assertThat(hendelse.tekstArgument).isNull()
        }

        @Test
        fun `saksStatus FOR vedtakFattet`() {
            every { innsynService.hentJsonDigisosSoker(any(), any()) } returns
                JsonDigisosSoker()
                    .withAvsender(avsender)
                    .withVersion("123")
                    .withHendelser(
                        listOf(
                            SOKNADS_STATUS_MOTTATT.withHendelsestidspunkt(tidspunkt_1),
                            SOKNADS_STATUS_UNDERBEHANDLING.withHendelsestidspunkt(tidspunkt_2),
                            SAK1_SAKS_STATUS_UNDERBEHANDLING.withHendelsestidspunkt(tidspunkt_3),
                            SAK1_VEDTAK_FATTET_INNVILGET.withHendelsestidspunkt(tidspunkt_4)
                        )
                    )
            every { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any(), any()) } returns emptyList()

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
            assertThat(hendelse.hendelseType).isEqualTo(HendelseTekstType.SAK_FERDIGBEHANDLET_MED_TITTEL)
            assertThat(hendelse.tekstArgument).isEqualTo(tittel_1)
            assertThat(hendelse.url?.link).contains("/dokumentlager/nedlasting/niva4/$dokumentlagerId_1")
        }

        @Test
        fun `vedtakFattet UTEN saksStatus`() {
            every { innsynService.hentJsonDigisosSoker(any(), any()) } returns
                JsonDigisosSoker()
                    .withAvsender(avsender)
                    .withVersion("123")
                    .withHendelser(
                        listOf(
                            SOKNADS_STATUS_MOTTATT.withHendelsestidspunkt(tidspunkt_1),
                            SOKNADS_STATUS_UNDERBEHANDLING.withHendelsestidspunkt(tidspunkt_2),
                            SAK1_VEDTAK_FATTET_INNVILGET.withHendelsestidspunkt(tidspunkt_3)
                        )
                    )
            every { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any(), any()) } returns emptyList()

            val model = service.createModel(mockDigisosSak, "token")

            assertThat(model).isNotNull
            assertThat(model.status).isEqualTo(SoknadsStatus.UNDER_BEHANDLING)
            assertThat(model.saker).hasSize(1)
            assertThat(model.historikk).hasSize(4)

            val sak = model.saker.last()
            assertThat(sak.saksStatus).isEqualTo(SaksStatus.UNDER_BEHANDLING)
            assertThat(sak.referanse).isEqualTo(referanse_1)
            assertThat(sak.tittel).isEqualTo(DEFAULT_SAK_TITTEL)
            assertThat(sak.vedtak).hasSize(1)
            assertThat(sak.utbetalinger).isEmpty()

            val vedtak = sak.vedtak.last()
            assertThat(vedtak.utfall).isEqualTo(UtfallVedtak.INNVILGET)
            assertThat(vedtak.vedtaksFilUrl).contains("/dokumentlager/nedlasting/niva4/$dokumentlagerId_1")

            val hendelse = model.historikk.last()
            assertThat(hendelse.tidspunkt).isEqualTo(tidspunkt_3.toLocalDateTime())
            assertThat(hendelse.hendelseType).isEqualTo(HendelseTekstType.SAK_FERDIGBEHANDLET_MED_TITTEL)
            assertThat(hendelse.tekstArgument).isEqualTo(DEFAULT_SAK_TITTEL) // eller DEFAULT_SAKSTITTEL
            assertThat(hendelse.url?.link).contains("/dokumentlager/nedlasting/niva4/$dokumentlagerId_1")
        }

        @Test
        fun `vedtakFattet FOR saksStatus`() {
            every { innsynService.hentJsonDigisosSoker(any(), any()) } returns
                JsonDigisosSoker()
                    .withAvsender(avsender)
                    .withVersion("123")
                    .withHendelser(
                        listOf(
                            SOKNADS_STATUS_MOTTATT.withHendelsestidspunkt(tidspunkt_1),
                            SOKNADS_STATUS_UNDERBEHANDLING.withHendelsestidspunkt(tidspunkt_2),
                            SAK1_VEDTAK_FATTET_INNVILGET.withHendelsestidspunkt(tidspunkt_3),
                            SAK1_SAKS_STATUS_UNDERBEHANDLING.withHendelsestidspunkt(tidspunkt_4)
                        )
                    )
            every { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any(), any()) } returns emptyList()

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
                .isNotEqualTo(DEFAULT_SAK_TITTEL)
            assertThat(sak.vedtak).hasSize(1)

            val vedtak = sak.vedtak.last()
            assertThat(vedtak.utfall).isEqualTo(UtfallVedtak.INNVILGET)
            assertThat(vedtak.vedtaksFilUrl).contains("/dokumentlager/nedlasting/niva4/$dokumentlagerId_1")

            val hendelse = model.historikk.last()
            assertThat(hendelse.tidspunkt).isEqualTo(tidspunkt_3.toLocalDateTime())
            assertThat(hendelse.hendelseType).isEqualTo(HendelseTekstType.SAK_FERDIGBEHANDLET_MED_TITTEL)
            assertThat(hendelse.url?.link).contains("/dokumentlager/nedlasting/niva4/$dokumentlagerId_1")
        }

        @Test
        fun `saksStatus med 2 vedtakFattet`() {
            every { innsynService.hentJsonDigisosSoker(any(), any()) } returns
                JsonDigisosSoker()
                    .withAvsender(avsender)
                    .withVersion("123")
                    .withHendelser(
                        listOf(
                            SOKNADS_STATUS_MOTTATT.withHendelsestidspunkt(tidspunkt_1),
                            SOKNADS_STATUS_UNDERBEHANDLING.withHendelsestidspunkt(tidspunkt_2),
                            SAK1_SAKS_STATUS_UNDERBEHANDLING.withHendelsestidspunkt(tidspunkt_3),
                            SAK1_VEDTAK_FATTET_INNVILGET.withHendelsestidspunkt(tidspunkt_4),
                            SAK1_VEDTAK_FATTET_AVSLATT.withHendelsestidspunkt(tidspunkt_5)
                        )
                    )
            every { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any(), any()) } returns emptyList()

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
            every { innsynService.hentJsonDigisosSoker(any(), any()) } returns
                JsonDigisosSoker()
                    .withAvsender(avsender)
                    .withVersion("123")
                    .withHendelser(
                        listOf(
                            SOKNADS_STATUS_MOTTATT.withHendelsestidspunkt(tidspunkt_1),
                            SOKNADS_STATUS_UNDERBEHANDLING.withHendelsestidspunkt(tidspunkt_2),
                            SAK1_UTEN_SAKS_STATUS_ELLER_TITTEL.withHendelsestidspunkt(tidspunkt_3),
                            SAK1_VEDTAK_FATTET_UTEN_UTFALL.withHendelsestidspunkt(tidspunkt_4)
                        )
                    )
            every { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any(), any()) } returns emptyList()

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
            assertThat(hendelse.hendelseType).isEqualTo(HendelseTekstType.SAK_FERDIGBEHANDLET_UTEN_TITTEL)
            assertThat(hendelse.url?.link).contains("/dokumentlager/nedlasting/niva4/$dokumentlagerId_1")
        }

        @Test
        internal fun `saksStatus ikke_innsyn`() {
            every { innsynService.hentJsonDigisosSoker(any(), any()) } returns
                JsonDigisosSoker()
                    .withAvsender(avsender)
                    .withVersion("123")
                    .withHendelser(
                        listOf(
                            SOKNADS_STATUS_MOTTATT.withHendelsestidspunkt(tidspunkt_1),
                            SOKNADS_STATUS_UNDERBEHANDLING.withHendelsestidspunkt(tidspunkt_2),
                            SAK1_SAKS_STATUS_IKKEINNSYN.withHendelsestidspunkt(tidspunkt_3)
                        )
                    )
            every { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any(), any()) } returns emptyList()

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
            assertThat(hendelse.hendelseType).isEqualTo(HendelseTekstType.SAK_KAN_IKKE_VISE_STATUS_MED_TITTEL)
            assertThat(hendelse.tekstArgument).isEqualTo(tittel_1)
        }

        @Test
        internal fun `saksStatus endres fra under_behandling til ikke_innsyn`() {
            every { innsynService.hentJsonDigisosSoker(any(), any()) } returns
                JsonDigisosSoker()
                    .withAvsender(avsender)
                    .withVersion("123")
                    .withHendelser(
                        listOf(
                            SOKNADS_STATUS_MOTTATT.withHendelsestidspunkt(tidspunkt_1),
                            SOKNADS_STATUS_UNDERBEHANDLING.withHendelsestidspunkt(tidspunkt_2),
                            SAK1_SAKS_STATUS_UNDERBEHANDLING.withHendelsestidspunkt(tidspunkt_3),
                            SAK1_SAKS_STATUS_IKKEINNSYN.withHendelsestidspunkt(tidspunkt_4)
                        )
                    )
            every { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any(), any()) } returns emptyList()

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
            assertThat(hendelse.hendelseType).isEqualTo(HendelseTekstType.SOKNAD_KAN_IKKE_VISE_STATUS_MED_TITTEL)
            assertThat(hendelse.tekstArgument).isEqualTo(tittel_1)
        }
    }

    @Test
    fun `forelopigSvar skal gi historikk`() {
        every { innsynService.hentJsonDigisosSoker(any(), any()) } returns
            JsonDigisosSoker()
                .withAvsender(avsender)
                .withVersion("123")
                .withHendelser(
                    listOf(
                        SOKNADS_STATUS_MOTTATT.withHendelsestidspunkt(tidspunkt_1),
                        SOKNADS_STATUS_UNDERBEHANDLING.withHendelsestidspunkt(tidspunkt_2),
                        FORELOPIGSVAR.withHendelsestidspunkt(tidspunkt_3)
                    )
                )
        every { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any(), any()) } returns emptyList()

        val model = service.createModel(mockDigisosSak, "token")

        assertThat(model).isNotNull
        assertThat(model.status).isEqualTo(SoknadsStatus.UNDER_BEHANDLING)
        assertThat(model.saker).isEmpty()
        assertThat(model.historikk).hasSize(4)

        val hendelse = model.historikk.last()
        assertThat(hendelse.tidspunkt).isEqualTo(tidspunkt_3.toLocalDateTime())
        assertThat(hendelse.hendelseType).isEqualTo(HendelseTekstType.BREV_OM_SAKSBEANDLINGSTID)
        assertThat(hendelse.url?.link).contains("/forsendelse/$svarUtId/$svarUtNr")
    }

    @Test
    fun `At soknad sendt hendelse blir lagt til og at linkTekst er Vis soknaden`() {
        every { innsynService.hentJsonDigisosSoker(any(), any()) } returns
            JsonDigisosSoker()
                .withAvsender(avsender)
                .withVersion("123")
        every { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any(), any()) } returns emptyList()
        every { mockDigisosSak.originalSoknadNAV?.soknadDokument?.dokumentlagerDokumentId } returns "asdf"

        val model = service.createModel(mockDigisosSak, "token")
        assertThat(model).isNotNull
        val hendelse: Hendelse = model.historikk[0]
        assertThat(hendelse).isNotNull
        assertThat(hendelse.hendelseType).isEqualTo(HendelseTekstType.SOKNAD_SEND_TIL_KONTOR)
        assertThat(hendelse.tekstArgument).isEqualTo("The Office")
        assertThat(hendelse.url?.linkTekst).isEqualTo("Vis søknaden")
    }

    @Test
    fun `At soknad sendt hendelse blir lagt til selv om soknad pdf ikke eksisterer`() {
        every { innsynService.hentJsonDigisosSoker(any(), any()) } returns
            JsonDigisosSoker()
                .withAvsender(avsender)
                .withVersion("123")
        every { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any(), any()) } returns emptyList()
        every { mockDigisosSak.originalSoknadNAV?.soknadDokument?.dokumentlagerDokumentId } returns null

        val model = service.createModel(mockDigisosSak, "token")
        assertThat(model).isNotNull
        val hendelse = model.historikk[0]
        assertThat(hendelse).isNotNull
        assertThat(hendelse.hendelseType).isEqualTo(HendelseTekstType.SOKNAD_SEND_TIL_KONTOR)
        assertThat(hendelse.tekstArgument).isEqualTo("The Office")
        assertThat(hendelse.url).isNull()
    }

    @Test
    fun `skal legge til dokumentasjonkrav fra soknaden`() {
        every { innsynService.hentJsonDigisosSoker(any(), any()) } returns
            JsonDigisosSoker()
                .withAvsender(avsender)
                .withVersion("123")
                .withHendelser(
                    listOf(
                        SOKNADS_STATUS_MOTTATT.withHendelsestidspunkt(tidspunkt_1)
                    )
                )
        every { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any(), any()) } returns listOf(
            InternalVedlegg(
                type = "statsborgerskap",
                tilleggsinfo = "dokumentasjon",
                null,
                null,
                dokumentInfoList = mutableListOf(),
                tidspunktLastetOpp = LocalDateTime.now(),
                null
            )
        )

        val model = service.createModel(mockDigisosSak, "token")
        assertThat(model).isNotNull
        assertThat(model.oppgaver).hasSize(1)
        verify(exactly = 1) { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any(), any()) }

        val saksoversiktModel = service.createSaksoversiktModel(mockDigisosSak, "token")
        assertThat(saksoversiktModel).isNotNull
        assertThat(saksoversiktModel.oppgaver).hasSize(1)
        verify(exactly = 2) { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any(), any()) }
    }

    @Test
    fun `skal ikke vise krav fra soknaden hvis soknaden ble sendt for mer enn 30 dager siden`() {
        val tidspunktSendt31dagerSiden = ZonedDateTime.now().minusDays(31).toEpochSecond() * 1000L
        every { mockDigisosSak.originalSoknadNAV?.timestampSendt } returns tidspunktSendt31dagerSiden
        every { innsynService.hentJsonDigisosSoker(any(), any()) } returns
            JsonDigisosSoker()
                .withAvsender(avsender)
                .withVersion("123")
                .withHendelser(
                    listOf(
                        SOKNADS_STATUS_MOTTATT.withHendelsestidspunkt(tidspunkt_1)
                    )
                )
        every { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any(), any()) } returns listOf(
            InternalVedlegg(
                type = "statsborgerskap",
                tilleggsinfo = "dokumentasjon",
                null,
                null,
                dokumentInfoList = mutableListOf(),
                tidspunktLastetOpp = LocalDateTime.now(),
                null
            )
        )

        val model = service.createModel(mockDigisosSak, "token")
        assertThat(model).isNotNull
        assertThat(model.oppgaver).hasSize(0)
        verify(exactly = 0) { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any(), any()) }

        val saksoversiktModel = service.createSaksoversiktModel(mockDigisosSak, "token")
        assertThat(saksoversiktModel).isNotNull
        assertThat(saksoversiktModel.oppgaver).hasSize(0)
        verify(exactly = 0) { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any(), any()) }
    }

    @Test
    fun `skal ikke logge nar vi ikke har utbetalinger`() {
        val utbetalinger = mutableListOf<Utbetaling>()
        every { model.utbetalinger } returns utbetalinger

        service.logTekniskSperre(jsonDigisosSoker, model, digisosSak, log)

        verify(exactly = 0) { log.info(any()) }
    }

    @Test
    fun `skal ikke logge nar vi ikke har gamle utbetalinger`() {
        val nyUtbetaling = Utbetaling("referanse", UtbetalingsStatus.PLANLAGT_UTBETALING, BigDecimal.TEN, "Nødhjelp", LocalDate.now(), null, null, null, null, null, false, null, null, mutableListOf(), mutableListOf(), LocalDateTime.now())
        val utbetalinger = mutableListOf(nyUtbetaling)
        every { model.utbetalinger } returns utbetalinger

        service.logTekniskSperre(jsonDigisosSoker, model, digisosSak, log)

        verify(exactly = 0) { log.info(any()) }
    }

    @Test
    fun `skal logge selv nar vi har gamel utbetaling som er utbetalt i tide`() {
        val nyUtbetaling = Utbetaling("referanse", UtbetalingsStatus.UTBETALT, BigDecimal.TEN, "Nødhjelp", LocalDate.now().minusDays(2), LocalDate.now().minusDays(2), null, null, null, null, false, null, null, mutableListOf(), mutableListOf(), LocalDateTime.now())
        val utbetalinger = mutableListOf(nyUtbetaling)
        every { model.utbetalinger } returns utbetalinger
        val nyUtbetalingHendelse = JsonUtbetaling()
            .withUtbetalingsreferanse("referanse")
            .withStatus(JsonUtbetaling.Status.UTBETALT)
            .withBelop(10.0)
            .withBeskrivelse("Nødhjelp")
            .withForfallsdato(LocalDate.now().minusDays(20).toString())
            .withUtbetalingsdato(LocalDate.now().toString())
            .withHendelsestidspunkt(ZonedDateTime.now(ZoneId.of("UTC")).format(DateTimeFormatter.ISO_DATE_TIME))
        val hendelser = listOf(nyUtbetalingHendelse)
        every { jsonDigisosSoker.hendelser } returns hendelser
        every { log.info(any()) } just Runs

        service.logTekniskSperre(jsonDigisosSoker, model, digisosSak, log)

        verify(exactly = 1) { log.info(any()) }
    }

    @Test
    fun `skal logge selv nar vi har gamel utbetaling som er stoppet i tide`() {
        val nyUtbetaling = Utbetaling("referanse", UtbetalingsStatus.STOPPET, BigDecimal.TEN, "Nødhjelp", LocalDate.now().minusDays(2), null, LocalDate.now().minusDays(2), null, null, null, false, null, null, mutableListOf(), mutableListOf(), LocalDateTime.now())
        val utbetalinger = mutableListOf(nyUtbetaling)
        every { model.utbetalinger } returns utbetalinger
        val nyUtbetalingHendelse = JsonUtbetaling()
            .withUtbetalingsreferanse("referanse")
            .withStatus(JsonUtbetaling.Status.UTBETALT)
            .withBelop(10.0)
            .withBeskrivelse("Nødhjelp")
            .withForfallsdato(LocalDate.now().minusDays(20).toString())
            .withUtbetalingsdato(LocalDate.now().toString())
            .withHendelsestidspunkt(ZonedDateTime.now(ZoneId.of("UTC")).format(DateTimeFormatter.ISO_DATE_TIME))
        val hendelser = listOf(nyUtbetalingHendelse)
        every { jsonDigisosSoker.hendelser } returns hendelser
        every { log.info(any()) } just Runs

        service.logTekniskSperre(jsonDigisosSoker, model, digisosSak, log)

        verify(exactly = 1) { log.info(any()) }
    }

    @Test
    fun `skal logge selv nar vi har gamel forfallsdato som er utbetalt samtidig som event er opprettet`() {
        val nyUtbetaling = Utbetaling("referanse", UtbetalingsStatus.UTBETALT, BigDecimal.TEN, "Nødhjelp", LocalDate.now().minusDays(20), LocalDate.now(), null, null, null, null, false, null, null, mutableListOf(), mutableListOf(), LocalDateTime.now())
        val utbetalinger = mutableListOf(nyUtbetaling)
        every { model.utbetalinger } returns utbetalinger
        val nyUtbetalingHendelse = JsonUtbetaling()
            .withUtbetalingsreferanse("referanse")
            .withStatus(JsonUtbetaling.Status.UTBETALT)
            .withBelop(10.0)
            .withBeskrivelse("Nødhjelp")
            .withForfallsdato(LocalDate.now().minusDays(20).toString())
            .withUtbetalingsdato(LocalDate.now().toString())
            .withHendelsestidspunkt(ZonedDateTime.now(ZoneId.of("UTC")).format(DateTimeFormatter.ISO_DATE_TIME))
        val hendelser = listOf(nyUtbetalingHendelse)
        every { jsonDigisosSoker.hendelser } returns hendelser
        every { log.info(any()) } just Runs

        service.logTekniskSperre(jsonDigisosSoker, model, digisosSak, log)

        verify(exactly = 1) { log.info(any()) }
    }

    @Test
    fun `skal logge selv nar vi har gamel forfallsdato som er utbetalt fort nok etter at event er opprettet`() {
        val nyUtbetaling = Utbetaling("referanse", UtbetalingsStatus.UTBETALT, BigDecimal.TEN, "Nødhjelp", LocalDate.now().minusDays(20), LocalDate.now(), null, null, null, null, false, null, null, mutableListOf(), mutableListOf(), LocalDateTime.now())
        val utbetalinger = mutableListOf(nyUtbetaling)
        every { model.utbetalinger } returns utbetalinger
        val nyUtbetalingHendelse1 = JsonUtbetaling()
            .withUtbetalingsreferanse("referanse")
            .withStatus(JsonUtbetaling.Status.PLANLAGT_UTBETALING)
            .withBelop(10.0)
            .withBeskrivelse("Nødhjelp")
            .withForfallsdato(LocalDate.now().minusDays(20).toString())
            .withUtbetalingsdato(null)
            .withHendelsestidspunkt(ZonedDateTime.now(ZoneId.of("UTC")).minusDays(1).format(DateTimeFormatter.ISO_DATE_TIME))
        val nyUtbetalingHendelse2 = JsonUtbetaling()
            .withUtbetalingsreferanse("referanse")
            .withStatus(JsonUtbetaling.Status.UTBETALT)
            .withBelop(10.0)
            .withBeskrivelse("Nødhjelp")
            .withForfallsdato(LocalDate.now().minusDays(20).toString())
            .withUtbetalingsdato(LocalDate.now().toString())
            .withHendelsestidspunkt(ZonedDateTime.now(ZoneId.of("UTC")).format(DateTimeFormatter.ISO_DATE_TIME))
        val hendelser = listOf(nyUtbetalingHendelse1, nyUtbetalingHendelse2)
        every { jsonDigisosSoker.hendelser } returns hendelser
        every { log.info(any()) } just Runs

        service.logTekniskSperre(jsonDigisosSoker, model, digisosSak, log)

        verify(exactly = 1) { log.info(any()) }
    }

    @Test
    fun `skal logge nar vi har gamel utbetaling som ikke er utbetalt`() {
        val nyUtbetaling = Utbetaling("referanse", UtbetalingsStatus.PLANLAGT_UTBETALING, BigDecimal.TEN, "Nødhjelp", LocalDate.now().minusDays(2), null, null, null, null, null, false, null, null, mutableListOf(), mutableListOf(), LocalDateTime.now())
        val utbetalinger = mutableListOf(nyUtbetaling)
        every { model.utbetalinger } returns utbetalinger
        val nyUtbetalingHendelse = JsonUtbetaling()
            .withUtbetalingsreferanse("referanse")
            .withStatus(JsonUtbetaling.Status.UTBETALT)
            .withBelop(10.0)
            .withBeskrivelse("Nødhjelp")
            .withForfallsdato(LocalDate.now().minusDays(2).toString())
            .withUtbetalingsdato(LocalDate.now().minusDays(2).toString())
            .withHendelsestidspunkt(ZonedDateTime.now(ZoneId.of("UTC")).minusDays(8).format(DateTimeFormatter.ISO_DATE_TIME))
        val hendelser = listOf(nyUtbetalingHendelse)
        every { jsonDigisosSoker.hendelser } returns hendelser
        every { log.info(any()) } just Runs

        service.logTekniskSperre(jsonDigisosSoker, model, digisosSak, log)

        val logtekstSlot = slot<String>()
        verify(exactly = 1) { log.info(capture(logtekstSlot)) }
        assertThat(logtekstSlot.captured).startsWith("Utbetaling på overtid:")
    }

    @Test
    fun `skal logge nar vi har gamel utbetaling som er utbetalt for sent`() {
        val nyUtbetaling = Utbetaling("referanse", UtbetalingsStatus.UTBETALT, BigDecimal.TEN, "Nødhjelp", LocalDate.now().minusDays(4), LocalDate.now().minusDays(2), null, null, null, null, false, null, null, mutableListOf(), mutableListOf(), LocalDateTime.now())
        val utbetalinger = mutableListOf(nyUtbetaling)
        every { model.utbetalinger } returns utbetalinger
        val nyUtbetalingHendelse1 = JsonUtbetaling()
            .withUtbetalingsreferanse("referanse")
            .withStatus(JsonUtbetaling.Status.PLANLAGT_UTBETALING)
            .withBelop(10.0)
            .withBeskrivelse("Nødhjelp")
            .withForfallsdato(LocalDate.now().minusDays(4).toString())
            .withUtbetalingsdato(null)
            .withHendelsestidspunkt(ZonedDateTime.now(ZoneId.of("UTC")).minusDays(8).format(DateTimeFormatter.ISO_DATE_TIME))
        val nyUtbetalingHendelse2 = JsonUtbetaling()
            .withUtbetalingsreferanse("referanse")
            .withStatus(JsonUtbetaling.Status.UTBETALT)
            .withBelop(10.0)
            .withBeskrivelse("Nødhjelp")
            .withForfallsdato(LocalDate.now().minusDays(4).toString())
            .withUtbetalingsdato(LocalDate.now().minusDays(2).toString())
            .withHendelsestidspunkt(ZonedDateTime.now(ZoneId.of("UTC")).minusDays(2).format(DateTimeFormatter.ISO_DATE_TIME))
        val hendelser = listOf(nyUtbetalingHendelse1, nyUtbetalingHendelse2)
        every { jsonDigisosSoker.hendelser } returns hendelser
        every { log.info(any()) } just Runs

        service.logTekniskSperre(jsonDigisosSoker, model, digisosSak, log)

        val logtekstSlot = slot<String>()
        verify(exactly = 1) { log.info(capture(logtekstSlot)) }
        assertThat(logtekstSlot.captured).startsWith("Utbetaling på overtid:")
    }

    @Test
    fun `skal logge nar vi har gamel utbetaling som er stoppet for sent`() {
        val nyUtbetaling = Utbetaling("referanse", UtbetalingsStatus.STOPPET, BigDecimal.TEN, "Nødhjelp", LocalDate.now().minusDays(4), null, LocalDate.now().minusDays(2), null, null, null, false, null, null, mutableListOf(), mutableListOf(), LocalDateTime.now())
        val utbetalinger = mutableListOf(nyUtbetaling)
        every { model.utbetalinger } returns utbetalinger
        val nyUtbetalingHendelse1 = JsonUtbetaling()
            .withUtbetalingsreferanse("referanse")
            .withStatus(JsonUtbetaling.Status.PLANLAGT_UTBETALING)
            .withBelop(10.0)
            .withBeskrivelse("Nødhjelp")
            .withForfallsdato(LocalDate.now().minusDays(4).toString())
            .withUtbetalingsdato(null)
            .withHendelsestidspunkt(ZonedDateTime.now(ZoneId.of("UTC")).minusDays(8).format(DateTimeFormatter.ISO_DATE_TIME))
        val nyUtbetalingHendelse2 = JsonUtbetaling()
            .withUtbetalingsreferanse("referanse")
            .withStatus(JsonUtbetaling.Status.STOPPET)
            .withBelop(10.0)
            .withBeskrivelse("Nødhjelp")
            .withForfallsdato(LocalDate.now().minusDays(4).toString())
            .withUtbetalingsdato(null)
            .withHendelsestidspunkt(ZonedDateTime.now(ZoneId.of("UTC")).minusDays(2).format(DateTimeFormatter.ISO_DATE_TIME))
        val hendelser = listOf(nyUtbetalingHendelse1, nyUtbetalingHendelse2)
        every { jsonDigisosSoker.hendelser } returns hendelser
        every { log.info(any()) } just Runs

        service.logTekniskSperre(jsonDigisosSoker, model, digisosSak, log)

        val logtekstSlot = slot<String>()
        verify(exactly = 1) { log.info(capture(logtekstSlot)) }
        assertThat(logtekstSlot.captured).startsWith("Utbetaling på overtid:")
    }

    @Test
    fun `skal logge nar vi har gamel forfallsdato som ikke er utbetalt fort nok etter at event er opprettet`() {
        val nyUtbetaling = Utbetaling("referanse", UtbetalingsStatus.UTBETALT, BigDecimal.TEN, "Nødhjelp", LocalDate.now().minusDays(20), LocalDate.now(), null, null, null, null, false, null, null, mutableListOf(), mutableListOf(), LocalDateTime.now())
        val utbetalinger = mutableListOf(nyUtbetaling)
        every { model.utbetalinger } returns utbetalinger
        val nyUtbetalingHendelse1 = JsonUtbetaling()
            .withUtbetalingsreferanse("referanse")
            .withStatus(JsonUtbetaling.Status.PLANLAGT_UTBETALING)
            .withBelop(10.0)
            .withBeskrivelse("Nødhjelp")
            .withForfallsdato(LocalDate.now().minusDays(20).toString())
            .withUtbetalingsdato(null)
            .withHendelsestidspunkt(ZonedDateTime.now(ZoneId.of("UTC")).minusDays(2).format(DateTimeFormatter.ISO_DATE_TIME))
        val nyUtbetalingHendelse2 = JsonUtbetaling()
            .withUtbetalingsreferanse("referanse")
            .withStatus(JsonUtbetaling.Status.UTBETALT)
            .withBelop(10.0)
            .withBeskrivelse("Nødhjelp")
            .withForfallsdato(LocalDate.now().minusDays(20).toString())
            .withUtbetalingsdato(LocalDate.now().toString())
            .withHendelsestidspunkt(ZonedDateTime.now(ZoneId.of("UTC")).format(DateTimeFormatter.ISO_DATE_TIME))
        val hendelser = listOf(nyUtbetalingHendelse1, nyUtbetalingHendelse2)
        every { jsonDigisosSoker.hendelser } returns hendelser
        every { log.info(any()) } just Runs

        service.logTekniskSperre(jsonDigisosSoker, model, digisosSak, log)

        val logtekstSlot = slot<String>()
        verify(exactly = 1) { log.info(capture(logtekstSlot)) }
        assertThat(logtekstSlot.captured).startsWith("Utbetaling på overtid:")
    }
}
