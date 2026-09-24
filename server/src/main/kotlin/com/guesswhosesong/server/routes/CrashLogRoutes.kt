package com.guesswhosesong.server.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory

@Serializable
data class ClientCrashLogRequest(
    val playerId: String? = null,
    val device: String? = null,
    val appVersion: String? = null,
    val exceptionClass: String,
    val message: String? = null,
    val stackTrace: String
)

fun Route.crashLogRoutes() {
    val logger = LoggerFactory.getLogger("ClientCrashReport")

    route("/api/logs") {
        post("/crash") {
            try {
                val report = call.receive<ClientCrashLogRequest>()
                logger.error(
                    buildString {
                        appendLine("\n=================== CLIENT CRASH REPORT ===================")
                        appendLine("Player ID : ${report.playerId ?: "unknown"}")
                        appendLine("Device    : ${report.device ?: "unknown"}")
                        appendLine("App Ver   : ${report.appVersion ?: "unknown"}")
                        appendLine("Exception : ${report.exceptionClass}: ${report.message ?: ""}")
                        appendLine("--- Stack Trace ---")
                        appendLine(report.stackTrace)
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
