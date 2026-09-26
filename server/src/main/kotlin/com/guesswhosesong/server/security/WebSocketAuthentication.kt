package com.guesswhosesong.server.security

/** Browser WebSocket subprotocol helpers. Credentials are never accepted in a URL. */
object WebSocketAuthentication {
    /**
     * Ktor needs a protocol value known before the WebSocket upgrade so it can
     * echo a valid Sec-WebSocket-Protocol response. The ticket remains in the
     * second, dynamic protocol value and is still never put in the URL.
     */
    const val ROUTE_PROTOCOL = "gws-ticket"
    const val PROTOCOL_PREFIX = "gws-ticket."

    fun offeredTicketProtocol(header: String?): String? = header
        ?.split(',')
        ?.asSequence()
        ?.map(String::trim)
        ?.firstOrNull { it.startsWith(PROTOCOL_PREFIX) && it.length > PROTOCOL_PREFIX.length }

    fun ticketFromProtocol(protocol: String?): String? = protocol
        ?.takeIf { it.startsWith(PROTOCOL_PREFIX) }
        ?.removePrefix(PROTOCOL_PREFIX)
        ?.takeIf { it.isNotBlank() }
}
