package com.example.newsreporter

import androidx.room.*

@Dao
interface DraftDao {

    @Query("SELECT * FROM drafts WHERE id = :id")
    suspend fun getDraftById(id: Int): Draft?

    @Query("SELECT * FROM drafts WHERE status = :status ORDER BY id DESC")
    suspend fun getDraftsByStatus(status: String): List<Draft>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveDraft(draft: Draft)

    @Update
    suspend fun updateDraft(draft: Draft)

    @Delete
    suspend fun deleteDraft(draft: Draft)

    @Query("DELETE FROM drafts")
    suspend fun deleteAllDrafts()
}

