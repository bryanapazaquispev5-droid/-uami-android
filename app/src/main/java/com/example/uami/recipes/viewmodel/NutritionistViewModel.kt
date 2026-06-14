package com.example.uami.recipes.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uami.recipes.data.RecipeRepository
import com.example.uami.recipes.models.RecipeModel
import com.example.uami.recipes.ui.SavedDayPlanIds
import com.example.uami.utils.DietDayPlan
import com.example.uami.utils.NutritionistAIManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NutritionistViewModel(
    private val repository: RecipeRepository,
    private val nutritionistManager: NutritionistAIManager
) : ViewModel() {
    private val _dietPlan = MutableStateFlow<List<DietDayPlan>>(emptyList())
    val dietPlan: StateFlow<List<DietDayPlan>> = _dietPlan.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    val isModelReady: StateFlow<Boolean> = nutritionistManager.isModelReady
    val isInitializing: StateFlow<Boolean> = nutritionistManager.isInitializing
    val downloadProgress: StateFlow<Float> = nutritionistManager.downloadProgress

    private val sharedPrefs = repository.context.getSharedPreferences("nutritionist_prefs", Context.MODE_PRIVATE)

    fun loadSavedPlan(allRecipes: List<RecipeModel>) {
        val savedPlanJson = sharedPrefs.getString("saved_diet_plan", null)
        if (savedPlanJson != null) {
            try {
                val gson = Gson()
                val type = object : TypeToken<List<SavedDayPlanIds>>() {}.type
                val savedIdsList: List<SavedDayPlanIds> = gson.fromJson(savedPlanJson, type)
                
                _dietPlan.value = savedIdsList.map { savedDay ->
                    DietDayPlan(
                        dayName = savedDay.dayName,
                        breakfast = allRecipes.find { it.id == savedDay.breakfastId } ?: allRecipes.first(),
                        lunch = allRecipes.find { it.id == savedDay.lunchId } ?: allRecipes.first(),
                        dinner = allRecipes.find { it.id == savedDay.dinnerId } ?: allRecipes.first()
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        // Inicializar el modelo LLM en background
        viewModelScope.launch {
            nutritionistManager.initializeLLM()
        }
    }

    fun generateDietPlan(allRecipes: List<RecipeModel>, favoriteIds: List<Int>, isEs: Boolean) {
        viewModelScope.launch {
            _isGenerating.value = true
            val initialized = nutritionistManager.initializeLLM()
            val newPlan = if (initialized) {
                nutritionistManager.generateAIDietPlan(allRecipes, favoriteIds, isEs)
            } else {
                nutritionistManager.generateOfflineDietPlan(allRecipes, favoriteIds, isEs)
            }
            _dietPlan.value = newPlan
            
            // Guardar en SharedPreferences los IDs
            val savedIds = newPlan.map {
                SavedDayPlanIds(it.dayName, it.breakfast.id ?: 0, it.lunch.id ?: 0, it.dinner.id ?: 0)
            }
            sharedPrefs.edit().putString("saved_diet_plan", Gson().toJson(savedIds)).apply()
            _isGenerating.value = false
        }
    }
}
