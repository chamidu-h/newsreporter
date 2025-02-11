package com.example.newsreporter

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

@Entity(tableName = "drafts")
@TypeConverters(ArticleElementConverter::class)
data class Draft(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: List<ArticleElement>,
    val status: String,
)




