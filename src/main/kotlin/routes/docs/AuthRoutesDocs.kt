package com.telegram.routes.docs

import com.telegram.models.AuthStatusResponse
import com.telegram.models.SimpleMessageResponse
import com.telegram.models.SubmitCodeRequest
import com.telegram.models.SubmitPasswordRequest
import io.ktor.http.HttpStatusCode
import io.ktor.openapi.jsonSchema
import io.ktor.server.routing.Route
import io.ktor.server.routing.openapi.describe
import io.ktor.utils.io.ExperimentalKtorApi

@OptIn(ExperimentalKtorApi::class)
fun Route.describeAuthStatus() = describe {
    summary = "Current Auth status in Telegram"
    responses {
        HttpStatusCode.OK {
            description = "Http status OK"
            schema = jsonSchema<AuthStatusResponse>()

        }
    }
}

@OptIn(ExperimentalKtorApi::class)
fun Route.describeStartApp() = describe {
    summary = "Start an Application"
    responses {
        HttpStatusCode.Accepted {
            description = "Auth process started successfully"
            schema = jsonSchema<SimpleMessageResponse>()
        }
        HttpStatusCode.Conflict {
            description = "Auth process is running"
            schema = jsonSchema<SimpleMessageResponse>()
        }
    }
}

@OptIn(ExperimentalKtorApi::class)
fun Route.describeResendCode() = describe {
    summary = "Resend code of a Telegram Code"
    responses {
        HttpStatusCode.Accepted {
            description = "Code resent successfully"
            schema = jsonSchema<SimpleMessageResponse>()
        }
        HttpStatusCode.Conflict {
            description = "Client is not waiting for code"
            schema = jsonSchema<SimpleMessageResponse>()
        }
    }
}

@OptIn(ExperimentalKtorApi::class)
fun Route.describeSubmitCode() = describe {
    summary = "Submit code for Telegram"
    requestBody {
        schema = jsonSchema<SubmitCodeRequest>()
    }
    responses {
        HttpStatusCode.Accepted {
            description = "Http status OK"
            schema = jsonSchema<SimpleMessageResponse>()
        }
        HttpStatusCode.Conflict {
            description = "Http status Conflict"
            schema = jsonSchema<SimpleMessageResponse>()
        }
    }
}

@OptIn(ExperimentalKtorApi::class)
fun Route.describeSubmitPassword() = describe {
    summary = "Submit password for Telegram"
    requestBody {
        schema = jsonSchema<SubmitPasswordRequest>()
    }
    responses {
        HttpStatusCode.Accepted {
            description = "Http status OK"
            schema = jsonSchema<SimpleMessageResponse>()
        }
        HttpStatusCode.Conflict {
            description = "Http status Conflict"
            schema = jsonSchema<SimpleMessageResponse>()
        }
    }
}