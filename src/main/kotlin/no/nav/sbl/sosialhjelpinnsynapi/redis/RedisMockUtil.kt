package no.nav.sbl.sosialhjelpinnsynapi.redis

import com.github.fppt.jedismock.RedisServer
import no.nav.sbl.sosialhjelpinnsynapi.logger

object RedisMockUtil {
    private val log by logger()
    private var mockedRedisServer = RedisServer.newRedisServer(6379)

    @JvmStatic
    fun startRedisMocked() {
        log.warn("Starter MOCKET in-memory redis-server. Denne meldingen skal du aldri se i prod")
        mockedRedisServer.start()
    }

    @JvmStatic
    fun stopRedisMocked() {
        mockedRedisServer.stop()
    }

}