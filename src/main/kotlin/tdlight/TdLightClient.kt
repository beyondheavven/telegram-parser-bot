package com.telegram.tdlight

import com.telegram.models.AuthStatusResponse
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.util.concurrent.CompletableFuture

enum class TdLightAuthStatus {
    NOT_STARTED,
    WAITING_FOR_CODE,
    WAITING_FOR_PASSWORD,
    READY,
    CLOSED
}

/**
 * Класс-клиент для работы с Telegram через TDLight.
 * Реализует интерфейс AutoCloseable, чтобы можно было использовать конструкцию use { }
 * и гарантированно освобождать ресурсы при завершении работы.
 */
class TdLightClient(private val config: TdLightConfig) : AutoCloseable {

    private val log = LoggerFactory.getLogger(TdLightClient::class.java)

    private val clientFactory = SimpleTelegramClientFactory()

    private lateinit var client: SimpleTelegramClient

    @Volatile
    private var codeDeferred: CompletableDeferred<String>? = null

    @Volatile
    private var passwordDeferred: CompletableDeferred<String>? = null

    private val ready = CompletableDeferred<Unit>()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _authStatus = MutableStateFlow(TdLightAuthStatus.NOT_STARTED)

    val authStatus: StateFlow<TdLightAuthStatus> = _authStatus.asStateFlow()


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
                log.info("Waiting for code")
                _authStatus.value = TdLightAuthStatus.WAITING_FOR_CODE
                scope.launch {
                    collectCodeUntilAccept()
                }
            }

            is TdApi.AuthorizationStateWaitPassword -> {
                log.info("Waiting for password")
                _authStatus.value = TdLightAuthStatus.WAITING_FOR_PASSWORD
                scope.launch {
                    collectPasswordUntilAccepted()
                }
            }

            is TdApi.AuthorizationStateReady -> {
                log.info("TDLight is ready")
                _authStatus.value = TdLightAuthStatus.READY
                if (!ready.isCompleted) ready.complete(Unit)
            }

            is TdApi.AuthorizationStateClosed -> {
                log.info("TDLight is closed")
                _authStatus.value = TdLightAuthStatus.CLOSED
            }

            else -> {log.debug("Auth state: {}", authorizationState.javaClass.simpleName)}

        }
    }


    private suspend fun collectCodeUntilAccept() {
        while(true) {
            val deferred = CompletableDeferred<String>()
            codeDeferred = deferred

            val code = deferred.await()

            client.send(TdApi.CheckAuthenticationCode(code)).awaitResult()
            codeDeferred = null
        }
    }

    private suspend fun collectPasswordUntilAccepted() {
        val deferred = CompletableDeferred<String>()
        passwordDeferred = deferred

        val password = deferred.await()

        client.send(TdApi.CheckAuthenticationPassword(password)).awaitResult()
        passwordDeferred = null
    }

    fun submitCode(code: String) {
        val deferred = codeDeferred ?: error("Client waiting for code: $code")
        deferred.complete(code)
    }

    fun submitPassword(password: String) {
        val deferred = passwordDeferred ?: error("Client waiting for password: $password")
        deferred.complete(password)
    }


    suspend fun awaitReady() = ready.await()


    fun rawTelegramClient(): SimpleTelegramClient = client

    override fun close() {
        if (::client.isInitialized) client.close()
        clientFactory.close()
    }

    /**
     * Вспомогательная функция-расширение.
     * Преобразует Java CompletableFuture в Kotlin suspend-функцию.
     */
    suspend fun <T> CompletableFuture<T>.awaitResult(): T = this.await()


}