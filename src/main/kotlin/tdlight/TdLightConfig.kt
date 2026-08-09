package com.telegram.tdlight

/**
 * Конфигурация для инициализации клиента TDLight (Telegram TDLib).
 * Хранит необходимые учетные данные и пути для работы сессии.
 *
 * @property apiId Идентификатор приложения.
 * @property apiHash Секретный хэш приложения.
 * @property phoneNumber Номер телефона пользователя для авторизации в формате +1234567890.
 * @property authCode Код подтверждения из СМС или другого клиента Telegram (может быть null до ввода).
 * @property password Пароль двухэтапной аутентификации, если он установлен (иначе null).
 * @property sessionPath Путь к директории, где TDLib будет хранить файлы сессии и кэш базы данных.
 */

data class TdLightConfig(
    val apiId: Int,
    val apiHash: String,
    val phoneNumber: String,
    val authCode: String?,
    val password: String?,
    val sessionPath: String = "/app/tdlight-session"
){
    /**
     * Фабричный метод для создания конфигурации из переменных окружения.
     *
     * @throws IllegalStateException если критически важные переменные (API_ID, API_HASH, PHONE_NUMBER) не заданы.
     */
    companion object {
        fun fromEnv(): TdLightConfig = TdLightConfig(
            apiId = System.getenv("TDLIGHT_API_ID")?.toIntOrNull() ?: error("TDLIGHT_API_ID is not set"),
            apiHash = System.getenv("TDLIGHT_API_HASH") ?: error("TDLIGHT_API_HASH is not set"),
            phoneNumber = System.getenv("TDLIGHT_PHONE_NUMBER") ?: error("TDLIGHT_PHONE_NUMBER is not set"),
            authCode = System.getenv("TDLIGHT_AUTH_CODE"),
            password = System.getenv("TDLIGHT_PASSWORD"),
            sessionPath = System.getenv("TDLIGHT_SESSION_PATH")
        )
    }
}