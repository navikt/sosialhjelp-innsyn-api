package no.nav.sbl.sosialhjelpinnsynapi.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import kotlin.properties.Delegates

@Component
@ConfigurationProperties(prefix = "innsyn.features")
class FeatureToggles {

    var utbetalingerEnabled: Boolean by Delegates.notNull()
}