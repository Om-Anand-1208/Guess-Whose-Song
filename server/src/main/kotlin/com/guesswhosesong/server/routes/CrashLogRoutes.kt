package com.guesswhosesong.server.routes

import com.guesswhosesong.server.auth.FirebaseTokenVerifier
import com.guesswhosesong.server.auth.requireUser
import com.guesswhosesong.server.redis.RateLimiter
import com.guesswhosesong.server.redis.RedisClient
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory

@Serializable
data class ClientCrashLogRequest(
    val device: String? = null,
    val appVersion: String? = null,
    val exceptionClass: String,
    val message: String? = null,
    val stackTrace: String
)

fun Route.crashLogRoutes(tokenVerifier: FirebaseTokenVerifier, redisClient: RedisClient) {
    val logger = LoggerFactory.getLogger("ClientCrashReport")
    val rateLimiter = RateLimiter(redisClient)

    route("/api/logs") {
        post("/crash") {
            try {
                if ((call.request.contentLength() ?: 0L) > 32_768L) {
                    call.respond(HttpStatusCode.PayloadTooLarge, mapOf("error" to "Crash report too large"))
                    return@post
                }
                val user = call.requireUser(tokenVerifier)
                if (!rateLimiter.allow("crash", user.uid, 5, 3600)) {
                    call.respond(HttpStatusCode.TooManyRequests, mapOf("error" to "RATE_LIMITED"))
                    return@post
                }
                val report = call.receive<ClientCrashLogRequest>()
                if (report.exceptionClass.length !in 1..200 ||
                    report.stackTrace.length !in 1..20_000 ||
                    report.message.orEmpty().length > 2_000 ||
                    report.device.orEmpty().length > 200 ||
                    report.appVersion.orEmpty().length > 100 ||
                    report.listFieldsContainControlCharacters()
                ) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid crash report"))
                    return@post
                }
                logger.error(
                    buildString {
                        appendLine("\n=================== CLIENT CRASH REPORT ===================")
                        appendLine("Player ID : [redacted]")
                        appendLine("Device    : ${logSafe(report.device ?: "unknown")}")
                        appendLine("App Ver   : ${logSafe(report.appVersion ?: "unknown")}")
                        appendLine("Exception : ${logSafe(report.exceptionClass)}: ${logSafe(report.message ?: "")}")
                        appendLine("--- Stack Trace ---")
                        appendLine(logSafe(report.stackTrace))
                        append("===========================================================")
                    }
                )
                call.respond(HttpStatusCode.OK, mapOf("status" to "logged"))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Failed to parse crash log")))
            }
        }
    }
}

private fun ClientCrashLogRequest.listFieldsContainControlCharacters(): Boolean =
    sequenceOf(device, appVersion, message, exceptionClass, stackTrace)
        .filterNotNull()
        .any { value -> value.any { it.isISOControl() && it != '\n' && it != '\r' && it != '\t' } }

private fun logSafe(value: String): String = value
    .replace("\\", "\\\\")
    .replace("\r", "\\r")
    .replace("\n", "\\n")
    .replace("\t", "\\t")
