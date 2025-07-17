package com.example.newsreporter

import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileOutputStream

class DraftArticleActivity : AppCompatActivity() {

    private lateinit var database: DraftDatabase
    private lateinit var titleInput: TextInputEditText
    private lateinit var categoryDropdown: AutoCompleteTextView
    private lateinit var contentRecyclerView: RecyclerView
    private lateinit var addElementButton: MaterialButton
    private lateinit var saveDraftButton: MaterialButton
    private lateinit var submitArticleButton: MaterialButton
    private lateinit var fabPreview: FloatingActionButton
    private lateinit var toolbar: MaterialToolbar

    private var originalRecyclerViewPaddingBottom = 0

    private lateinit var bottomAppBar: BottomAppBar
    private var defaultBottomAppBarY = 0f
    private var isKeyboardVisible = false
    private var keyboardHeight = 0

    private var draftId: Int = -1
    private var isEditingFromRejected: Boolean = false  // NEW: Track if editing from rejected article
    private var rejectedArticleId: Long = -1L          // NEW: Store rejected article ID
    private lateinit var contentAdapter: ArticleContentAdapter
    private var currentFocusedPosition: Int = 0

    // Used to remember which element needs an image update.
    private var imageElementPosition: Int = -1

    // Single image picker launcher instance.
    private lateinit var imagePickerLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_draft_article)

        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        initializeViews()
        setupToolbar()
        setupCategoryDropdown()
        setupRecyclerView()
        setupListeners()
        setupImagePickerLauncher()
        setupKeyboardAwareBottomBar()

        // Request focus on the title field
        titleInput.requestFocus()
        titleInput.post {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(titleInput, InputMethodManager.SHOW_IMPLICIT)
        }

        // NEW: Enhanced intent handling for different sources
        handleIntentData()
    }

    /**
     * NEW: Enhanced method to handle different intent sources
     */
    private fun handleIntentData() {
        // Check for rejected article editing (from RejectedArticlesActivity)
        val rejectedArticleIdStr = intent.getStringExtra("REJECTED_ARTICLE_ID")
        if (!rejectedArticleIdStr.isNullOrEmpty()) {
            val rejectedId = rejectedArticleIdStr.toLongOrNull()
            if (rejectedId != null) {
                isEditingFromRejected = true
                rejectedArticleId = rejectedId
                loadRejectedArticleForEditing(rejectedId)
                return
            }
        }

        // Check for regular article preview (from submitted articles)
        val articleIdStr = intent.getStringExtra("ARTICLE_ID")
        if (!articleIdStr.isNullOrEmpty()) {
            val articleId = articleIdStr.toLongOrNull()
            if (articleId != null) {
                loadArticleDetailsFromBackend(articleId)
                return
            } else {
                showToast("Invalid article ID")
                return
            }
        }

        // Check for draft editing (from SavedDraftsActivity)
        draftId = intent.getIntExtra("DRAFT_ID", -1)
        if (draftId != -1) {
            loadDraft(draftId)
        }
    }

    /**
     * NEW: Load rejected article for editing
     */
    private fun loadRejectedArticleForEditing(articleId: Long) {
        RetrofitClient.articleApi.getArticleById(articleId)
            .enqueue(object : Callback<ArticleSubmissionResponse> {
                override fun onResponse(
                    call: Call<ArticleSubmissionResponse>,
                    response: Response<ArticleSubmissionResponse>
                ) {
                    if (response.isSuccessful) {
                        val article = response.body()
                        if (article != null) {
                            // Populate UI fields with the rejected article details
                            titleInput.setText(article.title)
                            categoryDropdown.setText(article.category, false)

                            try {
                                val gson = Gson()
                                val contentList: MutableList<ArticleElement> = gson.fromJson(
                                    article.content,
                                    object : TypeToken<MutableList<ArticleElement>>() {}.type
                                )
                                contentAdapter.updateContent(contentList)
                            } catch (e: Exception) {
                                Log.e("DraftArticle", "Error parsing rejected article content", e)
                                contentAdapter.updateContent(mutableListOf(ArticleElement(ElementType.PARAGRAPH, "")))
                            }

                            // Update UI to show this is editing a rejected article
                            supportActionBar?.title = "Edit Rejected Article"
                            submitArticleButton.text = "Resubmit"
                        } else {
                            showToast("Rejected article details not available")
                            finish()
                        }
                    } else {
                        showToast("Error loading rejected article: ${response.code()}")
                        finish()
                    }
                }

                override fun onFailure(call: Call<ArticleSubmissionResponse>, t: Throwable) {
                    showToast("Network error: ${t.localizedMessage}")
                    finish()
                }
            })
    }

    private fun initializeViews() {
        titleInput = findViewById(R.id.article_title)
        categoryDropdown = findViewById(R.id.category_dropdown)
        contentRecyclerView = findViewById(R.id.content_recycler_view)
        addElementButton = findViewById(R.id.btn_add_element)
        saveDraftButton = findViewById(R.id.btn_save_draft)
        submitArticleButton = findViewById(R.id.btn_submit_article)
        fabPreview = findViewById(R.id.fab_preview)
        toolbar = findViewById(R.id.toolbar)
        database = DraftDatabase.getDatabase(this)

        bottomAppBar = findViewById(R.id.bottomAppBar)
    }

    private fun setupKeyboardAwareBottomBar() {
        bottomAppBar.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                defaultBottomAppBarY = bottomAppBar.y
                bottomAppBar.viewTreeObserver.removeOnGlobalLayoutListener(this)
                setupKeyboardVisibilityListener()
            }
        })
    }

    private fun setupKeyboardVisibilityListener() {
        val rootView = findViewById<View>(android.R.id.content)

        ViewCompat.setOnApplyWindowInsetsListener(rootView) { _, insets ->
            val isKeyboardVisibleNow = insets.isVisible(WindowInsetsCompat.Type.ime())
            val keyboardHeightNow = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom

            if (isKeyboardVisibleNow != isKeyboardVisible || (isKeyboardVisibleNow && keyboardHeightNow != keyboardHeight)) {
                isKeyboardVisible = isKeyboardVisibleNow
                keyboardHeight = keyboardHeightNow

                animateBottomBarWithKeyboard(isKeyboardVisible, keyboardHeight)

                contentRecyclerView.setPadding(
                    contentRecyclerView.paddingLeft,
                    contentRecyclerView.paddingTop,
                    contentRecyclerView.paddingRight,
                    if (isKeyboardVisibleNow) originalRecyclerViewPaddingBottom + keyboardHeightNow else originalRecyclerViewPaddingBottom
                )
            }
            insets
        }
    }

    private fun animateBottomBarWithKeyboard(keyboardVisible: Boolean, keyboardHeight: Int) {
        val startY = bottomAppBar.y
        val endY = if (keyboardVisible) {
            defaultBottomAppBarY - keyboardHeight
        } else {
            defaultBottomAppBarY
        }

        val animator = ValueAnimator.ofFloat(startY, endY)
        animator.duration = 250

        animator.addUpdateListener { animation ->
            val value = animation.animatedValue as Float
            bottomAppBar.y = value
        }

        animator.start()
    }

    override fun onBackPressed() {
        if (isKeyboardVisible) {
            val windowInsetsController = ViewCompat.getWindowInsetsController(findViewById(android.R.id.content))
            windowInsetsController?.hide(WindowInsetsCompat.Type.ime())
        } else {
            super.onBackPressed()
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupCategoryDropdown() {
        val categories = resources.getStringArray(R.array.article_categories)
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, categories)
        categoryDropdown.setAdapter(adapter)
    }

    private fun setupRecyclerView() {
        val defaultContent = mutableListOf(ArticleElement(ElementType.PARAGRAPH, ""))
        contentAdapter = ArticleContentAdapter(defaultContent) { position ->
            currentFocusedPosition = position
        }

        // CRITICAL: Add stable IDs to prevent view recycling issues
        contentAdapter.setHasStableIds(true)

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
        submitArticleButton.setOnClickListener {
            submitArticleToBackend()
        }
        fabPreview.setOnClickListener {
            previewArticle()
        }
    }

    private fun copyUriToPersistentFile(context: Context, uri: Uri): Uri? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val dir = File(context.filesDir, "draft_images")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            val file = File(dir, "draft_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            inputStream.close()
            Uri.fromFile(file)
        } catch (e: Exception) {
            Log.e("DraftArticle", "Error copying image to persistent storage: ${e.message}")
            null
        }
    }

    private fun setupImagePickerLauncher() {
        imagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val data = result.data
                val uri: Uri? = data?.data
                if (uri != null && imageElementPosition != -1) {
                    val currentImagePos = imageElementPosition

                    val dataFlags = data.flags
                    var flags = dataFlags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    if (flags == 0) {
                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    }
                    try {
                        contentResolver.takePersistableUriPermission(uri, flags)
                    } catch (e: SecurityException) {
                        e.printStackTrace()
                    }

                    val persistentUri = copyUriToPersistentFile(this, uri)
                    if (persistentUri != null) {
                        contentAdapter.updateElementContentAt(currentImagePos, persistentUri.toString())
                    } else {
                        contentAdapter.updateElementContentAt(currentImagePos, uri.toString())
                    }

                    ImageUploader.uploadImage(this, uri) { serverUrl ->
                        runOnUiThread {
                            if (serverUrl != null) {
                                contentAdapter.updateElementContentAt(currentImagePos, serverUrl)
                            } else {
                                Toast.makeText(this, "Failed to upload image", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
            imageElementPosition = -1
        }
    }

    fun startImagePickerForElement(position: Int) {
        imageElementPosition = position
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        imagePickerLauncher.launch(intent)
    }

    private fun showElementSelectorDialog() {
        val types = ElementType.values().filterNot { it == ElementType.VIDEO }
        val typeNames = types.map { it.name.lowercase().replaceFirstChar { char -> char.uppercase() } }

        val dialogView = layoutInflater.inflate(R.layout.dialog_element_selector, null)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.rvElementTypes)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .create()

        recyclerView.adapter = ElementTypeAdapter(typeNames) { which ->
            val selectedType = types[which]
            insertElementAtCurrentPosition(selectedType)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun insertElementAtCurrentPosition(type: ElementType) {
        val currentElement = contentAdapter.getContent().getOrNull(currentFocusedPosition)
        if (currentElement != null) {
            val viewHolder = contentRecyclerView.findViewHolderForAdapterPosition(currentFocusedPosition)
            if (viewHolder is ArticleContentAdapter.ViewHolder) {
                val editText = viewHolder.itemView.findViewById<EditText>(R.id.content_text)
                val currentContent = editText.text.toString()
                if (currentContent.trim().isEmpty()) {
                    contentAdapter.updateElementTypeAt(currentFocusedPosition, type)
                    if (type == ElementType.IMAGE) {
                        startImagePickerForElement(currentFocusedPosition)
                    }
                    return
                }
                val cursorPosition = editText.selectionStart
                val textBeforeCursor = currentContent.substring(0, cursorPosition)
                val textAfterCursor = currentContent.substring(cursorPosition)
                contentAdapter.updateElementContentAt(currentFocusedPosition, textBeforeCursor)
                val newElement = ArticleElement(type, textAfterCursor)
                contentAdapter.insertElementAfter(currentFocusedPosition, newElement)
                if (type == ElementType.IMAGE) {
                    startImagePickerForElement(currentFocusedPosition + 1)
                }
                contentRecyclerView.post {
                    contentRecyclerView.scrollToPosition(currentFocusedPosition + 1)
                    val newViewHolder = contentRecyclerView.findViewHolderForAdapterPosition(currentFocusedPosition + 1)
                    if (newViewHolder is ArticleContentAdapter.ViewHolder) {
                        newViewHolder.contentView.requestFocus()
                        newViewHolder.contentView.setSelection(0)
                    }
                }
            }
        }
    }

    private fun loadArticleDetailsFromBackend(articleId: Long) {
        RetrofitClient.articleApi.getArticleById(articleId)
            .enqueue(object : Callback<ArticleSubmissionResponse> {
                override fun onResponse(
                    call: Call<ArticleSubmissionResponse>,
                    response: Response<ArticleSubmissionResponse>
                ) {
                    if (response.isSuccessful) {
                        val article = response.body()
                        if (article != null) {
                            titleInput.setText(article.title)
                            categoryDropdown.setText(article.category, false)

                            try {
                                val gson = Gson()
                                val contentList: MutableList<ArticleElement> = gson.fromJson(
                                    article.content,
                                    object : TypeToken<MutableList<ArticleElement>>() {}.type
                                )
                                contentAdapter.updateContent(contentList)
                            } catch (e: Exception) {
                                contentAdapter.updateContent(mutableListOf(ArticleElement(ElementType.PARAGRAPH, "")))
                            }
                        } else {
                            showToast("Article details not available")
                            finish()
                        }
                    } else {
                        showToast("Error loading article: ${response.code()}")
                        finish()
                    }
                }
                override fun onFailure(call: Call<ArticleSubmissionResponse>, t: Throwable) {
                    showToast("Network error: ${t.localizedMessage}")
                    finish()
                }
            })
    }

    private fun loadDraft(draftId: Int) {
        lifecycleScope.launch {
            try {
                val draft = withContext(Dispatchers.IO) {
                    database.draftDao().getDraftById(draftId)
                }
                if (draft != null) {
                    titleInput.setText(draft.title)
                    categoryDropdown.setText(draft.category, false)
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
        val category = categoryDropdown.text.toString().trim()
        val content = contentAdapter.getContent()

        if (title.isEmpty()) {
            showToast("Title cannot be empty!")
            return
        }

        // ✅ FIX: Add validation to ensure a category is selected.
        if (category.isEmpty()) {
            showToast("Please select a category!")
            categoryDropdown.error = "Category is required" // Provides direct visual feedback
            return
        }

        // Clear any previous error message once validation passes
        categoryDropdown.error = null

        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val draft = if (draftId != -1) {
                        Draft(draftId, title, content, category, "Draft")
                    } else {
                        Draft(title = title, content = content, category = category, status = "Draft")
                    }
                    database.draftDao().saveDraft(draft)
                }
                showToast("Draft Saved!")
                if (draftId == -1) {
                    titleInput.text?.clear()
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

    /**
     * ENHANCED: Submit article with draft removal logic
     */
    private fun submitArticleToBackend() {
        val title = titleInput.text.toString().trim()
        val category = categoryDropdown.text.toString().trim()
        val contentElements = contentAdapter.getContent()

        if (title.isEmpty() || contentElements.isEmpty()) {
            Toast.makeText(this, "Title and content cannot be empty!", Toast.LENGTH_SHORT).show()
            return
        }

        // Serialize the list of ArticleElement objects to a JSON string.
        val gson = Gson()
        val contentJson = gson.toJson(contentElements)

        val reporterId = UserSessionManager.getUserId()
        if (reporterId == -1L) {
            Toast.makeText(this, "User session not found. Please log in again.", Toast.LENGTH_SHORT).show()
            return
        }

        val submissionRequest = ArticleSubmissionRequest(
            title = title,
            category = category,
            content = contentJson,
            reporterId = reporterId
        )

        // Disable submit button to prevent multiple submissions
        submitArticleButton.isEnabled = false
        submitArticleButton.text = "Submitting..."

        RetrofitClient.articleApi.submitArticle(submissionRequest).enqueue(object : Callback<ArticleSubmissionResponse> {
            override fun onResponse(call: Call<ArticleSubmissionResponse>, response: Response<ArticleSubmissionResponse>) {
                submitArticleButton.isEnabled = true
                submitArticleButton.text = if (isEditingFromRejected) "Resubmit Article" else "Submit Article"

                if (response.isSuccessful) {
                    Toast.makeText(this@DraftArticleActivity, "Article submitted successfully!", Toast.LENGTH_SHORT).show()

                    // NEW: Handle post-submission cleanup
                    handlePostSubmissionCleanup()

                } else {
                    Toast.makeText(this@DraftArticleActivity, "Submission failed: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ArticleSubmissionResponse>, t: Throwable) {
                submitArticleButton.isEnabled = true
                submitArticleButton.text = if (isEditingFromRejected) "Resubmit Article" else "Submit Article"
                Toast.makeText(this@DraftArticleActivity, "Error submitting article: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    /**
     * NEW: Handle cleanup after successful submission
     */
    private fun handlePostSubmissionCleanup() {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    // If this was a draft being edited, remove it from drafts
                    if (draftId != -1) {
                        database.draftDao().deleteDraftById(draftId)  // Changed method name
                        Log.d("DraftArticle", "Draft with ID $draftId removed after submission")
                    }
                }


                // Navigate based on the source
                when {
                    isEditingFromRejected -> {
                        // Navigate back to submitted articles (the rejected article will now show as submitted)
                        navigateToSubmittedArticles()
                    }
                    draftId != -1 -> {
                        // Was editing a draft, navigate to submitted articles
                        navigateToSubmittedArticles()
                    }
                    else -> {
                        // Was creating new article, navigate to submitted articles
                        navigateToSubmittedArticles()
                    }
                }

            } catch (e: Exception) {
                Log.e("DraftArticle", "Error during post-submission cleanup", e)
                // Even if cleanup fails, still navigate away
                navigateToSubmittedArticles()
            }
        }
    }

    /**
     * NEW: Navigate to submitted articles activity
     */
    private fun navigateToSubmittedArticles() {
        val intent = Intent(this, SubmittedArticlesActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
        finish()
    }

    private fun previewArticle() {
        val bottomSheetDialog = BottomSheetDialog(this, R.style.BottomSheetDialogTheme)
        val view = layoutInflater.inflate(R.layout.layout_article_preview, null)

        // Initialize views
        val titleView = view.findViewById<TextView>(R.id.tv_article_title)
        val categoryView = view.findViewById<TextView>(R.id.tv_article_category)
        val contentRecyclerView = view.findViewById<RecyclerView>(R.id.preview_content_recycler)

        // Set up the preview content
        titleView.text = titleInput.text.toString()
        categoryView.text = categoryDropdown.text.toString()

        // Filter out empty elements and the trailing blank element
        val previewContent = contentAdapter.getContent()
            .filter { it.content.isNotEmpty() || it.type == ElementType.IMAGE }

        // Set up the preview recycler view
        contentRecyclerView.layoutManager = LinearLayoutManager(this)
        contentRecyclerView.adapter = PreviewAdapter(previewContent)

        // Show the preview
        bottomSheetDialog.setContentView(view)
        bottomSheetDialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        bottomSheetDialog.show()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    // --- ArticleContentAdapter (inner class) ---
    class ArticleContentAdapter(
        private val content: MutableList<ArticleElement>,
        private val onElementFocusChanged: (Int) -> Unit
    ) : RecyclerView.Adapter<ArticleContentAdapter.ViewHolder>() {

        var autoFocusTrailingElement: Boolean = true
        lateinit var contentRecyclerView: RecyclerView

        // ViewHolder holds references to the views for each item.
        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            // Views for text elements
            val contentView: EditText = itemView.findViewById(R.id.content_text)

            // Views for image elements
            val imageContainer: View = itemView.findViewById(R.id.image_container) // Key reference to the parent container
            val imageView: ImageView = itemView.findViewById(R.id.image_view)
            val removeIcon: ImageView = itemView.findViewById(R.id.remove_icon)

            init {
                // Your existing focus change listener
                contentView.setOnFocusChangeListener { _, hasFocus ->
                    if (hasFocus) {
                        onElementFocusChanged(adapterPosition)
                    } else {
                        val pos = adapterPosition
                        if (
                            pos != RecyclerView.NO_POSITION &&
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

            // --- BINDING LOGIC BASED ON ELEMENT TYPE ---
            if (element.type == ElementType.IMAGE) {
                // Configure for an IMAGE element
                holder.contentView.visibility = View.GONE
                holder.imageContainer.visibility = View.VISIBLE // FIX: Show the entire image container

                loadImageIntoView(holder.imageView, element.content)

                // Click handlers for the image
                holder.imageView.setOnClickListener {
                    (holder.itemView.context as? DraftArticleActivity)
                        ?.startImagePickerForElement(holder.adapterPosition)
                }

                holder.removeIcon.setOnClickListener {
                    removeElementAt(holder.adapterPosition)
                }

            } else {
                // Configure for a TEXT element (Paragraph, Heading, etc.)
                holder.contentView.visibility = View.VISIBLE
                holder.imageContainer.visibility = View.GONE // FIX: Hide the image container

                // Set text content
                holder.contentView.setText(Editable.Factory.getInstance().newEditable(element.content))
                holder.contentView.setLineSpacing(0f, 1f)

                // Apply text appearance based on type
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

                // Listen for text changes to update the data model
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

                // Auto-focus logic for trailing blank element
                if (
                    position == content.size - 1 &&
                    element.content.isEmpty() &&
                    autoFocusTrailingElement &&
                    contentRecyclerView.findFocus() == null
                ) {
                    val activity = holder.itemView.context as? DraftArticleActivity
                    val currentFocusView = activity?.currentFocus
                    if (currentFocusView?.id != R.id.article_title) {
                        holder.contentView.post {
                            holder.contentView.requestFocus()
                            contentRecyclerView.scrollToPosition(position)
                        }
                    }
                }

                // Handle Enter and Delete key events for text editing
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

        private fun loadImageIntoView(imageView: ImageView, imageContent: String) {
            Glide.with(imageView.context).clear(imageView)

            if (imageContent.isEmpty()) {
                imageView.setImageResource(android.R.drawable.ic_menu_gallery)
                return
            }

            val contentValue = imageContent.trim()
            Log.d("ImageLoading", "Loading image: $contentValue")

            imageView.scaleType = ImageView.ScaleType.CENTER_CROP

            try {
                val glideRequest = when {
                    contentValue.startsWith("http", ignoreCase = true) -> Glide.with(imageView.context).load(contentValue)
                    contentValue.startsWith("content://", ignoreCase = true) -> Glide.with(imageView.context).load(Uri.parse(contentValue))
                    contentValue.startsWith("file://", ignoreCase = true) -> Glide.with(imageView.context).load(Uri.parse(contentValue))
                    contentValue.startsWith("/") -> {
                        val file = File(contentValue)
                        if (file.exists()) Glide.with(imageView.context).load(file)
                        else {
                            Log.e("ImageLoading", "File does not exist: $contentValue")
                            imageView.setImageResource(android.R.drawable.ic_menu_report_image)
                            return
                        }
                    }
                    else -> Glide.with(imageView.context).load(Uri.parse(contentValue))
                }

                glideRequest
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_report_image)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .skipMemoryCache(false)
                    .centerCrop()
                    .into(imageView)

            } catch (e: Exception) {
                Log.e("ImageLoading", "Exception loading image: $contentValue", e)
                imageView.setImageResource(android.R.drawable.ic_menu_report_image)
            }
        }

        // --- ADAPTER HELPER METHODS ---
        override fun getItemCount(): Int = content.size

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

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
            if (
                content.isEmpty() ||
                content.last().type != ElementType.PARAGRAPH ||
                content.last().content.isNotEmpty()
            ) {
                content.add(ArticleElement(ElementType.PARAGRAPH, ""))
                notifyItemInserted(content.size - 1)
            }
        }
    }


}
