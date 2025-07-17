package com.example.newsreporter

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RejectedArticlesActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: RejectedArticlesAdapter
    private val rejectedArticles = mutableListOf<ArticleSubmissionResponse>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rejected_articles)

        recyclerView = findViewById(R.id.recyclerViewRejectedArticles)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = RejectedArticlesAdapter(
            rejectedArticles,
            onPreviewClick = { article ->
                // Open preview activity showing the rejection details (including editor comment)
                val intent = Intent(this, ArticlePreviewActivity::class.java)
                intent.putExtra("ARTICLE_ID", article.id)
                intent.putExtra("REVIEW_COMMENT", article.reviewComment)
                startActivity(intent)
            },
            onEditClick = { article ->
                // Open the draft editor to rework the rejected article
                val intent = Intent(this, DraftArticleActivity::class.java)
                intent.putExtra("REJECTED_ARTICLE_ID", article.id.toString()) // Use REJECTED_ARTICLE_ID instead
                startActivity(intent)
            }


        )

        recyclerView.adapter = adapter

        loadRejectedArticles()
    }

    private fun loadRejectedArticles() {
        RetrofitClient.articleApi.getRejectedArticles().enqueue(object : Callback<List<ArticleSubmissionResponse>> {
            override fun onResponse(
                call: Call<List<ArticleSubmissionResponse>>,
                response: Response<List<ArticleSubmissionResponse>>
            ) {
                if (response.isSuccessful) {
                    rejectedArticles.clear()
                    response.body()?.let { list ->
                        val sortedArticles = list.sortedByDescending { it.updatedAt }
                        rejectedArticles.addAll(sortedArticles)
                        adapter.notifyDataSetChanged()
                    }
                } else {
                    Toast.makeText(this@RejectedArticlesActivity, "Error loading rejected articles", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<List<ArticleSubmissionResponse>>, t: Throwable) {
                Toast.makeText(this@RejectedArticlesActivity, "Network error", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
