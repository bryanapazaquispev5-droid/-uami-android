package com.example.uami.utils

import android.content.Context
import android.net.Uri
import com.example.uami.recipes.models.RecipeModel
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    private val initMutex = Mutex()

    private val _downloadProgress = MutableStateFlow<Float>(-1f)
    val downloadProgress: StateFlow<Float> = _downloadProgress

    private val _isModelReady = MutableStateFlow(false)
    val isModelReady: StateFlow<Boolean> = _isModelReady

    private val _isInitializing = MutableStateFlow(false)
    val isInitializing: StateFlow<Boolean> = _isInitializing

    init {
        checkModelAvailability()
    }

    private fun isModelInAssets(): Boolean {
        return try {
            context.assets.open(MODEL_FILENAME).use { true }
        } catch (e: Exception) {
            false
        }
    }

    fun checkModelAvailability() {
        val existsOnDisk = modelFile.exists() && modelFile.length() > 1300000000L
        val existsInAssets = isModelInAssets()
        _isModelReady.value = existsOnDisk || existsInAssets
    }

    private suspend fun copyModelFromAssets(): Boolean = withContext(Dispatchers.IO) {
        var inputStream: java.io.InputStream? = null
        var outputStream: FileOutputStream? = null
        try {
            _downloadProgress.value = 0f
            
            // Try to find the total size to show progress
            var totalBytes = -1L
            try {
                context.assets.openFd(MODEL_FILENAME).use { fd ->
                    totalBytes = fd.length
                }
            } catch (e: Exception) {
                try {
                    context.assets.open(MODEL_FILENAME).use { stream ->
                        totalBytes = stream.available().toLong()
                    }
                } catch (ex: Exception) {}
            }

            inputStream = context.assets.open(MODEL_FILENAME)
            val tempFile = File(context.filesDir, "$MODEL_FILENAME.tmp")
            outputStream = FileOutputStream(tempFile)

            val buffer = ByteArray(65536)
            var bytesCopied = 0L
            var read: Int

            while (inputStream.read(buffer).also { read = it } != -1) {
                outputStream.write(buffer, 0, read)
                bytesCopied += read
                if (totalBytes > 0) {
                    _downloadProgress.value = bytesCopied.toFloat() / totalBytes
                }
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()

            if (tempFile.exists()) {
                if (modelFile.exists()) modelFile.delete()
                tempFile.renameTo(modelFile)
            }
            
            _downloadProgress.value = -1f
            true
        } catch (e: Exception) {
            e.printStackTrace()
            _downloadProgress.value = -1f
            false
        } finally {
            try { inputStream?.close() } catch (e: Exception) {}
            try { outputStream?.close() } catch (e: Exception) {}
        }
    }

    // Initializes the local LLM engine (thread-safe, protects against corrupted files)
    suspend fun initializeLLM(): Boolean = initMutex.withLock {
        if (llmInference != null) return@withLock true
        
        _isInitializing.value = true
        try {
            val existsOnDisk = modelFile.exists() && modelFile.length() > 1300000000L
            if (!existsOnDisk) {
                if (isModelInAssets()) {
                    val copySuccess = copyModelFromAssets()
                    if (!copySuccess) {
                        _isInitializing.value = false
                        return@withLock false
                    }
                } else {
                    _isInitializing.value = false
                    return@withLock false
                }
            }

            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelFile.absolutePath)
                .setMaxTokens(512)
                .setTemperature(0.7f)
                .build()
            llmInference = LlmInference.createFromOptions(context, options)
            _isInitializing.value = false
            true
        } catch (e: Throwable) {
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
    suspend fun generateResponse(
        prompt: String, 
        favoriteRecipes: List<RecipeModel>, 
        allRecipes: List<RecipeModel>, 
        lang: String
    ): String = withContext(Dispatchers.Default) {
        val inference = llmInference ?: return@withContext if (lang == "es") {
            "El nutriólogo offline no está listo. Por favor, asegúrate de descargar o importar el modelo primero."
        } else {
            "Offline AI nutritionist is not ready. Please make sure to download or import the model first."
        }

        val isEs = lang == "es"
        val queryLower = prompt.lowercase()

        // 1. Define keywords for greetings and recipe/food related topics
        val greetingKeywords = listOf(
            "hola", "hello", "buenos dias", "buenas tardes", "buenas noches", "hey", "hi", 
            "saludos", "como estas", "que tal", "como te va", "como andas", "welcome", "bienvenido"
        )
        val recipeKeywords = listOf(
            "receta", "recipe", "cocinar", "cook", "comer", "eat", "cena", "dinner", 
            "almuerzo", "lunch", "desayuno", "breakfast", "ingrediente", "ingredient", 
            "recomienda", "recommend", "sugiere", "suggest", "platillo", "comida", 
            "food", "tengo", "hacer", "preparar", "prepare", "pollo", "carne", "vegetal",
            "fruta", "verdura", "ensalada", "sopa", "postre", "dessert", "nutri", "dieta", "uami"
        )

        val isGreeting = greetingKeywords.any { queryLower.contains(it) }
        val isRecipeQuery = recipeKeywords.any { queryLower.contains(it) }

        // 2. Intercept out-of-scope requests in Kotlin for 100% reliability and speed
        if (!isGreeting && !isRecipeQuery) {
            return@withContext if (isEs) {
                "Lo siento, solo puedo responder preguntas relacionadas con las recetas de la aplicación Uami o temas de alimentación."
            } else {
                "Sorry, I can only answer questions related to Uami app recipes or food topics."
            }
        }

        // 3. If it's a recipe/food query, filter and append recipe context
        val finalPrompt = if (isRecipeQuery) {
            val mealTypeKeywords = if (isEs) {
                mapOf(
                    "desayuno" to listOf("breakfast", "desayuno", "desayunar"),
                    "almuerzo" to listOf("lunch", "almuerzo", "almorzar", "comida", "comer"),
                    "cena" to listOf("dinner", "cena", "cenar"),
                    "postre" to listOf("dessert", "postre"),
                    "merienda" to listOf("snack", "merienda", "colacion", "merendar")
                )
            } else {
                mapOf(
                    "breakfast" to listOf("breakfast", "have breakfast"),
                    "lunch" to listOf("lunch", "have lunch"),
                    "dinner" to listOf("dinner", "have dinner"),
                    "dessert" to listOf("dessert"),
                    "snack" to listOf("snack", "have snack")
                )
            }
            
            val matchedMealTypes = mutableListOf<String>()
            for ((key, keywords) in mealTypeKeywords) {
                if (keywords.any { queryLower.contains(it) }) {
                    matchedMealTypes.add(key)
                }
            }
            
            val filtered = allRecipes.filter { recipe ->
                val nameMatch = recipe.name?.lowercase()?.contains(queryLower) == true
                val ingredientMatch = recipe.ingredients?.any { it.lowercase().contains(queryLower) } == true
                val cuisineMatch = recipe.cuisine?.lowercase()?.contains(queryLower) == true
                val mealMatch = if (matchedMealTypes.isNotEmpty()) {
                    matchedMealTypes.any { type -> 
                        recipe.mealType?.lowercase()?.contains(type) == true
                    }
                } else {
                    false
                }
                nameMatch || ingredientMatch || cuisineMatch || mealMatch
            }

            val relevantRecipes = if (filtered.isNotEmpty()) {
                filtered.take(2)
            } else {
                allRecipes.take(2)
            }

            val recipesContextText = if (relevantRecipes.isNotEmpty()) {
                val listText = relevantRecipes.joinToString("\n") { recipe ->
                    "- ${recipe.name ?: ""} (Cuisine: ${recipe.cuisine ?: "General"}, Meal: ${recipe.mealType ?: "General"}, Ingredients: ${recipe.ingredients?.joinToString(", ") ?: "None"})"
                }
                if (isEs) {
                    "\n\n[Contexto - Recetas oficiales disponibles en la app Uami]:\n$listText\n\nPor favor, responde a mi consulta basándote únicamente en estas recetas de arriba de la app."
                } else {
                    "\n\n[Contexto - Official recipes available in the Uami app]:\n$listText\n\nPlease reply to my query based on these app recipes above."
                }
            } else {
                ""
            }

            prompt + recipesContextText
        } else {
            prompt // Plain raw prompt for greetings
        }

        val formattedPrompt = "<start_of_turn>user\n$finalPrompt<end_of_turn>\n<start_of_turn>model\n"

        try {
            inference.generateResponse(formattedPrompt)
        } catch (e: Throwable) {
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
        
        // Categorize recipes by meal type (check both translation and original English version)
        val breakfasts = allRecipes.filter { 
            val mt = it.mealType?.lowercase() ?: ""
            val mte = it.mealTypeEn?.lowercase() ?: ""
            mt.contains("breakfast") || mt.contains("desayuno") || 
            mte.contains("breakfast") || mte.contains("desayuno")
        }
        val lunchAndDinners = allRecipes.filter { 
            val mt = it.mealType?.lowercase() ?: ""
            val mte = it.mealTypeEn?.lowercase() ?: ""
            mt.contains("lunch") || mt.contains("dinner") || mt.contains("almuerzo") || mt.contains("cena") || mt.contains("comida") ||
            mte.contains("lunch") || mte.contains("dinner") || mte.contains("almuerzo") || mte.contains("cena") || mte.contains("comida")
        }

        // Fallbacks if lists are empty
        val fallbackBreakfasts = breakfasts.ifEmpty { allRecipes.take(8) }
        val fallbackLunches = lunchAndDinners.ifEmpty { allRecipes.drop(8).take(12) }

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

    suspend fun generateAIDietPlan(allRecipes: List<RecipeModel>, favoriteIds: List<Int>, isEs: Boolean): List<DietDayPlan> = withContext(Dispatchers.Default) {
        val inference = llmInference
        if (inference == null) {
            return@withContext generateOfflineDietPlan(allRecipes, favoriteIds, isEs)
        }

        // Categorize recipes (check both translation and original English version)
        val breakfastsAll = allRecipes.filter { 
            val mt = it.mealType?.lowercase() ?: ""
            val mte = it.mealTypeEn?.lowercase() ?: ""
            mt.contains("breakfast") || mt.contains("desayuno") || 
            mte.contains("breakfast") || mte.contains("desayuno")
        }
        val lunchDinnersAll = allRecipes.filter { 
            val mt = it.mealType?.lowercase() ?: ""
            val mte = it.mealTypeEn?.lowercase() ?: ""
            mt.contains("lunch") || mt.contains("dinner") || mt.contains("almuerzo") || mt.contains("cena") || mt.contains("comida") ||
            mte.contains("lunch") || mte.contains("dinner") || mte.contains("almuerzo") || mte.contains("cena") || mte.contains("comida")
        }

        // Separate favorite and non-favorite recipes to prioritize favorites while keeping the list small (max 4 breakfasts, 6 lunches/dinners)
        val favoriteBreakfasts = breakfastsAll.filter { favoriteIds.contains(it.id) }
        val nonFavoriteBreakfasts = breakfastsAll.filter { !favoriteIds.contains(it.id) }
        val fallbackBreakfasts = (favoriteBreakfasts + nonFavoriteBreakfasts.shuffled()).take(4).ifEmpty { allRecipes.take(4) }

        val favoriteLunches = lunchDinnersAll.filter { favoriteIds.contains(it.id) }
        val nonFavoriteLunches = lunchDinnersAll.filter { !favoriteIds.contains(it.id) }
        val fallbackLunches = (favoriteLunches + nonFavoriteLunches.shuffled()).take(6).ifEmpty { allRecipes.drop(4).take(6) }

        // Include metadata (cuisine, meal type, difficulty) to help AI classify dinners/desserts/etc.
        val breakfastsList = fallbackBreakfasts.joinToString("\n") { recipe ->
            val isFav = favoriteIds.contains(recipe.id)
            val info = listOfNotNull(
                if (isFav) (if (isEs) "Favorito" else "Favorite") else null,
                recipe.cuisine?.takeIf { it.isNotBlank() },
                recipe.mealType?.takeIf { it.isNotBlank() },
                recipe.difficulty?.takeIf { it.isNotBlank() }
            ).joinToString(", ")
            "- ${recipe.name ?: ""}" + if (info.isNotEmpty()) " ($info)" else ""
        }
        
        val lunchDinnersList = fallbackLunches.joinToString("\n") { recipe ->
            val isFav = favoriteIds.contains(recipe.id)
            val info = listOfNotNull(
                if (isFav) (if (isEs) "Favorito" else "Favorite") else null,
                recipe.cuisine?.takeIf { it.isNotBlank() },
                recipe.mealType?.takeIf { it.isNotBlank() },
                recipe.difficulty?.takeIf { it.isNotBlank() }
            ).joinToString(", ")
            "- ${recipe.name ?: ""}" + if (info.isNotEmpty()) " ($info)" else ""
        }

        val prompt = if (isEs) {
            "Genera un menú semanal saludable de Lunes a Domingo utilizando ÚNICAMENTE las siguientes recetas:\n\n" +
            "Desayunos disponibles:\n$breakfastsList\n\n" +
            "Almuerzos y Cenas disponibles:\n$lunchDinnersList\n\n" +
            "Instrucciones importantes:\n" +
            "1. Responde EXACTAMENTE en el formato de abajo, una línea por día.\n" +
            "2. No agregues textos introductorios, saludos ni explicaciones.\n" +
            "3. En la respuesta, escribe ÚNICAMENTE el nombre de la receta (no incluyas la información entre paréntesis).\n\n" +
            "Formato:\n" +
            "Lunes: Desayuno: [receta], Almuerzo: [receta], Cena: [receta]\n" +
            "Martes: Desayuno: [receta], Almuerzo: [receta], Cena: [receta]\n" +
            "Miércoles: Desayuno: [receta], Almuerzo: [receta], Cena: [receta]\n" +
            "Jueves: Desayuno: [receta], Almuerzo: [receta], Cena: [receta]\n" +
            "Viernes: Desayuno: [receta], Almuerzo: [receta], Cena: [receta]\n" +
            "Sábado: Desayuno: [receta], Almuerzo: [receta], Cena: [receta]\n" +
            "Domingo: Desayuno: [receta], Almuerzo: [receta], Cena: [receta]"
        } else {
            "Generate a healthy weekly menu from Monday to Sunday using ONLY these available recipes:\n\n" +
            "Breakfast options:\n$breakfastsList\n\n" +
            "Lunch and Dinner options:\n$lunchDinnersList\n\n" +
            "Important instructions:\n" +
            "1. Respond EXACTLY in the format below, one line per day.\n" +
            "2. Do not add intro text, greetings, or extra explanations.\n" +
            "3. In the response, write ONLY the recipe name (do not include the details in parentheses).\n\n" +
            "Format:\n" +
            "Monday: Breakfast: [recipe], Lunch: [recipe], Dinner: [recipe]\n" +
            "Tuesday: Breakfast: [recipe], Lunch: [recipe], Dinner: [recipe]\n" +
            "Wednesday: Breakfast: [recipe], Lunch: [recipe], Dinner: [recipe]\n" +
            "Thursday: Breakfast: [recipe], Lunch: [recipe], Dinner: [recipe]\n" +
            "Friday: Breakfast: [recipe], Lunch: [recipe], Dinner: [recipe]\n" +
            "Saturday: Breakfast: [recipe], Lunch: [recipe], Dinner: [recipe]\n" +
            "Sunday: Breakfast: [recipe], Lunch: [recipe], Dinner: [recipe]"
        }

        val formattedPrompt = "<start_of_turn>user\n$prompt<end_of_turn>\n<start_of_turn>model\n"

        val response = try {
            inference.generateResponse(formattedPrompt)
        } catch (e: Throwable) {
            e.printStackTrace()
            ""
        }

        if (response.isBlank()) {
            return@withContext generateOfflineDietPlan(allRecipes, favoriteIds, isEs)
        }

        // Parse response lines
        val days = if (isEs) {
            listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")
        } else {
            listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
        }

        val plans = mutableListOf<DietDayPlan>()
        val rand = java.util.Random()
        
        for (day in days) {
            // Pre-initialize with a diverse random selection to avoid always falling back to the 1st item
            var breakfast = fallbackBreakfasts[rand.nextInt(fallbackBreakfasts.size)]
            var lunch = fallbackLunches[rand.nextInt(fallbackLunches.size)]
            var dinner = fallbackLunches[rand.nextInt(fallbackLunches.size)]
            
            // Find the line for this day
            val dayLine = response.lines().find { it.contains(day, ignoreCase = true) }
            if (dayLine != null) {
                val breakfastPart = extractRecipePart(dayLine, if (isEs) "Desayuno" else "Breakfast")
                val lunchPart = extractRecipePart(dayLine, if (isEs) "Almuerzo" else "Lunch")
                val dinnerPart = extractRecipePart(dayLine, if (isEs) "Cena" else "Dinner")
                
                if (breakfastPart.isNotBlank()) breakfast = matchRecipe(breakfastPart, fallbackBreakfasts)
                if (lunchPart.isNotBlank()) lunch = matchRecipe(lunchPart, fallbackLunches)
                if (dinnerPart.isNotBlank()) dinner = matchRecipe(dinnerPart, fallbackLunches)
            }
            
            plans.add(DietDayPlan(day, breakfast, lunch, dinner))
        }

        plans
    }

    private fun extractRecipePart(line: String, label: String): String {
        val cleanLine = line.lowercase()
        val cleanLabel = label.lowercase().replace(":", "").trim()
        
        var index = cleanLine.indexOf("$cleanLabel:")
        var labelLength = cleanLabel.length + 1
        if (index == -1) {
            index = cleanLine.indexOf(cleanLabel)
            labelLength = cleanLabel.length
        }
        if (index == -1) return ""
        
        val start = index + labelLength
        val delimiters = listOf(",", ";", "almuerzo", "lunch", "cena", "dinner", "breakfast", "desayuno")
        var end = line.length
        
        for (delim in delimiters) {
            if (delim == cleanLabel) continue
            val delimIndex = cleanLine.indexOf(delim, start)
            if (delimIndex in start until end) {
                end = delimIndex
            }
        }
        
        var extracted = line.substring(start, end).trim()
        
        // Clean up formatting remnants
        while (extracted.startsWith("-") || extracted.startsWith(":") || extracted.startsWith(" ") || extracted.startsWith("[")) {
            extracted = extracted.substring(1).trim()
        }
        while (extracted.endsWith("]") || extracted.endsWith(",") || extracted.endsWith(".") || extracted.endsWith("*")) {
            extracted = extracted.substring(0, extracted.length - 1).trim()
        }
        
        return extracted
    }

    private fun matchRecipe(name: String, sourceList: List<RecipeModel>): RecipeModel {
        if (sourceList.isEmpty()) {
            return RecipeModel(0, "Healthy Meal", emptyList(), emptyList(), 10, 15, "Easy", "General", "Breakfast", "", 5.0)
        }
        
        var cleanName = name.lowercase()
            .replace("[", "")
            .replace("]", "")
            .replace("*", "")
            .replace(":", "")
            .trim()
            
        if (cleanName.contains("(")) {
            cleanName = cleanName.substringBefore("(").trim()
        }
            
        if (cleanName.isNotBlank()) {
            // 1. Direct or partial substring matching
            val matched = sourceList.find { 
                val recipeName = (it.name ?: "").lowercase()
                recipeName.contains(cleanName) || cleanName.contains(recipeName) 
            }
            if (matched != null) return matched
            
            // 2. Fuzzy word overlap matching (words longer than 3 characters)
            val words = cleanName.split(" ").filter { it.length > 3 }
            if (words.isNotEmpty()) {
                val matchedFuzzy = sourceList.find { recipe ->
                    val recipeName = (recipe.name ?: "").lowercase()
                    words.any { word -> recipeName.contains(word) }
                }
                if (matchedFuzzy != null) return matchedFuzzy
            }
        }
        
        // 3. Fallback to a diverse random selection if no match is found
        val rand = java.util.Random()
        return sourceList[rand.nextInt(sourceList.size)]
    }

    fun generateOfflineRecommendations(allRecipes: List<RecipeModel>, favoriteIds: List<Int>, isEs: Boolean): List<RecipeModel> {
        val favorites = allRecipes.filter { favoriteIds.contains(it.id) }
        val recommendations = favorites.take(6).toMutableList()
        if (recommendations.size < 6) {
            val nonFavorites = allRecipes.filter { !favoriteIds.contains(it.id) }
            val remaining = 6 - recommendations.size
            recommendations.addAll(nonFavorites.shuffled().take(remaining))
        }
        return recommendations.ifEmpty { allRecipes.take(6) }
    }

    suspend fun generateAIRecommendations(allRecipes: List<RecipeModel>, favoriteIds: List<Int>, isEs: Boolean): List<RecipeModel> = withContext(Dispatchers.Default) {
        val inference = llmInference
        if (inference == null) {
            return@withContext generateOfflineRecommendations(allRecipes, favoriteIds, isEs)
        }

        // Limit the candidate list to prevent JNI boundary crashes
        val favorites = allRecipes.filter { favoriteIds.contains(it.id) }
        val nonFavorites = allRecipes.filter { !favoriteIds.contains(it.id) }
        val candidateList = (favorites + nonFavorites.shuffled()).take(15).ifEmpty { allRecipes.take(15) }

        val candidateListStr = candidateList.joinToString("\n") { recipe ->
            val isFav = favoriteIds.contains(recipe.id)
            val info = listOfNotNull(
                if (isFav) (if (isEs) "Favorito" else "Favorite") else null,
                recipe.cuisine?.takeIf { it.isNotBlank() },
                recipe.mealType?.takeIf { it.isNotBlank() },
                recipe.difficulty?.takeIf { it.isNotBlank() }
            ).joinToString(", ")
            "- ${recipe.name ?: ""}" + if (info.isNotEmpty()) " ($info)" else ""
        }

        val prompt = if (isEs) {
            "Elige las 6 mejores recetas recomendadas para el usuario de entre la siguiente lista. Prioriza las marcadas como Favorito, pero mantén variedad de comidas y sabores:\n\n" +
            "$candidateListStr\n\n" +
            "Instrucciones:\n" +
            "1. Escribe ÚNICAMENTE los nombres de las 6 recetas seleccionadas, una por línea.\n" +
            "2. No agregues explicaciones, números, viñetas ni comentarios adicionales."
        } else {
            "Select the 6 best recommended recipes for the user from the following list. Prioritize the ones marked as Favorite, but keep a variety of meals and flavors:\n\n" +
            "$candidateListStr\n\n" +
            "Instructions:\n" +
            "1. Output ONLY the names of the 6 selected recipes, one per line.\n" +
            "2. Do not add explanations, numbers, bullet points, or extra text."
        }

        try {
            val response = inference.generateResponse(prompt)
            val lines = response.split("\n")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .map { line ->
                    var clean = line
                    while (clean.startsWith("-") || clean.startsWith("*") || clean.startsWith(" ") || clean.matches("^\\d+\\..*".toRegex())) {
                        clean = clean.replaceFirst("^(\\d+\\.|-|\\*|\\s)+".toRegex(), "").trim()
                    }
                    clean
                }
                .filter { it.isNotEmpty() }
                .take(6)

            if (lines.size >= 4) {
                return@withContext lines.map { matchRecipe(it, allRecipes) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return@withContext generateOfflineRecommendations(allRecipes, favoriteIds, isEs)
    }
}

data class DietDayPlan(
    val dayName: String,
    val breakfast: RecipeModel,
    val lunch: RecipeModel,
    val dinner: RecipeModel
)
