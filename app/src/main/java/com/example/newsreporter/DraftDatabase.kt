package com.example.newsreporter

import android.content.Context
import android.database.Cursor
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Draft::class], version = 4)
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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create a new table with the updated schema
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS new_drafts (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        content TEXT NOT NULL DEFAULT '[]',
                        status TEXT NOT NULL
                    )
                """)

                // Copy the data from the old table to the new table
                database.execSQL("""
                    INSERT INTO new_drafts (id, title, content, status)
                    SELECT id, 
                           COALESCE(title, ''), 
                           CASE 
                               WHEN body IS NULL OR body = '' 
                               THEN '[]'
                               ELSE '[{"type":"PARAGRAPH","content":"' || replace(body, '"', '\\"') || '"}]'
                           END,
                           COALESCE(status, 'Draft')
                    FROM drafts
                """)

                // Remove the old table
                database.execSQL("DROP TABLE IF EXISTS drafts")

                // Rename the new table to the correct name
                database.execSQL("ALTER TABLE new_drafts RENAME TO drafts")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Simple update to ensure content is not null and has a default value
                database.execSQL("""
                    UPDATE drafts 
                    SET content = CASE 
                        WHEN content IS NULL OR trim(content) = '' 
                        THEN '[]'
                        WHEN substr(content, 1, 1) != '[' 
                        THEN '[{"type":"PARAGRAPH","content":"' || replace(content, '"', '\\"') || '"}]'
                        ELSE content 
                    END
                """)
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Check if the timestamp column exists
                val cursor = database.query("PRAGMA table_info(drafts)")
                var hasTimestamp = false
                cursor.use { c ->
                    val nameIndex = c.getColumnIndexOrThrow("name")
                    while (c.moveToNext()) {
                        val columnName = c.getString(nameIndex)
                        if (columnName == "timestamp") {
                            hasTimestamp = true
                            break
                        }
                    }
                }

                if (hasTimestamp) {
                    // Create a new table without the timestamp column
                    database.execSQL("""
                        CREATE TABLE new_drafts (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            title TEXT NOT NULL,
                            content TEXT NOT NULL,
                            status TEXT NOT NULL
                        )
                    """)

                    // Copy data from the old table to the new one, excluding the timestamp
                    database.execSQL("""
                        INSERT INTO new_drafts (id, title, content, status)
                        SELECT id, title, content, status FROM drafts
                    """)

                    // Drop the old table
                    database.execSQL("DROP TABLE drafts")

                    // Rename the new table
                    database.execSQL("ALTER TABLE new_drafts RENAME TO drafts")
                }
            }
        }
    }
}

