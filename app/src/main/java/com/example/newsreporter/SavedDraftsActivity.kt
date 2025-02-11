package com.example.newsreporter

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SavedDraftsActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: DraftAdapter
    private lateinit var database: DraftDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_saved_drafts)

        recyclerView = findViewById(R.id.rv_saved_drafts)
        database = DraftDatabase.getDatabase(this)

        setupRecyclerView()
        setupDeleteAllButton()

        loadDrafts()
    }

    private fun setupRecyclerView() {
        adapter = DraftAdapter(
            drafts = mutableListOf(),
            onDraftClick = { draft ->
                navigateToDraftArticle(draft)
            },
            onDeleteClick = { draft ->
                deleteDraft(draft)
            }
        )
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun setupDeleteAllButton() {
        val deleteAllButton: View = findViewById(R.id.btn_delete_all_drafts)
        deleteAllButton.setOnClickListener {
            deleteAllDrafts()
        }
    }

    private fun navigateToDraftArticle(draft: Draft) {
        val intent = Intent(this, DraftArticleActivity::class.java)
        intent.putExtra("DRAFT_ID", draft.id)
        startActivity(intent)
    }

    private fun loadDrafts() {
        lifecycleScope.launch {
            try {
                val drafts = withContext(Dispatchers.IO) {
                    database.draftDao().getDraftsByStatus("Draft")
                }
                adapter.updateDrafts(drafts)
                if (drafts.isEmpty()) {
                    showToast("No saved drafts found")
                }
            } catch (e: Exception) {
                showToast("Error loading drafts: ${e.localizedMessage}")
            }
        }
    }

    private fun deleteDraft(draft: Draft) {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    database.draftDao().deleteDraft(draft)
                }
                adapter.removeDraft(draft)
                showToast("Draft deleted successfully")
            } catch (e: Exception) {
                showToast("Error deleting draft: ${e.localizedMessage}")
            }
        }
    }

    private fun deleteAllDrafts() {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    database.draftDao().deleteAllDrafts()
                }
                adapter.clearDrafts()
                showToast("All drafts deleted successfully")
            } catch (e: Exception) {
                showToast("Error deleting all drafts: ${e.localizedMessage}")
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        loadDrafts() // Refresh the list when returning to this activity
    }
}

