package com.jetbrains.greeting.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jetbrains.greeting.data.entitys.CommentEntity
import com.jetbrains.greeting.data.entitys.UserEntity
import com.jetbrains.greeting.data.repositories.CommentRepository
import com.jetbrains.greeting.data.repositories.MenuRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CommentViewModel(
    private val commentRepository: CommentRepository,
    private val menuRepository: MenuRepository
) : ViewModel() {
    private val _comments = MutableStateFlow<List<CommentEntity>>(emptyList())
    val comments: StateFlow<List<CommentEntity>> = _comments.asStateFlow()

    private val _newCommentText = MutableStateFlow("")
    val newCommentText: StateFlow<String> = _newCommentText.asStateFlow()

    private val _puedeComentar = MutableStateFlow(false)
    val puedeComentar: StateFlow<Boolean> = _puedeComentar.asStateFlow()

    private var currentItemId: Long = 0

    init {
        viewModelScope.launch {
            while (true) {
                delay(43200000) // 12 horas en milisegundos
                loadComments(currentItemId)
            }
        }
    }

    fun clearComments(itemId: Long) {
        viewModelScope.launch {
            try {
                commentRepository.clearComments(itemId)
                _comments.value = emptyList()
            } catch (e: Exception) {
                println("❌ [ViewModel] Error al limpiar comentarios: ${e.message}")
            }
        }
    }

    fun loadComments(itemId: Long) {
        currentItemId = itemId
        viewModelScope.launch {
            commentRepository.getCommentsForItem(itemId).collect { comments ->
                println("👀 [ViewModel] Comentarios cargados: $comments")
                _comments.value = comments
            }
        }
    }

    fun updateCommentText(text: String) {
        _newCommentText.value = text
    }

    fun getAverageRatingFlow(itemId: Long): Flow<Float?> {
        return commentRepository.getAverageRating(itemId)
    }

    suspend fun addComment(
        itemId: Long,
        text: String,
        rating: Int,
        user: UserEntity,
        date: String,
        destacado: Boolean
    ) {
        println("🟢 [ViewModel] Enviando comentario: $text, rating: $rating, user: ${user.email}, date: $date, destacado: $destacado")
        println("🔑 [ViewModel] Token del usuario: ${user.token}")
        if (user.token.isNullOrEmpty()) {
            throw Exception("Token no disponible")
        }
        
        try {
            println("👤 [ViewModel] Rol del usuario: ${user.rol}")
            println("🔑 [ViewModel] Token completo: ${user.token}")
            commentRepository.addComment(
                itemId = itemId,
                text = text,
                rating = rating,
                email = user.email,
                token = user.token,
                date = date,
                destacado = destacado,
                getUserByEmail = { user }
            )
            println("✅ [ViewModel] Comentario enviado y cargando comentarios...")
            loadComments(itemId)
        } catch (e: Exception) {
            println("❌ [ViewModel] Error al enviar comentario: ${e.message}")
        }
    }

    suspend fun deleteComment(commentId: Long, token: String) {
        try {
            println("🔑 [ViewModel] Token disponible al eliminar: ${token != null}")
            commentRepository.deleteComment(commentId, token)
            loadComments(currentItemId)
        } catch (e: Exception) {
            // Manejar el error si es necesario
        }
    }

    suspend fun addDefaultAdminComment(itemId: Long) {
        try {
            commentRepository.addDefaultAdminComment(itemId)
            loadComments(itemId) // Recargar los comentarios después de añadir el comentario por defecto
        } catch (e: Exception) {
            // Manejar el error si es necesario
        }
    }

    fun getAllComments(): List<CommentEntity> {
        return _comments.value
    }

    suspend fun verificarPuedeComentar(itemId: Long, token: String) {
        try {
            println("🔄 [ViewModel] Verificando si puede comentar para itemId: $itemId")
            val menuItem = menuRepository.getMenuItemByIdOnce(itemId)
            if (menuItem != null) {
                println("📦 [ViewModel] MenuItem encontrado: ${menuItem.name} en restaurante: ${menuItem.restaurant}")
                val puede = commentRepository.puedeComentar(itemId, token)
                println("✅ [ViewModel] Resultado de verificación: $puede")
                _puedeComentar.value = puede
            } else {
                println("❌ [ViewModel] MenuItem no encontrado para id: $itemId")
                _puedeComentar.value = false
            }
            println("🔍 [ViewModel] Verificando si puede comentar para itemId: $itemId")
            println("🔍 [ViewModel] Resultado verificación: $puedeComentar")
        } catch (e: Exception) {
            println("❌ [ViewModel] Error al verificar si puede comentar: ${e.message}")
            e.printStackTrace()
            _puedeComentar.value = false
        }
    }
} 