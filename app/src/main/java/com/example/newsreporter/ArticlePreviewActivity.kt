package com.example.newsreporter

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

class ArticlePreviewActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var previewAdapter: PreviewAdapter
    private lateinit var tvArticleTitle: TextView
    private lateinit var tvArticleCategory: TextView
    private lateinit var tvArticleDate: TextView
    private lateinit var tvReadTime: TextView
    private lateinit var tvPreviewStatus: TextView
    private lateinit var fabShareArticle: ExtendedFloatingActionButton
    private var articleElements: List<ArticleElement> = listOf()
    private var currentArticle: ArticleSubmissionResponse? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_article_preview)

        setupViews()
        setupToolbar()
        setupRecyclerView()
        setupFab()

        val articleId = intent.getLongExtra("ARTICLE_ID", -1)
        if (articleId != -1L) {
            Log.d("ArticlePreview", "Loading article with ID: $articleId")
            loadArticleDetails(articleId)
        } else {
            Toast.makeText(this, "Invalid article", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupViews() {
        recyclerView = findViewById(R.id.preview_content_recycler)
        tvArticleTitle = findViewById(R.id.tv_article_title)
        tvArticleCategory = findViewById(R.id.tv_article_category)
        tvArticleDate = findViewById(R.id.tv_article_date)
        tvReadTime = findViewById(R.id.tv_read_time)
        tvPreviewStatus = findViewById(R.id.preview_status)
        fabShareArticle = findViewById(R.id.fab_share_article)
    }

    private fun setupToolbar() {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressed() }
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(this)
        previewAdapter = PreviewAdapter(articleElements)
        recyclerView.adapter = previewAdapter
    }

    private fun setupFab() {
        fabShareArticle.setOnClickListener {
            shareArticle()
        }
    }

    private fun loadArticleDetails(articleId: Long) {
        RetrofitClient.articleApi.getArticleById(articleId).enqueue(object : Callback<ArticleSubmissionResponse> {
            override fun onResponse(
                call: Call<ArticleSubmissionResponse>,
                response: Response<ArticleSubmissionResponse>
            ) {
                if (response.isSuccessful) {
                    response.body()?.let { article ->
                        Log.d("ArticlePreview", "Article loaded successfully: ${article.title}")
                        currentArticle = article
                        displayArticle(article)
                    } ?: run {
                        Log.e("ArticlePreview", "Article response body is null")
                        showError("Article data not found")
                    }
                } else {
                    Log.e("ArticlePreview", "Error loading article: ${response.code()} - ${response.message()}")
                    showError("Error loading article: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<ArticleSubmissionResponse>, t: Throwable) {
                Log.e("ArticlePreview", "Network error loading article", t)
                showError("Network error: ${t.localizedMessage}")
            }
        })
    }

    private fun displayArticle(article: ArticleSubmissionResponse) {
        // Display title with HTML rendering
        tvArticleTitle.text = HtmlCompat.fromHtml(article.title, HtmlCompat.FROM_HTML_MODE_COMPACT)

        // Display category
        tvArticleCategory.text = article.category

        // Display status with proper formatting
        displayArticleStatus(article.status)

        // Display formatted date
        displayArticleDate(article)

        // Calculate and display read time
        tvReadTime.text = calculateReadTime(article.content)

        // Convert and display content
        articleElements = ArticleElementConverter.convert(article.content)
        previewAdapter = PreviewAdapter(articleElements)
        recyclerView.adapter = previewAdapter

        // Set toolbar title
        supportActionBar?.title = "Article Preview"

        Log.d("ArticlePreview", "Article displayed - Title: ${article.title}, Category: ${article.category}, Status: ${article.status}, Elements: ${articleElements.size}")
    }

    private fun displayArticleStatus(status: String) {
        when (status) {
            "APPROVED" -> {
                tvPreviewStatus.text = "PUBLISHED"
                tvPreviewStatus.setBackgroundResource(R.drawable.status_badge_published)
            }
            "SUBMITTED" -> {
                tvPreviewStatus.text = "PENDING"
                tvPreviewStatus.setBackgroundResource(R.drawable.status_badge_pending)
            }
            "REJECTED" -> {
                tvPreviewStatus.text = "REJECTED"
                tvPreviewStatus.setBackgroundResource(R.drawable.status_badge_rejected)
            }
            else -> {
                tvPreviewStatus.text = status.uppercase()
                tvPreviewStatus.setBackgroundResource(R.drawable.status_badge_published)
            }
        }
    }

    private fun displayArticleDate(article: ArticleSubmissionResponse) {
        val dateText = when (article.status) {
            "APPROVED" -> formatArticleDate(article.createdAt, "Published on")
            "SUBMITTED" -> formatArticleDate(article.createdAt, "Submitted on")
            "REJECTED" -> formatArticleDate(article.createdAt, "Submitted on")
            else -> formatArticleDate(article.createdAt, "Created on")
        }
        tvArticleDate.text = dateText
    }

    private fun formatArticleDate(dateString: String, prefix: String): String {
        return try {
            // Try different date formats that might be returned from the API
            val possibleFormats = listOf(
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd'T'HH:mm:ss.SSS",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd"
            )

            var parsedDate: Date? = null
            for (format in possibleFormats) {
                try {
                    val inputFormat = SimpleDateFormat(format, Locale.getDefault())
                    parsedDate = inputFormat.parse(dateString)
                    break
                } catch (e: Exception) {
                    // Try next format
                }
            }

            parsedDate?.let { date ->
                val outputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                "$prefix ${outputFormat.format(date)}"
            } ?: "$prefix recently"

        } catch (e: Exception) {
            Log.e("ArticlePreview", "Error parsing date: $dateString", e)
            "$prefix recently"
        }
    }

    private fun calculateReadTime(content: String): String {
        // Remove HTML tags for accurate word count
        val plainText = HtmlCompat.fromHtml(content, HtmlCompat.FROM_HTML_MODE_COMPACT).toString()
        val wordCount = plainText.split("\\s+".toRegex()).filter { it.isNotBlank() }.size
        val readingSpeed = 200 // words per minute (average reading speed)
        val minutes = (wordCount / readingSpeed).coerceAtLeast(1)
        return "$minutes min read"
    }

    private fun shareArticle() {
        currentArticle?.let { article ->
            val shareText = """
                ${HtmlCompat.fromHtml(article.title, HtmlCompat.FROM_HTML_MODE_COMPACT)}
                
                Category: ${article.category}
                
                Check out this article from TRENT News!
            """.trimIndent()

            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, shareText)
                type = "text/plain"
            }

            startActivity(Intent.createChooser(shareIntent, "Share Article"))
        }
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
