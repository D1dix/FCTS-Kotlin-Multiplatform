package com.jetbrains.greeting.data.local

import androidx.room.*
import com.jetbrains.greeting.data.entitys.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Query("SELECT * FROM users WHERE email = :email AND contrasenia = :contrasenia")
    suspend fun login(email: String, contrasenia: String): UserEntity?

    @Query("SELECT * FROM users WHERE email = :email")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE isCurrentUser = 1")
    fun getCurrentUser(): Flow<UserEntity?>

    @Insert
    suspend fun insertUser(user: UserEntity): Long

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("SELECT * FROM users WHERE email = :email")
    fun getUserById(email: String): Flow<UserEntity?>

    @Query("DELETE FROM users WHERE email = :email")
    suspend fun deleteUser(email: String)

    @Query("UPDATE users SET isCurrentUser = 0")
    suspend fun clearCurrentUser()

    @Update
    suspend fun setCurrentUser(user: UserEntity)
}
