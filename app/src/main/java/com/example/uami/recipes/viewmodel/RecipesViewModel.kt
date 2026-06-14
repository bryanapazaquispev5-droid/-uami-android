package com.example.uami.recipes.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uami.recipes.data.RecipeRepository
import com.example.uami.recipes.models.RecipeModel
import com.example.uami.ui.filters.FilterLogic
import com.example.uami.ui.filters.FilterState
import com.example.uami.utils.LanguageManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RecipesViewModel(private val repository: RecipeRepository) : ViewModel() {
    private val _allRecipes = MutableStateFlow<List<RecipeModel>>(emptyList())
    val allRecipes: StateFlow<List<RecipeModel>> = _allRecipes.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _isListView = MutableStateFlow(LanguageManager.isListView())
    val isListView: StateFlow<Boolean> = _isListView.asStateFlow()

    private val _filterState = MutableStateFlow(FilterState())
    val filterState: StateFlow<FilterState> = _filterState.asStateFlow()

    // Lógica reactiva para filtrar y ordenar recetas en background
    val filteredAndSortedRecipes: StateFlow<List<RecipeModel>> = combine(
        _allRecipes,
        _filterState
    ) { recipes, filters ->
        FilterLogic.applyFilters(recipes, filters)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun loadRecipes(preloadedRecipes: List<RecipeModel>) {
        if (preloadedRecipes.isNotEmpty()) {
            _allRecipes.value = preloadedRecipes
            _isLoading.value = false
        } else {
            _isLoading.value = true
            viewModelScope.launch {
                _allRecipes.value = repository.loadRecipesFromCache()
                _isLoading.value = false
            }
        }
    }

    fun refreshRecipes() {
        viewModelScope.launch {
            _isRefreshing.value = true
            delay(500) // Mantener el delay original de la UI
            _isRefreshing.value = false
        }
    }

    fun setListView(isList: Boolean) {
        _isListView.value = isList
        LanguageManager.setListView(isList)
    }

    fun updateFilterState(newState: FilterState) {
        _filterState.value = newState
    }

    fun updateSearchQuery(query: String) {
        _filterState.value = _filterState.value.copy(searchQuery = query)
    }
}
