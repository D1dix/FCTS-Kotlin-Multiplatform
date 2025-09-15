package com.jetbrains.greeting.data.remote

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object LocalDateSerializer : KSerializer<LocalDate> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LocalDate", PrimitiveKind.STRING)

    @RequiresApi(Build.VERSION_CODES.O)
    override fun serialize(encoder: Encoder, value: LocalDate) {
        encoder.encodeString(value.format(DateTimeFormatter.ISO_LOCAL_DATE))
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun deserialize(decoder: Decoder): LocalDate {
        return LocalDate.parse(decoder.decodeString(), DateTimeFormatter.ISO_LOCAL_DATE)
    }
}

class CartService(private val client: HttpClient, private val baseUrl: String) {

    suspend fun crearPedidoSimple(email: String, restauranteId: String): PedidoCreadoResponse {
        val response = client.post("$baseUrl/api/pedidos/crear-simple") {
            url {
                parameters.append("email", email)
                parameters.append("restaurante", restauranteId)
            }
        }

        if (response.status == HttpStatusCode.OK) {
            return response.body()
        } else {
            throw Exception("No se pudo crear el pedido: ${response.status}")
        }
    }

    suspend fun añadirComida(pedidoId: String, comidaNombre: String, restauranteId: String): PedidoResponse {
        val response = client.post("$baseUrl/api/pedidos/añadir-comida") {
            url {
                parameters.append("pedidoId", pedidoId)
                parameters.append("nComida", comidaNombre)
                parameters.append("nRestaurante", restauranteId)
            }
        }

        if (response.status == HttpStatusCode.OK) {
            return response.body()
        } else {
            throw Exception("Error al añadir comida: ${response.status}")
        }
    }

    suspend fun cambiarEstadoPedido(pedidoId: String, activo: Boolean): PedidoResponse {
        Log.d("CartService", "🔄 Cambiando estado del pedido $pedidoId a ${if (activo) "activo" else "inactivo"}")
        val response = client.put("$baseUrl/api/pedidos/cambiar-estado") {
            url {
                parameters.append("id", pedidoId)
                parameters.append("activo", activo.toString())
            }
        }

        if (response.status == HttpStatusCode.OK) {
            Log.d("CartService", "✅ Estado del pedido actualizado correctamente")
            return response.body()
        } else {
            Log.e("CartService", "❌ Error al cambiar estado del pedido: ${response.status}")
            throw Exception("Error al cambiar estado del pedido: ${response.status}")
        }
    }

    suspend fun getTop5Comidas(restaurante: String? = null): List<TopComidaResponse> {
        val response = client.get("$baseUrl/api/pedidos/top5-comidas") {
            url {
                restaurante?.let { parameters.append("restaurante", it) }
            }
        }

        if (response.status == HttpStatusCode.OK) {
            return response.body()
        } else {
            return emptyList()
        }
    }

    suspend fun getPedidosUltimos7Dias(): Int {
        val response = client.get("$baseUrl/api/pedidos/ultimos-7-dias")
        if (response.status == HttpStatusCode.OK) {
            return response.body()
        } else {
            return 0
        }
    }

    suspend fun listarPedidos(): List<PedidoListadoDTO> {
        val response = client.get("$baseUrl/api/pedidos/listar")
        if (response.status == HttpStatusCode.OK) {
            return response.body()
        } else {
            return emptyList()
        }
    }

    suspend fun restarComida(pedidoId: String, comidaNombre: String, restauranteId: String): PedidoResponse {
        val response = client.post("$baseUrl/api/pedidos/restar-comida") {
            url {
                parameters.append("pedidoId", pedidoId)
                parameters.append("nComida", comidaNombre)
                parameters.append("nRestaurante", restauranteId)
            }
        }

        if (response.status == HttpStatusCode.OK) {
            return response.body()
        } else {
            throw Exception("Error al restar comida: ${response.status}")
        }
    }

    suspend fun eliminarComida(pedidoId: String, comidaNombre: String, restauranteId: String): PedidoResponse {
        val response = client.delete("$baseUrl/api/pedidos/eliminar-comida") {
            url {
                parameters.append("pedidoId", pedidoId)
                parameters.append("nComida", comidaNombre)
                parameters.append("nRestaurante", restauranteId)
            }
        }

        if (response.status == HttpStatusCode.OK) {
            return response.body()
        } else {
            throw Exception("Error al eliminar comida: ${response.status}")
        }
    }

    suspend fun getTop5ComidasGlobal(): List<TopComidaResponse> {
        Log.d("CartService", "🔄 Realizando petición GET a $baseUrl/api/pedidos/top5-comidas/todos")
        val response = client.get("$baseUrl/api/pedidos/top5-comidas/todos")
        Log.d("CartService", "📡 Respuesta recibida con status: ${response.status}")

        if (response.status == HttpStatusCode.OK) {
            try {
                val result = response.body<List<TopComidaResponse>>()
                Log.d("CartService", "✅ Datos recibidos correctamente: ${result.size} items")
                return result
            } catch (e: Exception) {
                Log.e("CartService", "❌ Error deserializando respuesta: ${e.message}")
                e.printStackTrace()
                return emptyList()
            }
        } else {
            Log.e("CartService", "❌ Error en la respuesta: ${response.status}")
            return emptyList()
        }
    }

    suspend fun getTop5ComidasPorRestaurante(restaurante: String): List<TopComidaResponse> {
        Log.d("CartService", "🔄 Realizando petición GET a $baseUrl/api/pedidos/top5-comidas para restaurante: $restaurante")
        try {
            val response = client.get("$baseUrl/api/pedidos/top5-comidas") {
                url { parameters.append("restaurante", restaurante) }
            }
            Log.d("CartService", "📡 Respuesta recibida con status: ${response.status}")


            if (response.status == HttpStatusCode.OK) {
                try {
                    val result = response.body<List<TopComidaResponse>>()
                    Log.d("CartService", "✅ Datos recibidos correctamente: ${result.size} items")
                    return result
                } catch (e: Exception) {
                    Log.e("CartService", "❌ Error deserializando respuesta: ${e.message}")
                    e.printStackTrace()
                    return emptyList()
                }
            } else {
                Log.e("CartService", "❌ Error en la respuesta: ${response.status}")
                return emptyList()
            }
        } catch (e: Exception) {
            Log.e("CartService", "❌ Error en la petición: ${e.message}")
            e.printStackTrace()
            return emptyList()
        }
    }

    suspend fun eliminarPedido(pedidoId: String): Boolean {
        Log.d("CartService", "🔄 Eliminando pedido: $pedidoId")
        val response = client.delete("$baseUrl/api/pedidos/eliminar") {
            url {
                parameters.append("id", pedidoId)
            }
        }

        if (response.status == HttpStatusCode.OK) {
            Log.d("CartService", "✅ Pedido eliminado correctamente")
            return true
        } else {
            Log.e("CartService", "❌ Error al eliminar pedido: ${response.status}")
            return false
        }
    }

    suspend fun getUltimos5Pedidos(email: String): List<PedidoListadoDTO> {
        Log.d("CartService", "🔄 Obteniendo últimos 5 pedidos para el usuario: $email")
        val response = client.get("$baseUrl/api/pedidos/ultimos5") {
            url {
                parameters.append("email", email)
            }
        }

        if (response.status == HttpStatusCode.OK) {
            try {
                val result = response.body<List<PedidoListadoDTO>>()
                Log.d("CartService", "✅ Datos recibidos correctamente: ${result.size} pedidos")
                return result
            } catch (e: Exception) {
                Log.e("CartService", "❌ Error deserializando respuesta: ${e.message}")
                e.printStackTrace()
                return emptyList()
            }
        } else {
            Log.e("CartService", "❌ Error en la respuesta: ${response.status}")
            return emptyList()
        }
    }
}

@kotlinx.serialization.Serializable
data class PedidoCreadoResponse(
    val id: String,
    val emailCliente: String,
    val restaurante: String
)

@kotlinx.serialization.Serializable
data class PedidoResponse(
    val id: String,
    val cliente: ClienteResponse,
    @SerialName("comidas")
    val comidas: List<ComidaResponse> = emptyList(),
    val cantidadFinal: Int,
    val activo: Boolean = true,
    @SerialName("restaurante")
    val restaurante: String
)

@kotlinx.serialization.Serializable
data class ClienteResponse(
    val email: String,
    val nombre: String
)

@kotlinx.serialization.Serializable
data class ComidaResponse(
    val nombre: String,
    val precio: Int
)

@kotlinx.serialization.Serializable
data class TopComidaResponse(
    @SerialName("nombreComida")
    val nombre: String,
    @SerialName("cantidadTotal")
    val cantidad: Int,
    @SerialName("restaurante")
    val restaurante: String
)

@kotlinx.serialization.Serializable
data class PedidoListadoDTO(
    val id: String,
    val emailCliente: String,
    val items: List<ItemDTO>,
    @Serializable(with = LocalDateSerializer::class)
    val fechaCreacion: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val fechaExpiracion: LocalDate,
    val restaurante: String,
    val cantidadFinal: Int
)

@kotlinx.serialization.Serializable
data class ItemDTO(
    val nombreComida: String,
    val cantidad: Int,
    val precioUnitario: Int
)
