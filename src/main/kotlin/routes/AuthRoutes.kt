package com.telegram.routes

import com.telegram.plugins.tdLightClient
import com.telegram.tdlight.TdLightAuthStatus
import io.ktor.http.HttpStatusCode
import io.ktor.openapi.ReferenceOr.Companion.schema
import io.ktor.openapi.jsonSchema
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.openapi.describe
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.utils.io.ExperimentalKtorApi
import kotlinx.serialization.Serializable

@Serializable
data class SubmitCodeRequest(val code: String)

@Serializable
data class SubmitPasswordRequest(val password: String)

@Serializable
data class AuthStatusResponse(val status: String)

@Serializable
data class SimpleMessageResponse(val message: String)

@OptIn(ExperimentalKtorApi::class)
fun Route.authRoutes() {
    route("/api/auth"){

        get("/status"){
            val status = call.application.tdLightClient.authStatus.value
            call.respond(AuthStatusResponse(status.name))
        }.describe {
            summary = "Current auth status in Telegram"
            responses {
                HttpStatusCode.OK {
                    description = "Status OK"
                    schema = jsonSchema<AuthStatusResponse>()
                }
            }
        }

        post("/code"){
            val client = call.application.tdLightClient

            if(client.authStatus.value != TdLightAuthStatus.WAITING_FOR_CODE){
                call.respond(HttpStatusCode.Conflict, SimpleMessageResponse("Wrong code"))
                return@post
            }

            val request = call.receive<SubmitCodeRequest>()
            client.submitCode(request.code)
            call.respond(HttpStatusCode.Accepted, SimpleMessageResponse("code_submitted"))
        }.describe {
            summary = "Waiting for code from Telegram"
            requestBody{
                schema = jsonSchema<SubmitCodeRequest>()
            }
            responses {
                HttpStatusCode.Accepted {
                    description = "Code submitted"
                    schema = jsonSchema<SimpleMessageResponse>()
                }
                HttpStatusCode.Conflict {
                    description = "Code not found"
                    schema = jsonSchema<SimpleMessageResponse>()
                }
            }
        }

        post("/password"){
            val client = call.application.tdLightClient
            if(client.authStatus.value != TdLightAuthStatus.WAITING_FOR_PASSWORD){
                call.respond(HttpStatusCode.Conflict, SimpleMessageResponse("Client is not waiting for password"))
                return@post
            }

            val request = call.receive<SubmitPasswordRequest>()
            client.submitPassword(request.password)
            call.respond(HttpStatusCode.Accepted, SimpleMessageResponse("Password submitted"))
        }.describe {
            summary = "Waiting for password from Telegram"
            requestBody{
                schema = jsonSchema<AuthStatusResponse>()
            }
            responses {
                HttpStatusCode.Accepted {
                    description = "Password submitted"
                    schema = jsonSchema<SimpleMessageResponse>()
                }
                HttpStatusCode.Conflict {
                    description = "Password not found"
                    schema = jsonSchema<SimpleMessageResponse>()
                }
            }
        }
    }
}