package com.jetbrains.greeting.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.jetbrains.greeting.data.entitys.CartItemEntity
import com.jetbrains.greeting.data.entitys.CommentEntity
import com.jetbrains.greeting.data.entitys.MenuItemEntity
import com.jetbrains.greeting.data.entitys.UserEntity

@Database(
    entities = [
        UserEntity::class,
        MenuItemEntity::class,
        CommentEntity::class,
        CartItemEntity::class
    ],
    version = 8
    ,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun menuItemDao(): MenuItemDao
    abstract fun commentDao(): CommentDao
    abstract fun cartItemDao(): CartItemDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
} 