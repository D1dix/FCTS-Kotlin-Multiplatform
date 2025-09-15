package com.jetbrains.greeting.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jetbrains.greeting.data.entitys.ChatMessage
import com.jetbrains.greeting.data.repository.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatViewModel(private val repository: ChatRepository) : ViewModel() {
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun sendMessage(message: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _messages.value = _messages.value + ChatMessage(message, true)
            
            try {
                val response = repository.sendMessage(message)
                _messages.value = _messages.value + ChatMessage(response, false)
            } catch (e: Exception) {
                _messages.value = _messages.value + ChatMessage("Error: ${e.message}", false)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearChat() {
        _messages.value = emptyList()
    }
} 