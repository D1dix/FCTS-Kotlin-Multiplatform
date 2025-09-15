package com.jetbrains.greeting.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jetbrains.greeting.data.entitys.UserEntity
import com.jetbrains.greeting.data.entitys.UserType
import com.jetbrains.greeting.data.repositories.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UserViewModel(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    init {
        viewModelScope.launch {
            userRepository.getCurrentUser().collect { user ->
                println("🔍 [UserViewModel] Usuario actual actualizado: ${user?.email}, Rol: ${user?.rol}, Restaurante: ${user?.restaurant}")
                _currentUser.value = user
                println("🔑 [UserViewModel] Token del usuario: ${user?.token}")
            }
        }
    }

    fun uploadProfileImage(email: String, imageBytes: ByteArray) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                val success = userRepository.uploadProfileImage(email, imageBytes)
                if (success) {
                    _loginState.value = LoginState.Success
                } else {
                    _loginState.value = LoginState.Error("Error al subir la imagen")
                }
            } catch (e: Exception) {
                println("❌ [UserViewModel] Error al subir imagen: ${e.message}")
                _loginState.value = LoginState.Error(e.message ?: "Error al subir la imagen")
            }
        }
    }

    fun register(
        email: String,
        contrasenia: String,
        nombre: String,
        role: String = UserType.ROLE_USER.toString(),
        restaurant: String? = null,
        context: Context? = null,
        imageBytes: ByteArray? = null
    ) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                val existingUser = userRepository.getUserByEmail(email)
                if (existingUser != null) {
                    _loginState.value = LoginState.Error("El email ya está registrado")
                    return@launch
                }

                val newUser = UserEntity(
                    email = email,
                    contrasenia = contrasenia,
                    nombre = nombre,
                    isCurrentUser = true,
                    rol = role,
                    token = "",
                    restaurant = restaurant
                )

                userRepository.registerUser(newUser)

                val loggedInUser = userRepository.login(email, contrasenia)
                _currentUser.value = loggedInUser

                println("📷 [UserViewModel] imageBytes is ${if (imageBytes == null) "null" else "not null"}")

                if (imageBytes != null) {
                    println("📤 [UserViewModel] Subiendo imagen de perfil para $email")
                    userRepository.uploadProfileImage(email, imageBytes)
                }


                _loginState.value = LoginState.Success

            } catch (e: Exception) {
                _loginState.value = LoginState.Error(e.message ?: "Error al registrar usuario")
            }
        }
    }



    fun loginAsGuest() {
        viewModelScope.launch {
            val guestUser = UserEntity(
                email = "guest@restaurant.com",
                contrasenia = "",
                nombre = "Invitado",
                rol = UserType.INVITADO.toString(),
                isCurrentUser = true,
                token = "",
                restaurant = null
            )
            userRepository.setCurrentUser(guestUser)
            _currentUser.value = guestUser
            _loginState.value = LoginState.Success
        }
    }

    fun login(email: String, contrasenia: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                val user = userRepository.login(email, contrasenia)
                if (user != null) {
                    _loginState.value = LoginState.Success
                } else {
                    _loginState.value = LoginState.Error("Credenciales inválidas")
                }
            } catch (e: Exception) {
                _loginState.value = LoginState.Error(e.message ?: "Error al iniciar sesión")
            }
        }
    }



    fun logout() {
        viewModelScope.launch {
            userRepository.logout()
            _currentUser.value = null
            _loginState.value = LoginState.Idle
        }
    }


}

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    object Success : LoginState()
    data class Error(val message: String) : LoginState()
}
