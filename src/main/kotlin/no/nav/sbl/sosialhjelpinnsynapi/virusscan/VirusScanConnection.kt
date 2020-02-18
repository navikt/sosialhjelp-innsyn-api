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

    fun isInfected(filnavn: String?, data: ByteArray, digisosId: String): Boolean {
        try {
            if (!isRunningInProd() && filnavn != null && filnavn.startsWith("virustest")) {
                return true
            }
            log.info("Scanner {} bytes for virus", data.size)
            val scanResults = putForObject(config.getUri(), data)
            if (scanResults!!.size != 1) {
                log.warn("Virusscan returnerte uventet respons med lengde ${scanResults.size}, forventet lengde er 1. digisosId=$digisosId")
                return false
            }
            val scanResult = scanResults[0]
            log.debug("Fikk scan result {}", scanResult)
            if (OK == scanResult.result) {
                log.debug("Ingen virus i fil")
                return false
            }
            log.warn("Fant virus med status ${scanResult.result} i fil forsøkt opplastet til digisosId=$digisosId")
            return true
        } catch (e: Exception) {
            log.warn("Kunne ikke scanne fil opplastet til digisosId=$digisosId", e)
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