package com.guesswhosesong.server.security

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class WebSocketAuthenticationTest {
    @Test
    fun `browser ticket is read from offered subprotocol and not a query value`() {
        val offered = "${WebSocketAuthentication.ROUTE_PROTOCOL}, ${WebSocketAuthentication.PROTOCOL_PREFIX}abc_DEF-123"
        val protocol = WebSocketAuthentication.offeredTicketProtocol(offered)

        assertEquals("${WebSocketAuthentication.PROTOCOL_PREFIX}abc_DEF-123", protocol)
        assertEquals("abc_DEF-123", WebSocketAuthentication.ticketFromProtocol(protocol))
    }

    @Test
    fun `malformed or absent protocols are rejected`() {
        assertNull(WebSocketAuthentication.offeredTicketProtocol(null))
        assertNull(WebSocketAuthentication.offeredTicketProtocol("chat, gws-ticket."))
        assertNull(WebSocketAuthentication.ticketFromProtocol("Authorization=secret"))
    }
}
