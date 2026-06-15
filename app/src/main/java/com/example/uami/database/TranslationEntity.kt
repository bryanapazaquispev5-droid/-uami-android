package com.example.uami.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "translations")
data class TranslationEntity(
    @PrimaryKey val originalText: String,
    val translatedText: String
)
