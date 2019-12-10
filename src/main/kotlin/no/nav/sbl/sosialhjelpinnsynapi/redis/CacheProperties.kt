package no.nav.sbl.sosialhjelpinnsynapi.redis

import no.nav.sbl.sosialhjelpinnsynapi.redis.RedisMockUtil.startRedisMocked
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import kotlin.properties.Delegates

@Component
@ConfigurationProperties(prefix = "innsyn.cache")
class CacheProperties {

    var redisMocked: Boolean by Delegates.notNull()

    var timeToLive: Long by Delegates.notNull()

    fun startInMemoryRedisIfMocked() {
        if (redisMocked) {
            startRedisMocked()
        }
    }
}