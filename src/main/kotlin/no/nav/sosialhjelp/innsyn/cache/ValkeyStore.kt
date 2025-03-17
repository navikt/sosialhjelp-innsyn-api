package no.nav.sosialhjelp.innsyn.cache

import com.fasterxml.jackson.databind.exc.MismatchedInputException

import no.nav.sosialhjelp.innsyn.app.mdc.MDCUtils.get
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import redis.clients.jedis.JedisPool
import redis.clients.jedis.exceptions.JedisConnectionException
import java.lang.Exception
import no.nav.sosialhjelp.innsyn.utils.objectMapper
import org.springframework.boot.autoconfigure.data.redis.RedisProperties.Jedis


// TODO Krøll i Jeddis importer
class ValkeyStore(private val jedisPool: Jedis) {

    private val log: Logger = LoggerFactory.getLogger("no.nav.sosialhjelp.innsyn.cache")
    val mapper = objectMapper

    inline fun <reified T> getObject(key: String): T? {
        val value = get(key)
        return if (value != null) mapper.readValue(value, T::class.java)
        else null
    }

    inline fun <reified T> getListObject(key: String): List<T>? {
        val value = get(key)
        return if (value != null) {
            mapper.readValue(value, mapper.typeFactory.constructCollectionType(ArrayList::class.java, T::class.java))
        } else null
    }

    fun get(key: String): String? {
        try {
            jedisPool.resource.use { jedis -> return jedis.get(key) }
        } catch (e: JedisConnectionException) {
            log.warn("Got connection error when fetching from valkey! Continuing without cached value", e)
            return null
        } catch (e: MismatchedInputException) {
            log.error("Got deserialization error when fetching from valkey! Continuing without cached value", e)
            return null
        } catch (e: Exception) {
            log.error("Got error when fetching from valkey! Continuing without cached value", e)
            return null
        }
    }

    fun <T> setObject(key: String, value: T, expireSeconds: Long) {
        set(key, mapper.writeValueAsString(value), expireSeconds)
    }

    fun set(key: String, value: String, expireSeconds: Long) {
        try {
            jedisPool.resource.use { jedis ->
                jedis.setex(
                    key,
                    expireSeconds,
                    value,
                )
            }
        } catch (e: JedisConnectionException) {
            log.warn("Got connection error when storing in valkey! Continue without caching", e)
        }
    }
}
