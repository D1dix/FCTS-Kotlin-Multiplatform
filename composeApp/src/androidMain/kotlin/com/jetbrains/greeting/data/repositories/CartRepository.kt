package com.jetbrains.greeting.data.repositories

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.jetbrains.greeting.data.entitys.CartItemEntity
import com.jetbrains.greeting.data.entitys.CartStatus
import com.jetbrains.greeting.data.entitys.MenuItemEntity
import com.jetbrains.greeting.data.local.CartItemDao
import com.jetbrains.greeting.data.local.MenuItemDao
import com.jetbrains.greeting.data.remote.CartService
import com.jetbrains.greeting.data.remote.MenuService
import com.jetbrains.greeting.data.remote.PaymentService
import com.jetbrains.greeting.data.remote.TopComidaResponse
import com.jetbrains.greeting.data.remote.PedidoListadoDTO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.util.Calendar
import java.util.Date
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

@RequiresApi(Build.VERSION_CODES.O)
class CartRepository(
    private val cartItemDao: CartItemDao,
    private val menuItemDao: MenuItemDao,
    val cartService: CartService,
    private val paymentService: PaymentService
) {
    suspend fun getActiveCartForUser(userEmail: String): Flow<CartItemEntity?> {
        try {
            Log.d("CartRepository", "🔄 Obteniendo pedidos activos para usuario: $userEmail")
            // Obtener todos los pedidos del backend
            val pedidos = cartService.listarPedidos()
            Log.d("CartRepository", "📦 Total de pedidos obtenidos: ${pedidos.size}")

            // Filtrar el pedido activo del usuario
            val pedidoActivo = pedidos.find { it.emailCliente == userEmail && it.fechaExpiracion.isAfter(LocalDate.now()) }
            Log.d("CartRepository", "🔍 Pedido activo encontrado: ${pedidoActivo != null}")

            pedidoActivo?.let { pedido ->
                val itemsMap = mutableMapOf<Long, Int>()
                for (item in pedido.items) {
                    val id = menuItemDao.getMenuItemIdByNameAndRestaurant(item.nombreComida, pedido.restaurante)
                    if (id != null) {
                        itemsMap[id] = item.cantidad
                    } else {
                        Log.w("CartRepository", "❗ MenuItem no encontrado para ${item.nombreComida} en restaurante ${pedido.restaurante}")
                    }
                }

                Log.d("CartRepository", "✅ Creando entidad de carrito para pedido: ${pedido.id}")
                val cartEntity = CartItemEntity(
                    id = pedido.id,
                    userEmail = pedido.emailCliente,
                    items = itemsMap,
                    creationDate = Date.from(pedido.fechaCreacion.atStartOfDay().toInstant(ZoneOffset.UTC)),
                    expirationDate = Date.from(pedido.fechaExpiracion.atStartOfDay().toInstant(ZoneOffset.UTC)),
                    status = CartStatus.ACTIVE,
                    total = pedido.cantidadFinal,
                    restaurantId = pedido.restaurante
                )
                cartItemDao.insertCart(cartEntity)
                Log.d("CartRepository", "💾 Carrito guardado en base de datos local")
            }
        } catch (e: Exception) {
            Log.e("CartRepository", "❌ Error sincronizando pedido activo: ${e.message}")
        }

        return cartItemDao.getCartByUserAndStatus(userEmail, CartStatus.ACTIVE)
    }

    suspend fun getCartsByUserAndStatuses(userEmail: String, statuses: List<CartStatus>, restaurantId: String? = null): Flow<List<CartItemEntity>> {
        try {
            // Obtener todos los pedidos del backend
            val pedidos = cartService.listarPedidos()

            // Filtrar los pedidos según el rol y restaurante, ignorando los que tienen emailCliente null
            val pedidosFiltrados = when {
                restaurantId == "all" -> pedidos.filter { it.emailCliente != null } // Si es admin con acceso a todos los restaurantes
                restaurantId != null -> pedidos.filter { it.emailCliente != null && it.restaurante == restaurantId } // Si es admin de un restaurante específico
                else -> pedidos.filter { it.emailCliente == userEmail } // Si es usuario normal
            }

            pedidosFiltrados.forEach { pedido ->
                val itemsMap = mutableMapOf<Long, Int>()
                for (item in pedido.items) {
                    val id = menuItemDao.getMenuItemIdByNameAndRestaurant(item.nombreComida, pedido.restaurante)
                    if (id != null) {
                        itemsMap[id] = item.cantidad
                    } else {
                        Log.w("CartRepository", "❗ MenuItem no encontrado para ${item.nombreComida} en restaurante ${pedido.restaurante}")
                    }
                }

                Log.d("CartRepository", "✅ Creando entidad de carrito para pedido: ${pedido.id}")
                val cartEntity = CartItemEntity(
                    id = pedido.id,
                    userEmail = pedido.emailCliente ?: userEmail, // Usar userEmail como fallback si emailCliente es null
                    items = itemsMap,
                    creationDate = Date.from(pedido.fechaCreacion.atStartOfDay().toInstant(ZoneOffset.UTC)),
                    expirationDate = Date.from(pedido.fechaExpiracion.atStartOfDay().toInstant(ZoneOffset.UTC)),
                    status = CartStatus.ACTIVE,
                    total = pedido.cantidadFinal,
                    restaurantId = pedido.restaurante
                )
                cartItemDao.insertCart(cartEntity)
            }
        } catch (e: Exception) {
            Log.e("CartRepository", "Error sincronizando pedidos del backend: ${e.message}")
        }

        return if (restaurantId == "all") {
            cartItemDao.getCartsByStatusesAndRestaurant(statuses, null)
        } else {
            cartItemDao.getCartsByStatusesAndRestaurant(statuses, restaurantId)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun refreshActiveCartFromBackend(userEmail: String): CartItemEntity? {
        return try {
            val pedidos = cartService.listarPedidos()
            val pedidoActivo = pedidos.find { it.emailCliente == userEmail && it.fechaExpiracion.isAfter(LocalDate.now()) }

            pedidoActivo?.let {pedido ->
                val itemsMap = mutableMapOf<Long, Int>()
                for (item in pedido.items) {
                    val id = menuItemDao.getMenuItemIdByNameAndRestaurant(item.nombreComida, pedido.restaurante)
                    if (id != null) {
                        itemsMap[id] = item.cantidad
                    } else {
                        Log.w("CartRepository", "❗ MenuItem no encontrado para ${item.nombreComida} en restaurante ${pedido.restaurante}")
                    }
                }

                Log.d("CartRepository", "✅ Creando entidad de carrito para pedido: ${pedido.id}")
                val cartEntity = CartItemEntity(
                    id = pedido.id,
                    userEmail = pedido.emailCliente,
                    items = itemsMap,
                    creationDate = Date.from(pedido.fechaCreacion.atStartOfDay().toInstant(ZoneOffset.UTC)),
                    expirationDate = Date.from(pedido.fechaExpiracion.atStartOfDay().toInstant(ZoneOffset.UTC)),
                    status = CartStatus.ACTIVE,
                    total = pedido.cantidadFinal,
                    restaurantId = pedido.restaurante
                )
                cartItemDao.insertCart(cartEntity)
                cartEntity
            }
        } catch (e: Exception) {
            Log.e("CartRepository", "Error actualizando carrito desde el backend: ${e.message}")
            null
        }
    }

    suspend fun createCart(id: String, userEmail: String, items: Map<Long, Int>, restaurantId: String? = null): Long? {
        try {
            // Crear el pedido en el backend
            val pedidoResponse = cartService.crearPedidoSimple(userEmail, restaurantId ?: throw Exception("Se requiere un restaurante"))

            // Crear el carrito local con los datos de la respuesta
            val cart = CartItemEntity(
                id = pedidoResponse.id,
                userEmail = pedidoResponse.emailCliente,
                items = items,
                creationDate = Date(),
                expirationDate = Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000), // 24 horas
                status = CartStatus.ACTIVE,
                total = 0,
                restaurantId = pedidoResponse.restaurante
            )

            // Guardar en Room
            val cartId = cartItemDao.insertCart(cart)

            // Cargar el carrito actualizado
            getActiveCartForUser(userEmail)

            return cartId
        } catch (e: Exception) {
            Log.e("CartRepository", "Error al crear el carrito: ${e.message}")
            return null
        }
    }

    suspend fun updateCart(cart: CartItemEntity) {
        try {
            // Primero obtenemos el carrito original de la base de datos
            val existingCart = cartItemDao.getCartById(cart.id)

            val oldItems = existingCart?.items ?: emptyMap()
            val newItems = cart.items

            // Comparar los items antiguos y nuevos
            newItems.forEach { (itemId, newQty) ->
                val oldQty = oldItems[itemId] ?: 0
                val diff = newQty - oldQty

                if (diff > 0) {
                    // Si hay diferencia positiva, añadir solo esa cantidad
                    val menuItem = menuItemDao.getMenuItemById(itemId).firstOrNull()
                    menuItem?.let {
                        repeat(diff) {
                            try {
                                cartService.añadirComida(
                                    pedidoId = cart.id,
                                    comidaNombre = menuItem.name,
                                    restauranteId = cart.restaurantId ?: menuItem.restaurant
                                )
                            } catch (e: Exception) {
                                Log.e("CartRepository", "Error añadiendo comida ${menuItem}: ${e.message}")
                            }
                        }
                    }
                } else if (diff < 0) {
                    // Si hay diferencia negativa, restar la cantidad
                    val menuItem = menuItemDao.getMenuItemById(itemId).firstOrNull()
                    menuItem?.let {
                        repeat(-diff) {
                            try {
                                cartService.restarComida(
                                    pedidoId = cart.id,
                                    comidaNombre = menuItem.name,
                                    restauranteId = cart.restaurantId ?: menuItem.restaurant
                                )
                            } catch (e: Exception) {
                                Log.e("CartRepository", "Error restando comida ${menuItem}: ${e.message}")
                            }
                        }
                    }
                }
            }

            // Eliminar items que ya no existen
            oldItems.forEach { (itemId, _) ->
                if (!newItems.containsKey(itemId)) {
                    val menuItem = menuItemDao.getMenuItemById(itemId).firstOrNull()
                    menuItem?.let {
                        try {
                            cartService.eliminarComida(
                                pedidoId = cart.id,
                                comidaNombre = menuItem.name,
                                restauranteId = cart.restaurantId ?: menuItem.restaurant
                            )
                        } catch (e: Exception) {
                            Log.e("CartRepository", "Error eliminando comida ${menuItem}: ${e.message}")
                        }
                    }
                }
            }

            // Guardar el carrito actualizado en Room
            cartItemDao.insertCart(cart.copy(
                total = cart.total,
                restaurantId = cart.restaurantId
            ))
        } catch (e: Exception) {
            Log.e("CartRepository", "Error al actualizar el carrito: ${e.message}")
        }
    }


    suspend fun closeCart(cartId: String) {
        try {
            Log.d("CartRepository", "🔄 Cerrando carrito: $cartId")
            // Primero actualizamos el estado en el backend
            try {
                cartService.cambiarEstadoPedido(cartId, false)
                Log.d("CartRepository", "✅ Estado actualizado en el backend")
            } catch (e: Exception) {
                Log.e("CartRepository", "❌ Error al actualizar el estado en el backend: ${e.message}")
                throw e
            }

            // Luego actualizamos en Room
            cartItemDao.updateCartStatus(cartId, CartStatus.CLOSED)
            Log.d("CartRepository", "✅ Estado actualizado en Room")

            // Actualizamos el carrito local con el estado CLOSED
            val currentCart = cartItemDao.getCartById(cartId)
            currentCart?.let { cart ->
                val updatedCart = cart.copy(
                    status = CartStatus.CLOSED,
                    total = cart.total
                )
                cartItemDao.insertCart(updatedCart)
                Log.d("CartRepository", "✅ Carrito actualizado en Room")
            }
        } catch (e: Exception) {
            Log.e("CartRepository", "❌ Error al cerrar el carrito: ${e.message}")
            throw e
        }
    }

    suspend fun hasActiveCartForRestaurant(userEmail: String, restaurantId: String): Boolean {
        return try {
            cartItemDao.hasActiveCartForRestaurant(userEmail, restaurantId)
        } catch (e: Exception) {
            Log.e("CartRepository", "Error al verificar carrito activo: ${e.message}")
            false
        }
    }

    suspend fun checkAndExpireCarts() {
        try {
            val now = Date()
            val twoHoursAgo = Date(now.time - (2 * 60 * 60 * 1000)) // 2 horas atrás

            // Marcar carritos activos como expirados si han pasado 24 horas
            cartItemDao.expireActiveCarts(now)

            // Eliminar carritos expirados y cerrados después de 2 horas
            cartItemDao.deleteExpiredCarts(twoHoursAgo)

            // Actualizar en el backend los carritos expirados
            val expiredCarts = cartItemDao.getExpiredCarts(now)
            expiredCarts.forEach { cart ->
                if (cart.status == CartStatus.ACTIVE) {
                    try {
                        cartService.cambiarEstadoPedido(cart.id, false)
                    } catch (e: Exception) {
                        Log.e("CartRepository", "Error actualizando estado en backend: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("CartRepository", "Error al expirar carritos: ${e.message}")
        }
    }

    suspend fun restarItemDelCarrito(cartId: String, itemId: Long) {
        try {
            val cart = cartItemDao.getCartById(cartId) ?: return
            val menuItem = menuItemDao.getMenuItemById(itemId).firstOrNull() ?: return

            // Actualizar en el backend
            cartService.restarComida(
                pedidoId = cartId,
                comidaNombre = menuItem.name,
                restauranteId = cart.restaurantId ?: menuItem.restaurant
            )

            // Actualizar localmente
            val updatedItems = cart.items.toMutableMap()
            val currentQuantity = updatedItems[itemId] ?: 0
            if (currentQuantity > 1) {
                updatedItems[itemId] = currentQuantity - 1
            } else {
                updatedItems.remove(itemId)
            }

            val updatedCart = cart.copy(items = updatedItems)
            cartItemDao.insertCart(updatedCart)
        } catch (e: Exception) {
            Log.e("CartRepository", "Error al restar item del carrito: ${e.message}")
            throw e
        }
    }

    suspend fun eliminarItemDelCarrito(cartId: String, itemId: Long) {
        try {
            val cart = cartItemDao.getCartById(cartId) ?: return
            val menuItem = menuItemDao.getMenuItemById(itemId).firstOrNull() ?: return

            // Actualizar en el backend
            cartService.eliminarComida(
                pedidoId = cartId,
                comidaNombre = menuItem.name,
                restauranteId = cart.restaurantId ?: menuItem.restaurant
            )

            // Actualizar localmente
            val updatedItems = cart.items.toMutableMap()
            updatedItems.remove(itemId)

            val updatedCart = cart.copy(items = updatedItems)
            cartItemDao.insertCart(updatedCart)
        } catch (e: Exception) {
            Log.e("CartRepository", "Error al eliminar item del carrito: ${e.message}")
            throw e
        }
    }

    suspend fun cancelPayment(paymentIntentId: String): Boolean {
        return try {
            paymentService.cancelPayment(paymentIntentId)
            true
        } catch (e: Exception) {
            Log.e("CartRepository", "Error cancelando pago: ${e.message}")
            false
        }
    }

    suspend fun getTop5ComidasGlobal(): List<TopComidaResponse> {
        return try {
            Log.d("CartRepository", "🔄 Obteniendo Top 5 comidas global desde el servicio")
            val result = cartService.getTop5ComidasGlobal()
            Log.d("CartRepository", "✅ Top 5 comidas global obtenido: ${result.size} items")
            result
        } catch (e: Exception) {
            Log.e("CartRepository", "❌ Error obteniendo Top 5 comidas global: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getTop5ComidasPorRestaurante(restaurante: String): List<TopComidaResponse> {
        return try {
            Log.d("CartRepository", "🔄 Obteniendo Top 5 comidas para restaurante: $restaurante")
            val result = cartService.getTop5ComidasPorRestaurante(restaurante)
            Log.d("CartRepository", "✅ Top 5 comidas obtenido para restaurante $restaurante: ${result.size} items")
            result.forEach { comida ->
                Log.d("CartRepository", "🍽️ Comida: ${comida.nombre}, Cantidad: ${comida.cantidad}, Restaurante: ${comida.restaurante}")
            }
            result
        } catch (e: Exception) {
            Log.e("CartRepository", "❌ Error obteniendo Top 5 comidas para restaurante $restaurante: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun processStripePayment(cartId: String, amount: Double, paymentMethodId: String, correo: String): Boolean {
        return try {
            Log.d("CartRepository", "🔄 Iniciando proceso de pago con Stripe - CartID: $cartId, Amount: $amount, PaymentMethodID: $paymentMethodId, Correo: $correo")
            // Llama a tu backend y pásale el paymentMethodId
            val response = paymentService.createPaymentIntent(
                amount = (amount * 100).toInt(),
                description = "Pago de pedido #$cartId",
                paymentIntentId = paymentMethodId,
                correo = correo
            )
            Log.d("CartRepository", "✅ Respuesta del backend: $response")
            // Confirmar el pago, cerrar carrito, etc.
            closeCart(cartId)
            Log.d("CartRepository", "✅ Carrito cerrado exitosamente")
            true
        } catch (e: Exception) {
            Log.e("CartRepository", "❌ Error procesando pago con Stripe: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    suspend fun eliminarPedido(pedidoId: String): Boolean {
        return try {
            Log.d("CartRepository", "🔄 Iniciando eliminación de pedido: $pedidoId")
            val success = cartService.eliminarPedido(pedidoId)
            if (success) {
                // Eliminar también de la base de datos local
                cartItemDao.deleteCartById(pedidoId)
                Log.d("CartRepository", "✅ Pedido eliminado correctamente")
            }
            success
        } catch (e: Exception) {
            Log.e("CartRepository", "❌ Error eliminando pedido: ${e.message}")
            false
        }
    }

    suspend fun getUltimos5Pedidos(email: String): List<PedidoListadoDTO> {
        return try {
            Log.d("CartRepository", "🔄 Obteniendo últimos 5 pedidos para usuario: $email")
            val result = cartService.getUltimos5Pedidos(email)
            Log.d("CartRepository", "✅ Últimos 5 pedidos obtenidos: ${result.size}")

            // Actualizar la base de datos local con los últimos pedidos
            result.forEach { pedido ->
                val itemsMap = mutableMapOf<Long, Int>()
                for (item in pedido.items) {
                    val id = menuItemDao.getMenuItemIdByNameAndRestaurant(item.nombreComida, pedido.restaurante)
                    if (id != null) {
                        itemsMap[id] = item.cantidad
                    }
                }

                val cartEntity = CartItemEntity(
                    id = pedido.id,
                    userEmail = pedido.emailCliente,
                    items = itemsMap,
                    creationDate = Date.from(pedido.fechaCreacion.atStartOfDay().toInstant(ZoneOffset.UTC)),
                    expirationDate = Date.from(pedido.fechaExpiracion.atStartOfDay().toInstant(ZoneOffset.UTC)),
                    status = CartStatus.CLOSED,
                    total = pedido.cantidadFinal,
                    restaurantId = pedido.restaurante
                )
                cartItemDao.insertCart(cartEntity)
            }

            result
        } catch (e: Exception) {
            Log.e("CartRepository", "❌ Error obteniendo últimos 5 pedidos: ${e.message}")
            emptyList()
        }
    }
}
