package com.example.newsreporter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.core.text.HtmlCompat


class SubmittedArticlesAdapter(
    private var articles: List<ArticleSubmissionResponse>,
    private val onItemClick: (ArticleSubmissionResponse) -> Unit
) : RecyclerView.Adapter<SubmittedArticlesAdapter.ArticleViewHolder>() {

    inner class ArticleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tvArticleTitle)
        val tvPreview: TextView = itemView.findViewById(R.id.tvArticlePreview)
        val tvStatus: TextView = itemView.findViewById(R.id.tvArticleStatus)
        val tvDate: TextView = itemView.findViewById(R.id.tvArticleDate)
        val tvCategory: TextView = itemView.findViewById(R.id.tvArticleCategory)

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

        // Bind title with HTML rendering
        holder.tvTitle.text = renderHtmlContentCompat(article.title)

        holder.tvPreview.text = article.preview ?: "No preview available"

        // Bind status with proper styling
        holder.tvStatus.text = when(article.status) {
            "SUBMITTED" -> "Pending Approval"
            "APPROVED" -> "Published"
            "REJECTED" -> "Rejected"
            else -> article.status
        }

        // Update status background based on status
        val statusBackground = when(article.status) {
            "SUBMITTED" -> R.drawable.status_badge_pending
            "APPROVED" -> R.drawable.status_badge_published
            "REJECTED" -> R.drawable.status_badge_rejected
            else -> R.drawable.status_badge_published
        }
        holder.tvStatus.setBackgroundResource(statusBackground)

        // Bind formatted date
        holder.tvDate.text = formatDate(article.createdAt)

        // Bind category
        holder.tvCategory.text = article.category
    }

    override fun getItemCount() = articles.size

    fun updateArticles(newArticles: List<ArticleSubmissionResponse>) {
        articles = newArticles
        notifyDataSetChanged()
    }


    /**
     * ✅ METHOD: HtmlCompat.fromHtml() - RECOMMENDED
     * Better backward compatibility without manual API checks
     */
    private fun renderHtmlContentCompat(content: String): CharSequence {
        return HtmlCompat.fromHtml(content, HtmlCompat.FROM_HTML_MODE_COMPACT)
    }

    /**
     * ✅ ENHANCED: Generate HTML preview with formatting preserved
     * Truncates content while maintaining HTML structure
     */
    /**
     * Generate clean preview text by stripping HTML tags and formatting
     */
    private fun generateCleanPreview(content: String): String {
        // First, strip all HTML tags to get plain text
        val plainTextContent = stripHtmlTags(content)

        // Then truncate to reasonable length
        val maxPreviewLength = 150 // Shorter for better UI

        return if (plainTextContent.length > maxPreviewLength) {
            val truncateIndex = findSafeWordBreak(plainTextContent, maxPreviewLength)
            plainTextContent.substring(0, truncateIndex).trim() + "..."
        } else {
            plainTextContent.trim()
        }
    }

    /**
     * Strip HTML tags and decode HTML entities
     */
    private fun stripHtmlTags(html: String): String {
        // Use HtmlCompat to parse and convert to plain text
        val spanned = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_COMPACT)
        return spanned.toString()
    }

    /**
     * Find safe word break point to avoid cutting words
     */
    private fun findSafeWordBreak(content: String, maxLength: Int): Int {
        if (content.length <= maxLength) return content.length

        // Look for last space before maxLength
        var breakPoint = maxLength
        while (breakPoint > 0 && content[breakPoint] != ' ') {
            breakPoint--
        }

        return if (breakPoint > 0) breakPoint else maxLength
    }


    /**
     * Find a safe point to truncate HTML content without breaking tags
     */
    private fun findSafeBreakPoint(content: String, maxLength: Int): Int {
        if (content.length <= maxLength) return content.length

        // Look for the last complete word before maxLength
        var breakPoint = maxLength
        while (breakPoint > 0 && content[breakPoint] != ' ' && content[breakPoint] != '<') {
            breakPoint--
        }

        // If we're inside an HTML tag, find the end of the tag
        if (content[breakPoint] == '<') {
            val tagEnd = content.indexOf('>', breakPoint)
            if (tagEnd != -1 && tagEnd < maxLength + 50) { // Allow some extra space for tag completion
                breakPoint = tagEnd + 1
            }
        }

        return if (breakPoint > 0) breakPoint else maxLength
    }

    /**
     * Format date string to relative time
     */
    private fun formatDate(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

            val date = inputFormat.parse(dateString)
            date?.let {
                val now = Date()
                val diffInMillis = now.time - it.time
                val diffInHours = diffInMillis / (1000 * 60 * 60)
                val diffInDays = diffInHours / 24

                when {
                    diffInHours < 1 -> "Just now"
                    diffInHours < 24 -> "${diffInHours} hours ago"
                    diffInDays < 7 -> "${diffInDays} days ago"
                    else -> outputFormat.format(it)
                }
            } ?: "Unknown date"
        } catch (e: Exception) {
            "Unknown date"
        }
    }
}
