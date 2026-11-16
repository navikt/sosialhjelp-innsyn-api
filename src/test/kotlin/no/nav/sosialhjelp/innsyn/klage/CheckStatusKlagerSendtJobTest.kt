package no.nav.sosialhjelp.innsyn.klage

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.test.runTest
import no.nav.sosialhjelp.innsyn.app.LeaderElection
import no.nav.sosialhjelp.innsyn.klage.fiks.FiksKlageClient
import no.nav.sosialhjelp.innsyn.klage.fiks.KlageForsendelseStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

internal class CheckStatusKlagerSendtJobTest {
    private val fiksKlageClient: FiksKlageClient = mockk()
    private val leaderElection: LeaderElection = mockk()
    private val meterRegistry = SimpleMeterRegistry()
    private val job = CheckStatusKlagerSendtJob(fiksKlageClient, leaderElection, KlageMetricsService(meterRegistry))

    @BeforeEach
    fun setUp() {
        mockkObject(TimestampUtil)
        coEvery { leaderElection.isLeader() } returns true
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(TimestampUtil)
        meterRegistry.close()
    }

    @ParameterizedTest
    @CsvSource(
        "2026-11-11T16:00, 2026-11-13T15:59:59.999, false, 0",
        "2026-11-11T16:00, 2026-11-13T16:00, false, 0",
        "2026-11-11T16:00, 2026-11-13T16:00:00.001, false, 1",
        "2026-11-12T16:00, 2026-11-14T17:00, false, 0",
        "2026-11-12T16:00, 2026-11-15T17:00, false, 0",
        "2026-11-12T16:00, 2026-11-16T16:00, false, 0",
        "2026-11-12T16:00, 2026-11-16T16:00:00.001, false, 1",
        "2026-11-13T16:00, 2026-11-16T17:00, false, 0",
        "2026-11-13T16:00, 2026-11-17T15:59:59.999, false, 0",
        "2026-11-13T16:00, 2026-11-17T16:00, false, 0",
        "2026-11-13T16:00, 2026-11-17T16:00:00.001, false, 1",
        "2026-11-14T16:00, 2026-11-17T16:00, false, 0",
        "2026-11-14T16:00, 2026-11-17T16:00:00.001, false, 1",
        "2026-11-15T16:00, 2026-11-17T16:00, false, 0",
        "2026-11-15T16:00, 2026-11-17T16:00:00.001, false, 1",
        "2026-11-11T16:00, 2026-11-14T17:00, false, 1",
        "2026-11-11T16:00, 2026-11-16T16:00, true, 0",
        "2026-11-17T16:00, 2026-11-16T16:00, false, 0",
        "2026-10-23T16:00, 2026-10-27T16:00, false, 0",
        "2026-10-23T16:00, 2026-10-27T16:00:00.001, false, 1",
    )
    fun `teller bare ikke-leverte klager eldre enn to arbeidsdager og nullstiller ved tom liste`(
        sendtTidspunkt: String,
        naa: String,
        levertFagsystem: Boolean,
        forventetAntall: Double,
    ) = runTest {
        every { TimestampUtil.nowWithMillis() } returns LocalDateTime.parse(naa)
        val tidspunkt = LocalDateTime.parse(sendtTidspunkt).atZone(ZoneId.of("Europe/Oslo")).toInstant().toEpochMilli()
        val status =
            KlageForsendelseStatus(
                digisosId = UUID.randomUUID(),
                klageId = UUID.randomUUID(),
                levertFagsystem = levertFagsystem,
                tidspunktEpochMillis = tidspunkt,
                kommunenr = "0301",
            )
        coEvery { fiksKlageClient.hentKlageStatus(10, any()) } returns listOf(status)

        job.checkStatus()

        assertThat(meterRegistry.get("sosialhjelp.klage.ikke.mottatt").gauge().value()).isEqualTo(forventetAntall)

        coEvery { fiksKlageClient.hentKlageStatus(10, any()) } returns emptyList()

        job.checkStatus()

        assertThat(meterRegistry.get("sosialhjelp.klage.ikke.mottatt").gauge().value()).isZero()
    }
}
