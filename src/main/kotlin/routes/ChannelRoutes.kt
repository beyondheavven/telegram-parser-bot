package com.telegram.routes

import com.telegram.controllers.ChannelHistoryResult
import com.telegram.controllers.channelController
import com.telegram.models.ErrorResponse
import com.telegram.routes.docs.describeGetMessagesByUsername
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

fun Route.channelRoutes() {
    route("/api/channels"){
        get("/{username}/messages"){
            val username = call.parameters["username"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("Username is required"))

            val limit = (call.request.queryParameters["limit"]?.toIntOrNull() ?: 20).coerceIn(1,100)
            val fromMessageId =  call.request.queryParameters["fromMessageId"]?.toLongOrNull() ?: 0

            when(val result = call.application.channelController.getHistory(username, limit, fromMessageId)){
                is ChannelHistoryResult.Success -> call.respond(HttpStatusCode.OK, result.data)
                is ChannelHistoryResult.Failure -> call.respond(HttpStatusCode.BadRequest, ErrorResponse(result.error))
                is ChannelHistoryResult.NotFound -> call.respond(HttpStatusCode.NotFound, ErrorResponse(result.error))
            }
        }.describeGetMessagesByUsername()

    }
}