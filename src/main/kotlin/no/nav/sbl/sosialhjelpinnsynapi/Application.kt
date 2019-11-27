package no.nav.sbl.sosialhjelpinnsynapi

import no.nav.sbl.sosialhjelpinnsynapi.redis.RedisMockUtil.startRedisIfMocked
import no.nav.sbl.sosialhjelpinnsynapi.redis.RedisMockUtil.stopRedisIfMocked
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class Application

fun main(args: Array<String>) {
    startRedisIfMocked()

    runApplication<Application>(*args)

    stopRedisIfMocked()
}