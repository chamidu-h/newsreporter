package com.example.newsreporter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

class DraftAdapter(
    private val drafts: MutableList<Draft>,
    private val onDraftClick: (Draft) -> Unit,
    private val onDeleteClick: (Draft) -> Unit
) : RecyclerView.Adapter<DraftAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleView: TextView = view.findViewById(R.id.draft_title)
        val previewView: TextView = view.findViewById(R.id.draft_preview)
        val deleteButton: ImageButton = view.findViewById(R.id.btn_delete_draft)

        // ✅ ADD MISSING UI ELEMENT REFERENCES
        val dateView: TextView = view.findViewById(R.id.tvDraftDate)
        val categoryView: TextView = view.findViewById(R.id.tvDraftCategory)
        val editButton: MaterialButton = view.findViewById(R.id.btnEditDraft)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_draft, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val draft = drafts[position]

        // Bind existing data
        holder.titleView.text = draft.title
        holder.previewView.text = generatePreview(draft.content)

        // ✅ BIND DATE DATA - Format the lastModified timestamp
        holder.dateView.text = formatDate(draft.lastModified)

        // ✅ BIND CATEGORY DATA
        holder.categoryView.text = draft.category

        // Set up click listeners
        holder.itemView.setOnClickListener { onDraftClick(draft) }
        holder.deleteButton.setOnClickListener { onDeleteClick(draft) }

        // ✅ ADD EDIT BUTTON CLICK LISTENER
        holder.editButton.setOnClickListener { onDraftClick(draft) }
    }

    override fun getItemCount() = drafts.size

    fun updateDrafts(newDrafts: List<Draft>) {
        drafts.clear()
        drafts.addAll(newDrafts)
        notifyDataSetChanged()
    }

    fun removeDraft(draft: Draft) {
        val position = drafts.indexOf(draft)
        if (position != -1) {
            drafts.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    fun clearDrafts() {
        drafts.clear()
        notifyDataSetChanged()
    }

    private fun generatePreview(content: List<ArticleElement>): String {
        val previewText = content.joinToString(" ") { element ->
            when (element.type) {
                ElementType.HEADING, ElementType.SUBHEADING, ElementType.PARAGRAPH -> element.content
                ElementType.QUOTE -> "\"${element.content}\""
                ElementType.IMAGE -> "[Image]"
                ElementType.VIDEO -> "[Video]"
            }
        }
        return if (previewText.length > 100) previewText.substring(0, 97) + "..." else previewText
    }

    // ✅ ADD DATE FORMATTING METHOD
    private fun formatDate(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diffInMillis = now - timestamp
        val diffInMinutes = diffInMillis / (1000 * 60)
        val diffInHours = diffInMinutes / 60
        val diffInDays = diffInHours / 24

        return when {
            diffInMinutes < 1 -> "Just now"
            diffInMinutes < 60 -> "${diffInMinutes}m ago"
            diffInHours < 24 -> "${diffInHours}h ago"
            diffInDays < 7 -> "${diffInDays}d ago"
            else -> {
                val date = java.util.Date(timestamp)
                val formatter = java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault())
                formatter.format(date)
            }
        }
    }
}


