package com.example.newsreporter

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android. graphics. ImageDecoder

class DraftArticleActivity : AppCompatActivity() {

    private lateinit var database: DraftDatabase
    private lateinit var titleInput: EditText
    private lateinit var contentRecyclerView: RecyclerView
    private lateinit var addElementButton: FloatingActionButton
    private lateinit var saveDraftButton: Button
    private var draftId: Int = -1
    private lateinit var contentAdapter: ArticleContentAdapter
    private var currentFocusedPosition: Int = 0

    // Used to remember which element needs an image update.
    private var imageElementPosition: Int = -1

    // Register an ActivityResultLauncher for image picking.
    private lateinit var imagePickerLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_draft_article)

        initializeViews()
        setupRecyclerView()
        setupListeners()
        setupImagePickerLauncher()

        draftId = intent.getIntExtra("DRAFT_ID", -1)
        if (draftId == -1) {
            titleInput.setText("Title")
        } else {
            loadDraft(draftId)
        }
    }

    private fun initializeViews() {
        titleInput = findViewById(R.id.article_title)
        contentRecyclerView = findViewById(R.id.content_recycler_view)
        addElementButton = findViewById(R.id.add_element_button)
        saveDraftButton = findViewById(R.id.btn_save_draft)
        database = DraftDatabase.getDatabase(this)
    }

    private fun setupRecyclerView() {
        // Start with a single empty paragraph element.
        val defaultContent = mutableListOf(
            ArticleElement(ElementType.PARAGRAPH, "")
        )
        contentAdapter = ArticleContentAdapter(defaultContent) { position ->
            currentFocusedPosition = position
        }
        contentRecyclerView.layoutManager = LinearLayoutManager(this)
        contentRecyclerView.adapter = contentAdapter
    }

    private fun setupListeners() {
        addElementButton.setOnClickListener {
            showElementSelectorDialog()
        }
        saveDraftButton.setOnClickListener {
            saveDraft()
        }
    }

    private fun setupImagePickerLauncher() {
        imagePickerLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                val uri: Uri? = data?.data
                if (uri != null && imageElementPosition != -1) {
                    // Get the persistable flags from the result
                    var flags = data.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION
                    // If for some reason flags is 0, ensure we pass a valid flag.
                    if (flags == 0) {
                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    }
                    try {
                        // Persist the permission so the image is accessible later.
                        contentResolver.takePersistableUriPermission(uri, flags)
                    } catch (e: SecurityException) {
                        e.printStackTrace()
                        // Optionally notify the user or log the error.
                    }
                    // Update the element with the image URI.
                    contentAdapter.updateElementContentAt(imageElementPosition, uri.toString())
                }
            }
            imageElementPosition = -1
        }
    }


    /**
     * Show a dialog to let the user select an element type.
     * Now includes IMAGE (while still excluding VIDEO).
     */
    private fun showElementSelectorDialog() {
        // Include IMAGE, but exclude VIDEO.
        val types = ElementType.values().filterNot { it == ElementType.VIDEO }
        val typeNames = types.map {
            it.name.lowercase().replaceFirstChar { char -> char.uppercase() }
        }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Select Element Type")
            .setItems(typeNames) { _, which ->
                val selectedType = types[which]
                insertElementAtCurrentPosition(selectedType)
            }
            .show()
    }

    /**
     * Inserts an element at the current focus position.
     * For IMAGE elements, launches the image picker after inserting/updating.
     */
    private fun insertElementAtCurrentPosition(type: ElementType) {
        val currentElement = contentAdapter.getContent().getOrNull(currentFocusedPosition)
        if (currentElement != null) {
            val viewHolder =
                contentRecyclerView.findViewHolderForAdapterPosition(currentFocusedPosition)
            if (viewHolder is ArticleContentAdapter.ViewHolder) {
                val editText = viewHolder.itemView.findViewById<EditText>(R.id.content_text)
                val currentContent = editText.text.toString()
                // If the content is empty (ignoring whitespace), update the type in place.
                if (currentContent.trim().isEmpty()) {
                    contentAdapter.updateElementTypeAt(currentFocusedPosition, type)
                    // For an image element, immediately launch the picker.
                    if (type == ElementType.IMAGE) {
                        startImagePickerForElement(currentFocusedPosition)
                    }
                    return
                }
                // Otherwise, split the content at the cursor position.
                val cursorPosition = editText.selectionStart
                val textBeforeCursor = currentContent.substring(0, cursorPosition)
                val textAfterCursor = currentContent.substring(cursorPosition)

                contentAdapter.updateElementContentAt(currentFocusedPosition, textBeforeCursor)
                val newElement = ArticleElement(type, textAfterCursor)
                contentAdapter.insertElementAfter(currentFocusedPosition, newElement)

                // If the new element is an IMAGE, launch the picker.
                if (type == ElementType.IMAGE) {
                    startImagePickerForElement(currentFocusedPosition + 1)
                }

                contentRecyclerView.post {
                    contentRecyclerView.scrollToPosition(currentFocusedPosition + 1)
                    val newViewHolder =
                        contentRecyclerView.findViewHolderForAdapterPosition(currentFocusedPosition + 1)
                    if (newViewHolder is ArticleContentAdapter.ViewHolder) {
                        newViewHolder.contentView.requestFocus()
                        newViewHolder.contentView.setSelection(0)
                    }
                }
            }
        }
    }

    /**
     * Launches the image picker and remembers the position to update.
     */
    fun startImagePickerForElement(position: Int) {
        imageElementPosition = position
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
            // Add the read flag so the returned URI comes with it
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        imagePickerLauncher.launch(intent)
    }


    private fun loadDraft(draftId: Int) {
        lifecycleScope.launch {
            try {
                val draft = withContext(Dispatchers.IO) {
                    database.draftDao().getDraftById(draftId)
                }
                if (draft != null) {
                    titleInput.setText(draft.title)
                    contentAdapter.updateContent(draft.content.toMutableList())
                } else {
                    showToast("Draft not found")
                }
            } catch (e: Exception) {
                showToast("Error loading draft: ${e.localizedMessage}")
            }
        }
    }

    private fun saveDraft() {
        val title = titleInput.text.toString().trim()
        val content = contentAdapter.getContent()

        if (title.isEmpty() || content.isEmpty()) {
            showToast("Title and content cannot be empty!")
            return
        }

        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val draft = if (draftId != -1) {
                        Draft(draftId, title, content, "Draft")
                    } else {
                        Draft(title = title, content = content, status = "Draft")
                    }
                    database.draftDao().saveDraft(draft)
                }
                showToast("Draft Saved!")
                if (draftId == -1) {
                    titleInput.text.clear()
                    contentAdapter.clearContent()
                }
                navigateToSavedDrafts()
            } catch (e: Exception) {
                showToast("Error saving draft: ${e.localizedMessage}")
            }
        }
    }

    private fun navigateToSavedDrafts() {
        val intent = Intent(this, SavedDraftsActivity::class.java).apply {
            putExtra("REFRESH_LIST", true)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
        finish()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    // ... (Keep your existing adapter implementation, with modifications below)
    class ArticleContentAdapter(
        private val content: MutableList<ArticleElement>,
        private val onElementFocusChanged: (Int) -> Unit
    ) : RecyclerView.Adapter<ArticleContentAdapter.ViewHolder>() {

        // Reference to the RecyclerView (set externally or inferred from parent)
        lateinit var contentRecyclerView: RecyclerView

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val contentView: EditText = itemView.findViewById(R.id.content_text)
            val imageView: ImageView = itemView.findViewById(R.id.image_view)
            val removeIcon: ImageView = itemView.findViewById(R.id.remove_icon)

            init {
                contentView.setOnFocusChangeListener { _, hasFocus ->
                    if (hasFocus) {
                        onElementFocusChanged(adapterPosition)
                    } else {
                        // Auto-remove an empty text element only if:
                        // • There is more than one element,
                        // • This element is NOT the trailing element,
                        // • The element’s type is still PARAGRAPH.
                        val pos = adapterPosition
                        if (pos != RecyclerView.NO_POSITION &&
                            content.size > 1 &&
                            pos != content.size - 1 &&
                            contentView.text.toString().trim().isEmpty() &&
                            content[pos].type == ElementType.PARAGRAPH
                        ) {
                            removeElementAt(pos)
                        }
                    }
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_article_element, parent, false)
            if (!::contentRecyclerView.isInitialized && parent is RecyclerView) {
                contentRecyclerView = parent
            }
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val element = content[position]

            if (element.type == ElementType.IMAGE) {
                // IMAGE element: show image view and remove icon; hide text field.
                holder.contentView.visibility = View.GONE
                holder.imageView.visibility = View.VISIBLE
                holder.removeIcon.visibility = View.VISIBLE

                if (element.content.isNotEmpty()) {
                    try {
                        val uri = Uri.parse(element.content)
                        val source = ImageDecoder.createSource(holder.itemView.context.contentResolver, uri)
                        val drawable = ImageDecoder.decodeDrawable(source)
                        holder.imageView.setImageDrawable(drawable)
                    } catch (e: SecurityException) {
                        e.printStackTrace()
                        holder.imageView.setImageResource(android.R.drawable.ic_menu_gallery)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        holder.imageView.setImageResource(android.R.drawable.ic_menu_gallery)
                    }
                } else {
                    holder.imageView.setImageResource(android.R.drawable.ic_menu_gallery)
                }

                // Tapping the image launches the image picker.
                holder.imageView.setOnClickListener {
                    (holder.itemView.context as? DraftArticleActivity)
                        ?.startImagePickerForElement(holder.adapterPosition)
                }
                // Tapping the delete icon removes this image element.
                holder.removeIcon.setOnClickListener {
                    removeElementAt(holder.adapterPosition)
                }
            } else {
                // Text element: show the EditText; hide image view and remove icon.
                holder.contentView.visibility = View.VISIBLE
                holder.imageView.visibility = View.GONE
                holder.removeIcon.visibility = View.GONE

                holder.contentView.setText(Editable.Factory.getInstance().newEditable(element.content))
                holder.contentView.setLineSpacing(0f, 1f)
                when (element.type) {
                    ElementType.HEADING ->
                        holder.contentView.setTextAppearance(R.style.CustomTextAppearance_Headline)
                    ElementType.SUBHEADING ->
                        holder.contentView.setTextAppearance(R.style.CustomTextAppearance_Subhead)
                    ElementType.PARAGRAPH ->
                        holder.contentView.setTextAppearance(R.style.CustomTextAppearance_Body1)
                    ElementType.QUOTE ->
                        holder.contentView.setTextAppearance(R.style.CustomTextAppearance_Quote)
                    else -> { }
                }

                // Listen for text changes.
                holder.contentView.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                        val pos = holder.adapterPosition
                        if (pos != RecyclerView.NO_POSITION && s != null) {
                            content[pos] = content[pos].copy(content = s.toString())
                        }
                    }
                    override fun afterTextChanged(s: Editable?) {}
                })

                // Only auto-focus the trailing blank element if nothing else is focused.
                // (This will not force focus if the user has already tapped elsewhere.)
                if (position == content.size - 1 && element.content.isEmpty() &&
                    contentRecyclerView.findFocus() == null) {
                    holder.contentView.post {
                        holder.contentView.requestFocus()
                        contentRecyclerView.scrollToPosition(position)
                    }
                }

                // Handle key events:
                // ENTER: Insert a new element (always defaulting to PARAGRAPH).
                // DELETE at the start: Merge with the previous element.
                holder.contentView.setOnKeyListener { _, keyCode, event ->
                    if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN) {
                        val pos = holder.adapterPosition
                        if (pos != RecyclerView.NO_POSITION) {
                            val newElement = ArticleElement(ElementType.PARAGRAPH, "")
                            content.add(pos + 1, newElement)
                            notifyItemInserted(pos + 1)
                            ensureTrailingBlankElement()
                        }
                        return@setOnKeyListener true
                    }
                    if (keyCode == KeyEvent.KEYCODE_DEL && event.action == KeyEvent.ACTION_DOWN) {
                        val pos = holder.adapterPosition
                        if (pos != RecyclerView.NO_POSITION && holder.contentView.selectionStart == 0) {
                            if (pos > 0) {
                                val previousPos = pos - 1
                                val previousContent = content[previousPos].content
                                val currentContent = holder.contentView.text.toString()
                                removeElementAt(pos)
                                val mergedContent = previousContent + currentContent
                                updateElementContentAt(previousPos, mergedContent)
                                contentRecyclerView.post {
                                    contentRecyclerView.scrollToPosition(previousPos)
                                    val previousHolder = contentRecyclerView.findViewHolderForAdapterPosition(previousPos)
                                    if (previousHolder is ViewHolder) {
                                        previousHolder.contentView.requestFocus()
                                        previousHolder.contentView.setSelection(previousHolder.contentView.text.length)
                                    }
                                }
                                return@setOnKeyListener true
                            }
                        }
                        false
                    } else {
                        false
                    }
                }
            }
        }

        override fun getItemCount(): Int = content.size

        fun getContent(): List<ArticleElement> = content.toList()

        fun updateContent(newContent: MutableList<ArticleElement>) {
            content.clear()
            content.addAll(newContent)
            notifyDataSetChanged()
            ensureTrailingBlankElement()
        }

        fun clearContent() {
            content.clear()
            notifyDataSetChanged()
        }

        fun updateElementContentAt(position: Int, newContent: String) {
            if (position in content.indices) {
                content[position] = content[position].copy(content = newContent)
                notifyItemChanged(position)
                ensureTrailingBlankElement()
            }
        }

        fun insertElementAfter(position: Int, newElement: ArticleElement) {
            content.add(position + 1, newElement)
            notifyItemInserted(position + 1)
            ensureTrailingBlankElement()
        }

        fun updateElementTypeAt(position: Int, newType: ElementType) {
            if (position in content.indices) {
                content[position] = content[position].copy(type = newType)
                notifyItemChanged(position)
                ensureTrailingBlankElement()
            }
        }

        fun removeElementAt(position: Int) {
            if (position in content.indices) {
                content.removeAt(position)
                notifyItemRemoved(position)
                ensureTrailingBlankElement()
            }
        }

        private fun ensureTrailingBlankElement() {
            // Ensure the list always ends with a blank paragraph.
            if (content.isEmpty() ||
                content.last().type != ElementType.PARAGRAPH ||
                content.last().content.isNotEmpty()
            ) {
                content.add(ArticleElement(ElementType.PARAGRAPH, ""))
                notifyItemInserted(content.size - 1)
            }
        }
    }

}










