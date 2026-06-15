package com.example.uami.recipes.data

import android.content.Context
import com.example.uami.recipes.models.RecipeModel
import com.example.uami.utils.RecipeCacheManager
import com.example.uami.utils.FavoriteManager
import com.example.uami.sync.UpdateManager
import com.example.uami.sync.SyncResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient

class RecipeRepository(val context: Context) {
    private val favoriteManager = FavoriteManager(context)
    private val cacheManager = RecipeCacheManager(context)

    private val _favoritos = MutableStateFlow<List<Int>>(favoriteManager.loadFavorites())
    val favoritos: StateFlow<List<Int>> = _favoritos.asStateFlow()

    private val okHttpClient = OkHttpClient.Builder().addInterceptor { chain ->
        val request = chain.request().newBuilder()
            .addHeader("User-Agent", "Mozilla/5.0")
            .build()
        chain.proceed(request)
    }.build()

    private val updateManager = UpdateManager(context, cacheManager, okHttpClient)

    suspend fun hasCache(): Boolean = cacheManager.hasCache()

    suspend fun loadRecipesFromCache(): List<RecipeModel> = cacheManager.loadRecipes()

    suspend fun checkAndSync(
        currentLanguage: String,
        isFirstRun: Boolean,
        onProgress: (Float, String) -> Unit
    ): SyncResult {
        return updateManager.checkAndSync(currentLanguage, isFirstRun, onProgress)
    }

    fun loadFavorites(): List<Int> = favoriteManager.loadFavorites()

    fun saveFavorites(favorites: List<Int>) {
        favoriteManager.saveFavorites(favorites)
        _favoritos.value = favorites
    }
}
