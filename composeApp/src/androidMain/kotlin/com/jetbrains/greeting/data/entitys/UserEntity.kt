package com.jetbrains.greeting.data.entitys

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.jetbrains.greeting.data.local.Converters
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "users")
@TypeConverters(Converters::class)
data class UserEntity(
    @PrimaryKey
    val email: String,
    val nombre: String,
    val contrasenia: String,
    var rol: String? = UserType.ROLE_USER.toString(),
    val token: String,
    val isCurrentUser: Boolean = false,
    var restaurant: String? = null,
    var imageUrl: String? = null
)


