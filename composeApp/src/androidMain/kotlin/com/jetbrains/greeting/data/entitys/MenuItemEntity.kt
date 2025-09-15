package com.jetbrains.greeting.data.entitys

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.jetbrains.greeting.data.local.Converters
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Entity(tableName = "menu_items", indices = [Index(value = ["name", "restaurant"], unique = true)])
@TypeConverters(Converters::class)
data class MenuItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long? = null,
    val name: String,
    val description: String,
    val price: Int,
    val restaurant: String,
    var imageUrl: String? = null,
    val category: String,
    val rating: Int? = 0,
    val features: List<String>,
    val preparationTime: Int,
    val isVegetarian: Boolean,
    val isSpicy: Boolean,
    val isGlutenFree: Boolean,
    val isDairyFree: Boolean,
    val isNutFree: Boolean,
    val oculto: Boolean = false
)

@Serializable
data class MenuItemDto(
    val name: String,
    val description: String,
    val price: Int,
    val imageUrl: String,
    val category: String,
    val features: List<String>,
    val preparationTime: Int,
    val isVegetarian: Boolean,
    val isSpicy: Boolean,
    val restaurant: String
)

// DTOs para la comunicación con el backend
@Serializable
data class ComidaPK(
    @SerialName("ncomida")
    val nComida: String,
    @SerialName("nrestaurante")
    val nRestaurante: String
)

@Serializable
data class Comida(
    val comidaPK: ComidaPK,
    val description: String,
    val price: Int,
    val category: String,
    val attributes: List<String>,
    val features: List<String>,
    val preparationTime: Int,
    val foto: Foto?,
    val ocultar: Boolean = false
)

@Serializable
data class Foto(
    @SerialName("imagenUrl")
    val url: String,
    val fecha: String? = null
)

fun Comida.toMenuItemEntity(): MenuItemEntity {
    println("🔄 [MenuItemEntity] Convirtiendo Comida a MenuItemEntity: ${this.comidaPK.nComida}")
    return MenuItemEntity(
        id = null,
        name = this.comidaPK.nComida,
        description = this.description,
        price = this.price,
        restaurant = this.comidaPK.nRestaurante,
        imageUrl = this.foto?.url ?: "",
        category = this.category,
        features = this.features,
        preparationTime = this.preparationTime,
        isVegetarian = this.attributes.contains("VEGETARIAN"),
        isSpicy = this.attributes.contains("SPICY"),
        isGlutenFree = this.attributes.contains("GLUTEN_FREE"),
        isDairyFree = this.attributes.contains("DAIRY_FREE"),
        isNutFree = this.attributes.contains("NUT_FREE"),
        oculto = this.ocultar
    ).also {
        println("✅ [MenuItemEntity] Conversión completada: ${it.name}")
    }
}

// Función de extensión para convertir MenuItemEntity a Comida
fun MenuItemEntity.toComida(): Comida {
    println("🔄 [MenuItemEntity] Convirtiendo MenuItemEntity a Comida: ${this.name}")
    val attributes = mutableListOf<String>().apply {
        if (isVegetarian) add("VEGETARIAN")
        if (isSpicy) add("SPICY")
        if (isGlutenFree) add("GLUTEN_FREE")
        if (isDairyFree) add("DAIRY_FREE")
        if (isNutFree) add("NUT_FREE")
    }
    
    return Comida(
        comidaPK = ComidaPK(
            nComida = this.name,
            nRestaurante = this.restaurant
        ),
        description = this.description,
        price = this.price,
        category = this.category,
        attributes = attributes,
        features = this.features,
        preparationTime = this.preparationTime,
        foto = this.imageUrl?.let { Foto(it) },
        ocultar = this.oculto
    ).also {
        println("✅ [MenuItemEntity] Conversión completada: ${it.comidaPK.nComida}")
    }
}
