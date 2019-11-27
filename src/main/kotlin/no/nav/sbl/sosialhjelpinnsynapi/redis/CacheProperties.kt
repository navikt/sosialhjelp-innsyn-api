package no.nav.sbl.sosialhjelpinnsynapi.redis

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import kotlin.properties.Delegates

const val CACHE_TIME_TO_LIVE_SECONDS: Long = 2

@Component
@ConfigurationProperties(prefix = "innsyn.cache")
class CacheProperties {

    var redisMocked by Delegates.notNull<Boolean>()

}