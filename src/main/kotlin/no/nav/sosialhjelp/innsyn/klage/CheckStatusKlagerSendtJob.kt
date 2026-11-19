package no.nav.sosialhjelp.innsyn.klage

import io.getunleash.Unleash
import kotlinx.coroutines.time.withTimeout
import no.nav.sosialhjelp.innsyn.app.LeaderElection
import no.nav.sosialhjelp.innsyn.klage.TimestampUtil.epochMillisToLocalDateTime
import no.nav.sosialhjelp.innsyn.klage.TimestampUtil.nowWithMillis
import no.nav.sosialhjelp.innsyn.klage.fiks.FiksKlageClient
import no.nav.sosialhjelp.innsyn.klage.fiks.KlageForsendelseStatus
import no.nav.sosialhjelp.innsyn.utils.logger
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDateTime
import java.util.UUID

@Component
class CheckStatusKlagerSendtJob(
    private val fiksKlageClient: FiksKlageClient,
    private val leaderElection: LeaderElection,
    private val klageMetricsService: KlageMetricsService,
    private val unleash: Unleash,
) {
    @Scheduled(cron = "0 0 */1 * * *", zone = "Europe/Oslo")
    suspend fun checkStatus() {
        if (leaderElection.isLeader() && unleash.isEnabled(SJEKK_TOGGLE)) {
            doCheckStatus()
        }
    }

    private suspend fun doCheckStatus() {
        val sporingsId = UUID.randomUUID().toString()
        val statuser =
            withTimeout(Duration.ofSeconds(30)) {
                fiksKlageClient.hentKlageStatus(
                    antallDager = ANTALL_DAGER_QUERY,
                    sporingsId = sporingsId,
                )
            }

        log.info(
            "Hentet status for {} klager siste {} dager. SporingsId: {}",
            statuser.size,
            ANTALL_DAGER_QUERY,
            sporingsId,
        )

        setIkkeMottatte(statuser.filter { !it.levertFagsystem })
    }

    private fun setIkkeMottatte(statusIkkeLevert: List<KlageForsendelseStatus>) {
        val now = nowWithMillis()

        val antallIkkeMotattInnenFrist = statusIkkeLevert.count { erEldreEnnToArbeidsdager(it.tidspunktEpochMillis, now) }
        klageMetricsService.setKlagerIkkeMottattGauge(antallIkkeMotattInnenFrist)

        if (antallIkkeMotattInnenFrist > 0) {
            log.warn(
                "Antall klager som ikke er mottatt av FS innen {} dager: {}",
                ANTALL_DAGER_LIMIT,
                antallIkkeMotattInnenFrist,
            )
        }
    }

    private fun erEldreEnnToArbeidsdager(
        tidspunktEpochMillis: Long,
        now: LocalDateTime,
    ): Boolean {
        val tidspunkt = epochMillisToLocalDateTime(tidspunktEpochMillis)
        var frist = tidspunkt
        repeat(ANTALL_DAGER_LIMIT) {
            do {
                frist = frist.plusDays(1)
            } while (frist.dayOfWeek == DayOfWeek.SATURDAY || frist.dayOfWeek == DayOfWeek.SUNDAY)
        }
        return now.isAfter(frist)
    }

    companion object {
        private const val ANTALL_DAGER_QUERY = 10
        private const val ANTALL_DAGER_LIMIT = 2
        private const val SJEKK_TOGGLE = "sosialhjelp.innsyn.klage.sjekkmottatt"
        private val log by logger()
    }
}
