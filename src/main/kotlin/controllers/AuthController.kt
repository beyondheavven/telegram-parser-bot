package com.telegram.controllers

import com.telegram.models.AuthStatusResponse
import com.telegram.models.SubmitCodeRequest
import com.telegram.models.SubmitPasswordRequest
import com.telegram.plugins.tdLightClient
import com.telegram.tdlight.TdLightAuthStatus
import com.telegram.tdlight.TdLightClient
import io.ktor.server.application.Application

sealed class AuthActionsResult {
    data class Accepted(val message: String) : AuthActionsResult()
    data class Conflict(val errorMessage: String) : AuthActionsResult()
}

class AuthController(private val client: TdLightClient) {

    fun start(): AuthActionsResult {
        if (client.authStatus.value != TdLightAuthStatus.NOT_STARTED){
            return AuthActionsResult.Conflict("Auth process is already running (status: ${client.authStatus.value})")
        }
        client.start()
        return AuthActionsResult.Accepted("auth_started")
    }

    suspend fun resendCode(): AuthActionsResult {
        if (client.authStatus.value != TdLightAuthStatus.WAITING_FOR_CODE){
            return AuthActionsResult.Conflict("Not currently waiting for code")
        }
        client.resendCode()
        return AuthActionsResult.Accepted("code_resent")
    }

    fun getStatus(): AuthStatusResponse = AuthStatusResponse(client.authStatus.value.name)

    fun submitCode(request: SubmitCodeRequest): AuthActionsResult {
        if (client.authStatus.value != TdLightAuthStatus.WAITING_FOR_PASSWORD){
            return AuthActionsResult.Conflict("Client is not waiting for code")
        }

        client.submitCode(request.code)
        return AuthActionsResult.Accepted("Code submitted to ${request.code}")
    }

    fun submitPassword(request: SubmitPasswordRequest): AuthActionsResult {
        if (client.authStatus.value != TdLightAuthStatus.WAITING_FOR_PASSWORD){
            return AuthActionsResult.Conflict("Client is not waiting for password")
        }
        client.submitPassword(request.password)
        return AuthActionsResult.Accepted("Password submitted to ${request.password}")
    }
}

val Application.authController: AuthController
    get() = AuthController(tdLightClient)