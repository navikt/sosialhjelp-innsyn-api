package no.nav.sbl.sosialhjelpinnsynapi.virusscan

import no.nav.sbl.sosialhjelpinnsynapi.common.OpplastingException
import no.nav.sbl.sosialhjelpinnsynapi.logger
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

@Component
class ClamAvVirusScanner(config: VirusScanConfig, restTemplate: RestTemplate) : VirusScanner {

    private val connection: VirusScanConnection = VirusScanConnection(config, restTemplate)

    @Throws(OpplastingException::class)
    override fun scan(filnavn: String, data: ByteArray) {
        if (connection.isEnabled && connection.isInfected(filnavn, data)) {
            throw OpplastingException("Fant virus i $filnavn", null)
        } else if (!connection.isEnabled) {
            log.info("Virusscanning er ikke aktivert")
        }
    }

    companion object {
        private val log by logger()
    }
}