package com.example.uami.recipes.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.uami.recipes.data.RecipeRepository
import com.example.uami.utils.NutritionistAIManager

class ViewModelFactory(
    private val repository: RecipeRepository,
    private val context: Context
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(MainViewModel::class.java) -> {
                MainViewModel(repository) as T
            }
            modelClass.isAssignableFrom(RecipesViewModel::class.java) -> {
                RecipesViewModel(repository) as T
            }
            modelClass.isAssignableFrom(NutritionistViewModel::class.java) -> {
                val AIContext = context.applicationContext
                val nutritionistManager = NutritionistAIManager(AIContext)
                NutritionistViewModel(repository, nutritionistManager) as T
            }
            modelClass.isAssignableFrom(ReviewsViewModel::class.java) -> {
                ReviewsViewModel(repository) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
