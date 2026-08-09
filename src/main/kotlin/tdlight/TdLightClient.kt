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

    //Асинхронный флаг, который срабатывает, когда клиент успешно авторизуется
    private val ready = CompletableDeferred<Unit>()

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
                val code = config.authCode ?: error("Auth code is required, bit TDLIGHT_AUTH_CODE is not set")

                //Отправка кода в Telegram
                client.send(TdApi.CheckAuthenticationCode(code))
                    .exceptionally { log.error("Code is not verified", it); null }
            }

            //У пользователя включена 2FA, требуется пароль
            is TdApi.AuthorizationStateWaitPassword -> {
                val password = config.password ?: error("Auth password is required, bit TDLIGHT_PASSWORD is not set")

                //Отправляем пароль
                client.send(TdApi.CheckAuthenticationPassword(password))
                    .exceptionally { log.error("Password is not verified", it); null }
            }

            //Авторизация прошла успешно, клиент готов к работе
            is TdApi.AuthorizationStateReady -> {
                log.info("TDLight is ready")
                //Завершаем Deffered, разблокируем все корутины
                ready.complete(Unit)
            }

            //Клиент закрыл подключение
            is TdApi.AuthorizationStateClosed -> {
                log.info("TDLight is closed")
            }

            //Логируем все остальные промежуточные состояния для отладки
            else -> {log.debug("Auth state: {}", authorizationState.javaClass.simpleName)}

        }
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