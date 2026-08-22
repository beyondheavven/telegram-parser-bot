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
            val dto = message.map { it.toDto() }
            ChannelHistoryResult.Success(ChannelHistoryResponse(username, dto))
        } catch (e: TelegramError){
            ChannelHistoryResult.NotFound(e.message ?: "Channel not found")
        } catch (e: Exception){
            ChannelHistoryResult.Failure(e.message ?: "Unknown error ")
        }
    }

    private fun TdApi.Message.toDto(): ChannelMessageResponse {
        val content = this.content
        val (type, text) = when(content) {
            is TdApi.MessageText -> "text" to content.text.text
            is TdApi.MessagePhoto -> "photo" to content.caption.text.ifBlank { null }
            is TdApi.MessageVideo -> "video" to content.caption.text.ifBlank { null }
            is TdApi.MessageAudio -> "audio" to content.caption.text.ifBlank { null }
            is TdApi.MessageAnimation -> "animation" to content.caption.text.ifBlank { null }
            is TdApi.MessageDocument -> "document" to content.caption.text.ifBlank { null }
            is TdApi.MessageVoiceNote -> "voice-note" to content.caption.text.ifBlank { null }
            is TdApi.MessageSticker -> "sticker" to content.sticker.emoji
            is TdApi.MessageAnimatedEmoji -> "animated_emoji" to content.emoji
            else -> "unsupported" to null
        }
        return ChannelMessageResponse(id = this.id, date = this.date, type = type, text = text)
    }
}

val Application.channelController: ChannelController
    get() = ChannelController(tdLightClient)