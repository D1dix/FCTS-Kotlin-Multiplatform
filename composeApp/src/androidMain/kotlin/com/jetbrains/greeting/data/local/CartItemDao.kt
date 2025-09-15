package com.jetbrains.greeting.data.local

import androidx.room.*
import com.jetbrains.greeting.data.entitys.CartItemEntity
import com.jetbrains.greeting.data.entitys.CartStatus
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface CartItemDao {
    @Query("SELECT * FROM cart_items WHERE userEmail = :userEmail AND status = :status")
    fun getCartByUserAndStatus(userEmail: String, status: CartStatus): Flow<CartItemEntity?>

    @Query("SELECT * FROM cart_items WHERE userEmail = :userEmail AND status IN (:statuses)")
    fun getCartsByUserAndStatuses(userEmail: String, statuses: List<CartStatus>): Flow<List<CartItemEntity>>

    @Query("SELECT * FROM cart_items WHERE status IN (:statuses) AND (:restaurantId IS NULL OR restaurantId = :restaurantId) ORDER BY creationDate DESC")
    fun getCartsByStatusesAndRestaurant(statuses: List<CartStatus>, restaurantId: String?): Flow<List<CartItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCart(cart: CartItemEntity): Long

    @Query("SELECT * FROM cart_items WHERE userEmail = :email AND status = :status LIMIT 1")
    suspend fun getCartByUserAndStatusOnce(email: String, status: CartStatus): CartItemEntity?

    @Query("UPDATE cart_items SET status = :newStatus WHERE id = :cartId")
    suspend fun updateCartStatus(cartId: String, newStatus: CartStatus)

    @Query("SELECT * FROM cart_items WHERE id = :cartId")
    suspend fun getCartById(cartId: String): CartItemEntity?

    @Query("SELECT DISTINCT restaurantId FROM cart_items WHERE userEmail = :userEmail AND status = 'ACTIVE' AND restaurantId IS NOT NULL")
    fun getRestaurantsFromActiveCart(userEmail: String): Flow<List<String>>

    @Query("SELECT * FROM cart_items WHERE expirationDate < :currentDate AND status = 'ACTIVE'")
    suspend fun getExpiredCarts(currentDate: Date): List<CartItemEntity>

    @Query("UPDATE cart_items SET status = :newStatus WHERE id IN (:cartIds)")
    suspend fun updateCartsStatus(cartIds: List<String>, newStatus: CartStatus)

    @Query("DELETE FROM cart_items WHERE (status = 'EXPIRED' OR status = 'CLOSED') AND expirationDate < :currentDate")
    suspend fun deleteExpiredCarts(currentDate: Date)

    @Query("UPDATE cart_items SET status = 'EXPIRED' WHERE status = 'ACTIVE' AND expirationDate < :currentDate")
    suspend fun expireActiveCarts(currentDate: Date)

    @Query("SELECT COUNT(*) FROM cart_items WHERE userEmail = :userEmail AND restaurantId = :restaurantId AND status = 'ACTIVE'")
    suspend fun hasActiveCartForRestaurant(userEmail: String, restaurantId: String): Boolean

    @Query("DELETE FROM cart_items WHERE id = :cartId")
    suspend fun deleteCartById(cartId: String)

    @Query("SELECT * FROM cart_items WHERE userEmail = :userEmail AND status = 'CLOSED' ORDER BY creationDate DESC LIMIT 5")
    fun getUltimos5Pedidos(userEmail: String): Flow<List<CartItemEntity>>

    @Query("SELECT * FROM cart_items WHERE userEmail = :userEmail AND status = 'CLOSED' ORDER BY creationDate DESC LIMIT 5")
    suspend fun getUltimos5PedidosOnce(userEmail: String): List<CartItemEntity>
}