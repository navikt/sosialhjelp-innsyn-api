package no.nav.sbl.sosialhjelpinnsynapi.virusscan

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import java.net.URI

@Configuration
class VirusScanConfig {

    private val DEFAULT_CLAM_URI = URI.create("http://clamav.nais.svc.nais.local/scan")

    @Value("\${innsyn.vedlegg.virusscan.enabled}")
    var enabled: Boolean = true

    fun getUri(): URI {
        return DEFAULT_CLAM_URI
    }
}