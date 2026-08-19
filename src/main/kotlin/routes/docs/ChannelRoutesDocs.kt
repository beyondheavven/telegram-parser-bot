package com.telegram.routes.docs

import com.telegram.models.ChannelHistoryResponse
import com.telegram.models.ErrorResponse
import io.ktor.http.HttpStatusCode
import io.ktor.openapi.jsonSchema
import io.ktor.server.routing.Route
import io.ktor.server.routing.openapi.describe
import io.ktor.utils.io.ExperimentalKtorApi

@OptIn(ExperimentalKtorApi::class)
fun Route.describeGetMessagesByUsername() = describe {
    summary = "Get messages by username"
    responses {
        HttpStatusCode.OK {
            description = "Messages are retrieved"
            schema = jsonSchema<ChannelHistoryResponse>()
        }
        HttpStatusCode.BadRequest {
            description = "Username is required"
            schema = jsonSchema<ErrorResponse>()
        }
        HttpStatusCode.NotFound {
            description = "Channel is not found"
            schema = jsonSchema<ErrorResponse>()
        }
    }
}