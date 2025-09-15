package com.jetbrains.greeting.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jetbrains.greeting.data.entitys.MenuItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MenuItemDao {
    @Query("SELECT * FROM menu_items")
    fun getAllMenuItems(): Flow<List<MenuItemEntity>>

    @Query("SELECT * FROM menu_items WHERE restaurant = :restaurantName")
    fun getMenuItemsByRestaurant(restaurantName: String): Flow<List<MenuItemEntity>>

    @Query("SELECT * FROM menu_items WHERE id = :itemId")
    fun getMenuItemById(itemId: Long): Flow<MenuItemEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMenuItem(item: MenuItemEntity): Long

    @Delete
    suspend fun deleteMenuItem(item: MenuItemEntity)

    @Query("UPDATE menu_items SET oculto = :oculto WHERE id = :itemId")
    suspend fun updateMenuItemVisibility(itemId: Long, oculto: Boolean)

    @Query("SELECT * FROM menu_items WHERE id = :itemId LIMIT 1")
    suspend fun getMenuItemByIdOnce(itemId: Long): MenuItemEntity?

    @Query("SELECT DISTINCT restaurant FROM menu_items")
    suspend fun getAllRestaurants(): List<String>

    @Query("SELECT id FROM menu_items WHERE name = :itemName AND restaurant = :restaurantName LIMIT 1")
    suspend fun getMenuItemIdByNameAndRestaurant(itemName: String, restaurantName: String): Long?

}