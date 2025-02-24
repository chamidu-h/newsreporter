package com.example.newsreporter

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ArticlePreviewActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var previewAdapter: PreviewAdapter
    private var articleElements: List<ArticleElement> = listOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_article_preview)

        recyclerView = findViewById(R.id.preview_content_recycler)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Initially empty adapter, update once data is loaded
        previewAdapter = PreviewAdapter(articleElements)
        recyclerView.adapter = previewAdapter

        val articleId = intent.getLongExtra("ARTICLE_ID", -1)
        if (articleId != -1L) {
            loadArticleDetails(articleId)
        } else {
            Toast.makeText(this, "Invalid article", Toast.LENGTH_SHORT).show()
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
                        // Convert the article content to a list of elements (using your converter)
                        articleElements = ArticleElementConverter.convert(article.content)
                        previewAdapter = PreviewAdapter(articleElements)
                        recyclerView.adapter = previewAdapter
                        // Optionally, set the title or other details in the activity's UI
                        title = article.title
                    }
                } else {
                    Toast.makeText(this@ArticlePreviewActivity, "Error loading article", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<ArticleSubmissionResponse>, t: Throwable) {
                Toast.makeText(this@ArticlePreviewActivity, "Network error", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
