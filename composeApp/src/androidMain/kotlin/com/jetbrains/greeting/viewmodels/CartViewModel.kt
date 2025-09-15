package com.jetbrains.greeting.viewmodels

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jetbrains.greeting.data.entitys.CartItemEntity
import com.jetbrains.greeting.data.entitys.CartStatus
import com.jetbrains.greeting.data.entitys.MenuItemEntity
import com.jetbrains.greeting.data.local.MenuItemDao
import com.jetbrains.greeting.data.remote.TopComidaResponse
import com.jetbrains.greeting.data.repositories.CartRepository
import com.jetbrains.greeting.data.remote.PedidoListadoDTO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.firstOrNull
import java.util.Date

@RequiresApi(Build.VERSION_CODES.O)
class CartViewModel(
    private val cartRepository: CartRepository,
    private val menuItemDao: MenuItemDao
) : ViewModel() {

    private val _activeCart = MutableStateFlow<CartItemEntity?>(null)
    val activeCart: StateFlow<CartItemEntity?> = _activeCart.asStateFlow()

    private val _userCarts = MutableStateFlow<List<CartItemEntity>>(emptyList())
    val userCarts: StateFlow<List<CartItemEntity>> = _userCarts.asStateFlow()

    private val _totalPrice = MutableStateFlow(0)
    val totalPrice: StateFlow<Int> = _totalPrice.asStateFlow()

    private val _hasActiveCart = MutableStateFlow(false)
    val hasActiveCart: StateFlow<Boolean> = _hasActiveCart.asStateFlow()

    private val _restaurants = MutableStateFlow<List<String>>(emptyList())
    val restaurants: StateFlow<List<String>> = _restaurants.asStateFlow()

    private val _topComidas = MutableStateFlow<List<TopComidaResponse>>(emptyList())
    val topComidas: StateFlow<List<TopComidaResponse>> = _topComidas.asStateFlow()

    private val _pedidosUltimos7Dias = MutableStateFlow(0)
    val pedidosUltimos7Dias: StateFlow<Int> = _pedidosUltimos7Dias.asStateFlow()

    private val _paymentStatus = MutableStateFlow<PaymentStatus?>(null)
    val paymentStatus: StateFlow<PaymentStatus?> = _paymentStatus.asStateFlow()

    private val _top5Comidas = MutableStateFlow<List<TopComidaResponse>>(emptyList())
    val top5Comidas: StateFlow<List<TopComidaResponse>> = _top5Comidas.asStateFlow()

    private val _ultimos5Pedidos = MutableStateFlow<List<PedidoListadoDTO>>(emptyList())
    val ultimos5Pedidos: StateFlow<List<PedidoListadoDTO>> = _ultimos5Pedidos.asStateFlow()

    private var syncJob: Job? = null

    init {
        startSync()
    }

    private fun startSync() {
        syncJob?.cancel()
        syncJob = viewModelScope.launch {
            try {
                while (true) {
                    syncWithBackend()
                    kotlinx.coroutines.delay(30000) // Sincronizar cada 30 segundos
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) {
                    return@launch
                }
                Log.e("CartViewModel", "Error en sincronización: ${e.message}")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        syncJob?.cancel()
    }

    private suspend fun syncWithBackend() {
        try {
            // Actualizar estadísticas
            _topComidas.value = cartRepository.cartService.getTop5Comidas()
            _pedidosUltimos7Dias.value = cartRepository.cartService.getPedidosUltimos7Dias()

            // Refrescar carrito activo desde el backend (solo leer)
            _activeCart.value?.let { cart ->
                if (cart.status == CartStatus.ACTIVE) {
                    val updatedCart = cartRepository.refreshActiveCartFromBackend(cart.userEmail)
                    _activeCart.value = updatedCart
                    _hasActiveCart.value = updatedCart != null
                    updatedCart?.let {
                        _totalPrice.value = it.total
                    }
                }
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("CartViewModel", "Error en sincronización: ${e.message}")
        }
    }

    fun loadActiveCartForUser(userEmail: String) {
        viewModelScope.launch {
            try {
                println("🔄 [CartViewModel] Cargando carrito activo para: $userEmail")
                cartRepository.getActiveCartForUser(userEmail).collect { cart ->
                    println("📦 [CartViewModel] Carrito recibido: $cart")
                    _activeCart.value = cart
                    _hasActiveCart.value = cart != null
                    cart?.let {
                        _totalPrice.value = it.total
                        println("✅ [CartViewModel] Carrito activo actualizado - ID: ${it.id}, Restaurant: ${it.restaurantId}")
                    }
                }
            } catch (e: Exception) {
                Log.e("CartViewModel", "Error cargando carrito activo: ${e.message}")
                _hasActiveCart.value = false
                _activeCart.value = null
            }
        }
    }

    fun loadUserCarts(userEmail: String, restaurantId: String? = null) {
        viewModelScope.launch {
            try {
                Log.d("CartViewModel", "🔄 Cargando carritos para usuario: $userEmail, restaurante: $restaurantId")
                cartRepository.getCartsByUserAndStatuses(
                    userEmail,
                    listOf(CartStatus.ACTIVE, CartStatus.CLOSED),
                    restaurantId
                ).collect { carts ->
                    Log.d("CartViewModel", "📦 Carritos recibidos: ${carts.size}")
                    _userCarts.value = carts
                    _hasActiveCart.value = carts.any { it.status == CartStatus.ACTIVE }
                    Log.d("CartViewModel", "✅ Estado actualizado - Tiene carrito activo: ${_hasActiveCart.value}")
                }
            } catch (e: Exception) {
                Log.e("CartViewModel", "❌ Error cargando carritos: ${e.message}")
            }
        }
    }

    fun createCart(userEmail: String, items: Map<Long, Int>, restaurantId: String? = null, orderEmail: String? = null) {
        viewModelScope.launch {
            try {
                // Verificar si ya existe un carrito activo
                if (cartRepository.hasActiveCartForRestaurant(orderEmail ?: userEmail, restaurantId ?: "")) {
                    Log.e("CartViewModel", "Ya existe un pedido activo para este restaurante")
                    return@launch
                }

                // Crear el carrito
                val cartId = cartRepository.createCart(
                    id = "",
                    userEmail = orderEmail ?: userEmail,
                    items = items,
                    restaurantId = restaurantId
                )

                if (cartId != null && cartId > 0) {
                    _hasActiveCart.value = true
                    loadActiveCartForUser(orderEmail ?: userEmail)
                }
            } catch (e: Exception) {
                Log.e("CartViewModel", "Error al crear el carrito: ${e.message}")
            }
        }
    }

    private suspend fun calculateTotal(cart: CartItemEntity): Int {
        val menuItems = cart.items.keys.associateWith { itemId ->
            menuItemDao.getMenuItemById(itemId).firstOrNull()
        }.filterValues { it != null }.mapValues { it.value!! }

        return cart.items.entries.sumOf { (itemId, quantity) ->
            val menuItem = menuItems[itemId]
            (menuItem?.price ?: 0) * quantity
        }
    }

    fun updateCart() {
        viewModelScope.launch {
            try {
                _activeCart.value?.let { cart ->
                    // Calcular el total
                    val total = calculateTotal(cart)

                    // Actualizar el carrito con el nuevo total
                    val updatedCart = cart.copy(total = total)
                    cartRepository.updateCart(updatedCart)
                    loadActiveCartForUser(cart.userEmail)
                }
            } catch (e: Exception) {
                Log.e("CartViewModel", "Error actualizando carrito: ${e.message}")
            }
        }
    }

    fun addItemToCart(itemId: Long, quantity: Int = 1) {
        viewModelScope.launch {
            try {
                _activeCart.value?.let { cart ->
                    val updatedItems = cart.items.toMutableMap()
                    updatedItems[itemId] = (updatedItems[itemId] ?: 0) + quantity
                    val updatedCart = cart.copy(items = updatedItems)
                    val total = calculateTotal(updatedCart)
                    cartRepository.updateCart(updatedCart.copy(total = total))
                    loadActiveCartForUser(cart.userEmail)
                }
            } catch (e: Exception) {
                Log.e("CartViewModel", "Error añadiendo item: ${e.message}")
            }
        }
    }

    fun restarItemDelCarrito(itemId: Long) {
        viewModelScope.launch {
            try {
                _activeCart.value?.let { cart ->
                    val updatedItems = cart.items.toMutableMap()
                    val currentQuantity = updatedItems[itemId] ?: 0

                    if (currentQuantity > 1) {
                        updatedItems[itemId] = currentQuantity - 1
                    } else {
                        updatedItems.remove(itemId)
                    }

                    val updatedCart = cart.copy(items = updatedItems)
                    val total = calculateTotal(updatedCart)
                    cartRepository.updateCart(updatedCart.copy(total = total))
                    loadActiveCartForUser(cart.userEmail)
                }
            } catch (e: Exception) {
                Log.e("CartViewModel", "Error al restar item: ${e.message}")
            }
        }
    }

    fun eliminarItemDelCarrito(itemId: Long) {
        viewModelScope.launch {
            try {
                _activeCart.value?.let { cart ->
                    val updatedItems = cart.items.toMutableMap()
                    updatedItems.remove(itemId)

                    val updatedCart = cart.copy(items = updatedItems)
                    val total = calculateTotal(updatedCart)
                    cartRepository.updateCart(updatedCart.copy(total = total))
                    loadActiveCartForUser(cart.userEmail)
                }
            } catch (e: Exception) {
                Log.e("CartViewModel", "Error al eliminar item: ${e.message}")
            }
        }
    }

    fun updateItemQuantity(itemId: Long, quantity: Int) {
        viewModelScope.launch {
            try {
                _activeCart.value?.let { cart ->
                    val updatedItems = cart.items.toMutableMap()
                    if (quantity > 0) {
                        updatedItems[itemId] = quantity
                    } else {
                        updatedItems.remove(itemId)
                    }
                    val updatedCart = cart.copy(items = updatedItems)
                    val total = calculateTotal(updatedCart)
                    cartRepository.updateCart(updatedCart.copy(total = total))
                    loadActiveCartForUser(cart.userEmail)
                }
            } catch (e: Exception) {
                Log.e("CartViewModel", "Error actualizando cantidad: ${e.message}")
            }
        }
    }

    fun closeCart(cartId: String) {
        viewModelScope.launch {
            try {
                cartRepository.closeCart(cartId)
                _hasActiveCart.value = false
                _activeCart.value = null
                _totalPrice.value = 0

                // Actualizar la lista de carritos después de cerrar
                _activeCart.value?.userEmail?.let { email ->
                    loadUserCarts(email)
                }

                // Forzar una actualización del estado
                _userCarts.value = _userCarts.value.filter { it.id != cartId }
            } catch (e: Exception) {
                Log.e("CartViewModel", "Error al cerrar el carrito: ${e.message}")
            }
        }
    }

    fun checkAndExpireCarts() {
        viewModelScope.launch {
            try {
                cartRepository.checkAndExpireCarts()
            } catch (e: Exception) {
                Log.e("CartViewModel", "Error expirando carritos: ${e.message}")
            }
        }
    }

    fun loadRestaurants(userEmail: String) {
        viewModelScope.launch {
            try {
                cartRepository.cartService.listarPedidos()
                    .map { it.emailCliente }
                    .distinct()
                    .let { emails ->
                        _restaurants.value = emails
                    }
            } catch (e: Exception) {
                Log.e("CartViewModel", "Error cargando restaurantes: ${e.message}")
            }
        }
    }

    fun cancelPayment(paymentIntentId: String) {
        viewModelScope.launch {
            try {
                cartRepository.cancelPayment(paymentIntentId)
                _paymentStatus.value = null
            } catch (e: Exception) {
                Log.e("CartViewModel", "Error cancelando pago: ${e.message}")
            }
        }
    }

    fun resetPaymentStatus() {
        _paymentStatus.value = null
    }

    fun loadTop5ComidasGlobal() {
        viewModelScope.launch {
            try {
                Log.d("CartViewModel", "🔄 Iniciando carga de Top 5 comidas global")
                val result = cartRepository.getTop5ComidasGlobal()
                Log.d("CartViewModel", "✅ Top 5 comidas global recibido: ${result.size} items")
                result.forEach { comida ->
                    Log.d("CartViewModel", "🍽️ Comida: ${comida.nombre}, Cantidad: ${comida.cantidad}, Restaurante: ${comida.restaurante}")
                }
                _top5Comidas.value = result
            } catch (e: Exception) {
                Log.e("CartViewModel", "❌ Error cargando Top 5 comidas global: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    fun loadTop5ComidasPorRestaurante(restaurante: String) {
        viewModelScope.launch {
            try {
                Log.d("CartViewModel", "🔄 Iniciando carga de Top 5 comidas para restaurante: $restaurante")
                val result = cartRepository.getTop5ComidasPorRestaurante(restaurante)
                Log.d("CartViewModel", "✅ Top 5 comidas recibido para restaurante $restaurante: ${result.size} items")
                result.forEach { comida ->
                    Log.d("CartViewModel", "🍽️ Comida: ${comida.nombre}, Cantidad: ${comida.cantidad}, Restaurante: ${comida.restaurante}")
                }
                _top5Comidas.value = result
            } catch (e: Exception) {
                Log.e("CartViewModel", "❌ Error cargando Top 5 comidas para restaurante $restaurante: ${e.message}")
                e.printStackTrace()
                _top5Comidas.value = emptyList()
            }
        }
    }

    fun processStripePayment(cartId: String, amount: Double, paymentMethodId: String, correo: String) {
        viewModelScope.launch {
            try {
                Log.d("CartViewModel", "🔄 Iniciando procesamiento de pago - CartID: $cartId, Amount: $amount, PaymentMethodID: $paymentMethodId, Correo: $correo")
                _paymentStatus.value = PaymentStatus.LOADING
                val success = cartRepository.processStripePayment(cartId, amount, paymentMethodId, correo)
                if (success) {
                    Log.d("CartViewModel", "✅ Pago procesado exitosamente")
                    // Cerrar el pedido en el backend
                    Log.d("CartViewModel", "🔄 Cerrando carrito en el backend")
                    cartRepository.closeCart(cartId)
                    _paymentStatus.value = PaymentStatus.SUCCESS
                    _hasActiveCart.value = false
                    _activeCart.value = null
                    _totalPrice.value = 0
                    Log.d("CartViewModel", "✅ Carrito cerrado y estado actualizado")
                } else {
                    Log.e("CartViewModel", "❌ Error en el procesamiento del pago")
                    _paymentStatus.value = PaymentStatus.ERROR
                }
            } catch (e: Exception) {
                Log.e("CartViewModel", "❌ Error procesando pago con Stripe: ${e.message}")
                e.printStackTrace()
                _paymentStatus.value = PaymentStatus.ERROR
            }
        }
    }

    fun eliminarPedido(pedidoId: String) {
        viewModelScope.launch {
            try {
                Log.d("CartViewModel", "🔄 Iniciando eliminación de pedido: $pedidoId")
                val success = cartRepository.eliminarPedido(pedidoId)
                if (success) {
                    // Actualizar la lista de carritos
                    _userCarts.value = _userCarts.value.filter { it.id != pedidoId }
                    Log.d("CartViewModel", "✅ Pedido eliminado y lista actualizada")
                } else {
                    Log.e("CartViewModel", "❌ Error al eliminar pedido")
                }
            } catch (e: Exception) {
                Log.e("CartViewModel", "❌ Error eliminando pedido: ${e.message}")
            }
        }
    }

    suspend fun getUltimos5Pedidos(email: String): List<PedidoListadoDTO> {
        try {
            Log.d("CartViewModel", "🔄 Obteniendo últimos 5 pedidos para usuario: $email")
            val result = cartRepository.getUltimos5Pedidos(email)
            Log.d("CartViewModel", "✅ Últimos 5 pedidos obtenidos: ${result.size}")
            _ultimos5Pedidos.value = result
            return result
        } catch (e: Exception) {
            Log.e("CartViewModel", "❌ Error obteniendo últimos 5 pedidos: ${e.message}")
            _ultimos5Pedidos.value = emptyList()
            return emptyList()
        }
    }
}

enum class PaymentStatus {
    LOADING,
    SUCCESS,
    ERROR
}
