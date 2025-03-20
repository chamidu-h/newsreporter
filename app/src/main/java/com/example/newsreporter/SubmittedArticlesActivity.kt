package com.example.newsreporter

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SubmittedArticlesActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SubmittedArticlesAdapter
    private val articles = mutableListOf<ArticleSubmissionResponse>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_submitted_articles)

        recyclerView = findViewById(R.id.recyclerViewSubmittedArticles)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = SubmittedArticlesAdapter(articles) { article ->
            // When an item is clicked, open preview activity!
            val intent = Intent(this, ArticlePreviewActivity::class.java)
            intent.putExtra("ARTICLE_ID", article.id)
            startActivity(intent)
        }
        recyclerView.adapter = adapter

        // Add this block to wire up the "View Rejected Articles" button
        val btnViewRejectedArticles = findViewById<MaterialButton>(R.id.btn_view_rejected_articles)
        btnViewRejectedArticles.setOnClickListener {
            val intent = Intent(this, RejectedArticlesActivity::class.java)
            startActivity(intent)
        }

        loadSubmittedArticles()
    }

    private fun loadSubmittedArticles() {
        RetrofitClient.articleApi.getSubmittedArticles().enqueue(object : Callback<List<ArticleSubmissionResponse>> {
            override fun onResponse(
                call: Call<List<ArticleSubmissionResponse>>,
                response: Response<List<ArticleSubmissionResponse>>
            ) {
                if (response.isSuccessful) {
                    articles.clear()
                    response.body()?.let { list ->
                        // Sort articles descending by createdAt so that the latest appears first
                        val sortedArticles = list.sortedByDescending { it.createdAt }
                        articles.addAll(sortedArticles)
                        adapter.notifyDataSetChanged()
                    }
                } else {
                    Toast.makeText(this@SubmittedArticlesActivity, "Error loading articles", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<List<ArticleSubmissionResponse>>, t: Throwable) {
                Toast.makeText(this@SubmittedArticlesActivity, "Network error", Toast.LENGTH_SHORT).show()
            }
        })
    }
}


