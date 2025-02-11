package com.example.newsreporter

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Button to open the Draft Article Page
        val draftArticleButton: Button = findViewById(R.id.btn_draft_article)
        draftArticleButton.setOnClickListener {
            val intent = Intent(this, DraftArticleActivity::class.java)
            startActivity(intent)
        }

        // Button to view the list of saved drafts
        val viewSavedDraftsButton: Button = findViewById(R.id.btn_view_saved_drafts)
        viewSavedDraftsButton.setOnClickListener {
            val intent = Intent(this, SavedDraftsActivity::class.java)
            startActivity(intent)
        }

        // Button to view the list of submitted articles
        val viewSubmittedArticlesButton: Button = findViewById(R.id.btn_view_submitted_articles)
        viewSubmittedArticlesButton.setOnClickListener {
            val intent = Intent(this, SubmittedArticlesActivity::class.java)
            startActivity(intent)
        }
    }
}

