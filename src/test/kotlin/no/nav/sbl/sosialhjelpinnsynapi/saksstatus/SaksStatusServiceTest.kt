package no.nav.sbl.sosialhjelpinnsynapi.saksstatus

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonAvsender
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonFilreferanse
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonHendelse
import no.nav.sbl.soknadsosialhjelp.digisos.soker.filreferanse.JsonDokumentlagerFilreferanse
import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonSaksStatus
import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonUtfall
import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonVedtakFattet
import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonVedtaksfil
import no.nav.sbl.sosialhjelpinnsynapi.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.domain.DigisosSak
import no.nav.sbl.sosialhjelpinnsynapi.domain.SaksStatusResponse
import no.nav.sbl.sosialhjelpinnsynapi.domain.UtfallEllerSaksStatus
import no.nav.sbl.sosialhjelpinnsynapi.fiks.DokumentlagerClient
import no.nav.sbl.sosialhjelpinnsynapi.fiks.FiksClient
import no.nav.sbl.sosialhjelpinnsynapi.soknadstatus.SOKNAD_MOTTATT
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val JSON_AVSENDER = JsonAvsender().withSystemnavn("test")
private val VERSION = "1.2.3"
private val SAKS_STATUS_UNDER_BEHANDLING = JsonSaksStatus()
        .withType(JsonHendelse.Type.SAKS_STATUS)
        .withHendelsestidspunkt(LocalDateTime.now().minusHours(10).format(DateTimeFormatter.ISO_DATE_TIME))
        .withStatus(JsonSaksStatus.Status.UNDER_BEHANDLING)
        .withTittel("Tittel")
        .withReferanse("Referanse")
private val SAKS_STATUS_IKKE_INNSYN = JsonSaksStatus()
        .withType(JsonHendelse.Type.SAKS_STATUS)
        .withHendelsestidspunkt(LocalDateTime.now().minusHours(8).format(DateTimeFormatter.ISO_DATE_TIME))
        .withStatus(JsonSaksStatus.Status.IKKE_INNSYN)
        .withTittel("Tittel - 2")
        .withReferanse("Referanse - 2")
private val VEDTAK_FATTET = JsonVedtakFattet()
        .withType(JsonHendelse.Type.VEDTAK_FATTET)
        .withHendelsestidspunkt(LocalDateTime.now().minusHours(6).format(DateTimeFormatter.ISO_DATE_TIME))
        .withReferanse("Referanse")
        .withVedtaksfil(JsonVedtaksfil().withReferanse(JsonDokumentlagerFilreferanse().withType(JsonFilreferanse.Type.DOKUMENTLAGER).withId("dokumentlagerId")))
        .withUtfall(JsonUtfall().withUtfall(JsonUtfall.Utfall.INNVILGET))

internal class SaksStatusServiceTest {
    private val clientProperties: ClientProperties = mockk(relaxed = true)
    private val fiksClient: FiksClient = mockk()
    private val dokumentlagerClient: DokumentlagerClient = mockk()

    private val service = SaksStatusService(clientProperties, fiksClient, dokumentlagerClient)

    private val mockDigisosSak: DigisosSak = mockk()

    @BeforeEach
    fun init() {
        clearMocks(fiksClient, dokumentlagerClient, mockDigisosSak)

        every { fiksClient.hentDigisosSak(any()) } returns mockDigisosSak
        every { mockDigisosSak.digisosSoker?.metadata } returns "123"
    }

    @Test
    fun `Skal returnere response med status = UNDER_BEHANDLING`() {
        val jsonDigisosSoker_med_saksStatus = JsonDigisosSoker()
                .withAvsender(JSON_AVSENDER)
                .withVersion(VERSION)
                .withHendelser(listOf(SAKS_STATUS_UNDER_BEHANDLING))

        every { dokumentlagerClient.hentDokument(any(), JsonDigisosSoker::class.java) } returns jsonDigisosSoker_med_saksStatus

        val response: List<SaksStatusResponse> = service.hentSaksStatuser("123")

        assertThat(response).isNotNull
        assertThat(response).hasSize(1)
        assertThat(response[0].status).isEqualTo(UtfallEllerSaksStatus.UNDER_BEHANDLING)
        assertThat(response[0].tittel).isEqualTo("Tittel")
        assertThat(response[0].vedtaksfilUrl).isNull()
    }

    @Test
    fun `Skal returnere response med status = INNVILGET og vedtaksfilUrl`() {
        val jsonDigisosSoker_med_saksStatus_og_vedtakfattet_samme_referanse = JsonDigisosSoker()
                .withAvsender(JSON_AVSENDER)
                .withVersion(VERSION)
                .withHendelser(listOf(
                        SAKS_STATUS_UNDER_BEHANDLING,
                        VEDTAK_FATTET))

        every { dokumentlagerClient.hentDokument(any(), JsonDigisosSoker::class.java) } returns jsonDigisosSoker_med_saksStatus_og_vedtakfattet_samme_referanse

        val response: List<SaksStatusResponse> = service.hentSaksStatuser("123")

        assertThat(response).isNotNull
        assertThat(response).hasSize(1)
        assertThat(response[0].status).isEqualTo(UtfallEllerSaksStatus.INNVILGET)
        assertThat(response[0].tittel).isEqualTo("Tittel")
        assertThat(response[0].vedtaksfilUrl).contains("/dokumentlager/nedlasting")
    }

    @Test
    fun `Skal returnere response med status = INNVILGET og vedtaksfilUrl uten saksStatus`() {
        val jsonDigisosSoker_med_vedtakfattet_uten_saksStatus = JsonDigisosSoker()
                .withAvsender(JSON_AVSENDER)
                .withVersion(VERSION)
                .withHendelser(listOf(VEDTAK_FATTET.withReferanse(null)))

        every { dokumentlagerClient.hentDokument(any(), JsonDigisosSoker::class.java) } returns jsonDigisosSoker_med_vedtakfattet_uten_saksStatus

        val response: List<SaksStatusResponse> = service.hentSaksStatuser("123")

        assertThat(response).isNotNull
        assertThat(response).hasSize(1)
        assertThat(response[0].status).isEqualTo(UtfallEllerSaksStatus.INNVILGET)
        assertThat(response[0].tittel).isEqualTo(DEFAULT_TITTEL)
        assertThat(response[0].vedtaksfilUrl).contains("/dokumentlager/nedlasting")
    }

    @Test
    fun `Skal returnere emptyList når JsonDigisosSoker ikke inneholder saksStatus eller vedtakFattet`() {
        val jsonDigisosSoker_uten_saksStatus_eller_vedtakFattet = JsonDigisosSoker()
                .withAvsender(JSON_AVSENDER)
                .withVersion(VERSION)
                .withHendelser(listOf(SOKNAD_MOTTATT))

        every { dokumentlagerClient.hentDokument(any(), JsonDigisosSoker::class.java) } returns jsonDigisosSoker_uten_saksStatus_eller_vedtakFattet

        val response: List<SaksStatusResponse> = service.hentSaksStatuser("123")

        assertThat(response).isNotNull
        assertThat(response).hasSize(0)
    }

    @Test
    fun `Skal returnere emptyList når JsonDigisosSoker er null`() {
        every { mockDigisosSak.digisosSoker } returns null

        val response: List<SaksStatusResponse> = service.hentSaksStatuser("123")

        assertThat(response).isNotNull
        assertThat(response).hasSize(0)
    }

    @Test
    fun `Skal returnere response med 3 elementer`() {
        val jsonDigisosSoker_med_2_vedtakfattet_og_2_saksStatuser = JsonDigisosSoker()
                .withAvsender(JSON_AVSENDER)
                .withVersion(VERSION)
                .withHendelser(listOf(
                        SAKS_STATUS_UNDER_BEHANDLING.withReferanse("A"),
                        VEDTAK_FATTET.withReferanse("A"),
                        SAKS_STATUS_UNDER_BEHANDLING.withReferanse("B"),
                        VEDTAK_FATTET.withReferanse(null)))

        every { dokumentlagerClient.hentDokument(any(), JsonDigisosSoker::class.java) } returns jsonDigisosSoker_med_2_vedtakfattet_og_2_saksStatuser

        val response: List<SaksStatusResponse> = service.hentSaksStatuser("123")

        assertThat(response).isNotNull
        assertThat(response).hasSize(3)
    }
}