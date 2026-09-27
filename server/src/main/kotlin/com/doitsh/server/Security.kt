package com.doitsh.server

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureSecurity() {
    val jwtSecret = environment.config.propertyOrNull("jwt.secret")?.getString()
        ?: "doitsh-dev-secret-change-in-production"
    val jwtRealm = environment.config.propertyOrNull("jwt.realm")?.getString()
        ?: "doitsh-server"

    install(Authentication) {
        jwt("auth-jwt") {
            realm = jwtRealm
            verifier(
                JWT
                    .require(Algorithm.HMAC256(jwtSecret))
                    .withAudience("doitsh-client")
                    .withIssuer(jwtRealm)
                    .build()
            )
            validate { credential ->
                if (credential.payload.audience.contains("doitsh-client")) {
                    JWTPrincipal(credential.payload)
                } else null
            }
        }
    }
}
