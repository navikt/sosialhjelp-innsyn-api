package no.nav.sosialhjelp.innsyn.redis

import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.data.redis.RedisProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import java.time.Duration

private const val TIMEOUT_SECONDS: Long = 1

@Profile("!mock-redis")
@Configuration
@EnableConfigurationProperties(RedisProperties::class)
class RedisConfig(
    @Value("\${redis_host}") private val host: String,
    @Value("\${redis_port}") private val port: Int,
    @Value("\${redis_password}") private val password: String,
    @Value("\${redis_username}") private val username: String,
) {
    @Bean
    @Profile("preprod|prodgcp")
    fun redisClientPreprod(): RedisClient {
        val redisURI =
            RedisURI
                .builder()
                .withHost(host)
                .withPort(port)
                .withAuthentication(username, password.toCharArray())
                .withTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .withSsl(true)
                .build()
        println("connecting to redis with host: $host")
        println("connecting to redis with port: $port")
        println("connecting to redis with username: $username")
        println("connecting to redis with url: $redisURI")
        return RedisClient.create(redisURI)
    }

    @Bean
    @Profile("!preprod&!prodgcp")
    fun redisClient(properties: RedisProperties): RedisClient {
        val redisUri =
            RedisURI.Builder.redis(properties.host, properties.port)
                .withPassword(properties.password as CharSequence)
                .build()

        return RedisClient.create(redisUri)
    }
}
