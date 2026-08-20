package com.telegram.plugins

import com.telegram.tdlight.TdLightClient
import com.telegram.tdlight.TdLightConfig
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.util.AttributeKey

val TdLightClientKey = AttributeKey<TdLightClient>("TdLightCLient")

fun Application.configureTdLight() {
    val tdLightClient = TdLightClient(TdLightConfig.fromEnv())
    attributes.put(TdLightClientKey, tdLightClient)
    monitor.subscribe(ApplicationStopped){
        tdLightClient.close()
    }
}

val Application.tdLightClient: TdLightClient get() = attributes[TdLightClientKey]