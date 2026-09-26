package com.guesswhosesong.server.redis

/** Small fixed-window limiter backed by Redis. */
class RateLimiter(private val redis: RedisClient) {
    fun allow(scope: String, subject: String, limit: Long, windowSeconds: Long): Boolean {
        val safeSubject = subject.replace(Regex("[^A-Za-z0-9_.:-]"), "_").take(128)
        val key = "rate:$scope:$safeSubject:${System.currentTimeMillis() / (windowSeconds * 1000L)}"
        val count = redis.increment(key)
        if (count == 1L) redis.expire(key, windowSeconds)
        return count <= limit
    }
}
