package com.example.newsreporter

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.widget.Toast
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {
    private lateinit var recentDraftsAdapter: DraftAdapter
    private lateinit var recentPublishedAdapter: SubmittedArticlesAdapter
    private lateinit var database: DraftDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        database = DraftDatabase.getDatabase(this)
        setupToolbar()
        setupRecyclerViews()
        setupClickListeners()
        loadRecentDrafts()
        loadRecentPublished()
    }

    private fun setupToolbar() {
        setSupportActionBar(findViewById(R.id.toolbar))
    }

    private fun setupRecyclerViews() {
        // Setup Recent Drafts RecyclerView
        val rvRecentDrafts: RecyclerView = findViewById(R.id.rv_recent_drafts)
        recentDraftsAdapter = DraftAdapter(
            drafts = mutableListOf(),
            onDraftClick = { draft -> navigateToDraftArticle(draft) },
            onDeleteClick = { draft -> deleteDraft(draft) }
        )
        rvRecentDrafts.adapter = recentDraftsAdapter
        rvRecentDrafts.layoutManager = LinearLayoutManager(this)

        // Setup Published Articles (Submitted Articles) RecyclerView
        val rvRecentPublished: RecyclerView = findViewById(R.id.rv_recent_submitted)
        recentPublishedAdapter = SubmittedArticlesAdapter(
            articles = listOf(),
            onItemClick = { article ->
                // Navigate to preview when an article is clicked
                val intent = Intent(this, ArticlePreviewActivity::class.java)
                intent.putExtra("ARTICLE_ID", article.id)
                startActivity(intent)
            }
        )
        rvRecentPublished.adapter = recentPublishedAdapter
        rvRecentPublished.layoutManager = LinearLayoutManager(this)
    }

    private fun setupClickListeners() {
        // FAB and regular button both navigate to new article
        findViewById<FloatingActionButton>(R.id.fab_new_article).setOnClickListener {
            startDraftArticleActivity()
        }
        findViewById<Button>(R.id.btn_draft_article).setOnClickListener {
            startDraftArticleActivity()
        }

        // View all drafts
        findViewById<Button>(R.id.btn_view_saved_drafts).setOnClickListener {
            val intent = Intent(this, SavedDraftsActivity::class.java)
            startActivity(intent)
        }

        // View all published (submitted) articles
        findViewById<Button>(R.id.btn_view_submitted_articles).setOnClickListener {
            val intent = Intent(this, SubmittedArticlesActivity::class.java)
            startActivity(intent)
        }
    }

    private fun startDraftArticleActivity() {
        val intent = Intent(this, DraftArticleActivity::class.java)
        startActivity(intent)
    }

    private fun loadRecentDrafts() {
        lifecycleScope.launch {
            try {
                val drafts = withContext(Dispatchers.IO) {
                    database.draftDao().getDraftsByStatus("Draft")
                        .sortedByDescending { it.lastModified }
                        .take(3) // Only show 3 most recent drafts
                }
                recentDraftsAdapter.updateDrafts(drafts)
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Error loading drafts", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadRecentPublished() {
        RetrofitClient.articleApi.getSubmittedArticles().enqueue(object : Callback<List<ArticleSubmissionResponse>> {
            override fun onResponse(
                call: Call<List<ArticleSubmissionResponse>>,
                response: Response<List<ArticleSubmissionResponse>>
            ) {
                if (response.isSuccessful) {
                    // Sort articles descending by createdAt (newest first)
                    val sortedArticles = response.body()?.sortedByDescending { it.createdAt } ?: listOf()
                    // Take the first 3 items
                    val recentArticles = sortedArticles.take(3)
                    recentPublishedAdapter.updateArticles(recentArticles)
                } else {
                    Toast.makeText(this@MainActivity, "Error loading published articles", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<List<ArticleSubmissionResponse>>, t: Throwable) {
                Toast.makeText(this@MainActivity, "Network error", Toast.LENGTH_SHORT).show()
            }
        })
    }


    private fun navigateToDraftArticle(draft: Draft) {
        val intent = Intent(this, DraftArticleActivity::class.java)
        intent.putExtra("DRAFT_ID", draft.id)
        startActivity(intent)
    }

    private fun deleteDraft(draft: Draft) {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    database.draftDao().deleteDraft(draft)
                }
                loadRecentDrafts() // Reload the list after deletion
                Toast.makeText(this@MainActivity, "Draft deleted", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Error deleting draft", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadRecentDrafts()
        loadRecentPublished()
    }
}

