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
import no.nav.sosialhjelp.innsyn.domain.SoknadsStatus
import no.nav.sosialhjelp.innsyn.navenhet.NavEnhet
import no.nav.sosialhjelp.innsyn.navenhet.NorgClient
import no.nav.sosialhjelp.innsyn.utils.toLocalDateTime
import no.nav.sosialhjelp.innsyn.utils.unixToLocalDateTime
import no.nav.sosialhjelp.innsyn.vedlegg.VEDLEGG_KREVES_STATUS
import no.nav.sosialhjelp.innsyn.vedlegg.VedleggService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds

internal class SoknadsStatusTest {
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
        every { mockDigisosSak.originalSoknadNAV?.timestampSendt } returns tidspunkt_soknad_fixed
        every { mockDigisosSak.originalSoknadNAV?.navEksternRefId } returns "1100001"
        every { mockDigisosSak.originalSoknadNAV?.soknadDokument?.dokumentlagerDokumentId } returns null
        every { mockJsonSoknad.mottaker.navEnhetsnavn } returns soknadsmottaker
        every { mockJsonSoknad.mottaker.enhetsnummer } returns enhetsnr
        every { mockDigisosSak.ettersendtInfoNAV } returns null
        coEvery { innsynService.hentOriginalSoknad(any(), any()) } returns mockJsonSoknad
        coEvery { norgClient.hentNavEnhet(enhetsnr) } returns mockNavEnhet

        resetHendelser()
    }

    @Test
    fun `soknadsStatus SENDT`() =
        runTest(timeout = 5.seconds) {
            coEvery { innsynService.hentJsonDigisosSoker(any(), any()) } returns null
            coEvery { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any(), any()) } returns emptyList()

            val model = service.createModel(mockDigisosSak, Token("token"))

            assertThat(model).isNotNull
            assertThat(model.status).isEqualTo(SoknadsStatus.SENDT)
            assertThat(model.historikk).hasSize(1)

            val hendelse = model.historikk.last()
            assertThat(hendelse.tidspunkt).isEqualTo(unixToLocalDateTime(tidspunkt_soknad_fixed))
            assertThat(hendelse.hendelseType).isEqualTo(HendelseTekstType.SOKNAD_SEND_TIL_KONTOR)
        }

    @Test
    fun `soknadsStatus MOTTATT`() =
        runTest(timeout = 5.seconds) {
            coEvery { innsynService.hentJsonDigisosSoker(any(), any()) } returns
                JsonDigisosSoker()
                    .withAvsender(avsender)
                    .withVersion("123")
                    .withHendelser(
                        listOf(
                            SOKNADS_STATUS_MOTTATT.withHendelsestidspunkt(tidspunkt_1),
                        ),
                    )
            coEvery { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any(), any()) } returns emptyList()

            val model = service.createModel(mockDigisosSak, Token("token"))

            assertThat(model).isNotNull
            assertThat(model.status).isEqualTo(SoknadsStatus.MOTTATT)
            assertThat(model.historikk).hasSize(2)

            val hendelse = model.historikk.last()
            assertThat(hendelse.tidspunkt).isEqualTo(tidspunkt_1.toLocalDateTime())
            assertThat(hendelse.hendelseType).isEqualTo(HendelseTekstType.SOKNAD_MOTTATT_MED_KOMMUNENAVN)
        }

    @Test
    fun `soknadsStatus SENDT innsynDeaktivert`() =
        runTest(timeout = 5.seconds) {
            every { mockJsonSoknad.mottaker } returns null
            coEvery { innsynService.hentJsonDigisosSoker(any(), any()) } returns null
            coEvery { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any(), any()) } returns emptyList()

            val model = service.createModel(mockDigisosSak, Token("token"))

            assertThat(model).isNotNull
            assertThat(model.status).isEqualTo(SoknadsStatus.SENDT)
            assertThat(model.historikk).hasSize(0)
        }

    @Test
    fun `soknadsStatus SENDT papirsoknad`() =
        runTest(timeout = 5.seconds) {
            every { mockJsonSoknad.mottaker } returns null
            coEvery { innsynService.hentJsonDigisosSoker(any(), any()) } returns
                JsonDigisosSoker()
                    .withAvsender(avsender)
                    .withVersion("123")
                    .withHendelser(emptyList())
            coEvery { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any(), any()) } returns emptyList()

            val model = service.createModel(mockDigisosSak, Token("token"))

            assertThat(model).isNotNull
            assertThat(model.status).isEqualTo(SoknadsStatus.SENDT)
            assertThat(model.historikk).hasSize(0)
        }

    @Test
    fun `soknadsStatus MOTTATT papirsoknad`() =
        runTest(timeout = 5.seconds) {
            every { mockJsonSoknad.mottaker } returns null
            coEvery { innsynService.hentJsonDigisosSoker(any(), any()) } returns
                JsonDigisosSoker()
                    .withAvsender(avsender)
                    .withVersion("123")
                    .withHendelser(
                        listOf(
                            SOKNADS_STATUS_MOTTATT.withHendelsestidspunkt(tidspunkt_1),
                        ),
                    )
            coEvery { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any(), any()) } returns emptyList()

            val model = service.createModel(mockDigisosSak, Token("token"))

            assertThat(model).isNotNull
            assertThat(model.status).isEqualTo(SoknadsStatus.MOTTATT)
            assertThat(model.historikk).hasSize(1)

            val hendelse = model.historikk.last()
            assertThat(hendelse.tidspunkt).isEqualTo(tidspunkt_1.toLocalDateTime())
            assertThat(hendelse.hendelseType).isEqualTo(HendelseTekstType.SOKNAD_MOTTATT_UTEN_KOMMUNENAVN)
        }

    @Test
    fun `soknadsStatus UNDER_BEHANDLING`() =
        runTest(timeout = 5.seconds) {
            coEvery { innsynService.hentJsonDigisosSoker(any(), any()) } returns
                JsonDigisosSoker()
                    .withAvsender(avsender)
                    .withVersion("123")
                    .withHendelser(
                        listOf(
                            SOKNADS_STATUS_MOTTATT.withHendelsestidspunkt(tidspunkt_1),
                            SOKNADS_STATUS_UNDERBEHANDLING.withHendelsestidspunkt(tidspunkt_2),
                        ),
                    )
            coEvery { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any(), any()) } returns emptyList()

            val model = service.createModel(mockDigisosSak, Token("token"))

            assertThat(model).isNotNull
            assertThat(model.status).isEqualTo(SoknadsStatus.UNDER_BEHANDLING)
            assertThat(model.saker).isEmpty()
            assertThat(model.historikk).hasSize(3)

            val hendelse = model.historikk.last()
            assertThat(hendelse.tidspunkt).isEqualTo(tidspunkt_2.toLocalDateTime())
            assertThat(hendelse.hendelseType).isEqualTo(HendelseTekstType.SOKNAD_UNDER_BEHANDLING)
        }

    @Test
    fun `soknadsStatus FERDIGBEHANDLET`() =
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
                        ),
                    )
            coEvery { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any(), any()) } returns emptyList()

            val model = service.createModel(mockDigisosSak, Token("token"))

            assertThat(model).isNotNull
            assertThat(model.status).isEqualTo(SoknadsStatus.FERDIGBEHANDLET)
            assertThat(model.saker).isEmpty()
            assertThat(model.historikk).hasSize(4)

            val hendelse = model.historikk.last()
            assertThat(hendelse.tidspunkt).isEqualTo(tidspunkt_3.toLocalDateTime())
            assertThat(hendelse.hendelseType).isEqualTo(HendelseTekstType.SOKNAD_FERDIGBEHANDLET)
        }

    @Test
    fun `soknadsStatus BEHANDLES_IKKE`() =
        runTest(timeout = 5.seconds) {
            coEvery { innsynService.hentJsonDigisosSoker(any(), any()) } returns
                JsonDigisosSoker()
                    .withAvsender(avsender)
                    .withVersion("123")
                    .withHendelser(
                        listOf(
                            SOKNADS_STATUS_MOTTATT.withHendelsestidspunkt(tidspunkt_1),
                            SOKNADS_STATUS_BEHANDLES_IKKE.withHendelsestidspunkt(tidspunkt_2),
                        ),
                    )
            coEvery { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any(), any()) } returns emptyList()

            val model = service.createModel(mockDigisosSak, Token("token"))

            assertThat(model).isNotNull
            assertThat(model.status).isEqualTo(SoknadsStatus.BEHANDLES_IKKE)
            assertThat(model.saker).isEmpty()
            assertThat(model.historikk).hasSize(3)

            val hendelse = model.historikk.last()
            assertThat(hendelse.tidspunkt).isEqualTo(tidspunkt_2.toLocalDateTime())
            assertThat(hendelse.hendelseType).isEqualTo(HendelseTekstType.SOKNAD_BEHANDLES_IKKE)
        }

    @Test
    fun `modell inneholder referanse`() =
        runTest(timeout = 5.seconds) {
            coEvery { innsynService.hentJsonDigisosSoker(any(), any()) } returns JsonDigisosSoker()
            coEvery { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any(), any()) } returns emptyList()

            val model = service.createModel(mockDigisosSak, Token("token"))

            assertThat(model).isNotNull
            assertThat(model.referanse).isEqualTo("1100001")
        }

    @Test
    fun `soknadsStatus inneholder tidspunktSendt`() =
        runTest(timeout = 5.seconds) {
            coEvery { innsynService.hentJsonDigisosSoker(any(), any()) } returns JsonDigisosSoker()
            coEvery { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any(), any()) } returns emptyList()

            val model = service.createModel(mockDigisosSak, Token("token"))

            assertThat(model.tidspunktSendt).isEqualTo(tidspunkt_soknad_fixed_localDateTime)
        }
}
