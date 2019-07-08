package no.nav.sbl.sosialhjelpinnsynapi.soknadsstatus

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonAvsender
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonHendelse
import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonSoknadsStatus
import no.nav.sbl.sosialhjelpinnsynapi.domain.InternalDigisosSoker
import no.nav.sbl.sosialhjelpinnsynapi.domain.SoknadsStatus
import no.nav.sbl.sosialhjelpinnsynapi.domain.SoknadsStatusResponse
import no.nav.sbl.sosialhjelpinnsynapi.event.EventService
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

    private val eventService: EventService = mockk()

    private val service = SoknadsStatusService(eventService)

    private val mockInternalDigisosSoker: InternalDigisosSoker = mockk()

    private val token = "token"

    @BeforeEach
    fun init() {
        clearMocks(eventService, mockInternalDigisosSoker)
    }

    @Test
    fun `Skal returnere mest nylige SoknadsStatus`() {
        every { eventService.createModel(any()) } returns mockInternalDigisosSoker
        every { mockInternalDigisosSoker.status } returns SoknadsStatus.UNDER_BEHANDLING

        val response: SoknadsStatusResponse = service.hentSoknadsStatus("123", token)

        assertThat(response).isNotNull
        assertThat(response.status).isEqualTo(SoknadsStatus.UNDER_BEHANDLING)
    }
}