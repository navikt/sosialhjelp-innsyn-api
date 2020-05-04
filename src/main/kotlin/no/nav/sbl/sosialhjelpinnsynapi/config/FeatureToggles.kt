package no.nav.sbl.sosialhjelpinnsynapi.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import kotlin.properties.Delegates

@Component
@ConfigurationProperties(prefix = "innsyn.features")
class FeatureToggles {

    var vilkarEnabled: Boolean by Delegates.notNull()
    var dokumentasjonkravEnabled: Boolean by Delegates.notNull()
}