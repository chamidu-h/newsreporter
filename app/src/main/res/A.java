package com.example.newsreporter

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Draft::class], version = 2)
@TypeConverters(ArticleElementConverter::class)
abstract class DraftDatabase : RoomDatabase() {
    abstract fun draftDao(): DraftDao

    companion object {
        @Volatile
        private var INSTANCE: DraftDatabase? = null

        fun getDatabase(context: Context): DraftDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DraftDatabase::class.java,
                    "draft_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Create a new table with the updated schema
        database.execSQL("""
            CREATE TABLE new_drafts (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                title TEXT NOT NULL,
                content TEXT NOT NULL,
                status TEXT NOT NULL,
                timestamp INTEGER NOT NULL
            )
        """)

        // Copy the data from the old table to the new table
        // Convert the 'body' column to a JSON array with a single paragraph element
        database.execSQL("""
            INSERT INTO new_drafts (id, title, content, status, timestamp)
            SELECT id, title, 
                   '[{"type":"PARAGRAPH","content":"' || body || '"}]', 
                   status, timestamp 
            FROM drafts
        """)

        // Remove the old table
        database.execSQL("DROP TABLE drafts")

        // Rename the new table to the correct name
        database.execSQL("ALTER TABLE new_drafts RENAME TO drafts")
    }
}