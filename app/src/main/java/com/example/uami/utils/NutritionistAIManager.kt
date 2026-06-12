package com.example.uami.utils

import android.content.Context
import android.net.Uri
import com.example.uami.recipes.models.RecipeModel
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class NutritionistAIManager(private val context: Context) {
    
    companion object {
        const val MODEL_FILENAME = "gemma-2b-it-cpu-int4.bin"
        // Public stable URL of Google Gemma-2B-it CPU Quantized model on Hugging Face (non-gated mirror)
        const val DEFAULT_MODEL_URL = "https://huggingface.co/metsman/gemma-2b-it-cpu-int4-org/resolve/main/gemma-2b-it-cpu-int4.bin"
    }

    private val modelFile: File
        get() = File(context.filesDir, MODEL_FILENAME)

    private var llmInference: LlmInference? = null

    private val _downloadProgress = MutableStateFlow<Float>(-1f)
    val downloadProgress: StateFlow<Float> = _downloadProgress

    private val _isModelReady = MutableStateFlow(false)
    val isModelReady: StateFlow<Boolean> = _isModelReady

    private val _isInitializing = MutableStateFlow(false)
    val isInitializing: StateFlow<Boolean> = _isInitializing

    init {
        checkModelAvailability()
    }

    fun checkModelAvailability() {
        val ready = modelFile.exists() && modelFile.length() > 100 * 1024 * 1024 // At least 100MB
        _isModelReady.value = ready
    }

    // Initializes the local LLM engine
    suspend fun initializeLLM(): Boolean = withContext(Dispatchers.Default) {
        if (llmInference != null) return@withContext true
        if (!modelFile.exists()) return@withContext false

        _isInitializing.value = true
        try {
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelFile.absolutePath)
                .setMaxTokens(512)
                .setTemperature(0.7f)
                .build()
            llmInference = LlmInference.createFromOptions(context, options)
            _isInitializing.value = false
            true
        } catch (e: Exception) {
            e.printStackTrace()
            _isInitializing.value = false
            false
        }
    }

    // Releases LLM memory
    fun close() {
        llmInference?.close()
        llmInference = null
    }

    // Generates conversational responses using the local LLM
    suspend fun generateResponse(prompt: String, favoriteRecipes: List<RecipeModel>, lang: String): String = withContext(Dispatchers.Default) {
        val inference = llmInference ?: return@withContext if (lang == "es") {
            "El nutriólogo offline no está listo. Por favor, asegúrate de descargar o importar el modelo primero."
        } else {
            "Offline AI nutritionist is not ready. Please make sure to download or import the model first."
        }

        val isEs = lang == "es"
        
        // Build contextual rules from user liked recipe profiles
        val favContext = if (favoriteRecipes.isNotEmpty()) {
            val listText = favoriteRecipes.joinToString("; ") { recipe ->
                "${recipe.name ?: ""} (${recipe.cuisine ?: ""}, ingredients: ${recipe.ingredients?.joinToString(", ") ?: ""})"
            }
            if (isEs) {
                "El usuario tiene estas recetas favoritas: $listText. Adapta tus sugerencias e intenta incorporar ingredientes o estilos de estas recetas si es relevante."
            } else {
                "The user has these favorite recipes: $listText. Adapt your suggestions and try to incorporate ingredients or styles from these recipes if relevant."
            }
        } else {
            if (isEs) {
                "El usuario no tiene recetas favoritas aún. Recomienda comidas saludables variadas."
            } else {
                "The user has no favorite recipes yet. Recommend general healthy and varied meals."
            }
        }

        val systemPrompt = if (isEs) {
            "Eres un Nutriólogo Profesional de IA local. Eres experto en alimentación saludable, dietas balanceadas y cocina. Responde de forma amable, clara y concisa en español. Contexto: $favContext\n\nUsuario: $prompt\nNutriólogo IA:"
        } else {
            "You are a local Professional AI Nutritionist. You are an expert in healthy eating, balanced diets, and cooking. Respond in a friendly, clear, and concise manner in English. Context: $favContext\n\nUser: $prompt\nAI Nutritionist:"
        }

        try {
            inference.generateResponse(systemPrompt)
        } catch (e: Exception) {
            e.printStackTrace()
            if (isEs) "Error en la inferencia local de la IA: ${e.localizedMessage}" else "Local AI inference error: ${e.localizedMessage}"
        }
    }

    // Downloads the model from the specified URL in a background worker
    suspend fun downloadModel(urlStr: String = DEFAULT_MODEL_URL): Boolean = withContext(Dispatchers.IO) {
        _downloadProgress.value = 0f
        var input: BufferedInputStream? = null
        var output: FileOutputStream? = null
        var connection: HttpURLConnection? = null
        try {
            val url = URL(urlStr)
            connection = url.openConnection() as HttpURLConnection
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                _downloadProgress.value = -1f
                return@withContext false
            }

            val fileLength = connection.contentLengthLong
            input = BufferedInputStream(url.openStream(), 8192)
            
            // Download to a temporary file first to prevent corruption if interrupted
            val tempFile = File(context.filesDir, "$MODEL_FILENAME.tmp")
            output = FileOutputStream(tempFile)

            val data = ByteArray(8192)
            var total: Long = 0
            var count: Int
            while (input.read(data).also { count = it } != -1) {
                total += count
                if (fileLength > 0) {
                    _downloadProgress.value = total.toFloat() / fileLength
                }
                output.write(data, 0, count)
            }

            output.flush()
            output.close()
            input.close()

            // Safe atomic rename
            if (tempFile.exists()) {
                if (modelFile.exists()) modelFile.delete()
                tempFile.renameTo(modelFile)
            }

            checkModelAvailability()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            _downloadProgress.value = -1f
            false
        } finally {
            connection?.disconnect()
        }
    }

    // Imports the model binary from a local URI (selected from device file chooser)
    suspend fun importModel(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val tempFile = File(context.filesDir, "$MODEL_FILENAME.tmp")
                FileOutputStream(tempFile).use { outputStream ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (inputStream.read(buffer).also { read = it } != -1) {
                        outputStream.write(buffer, 0, read)
                    }
                    outputStream.flush()
                }

                if (tempFile.exists()) {
                    if (modelFile.exists()) modelFile.delete()
                    tempFile.renameTo(modelFile)
                }
                checkModelAvailability()
                true
            } ?: false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun resetDownloadProgress() {
        _downloadProgress.value = -1f
    }

    // 🍎 OFFLINE EXPERT DIET ENGINE
    // Creates a highly customized daily/weekly meal plan locally using database resources
    fun generateOfflineDietPlan(allRecipes: List<RecipeModel>, favoriteIds: List<Int>, isEs: Boolean): List<DietDayPlan> {
        val favoriteRecipes = allRecipes.filter { favoriteIds.contains(it.id) }
        
        // Categorize recipes by meal type
        val breakfasts = allRecipes.filter { it.mealType?.contains("Breakfast", ignoreCase = true) == true }
        val lunchAndDinners = allRecipes.filter { 
            it.mealType?.contains("Lunch", ignoreCase = true) == true || 
            it.mealType?.contains("Dinner", ignoreCase = true) == true 
        }

        // Fallbacks if lists are empty
        val fallbackBreakfasts = breakfasts.ifEmpty { allRecipes }
        val fallbackLunches = lunchAndDinners.ifEmpty { allRecipes }

        val days = if (isEs) {
            listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")
        } else {
            listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
        }

        val plans = mutableListOf<DietDayPlan>()
        val rand = java.util.Random()

        // Prioritize liked cuisines
        val favoriteCuisines = favoriteRecipes.mapNotNull { it.cuisine }.distinct()

        for (day in days) {
            val breakfast = selectMeal(fallbackBreakfasts, favoriteRecipes, favoriteCuisines, rand)
            val lunch = selectMeal(fallbackLunches, favoriteRecipes, favoriteCuisines, rand)
            var dinner = selectMeal(fallbackLunches, favoriteRecipes, favoriteCuisines, rand)
            if (dinner.id == lunch.id && fallbackLunches.size > 1) {
                val alternativeList = fallbackLunches.filter { it.id != lunch.id }
                dinner = selectMeal(alternativeList, favoriteRecipes, favoriteCuisines, rand)
            }

            plans.add(DietDayPlan(day, breakfast, lunch, dinner))
        }

        return plans
    }

    private fun selectMeal(
        sourceList: List<RecipeModel>,
        favoriteRecipes: List<RecipeModel>,
        favoriteCuisines: List<String>,
        rand: java.util.Random
    ): RecipeModel {
        if (sourceList.isEmpty()) {
            return RecipeModel(0, "Healthy Meal", emptyList(), emptyList(), 10, 15, "Easy", "General", "Breakfast", "", 5.0)
        }

        // 60% probability of selecting from favored cuisines
        if (favoriteCuisines.isNotEmpty() && rand.nextFloat() < 0.60f) {
            val matchingRecipes = sourceList.filter { favoriteCuisines.contains(it.cuisine) }
            if (matchingRecipes.isNotEmpty()) {
                return matchingRecipes[rand.nextInt(matchingRecipes.size)]
            }
        }

        // 30% probability of choosing directly from specific liked items
        val matchingFavorites = favoriteRecipes.filter { sourceList.contains(it) }
        if (matchingFavorites.isNotEmpty() && rand.nextFloat() < 0.30f) {
            return matchingFavorites[rand.nextInt(matchingFavorites.size)]
        }

        return sourceList[rand.nextInt(sourceList.size)]
    }
}

data class DietDayPlan(
    val dayName: String,
    val breakfast: RecipeModel,
    val lunch: RecipeModel,
    val dinner: RecipeModel
)
