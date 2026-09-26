package com.guesswhosesong.server.auth

import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.authorization

fun ApplicationCall.requireUser(verifier: FirebaseTokenVerifier): AuthenticatedUser =
    verifier.verifyBearerHeader(request.authorization())
