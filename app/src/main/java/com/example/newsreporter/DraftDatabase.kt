package com.example.newsreporter

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase


@Database(entities = [Draft::class], version = 6)
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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS new_drafts (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        content TEXT NOT NULL DEFAULT '[]',
                        status TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    """
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
                    """.trimIndent()
                )
                database.execSQL("DROP TABLE IF EXISTS drafts")
                database.execSQL("ALTER TABLE new_drafts RENAME TO drafts")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    UPDATE drafts 
                    SET content = CASE 
                        WHEN content IS NULL OR trim(content) = '' 
                        THEN '[]'
                        WHEN substr(content, 1, 1) != '[' 
                        THEN '[{"type":"PARAGRAPH","content":"' || replace(content, '"', '\\"') || '"}]'
                        ELSE content 
                    END
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
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
                    database.execSQL(
                        """
                        CREATE TABLE new_drafts (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            title TEXT NOT NULL,
                            content TEXT NOT NULL,
                            status TEXT NOT NULL
                        )
                        """.trimIndent()
                    )
                    database.execSQL(
                        """
                        INSERT INTO new_drafts (id, title, content, status)
                        SELECT id, title, content, status FROM drafts
                        """.trimIndent()
                    )
                    database.execSQL("DROP TABLE drafts")
                    database.execSQL("ALTER TABLE new_drafts RENAME TO drafts")
                }
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE drafts ADD COLUMN lastModified INTEGER NOT NULL DEFAULT 0")
                database.execSQL("UPDATE drafts SET lastModified = (strftime('%s','now') * 1000) WHERE lastModified = 0")
            }
        }

        // New migration: version 5 -> 6 adds the 'category' column
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE drafts ADD COLUMN category TEXT NOT NULL DEFAULT 'General'")
            }
        }
    }
}

