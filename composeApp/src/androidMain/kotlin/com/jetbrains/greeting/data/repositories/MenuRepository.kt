package com.jetbrains.greeting.data.repositories

import com.jetbrains.greeting.data.entitys.*
import com.jetbrains.greeting.data.local.MenuItemDao
import com.jetbrains.greeting.data.remote.MenuService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class MenuRepository(
    private val dao: MenuItemDao,
    private val menuService: MenuService
) {
    fun getAllMenuItems(): Flow<List<MenuItemEntity>> = dao.getAllMenuItems()
    
    fun getMenuItemsByRestaurant(restaurantName: String): Flow<List<MenuItemEntity>> = 
        dao.getMenuItemsByRestaurant(restaurantName)
    
    fun getMenuItemById(itemId: Long): Flow<MenuItemEntity?> = dao.getMenuItemById(itemId)

    suspend fun insertMenuItem(item: MenuItemEntity) {
        println("📝 [MenuRepository] Intentando insertar item: ${item.name} en restaurante ${item.restaurant}")
        val localItems = dao.getMenuItemsByRestaurant(item.restaurant).first()
        val exists = localItems.any { it.name == item.name }

        if (exists) {
            println("⚠️ [MenuRepository] El item ya existe localmente: ${item.name} en restaurante ${item.restaurant}, no se insertará.")
            return
        }
        val comida = item.toComida()
        if (!menuService.createMenuItem(comida)) {
            println("❌ [MenuRepository] Error al crear item en backend: ${item.name}")
            throw Exception("Error al crear el item en el backend")
        }
        println("✅ [MenuRepository] Item creado exitosamente en backend: ${item.name}")
        dao.insertMenuItem(item)
        println("💾 [MenuRepository] Item guardado en base de datos local: ${item.name}")
    }


    suspend fun deleteMenuItem(itemId: Long) {
        println("🗑️ [MenuRepository] Intentando eliminar item con ID: $itemId")
        val item = dao.getMenuItemByIdOnce(itemId)
        item?.let {
            println("🔍 [MenuRepository] Item encontrado para eliminar: ${it.name}")
            if (!menuService.deleteMenuItem(it.name, it.restaurant)) {
                println("❌ [MenuRepository] Error al eliminar item en backend: ${it.name}")
                throw Exception("Error al eliminar el item en el backend")
            }
            println("✅ [MenuRepository] Item eliminado exitosamente en backend: ${it.name}")
            dao.deleteMenuItem(it)
            println("💾 [MenuRepository] Item eliminado de base de datos local: ${it.name}")
        }
    }
    
    suspend fun getMenuItemByIdOnce(id: Long): MenuItemEntity? {
        println("🗑️ [MenuRepository] Intentando Mirar item con ID: $id")
        return dao.getMenuItemByIdOnce(id)
    }
    
    suspend fun getAllRestaurants(): List<String> {
        println("🏪 [MenuRepository] Obteniendo lista de restaurantes del backend")
        val restaurants = menuService.getAllRestaurants()
        println("✅ [MenuRepository] Restaurantes obtenidos: ${restaurants.joinToString()}")
        return restaurants
    }

    suspend fun syncWithBackend() {
        println("🔄 [MenuRepository] Iniciando sincronización con backend")
        try {
            val backendItems = menuService.getAllMenuItems()
            println("📦 [MenuRepository] Items obtenidos del backend: ${backendItems.size}")
            
            val localItems = dao.getAllMenuItems().first()
            println("💾 [MenuRepository] Items en base de datos local: ${localItems.size}")
            
            // Convertir items del backend a MenuItemEntity usando la función de extensión
            val backendEntities = backendItems.map { it.toMenuItemEntity() }
            
            // Eliminar items locales que no están en el backend
            localItems.forEach { localItem ->
                if (!backendEntities.any { it.name == localItem.name && it.restaurant == localItem.restaurant }) {
                    println("🗑️ [MenuRepository] Eliminando item local que no existe en backend: ${localItem.name}")
                    localItem.id?.let { dao.deleteMenuItem(localItem) }
                }
            }
            
            // Insertar o actualizar items del backend
            backendEntities.forEach { entity ->
                println("💾 [MenuRepository] Guardando/actualizando item en local: ${entity.name}")
                // Buscar si ya existe el item localmente
                val existingItem = localItems.find { it.name == entity.name && it.restaurant == entity.restaurant }
                if (existingItem != null) {
                    // Si existe, actualizar manteniendo el ID local y el estado de oculto
                    dao.insertMenuItem(entity.copy(
                        id = existingItem.id,
                        oculto = entity.oculto
                    ))
                } else {
                    // Si no existe, insertar nuevo
                    dao.insertMenuItem(entity)
                }
            }
            
            println("✅ [MenuRepository] Sincronización completada exitosamente")
        } catch (e: Exception) {
            println("❌ [MenuRepository] Error en sincronización con backend: ${e.message}")
            e.printStackTrace()
        }
    }

    suspend fun subirFotoComida(
        imageBytes: ByteArray,
        comida: String,
        restaurante: String
    ): String? {
        println("📸 [MenuRepository] Subiendo foto para comida: $comida en restaurante: $restaurante")
        return try {
            val url = menuService.subirFotoComida(imageBytes, comida, restaurante)
            if (url != null) {
                println("✅ [MenuRepository] Foto subida exitosamente: $url")
                url
            } else {
                println("❌ [MenuRepository] Error al subir foto: URL nula")
                null
            }
        } catch (e: Exception) {
            println("❌ [MenuRepository] Error al subir foto: ${e.message}")
            null
        }
    }

    suspend fun updateMenuItemVisibility(itemId: Long, oculto: Boolean) {
        try {
            println("🔄 [MenuRepository] Actualizando visibilidad del item $itemId")
            val item = dao.getMenuItemByIdOnce(itemId)
            item?.let {
                println("📝 [MenuRepository] Item encontrado: ${it.name}, Estado actual oculto: ${it.oculto}")
                val comidaPK = ComidaPK(it.name, it.restaurant)
                // Actualizar en el backend
                menuService.toggleMenuItemVisibility(comidaPK)
                // Actualizar en la base de datos local
                dao.updateMenuItemVisibility(itemId, oculto)
                println("✅ [MenuRepository] Visibilidad actualizada correctamente")
            } ?: throw Exception("Item no encontrado")
        } catch (e: Exception) {
            println("❌ [MenuRepository] Error al actualizar visibilidad: ${e.message}")
            throw e
        }
    }
} 