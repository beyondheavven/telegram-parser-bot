package com.telegram.tdlight


data class TdLightConfig(
    val apiId: Int,
    val apiHash: String,
    val phoneNumber: String,
    val authCode: String?,
    val password: String?,
    val sessionPath: String = "/app/tdlight-session"
){
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