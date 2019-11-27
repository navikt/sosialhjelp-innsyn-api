package no.nav.sbl.sosialhjelpinnsynapi.redis

import io.lettuce.core.RedisClient
import no.nav.sbl.sosialhjelpinnsynapi.logger
import no.nav.sbl.sosialhjelpinnsynapi.redis.RedisMockUtil.startRedisMocked
import org.springframework.boot.autoconfigure.data.redis.RedisProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(RedisProperties::class)
class RedisConfig(private val cacheProperties: CacheProperties) {

    @Bean
    fun redisClient(properties: RedisProperties): RedisClient {
        startInMemoryRedisIfMocked()

        return RedisClient.create("redis://${properties.host}:${properties.port}")
    }

    private fun startInMemoryRedisIfMocked() {
        if (cacheProperties.redisMocked) {
            log.error("in-memory redis brukes - denne feilmeldingen bør ALDRI vises i PROD")
            startRedisMocked()
        }
    }

    companion object{
        val log by logger()
    }
}