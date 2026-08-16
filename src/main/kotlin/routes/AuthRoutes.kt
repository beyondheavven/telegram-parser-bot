package com.telegram.routes

import com.telegram.plugins.tdLightClient
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

@OptIn
fun Route.authRoutes() {
    route("/api/auth"){

        get("/status"){
            val status = call.application.tdLightClient.authStatus.value
        }

        post("login"){
            val status = call.application.tdLightClient.authStatus.value
        }

        post("register"){
            val status = call.application.tdLightClient.authStatus.value
        }

        post("logout"){
            val status = call.application.tdLightClient.authStatus.value
        }
    }

}