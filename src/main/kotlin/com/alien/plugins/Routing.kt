package com.alien.plugins

import io.ktor.server.application.*
import io.ktor.server.routing.*
import com.alien.routes.*

fun Application.configureRouting() {
    routing {
        authRouting()
        productRouting()
    }
}
