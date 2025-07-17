package com.example.newsreporter

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

class SubmittedArticlesActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SubmittedArticlesAdapter
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var emptyStateContainer: View
    private lateinit var articlesCountText: TextView
    private lateinit var lastUpdatedText: TextView
    private val articles = mutableListOf<ArticleSubmissionResponse>()

    // Auto-refresh handler
    private val refreshHandler = Handler(Looper.getMainLooper())
    private val refreshRunnable = object : Runnable {
        override fun run() {
            loadSubmittedArticles(showProgress = false)
            refreshHandler.postDelayed(this, 30000) // Refresh every 30 seconds
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_submitted_articles)

        setupViews()
        setupToolbar()
        setupSwipeRefresh()
        setupRecyclerView()
        setupFab()

        loadSubmittedArticles()
    }

    private fun setupViews() {
        recyclerView = findViewById(R.id.recyclerViewSubmittedArticles)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        emptyStateContainer = findViewById(R.id.empty_state_container)
        articlesCountText = findViewById(R.id.tv_articles_count)
        lastUpdatedText = findViewById(R.id.tv_last_updated)
    }

    private fun setupToolbar() {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressed() }
    }

    private fun setupSwipeRefresh() {
        swipeRefreshLayout.setColorSchemeResources(
            R.color.colorPrimary,
            R.color.colorSecondary,
            R.color.teal_700
        )

        swipeRefreshLayout.setOnRefreshListener {
            loadSubmittedArticles(showProgress = false)
        }
    }

    private fun setupRecyclerView() {
        adapter = SubmittedArticlesAdapter(articles) { article ->
            val intent = Intent(this, ArticlePreviewActivity::class.java)
            intent.putExtra("ARTICLE_ID", article.id)
            startActivity(intent)
        }

        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun setupFab() {
        val btnViewRejectedArticles = findViewById<MaterialButton>(R.id.btn_view_rejected_articles)
        btnViewRejectedArticles.setOnClickListener {
            val intent = Intent(this, RejectedArticlesActivity::class.java)
            startActivity(intent)
        }

        // Setup create first article button
        val btnCreateFirstArticle = findViewById<MaterialButton>(R.id.btn_create_first_article)
        btnCreateFirstArticle?.setOnClickListener {
            val intent = Intent(this, DraftArticleActivity::class.java)
            startActivity(intent)
        }
    }

    // ✅ FIXED: Proper spinner management to prevent endless loading
    private fun loadSubmittedArticles(showProgress: Boolean = true) {
        if (showProgress) {
            swipeRefreshLayout.isRefreshing = true
        }

        // Enhanced: Try different endpoints with fallback mechanism
        loadArticlesWithFallback()
    }

    private fun loadArticlesWithFallback() {
        // Try to get user-specific articles first
        val userId = UserSessionManager.getUserId()

        if (userId != null) {
            Log.d("SubmittedArticles", "Loading articles for user: $userId")
            RetrofitClient.articleApi.getUserArticles(userId).enqueue(object : Callback<List<ArticleSubmissionResponse>> {
                override fun onResponse(call: Call<List<ArticleSubmissionResponse>>, response: Response<List<ArticleSubmissionResponse>>) {
                    if (response.isSuccessful) {
                        Log.d("SubmittedArticles", "User articles loaded successfully")
                        handleSuccessfulResponse(response.body())
                    } else {
                        Log.w("SubmittedArticles", "User articles failed, trying all articles. Code: ${response.code()}")
                        loadAllArticles()
                    }
                }

                override fun onFailure(call: Call<List<ArticleSubmissionResponse>>, t: Throwable) {
                    Log.e("SubmittedArticles", "User articles network error: ${t.message}")
                    loadAllArticles()
                }
            })
        } else {
            Log.d("SubmittedArticles", "No user ID found, loading all articles")
            loadAllArticles()
        }
    }

    private fun loadAllArticles() {
        Log.d("SubmittedArticles", "Attempting to load all articles...")

        RetrofitClient.articleApi.getAllSubmittedArticles().enqueue(object : Callback<List<ArticleSubmissionResponse>> {
            override fun onResponse(call: Call<List<ArticleSubmissionResponse>>, response: Response<List<ArticleSubmissionResponse>>) {
                Log.d("SubmittedArticles", "All articles response - Code: ${response.code()}, Success: ${response.isSuccessful}")

                // ✅ CRITICAL FIX: Always stop spinner in onResponse
                swipeRefreshLayout.isRefreshing = false

                if (response.isSuccessful) {
                    Log.d("SubmittedArticles", "All articles loaded successfully")
                    handleSuccessfulResponse(response.body())
                } else {
                    Log.w("SubmittedArticles", "All articles failed, trying submitted only. Code: ${response.code()}, Message: ${response.message()}")
                    loadSubmittedOnly()
                }
            }

            override fun onFailure(call: Call<List<ArticleSubmissionResponse>>, t: Throwable) {
                // ✅ CRITICAL FIX: Always stop spinner in onFailure
                swipeRefreshLayout.isRefreshing = false
                Log.e("SubmittedArticles", "All articles network error: ${t.message}")
                loadSubmittedOnly()
            }
        })
    }

    private fun loadSubmittedOnly() {
        Log.d("SubmittedArticles", "Attempting to load submitted articles only...")

        RetrofitClient.articleApi.getSubmittedArticles().enqueue(object : Callback<List<ArticleSubmissionResponse>> {
            override fun onResponse(call: Call<List<ArticleSubmissionResponse>>, response: Response<List<ArticleSubmissionResponse>>) {
                Log.d("SubmittedArticles", "Submitted articles response - Code: ${response.code()}, Success: ${response.isSuccessful}")

                // ✅ CRITICAL FIX: Always stop spinner in final fallback
                swipeRefreshLayout.isRefreshing = false

                if (response.isSuccessful) {
                    Log.d("SubmittedArticles", "Submitted articles loaded successfully")
                    handleSuccessfulResponse(response.body())
                } else {
                    val errorMsg = "Error loading articles: ${response.code()} - ${response.message()}"
                    Log.e("SubmittedArticles", errorMsg)
                    showError(errorMsg)
                }
            }

            override fun onFailure(call: Call<List<ArticleSubmissionResponse>>, t: Throwable) {
                // ✅ CRITICAL FIX: Always stop spinner in final fallback failure
                swipeRefreshLayout.isRefreshing = false
                val errorMsg = "Network error: ${t.localizedMessage ?: "Connection failed"}"
                Log.e("SubmittedArticles", errorMsg, t)
                showError(errorMsg)
            }
        })
    }

    private fun handleSuccessfulResponse(responseBody: List<ArticleSubmissionResponse>?) {
        // ✅ ENSURE: Spinner is stopped here too for safety
        swipeRefreshLayout.isRefreshing = false

        articles.clear()
        responseBody?.let { list ->
            Log.d("SubmittedArticles", "Received ${list.size} articles")
            val sortedArticles = list.sortedByDescending { it.createdAt }
            articles.addAll(sortedArticles)
            adapter.notifyDataSetChanged()

            updateUI()
            updateLastRefreshTime()
        } ?: run {
            Log.w("SubmittedArticles", "Response body was null")
            showError("No articles data received")
        }
    }

    private fun updateUI() {
        val isEmpty = articles.isEmpty()

        emptyStateContainer?.visibility = if (isEmpty) View.VISIBLE else View.GONE
        recyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE

        val count = articles.size
        articlesCountText?.text = "$count ${if (count == 1) "article" else "articles"}"

        Log.d("SubmittedArticles", "UI updated - Articles count: $count, Empty state: $isEmpty")
    }

    private fun updateLastRefreshTime() {
        val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        lastUpdatedText?.text = "Updated at $currentTime"
    }

    private fun showError(message: String) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG)
            .setBackgroundTint(ContextCompat.getColor(this, R.color.design_default_color_error))
            .show()
        Log.e("SubmittedArticlesActivity", "Error: $message")
    }

    override fun onResume() {
        super.onResume()
        loadSubmittedArticles()
        refreshHandler.post(refreshRunnable)
    }

    override fun onPause() {
        super.onPause()
        refreshHandler.removeCallbacks(refreshRunnable)
    }
}
