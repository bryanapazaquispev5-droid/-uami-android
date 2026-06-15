package com.example.uami.utils

import android.content.Context
import com.example.uami.database.UamiDatabase
import com.example.uami.database.TranslationEntity
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

object OnDeviceTranslator {
    private val options = TranslatorOptions.Builder()
        .setSourceLanguage(TranslateLanguage.ENGLISH)
        .setTargetLanguage(TranslateLanguage.SPANISH)
        .build()
    
    private val translator = Translation.getClient(options)
    private var isModelDownloaded = false
    
    private val memoryCache = mutableMapOf<String, String>()
    private var database: UamiDatabase? = null

    fun init(context: Context) {
        database = UamiDatabase.getDatabase(context)
    }

    suspend fun translate(text: String?): String = withContext(Dispatchers.IO) {
        if (text.isNullOrBlank()) return@withContext ""
        
        // 1. Check memory cache
        synchronized(memoryCache) {
            memoryCache[text]?.let { return@withContext it }
        }
        
        // 2. Check Room database
        val translationDao = database?.translationDao()
        val cachedTranslation = translationDao?.getTranslation(text)
        if (cachedTranslation != null) {
            synchronized(memoryCache) {
                memoryCache[text] = cachedTranslation
            }
            return@withContext cachedTranslation
        }
        
        // 3. Perform ML Kit translation
        try {
            ensureModelDownloaded()
            val translatedText = translator.translate(text).await()
            
            // Save to memory cache and Room
            synchronized(memoryCache) {
                memoryCache[text] = translatedText
            }
            translationDao?.insert(TranslationEntity(text, translatedText))
            
            return@withContext translatedText
        } catch (e: Exception) {
            return@withContext text // Fallback
        }
    }

    private suspend fun ensureModelDownloaded() {
        if (!isModelDownloaded) {
            val conditions = DownloadConditions.Builder()
                .build()
            translator.downloadModelIfNeeded(conditions).await()
            isModelDownloaded = true
        }
    }
}
