package com.example.newsreporter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class RejectedArticlesAdapter(
    private val articles: List<ArticleSubmissionResponse>,
    private val onPreviewClick: (ArticleSubmissionResponse) -> Unit,
    private val onEditClick: (ArticleSubmissionResponse) -> Unit
) : RecyclerView.Adapter<RejectedArticlesAdapter.RejectedArticleViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RejectedArticleViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_rejected_article, parent, false)
        return RejectedArticleViewHolder(view)
    }

    override fun onBindViewHolder(holder: RejectedArticleViewHolder, position: Int) {
        val article = articles[position]
        holder.bind(article)
    }

    override fun getItemCount(): Int = articles.size

    inner class RejectedArticleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleText: TextView = itemView.findViewById(R.id.textArticleTitle)
        private val rejectionPreview: TextView = itemView.findViewById(R.id.textRejectionPreview)
        private val previewButton: Button = itemView.findViewById(R.id.buttonPreview)
        private val editButton: Button = itemView.findViewById(R.id.buttonEdit)

        fun bind(article: ArticleSubmissionResponse) {
            titleText.text = article.title
            // Show a short preview of the rejection comment (or full text if short)
            rejectionPreview.text = article.reviewComment ?: "No rejection comment provided"

            previewButton.setOnClickListener { onPreviewClick(article) }
            editButton.setOnClickListener { onEditClick(article) }
        }
    }
}
