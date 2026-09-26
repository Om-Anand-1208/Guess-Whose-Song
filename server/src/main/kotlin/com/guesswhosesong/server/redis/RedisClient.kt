package com.guesswhosesong.server.redis

import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig
import java.net.URI
import java.net.URLDecoder

/**
 * Thin wrapper around a JedisPool.
 * Exposes suspend-friendly execute blocks for safe resource management.
 */
class RedisClient private constructor(private val pool: JedisPool) {

    fun <T> execute(block: (redis.clients.jedis.Jedis) -> T): T {
        return pool.resource.use { jedis -> block(jedis) }
    }

    fun set(key: String, value: String) = execute { it.set(key, value) }
    fun set(key: String, value: String, ttlSeconds: Long) = execute {
        it.setex(key, ttlSeconds, value)
    }
    fun setIfAbsent(key: String, value: String, ttlSeconds: Long): Boolean = execute {
        it.set(key, value, redis.clients.jedis.params.SetParams.setParams().nx().ex(ttlSeconds)) == "OK"
    }

    fun get(key: String): String? = execute { it.get(key) }
    /** Atomically reads and deletes a value. Used for one-time authentication tickets. */
    fun getAndDelete(key: String): String? = execute {
        @Suppress("UNCHECKED_CAST")
        it.eval(
            "local value = redis.call('get', KEYS[1]); if value then redis.call('del', KEYS[1]); end; return value",
            listOf(key),
            emptyList<String>()
        ) as? String
    }
    fun increment(key: String): Long = execute { it.incr(key) }
    fun del(key: String) = execute { it.del(key) }
    fun exists(key: String): Boolean = execute { it.exists(key) }
    fun expire(key: String, ttlSeconds: Long) = execute { it.expire(key, ttlSeconds) }

    /** Append to a Redis list (left push) */
    fun lpush(key: String, value: String) = execute { it.lpush(key, value) }

    /** Get a range from a list */
    fun lrange(key: String, start: Long, end: Long): List<String> =
        execute { it.lrange(key, start, end) }

    /** Trim a list to a max length */
    fun ltrim(key: String, start: Long, end: Long) = execute { it.ltrim(key, start, end) }

    fun close() = pool.close()

    companion object {
        fun fromEnv(): RedisClient {
            val redisUrl = System.getenv("REDIS_URL") ?: "redis://localhost:6379"
            val uri = URI(redisUrl)
            val port = if (uri.port > 0) uri.port else 6379
            val poolConfig = JedisPoolConfig().apply {
                maxTotal = 16
                maxIdle = 8
                minIdle = 2
                testOnBorrow = true
            }
            val pool = if (uri.rawUserInfo != null) {
                val password = uri.rawUserInfo.substringAfter(':', uri.rawUserInfo)
                    .let { URLDecoder.decode(it, Charsets.UTF_8.name()) }
                JedisPool(
                    poolConfig,
                    uri.host,
                    port,
                    2000,
                    password,
                    uri.scheme.equals("rediss", ignoreCase = true)
                )
            } else {
                JedisPool(
                    poolConfig,
                    uri.host,
                    port,
                    2000,
                    null,
                    uri.scheme.equals("rediss", ignoreCase = true)
                )
            }
            return RedisClient(pool)
        }
    }
}
