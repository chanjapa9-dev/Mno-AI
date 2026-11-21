package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "chat_sessions")
data class ChatSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val lastPreview: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "chat_messages",
    foreignKeys = [
        ForeignKey(
            entity = ChatSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["sessionId"])]
)
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val isUser: Boolean,
    val text: String,
    val imagePath: String? = null,
    val isImageGeneration: Boolean = false,
    val isError: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "north_myths")
data class NorthMythEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val entityName: String,
    val setting: String,
    val story: String,
    val catchphrase: String,
    val imagePath: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
