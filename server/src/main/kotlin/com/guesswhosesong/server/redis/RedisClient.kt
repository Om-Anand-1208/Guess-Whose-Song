package com.guesswhosesong.server.redis

import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig
import java.net.URI

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

    fun get(key: String): String? = execute { it.get(key) }
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
            val poolConfig = JedisPoolConfig().apply {
                maxTotal = 16
                maxIdle = 8
                minIdle = 2
                testOnBorrow = true
            }
            val pool = if (uri.userInfo != null) {
                val password = uri.userInfo.split(":").getOrNull(1)
                JedisPool(poolConfig, uri.host, uri.port, 2000, password)
            } else {
                JedisPool(poolConfig, uri.host, uri.port)
            }
            return RedisClient(pool)
        }
    }
}

