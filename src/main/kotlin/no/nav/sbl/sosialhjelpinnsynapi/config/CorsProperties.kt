package no.nav.sbl.sosialhjelpinnsynapi.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component


@Component
@ConfigurationProperties(prefix = "cors")
class CorsProperties {

    lateinit var allowedOrigins: Array<String>

}
