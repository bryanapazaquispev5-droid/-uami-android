package com.example.uami.sync

import android.content.Context
import android.util.Log
import com.example.uami.recipes.models.RecipeModel
import com.example.uami.recipes.remote.RecipeApiService
import com.example.uami.isInternetAvailable
import com.example.uami.utils.RecipeCacheManager
import com.example.uami.utils.translateRecipesListAsync
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

class UpdateManager(
    private val context: Context,
    private val apiService: RecipeApiService,
    private val cacheManager: RecipeCacheManager,
    private val okHttpClient: OkHttpClient
) {
    suspend fun checkAndSync(
        currentLanguage: String,
        isFirstRun: Boolean,
        onProgress: (Float, String) -> Unit
    ): SyncResult {
        val isEs = currentLanguage == "es"

        // 1. Si no hay internet
        if (!isInternetAvailable(context)) {
            if (isFirstRun || !cacheManager.hasCache()) {
                val msg = if (isEs) "No hay conexión a internet. Verifica tu red." else "No internet connection. Check your network."
                return SyncResult.Error(msg)
            }
            // Si hay caché y no hay internet, abrimos normal
            return SyncResult.Success(cacheManager.loadRecipes(), wasUpdated = false)
        }

        try {
            onProgress(0.1f, if (isEs) "Comprobando actualizaciones..." else "Checking for updates...")
            
            // 2. Descargar RAW de la API para comparar
            val rawRecipes = apiService.getRecipes()
            if (rawRecipes.isEmpty()) throw Exception("API_EMPTY")

            // Generar Hash basado en los datos crudos (JSON/Contenido)
            // Si cambian ingredientes, nombres o se agregan recetas, el hash cambia.
            val currentHash = rawRecipes.hashCode()

            // Si no es el primer arranque y el hash es igual, NO HAY CAMBIOS
            if (!isFirstRun && cacheManager.hasCache() && cacheManager.getApiHash() == currentHash) {
                Log.d("UPDATE_MANAGER", "No hay cambios detectados. Iniciando rápido.")
                return SyncResult.Success(cacheManager.loadRecipes(), wasUpdated = false)
            }

            Log.d("UPDATE_MANAGER", "Cambios detectados o primer arranque. Iniciando sincronización...")
            onProgress(0.2f, if (isEs) "Traduciendo recetas..." else "Translating recipes...")

            // 3. Traducción IA
            var processedRecipes = if (isEs) {
                translateRecipesListAsync(rawRecipes, "es")
            } else {
                rawRecipes
            }

            // 4. Descargar Imágenes
            val total = processedRecipes.size
            processedRecipes = processedRecipes.mapIndexed { index, recipe ->
                if (!isInternetAvailable(context)) throw Exception("INTERNET_LOST")

                onProgress(0.2f + (0.7f * index / total), if (isEs) "Descargando imágenes... ($index/$total)" else "Downloading images... ($index/$total)")

                var newImage = recipe.image
                if (recipe.image != null && recipe.image.startsWith("http")) {
                    val fileName = "img_${recipe.id}.jpg"
                    val file = File(context.filesDir, fileName)

                    if (file.exists() && file.length() > 0) {
                        newImage = "file://${file.absolutePath}"
                    } else {
                        try {
                            withContext(Dispatchers.IO) {
                                val request = Request.Builder().url(recipe.image).build()
                                val response = okHttpClient.newCall(request).execute()
                                if (response.isSuccessful) {
                                    val bytes = response.body?.bytes()
                                    if (bytes != null) {
                                        file.writeBytes(bytes)
                                        newImage = "file://${file.absolutePath}"
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            throw Exception("INTERNET_LOST")
                        }
                    }
                }
                recipe.copy(image = newImage)
            }

            onProgress(0.95f, if (isEs) "Guardando base de datos..." else "Saving database...")

            // 5. Guardar en Caché y actualizar Hash
            if (processedRecipes.isNotEmpty()) {
                cacheManager.saveRecipes(processedRecipes)
                cacheManager.saveApiHash(currentHash)
            }

            onProgress(1.0f, if (isEs) "¡Listo para cocinar!" else "Ready to cook!")
            delay(1000)

            return SyncResult.Success(processedRecipes, wasUpdated = true)

        } catch (e: Exception) {
            Log.e("UPDATE_MANAGER", "Error: ${e.message}")
            if (e.message == "INTERNET_LOST" || !cacheManager.hasCache()) {
                val msg = if (isEs) "Conexión interrumpida o API inalcanzable. No se pudo terminar la descarga." else "Connection interrupted or API unreachable. Download failed."
                return SyncResult.Error(msg)
            } else {
                // Si falla pero hay caché viejo, iniciamos con el caché viejo (ignorar update)
                return SyncResult.Success(cacheManager.loadRecipes(), wasUpdated = false)
            }
        }
    }
}

sealed class SyncResult {
    data class Success(val recipes: List<RecipeModel>, val wasUpdated: Boolean) : SyncResult()
    data class Error(val message: String) : SyncResult()
}
