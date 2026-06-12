package com.example.uami.ui.filters

import com.example.uami.ejercicio1.models.RecipeModel

object FilterLogic {
    fun applyFilters(
        recipes: List<RecipeModel>,
        state: FilterState
    ): List<RecipeModel> {
        var result = recipes

        // 1. Filtro por texto
        if (state.searchQuery.isNotEmpty()) {
            result = result.filter { 
                it.name?.contains(state.searchQuery, ignoreCase = true) == true ||
                it.cuisine?.contains(state.searchQuery, ignoreCase = true) == true
            }
        }

        // 2. Filtro de Cocina
        if (state.selectedCuisine != "All") {
            result = result.filter { it.cuisineEn?.equals(state.selectedCuisine, ignoreCase = true) == true }
        }

        // 3. Filtro de Dificultad
        if (state.selectedDifficulty != "All") {
            result = result.filter { 
                it.difficultyEn?.equals(state.selectedDifficulty, ignoreCase = true) == true ||
                it.difficulty?.equals(state.selectedDifficulty, ignoreCase = true) == true
            }
        }

        // 4. Filtro de Tipo de Plato
        if (state.selectedMealType != "All") {
            result = result.filter { it.mealTypeEn?.equals(state.selectedMealType, ignoreCase = true) == true }
        }

        // 5. Ordenamiento
        return when (state.sortOrder) {
            "A-Z" -> result.sortedBy { it.name }
            "Z-A" -> result.sortedByDescending { it.name }
            "Rating" -> result.sortedByDescending { it.rating }
            "Time" -> result.sortedBy { (it.prepTimeMinutes ?: 0) + (it.cookTimeMinutes ?: 0) }
            "Ingredients" -> result.sortedByDescending { it.ingredients?.size ?: 0 }
            else -> result
        }
    }
}
