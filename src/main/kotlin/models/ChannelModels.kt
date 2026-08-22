package com.telegram.models

import kotlinx.serialization.Serializable


@Serializable
data class ChannelMessageResponse (
    val id: Long,
    val date: Int,
    val type: String,
    val text: String? = null
)

@Serializable
data class ChannelHistoryResponse (
    val channelUsername: String,
    val message: List<ChannelMessageResponse>
)

@Serializable
data class ErrorResponse (val error: String)