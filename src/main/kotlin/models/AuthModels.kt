package com.telegram.models

import kotlinx.serialization.Serializable


@Serializable
data class SubmitCodeRequest(val code: String)

@Serializable
data class SubmitPasswordRequest(val password: String)

@Serializable
data class AuthStatusResponse(val status: String)

@Serializable
data class SimpleMessageResponse(val message: String)
