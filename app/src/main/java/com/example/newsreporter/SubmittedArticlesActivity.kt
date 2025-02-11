package com.example.newsreporter

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SubmittedArticlesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_submitted_articles)

        // Placeholder Text
        val placeholderText: TextView = findViewById(R.id.txt_placeholder)
        placeholderText.text = "This is where submitted articles will be displayed."
    }
}