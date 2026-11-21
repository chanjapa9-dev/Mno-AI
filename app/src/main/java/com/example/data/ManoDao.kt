package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ManoDao {

    @Query("SELECT * FROM chat_sessions ORDER BY updatedAt DESC")
    fun observeSessions(): Flow<List<ChatSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ChatSessionEntity): Long

    @Query("UPDATE chat_sessions SET title = :title, lastPreview = :preview, updatedAt = :updatedAt WHERE id = :sessionId")
    suspend fun updateSessionMeta(sessionId: Long, title: String, preview: String, updatedAt: Long)

    @Query("UPDATE chat_sessions SET lastPreview = :preview, updatedAt = :updatedAt WHERE id = :sessionId")
    suspend fun updateSessionPreview(sessionId: Long, preview: String, updatedAt: Long)

    @Query("DELETE FROM chat_sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: Long)

    @Query("DELETE FROM chat_sessions")
    suspend fun clearAllSessions()

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC, id ASC")
    fun observeMessages(sessionId: Long): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC, id ASC")
    suspend fun getMessagesSnapshot(sessionId: Long): List<ChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity): Long

    @Query("SELECT * FROM north_myths ORDER BY createdAt DESC")
    fun observeMyths(): Flow<List<NorthMythEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMyth(myth: NorthMythEntity): Long

    @Query("UPDATE north_myths SET imagePath = :imagePath WHERE id = :mythId")
    suspend fun updateMythImage(mythId: Long, imagePath: String)

    @Query("DELETE FROM north_myths WHERE id = :mythId")
    suspend fun deleteMyth(mythId: Long)
}
