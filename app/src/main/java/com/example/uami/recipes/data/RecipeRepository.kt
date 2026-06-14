package com.example.uami.recipes.data

import android.content.Context
import com.example.uami.recipes.models.RecipeModel
import com.example.uami.recipes.remote.RecipeApiService
import com.example.uami.utils.RecipeCacheManager
import com.example.uami.utils.FavoriteManager
import com.example.uami.sync.UpdateManager
import com.example.uami.sync.SyncResult
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RecipeRepository(val context: Context) {
    private val favoriteManager = FavoriteManager(context)
    private val cacheManager = RecipeCacheManager(context)

    private val okHttpClient = OkHttpClient.Builder().addInterceptor { chain ->
        val request = chain.request().newBuilder()
            .addHeader("Bypass-Tunnel-Reminder", "true")
            .addHeader("User-Agent", "Mozilla/5.0")
            .build()
        chain.proceed(request)
    }.build()

    val servicioRecipes: RecipeApiService = Retrofit.Builder()
        .baseUrl("https://recetasc24.loca.lt/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(RecipeApiService::class.java)

    private val updateManager = UpdateManager(context, servicioRecipes, cacheManager, okHttpClient)

    fun hasCache(): Boolean = cacheManager.hasCache()

    fun loadRecipesFromCache(): List<RecipeModel> = cacheManager.loadRecipes()

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
    }
}
