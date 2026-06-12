package com.example.lab09.utils

import android.content.Context
import com.example.lab09.ejercicio1.models.RecipeModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

class RecipeCacheManager(context: Context) {
    private val gson = Gson()
    private val cacheFile = File(context.filesDir, "recipes_cache.json")

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
