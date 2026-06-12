package com.example.uami.utils

import android.content.Context
import com.example.uami.recipes.models.RecipeModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

class RecipeCacheManager(context: Context) {
    private val gson = Gson()
    private val cacheFile = File(context.filesDir, "recipes_cache.json")
    private val prefs = context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)

    fun saveApiHash(hash: Int) {
        prefs.edit().putInt("api_hash", hash).apply()
    }

    fun getApiHash(): Int {
        return prefs.getInt("api_hash", 0)
    }

    fun saveRecipes(recipes: List<RecipeModel>) {
        try {
            val json = gson.toJson(recipes)
            cacheFile.writeText(json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadRecipes(): List<RecipeModel> {
        return try {
            if (cacheFile.exists()) {
                val json = cacheFile.readText()
                val type = object : TypeToken<List<RecipeModel>>() {}.type
                gson.fromJson(json, type)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun hasCache(): Boolean {
        if (!cacheFile.exists()) return false
        try {
            val content = cacheFile.readText().trim()
            return content.isNotEmpty() && content != "[]" && content != "{}"
        } catch (e: Exception) {
            return false
        }
    }
    
    fun clearCache() {
        if (cacheFile.exists()) {
            cacheFile.delete()
        }
    }
}
