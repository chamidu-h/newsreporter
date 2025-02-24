package com.example.newsreporter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SubmittedArticlesAdapter(
    private var articles: List<ArticleSubmissionResponse>,
    private val onItemClick: (ArticleSubmissionResponse) -> Unit
) : RecyclerView.Adapter<SubmittedArticlesAdapter.ArticleViewHolder>() {

    inner class ArticleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tvArticleTitle)
        val tvStatus: TextView = itemView.findViewById(R.id.tvArticleStatus)
        init {
            itemView.setOnClickListener {
                onItemClick(articles[adapterPosition])
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArticleViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_submitted_article, parent, false)
        return ArticleViewHolder(view)
    }

    override fun onBindViewHolder(holder: ArticleViewHolder, position: Int) {
        val article = articles[position]
        holder.tvTitle.text = article.title
        // You could map status if needed (for example, display "Pending" for SUBMITTED)
        holder.tvStatus.text = when(article.status) {
            "SUBMITTED" -> "Pending Approval"
            "APPROVED" -> "Approved"
            "REJECTED" -> "Rejected"
            else -> article.status
        }
    }

    override fun getItemCount() = articles.size

    fun updateArticles(newArticles: List<ArticleSubmissionResponse>) {
        articles = newArticles
        notifyDataSetChanged()
    }
}
