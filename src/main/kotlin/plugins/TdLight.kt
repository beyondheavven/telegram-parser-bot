package com.telegram.plugins

import com.telegram.tdlight.TdLightClient
import com.telegram.tdlight.TdLightConfig
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.application.ApplicationStopped
import io.ktor.util.AttributeKey
import kotlinx.coroutines.launch

/**
 * Уникальный типизированный ключ.
 * Используется для сохранения и безопасного извлечения экземпляра [TdLightClient]
 * из глобального словаря атрибутов (Attributes) сервера Ktor.
 */
val TdLightClientKey = AttributeKey<TdLightClient>("TdLightCLient")

/**
 * Модуль конфигурации для интеграции TDLight в Ktor.
 * Управляет жизненным циклом Telegram-клиента синхронно с веб-сервером.
 */
fun Application.configureTdLight() {
    //Инициализация: Создаем клиента, считывая настройки из переменных окружения
    val tdLightClient = TdLightClient(TdLightConfig.fromEnv())

    //Внедрение зависимостей (DI): Прячем готового клиента в глобальную память Ktor
    attributes.put(TdLightClientKey, tdLightClient)

    //Обработка запуска сервера
    monitor.subscribe(ApplicationStarted){
        // Запускаем сборку и настройку TDLib
        tdLightClient.start()

        // Запускаем фоновую корутину (не блокируя старт самого веб-сервера),
        // которая дождется успешной авторизации в Telegram
        launch {
            tdLightClient.awaitReady()
        }
    }

    //Обработка остановки сервера (Graceful shutdown)
    monitor.subscribe(ApplicationStopped){
        // Гарантированно закрываем базу данных TDLib и сетевые соединения
        // при выключении приложения, чтобы избежать утечек памяти
        tdLightClient.close()
    }
}

/**
 * Синтаксический сахар для быстрого доступа к Telegram-клиенту.
 * Позволяет обращаться к клиенту напрямую (например, `call.application.tdLightClient` в роутинге)
 */
val Application.tdLightClient: TdLightClient get() = attributes[TdLightClientKey]