package no.nav.sbl.sosialhjelpinnsynapi.virusscan

import no.nav.sbl.sosialhjelpinnsynapi.isRunningInProd
import no.nav.sbl.sosialhjelpinnsynapi.logger
import no.nav.sbl.sosialhjelpinnsynapi.virusscan.Result.OK
import org.springframework.http.RequestEntity
import org.springframework.web.client.RestOperations
import java.net.URI

internal class VirusScanConnection(private val config: VirusScanConfig, private val operations: RestOperations) {

    val isEnabled: Boolean
        get() = config.enabled

    fun isInfected(filnavn: String, data: ByteArray): Boolean {
        try {
            if (!isRunningInProd() && filnavn.startsWith("virustest")) {
                return true
            }
            log.info("Scanner {} bytes", data.size)
            val scanResults = putForObject(config.getUri(), data)
            if (scanResults!!.size != 1) {
                log.warn("Uventet respons med lengde {}, forventet lengde er 1", scanResults.size)
                return false
            }
            val scanResult = scanResults[0]
            log.info("Fikk scan result {}", scanResult)
            if (OK == scanResult.result) {
                log.info("Ingen virus i {}", filnavn)
                return false
            }
            log.warn("Fant virus i {}, status {}", filnavn, scanResult.result)
            return true
        } catch (e: Exception) {
            log.warn("Kunne ikke scanne {}", filnavn, e)
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