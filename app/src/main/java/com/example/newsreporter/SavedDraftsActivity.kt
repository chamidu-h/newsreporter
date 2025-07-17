package com.example.newsreporter

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
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
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import androidx.recyclerview.widget.DividerItemDecoration
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton


class SavedDraftsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: DraftAdapter
    private lateinit var database: DraftDatabase
    private lateinit var categoryAutoComplete: AutoCompleteTextView
    private lateinit var emptyStateContainer: ViewGroup

    // ✅ ADD MISSING PROPERTY
    private var allDrafts: List<Draft> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_saved_drafts)

        setupViews()
        setupToolbar()
        setupCategoryFilter()
        setupRecyclerView()
        setupFab()
        setupDeleteAllButton()

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

    // ✅ FIXED CATEGORY FILTER IMPLEMENTATION
    private fun setupCategoryFilter() {
        try {
            val categories = resources.getStringArray(R.array.article_categories)

            val arrayAdapter = ArrayAdapter(
                this,
                android.R.layout.simple_dropdown_item_1line,
                categories
            )

            categoryAutoComplete.setAdapter(arrayAdapter)
            categoryAutoComplete.setText(categories[0], false)

            categoryAutoComplete.setOnItemClickListener { _, _, position, _ ->
                val selectedCategory = categories[position]
                filterDraftsByCategory(selectedCategory)
                updateDraftsCount()
            }

            categoryAutoComplete.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {}
            })

        } catch (e: Exception) {
            Log.e("SavedDraftsActivity", "Error setting up category filter: ${e.message}")
            showErrorMessage("Failed to load categories")
        }
    }

    // ✅ FIXED FILTER METHOD
    private fun filterDraftsByCategory(category: String) {
        val filteredDrafts = if (category == "All") {
            allDrafts
        } else {
            allDrafts.filter { it.category == category }
        }

        // ✅ CORRECT - Use adapter instance, not static call
        adapter.updateDrafts(filteredDrafts)
        toggleEmptyState(filteredDrafts.isEmpty())
    }

    // ✅ FIXED UPDATE COUNT METHOD
    private fun updateDraftsCount() {
        // ✅ CORRECT - Access through adapter instance
        val count = adapter.itemCount
        val countText = findViewById<TextView>(R.id.tv_drafts_count)
        countText?.text = "$count ${if (count == 1) "article" else "articles"}"
    }

    private fun toggleEmptyState(isEmpty: Boolean) {
        val emptyContainer = findViewById<LinearLayout>(R.id.empty_state_container)
        val recyclerView = findViewById<RecyclerView>(R.id.rv_saved_drafts)

        if (isEmpty) {
            emptyContainer?.visibility = View.VISIBLE
            recyclerView?.visibility = View.GONE
        } else {
            emptyContainer?.visibility = View.GONE
            recyclerView?.visibility = View.VISIBLE
        }
    }

    private fun showErrorMessage(message: String) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG)
            .setBackgroundTint(ContextCompat.getColor(this, R.color.design_default_color_error))
            .show()
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
        // ✅ CORRECT - Use ExtendedFloatingActionButton instead of FloatingActionButton
        val fab = findViewById<ExtendedFloatingActionButton>(R.id.fab_new_draft)
        fab.setOnClickListener {
            startActivity(Intent(this, DraftArticleActivity::class.java))
        }
    }

    // ✅ ADD MISSING DELETE ALL BUTTON SETUP
    private fun setupDeleteAllButton() {
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_delete_all_drafts)
            ?.setOnClickListener {
                showDeleteAllConfirmation()
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

    // ✅ FIXED LOAD DRAFTS METHOD
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

                // ✅ UPDATE BOTH ADAPTER AND allDrafts PROPERTY
                allDrafts = sortedDrafts
                adapter.updateDrafts(sortedDrafts)
                updateDraftsCount()

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

                // ✅ UPDATE allDrafts PROPERTY
                allDrafts = allDrafts.filter { it.id != draft.id }
                updateDraftsCount()

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

                // ✅ UPDATE allDrafts PROPERTY
                allDrafts = emptyList()
                updateDraftsCount()

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
        loadDrafts()
    }
}


