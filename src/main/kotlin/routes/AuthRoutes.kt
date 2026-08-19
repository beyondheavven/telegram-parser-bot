package com.telegram.routes

import com.telegram.controllers.AuthActionsResult
import com.telegram.controllers.authController
import com.telegram.models.SimpleMessageResponse
import com.telegram.models.SubmitCodeRequest
import com.telegram.models.SubmitPasswordRequest
import com.telegram.plugins.tdLightClient
import com.telegram.routes.docs.describeAuthStatus
import com.telegram.routes.docs.describeSubmitCode
import com.telegram.routes.docs.describeSubmitPassword
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

@OptIn(ExperimentalKtorApi::class)
fun Route.authRoutes() {
    route("/api/auth"){
        post("/start"){
            when (val result = call.application.authController.start()){
                is AuthActionsResult.Accepted -> call.respond(HttpStatusCode.Accepted, SimpleMessageResponse(result.message))
                is AuthActionsResult.Conflict -> call.respond(HttpStatusCode.Conflict, SimpleMessageResponse(result.errorMessage))
            }
        }

        post("/resend-code"){
            when (val result = call.application.authController.resendCode()){
                is AuthActionsResult.Accepted -> call.respond(HttpStatusCode.Accepted, SimpleMessageResponse(result.message))
                is AuthActionsResult.Conflict -> call.respond(HttpStatusCode.Conflict, SimpleMessageResponse(result.errorMessage))
            }
        }


        get("/status"){
            call.respond(call.application.authController.getStatus())
        }.describeAuthStatus()

        post("/code"){
            val request = call.receive<SubmitCodeRequest>()
            when (val result = call.application.authController.submitCode(request)) {
                is AuthActionsResult.Accepted -> call.respond(HttpStatusCode.OK)
                is AuthActionsResult.Conflict -> call.respond(HttpStatusCode.Conflict)
            }
        }.describeSubmitCode()

        post("/password"){
            val request = call.receive<SubmitPasswordRequest>()
            when (val result = call.application.authController.submitPassword(request)) {
                is AuthActionsResult.Accepted -> call.respond(HttpStatusCode.OK)
                is AuthActionsResult.Conflict -> call.respond(HttpStatusCode.Conflict)
            }
        }.describeSubmitPassword()
    }
}