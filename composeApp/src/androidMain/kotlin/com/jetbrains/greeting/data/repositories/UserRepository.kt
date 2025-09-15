package com.jetbrains.greeting.data.repositories

import com.jetbrains.greeting.data.entitys.*
import com.jetbrains.greeting.data.local.UserDao
import com.jetbrains.greeting.data.remote.UserService
import io.ktor.client.call.body
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class UserRepository(
    private val userDao: UserDao,
    private val userService: UserService
) {
    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: Flow<UserEntity?> = _currentUser.asStateFlow()

    suspend fun uploadProfileImage(email: String, imageBytes: ByteArray): Boolean {
        val user = getUserByEmail(email) ?: throw Exception("Usuario no encontrado")

        return try {
            val updatedUserDto = userService.updateProfileImage(email, imageBytes)  // devuelve ClienteFotoDto

            val updatedUser = user.copy(imageUrl = updatedUserDto)
            userDao.updateUser(updatedUser)
            setCurrentUser(updatedUser)

            true
        } catch (e: Exception) {
            println("❌ [UserRepository] Error al subir imagen: ${e.message}")
            false
        }
    }

    suspend fun registerUser(user: UserEntity) {
        println("🔍 [UserRepository] Intentando registrar usuario: ${user.email}, Rol: ${user.rol}, Restaurante: ${user.restaurant}")
        try {
            if (userService.register(user)) {
                userDao.insertUser(user)
                setCurrentUser(user)
            } else {
                throw Exception("Error al registrar usuario en el servidor")
            }
        } catch (e: Exception) {
            println("❌ [UserRepository] Error en registro: ${e.message}")
            // Verificamos si el usuario ya existe localmente
            val existingUser = userDao.getUserByEmail(user.email)
            if (existingUser != null) {
                throw Exception("El email ya está registrado")
            }
            throw e
        }
    }

    suspend fun login(email: String, contrasenia: String): UserEntity? {
        return try {
            val user = userService.login(email, contrasenia)
            println("🔍 [UserRepository] Login exitoso: ${user.email}, Rol: ${user.rol}, Restaurante: ${user.restaurant}")
            

            userDao.insertUser(user)
            setCurrentUser(user)
            user
        } catch (e: Exception) {
            println("❌ [UserRepository] Error en login remoto: ${e.message}")
            val localUser = userDao.login(email, contrasenia)
            if (localUser != null) {
                println("🔍 [UserRepository] Usando usuario local: ${localUser.email}, Rol: ${localUser.rol}, Restaurante: ${localUser.restaurant}")
                setCurrentUser(localUser)
            }
            localUser
        }
    }

    suspend fun setCurrentUser(user: UserEntity) {
        println("🔍 [UserRepository] Estableciendo usuario actual: ${user.email}, Rol: ${user.rol}, Restaurante: ${user.restaurant}")
        userDao.clearCurrentUser()
        val updatedUser = user.copy(isCurrentUser = true)
        userDao.setCurrentUser(updatedUser)
        _currentUser.value = updatedUser
    }

    suspend fun logout() {
        userDao.clearCurrentUser()
        _currentUser.value = null
    }

    suspend fun getUserByEmail(email: String): UserEntity? =
        userDao.getUserByEmail(email)

    suspend fun getCurrentUser(): Flow<UserEntity?> {
        return userDao.getCurrentUser()
    }

}

 