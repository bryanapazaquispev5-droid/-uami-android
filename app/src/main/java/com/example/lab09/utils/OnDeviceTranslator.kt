package com.example.lab09.utils

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.tasks.await
import java.io.File

object OnDeviceTranslator {
    private val options = TranslatorOptions.Builder()
        .setSourceLanguage(TranslateLanguage.ENGLISH)
        .setTargetLanguage(TranslateLanguage.SPANISH)
        .build()
    
    private val translator = Translation.getClient(options)
    private var isModelDownloaded = false
    
    private val memoryCache = mutableMapOf<String, String>()
    private val gson = Gson()
    private var cacheFile: File? = null

    fun init(context: Context) {
        // Inicializar archivo de caché manual (Bypass de Room)
        cacheFile = File(context.filesDir, "translations_cache.json")
        loadCacheFromFile()
    }

    private fun loadCacheFromFile() {
        try {
            if (cacheFile?.exists() == true) {
                val json = cacheFile?.readText()
                val type = object : TypeToken<Map<String, String>>() {}.type
                val savedMap: Map<String, String> = gson.fromJson(json, type)
                memoryCache.putAll(savedMap)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveCacheToFile() {
        try {
            val json = gson.toJson(memoryCache)
            cacheFile?.writeText(json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun translate(text: String?): String {
        if (text.isNullOrBlank()) return ""
        
        // 1. Buscar en memoria (cargada desde archivo al inicio)
        memoryCache[text]?.let { return it }
        
        try {
            ensureModelDownloaded()
            val translatedText = translator.translate(text).await()
            
            // 2. Guardar en memoria y persistir en archivo
            memoryCache[text] = translatedText
            saveCacheToFile()
            
            return translatedText
        } catch (e: Exception) {
            return text // Fallback
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
