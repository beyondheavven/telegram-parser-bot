package com.telegram.plugins

import io.ktor.http.ContentType
import io.ktor.openapi.OpenApiInfo
import io.ktor.server.application.Application
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.routing.openapi.OpenApiDocSource
import io.ktor.server.routing.routing
import io.ktor.server.routing.routingRoot

fun Application.configureOpenApi(){
    routing {
        swaggerUI(path = "swagger-ui") {
            info = OpenApiInfo("Telegram Parser API", "1.0.0")
            source = OpenApiDocSource.Routing(ContentType.Application.Json){
                routingRoot.descendants()
            }
        }
    }

}