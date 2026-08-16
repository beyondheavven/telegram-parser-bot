package com.telegram.models

import kotlinx.serialization.Serializable

@Serializable
data class AuthCodeRequest(val code: String)

@Serializable
data class AuthPasswordRequest(val password: String)

@Serializable
data class AuthStatusResponse(val status: String)

@Serializable
data class AuthActionsResponse(val message: String)