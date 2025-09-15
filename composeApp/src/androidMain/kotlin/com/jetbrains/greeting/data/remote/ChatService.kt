package com.jetbrains.greeting.data.service

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

class ChatService(private val client: HttpClient,  private val baseUrl: String) {
 

    suspend fun sendMessage(message: String): String {
        val response = client.get("$baseUrl/api/chatbot/mandarMensaje") {
            parameter("message", message)
        }
        
        return if (response.status == HttpStatusCode.OK) {
            response.bodyAsText()
        } else {
            "Error al enviar el mensaje"
        }
    }
} 