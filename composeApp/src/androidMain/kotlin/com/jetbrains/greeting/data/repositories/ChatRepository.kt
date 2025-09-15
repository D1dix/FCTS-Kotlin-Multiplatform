package com.jetbrains.greeting.data.repository

import com.jetbrains.greeting.data.service.ChatService

class ChatRepository(private val chatService: ChatService) {
    suspend fun sendMessage(message: String): String {
        return chatService.sendMessage(message)
    }
} 