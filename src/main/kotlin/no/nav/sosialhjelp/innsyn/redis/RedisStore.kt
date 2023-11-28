package no.nav.sosialhjelp.innsyn.redis

import io.lettuce.core.RedisClient
import io.lettuce.core.RedisException
import io.lettuce.core.RedisFuture
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.async.RedisAsyncCommands
import io.lettuce.core.codec.ByteArrayCodec
import io.lettuce.core.codec.RedisCodec
import io.lettuce.core.codec.StringCodec
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Profile("!mock-redis")
@Component
class RedisStore(
    redisClient: RedisClient,
) {
    private val connection: StatefulRedisConnection<String, ByteArray> =
        redisClient.connect(
            RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE),
        )
    private val async: RedisAsyncCommands<String, ByteArray> =
        connection.async() ?: throw RedisException("Klarte ikke å få tak i connection.async()")

    fun get(key: String): ByteArray? {
        val get: RedisFuture<ByteArray> = async.get(key)
        val await = get.await(500, TimeUnit.MILLISECONDS)
        return if (await) {
            get.get()
        } else {
            null
        }
    }

    fun set(
        key: String,
        value: ByteArray,
        timeToLive: Long,
    ): String? {
        val set: RedisFuture<String> = async.setex(key, timeToLive, value)
        return if (set.await(500, TimeUnit.MILLISECONDS)) {
            set.get()
        } else {
            null
        }
    }

    fun delete(key: String): Boolean {
        val del = async.del(key)
        return if (del.await(500, TimeUnit.MILLISECONDS)) {
            del.get() == 1L
        } else {
            false
        }
    }
}
