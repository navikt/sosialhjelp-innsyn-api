package no.nav.sbl.sosialhjelpinnsynapi.service.virusscan

import no.nav.sbl.sosialhjelpinnsynapi.service.virusscan.Result.OK
import no.nav.sbl.sosialhjelpinnsynapi.utils.isRunningInProd
import no.nav.sbl.sosialhjelpinnsynapi.utils.logger
import org.springframework.http.RequestEntity
import org.springframework.web.client.RestOperations
import java.net.URI

internal class VirusScanConnection(
        private val config: VirusScanConfig,
        private val operations: RestOperations
) {

    val isEnabled: Boolean
        get() = config.enabled

    fun isInfected(filnavn: String?, data: ByteArray): Boolean {
        try {
            if (!isRunningInProd() && filnavn != null && filnavn.startsWith("virustest")) {
                return true
            }
            log.info("Scanner ${data.size} bytes for virus")
            val scanResults = putForObject(config.getUri(), data)
            if (scanResults!!.size != 1) {
                log.warn("Virusscan returnerte uventet respons med lengde ${scanResults.size}, forventet lengde er 1.")
                return false
            }
            val scanResult = scanResults[0]
            log.debug("Fikk scan result {}", scanResult)
            if (OK == scanResult.result) {
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

    private fun putForObject(uri: URI, payload: Any): Array<ScanResult>? {
        return operations.exchange<Array<ScanResult>>(RequestEntity.put(uri).body(payload), Array<ScanResult>::class.java).body
    }

    companion object {
        private val log by logger()
    }
}