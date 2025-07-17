package com.example.newsreporter

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    // Two-stage scrolling behavior components
    private lateinit var mainNestedScrollView: NestedScrollView
    private lateinit var publishedArticlesCard: MaterialCardView
    private var isNestedScrollingEnabled = false

    // Drafts
    private lateinit var rvRecentDrafts: RecyclerView
    private lateinit var recentDraftAdapter: DraftAdapter
    private val recentDrafts = mutableListOf<Draft>()

    // Published articles
    private lateinit var rvRecentPublished: RecyclerView
    private lateinit var publishedAdapter: SubmittedArticlesAdapter
    private val recentPublished = mutableListOf<ArticleSubmissionResponse>()

    // UI elements for published articles section
    private lateinit var tvPublishedCount: TextView
    private lateinit var tvLastUpdated: TextView
    private lateinit var emptyStatePublished: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupToolbar()
        setupQuickActions()
        setupDraftRecycler()
        setupPublishedRecycler()
        setupFab()
        initializeViews()
        setupScrollBehavior()

        loadDrafts()
        loadPublishedArticles()
    }

    /* ------------------------------------------------------------
       Initialize Views
       ------------------------------------------------------------ */
    private fun initializeViews() {
        tvPublishedCount = findViewById(R.id.tv_published_count)
        tvLastUpdated = findViewById(R.id.tv_last_updated)
        emptyStatePublished = findViewById(R.id.empty_state_published)

        // Initialize two-stage scrolling components
        mainNestedScrollView = findViewById(R.id.main_nested_scroll_view)
        publishedArticlesCard = findViewById(R.id.card_published_articles)
    }

    /* ------------------------------------------------------------
       Two-Stage Scrolling Behavior Setup with Reset Fix
       ------------------------------------------------------------ */
    private fun setupScrollBehavior() {
        // Stage 1: Initially disable nested scrolling on RecyclerView
        rvRecentDrafts.isNestedScrollingEnabled = false
        isNestedScrollingEnabled = false

        // Attach scroll listener to NestedScrollView
        mainNestedScrollView.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            checkAndUpdateScrollingBehavior(scrollY)
        }

        Log.d("MainActivity", "Two-stage scrolling behavior initialized")
    }

    private fun checkAndUpdateScrollingBehavior(scrollY: Int) {
        // Get the position of the Published Articles card
        val publishedCardLocation = IntArray(2)
        publishedArticlesCard.getLocationOnScreen(publishedCardLocation)

        // Get screen height to determine visibility
        val screenHeight = resources.displayMetrics.heightPixels
        val cardTopPosition = publishedCardLocation[1]

        // Check if Published Articles card is visible on screen
        val isPublishedCardVisible = cardTopPosition < screenHeight

        // Stage 2: Enable nested scrolling when Published Articles card becomes visible
        if (isPublishedCardVisible && !isNestedScrollingEnabled) {
            rvRecentDrafts.isNestedScrollingEnabled = true
            isNestedScrollingEnabled = true
            Log.d("MainActivity", "Stage 2: Nested scrolling enabled")
        } else if (!isPublishedCardVisible && isNestedScrollingEnabled) {
            // Re-disable if user scrolls back up (Stage 1)
            rvRecentDrafts.isNestedScrollingEnabled = false
            isNestedScrollingEnabled = false
            Log.d("MainActivity", "Stage 1: Nested scrolling disabled")
        }

        // ✅ NEW TWEAK: Reset recent drafts scroll position when at top
        if (scrollY == 0 && !isNestedScrollingEnabled) {
            rvRecentDrafts.scrollToPosition(0)
            Log.d("MainActivity", "Reset recent drafts to top position")
        }
    }

    /* ------------------------------------------------------------
       Toolbar
       ------------------------------------------------------------ */
    private fun setupToolbar() {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
    }

    /* ------------------------------------------------------------
       Quick actions
       ------------------------------------------------------------ */
    private fun setupQuickActions() {
        findViewById<MaterialButton>(R.id.btn_draft_article).setOnClickListener {
            startActivity(Intent(this, DraftArticleActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.btn_view_saved_drafts).setOnClickListener {
            startActivity(Intent(this, SavedDraftsActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.btn_view_submitted_articles).setOnClickListener {
            startActivity(Intent(this, SubmittedArticlesActivity::class.java))
        }
    }

    /* ------------------------------------------------------------
       Draft Recycler
       ------------------------------------------------------------ */
    private fun setupDraftRecycler() {
        rvRecentDrafts = findViewById(R.id.rv_recent_drafts)
        recentDraftAdapter = DraftAdapter(
            drafts = recentDrafts,
            onDraftClick = { draft ->
                startActivity(
                    Intent(this, DraftArticleActivity::class.java)
                        .putExtra("DRAFT_ID", draft.id)
                )
            },
            onDeleteClick = { /* no delete from home */ }
        )
        rvRecentDrafts.apply {
            adapter = recentDraftAdapter
            layoutManager = LinearLayoutManager(this@MainActivity)
            addItemDecoration(
                DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
            )
            // Initial state: nested scrolling disabled
            isNestedScrollingEnabled = false
        }
    }

    /* ------------------------------------------------------------
       Published Recycler
       ------------------------------------------------------------ */
    private fun setupPublishedRecycler() {
        rvRecentPublished = findViewById(R.id.rv_recent_submitted)
        publishedAdapter = SubmittedArticlesAdapter(recentPublished) { article ->
            startActivity(
                Intent(this, ArticlePreviewActivity::class.java)
                    .putExtra("ARTICLE_ID", article.id)
            )
        }
        rvRecentPublished.apply {
            adapter = publishedAdapter
            layoutManager = LinearLayoutManager(this@MainActivity)
            addItemDecoration(
                DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
            )
        }
    }

    /* ------------------------------------------------------------
       Floating-action button
       ------------------------------------------------------------ */
    private fun setupFab() {
        findViewById<FloatingActionButton>(R.id.fab_new_article).setOnClickListener {
            startActivity(Intent(this, DraftArticleActivity::class.java))
        }
    }

    /* ------------------------------------------------------------
       Data loading
       ------------------------------------------------------------ */
    private fun loadDrafts() {
        lifecycleScope.launch {
            val db = DraftDatabase.getDatabase(this@MainActivity)
            val drafts = withContext(Dispatchers.IO) {
                db.draftDao().getDraftsByStatus("Draft")
                    .sortedByDescending { it.lastModified }
                    .take(5)
            }
            recentDrafts.clear()
            recentDrafts.addAll(drafts)
            recentDraftAdapter.notifyDataSetChanged()
            rvRecentDrafts.isVisible = drafts.isNotEmpty()

            Log.d("MainActivity", "Loaded ${drafts.size} recent drafts")
        }
    }

    private fun loadPublishedArticles() {
        Log.d("MainActivity", "Starting to load published articles...")
        loadArticlesWithFallback()
    }

    private fun loadArticlesWithFallback() {
        val userId = UserSessionManager.getUserId()

        if (userId != null) {
            Log.d("MainActivity", "Loading articles for user: $userId")
            RetrofitClient.articleApi.getUserArticles(userId).enqueue(object : Callback<List<ArticleSubmissionResponse>> {
                override fun onResponse(call: Call<List<ArticleSubmissionResponse>>, response: Response<List<ArticleSubmissionResponse>>) {
                    if (response.isSuccessful) {
                        Log.d("MainActivity", "User articles loaded successfully")
                        handleSuccessfulResponse(response.body())
                    } else {
                        Log.w("MainActivity", "User articles failed, trying all articles. Code: ${response.code()}")
                        loadAllArticles()
                    }
                }

                override fun onFailure(call: Call<List<ArticleSubmissionResponse>>, t: Throwable) {
                    Log.e("MainActivity", "User articles network error: ${t.message}")
                    loadAllArticles()
                }
            })
        } else {
            Log.d("MainActivity", "No user ID found, loading all articles")
            loadAllArticles()
        }
    }

    private fun loadAllArticles() {
        Log.d("MainActivity", "Attempting to load all articles...")

        RetrofitClient.articleApi.getAllSubmittedArticles().enqueue(object : Callback<List<ArticleSubmissionResponse>> {
            override fun onResponse(call: Call<List<ArticleSubmissionResponse>>, response: Response<List<ArticleSubmissionResponse>>) {
                Log.d("MainActivity", "All articles response - Code: ${response.code()}, Success: ${response.isSuccessful}")

                if (response.isSuccessful) {
                    Log.d("MainActivity", "All articles loaded successfully")
                    handleSuccessfulResponse(response.body())
                } else {
                    Log.w("MainActivity", "All articles failed, trying submitted only. Code: ${response.code()}, Message: ${response.message()}")
                    loadSubmittedOnly()
                }
            }

            override fun onFailure(call: Call<List<ArticleSubmissionResponse>>, t: Throwable) {
                Log.e("MainActivity", "All articles network error: ${t.message}")
                loadSubmittedOnly()
            }
        })
    }

    private fun loadSubmittedOnly() {
        Log.d("MainActivity", "Attempting to load submitted articles only...")

        RetrofitClient.articleApi.getSubmittedArticles().enqueue(object : Callback<List<ArticleSubmissionResponse>> {
            override fun onResponse(call: Call<List<ArticleSubmissionResponse>>, response: Response<List<ArticleSubmissionResponse>>) {
                Log.d("MainActivity", "Submitted articles response - Code: ${response.code()}, Success: ${response.isSuccessful}")

                if (response.isSuccessful) {
                    Log.d("MainActivity", "Submitted articles loaded successfully")
                    handleSuccessfulResponse(response.body())
                } else {
                    val errorMsg = "Error loading articles: ${response.code()} - ${response.message()}"
                    Log.e("MainActivity", errorMsg)
                    showError(errorMsg)
                }
            }

            override fun onFailure(call: Call<List<ArticleSubmissionResponse>>, t: Throwable) {
                val errorMsg = "Network error: ${t.localizedMessage ?: "Connection failed"}"
                Log.e("MainActivity", errorMsg, t)
                showError(errorMsg)
            }
        })
    }

    private fun handleSuccessfulResponse(responseBody: List<ArticleSubmissionResponse>?) {
        recentPublished.clear()
        responseBody?.let { list ->
            Log.d("MainActivity", "Received ${list.size} articles")

            val publishedArticles = list.filter { it.status == "APPROVED" }
                .sortedByDescending { it.createdAt }
                .take(3)

            Log.d("MainActivity", "Found ${publishedArticles.size} published articles")

            recentPublished.addAll(publishedArticles)
            publishedAdapter.notifyDataSetChanged()

            updatePublishedArticlesUI(publishedArticles)
        } ?: run {
            Log.w("MainActivity", "Response body was null")
            showError("No articles data received")
        }
    }

    private fun updatePublishedArticlesUI(articles: List<ArticleSubmissionResponse>) {
        val count = articles.size
        tvPublishedCount.text = if (count == 1) "$count article" else "$count articles"

        val currentTime = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date())
        tvLastUpdated.text = "Updated $currentTime"

        if (articles.isEmpty()) {
            rvRecentPublished.isVisible = false
            emptyStatePublished.isVisible = true
        } else {
            rvRecentPublished.isVisible = true
            emptyStatePublished.isVisible = false
        }

        Log.d("MainActivity", "UI updated - Published articles count: $count, Empty state: ${articles.isEmpty()}")
    }

    private fun showError(message: String) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG)
            .setBackgroundTint(ContextCompat.getColor(this, R.color.design_default_color_error))
            .show()
        Log.e("MainActivity", "Error: $message")
    }

    override fun onResume() {
        super.onResume()
        loadDrafts()
        loadPublishedArticles()

        // Reset scrolling behavior when returning to activity
        if (::rvRecentDrafts.isInitialized) {
            rvRecentDrafts.isNestedScrollingEnabled = false
            isNestedScrollingEnabled = false
            rvRecentDrafts.scrollToPosition(0) // ✅ Also reset position on resume
            Log.d("MainActivity", "Scrolling behavior and position reset on resume")
        }
    }

    override fun onPause() {
        super.onPause()
        if (::mainNestedScrollView.isInitialized) {
            mainNestedScrollView.scrollTo(0, 0)
        }
    }
}
