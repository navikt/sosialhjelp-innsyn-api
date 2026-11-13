package no.nav.sosialhjelp.innsyn.klage

import kotlinx.coroutines.time.withTimeout
import no.nav.sosialhjelp.innsyn.klage.fiks.FiksKlageClient
import no.nav.sosialhjelp.innsyn.utils.logger
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.UUID

@Component
class CheckStatusKlagerSendtJob(
    private val fiksKlageClient: FiksKlageClient,
) {
    @Scheduled(cron = "0 0 4 * * *", zone = "Europe/Oslo")
    suspend fun checkStatus() {
        val sporingsId = UUID.randomUUID().toString()
        val statuser =
            withTimeout(Duration.ofSeconds(30)) {
                fiksKlageClient.hentKlageStatus(
                    antallDager = ANTALL_DAGER_QUERY,
                    sporingsId = sporingsId,
                )
            }
        val levertTilFagsystem = statuser.count { it.levertFagsystem }
        val ikkeLevertTilFagsystem = statuser.size - levertTilFagsystem

        log.info(
            "Hentet status for {} klager siste {} dager. Levert til fagsystem: {}, ikke levert: {}. SporingsId: {}",
            statuser.size,
            ANTALL_DAGER_LIMIT,
            levertTilFagsystem,
            ikkeLevertTilFagsystem,
            sporingsId,
        )
    }

    companion object {
        private const val ANTALL_DAGER_QUERY = 10
        private const val ANTALL_DAGER_LIMIT = 2
        private val log by logger()
    }
}
