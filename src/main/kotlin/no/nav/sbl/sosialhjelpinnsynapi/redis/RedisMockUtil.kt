package no.nav.sbl.sosialhjelpinnsynapi.redis

import com.github.fppt.jedismock.RedisServer
import no.nav.sbl.sosialhjelpinnsynapi.logger

object RedisMockUtil {
    private val log by logger()
    private var mockedRedisServer = RedisServer.newRedisServer(6379)

    @JvmStatic
    fun startRedisIfMocked() {
        if (isRedisMocked()) {
            log.warn("Starter MOCKET in-memory redis-server. Denne meldingen skal du aldri se i prod")
            mockedRedisServer.start()
        }
    }
    @JvmStatic
    fun startRedisMocked() {
             mockedRedisServer.start()
    }
    @JvmStatic
    fun stopRedisIfMocked() {
        if (isRedisMocked()) {
            mockedRedisServer.stop()
        }
    }
    @JvmStatic
    fun stopRedisMocked() {
         mockedRedisServer.stop()
    }
    fun isRedisMocked(): Boolean {
        return java.lang.Boolean.valueOf(System.getenv("IS_REDIS_MOCKED"))
    }

}