package no.nav.sosialhjelp.innsyn.event

import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonSaksStatus
import no.nav.sbl.soknadsosialhjelp.soknad.JsonSoknad
import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.innsyn.app.ClientProperties
import no.nav.sosialhjelp.innsyn.integrasjonstest.createDigisosSak
import no.nav.sosialhjelp.innsyn.navenhet.NavEnhet
import no.nav.sosialhjelp.innsyn.navenhet.NorgClient
import no.nav.sosialhjelp.innsyn.utils.InnsynsfilBuilder
import no.nav.sosialhjelp.innsyn.vedlegg.VEDLEGG_KREVES_STATUS
import no.nav.sosialhjelp.innsyn.vedlegg.VedleggService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

internal class VedtakFattetTest {
    private val clientProperties: ClientProperties = mockk(relaxed = true)
    private val innsynService: InnsynService = mockk()
    private val vedleggService: VedleggService = mockk()
    private val norgClient: NorgClient = mockk()
    private val hendelseFoldService: HendelseFoldService = mockk(relaxed = true)

    private val eventService =
        EventService(
            clientProperties = clientProperties,
            innsynService = innsynService,
            vedleggService = vedleggService,
            norgClient = norgClient,
            hendelseFoldService = hendelseFoldService,
        )

    private val mockDigisosSak: DigisosSak = mockk()
    private val mockJsonSoknad: JsonSoknad = mockk()

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
    }

    private val soknadsmottaker = "The Office"
    private val enhetsnr = "2317"

    private val mockNavEnhet: NavEnhet = mockk()
    private val enhetNavn = "Nav Holmenkollen"

    @Test
    suspend fun `For vedtakFattet uten vedtaksreferanse skal saksreferanse brukes`() {
        val digisosId = UUID.randomUUID()
        val digisosSak = createDigisosSak(digisosId, "0301")
        every { digisosSak.originalSoknadNAV?.timestampSendt } returns Instant.parse("2026-10-14T08:00:00Z").toEpochMilli()
        every { digisosSak.originalSoknadNAV?.navEksternRefId } returns UUID.randomUUID().toString()
        every { digisosSak.originalSoknadNAV?.soknadDokument?.dokumentlagerDokumentId } returns null

        val saksReferanse = "enSaksreferanse"

        val jsonDigisosSoker =
            InnsynsfilBuilder()
                .addSaksstatus(status = JsonSaksStatus.Status.UNDER_BEHANDLING, saksreferanse = saksReferanse)
                .addVedtakFattetHendelse(saksreferanse = saksReferanse)
                .build()

        coEvery { innsynService.hentJsonDigisosSoker(digisosSak) } returns jsonDigisosSoker
        coEvery { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, digisosSak) } returns emptyList()

        val innsynsfilModel = eventService.createModel(digisosSak)

        innsynsfilModel.saker
            .firstOrNull()
            ?.also { sak ->
                assertThat(sak.referanse).isEqualTo(saksReferanse)
                assertThat(sak.vedtak).hasSize(1)
                assertThat(sak.vedtak).allMatch { it.referanse == sak.referanse }
            }
            ?: error("Forventet at det finnes en sak i innsynsfilen")
    }

    @Test
    suspend fun `For vedtakFattet med vedtaksreferanse skal denne brukes for Vedtak`() {
        val digisosId = UUID.randomUUID()
        val digisosSak = createDigisosSak(digisosId, "0301")
        every { digisosSak.originalSoknadNAV?.timestampSendt } returns Instant.parse("2026-10-14T08:00:00Z").toEpochMilli()
        every { digisosSak.originalSoknadNAV?.navEksternRefId } returns UUID.randomUUID().toString()
        every { digisosSak.originalSoknadNAV?.soknadDokument?.dokumentlagerDokumentId } returns null

        val saksreferanse = "enSaksreferanse"
        val vedtaksreferanse = "enVedtaksreferanse"

        val jsonDigisosSoker =
            InnsynsfilBuilder()
                .addSaksstatus(status = JsonSaksStatus.Status.UNDER_BEHANDLING, saksreferanse = saksreferanse)
                .addVedtakFattetHendelse(saksreferanse = saksreferanse)
                .addVedtakFattetHendelse(saksreferanse = saksreferanse, vedtaksreferanse = vedtaksreferanse)
                .build()

        coEvery { innsynService.hentJsonDigisosSoker(digisosSak) } returns jsonDigisosSoker
        coEvery { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, digisosSak) } returns emptyList()

        val innsynsfilModel = eventService.createModel(digisosSak)

        innsynsfilModel.saker
            .firstOrNull()
            ?.also { sak ->
                assertThat(sak.referanse).isEqualTo(saksreferanse)
                assertThat(sak.vedtak).hasSize(2)
                assertThat(sak.vedtak).anyMatch { it.referanse == sak.referanse }
                assertThat(sak.vedtak).anyMatch { it.referanse == vedtaksreferanse }
            }
            ?: error("Forventet at det finnes en sak i innsynsfilen")
    }

    @Test
    suspend fun `For vedtakFattet med forskjellige saksreferanser blir det opprettet flere saker`() {
        val digisosId = UUID.randomUUID()
        val digisosSak = createDigisosSak(digisosId, "0301")
        every { digisosSak.originalSoknadNAV?.timestampSendt } returns Instant.parse("2026-10-14T08:00:00Z").toEpochMilli()
        every { digisosSak.originalSoknadNAV?.navEksternRefId } returns UUID.randomUUID().toString()
        every { digisosSak.originalSoknadNAV?.soknadDokument?.dokumentlagerDokumentId } returns null

        val saksreferanse = "enSaksreferanse"
        val saksreferanse2 = "enAnnenSaksreferanse"

        val jsonDigisosSoker =
            InnsynsfilBuilder()
                .addSaksstatus(status = JsonSaksStatus.Status.UNDER_BEHANDLING, saksreferanse = saksreferanse)
                .addVedtakFattetHendelse(saksreferanse = saksreferanse)
                .addVedtakFattetHendelse(saksreferanse = saksreferanse2)
                .build()

        coEvery { innsynService.hentJsonDigisosSoker(digisosSak) } returns jsonDigisosSoker
        coEvery { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, digisosSak) } returns emptyList()

        val innsynsfilModel = eventService.createModel(digisosSak)

        assertThat(innsynsfilModel.saker).hasSize(2)
        assertThat(innsynsfilModel.saker).anyMatch { it.referanse == saksreferanse2 }
        assertThat(innsynsfilModel.saker).anyMatch { it.referanse == saksreferanse2 }
        assertThat(innsynsfilModel.saker.flatMap { it.vedtak }).hasSize(2)
        assertThat(innsynsfilModel.saker.flatMap { it.vedtak }).anyMatch { it.referanse == saksreferanse }
        assertThat(innsynsfilModel.saker.flatMap { it.vedtak }).anyMatch { it.referanse == saksreferanse2 }
    }
}
