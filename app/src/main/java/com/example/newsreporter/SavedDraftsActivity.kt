package com.example.newsreporter

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import androidx.recyclerview.widget.DividerItemDecoration
import android. widget . Toast



class SavedDraftsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: DraftAdapter
    private lateinit var database: DraftDatabase
    private lateinit var categoryAutoComplete: AutoCompleteTextView
    private lateinit var emptyStateContainer: ViewGroup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_saved_drafts)

        setupViews()
        setupToolbar()
        setupCategoryFilter()
        setupRecyclerView()
        setupFab()

        loadDrafts()
    }

    private fun setupViews() {
        recyclerView = findViewById(R.id.rv_saved_drafts)
        categoryAutoComplete = findViewById(R.id.spinner_category_filter)
        emptyStateContainer = findViewById(R.id.empty_state_container)
        database = DraftDatabase.getDatabase(this)
    }

    private fun setupToolbar() {
        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressed() }
    }

    private fun setupCategoryFilter() {
        val categories = resources.getStringArray(R.array.article_categories)
        val adapter = ArrayAdapter(this, R.layout.item_dropdown, categories)
        categoryAutoComplete.setAdapter(adapter)
        categoryAutoComplete.setText(categories[0], false)

        categoryAutoComplete.setOnItemClickListener { _, _, _, _ ->
            loadDrafts()
        }
    }

    private fun setupRecyclerView() {
        adapter = DraftAdapter(
            drafts = mutableListOf(),
            onDraftClick = { draft -> navigateToDraftArticle(draft) },
            onDeleteClick = { draft -> showDeleteConfirmation(draft) }
        )
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.addItemDecoration(
            DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        )
    }

    private fun setupFab() {
        findViewById<FloatingActionButton>(R.id.fab_new_draft).setOnClickListener {
            startActivity(Intent(this, DraftArticleActivity::class.java))
        }
    }

    private fun showDeleteConfirmation(draft: Draft) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Draft")
            .setMessage("Are you sure you want to delete this draft?")
            .setPositiveButton("Delete") { _, _ -> deleteDraft(draft) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteAllConfirmation() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete All Drafts")
            .setMessage("Are you sure you want to delete all drafts? This action cannot be undone.")
            .setPositiveButton("Delete All") { _, _ -> deleteAllDrafts() }
            .setNegativeButton("Cancel", null)
            .show()
    }
    private fun navigateToDraftArticle(draft: Draft) {
        val intent = Intent(this, DraftArticleActivity::class.java)
        intent.putExtra("DRAFT_ID", draft.id)
        startActivity(intent)
    }

    private fun loadDrafts() {
        val selectedCategory = categoryAutoComplete.text.toString()

        lifecycleScope.launch {
            try {
                val drafts = withContext(Dispatchers.IO) {
                    if (selectedCategory.equals("All", ignoreCase = true)) {
                        database.draftDao().getDraftsByStatus("Draft")
                    } else {
                        database.draftDao().getDraftsByStatusAndCategory("Draft", selectedCategory)
                    }
                }

                val sortedDrafts = drafts.sortedByDescending { it.lastModified }
                adapter.updateDrafts(sortedDrafts)

                // Show/hide empty state
                emptyStateContainer.visibility = if (sortedDrafts.isEmpty()) View.VISIBLE else View.GONE
                recyclerView.visibility = if (sortedDrafts.isEmpty()) View.GONE else View.VISIBLE

            } catch (e: Exception) {
                showError("Error loading drafts: ${e.localizedMessage}")
            }
        }
    }

    private fun showError(message: String) {
        Snackbar.make(
            findViewById(android.R.id.content),
            message,
            Snackbar.LENGTH_LONG
        ).setBackgroundTint(resources.getColor(android.R.color.holo_red_dark))
            .show()
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

    // Refresh the list when returning to this activity.
    override fun onResume() {
        super.onResume()
        loadDrafts()
    }

}


