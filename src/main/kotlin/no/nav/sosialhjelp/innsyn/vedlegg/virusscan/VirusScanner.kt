package no.nav.sosialhjelp.innsyn.vedlegg.virusscan

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.withContext
import no.nav.sosialhjelp.innsyn.app.MiljoUtils.isRunningInProd
import no.nav.sosialhjelp.innsyn.app.client.RetryUtils
import no.nav.sosialhjelp.innsyn.app.exceptions.BadStateException
import no.nav.sosialhjelp.innsyn.app.exceptions.VirusScanException
import no.nav.sosialhjelp.innsyn.utils.logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

@Component
class VirusScanner(
    private val virusScanWebClient: WebClient,
    @Value("\${innsyn.vedlegg.virusscan.enabled}") val enabled: Boolean,
) {
    private val virusScanRetry = RetryUtils.retryBackoffSpec()

    suspend fun scan(
        filnavn: String?,
        data: FilePart,
    ) {
        if (enabled && isInfected(filnavn, data)) {
            throw VirusScanException("Fant virus i fil forsøkt opplastet", null)
        } else if (!enabled) {
            log.warn("Virusscanning er ikke aktivert")
        }
    }

    private suspend fun isInfected(
        filnavn: String?,
        data: FilePart,
    ): Boolean {
        try {
            if (!isRunningInProd() && filnavn != null && filnavn.startsWith("virustest")) {
                return true
            }

            val size = data.headers().contentLength
            log.info("Scanner $size bytes for virus")

            val scanResults: List<ScanResult> =
                withContext(Dispatchers.IO) {
                    virusScanWebClient.put()
                        .body(BodyInserters.fromDataBuffers(data.content()))
                        .retrieve()
                        .bodyToMono<List<ScanResult>>()
                        .retryWhen(virusScanRetry)
                        .awaitSingleOrNull()
                        ?: throw BadStateException("scanResult er null")
                }

            if (scanResults.size != 1) {
                log.warn("Virusscan returnerte uventet respons med lengde ${scanResults.size}, forventet lengde er 1.")
                return false
            }
            val scanResult = scanResults[0]
            log.debug("Fikk scan result $scanResult")
            if (Result.OK == scanResult.result) {
                log.info("Ingen virus i fil ($size bytes)")
                return false
            }
            log.warn("Fant virus med status ${scanResult.result} i fil forsøkt opplastet")
            return true
        } catch (e: Exception) {
            if (e is CancellationException) currentCoroutineContext().ensureActive()
            log.warn("Kunne ikke scanne fil opplastet", e)
            return false
        }
    }

    companion object {
        private val log by logger()
    }
}
