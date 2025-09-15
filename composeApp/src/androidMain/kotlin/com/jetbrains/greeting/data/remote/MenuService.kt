package com.jetbrains.greeting.data.remote

import com.jetbrains.greeting.data.entitys.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.delete
import io.ktor.client.request.forms.*
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class MenuService(private val client: HttpClient, private val baseUrl: String) {

    suspend fun getAllMenuItems(): List<Comida> {
        return withContext(Dispatchers.IO) {
            println("📋 [MenuService] Obteniendo todos los items del menú")
            client.get("$baseUrl/api/comida/listarComidas").body()
        }
    }

    suspend fun getMenuItemById(comida: String, restaurante: String): Comida? {
        return withContext(Dispatchers.IO) {
            println("🔍 [MenuService] Buscando item: $comida en restaurante: $restaurante")
            try {
                client.get("$baseUrl/api/comida/obtenerPorId") {
                    parameter("comida", comida)
                    parameter("restaurante", restaurante)
                }.body()
            } catch (e: Exception) {
                println("❌ [MenuService] Error al buscar item: ${e.message}")
                null
            }
        }
    }

    suspend fun getMenuItemsByRestaurant(restaurant: String): List<Comida> {
        return withContext(Dispatchers.IO) {
            println("🏪 [MenuService] Obteniendo items para restaurante: $restaurant")
            client.post("$baseUrl/api/comida/obtenerPorRestaurante") {
                contentType(ContentType.Application.Json)
                setBody(restaurant)
            }.body()
        }
    }

    suspend fun createMenuItem(comida: Comida): Boolean {
        println("📝 [MenuService] Creando nuevo item: ${comida.comidaPK.nComida}")
        try {
            val response = client.post("$baseUrl/api/comida/crear") {
                contentType(ContentType.Application.Json)
                setBody(comida)
            }
            println("✅ [MenuService] Item creado exitosamente")
            return response.status == HttpStatusCode.OK
        } catch (e: Exception) {
            println("❌ [MenuService] Error al crear item: ${e.message}")
            return false
        }
    }

    suspend fun updateMenuItem(comida: Comida): Boolean {
        return withContext(Dispatchers.IO) {
            println("🔄 [MenuService] Actualizando item: ${comida.comidaPK.nComida}")
            try {
                val response = client.post("$baseUrl/api/comida/actualizar") {
                    contentType(ContentType.Application.Json)
                    setBody(comida)
                }
                println("✅ [MenuService] Item actualizado exitosamente")
                response.status == HttpStatusCode.OK
            } catch (e: Exception) {
                println("❌ [MenuService] Error al actualizar item: ${e.message}")
                false
            }
        }
    }

    suspend fun deleteMenuItem(comida: String, restaurante: String): Boolean {
        return withContext(Dispatchers.IO) {
            println("🗑️ [MenuService] Eliminando item: $comida del restaurante: $restaurante")
            try {
                val response = client.delete("$baseUrl/api/comida/eliminar") {
                    parameter("comida", comida)
                    parameter("restaurante", restaurante)
                }
                println("✅ [MenuService] Item eliminado exitosamente")
                response.status == HttpStatusCode.OK
            } catch (e: Exception) {
                println("❌ [MenuService] Error al eliminar item: ${e.message}")
                false
            }
        }
    }

    suspend fun getAllRestaurants(): List<String> {
        return withContext(Dispatchers.IO) {
            println("🏪 [MenuService] Obteniendo lista de restaurantes")
            client.get("$baseUrl/api/comida/obtenerNombresRestaurante").body()
        }
    }

    suspend fun getRestaurantPhotos(restaurant: String): List<Foto> {
        return withContext(Dispatchers.IO) {
            println("📸 [MenuService] Obteniendo fotos del restaurante: $restaurant")
            client.get("$baseUrl/api/comida/obtenerTodosLosRestaurantes") {
                parameter("restaurante", restaurant)
            }.body()
        }
    }

    suspend fun subirFotoComida(imageBytes: ByteArray, comida: String, restaurante: String): String? {
        println("📸 [MenuService] Subiendo foto para comida: $comida en restaurante: $restaurante")
        try {
            val response = client.post("$baseUrl/api/fotos/subirfotocomida") {
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            append(
                                "imagenFichero", imageBytes,
                                Headers.build {
                                    append(HttpHeaders.ContentType, "image/jpeg")
                                    append(
                                        HttpHeaders.ContentDisposition,
                                        "filename=\"comida_${comida}_${restaurante}.jpg\""
                                    )
                                }
                            )
                            append("comida", comida)
                            append("restaurante", restaurante)
                        }
                    )
                )
            }

            println("📡 [MenuService] Respuesta HTTP: ${response.status}")
            if (response.status == HttpStatusCode.OK) {
                val dto = response.body<ComidaFotoDto>()
                println("✅ [MenuService] Foto subida exitosamente: ${dto.imagenUrl}")
                return dto.imagenUrl
            } else {
                println("❌ [MenuService] Error al subir foto: ${response.status}")
                return null
            }
        } catch (e: Exception) {
            println("❌ [MenuService] Error al subir foto: ${e.message}")
            e.printStackTrace()
            return null
        }
    }

    suspend fun toggleMenuItemVisibility(comidaPK: ComidaPK) {
        try {
            println("🔄 [MenuService] Cambiando visibilidad del item ${comidaPK.nComida} en ${comidaPK.nRestaurante}")
            println("📦 [MenuService] Enviando ComidaPK: $comidaPK")
            
            val response = client.put("$baseUrl/api/comida/alternar-ocultar") {
                contentType(ContentType.Application.Json)
                setBody(comidaPK)
            }
            
            println("📡 [MenuService] Respuesta del servidor: ${response.status}")
            val responseBody = response.bodyAsText()
            println("📦 [MenuService] Cuerpo de la respuesta: $responseBody")
            
            if (!response.status.isSuccess()) {
                throw Exception("Error del servidor: ${response.status} - $responseBody")
            }
            
            println("✅ [MenuService] Visibilidad actualizada correctamente")
        } catch (e: Exception) {
            println("❌ [MenuService] Error al cambiar visibilidad: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }
}

@kotlinx.serialization.Serializable
data class ComidaFotoDto(
    val comidaPK: ComidaPK,
    val imagenUrl: String?
)