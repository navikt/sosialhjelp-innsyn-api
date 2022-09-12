package no.nav.sosialhjelp.innsyn.vedlegg.virusscan

import no.nav.sosialhjelp.innsyn.app.MiljoUtils.isRunningInProd
import no.nav.sosialhjelp.innsyn.app.client.RetryUtils
import no.nav.sosialhjelp.innsyn.app.exceptions.BadStateException
import no.nav.sosialhjelp.innsyn.app.exceptions.VirusScanException
import no.nav.sosialhjelp.innsyn.utils.logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

@Component
class VirusScanner(
    private val virusScanWebClient: WebClient,
    @Value("\${innsyn.vedlegg.virusscan.enabled}") val enabled: Boolean
) {

    fun scan(filnavn: String?, data: ByteArray) {
        if (enabled && isInfected(filnavn, data)) {
            throw VirusScanException("Fant virus i fil forsøkt opplastet", null)
        } else if (!enabled) {
            log.warn("Virusscanning er ikke aktivert")
        }
    }

    private fun isInfected(filnavn: String?, data: ByteArray): Boolean {
        try {
            if (!isRunningInProd() && filnavn != null && filnavn.startsWith("virustest")) {
                return true
            }
            log.info("Scanner ${data.size} bytes for virus")

            val scanResults: List<ScanResult> = virusScanWebClient.put()
                .body(BodyInserters.fromValue(data))
                .retrieve()
                .bodyToMono<List<ScanResult>>()
                .retryWhen(RetryUtils.DEFAULT_RETRY_SERVER_ERRORS)
                .block()
                ?: throw BadStateException("scanResult er null")

            if (scanResults.size != 1) {
                log.warn("Virusscan returnerte uventet respons med lengde ${scanResults.size}, forventet lengde er 1.")
                return false
            }
            val scanResult = scanResults[0]
            log.debug("Fikk scan result $scanResult")
            if (Result.OK == scanResult.result) {
                log.info("Ingen virus i fil (${data.size} bytes)")
                return false
            }
            log.warn("Fant virus med status ${scanResult.result} i fil forsøkt opplastet")
            return true
        } catch (e: Exception) {
            log.warn("Kunne ikke scanne fil opplastet", e)
            return false
        }
    }

    companion object {
        private val log by logger()

        private const val RETRY_ATTEMPTS = 5
        private const val INITIAL_DELAY = 100L
        private const val MAX_DELAY = 2000L
    }
}
