package no.nav.sosialhjelp.innsyn.client.virusscan

import no.nav.sosialhjelp.innsyn.common.OpplastingException
import no.nav.sosialhjelp.innsyn.utils.isRunningInProd
import no.nav.sosialhjelp.innsyn.utils.logger
import no.nav.sosialhjelp.innsyn.utils.typeRef
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient

interface VirusScanner {

    fun scan(filnavn: String?, data: ByteArray)
}

@Component
class VirusScanClient(
    private val virusScanWebClient: WebClient
) : VirusScanner {

    @Value("\${innsyn.vedlegg.virusscan.enabled}")
    var enabled: Boolean = true

    override fun scan(filnavn: String?, data: ByteArray) {
        if (enabled && isInfected(filnavn, data)) {
            throw OpplastingException("Fant virus i fil forsøkt opplastet", null)
        } else {
            log.warn("Virusscanning er ikke aktivert")
        }
    }

    private fun isInfected(filnavn: String?, data: ByteArray): Boolean {
        try {
            if (!isRunningInProd() && filnavn != null && filnavn.startsWith("virustest")) {
                return true
            }
            log.info("Scanner ${data.size} bytes for virus")

            val scanResults: List<ScanResult>? = virusScanWebClient.put()
                .body(BodyInserters.fromValue(data))
                .retrieve()
                .bodyToMono(typeRef<List<ScanResult>>())
                .block()

            if (scanResults!!.size != 1) {
                log.warn("Virusscan returnerte uventet respons med lengde ${scanResults.size}, forventet lengde er 1.")
                return false
            }
            val scanResult = scanResults[0]
            log.debug("Fikk scan result {}", scanResult)
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
    }
}