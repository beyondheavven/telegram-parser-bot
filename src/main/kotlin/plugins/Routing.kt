package com.telegram.plugins

import com.telegram.routes.authRoutes
import com.telegram.routes.channelRoutes
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Hello, World!")
        }
        authRoutes()
        channelRoutes()
    }
}