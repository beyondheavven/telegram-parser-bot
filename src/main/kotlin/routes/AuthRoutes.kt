package com.telegram.routes

import com.telegram.plugins.tdLightClient
import com.telegram.tdlight.TdLightAuthStatus
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable

@Serializable
data class SubmitCodeRequest(val code: String)

@Serializable
data class SubmitPasswordRequest(val password: String)

fun Route.authRoutes() {
    route("/api/auth"){

        get("/status"){
            val status = call.application.tdLightClient.authStatus.value
            call.respond(mapOf("status" to status.name))
        }

        post("/code"){
            val client = call.application.tdLightClient

            if(client.authStatus.value != TdLightAuthStatus.WAITING_FOR_CODE){
                call.respond(HttpStatusCode.Conflict, mapOf("error" to "Client is not waiting for code"))
                return@post
            }

            val request = call.receive<SubmitCodeRequest>()
            client.submitCode(request.code)
            call.respond(HttpStatusCode.Accepted, mapOf("success" to "code_submitted"))
        }

        post("/password"){
            val client = call.application.tdLightClient

            if(client.authStatus.value != TdLightAuthStatus.WAITING_FOR_PASSWORD){
                call.respond(HttpStatusCode.Conflict, mapOf("error" to "Client is not waiting for password"))
                return@post
            }

            val request = call.receive<SubmitPasswordRequest>()
            client.submitPassword(request.password)
            call.respond(HttpStatusCode.Accepted, mapOf("success" to "password_submitted"))
        }
    }
}