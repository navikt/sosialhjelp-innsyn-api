package no.nav.sbl.sosialhjelpinnsynapi.redis

import io.lettuce.core.RedisClient
import org.springframework.boot.autoconfigure.data.redis.RedisProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(RedisProperties::class)
class RedisConfig {

    @Bean
    fun redisClient(properties: RedisProperties): RedisClient {
        return RedisClient.create("redis://${properties.host}:${properties.port}")
    }
}