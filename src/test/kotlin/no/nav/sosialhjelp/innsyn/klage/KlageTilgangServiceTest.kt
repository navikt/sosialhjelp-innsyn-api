package no.nav.sosialhjelp.innsyn.klage

import io.getunleash.Unleash
import io.getunleash.UnleashContext
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import no.nav.sosialhjelp.innsyn.digisosapi.FiksService
import no.nav.sosialhjelp.innsyn.domain.InternalDigisosSoker
import no.nav.sosialhjelp.innsyn.domain.Soknadsmottaker
import no.nav.sosialhjelp.innsyn.event.EventService
import no.nav.sosialhjelp.innsyn.responses.ok_digisossak_response
import no.nav.sosialhjelp.innsyn.utils.sosialhjelpJsonMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import tools.jackson.module.kotlin.readValue
import java.util.UUID

class KlageTilgangServiceTest {
    private val fiksService: FiksService = mockk()
    private val eventService: EventService = mockk()
    private val unleash: Unleash = mockk()
    private val service = KlageTilgangService(fiksService, eventService, unleash)

    private fun mottaker(enhetsnummer: String?) {
        coEvery { fiksService.getSoknad(any()) } returns sosialhjelpJsonMapper.readValue(ok_digisossak_response)
        coEvery { eventService.createModel(any()) } returns
            InternalDigisosSoker(soknadsmottaker = Soknadsmottaker(enhetsnummer, "Enhet"))
    }

    @Test
    fun `sender enhetsnummer og kommunenummer i context og returnerer true`() =
        runBlocking<Unit> {
            mottaker("1234")
            val ctx = slot<UnleashContext>()
            every { unleash.isEnabled(KlageTilgangService.KLAGE_TOGGLE, capture(ctx)) } returns true

            assertThat(service.kanSendeKlage(UUID.randomUUID())).isTrue()
            assertThat(ctx.captured.properties["enhetsnummer"]).isEqualTo("1234")
            assertThat(ctx.captured.properties).containsKey("kommunenummer")
        }

    @Test
    fun `toggle av gir false`() =
        runBlocking<Unit> {
            mottaker("1234")
            every { unleash.isEnabled(any<String>(), any<UnleashContext>()) } returns false

            assertThat(service.kanSendeKlage(UUID.randomUUID())).isFalse()
        }

    @Test
    fun `manglende enhetsnummer sendes ikke i context`() =
        runBlocking<Unit> {
            mottaker(null)
            val ctx = slot<UnleashContext>()
            every { unleash.isEnabled(KlageTilgangService.KLAGE_TOGGLE, capture(ctx)) } returns false

            assertThat(service.kanSendeKlage(UUID.randomUUID())).isFalse()
            assertThat(ctx.captured.properties).doesNotContainKey("enhetsnummer")
            assertThat(ctx.captured.properties).containsKey("kommunenummer")
        }
}
