package com.jetbrains.greeting.data.repositories

import com.jetbrains.greeting.data.entitys.CommentEntity
import com.jetbrains.greeting.data.entitys.UserEntity
import com.jetbrains.greeting.data.local.CommentDao
import com.jetbrains.greeting.data.remote.CommentService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.emitAll

class CommentRepository(
    private val commentDao: CommentDao,
    private val commentService: CommentService,
    private val menuRepository: MenuRepository
) {

    suspend fun clearComments(itemId: Long) {
        try {
            // Primero eliminamos los comentarios locales
            commentDao.deleteCommentsForItem(itemId)
            println("✅ [Repo] Comentarios locales eliminados para itemId: $itemId")
        } catch (e: Exception) {
            println("❌ [Repo] Error al limpiar comentarios: ${e.message}")
            throw e
        }
    }

    fun getCommentsForItem(menuItemId: Long): Flow<List<CommentEntity>> = flow {
        val menuItem = menuRepository.getMenuItemByIdOnce(menuItemId)
        println("📦 [CommentRepository] Comentario obtenido del dao de room: $menuItem")
        if (menuItem != null) {
            try {
                val remoteComments = menuItem.id?.let {
                    commentService.obtenerComentariosPorComida(menuItem.name, menuItem.restaurant, it)
                }
                println("📦 [Repo] Comentarios obtenidos del backend: $remoteComments")

                if (remoteComments != null) {
                    // Primero eliminamos todos los comentarios locales para este item
                    commentDao.deleteCommentsForItem(menuItemId)

                    // Luego insertamos los comentarios del backend
                    remoteComments.forEach {
                        println("🔄 Insertando comentario ${it.text}")
                        commentDao.insertComment(it.copy(menuItemId = menuItemId))
                    }
                }
            } catch (e: Exception) {
                println("⚠️ [Repo] Error al obtener comentarios del backend: ${e.message}")
                // Si falla, usamos los comentarios locales
            }
        }
        emitAll(commentDao.getCommentsForItem(menuItemId))
    }


    suspend fun addComment(
        itemId: Long,
        text: String,
        rating: Int,
        email: String,
        token: String,
        date: String,
        destacado: Boolean,
        getUserByEmail: (String) -> UserEntity?
    ) {
        val menuItem = menuRepository.getMenuItemByIdOnce(itemId)
            ?: throw Exception("Comida no encontrada")

        val comment = CommentEntity(
            menuItemId = itemId,
            text = text,
            rating = rating,
            user = email,
            date = date,
            destacado = destacado
        )

        try {
            println("🔄 [Repo] Enviando comentario al servicio con comida=${menuItem.name}")
            val success = commentService.crearComentario(comment, token, menuItem.name, menuItem.restaurant)
            if (success == true) {
                println("✅ [Repo] Comentario creado exitosamente")
                commentDao.insertComment(comment)
            } else {
                println("❌ [Repo] Backend respondió negativamente")
                throw Exception("Respuesta negativa del backend")
            }
        } catch (e: Exception) {
            throw Exception("No autorizado localmente")
        }

        if (destacado) {
            kotlinx.coroutines.delay(43200000) // 12 horas en milisegundos
            commentDao.deleteComment(comment.id)
        }
    }


    suspend fun deleteComment(commentId: Long, token: String) {
        try {
            val success = commentService.eliminarComentario(token, commentId)
            if (success) {
                commentDao.deleteComment(commentId)
            } else {
                throw Exception("Error al eliminar comentario en el backend")
            }
        } catch (e: Exception) {

        }
    }

    /*suspend fun deleteSpecialComments() {MIRAR POR QUE LE TINES QUE CREAR UNA NUEVA RITA EN EL BACKEND O PASARLE UN TOKEN DIRIA QUE LO MEJOR LA SEGUNDO
        val specialComments = commentDao.getSpecialComments()
        specialComments.forEach{comment ->

                commentService.eliminarComentario(comment.id, comment.user)
                commentDao.deleteComment(comment.id)
        }
    }*/


    fun getAverageRating(itemId: Long): Flow<Float?> = flow {
        val menuItem = menuRepository.getMenuItemByIdOnce(itemId)
        if (menuItem != null) {
            try {
                // Intentar obtener el promedio del backend
                val backendPromedio = commentService.obtenerPromedioValoracion(menuItem.name, menuItem.restaurant)
                emit(backendPromedio.toFloat())
            } catch (e: Exception) {
                println("⚠️ [Repo] Error al obtener promedio del backend: ${e.message}")
                println("⚠️ [Repo] Usando promedio local como fallback")
                // Si falla, usar el promedio local de Room
                emitAll(commentDao.getAverageRating(itemId))
            }
        } else {
            emitAll(commentDao.getAverageRating(itemId))
        }
    }

    suspend fun addDefaultAdminComment(itemId: Long) {
        val defaultComment = CommentEntity(
            menuItemId = itemId,
            text = "¡Este plato es una especialidad de la casa! Recomendado por nuestro chef.",
            rating = 5,
            user = "Chef",
            date = System.currentTimeMillis().toString(),
            destacado = true
        )
        commentDao.insertComment(defaultComment)
        kotlinx.coroutines.delay(43200000) // 12 horas en milisegundos
        commentDao.deleteComment(defaultComment.id)
    }

    suspend fun puedeComentar(itemId: Long, token: String): Boolean {
        val menuItem = menuRepository.getMenuItemByIdOnce(itemId)
            ?: return false

        return try {
            println("🔄 [Repo] Verificando si puede comentar para: ${menuItem.name} en restaurante: ${menuItem.restaurant}")
            val result = commentService.puedeComentar(menuItem.name, menuItem.restaurant, token)
            println("✅ [Repo] Resultado de verificación: $result")
            result
        } catch (e: Exception) {
            println("❌ [Repo] Error al verificar si puede comentar: ${e.message}")
            e.printStackTrace()
            false
        }
    }
}
