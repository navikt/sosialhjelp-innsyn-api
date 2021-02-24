package no.nav.sosialhjelp.innsyn.service.virusscan

import no.nav.sosialhjelp.innsyn.common.OpplastingException
import no.nav.sosialhjelp.innsyn.utils.logger
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

@Component
class ClamAvVirusScanner(
        config: VirusScanConfig,
        restTemplate: RestTemplate
) : VirusScanner {

    private val connection: VirusScanConnection = VirusScanConnection(config, restTemplate)

    @Throws(OpplastingException::class)
    override fun scan(filnavn: String?, data: ByteArray) {
        if (connection.isEnabled && connection.isInfected(filnavn, data)) {
            throw OpplastingException("Fant virus i fil forsøkt opplastet", null)
        } else if (!connection.isEnabled) {
            log.warn("Virusscanning er ikke aktivert")
        }
    }

    companion object {
        private val log by logger()
    }
}