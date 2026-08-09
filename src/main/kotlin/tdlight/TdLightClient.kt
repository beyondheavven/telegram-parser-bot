package com.telegram.tdlight

import it.tdlight.Init
import it.tdlight.Log
import it.tdlight.client.APIToken
import it.tdlight.client.AuthenticationSupplier
import it.tdlight.client.SimpleTelegramClient
import it.tdlight.client.SimpleTelegramClientFactory
import it.tdlight.Slf4JLogMessageHandler
import it.tdlight.client.TDLibSettings
import it.tdlight.jni.TdApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.future.await
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.util.concurrent.CompletableFuture


class TdLightClient(private val config: TdLightConfig) : AutoCloseable {

    private val log = LoggerFactory.getLogger(TdLightClient::class.java)

    private val clientFactory = SimpleTelegramClientFactory()

    private lateinit var client: SimpleTelegramClient

    private val ready = CompletableDeferred<Unit>()

    fun start(){
        Init.init()
        Log.setLogMessageHandler(1, Slf4JLogMessageHandler())
        val apiToken = APIToken(config.apiId, config.apiHash)
        val settings = TDLibSettings.create(apiToken)

        val sessionPath = Path.of(config.sessionPath)
        settings.databaseDirectoryPath = sessionPath.resolve("data")
        settings.downloadedFilesDirectoryPath = sessionPath.resolve("downloads")

        val builder = clientFactory.builder(settings)

        builder.addUpdateHandler(TdApi.UpdateAuthorizationState::class.java){
            update -> handleAuthorizationState(update.authorizationState)
        }

        val authData = AuthenticationSupplier.user(config.phoneNumber)
        client = builder.build(authData)
    }

    private fun handleAuthorizationState(authorizationState: TdApi.AuthorizationState){
        when(authorizationState){
            is TdApi.AuthorizationStateWaitCode -> {
                val code = config.authCode ?: error("Auth code is required, bit TDLIGHT_AUTH_CODE is not set")
                client.send(TdApi.CheckAuthenticationCode(code))
                    .exceptionally { log.error("Code is not verified", it); null }
            }

            is TdApi.AuthorizationStateWaitPassword -> {
                val password = config.password ?: error("Auth password is required, bit TDLIGHT_PASSWORD is not set")
                client.send(TdApi.CheckAuthenticationPassword(password))
                    .exceptionally { log.error("Password is not verified", it); null }
            }

            is TdApi.AuthorizationStateReady -> {
                log.info("TDLight is ready")
                ready.complete(Unit)
            }

            is TdApi.AuthorizationStateClosed -> {
                log.info("TDLight is closed")
            }

            else -> {log.debug("Auth state: {}", authorizationState.javaClass.simpleName)}

        }
    }


    suspend fun awaitReady() = ready.await()

    fun rawTelegramClient(): SimpleTelegramClient = client

    override fun close() {
        if (::client.isInitialized) client.close()
        clientFactory.close()
    }

    suspend fun <T> CompletableFuture<T>.awaitResult(): T = this.await()


}