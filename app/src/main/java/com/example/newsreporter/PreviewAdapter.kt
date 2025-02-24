package com.example.newsreporter

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class PreviewAdapter(private val elements: List<ArticleElement>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_TEXT = 1
        private const val VIEW_TYPE_IMAGE = 2
    }

    class TextViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.preview_text)
    }

    class ImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.preview_image)
    }

    override fun getItemViewType(position: Int): Int {
        return when (elements[position].type) {
            ElementType.IMAGE -> VIEW_TYPE_IMAGE
            else -> VIEW_TYPE_TEXT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_IMAGE -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_preview_image, parent, false)
                ImageViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_preview_text, parent, false)
                TextViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val element = elements[position]

        when (holder) {
            is TextViewHolder -> {
                val textStyle = when (element.type) {
                    ElementType.HEADING -> R.style.CustomTextAppearance_Headline
                    ElementType.SUBHEADING -> R.style.CustomTextAppearance_Subhead
                    ElementType.QUOTE -> R.style.CustomTextAppearance_Quote
                    else -> R.style.CustomTextAppearance_Body1
                }
                holder.textView.setTextAppearance(textStyle)
                holder.textView.text = element.content
            }
            is ImageViewHolder -> {
                val imageUrl = element.content
                if (imageUrl.startsWith("content://") || imageUrl.startsWith("file://")) {
                    Glide.with(holder.imageView)
                        .load(Uri.parse(imageUrl))
                        .fitCenter()
                        .into(holder.imageView)
                } else {
                    Glide.with(holder.imageView)
                        .load(imageUrl)
                        .fitCenter()
                        .into(holder.imageView)
                }
            }
        }
    }

    override fun getItemCount() = elements.size
}