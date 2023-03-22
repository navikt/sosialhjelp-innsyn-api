package no.nav.sosialhjelp.innsyn.event

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import no.finn.unleash.Unleash
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sbl.soknadsosialhjelp.soknad.JsonSoknad
import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.innsyn.app.ClientProperties
import no.nav.sosialhjelp.innsyn.app.exceptions.NorgException
import no.nav.sosialhjelp.innsyn.domain.HendelseTekstType
import no.nav.sosialhjelp.innsyn.domain.SoknadsStatus
import no.nav.sosialhjelp.innsyn.navenhet.NavEnhet
import no.nav.sosialhjelp.innsyn.navenhet.NorgClient
import no.nav.sosialhjelp.innsyn.vedlegg.VEDLEGG_KREVES_STATUS
import no.nav.sosialhjelp.innsyn.vedlegg.VedleggService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class TildeltNavKontorTest {

    private val clientProperties: ClientProperties = mockk(relaxed = true)
    private val innsynService: InnsynService = mockk()
    private val vedleggService: VedleggService = mockk()
    private val norgClient: NorgClient = mockk()
    private val unleashClient: Unleash = mockk()

    private val service = EventService(clientProperties, innsynService, vedleggService, norgClient, unleashClient)

    private val mockDigisosSak: DigisosSak = mockk()
    private val mockJsonSoknad: JsonSoknad = mockk()

    private val soknadsmottaker = "The Office"
    private val enhetsnr = "2317"

    private val mockNavEnhet: NavEnhet = mockk()
    private val enhetNavn = "NAV Holmenkollen"

    private val mockNavEnhet2: NavEnhet = mockk()
    private val enhetNavn2 = "NAV Longyearbyen"

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

        resetHendelser()
    }

    @Test
    fun `tildeltNavKontor skal hente navenhets navn fra Norg`() {
        every { norgClient.hentNavEnhet(navKontor) } returns mockNavEnhet
        every { mockNavEnhet.navn } returns enhetNavn
        every { innsynService.hentJsonDigisosSoker(any(), any()) } returns
            JsonDigisosSoker()
                .withAvsender(avsender)
                .withVersion("123")
                .withHendelser(
                    listOf(
                        SOKNADS_STATUS_MOTTATT.withHendelsestidspunkt(tidspunkt_1),
                        TILDELT_NAV_KONTOR.withHendelsestidspunkt(tidspunkt_2)
                    )
                )
        every { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any(), any()) } returns emptyList()

        val model = service.createModel(mockDigisosSak, "token")

        assertThat(model).isNotNull
        assertThat(model.status).isEqualTo(SoknadsStatus.MOTTATT)
        assertThat(model.saker).hasSize(0)
        assertThat(model.historikk).hasSize(3)

        assertThat(model.historikk.last().hendelseType).isEqualTo(HendelseTekstType.SOKNAD_VIDERESENDT_MED_NORG_ENHET)
        assertThat(model.historikk.last().tekstArgument).isEqualTo(enhetNavn)
    }

    @Test
    fun `tildeltNavKontor skal gi generell melding hvis NorgClient kaster FiksException`() {
        every { norgClient.hentNavEnhet(navKontor) } throws NorgException("noe feilet", null)
        every { innsynService.hentJsonDigisosSoker(any(), any()) } returns
            JsonDigisosSoker()
                .withAvsender(avsender)
                .withVersion("123")
                .withHendelser(
                    listOf(
                        SOKNADS_STATUS_MOTTATT.withHendelsestidspunkt(tidspunkt_1),
                        TILDELT_NAV_KONTOR.withHendelsestidspunkt(tidspunkt_2)
                    )
                )
        every { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any(), any()) } returns emptyList()

        val model = service.createModel(mockDigisosSak, "token")

        assertThat(model).isNotNull
        assertThat(model.status).isEqualTo(SoknadsStatus.MOTTATT)
        assertThat(model.saker).hasSize(0)
        assertThat(model.historikk).hasSize(3)

        assertThat(model.historikk.last().tekstArgument).isNull()
    }

    @Test
    fun `tildeltNavKontor til samme navKontor som soknad ble sendt til - gir ingen hendelse`() {
        every { mockJsonSoknad.mottaker.enhetsnummer } returns navKontor
        every { norgClient.hentNavEnhet(navKontor) } returns mockNavEnhet
        every { mockNavEnhet.navn } returns enhetNavn
        every { innsynService.hentJsonDigisosSoker(any(), any()) } returns
            JsonDigisosSoker()
                .withAvsender(avsender)
                .withVersion("123")
                .withHendelser(
                    listOf(
                        SOKNADS_STATUS_MOTTATT.withHendelsestidspunkt(tidspunkt_1),
                        TILDELT_NAV_KONTOR.withHendelsestidspunkt(tidspunkt_2)
                    )
                )
        every { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any(), any()) } returns emptyList()

        val model = service.createModel(mockDigisosSak, "token")

        assertThat(model).isNotNull
        assertThat(model.status).isEqualTo(SoknadsStatus.MOTTATT)
        assertThat(model.saker).hasSize(0)
        assertThat(model.historikk).hasSize(2)

        assertThat(model.historikk.last().hendelseType).isEqualTo(HendelseTekstType.SOKNAD_MOTTATT_HOS_KOMMUNE)
    }

    @Test
    fun `flere identiske tildeltNavKontor-hendelser skal kun gi en hendelse i historikk`() {
        every { norgClient.hentNavEnhet(navKontor) } returns mockNavEnhet
        every { mockNavEnhet.navn } returns enhetNavn
        every { innsynService.hentJsonDigisosSoker(any(), any()) } returns
            JsonDigisosSoker()
                .withAvsender(avsender)
                .withVersion("123")
                .withHendelser(
                    listOf(
                        SOKNADS_STATUS_MOTTATT.withHendelsestidspunkt(tidspunkt_1),
                        TILDELT_NAV_KONTOR.withHendelsestidspunkt(tidspunkt_2),
                        TILDELT_NAV_KONTOR.withHendelsestidspunkt(tidspunkt_3)
                    )
                )
        every { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any(), any()) } returns emptyList()

        val model = service.createModel(mockDigisosSak, "token")

        assertThat(model).isNotNull
        assertThat(model.status).isEqualTo(SoknadsStatus.MOTTATT)
        assertThat(model.saker).hasSize(0)
        assertThat(model.historikk).hasSize(3)

        assertThat(model.historikk.last().tekstArgument).isEqualTo(enhetNavn)
    }

    @Test
    fun `tildeltNavKontor til ulike kontor gir like mange hendelser`() {
        every { norgClient.hentNavEnhet(navKontor) } returns mockNavEnhet
        every { norgClient.hentNavEnhet(navKontor2) } returns mockNavEnhet2
        every { mockNavEnhet.navn } returns enhetNavn
        every { mockNavEnhet2.navn } returns enhetNavn2
        every { innsynService.hentJsonDigisosSoker(any(), any()) } returns
            JsonDigisosSoker()
                .withAvsender(avsender)
                .withVersion("123")
                .withHendelser(
                    listOf(
                        SOKNADS_STATUS_MOTTATT.withHendelsestidspunkt(tidspunkt_1),
                        TILDELT_NAV_KONTOR.withHendelsestidspunkt(tidspunkt_2),
                        TILDELT_NAV_KONTOR_2.withHendelsestidspunkt(tidspunkt_3)
                    )
                )
        every { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any(), any()) } returns emptyList()

        val model = service.createModel(mockDigisosSak, "token")

        assertThat(model).isNotNull
        assertThat(model.status).isEqualTo(SoknadsStatus.MOTTATT)
        assertThat(model.saker).hasSize(0)
        assertThat(model.historikk).hasSize(4)

        assertThat(model.historikk[2].tekstArgument).isEqualTo(enhetNavn)
        assertThat(model.historikk[3].tekstArgument).isEqualTo(enhetNavn2)
    }

    @Test
    fun `forste gang en papirSoknad faar tildeltNavKontor skal hendelsen ikke nevne videresendt`() {
        every { norgClient.hentNavEnhet(navKontor) } returns mockNavEnhet
        every { mockNavEnhet.navn } returns enhetNavn
        every { mockDigisosSak.originalSoknadNAV } returns null
        every { innsynService.hentJsonDigisosSoker(any(), any()) } returns
            JsonDigisosSoker()
                .withAvsender(avsender)
                .withVersion("123")
                .withHendelser(
                    listOf(
                        SOKNADS_STATUS_MOTTATT.withHendelsestidspunkt(tidspunkt_1),
                        TILDELT_NAV_KONTOR.withHendelsestidspunkt(tidspunkt_2),
                    )
                )
        every { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any(), any()) } returns emptyList()

        val model = service.createModel(mockDigisosSak, "token")

        assertThat(model).isNotNull
        assertThat(model.status).isEqualTo(SoknadsStatus.MOTTATT)
        assertThat(model.saker).hasSize(0)
        assertThat(model.historikk).hasSize(2)

        assertThat(model.historikk[0].hendelseType).isEqualTo(HendelseTekstType.SOKNAD_MOTTATT_UTEN_KOMMUNENAVN)
        assertThat(model.historikk[1].tekstArgument).isEqualTo(enhetNavn)
    }

    @Test
    fun `andre gang en papirSoknad faar tildeltNavKontor skal hendelsen vise videresendt`() {
        every { norgClient.hentNavEnhet(navKontor) } returns mockNavEnhet
        every { norgClient.hentNavEnhet(navKontor2) } returns mockNavEnhet2
        every { mockNavEnhet.navn } returns enhetNavn
        every { mockNavEnhet2.navn } returns enhetNavn2
        every { mockDigisosSak.originalSoknadNAV } returns null
        every { innsynService.hentJsonDigisosSoker(any(), any()) } returns
            JsonDigisosSoker()
                .withAvsender(avsender)
                .withVersion("123")
                .withHendelser(
                    listOf(
                        SOKNADS_STATUS_MOTTATT.withHendelsestidspunkt(tidspunkt_1),
                        TILDELT_NAV_KONTOR.withHendelsestidspunkt(tidspunkt_2),
                        TILDELT_NAV_KONTOR_2.withHendelsestidspunkt(tidspunkt_3),
                    )
                )
        every { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any(), any()) } returns emptyList()

        val model = service.createModel(mockDigisosSak, "token")

        assertThat(model).isNotNull
        assertThat(model.status).isEqualTo(SoknadsStatus.MOTTATT)
        assertThat(model.saker).hasSize(0)
        assertThat(model.historikk).hasSize(3)

        assertThat(model.historikk[0].hendelseType).isEqualTo(HendelseTekstType.SOKNAD_MOTTATT_UTEN_KOMMUNENAVN)
        assertThat(model.historikk[2].hendelseType).isEqualTo(HendelseTekstType.SOKNAD_VIDERESENDT_MED_NORG_ENHET)
        assertThat(model.historikk[2].tekstArgument).isEqualTo(enhetNavn2)
    }
}
