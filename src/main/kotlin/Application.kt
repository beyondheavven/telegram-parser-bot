package com.telegram

import com.telegram.plugins.configureHttp
import com.telegram.plugins.configureRouting
import com.telegram.plugins.configureTdLight
import io.ktor.server.application.Application

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureTdLight()
    configureRouting()
    configureHttp()
}
