package no.nav.sosialhjelp.innsyn.redis

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import kotlin.properties.Delegates

@Component
@ConfigurationProperties(prefix = "innsyn.cache")
class CacheProperties {
    var timeToLiveSeconds: Long by Delegates.notNull()
}
