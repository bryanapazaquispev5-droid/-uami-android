package com.example.uami.sync

import android.content.Context
import android.util.Log
import com.example.uami.recipes.models.RecipeModel
import com.example.uami.isInternetAvailable
import com.example.uami.utils.RecipeCacheManager
import com.example.uami.utils.translateRecipesListAsync
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.example.uami.utils.OnDeviceTranslator
import kotlinx.coroutines.tasks.await

class UpdateManager(
    private val context: Context,
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
            return SyncResult.Success(cacheManager.loadRecipes(), wasUpdated = false)
        }

        try {
            onProgress(0.1f, if (isEs) "Comprobando actualizaciones..." else "Checking for updates...")

            // 2. Obtener recetas desde Firestore (fuente principal)
            Log.d("UPDATE_MANAGER", "Recuperando recetas desde Firestore...")
            val db = FirebaseFirestore.getInstance()
            val firestoreRecipes = mutableListOf<RecipeModel>()

            val snapshot = kotlinx.coroutines.withTimeoutOrNull(8000) {
                db.collection("recipes").get(Source.SERVER).await()
            }

            if (snapshot != null) {
                for (doc in snapshot.documents) {
                    val id = doc.getLong("id")?.toInt()
                    val name = doc.getString("name")
                    val ingredients = doc.get("ingredients") as? List<String>
                    val instructions = doc.get("instructions") as? List<String>
                    val prepTimeMinutes = doc.getLong("prepTimeMinutes")?.toInt()
                    val cookTimeMinutes = doc.getLong("cookTimeMinutes")?.toInt()
                    val difficulty = doc.getString("difficulty")
                    val cuisine = doc.getString("cuisine")
                    val mealType = doc.getString("mealType")
                    val image = doc.getString("image")
                    val rating = doc.getDouble("rating")
                    val difficultyEn = doc.getString("difficultyEn")
                    val cuisineEn = doc.getString("cuisineEn")
                    val mealTypeEn = doc.getString("mealTypeEn")

                    if (id != null) {
                        firestoreRecipes.add(
                            RecipeModel(
                                id = id,
                                name = name,
                                ingredients = ingredients,
                                instructions = instructions,
                                prepTimeMinutes = prepTimeMinutes,
                                cookTimeMinutes = cookTimeMinutes,
                                difficulty = difficulty,
                                cuisine = cuisine,
                                mealType = mealType,
                                image = image,
                                rating = rating,
                                difficultyEn = difficultyEn,
                                cuisineEn = cuisineEn,
                                mealTypeEn = mealTypeEn
                            )
                        )
                    }
                }
                Log.d("UPDATE_MANAGER", "Recuperadas ${firestoreRecipes.size} recetas desde Firestore.")
            } else {
                Log.w("UPDATE_MANAGER", "Timeout al leer Firestore.")
            }

            if (firestoreRecipes.isEmpty()) {
                if (cacheManager.hasCache()) {
                    return SyncResult.Success(cacheManager.loadRecipes(), wasUpdated = false)
                }
                throw Exception("FIRESTORE_EMPTY")
            }

            // Ordenar por ID para garantizar la estabilidad del hash
            firestoreRecipes.sortBy { it.id }
            val currentHash = firestoreRecipes.hashCode()

            // Si tenemos cache y el hash es igual, NO HAY CAMBIOS
            if (cacheManager.hasCache() && cacheManager.getApiHash() == currentHash) {
                Log.d("UPDATE_MANAGER", "No hay cambios detectados. Iniciando rápido.")
                return SyncResult.Success(cacheManager.loadRecipes(), wasUpdated = false)
            }

            Log.d("UPDATE_MANAGER", "Cambios detectados o primer arranque. Sincronizando...")
            onProgress(0.2f, if (isEs) "Traduciendo recetas..." else "Translating recipes...")

            // 3. Traducción IA
            var processedRecipes = if (isEs) {
                translateRecipesListAsync(firestoreRecipes, "es")
            } else {
                firestoreRecipes
            }

            // 4. Descargar Imágenes (ahora desde Google Drive)
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
                                val request = Request.Builder()
                                    .url(recipe.image)
                                    .header("User-Agent", "Mozilla/5.0")
                                    .build()
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
                            Log.w("UPDATE_MANAGER", "Error descargando imagen ${recipe.id}: ${e.message}")
                            // No lanzar excepción, seguir con la siguiente imagen
                        }
                    }
                }
                recipe.copy(image = newImage)
            }

            onProgress(0.95f, if (isEs) "Guardando base de datos..." else "Saving database...")

            // 5. Guardar en Caché
            if (processedRecipes.isNotEmpty()) {
                OnDeviceTranslator.clearCache()
                cacheManager.saveRecipes(processedRecipes)
                cacheManager.saveApiHash(currentHash)
            }

            onProgress(1.0f, if (isEs) "¡Listo para cocinar!" else "Ready to cook!")
            delay(1000)

            return SyncResult.Success(processedRecipes, wasUpdated = true)

        } catch (e: Exception) {
            Log.e("UPDATE_MANAGER", "Error: ${e.message}")
            if (e.message == "INTERNET_LOST" || e.message == "FIRESTORE_EMPTY" || !cacheManager.hasCache()) {
                val msg = if (isEs) "Error al conectar con la base de datos. Verifica tu conexión." else "Database connection error. Check your connection."
                return SyncResult.Error(msg)
            } else {
                return SyncResult.Success(cacheManager.loadRecipes(), wasUpdated = false)
            }
        }
    }
}

sealed class SyncResult {
    data class Success(val recipes: List<RecipeModel>, val wasUpdated: Boolean) : SyncResult()
    data class Error(val message: String) : SyncResult()
}
