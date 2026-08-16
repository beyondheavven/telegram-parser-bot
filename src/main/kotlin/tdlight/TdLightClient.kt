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

    //Инициализация логгера SLF4J для этого класса
    private val log = LoggerFactory.getLogger(TdLightClient::class.java)

    //Фабрика, отвечающая за создание экземпляров клиента Telegram
    private val clientFactory = SimpleTelegramClientFactory()

    //Сам клиент. Помечен как lateinit, позже будет проинициализирован в методе start()
    private lateinit var client: SimpleTelegramClient

    @Volatile
    private var codeDeferred: CompletableDeferred<String>? = null

    @Volatile
    private var passwordDeferred: CompletableDeferred<String>? = null

    //Асинхронный флаг, который срабатывает, когда клиент успешно авторизуется
    private val ready = CompletableDeferred<Unit>()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _authStatus = MutableStateFlow(TdLightAuthStatus.NOT_STARTED)

    val authStatus: StateFlow<TdLightAuthStatus> = _authStatus.asStateFlow()

    /**
     * Запуск и конфигурация клиента
     */
    fun start(){
        //Инициализация нативных библиотек TDLib
        Init.init()

        //Перенаправление внутренних логов TDLib в собственный логгер
        Log.setLogMessageHandler(1, Slf4JLogMessageHandler())

        //Создание токена приложения
        val apiToken = APIToken(config.apiId, config.apiHash)

        //Создание базовых настроек на основе токена
        val settings = TDLibSettings.create(apiToken)

        //Настройка путей, где TDLib будет хранить свою базу данных (кэш, сессии) и скачанные файлы
        val sessionPath = Path.of(config.sessionPath)
        settings.databaseDirectoryPath = sessionPath.resolve("data")
        settings.downloadedFilesDirectoryPath = sessionPath.resolve("downloads")

        //Создание билдера клиента с переданными настройками
        val builder = clientFactory.builder(settings)

        //Добавление слушателя событий для обновлений состояний авторизации
        builder.addUpdateHandler(TdApi.UpdateAuthorizationState::class.java){
            update -> handleAuthorizationState(update.authorizationState)
        }

        //Авторизация происходит как обычный пользователь (через номер телефона)
        val authData = AuthenticationSupplier.user(config.phoneNumber)

        //Финальная сборка клиента
        client = builder.build(authData)
    }

    /**
     * Метод обрабатывает различные этапы авторизации в Telegram
     */
    private fun handleAuthorizationState(authorizationState: TdApi.AuthorizationState){
        when(authorizationState){

            //Telegram просит ввести код из смс
            is TdApi.AuthorizationStateWaitCode -> {
                log.info("Waiting for code")
                _authStatus.value = TdLightAuthStatus.WAITING_FOR_CODE
                scope.launch {
                    collectCodeUntilAccept()
                }
            }

            //У пользователя включена 2FA, требуется пароль
            is TdApi.AuthorizationStateWaitPassword -> {
                log.info("Waiting for password")
                _authStatus.value = TdLightAuthStatus.WAITING_FOR_PASSWORD
                scope.launch {
                    collectPasswordUntilAccepted()
                }
            }

            //Авторизация прошла успешно, клиент готов к работе
            is TdApi.AuthorizationStateReady -> {
                log.info("TDLight is ready")
                _authStatus.value = TdLightAuthStatus.READY
                if (!ready.isCompleted) ready.complete(Unit)
            }

            //Клиент закрыл подключение
            is TdApi.AuthorizationStateClosed -> {
                log.info("TDLight is closed")
                _authStatus.value = TdLightAuthStatus.CLOSED
            }

            //Логируем все остальные промежуточные состояния для отладки
            else -> {log.debug("Auth state: {}", authorizationState.javaClass.simpleName)}

        }
    }


    private suspend fun collectCodeUntilAccept() {
        while(true) {

        }
    }

    private suspend fun collectPasswordUntilAccepted() {
        //TODO
    }


    /**
     * suspend-функция, которая приостанавливает выполнение корутины до тех пор,
     * пока не будет получено состояние AuthorizationStateReady
     */
    suspend fun awaitReady() = ready.await()

    /**
     * Возвращает сырой экземпляр клиента для вызова методов напрямую
     */
    fun rawTelegramClient(): SimpleTelegramClient = client

    /**
     * Безопасное закрытие клиента и освобождение ресурсов фабрики.
     * Вызовется автоматически
     */
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