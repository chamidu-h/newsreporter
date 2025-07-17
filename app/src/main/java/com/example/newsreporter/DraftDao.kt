package com.example.newsreporter

import androidx.room.*

@Dao
interface DraftDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveDraft(draft: Draft)

    @Query("SELECT * FROM drafts WHERE id = :id")
    suspend fun getDraftById(id: Int): Draft?

    @Query("SELECT * FROM drafts WHERE status = :status ORDER BY lastModified DESC")
    fun getDraftsByStatus(status: String): List<Draft>

    @Query("SELECT * FROM drafts WHERE status = :status AND category = :category")
    suspend fun getDraftsByStatusAndCategory(status: String, category: String): List<Draft>


    @Update
    suspend fun updateDraft(draft: Draft)

    @Delete
    suspend fun deleteDraft(draft: Draft)

    @Query("DELETE FROM drafts WHERE id = :draftId")
    suspend fun deleteDraftById(draftId: Int)

    @Query("DELETE FROM drafts")
    suspend fun deleteAllDrafts()
}

