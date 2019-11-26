package no.nav.sbl.sosialhjelpinnsynapi

import no.nav.sbl.sosialhjelpinnsynapi.redis.RedisMockUtil.startRedisIfMocked
import no.nav.sbl.sosialhjelpinnsynapi.redis.RedisMockUtil.stopRedisIfMocked
import no.nav.security.token.support.test.spring.TokenGeneratorConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Import

@SpringBootApplication
@Import(TokenGeneratorConfiguration::class)
class TestApplication

fun main(args: Array<String>) {
    startRedisIfMocked()

    runApplication<TestApplication>(*args)

    stopRedisIfMocked()
}