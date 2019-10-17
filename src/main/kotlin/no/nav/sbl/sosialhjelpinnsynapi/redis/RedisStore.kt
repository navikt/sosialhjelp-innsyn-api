package no.nav.sbl.sosialhjelpinnsynapi.redis

import io.lettuce.core.RedisClient
import io.lettuce.core.RedisFuture
import io.lettuce.core.codec.RedisCodec
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.io.*
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit


@Component
class RedisStore @Autowired constructor(redisClient: RedisClient) {

    private final val connection = redisClient.connect(SerializedObjectCodec())
    private val async = connection.async()!!

    fun get(key: String): Any? {
        val get: RedisFuture<Any> = async.get(key)
        val await = get.await(1, TimeUnit.SECONDS)
        return if (await) {
            get.get()
        } else null
    }

    fun set(key: String, value: Any, timeToLive: Long): String? {
        val set: RedisFuture<String> = async.set(key, value)
        return if (set.await(1, TimeUnit.SECONDS)) {
            async.pexpire(key, timeToLive)
            set.get()
        } else null
    }

}

class SerializedObjectCodec : RedisCodec<String, Any> {
    private val charset = StandardCharsets.UTF_8

    override fun decodeKey(bytes: ByteBuffer): String {
        return charset.decode(bytes).toString()
    }

    override fun decodeValue(bytes: ByteBuffer): Any? {
        return try {
            val array = ByteArray(bytes.remaining())
            bytes.get(array)
            val ois = ObjectInputStream(ByteArrayInputStream(array))
            ois.readObject()
        } catch (e: Exception) {
            null
        }
    }

    override fun encodeKey(key: String): ByteBuffer {
        return ByteBuffer.wrap(charset.encode(key).array())
    }

    override fun encodeValue(value: Any): ByteBuffer? {
        return try {
            val bytes = ByteArrayOutputStream()
            val os = ObjectOutputStream(bytes)
            os.writeObject(value)
            ByteBuffer.wrap(bytes.toByteArray())
        } catch (e: IOException) {
            null
        }
    }
}