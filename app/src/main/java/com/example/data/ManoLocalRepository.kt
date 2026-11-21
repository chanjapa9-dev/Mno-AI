package com.example.data

import kotlinx.coroutines.flow.Flow

class ManoLocalRepository(private val dao: ManoDao) {

    fun observeSessions(): Flow<List<ChatSessionEntity>> = dao.observeSessions()

    fun observeMessages(sessionId: Long): Flow<List<ChatMessageEntity>> = dao.observeMessages(sessionId)

    fun observeMyths(): Flow<List<NorthMythEntity>> = dao.observeMyths()

    suspend fun createSession(title: String, preview: String): Long {
        return dao.insertSession(
            ChatSessionEntity(
                title = title,
                lastPreview = preview,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun updateSessionPreview(sessionId: Long, preview: String) {
        dao.updateSessionPreview(
            sessionId = sessionId,
            preview = preview,
            updatedAt = System.currentTimeMillis()
        )
    }

    suspend fun deleteSession(sessionId: Long) = dao.deleteSession(sessionId)

    suspend fun clearAllSessions() = dao.clearAllSessions()

    suspend fun getMessagesSnapshot(sessionId: Long): List<ChatMessageEntity> =
        dao.getMessagesSnapshot(sessionId)

    suspend fun addMessage(message: ChatMessageEntity): Long = dao.insertMessage(message)

    suspend fun saveMyth(myth: NorthMythEntity): Long = dao.insertMyth(myth)

    suspend fun updateMythImage(mythId: Long, imagePath: String) =
        dao.updateMythImage(mythId, imagePath)

    suspend fun deleteMyth(mythId: Long) = dao.deleteMyth(mythId)
}
