package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ChatSessionEntity::class,
        ChatMessageEntity::class,
        NorthMythEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class ManoDatabase : RoomDatabase() {
    abstract fun manoDao(): ManoDao

    companion object {
        @Volatile
        private var INSTANCE: ManoDatabase? = null

        fun getInstance(context: Context): ManoDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ManoDatabase::class.java,
                    "mano_ai_db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
