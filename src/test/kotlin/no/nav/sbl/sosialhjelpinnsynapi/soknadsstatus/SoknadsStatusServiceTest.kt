package no.nav.sbl.sosialhjelpinnsynapi.soknadsstatus

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonAvsender
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonHendelse
import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonSoknadsStatus
import no.nav.sbl.sosialhjelpinnsynapi.domain.DigisosSak
import no.nav.sbl.sosialhjelpinnsynapi.domain.SoknadsStatus
import no.nav.sbl.sosialhjelpinnsynapi.domain.SoknadsStatusResponse
import no.nav.sbl.sosialhjelpinnsynapi.fiks.DokumentlagerClient
import no.nav.sbl.sosialhjelpinnsynapi.fiks.FiksClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val JSON_AVSENDER = JsonAvsender().withSystemnavn("test")
private val VERSION = "1.2.3"
val SOKNAD_MOTTATT: JsonSoknadsStatus = JsonSoknadsStatus()
        .withType(JsonHendelse.Type.SOKNADS_STATUS)
        .withHendelsestidspunkt(LocalDateTime.now().minusHours(10).format(DateTimeFormatter.ISO_DATE_TIME))
        .withStatus(JsonSoknadsStatus.Status.MOTTATT)

internal class SoknadsStatusServiceTest {

    private val fiksClient: FiksClient = mockk()
    private val dokumentlagerClient: DokumentlagerClient = mockk()

    private val service = SoknadsStatusService(fiksClient, dokumentlagerClient)

    private val mockDigisosSak: DigisosSak = mockk()

    private val token = "token"

    @BeforeEach
    fun init() {
        clearMocks(fiksClient, dokumentlagerClient, mockDigisosSak)
    }

    @Test
    fun `Skal returnere mest nylige SoknadsStatus`() {

        val response: SoknadsStatusResponse = service.hentSoknadsStatus("123", token)

        assertThat(response).isNotNull
        assertThat(response.status).isEqualTo(SoknadsStatus.UNDER_BEHANDLING)
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