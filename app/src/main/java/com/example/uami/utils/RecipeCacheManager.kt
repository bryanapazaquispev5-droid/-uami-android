package com.example.uami.utils

import android.content.Context
import com.example.uami.recipes.models.RecipeModel
import com.example.uami.database.UamiDatabase
import com.example.uami.database.RecipeEntity

class RecipeCacheManager(context: Context) {
    private val database = UamiDatabase.getDatabase(context)
    private val recipeDao = database.recipeDao()
    private val prefs = context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)

    fun saveApiHash(hash: Int) {
        prefs.edit().putInt("api_hash", hash).apply()
    }

    fun getApiHash(): Int {
        return prefs.getInt("api_hash", 0)
    }

    suspend fun saveRecipes(recipes: List<RecipeModel>) {
        val entities = recipes.map { RecipeEntity.fromModel(it) }
        recipeDao.updateRecipes(entities)
    }

    suspend fun loadRecipes(): List<RecipeModel> {
        return recipeDao.getAllRecipes().map { it.toModel() }
    }

    suspend fun hasCache(): Boolean {
        return recipeDao.getAllRecipes().isNotEmpty()
    }
    
    suspend fun clearCache() {
        recipeDao.clearAll()
    }
}
