package no.nav.sbl.sosialhjelpinnsynapi.soknadstatus

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonAvsender
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonHendelse
import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonSoknadsStatus
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

    private val clientProperties: ClientProperties = mockk(relaxed = true)
    private val fiksClient: FiksClient = mockk()
    private val dokumentlagerClient: DokumentlagerClient = mockk()

    private val service = SoknadStatusService(clientProperties, fiksClient, dokumentlagerClient)

    private val mockDigisosSak: DigisosSak = mockk()

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
}