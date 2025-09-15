package com.jetbrains.greeting.data.entitys

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable


@Entity(tableName = "comments")
data class CommentEntity(
    @PrimaryKey(autoGenerate = false)
    val id: Long = 0,
    val menuItemId: Long,
    val user: String,
    val rating: Int,
    val text: String,
    val date: String,
    val destacado: Boolean = false
)

@kotlinx.serialization.Serializable
data class ComentarioDTO(
    val contenido: String,
    val valoracion: Int,
    val destacado: Boolean
)

@Serializable
data class ComentarioResponseDTO(
    val id: Long,
    val contenido: String,
    val valoracion: Int,
    val clienteEmail: String,
    val destacado: Boolean
)


