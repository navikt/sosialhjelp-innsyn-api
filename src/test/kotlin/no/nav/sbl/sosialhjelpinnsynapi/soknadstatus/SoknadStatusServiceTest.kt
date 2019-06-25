package no.nav.sbl.sosialhjelpinnsynapi.soknadstatus

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonAvsender
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonHendelse
import no.nav.sbl.soknadsosialhjelp.digisos.soker.filreferanse.JsonDokumentlagerFilreferanse
import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.*
import no.nav.sbl.sosialhjelpinnsynapi.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.domain.DigisosSak
import no.nav.sbl.sosialhjelpinnsynapi.domain.SoknadStatus
import no.nav.sbl.sosialhjelpinnsynapi.domain.SoknadStatusResponse
import no.nav.sbl.sosialhjelpinnsynapi.fiks.DokumentlagerClient
import no.nav.sbl.sosialhjelpinnsynapi.fiks.FiksClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val JSON_AVSENDER = JsonAvsender().withSystemnavn("test")
private val VERSION = "1.2.3"
private val SOKNAD_MOTTATT = JsonSoknadsStatus()
        .withType(JsonHendelse.Type.SOKNADS_STATUS)
        .withHendelsestidspunkt(LocalDateTime.now().minusHours(10).format(DateTimeFormatter.ISO_DATE_TIME))
        .withStatus(JsonSoknadsStatus.Status.MOTTATT)

internal class SoknadStatusServiceTest {

    val clientProperties: ClientProperties = mockk(relaxed = true)
    val fiksClient: FiksClient = mockk()
    val dokumentlagerClient: DokumentlagerClient = mockk()

    val service = SoknadStatusService(clientProperties, fiksClient, dokumentlagerClient)

    val mockDigisosSak: DigisosSak = mockk()

    @BeforeEach
    fun init() {
        clearMocks(fiksClient, dokumentlagerClient, mockDigisosSak)
    }

    @Test
    fun `Skal returnere mest nylige SoknadStatus`() {
        every { fiksClient.hentDigisosSak(any()) } returns mockDigisosSak
        every { mockDigisosSak.digisosSoker?.metadata } returns "123"
        every { dokumentlagerClient.hentDokument(any(), JsonDigisosSoker::class.java) } returns jsonDigisosSoker_underbehandling

        val response: SoknadStatusResponse = service.hentSoknadStatus("123")

        assertThat(response).isNotNull
        assertThat(response.status).isEqualTo(SoknadStatus.UNDER_BEHANDLING)
    }

    @Test
    fun `Skal returnere response uten vedtaksinfo HVIS digisosSoker er null`() {
        every { fiksClient.hentDigisosSak(any()) } returns mockDigisosSak
        every { mockDigisosSak.digisosSoker } returns null

        val response: SoknadStatusResponse = service.hentSoknadStatus("123")

        assertThat(response).isNotNull
        assertThat(response.status).isEqualTo(SoknadStatus.SENDT)
        assertThat(response.vedtaksinfo).isNull()
    }

    @Test
    fun `Skal returnere Response med vedtaksinfo HVIS VedtakFattet finnes UTEN referanse`() {
        every { fiksClient.hentDigisosSak(any()) } returns mockDigisosSak
        every { mockDigisosSak.digisosSoker?.metadata } returns "123"
        every { dokumentlagerClient.hentDokument(any(), JsonDigisosSoker::class.java) } returns jsonDigisosSoker_med_vedtakFattet

        val response: SoknadStatusResponse = service.hentSoknadStatus("123")

        assertThat(response).isNotNull
        assertThat(response.status).isEqualTo(SoknadStatus.MOTTATT)
        assertThat(response.vedtaksinfo).contains("/dokumentlager/nedlasting/")
    }

    @Test
    fun `Skal returnere Response uten vedtaksinfo HVIS SaksStatus finnes`() {
        every { fiksClient.hentDigisosSak(any()) } returns mockDigisosSak
        every { mockDigisosSak.digisosSoker?.metadata } returns "123"
        every { dokumentlagerClient.hentDokument(any(), JsonDigisosSoker::class.java) } returns jsonDigisosSoker_med_saksstatus

        val response: SoknadStatusResponse = service.hentSoknadStatus("123")

        assertThat(response).isNotNull
        assertThat(response.status).isEqualTo(SoknadStatus.MOTTATT)
        assertThat(response.vedtaksinfo).isNull()
    }

    @Test
    fun `Skal returnere Response uten vedtaksinfo HVIS VedtakFattet har referanse satt OG `() {
        every { fiksClient.hentDigisosSak(any()) } returns mockDigisosSak
        every { mockDigisosSak.digisosSoker?.metadata } returns "123"
        every { dokumentlagerClient.hentDokument(any(), JsonDigisosSoker::class.java) } returns jsonDigisosSoker_med_vedtakFattet_og_referanse_ulik_null

        val response: SoknadStatusResponse = service.hentSoknadStatus("123")

        assertThat(response).isNotNull
        assertThat(response.status).isEqualTo(SoknadStatus.MOTTATT)
        assertThat(response.vedtaksinfo).isNull()
    }

    private val jsonDigisosSoker_underbehandling: JsonDigisosSoker = JsonDigisosSoker()
            .withAvsender(JSON_AVSENDER)
            .withVersion(VERSION)
            .withHendelser(listOf(
                    SOKNAD_MOTTATT,
                    JsonSoknadsStatus()
                            .withType(JsonHendelse.Type.SOKNADS_STATUS)
                            .withHendelsestidspunkt(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME))
                            .withStatus(JsonSoknadsStatus.Status.UNDER_BEHANDLING)

            ))

    private val jsonDigisosSoker_med_saksstatus: JsonDigisosSoker = JsonDigisosSoker()
            .withAvsender(JSON_AVSENDER)
            .withVersion(VERSION)
            .withHendelser(listOf(
                    SOKNAD_MOTTATT,
                    JsonSaksStatus()
                            .withType(JsonHendelse.Type.SAKS_STATUS)
                            .withHendelsestidspunkt(LocalDateTime.now().minusHours(2).format(DateTimeFormatter.ISO_DATE_TIME))
                            .withStatus(JsonSaksStatus.Status.UNDER_BEHANDLING),
                    JsonVedtakFattet()
                            .withType(JsonHendelse.Type.VEDTAK_FATTET)
                            .withHendelsestidspunkt(LocalDateTime.now().minusHours(1).format(DateTimeFormatter.ISO_DATE_TIME))
                            .withVedtaksfil(JsonVedtaksfil()
                                    .withReferanse(JsonDokumentlagerFilreferanse()
                                            .withId("123")))
                            .withReferanse(null)
                            .withUtfall(JsonUtfall().withUtfall(JsonUtfall.Utfall.INNVILGET))

            ))

    private val jsonDigisosSoker_med_vedtakFattet: JsonDigisosSoker = JsonDigisosSoker()
            .withAvsender(JSON_AVSENDER)
            .withVersion(VERSION)
            .withHendelser(listOf(
                    SOKNAD_MOTTATT,
                    JsonVedtakFattet()
                            .withType(JsonHendelse.Type.VEDTAK_FATTET)
                            .withHendelsestidspunkt(LocalDateTime.now().minusHours(1).format(DateTimeFormatter.ISO_DATE_TIME))
                            .withVedtaksfil(JsonVedtaksfil()
                                    .withReferanse(JsonDokumentlagerFilreferanse()
                                            .withId("123")))
                            .withReferanse(null)
                            .withUtfall(JsonUtfall().withUtfall(JsonUtfall.Utfall.INNVILGET))

            ))

    private val jsonDigisosSoker_med_vedtakFattet_og_referanse_ulik_null: JsonDigisosSoker = JsonDigisosSoker()
            .withAvsender(JSON_AVSENDER)
            .withVersion(VERSION)
            .withHendelser(listOf(
                    SOKNAD_MOTTATT,
                    JsonVedtakFattet()
                            .withType(JsonHendelse.Type.VEDTAK_FATTET)
                            .withHendelsestidspunkt(LocalDateTime.now().minusHours(1).format(DateTimeFormatter.ISO_DATE_TIME))
                            .withVedtaksfil(JsonVedtaksfil()
                                    .withReferanse(JsonDokumentlagerFilreferanse()
                                            .withId("123")))
                            .withReferanse("ReferanseTilSak")
                            .withUtfall(JsonUtfall().withUtfall(JsonUtfall.Utfall.INNVILGET))

            ))
}