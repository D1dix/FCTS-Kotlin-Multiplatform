package com.jetbrains.greeting.data.remote

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import proyectorest.composeapp.generated.resources.Res
import android.util.Log

@Serializable
enum class Currency {
    USD, EUR
}

@Serializable
data class PaymentIntentDTO(
    val description: String,
    val amount: Int,
    val currency: String,
    val paymentMethodId: String,
    val correo: String
)

class PaymentService(private val httpClient: HttpClient, private val baseUrl: String) {

    suspend fun createPaymentIntent(amount: Int, description: String, paymentIntentId: String, correo: String): String {
        Log.d("PaymentService", "🔄 Creando PaymentIntent - Amount: $amount, Description: $description, PaymentMethodID: $paymentIntentId, Correo: $correo")
        val paymentIntent = PaymentIntentDTO(
            description = description,
            amount = amount,
            currency = "EUR",
            paymentMethodId = paymentIntentId,
            correo = correo
        )

        try {
            val response = httpClient.post("$baseUrl/api/stripe/paymentintent") {
                contentType(ContentType.Application.Json)
                setBody(paymentIntent)
            }
            Log.d("PaymentService", "✅ PaymentIntent creado exitosamente: ${response.bodyAsText()}")
            return response.bodyAsText()
        } catch (e: Exception) {
            Log.e("PaymentService", "❌ Error creando PaymentIntent: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    suspend fun confirmPayment(paymentIntentId: String): String {
        Log.d("PaymentService", "🔄 Confirmando pago - PaymentIntentID: $paymentIntentId")
        try {
            val response = httpClient.post("$baseUrl/api/stripe/confirm/$paymentIntentId")
            Log.d("PaymentService", "✅ Pago confirmado exitosamente: ${response.bodyAsText()}")
            return response.bodyAsText()
        } catch (e: Exception) {
            Log.e("PaymentService", "❌ Error confirmando pago: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    suspend fun cancelPayment(paymentIntentId: String): String {
        Log.d("PaymentService", "🔄 Cancelando pago - PaymentIntentID: $paymentIntentId")
        try {
            val response = httpClient.post("$baseUrl/api/stripe/cancel/$paymentIntentId")
            Log.d("PaymentService", "✅ Pago cancelado exitosamente: ${response.bodyAsText()}")
            return response.bodyAsText()
        } catch (e: Exception) {
            Log.e("PaymentService", "❌ Error cancelando pago: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }
} 