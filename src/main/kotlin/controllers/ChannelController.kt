package com.telegram.controllers

import com.telegram.models.ChannelHistoryResponse
import com.telegram.models.ChannelMessageResponse
import com.telegram.plugins.tdLightClient
import com.telegram.tdlight.TdLightClient
import io.ktor.server.application.Application
import it.tdlight.client.TelegramError
import it.tdlight.jni.TdApi

sealed class ChannelHistoryResult{
    data class Success(val data: ChannelHistoryResponse) : ChannelHistoryResult()
    data class NotFound(val error: String) : ChannelHistoryResult()
    data class Failure(val error: String) : ChannelHistoryResult()
}

class ChannelController(private val client: TdLightClient) {

    suspend fun getHistory(username: String, limit: Int, fromMessageId: Long): ChannelHistoryResult {
        return try {
            val chat = client.resolveChannel(username)
            val message = client.getChatHistory(chat.id, fromMessageId, limit)
            val dto = message.mapNotNull { it.toDto() }
            ChannelHistoryResult.Success(ChannelHistoryResponse(username, dto))
        } catch (e: TelegramError){
            ChannelHistoryResult.NotFound(e.message ?: "Channel not found")
        } catch (e: Exception){
            ChannelHistoryResult.Failure(e.message ?: "Unknown error ")
        }
    }

    private fun TdApi.Message.toDto(): ChannelMessageResponse {
        val text = when (val content = this.content) {
            is TdApi.MessageText -> content.text.text
            else -> "Text is empty"
        }
        return ChannelMessageResponse(id = this.id, date = this.date, text = text)
    }
}

val Application.channelController: ChannelController
    get() = ChannelController(tdLightClient)