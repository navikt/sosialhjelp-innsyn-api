package no.nav.sosialhjelp.innsyn.digisossak.utbetalinger2

import io.getunleash.Unleash
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.innsyn.digisosapi.FiksService
import no.nav.sosialhjelp.innsyn.domain.InternalDigisosSoker
import no.nav.sosialhjelp.innsyn.domain.Utbetaling
import no.nav.sosialhjelp.innsyn.domain.UtbetalingsStatus
import no.nav.sosialhjelp.innsyn.event.EventService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

class UtbetalingerServiceTest {
    private val fiksService = mockk<FiksService>()
    private val eventService = mockk<EventService>()
    private val unleash = mockk<Unleash>()
    private val meterRegistry = SimpleMeterRegistry()
    private val service = UtbetalingerService(fiksService, eventService, unleash, meterRegistry)

    private fun sak(
        id: String,
        kommune: String,
    ) = mockk<DigisosSak>(relaxed = true) {
        every { fiksDigisosId } returns id
        every { kommunenummer } returns kommune
    }

    private fun utbetaling(referanse: String) =
        Utbetaling(
            referanse = referanse,
            status = UtbetalingsStatus.UTBETALT,
            belop = BigDecimal.TEN,
            beskrivelse = null,
            forfallsDato = LocalDate.now(),
            utbetalingsDato = LocalDate.now(),
            stoppetDato = null,
            fom = null,
            tom = null,
            mottaker = null,
            annenMottaker = false,
            kontonummer = null,
            utbetalingsmetode = null,
            vilkar = mutableListOf(),
            dokumentasjonkrav = mutableListOf(),
            datoHendelse = LocalDateTime.now(),
        )

    private fun counter(kommune: String) = meterRegistry.counter("innsyn_utbetalinger_hentet_total", "kommunenummer", kommune).count()

    @Test
    fun `teller utbetalinger per kommune i bulk og uten bulk`() {
        listOf(true, false).forEach { bulk ->
            val a = sak("a", "0301")
            val b = sak("b", "1103")
            val ma = InternalDigisosSoker(fiksDigisosId = "a", utbetalinger = mutableListOf(utbetaling("1"), utbetaling("2")))
            val mb = InternalDigisosSoker(fiksDigisosId = "b", utbetalinger = mutableListOf(utbetaling("3")))
            coEvery { fiksService.getAllSoknader() } returns listOf(a, b)
            every { unleash.isEnabled(any<String>()) } returns bulk
            coEvery { eventService.hentAlleUtbetalingerBulk(any()) } returns listOf(ma, mb)
            coEvery { eventService.hentAlleUtbetalinger(a) } returns ma
            coEvery { eventService.hentAlleUtbetalinger(b) } returns mb

            val before0301 = counter("0301")
            val before1103 = counter("1103")
            runBlocking { service.hentUtbetalinger() }

            assertEquals(2.0, counter("0301") - before0301)
            assertEquals(1.0, counter("1103") - before1103)
        }
    }

    @Test
    fun `samme referanse i flere saker telles en gang`() {
        val a = sak("a", "0301")
        val b = sak("b", "1103")
        val ma = InternalDigisosSoker(fiksDigisosId = "a", utbetalinger = mutableListOf(utbetaling("1")))
        val mb = InternalDigisosSoker(fiksDigisosId = "b", utbetalinger = mutableListOf(utbetaling("1")))
        coEvery { fiksService.getAllSoknader() } returns listOf(a, b)
        every { unleash.isEnabled(any<String>()) } returns true
        coEvery { eventService.hentAlleUtbetalingerBulk(any()) } returns listOf(ma, mb)

        runBlocking { service.hentUtbetalinger() }

        assertEquals(1.0, counter("0301") + counter("1103"))
    }
}
