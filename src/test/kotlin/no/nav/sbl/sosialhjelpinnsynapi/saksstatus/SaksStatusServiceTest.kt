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
import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.domain.DigisosSak
import no.nav.sbl.sosialhjelpinnsynapi.domain.SaksStatusResponse
import no.nav.sbl.sosialhjelpinnsynapi.domain.UtfallEllerSaksStatus
import no.nav.sbl.sosialhjelpinnsynapi.innsyn.InnsynService
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
private val SAKS_STATUS_2 = JsonSaksStatus()
        .withType(JsonHendelse.Type.SAKS_STATUS)
        .withHendelsestidspunkt(LocalDateTime.now().minusHours(9).format(DateTimeFormatter.ISO_DATE_TIME))
        .withStatus(JsonSaksStatus.Status.UNDER_BEHANDLING)
        .withTittel("Tittel")
        .withReferanse("B")
private val VEDTAK_FATTET = JsonVedtakFattet()
        .withType(JsonHendelse.Type.VEDTAK_FATTET)
        .withHendelsestidspunkt(LocalDateTime.now().minusHours(6).format(DateTimeFormatter.ISO_DATE_TIME))
        .withReferanse("Referanse")
        .withVedtaksfil(JsonVedtaksfil().withReferanse(JsonDokumentlagerFilreferanse().withType(JsonFilreferanse.Type.DOKUMENTLAGER).withId("dokumentlagerId")))
        .withUtfall(JsonUtfall().withUtfall(JsonUtfall.Utfall.INNVILGET))
private val VEDTAK_FATTET_REFERANSE_null = JsonVedtakFattet()
        .withType(JsonHendelse.Type.VEDTAK_FATTET)
        .withHendelsestidspunkt(LocalDateTime.now().minusHours(4).format(DateTimeFormatter.ISO_DATE_TIME))
        .withReferanse(null)
        .withVedtaksfil(JsonVedtaksfil().withReferanse(JsonDokumentlagerFilreferanse().withType(JsonFilreferanse.Type.DOKUMENTLAGER).withId("dokumentlagerId")))
        .withUtfall(JsonUtfall().withUtfall(JsonUtfall.Utfall.INNVILGET))

internal class SaksStatusServiceTest {
    private val clientProperties: ClientProperties = mockk(relaxed = true)
    private val innsynService: InnsynService = mockk()

    private val service = SaksStatusService(clientProperties, innsynService)

    private val mockDigisosSak: DigisosSak = mockk()

    private val token = "token"

    @BeforeEach
    fun init() {
        clearMocks(innsynService, mockDigisosSak)
    }

    @Test
    fun `Skal returnere response med status = UNDER_BEHANDLING`() {
        val jsonDigisosSoker_med_saksStatus = JsonDigisosSoker()
                .withAvsender(JSON_AVSENDER)
                .withVersion(VERSION)
                .withHendelser(listOf(SAKS_STATUS_UNDER_BEHANDLING))

        every { innsynService.hentJsonDigisosSoker(any(), token) } returns jsonDigisosSoker_med_saksStatus

        val response: List<SaksStatusResponse> = service.hentSaksStatuser("123", token)

        assertThat(response).isNotNull
        assertThat(response).hasSize(1)
        assertThat(response[0].status).isEqualTo(UtfallEllerSaksStatus.UNDER_BEHANDLING)
        assertThat(response[0].tittel).isEqualTo("Tittel")
        assertThat(response[0].vedtaksfilUrlList).isNull()
    }

    @Test
    fun `Skal returnere response med status = INNVILGET og vedtaksfilUrl`() {
        val jsonDigisosSoker_med_saksStatus_og_vedtakfattet_samme_referanse = JsonDigisosSoker()
                .withAvsender(JSON_AVSENDER)
                .withVersion(VERSION)
                .withHendelser(listOf(
                        SAKS_STATUS_UNDER_BEHANDLING,
                        VEDTAK_FATTET))

        every { innsynService.hentJsonDigisosSoker(any(), token) } returns jsonDigisosSoker_med_saksStatus_og_vedtakfattet_samme_referanse

        val response: List<SaksStatusResponse> = service.hentSaksStatuser("123", token)

        assertThat(response).isNotNull
        assertThat(response).hasSize(1)
        assertThat(response[0].status).isEqualTo(UtfallEllerSaksStatus.INNVILGET)
        assertThat(response[0].tittel).isEqualTo("Tittel")
        assertThat(response[0].vedtaksfilUrlList).isNotNull
        assertThat(response[0].vedtaksfilUrlList).hasSize(1)
        assertThat(response[0].vedtaksfilUrlList?.get(0)).contains("/dokumentlager/nedlasting")
    }

    @Test
    fun `Skal returnere response med status = INNVILGET og vedtaksfilUrl uten saksStatus`() {
        val jsonDigisosSoker_med_vedtakfattet_uten_saksStatus = JsonDigisosSoker()
                .withAvsender(JSON_AVSENDER)
                .withVersion(VERSION)
                .withHendelser(listOf(VEDTAK_FATTET_REFERANSE_null))

        every { innsynService.hentJsonDigisosSoker(any(), token) } returns jsonDigisosSoker_med_vedtakfattet_uten_saksStatus

        val response: List<SaksStatusResponse> = service.hentSaksStatuser("123", token)

        assertThat(response).isNotNull
        assertThat(response).hasSize(1)
        assertThat(response[0].status).isEqualTo(UtfallEllerSaksStatus.INNVILGET)
        assertThat(response[0].tittel).isEqualTo(DEFAULT_TITTEL)
        assertThat(response[0].vedtaksfilUrlList?.get(0)).contains("/dokumentlager/nedlasting")
    }

    @Test
    fun `Skal returnere emptyList når JsonDigisosSoker ikke inneholder saksStatus eller vedtakFattet`() {
        val jsonDigisosSoker_uten_saksStatus_eller_vedtakFattet = JsonDigisosSoker()
                .withAvsender(JSON_AVSENDER)
                .withVersion(VERSION)
                .withHendelser(listOf(SOKNAD_MOTTATT))

        every { innsynService.hentJsonDigisosSoker(any(), token) } returns jsonDigisosSoker_uten_saksStatus_eller_vedtakFattet

        val response: List<SaksStatusResponse> = service.hentSaksStatuser("123", token)

        assertThat(response).isNotNull
        assertThat(response).hasSize(0)
    }

    @Test
    fun `Skal returnere emptyList når JsonDigisosSoker er null`() {
        every { innsynService.hentJsonDigisosSoker(any(), token) } returns null

        val response: List<SaksStatusResponse> = service.hentSaksStatuser("123", token)

        assertThat(response).isNotNull
        assertThat(response).hasSize(0)
    }

    @Test
    fun `Skal returnere response med 3 elementer`() {
        val jsonDigisosSoker_med_2_vedtakfattet_og_2_saksStatuser = JsonDigisosSoker()
                .withAvsender(JSON_AVSENDER)
                .withVersion(VERSION)
                .withHendelser(listOf(
                        SAKS_STATUS_UNDER_BEHANDLING,
                        VEDTAK_FATTET,
                        SAKS_STATUS_2,
                        VEDTAK_FATTET_REFERANSE_null))

        every { innsynService.hentJsonDigisosSoker(any(), token) } returns jsonDigisosSoker_med_2_vedtakfattet_og_2_saksStatuser

        val response: List<SaksStatusResponse> = service.hentSaksStatuser("123", token)

        assertThat(response).isNotNull
        assertThat(response).hasSize(3)
        assertThat(response[0].tittel).isNotEqualTo(DEFAULT_TITTEL)
        assertThat(response[1].tittel).isNotEqualTo(DEFAULT_TITTEL)
        assertThat(response[2].tittel).isEqualTo(DEFAULT_TITTEL)

        assertThat(response[0].vedtaksfilUrlList).isNull()
        assertThat(response[1].vedtaksfilUrlList).hasSize(1)
        assertThat(response[2].vedtaksfilUrlList).hasSize(1)
    }
}