package no.nav.sosialhjelp.innsyn.event

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import no.finn.unleash.Unleash
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sbl.soknadsosialhjelp.soknad.JsonSoknad
import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.innsyn.client.norg.NorgClient
import no.nav.sosialhjelp.innsyn.config.ClientProperties
import no.nav.sosialhjelp.innsyn.domain.NavEnhet
import no.nav.sosialhjelp.innsyn.domain.SoknadsStatus
import no.nav.sosialhjelp.innsyn.service.innsyn.InnsynService
import no.nav.sosialhjelp.innsyn.service.vedlegg.VEDLEGG_KREVES_STATUS
import no.nav.sosialhjelp.innsyn.service.vedlegg.VedleggService
import no.nav.sosialhjelp.innsyn.utils.toLocalDateTime
import no.nav.sosialhjelp.innsyn.utils.unixToLocalDateTime
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class SoknadsStatusTest {

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
        every { mockDigisosSak.originalSoknadNAV?.timestampSendt } returns tidspunkt_soknad_fixed
        every { mockDigisosSak.originalSoknadNAV?.navEksternRefId } returns "1100001"
        every { mockDigisosSak.originalSoknadNAV?.soknadDokument?.dokumentlagerDokumentId } returns null
        every { mockJsonSoknad.mottaker.navEnhetsnavn } returns soknadsmottaker
        every { mockJsonSoknad.mottaker.enhetsnummer } returns enhetsnr
        every { mockDigisosSak.ettersendtInfoNAV } returns null
        every { innsynService.hentOriginalSoknad(any(), any(), any()) } returns mockJsonSoknad
        every { norgClient.hentNavEnhet(enhetsnr) } returns mockNavEnhet

        resetHendelser()
    }

    @Test
    fun `soknadsStatus SENDT`() {
        every { innsynService.hentJsonDigisosSoker(any(), any(), any()) } returns null
        every { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any(), any(), any()) } returns emptyList()

        val model = service.createModel(mockDigisosSak, "token")

        assertThat(model).isNotNull
        assertThat(model.status).isEqualTo(SoknadsStatus.SENDT)
        assertThat(model.historikk).hasSize(1)

        val hendelse = model.historikk.last()
        assertThat(hendelse.tidspunkt).isEqualTo(unixToLocalDateTime(tidspunkt_soknad_fixed))
        assertThat(hendelse.tittel).contains("Søknaden med vedlegg er sendt til ")
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
        assertThat(hendelse.tidspunkt).isEqualTo(tidspunkt_1.toLocalDateTime())
        assertThat(hendelse.tittel).contains("Søknaden med vedlegg er mottatt hos ")
    }

    @Test
    fun `soknadsStatus SENDT innsynDeaktivert`() {
        every { mockJsonSoknad.mottaker } returns null
        every { innsynService.hentJsonDigisosSoker(any(), any(), any()) } returns null
        every { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any(), any(), any()) } returns emptyList()

        val model = service.createModel(mockDigisosSak, "token")

        assertThat(model).isNotNull
        assertThat(model.status).isEqualTo(SoknadsStatus.SENDT)
        assertThat(model.historikk).hasSize(0)
    }

    @Test
    fun `soknadsStatus SENDT papirsoknad`() {
        every { mockJsonSoknad.mottaker } returns null
        every { innsynService.hentJsonDigisosSoker(any(), any(), any()) } returns JsonDigisosSoker()
                .withAvsender(avsender)
                .withVersion("123")
                .withHendelser(emptyList())
        every { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any(), any(), any()) } returns emptyList()

        val model = service.createModel(mockDigisosSak, "token")

        assertThat(model).isNotNull
        assertThat(model.status).isEqualTo(SoknadsStatus.SENDT)
        assertThat(model.historikk).hasSize(0)
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
        assertThat(hendelse.tidspunkt).isEqualTo(tidspunkt_1.toLocalDateTime())
        assertThat(hendelse.tittel).isEqualTo("Søknaden med vedlegg er mottatt.")
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
        assertThat(hendelse.tidspunkt).isEqualTo(tidspunkt_2.toLocalDateTime())
        assertThat(hendelse.tittel).isEqualTo("Søknaden er under behandling.")
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
        assertThat(hendelse.tidspunkt).isEqualTo(tidspunkt_3.toLocalDateTime())
        assertThat(hendelse.tittel).isEqualTo("Søknaden er ferdig behandlet.")
    }

    @Test
    fun `soknadsStatus BEHANDLES_IKKE`() {
        every { innsynService.hentJsonDigisosSoker(any(), any(), any()) } returns
                JsonDigisosSoker()
                        .withAvsender(avsender)
                        .withVersion("123")
                        .withHendelser(listOf(
                                SOKNADS_STATUS_MOTTATT.withHendelsestidspunkt(tidspunkt_1),
                                SOKNADS_STATUS_BEHANDLES_IKKE.withHendelsestidspunkt(tidspunkt_2)
                        ))
        every { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any(), any(), any()) } returns emptyList()

        val model = service.createModel(mockDigisosSak, "token")

        assertThat(model).isNotNull
        assertThat(model.status).isEqualTo(SoknadsStatus.BEHANDLES_IKKE)
        assertThat(model.saker).isEmpty()
        assertThat(model.historikk).hasSize(3)

        val hendelse = model.historikk.last()
        assertThat(hendelse.tidspunkt).isEqualTo(tidspunkt_2.toLocalDateTime())
        assertThat(hendelse.tittel).isEqualTo("Din søknad vil bli behandlet, men vi kan ikke vise behandlingsstatus digitalt.")
    }

    @Test
    fun `modell inneholder referanse`() {
        every { innsynService.hentJsonDigisosSoker(any(), any(), any()) } returns JsonDigisosSoker()
        every { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any(), any(), any()) } returns emptyList()

        val model = service.createModel(mockDigisosSak, "token")

        assertThat(model).isNotNull
        assertThat(model.referanse).isEqualTo("1100001")
    }

    @Test
    fun `soknadsStatus inneholder tidspunktSendt`() {
        every { innsynService.hentJsonDigisosSoker(any(), any(), any()) } returns JsonDigisosSoker()
        every { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any(), any(), any()) } returns emptyList()

        val model = service.createModel(mockDigisosSak, "token")

        assertThat(model.tidspunktSendt).isEqualTo(tidspunkt_soknad_fixed_localDateTime)
    }
}