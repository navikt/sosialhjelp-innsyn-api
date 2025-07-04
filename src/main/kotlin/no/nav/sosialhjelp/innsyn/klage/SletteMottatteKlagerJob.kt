package no.nav.sosialhjelp.innsyn.klage

import java.util.UUID
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.withTimeoutOrNull
import no.nav.sosialhjelp.innsyn.app.leaderelection.LeaderElection
import no.nav.sosialhjelp.innsyn.utils.logger
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class SletteMottatteKlagerJob(
    private val leaderElection: LeaderElection,
    private val fiksKlageClient: FiksKlageClient,
    private val klageService: ScheduledKlageService,
) {

    @Scheduled
    suspend fun slettMottatteKlager() {
        if (leaderElection.isLeader()) {
            logger.info("Starter sletting av klager med status MOTTATT fra Fiks")
            doDeleteMottatteKlager()
            logNumberOfStatusSendtAfterDeletion()
            logger.info("Jobb for sletting av klager med status MOTTATT fra Fiks ferdig")
        }
    }

    private suspend fun doDeleteMottatteKlager() {
        withTimeoutOrNull(60.seconds) {
            klageService.findKlagerStatusSendt()
                .map { klageRef -> klageRef.digisosId }
                .distinct()
                .flatMap { digisosId -> doGetKlageFromFiks(digisosId) }
                .filter { fiksKlage -> fiksKlage.status == KlageStatus.MOTTATT }
                .map { fiksKlage -> fiksKlage.klageId }
                .also { klageIds ->
                    if (klageIds.isNotEmpty()) {
                        logger.info("Sletter ${klageIds.size} klager med status MOTTATT")
                        klageService.slettKlagerMottattAvFagsysten(klageIds)
                    }
                }
        }
    }

    private suspend fun logNumberOfStatusSendtAfterDeletion() {
        klageService.findKlagerStatusSendt()
            .also { klager ->
                if (klager.isNotEmpty()) logger.info("Antall klager med status SENDT etter sletting: ${klager.size}")
            }
    }

    suspend fun doGetKlageFromFiks(digisosId: UUID): List<Klage> =
        runCatching { fiksKlageClient.hentKlager(digisosId) }
            .onFailure { logger.error("Feil ved henting av klager fra fiks for digisosId=$digisosId", it) }
            .getOrElse { emptyList() }

    companion object {
        private val logger by logger()
    }
}
