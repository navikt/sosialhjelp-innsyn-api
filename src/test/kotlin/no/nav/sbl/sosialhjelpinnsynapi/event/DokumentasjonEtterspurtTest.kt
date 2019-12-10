package no.nav.sbl.sosialhjelpinnsynapi.event

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sbl.soknadsosialhjelp.soknad.JsonSoknad
import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.domain.DigisosSak
import no.nav.sbl.sosialhjelpinnsynapi.domain.NavEnhet
import no.nav.sbl.sosialhjelpinnsynapi.domain.SoknadsStatus
import no.nav.sbl.sosialhjelpinnsynapi.innsyn.InnsynService
import no.nav.sbl.sosialhjelpinnsynapi.norg.NorgClient
import no.nav.sbl.sosialhjelpinnsynapi.toLocalDateTime
import no.nav.sbl.sosialhjelpinnsynapi.unixToLocalDateTime
import no.nav.sbl.sosialhjelpinnsynapi.vedlegg.VEDLEGG_KREVES_STATUS
import no.nav.sbl.sosialhjelpinnsynapi.vedlegg.VedleggService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class DokumentasjonEtterspurtTest {

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

    private val vedleggKrevesDokumenttype = "faktura"
    private val vedleggKrevesTilleggsinfo = "strom"

    @BeforeEach
    fun init() {
        clearAllMocks()
        every { mockDigisosSak.fiksDigisosId } returns "123"
        every { mockDigisosSak.digisosSoker?.metadata } returns "some id"
        every { mockDigisosSak.originalSoknadNAV?.metadata } returns "some other id"
        every { mockDigisosSak.originalSoknadNAV?.timestampSendt } returns tidspunkt_soknad
        every { mockJsonSoknad.mottaker.navEnhetsnavn } returns soknadsmottaker
        every { mockJsonSoknad.mottaker.enhetsnummer } returns enhetsnr
        every { mockDigisosSak.ettersendtInfoNAV } returns null
        every { innsynService.hentOriginalSoknad(any(), any(), any()) } returns mockJsonSoknad
        every { norgClient.hentNavEnhet(enhetsnr) } returns mockNavEnhet

        resetHendelser()
    }

    @Test
    fun `dokumentliste er satt OG vedtaksbrev er satt - skal gi oppgaver og historikk`() {
        every { innsynService.hentJsonDigisosSoker(any(), any(), any()) } returns
                JsonDigisosSoker()
                        .withAvsender(avsender)
                        .withVersion("123")
                        .withHendelser(listOf(
                                SOKNADS_STATUS_MOTTATT.withHendelsestidspunkt(tidspunkt_1),
                                SOKNADS_STATUS_UNDERBEHANDLING.withHendelsestidspunkt(tidspunkt_2),
                                DOKUMENTASJONETTERSPURT.withHendelsestidspunkt(tidspunkt_3)
                        ))

        val model = service.createModel(mockDigisosSak, "token")

        assertThat(model).isNotNull
        assertThat(model.status).isEqualTo(SoknadsStatus.UNDER_BEHANDLING)
        assertThat(model.saker).isEmpty()
        assertThat(model.oppgaver).hasSize(1)
        assertThat(model.historikk).hasSize(4)

        val oppgave = model.oppgaver.last()
        assertThat(oppgave.tittel).isEqualTo(dokumenttype)
        assertThat(oppgave.tilleggsinfo).isEqualTo(tilleggsinfo)
        assertThat(oppgave.innsendelsesfrist).isEqualTo(toLocalDateTime(innsendelsesfrist))
        assertThat(oppgave.erFraInnsyn).isEqualTo(true)

        val hendelse = model.historikk.last()
        assertThat(hendelse.tidspunkt).isEqualTo(toLocalDateTime(tidspunkt_3))
        assertThat(hendelse.tittel).contains("Du må sende dokumentasjon")
        assertThat(hendelse.url?.link).contains("/dokumentlager/nedlasting/$dokumentlagerId_1")
    }

    @Test
    internal fun `dokumentliste er satt OG forvaltningsbrev mangler - skal gi oppgaver men ikke historikk`() {
        every { innsynService.hentJsonDigisosSoker(any(), any(), any()) } returns
                JsonDigisosSoker()
                        .withAvsender(avsender)
                        .withVersion("123")
                        .withHendelser(listOf(
                                SOKNADS_STATUS_MOTTATT.withHendelsestidspunkt(tidspunkt_1),
                                SOKNADS_STATUS_UNDERBEHANDLING.withHendelsestidspunkt(tidspunkt_2),
                                DOKUMENTASJONETTERSPURT_UTEN_FORVALTNINGSBREV.withHendelsestidspunkt(tidspunkt_3)
                        ))

        val model = service.createModel(mockDigisosSak, "token")

        assertThat(model).isNotNull
        assertThat(model.status).isEqualTo(SoknadsStatus.UNDER_BEHANDLING)
        assertThat(model.saker).isEmpty()
        assertThat(model.oppgaver).hasSize(1)
        assertThat(model.historikk).hasSize(3)

        val oppgave = model.oppgaver.last()
        assertThat(oppgave.tittel).isEqualTo(dokumenttype)
        assertThat(oppgave.tilleggsinfo).isEqualTo(tilleggsinfo)
        assertThat(oppgave.innsendelsesfrist).isEqualTo(toLocalDateTime(innsendelsesfrist))
        assertThat(oppgave.erFraInnsyn).isEqualTo(true)
    }

    @Test
    fun `dokumentliste er tom OG forvaltningsbrev er satt - skal verken gi oppgaver eller historikk`() {
        every { innsynService.hentJsonDigisosSoker(any(), any(), any()) } returns
                JsonDigisosSoker()
                        .withAvsender(avsender)
                        .withVersion("123")
                        .withHendelser(listOf(
                                SOKNADS_STATUS_MOTTATT.withHendelsestidspunkt(tidspunkt_1),
                                SOKNADS_STATUS_UNDERBEHANDLING.withHendelsestidspunkt(tidspunkt_2),
                                DOKUMENTASJONETTERSPURT_TOM_DOKUMENT_LISTE.withHendelsestidspunkt(tidspunkt_3)
                        ))

        val model = service.createModel(mockDigisosSak, "token")

        assertThat(model).isNotNull
        assertThat(model.status).isEqualTo(SoknadsStatus.UNDER_BEHANDLING)
        assertThat(model.saker).isEmpty()
        assertThat(model.oppgaver).hasSize(0)
        assertThat(model.historikk).hasSize(3)
    }

    @Test
    fun `oppgaver skal hentes fra soknaden dersom det ikke finnes dokumentasjonEtterspurt`() {
        every { innsynService.hentJsonDigisosSoker(any(), any(), any()) } returns
                JsonDigisosSoker()
                        .withAvsender(avsender)
                        .withVersion("123")
                        .withHendelser(listOf(
                                SOKNADS_STATUS_MOTTATT.withHendelsestidspunkt(tidspunkt_1),
                                SOKNADS_STATUS_UNDERBEHANDLING.withHendelsestidspunkt(tidspunkt_2)
                        ))
        every { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any(), any(), any()) } returns
                listOf(VedleggService.InternalVedlegg(vedleggKrevesDokumenttype, vedleggKrevesTilleggsinfo, emptyList(), unixToLocalDateTime(tidspunkt_soknad)))

        val model = service.createModel(mockDigisosSak, "token")

        assertThat(model).isNotNull
        assertThat(model.status).isEqualTo(SoknadsStatus.UNDER_BEHANDLING)
        assertThat(model.saker).isEmpty()
        assertThat(model.oppgaver).hasSize(1)
        assertThat(model.historikk).hasSize(3)

        val oppgave = model.oppgaver.last()
        assertThat(oppgave.tittel).isEqualTo(vedleggKrevesDokumenttype)
        assertThat(oppgave.tilleggsinfo).isEqualTo(vedleggKrevesTilleggsinfo)
        assertThat(oppgave.innsendelsesfrist).isNull()
        assertThat(oppgave.erFraInnsyn).isEqualTo(false)

        val hendelse = model.historikk.last()
        assertThat(hendelse.tidspunkt).isEqualTo(toLocalDateTime(tidspunkt_2))
        assertThat(hendelse.tittel).contains("Søknaden er under behandling")
        assertThat(hendelse.url).isNull()
    }

    @Test
    internal fun `dokumentasjonEtterspurt overstyrer gjenstaende vedleggskrav fra soknad`() {
        every { innsynService.hentJsonDigisosSoker(any(), any(), any()) } returns
                JsonDigisosSoker()
                        .withAvsender(avsender)
                        .withVersion("123")
                        .withHendelser(listOf(
                                SOKNADS_STATUS_MOTTATT.withHendelsestidspunkt(tidspunkt_1),
                                SOKNADS_STATUS_UNDERBEHANDLING.withHendelsestidspunkt(tidspunkt_2),
                                DOKUMENTASJONETTERSPURT.withHendelsestidspunkt(tidspunkt_3)
                        ))
        every { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any(), any(), any()) } returns
                listOf(VedleggService.InternalVedlegg(vedleggKrevesDokumenttype, vedleggKrevesTilleggsinfo, emptyList(), unixToLocalDateTime(tidspunkt_soknad)))

        val model = service.createModel(mockDigisosSak, "token")

        assertThat(model).isNotNull
        assertThat(model.status).isEqualTo(SoknadsStatus.UNDER_BEHANDLING)
        assertThat(model.saker).isEmpty()
        assertThat(model.oppgaver).hasSize(1)
        assertThat(model.historikk).hasSize(4)

        val oppgave = model.oppgaver.last()
        assertThat(oppgave.tittel).isEqualTo(dokumenttype)
        assertThat(oppgave.tilleggsinfo).isEqualTo(tilleggsinfo)
        assertThat(oppgave.innsendelsesfrist).isEqualTo(toLocalDateTime(innsendelsesfrist))
        assertThat(oppgave.erFraInnsyn).isEqualTo(true)
    }
}