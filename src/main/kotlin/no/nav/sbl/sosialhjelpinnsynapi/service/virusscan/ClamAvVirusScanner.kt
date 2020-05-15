package no.nav.sbl.sosialhjelpinnsynapi.service.virusscan

import no.nav.sbl.sosialhjelpinnsynapi.common.OpplastingException
import no.nav.sbl.sosialhjelpinnsynapi.utils.logger
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

@Component
class ClamAvVirusScanner(
        config: VirusScanConfig,
        restTemplate: RestTemplate
) : VirusScanner {

    private val connection: VirusScanConnection = VirusScanConnection(config, restTemplate)

    @Throws(OpplastingException::class)
    override fun scan(filnavn: String?, data: ByteArray, digisosId: String) {
        if (connection.isEnabled && connection.isInfected(filnavn, data, digisosId)) {
            throw OpplastingException("Fant virus i fil forsøkt opplastet til digisosId=$digisosId", null)
        } else if (!connection.isEnabled) {
            log.warn("Virusscanning er ikke aktivert")
        }
    }

    companion object {
        private val log by logger()
    }
}