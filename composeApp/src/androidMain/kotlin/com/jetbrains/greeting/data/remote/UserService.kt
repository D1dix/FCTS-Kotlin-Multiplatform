package com.jetbrains.greeting.data.remote

import com.jetbrains.greeting.data.entitys.*
import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.content.*

class UserService(private val client: HttpClient, private val baseUrl: String) {

    suspend fun register(user: UserEntity): Boolean {
        val cliente = Cliente(
            email = user.email,
            nombre = user.nombre,
            contrasenia = user.contrasenia,
            rol = user.rol ?: UserType.ROLE_USER.toString(),
            token = user.token,
            restaurante = user.restaurant,
            imageUrl = user.imageUrl
        )

        println("🔍 [UserService] Registrando usuario: $cliente")

        val response = client.post("$baseUrl/api/clientes/register") {
            contentType(ContentType.Application.Json)
            setBody(cliente)
        }

        if (response.status == HttpStatusCode.OK) {
            val registeredUser = response.body<Cliente>()
            println("🔍 [UserService] Usuario registrado: $registeredUser")
            user.rol = registeredUser.rol
            user.restaurant = registeredUser.restaurante
            user.imageUrl = registeredUser.imageUrl
            return true
        }
        return false
    }

    suspend fun updateProfileImage(email: String, imageBytes: ByteArray): String {
        println("📡 [UserService] Subiendo imagen para: $email")

        val response = client.post("$baseUrl/api/fotos/actualizarfotoperfil") {
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append("imagenFichero", imageBytes,
                            Headers.build {
                                append(HttpHeaders.ContentType, "image/jpeg")
                                append(HttpHeaders.ContentDisposition, "filename=\"profile_$email.jpg\"")
                            }
                        )
                        append("email", email)
                    }
                )
            )
        }

        println("📡 [UserService] Respuesta HTTP: ${response.status}")
        val dto = response.body<ClienteFotoDto>()


        return dto.imagenUrl
    }



    suspend fun login(email: String, contrasenia: String): UserEntity {
        val response = client.post("$baseUrl/api/clientes/login") {
            url {
                parameters.append("email", email)
                parameters.append("password", contrasenia)
            }
        }

        if (response.status == HttpStatusCode.OK) {
            val loginResponse = response.body<LoginResponse>()
            println("🔍 [UserService] Respuesta del login: $loginResponse")
            
            val token = loginResponse.token ?: ""
            println("🔑 [UserService] Token recibido: $token")
            
            return UserEntity(
                email = email,
                nombre = loginResponse.nombre ?: "Usuario",
                contrasenia = contrasenia,
                rol = loginResponse.rol ?: UserType.ROLE_USER.toString(),
                token = token,
                isCurrentUser = true,
                restaurant = loginResponse.restaurant,
                imageUrl = loginResponse.imageUrl
            )
        } else {
            throw Exception("Error al iniciar sesión: ${response.status}")
        }
    }


}

// Clase para mapear la respuesta del backend
@kotlinx.serialization.Serializable
private data class Cliente(
    val email: String,
    val nombre: String,
    val contrasenia: String,
    val rol: String,
    val token: String? = null,
    val expirado: Boolean = false,
    val restaurante: String? = null,
    val imageUrl: String? = null
)
@kotlinx.serialization.Serializable
data class ClienteFotoDto(
    val email: String,
    val imagenUrl: String
)

@kotlinx.serialization.Serializable
private data class LoginResponse(
    val nombre: String? = null,
    val rol: String? = null,
    val token: String? = null,
    val restaurant: String? = null,
    val imageUrl: String? = null,
    val mensaje: String? = null
)
