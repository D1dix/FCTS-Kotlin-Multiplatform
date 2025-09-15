package com.jetbrains.greeting.data.local

import androidx.room.*
import com.jetbrains.greeting.data.entitys.CommentEntity
import com.jetbrains.greeting.data.entitys.MenuItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CommentDao {
   @Query("SELECT * FROM comments WHERE menuItemId = :itemId ORDER BY date DESC")
    fun getCommentsForItem(itemId: Long): Flow<List<CommentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: CommentEntity)

    @Query("DELETE FROM comments WHERE id = :commentId")
    suspend fun deleteComment(commentId: Long)

    @Query("SELECT AVG(rating) FROM comments WHERE menuItemId = :itemId")
    fun getAverageRating(itemId: Long): Flow<Float?>

    @Query("SELECT * FROM comments WHERE destacado = 1")
    suspend fun getDestacadosComments(): List<CommentEntity>

    @Query("DELETE FROM comments WHERE menuItemId = :itemId")
    suspend fun deleteCommentsForItem(itemId: Long)
}
