// app/src/main/java/com/example/data/database/AppDatabase.kt
package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.settings.ApiKeyConfig

@Database(
    entities = [
        ChatSession::class,
        ChatMessage::class,
        ApiKeyConfig::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun chatDao(): ChatDao
    abstract fun apiKeyDao(): ApiKeyDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS api_keys (
                        serviceName TEXT NOT NULL PRIMARY KEY,
                        apiKey TEXT NOT NULL DEFAULT '',
                        isEnabled INTEGER NOT NULL DEFAULT 1,
                        lastValidated INTEGER,
                        isValid INTEGER NOT NULL DEFAULT 0,
                        displayName TEXT NOT NULL DEFAULT '',
                        maskedKey TEXT NOT NULL DEFAULT ''
                    )
                """.trimIndent())
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add new columns to chat_sessions
                database.execSQL("ALTER TABLE chat_sessions ADD COLUMN personalityId TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE chat_sessions ADD COLUMN personalityName TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE chat_sessions ADD COLUMN contextSnapshotJson TEXT")

                // Add new columns to chat_messages
                database.execSQL("ALTER TABLE chat_messages ADD COLUMN attachedFilesJson TEXT")
                database.execSQL("ALTER TABLE chat_messages ADD COLUMN modelUsed TEXT")
                database.execSQL("ALTER TABLE chat_messages ADD COLUMN personalityUsed TEXT")
                database.execSQL("ALTER TABLE chat_messages ADD COLUMN generationConfigJson TEXT")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "troc_agent_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }

        fun destroyInstance() {
            INSTANCE = null
        }
    }
}
