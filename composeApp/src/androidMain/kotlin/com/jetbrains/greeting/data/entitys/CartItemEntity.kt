package com.jetbrains.greeting.data.entitys

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.jetbrains.greeting.data.local.Converters
import java.util.Date

enum class CartStatus {
    ACTIVE,
    CLOSED,
    EXPIRED
}

@Entity(
    tableName = "cart_items",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["email"],
            childColumns = ["userEmail"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
@TypeConverters(Converters::class)
data class CartItemEntity(
    @PrimaryKey(autoGenerate = false)
    val id: String,
    val userEmail: String,
    val items: Map<Long, Int>, // Map<menuItemId, quantity>
    val creationDate: Date,
    val expirationDate: Date,
    val status: CartStatus = CartStatus.ACTIVE,
    val total: Int,
    val restaurantId: String? = null // ID del restaurante asociado al carrito
)

data class OrderSummaryDto(
    val userEmail: String,
    val restaurantId: String,
    val items: List<OrderItemDetail>,
    val total: Double
)

data class OrderItemDetail(
    val name: String,
    val quantity: Int,
    val pricePerUnit: Double,
    val totalPrice: Double
)
