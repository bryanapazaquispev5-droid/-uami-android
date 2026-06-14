package com.example.uami.recipes.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uami.recipes.data.RecipeRepository
import com.example.uami.recipes.models.RecipeModel
import com.example.uami.sync.SyncResult
import com.example.uami.utils.LanguageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(private val repository: RecipeRepository) : ViewModel() {
    private val _currentLanguage = MutableStateFlow(LanguageManager.getLanguage())
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    private val _globalRecipes = MutableStateFlow<List<RecipeModel>>(emptyList())
    val globalRecipes: StateFlow<List<RecipeModel>> = _globalRecipes.asStateFlow()

    private val _isPreparingData = MutableStateFlow(false)
    val isPreparingData: StateFlow<Boolean> = _isPreparingData.asStateFlow()

    private val _isDownloadFailed = MutableStateFlow(false)
    val isDownloadFailed: StateFlow<Boolean> = _isDownloadFailed.asStateFlow()

    private val _isUpdateChecked = MutableStateFlow(false)
    val isUpdateChecked: StateFlow<Boolean> = _isUpdateChecked.asStateFlow()

    private val _downloadProgress = MutableStateFlow(0f)
    val downloadProgress: StateFlow<Float> = _downloadProgress.asStateFlow()

    private val _downloadStatus = MutableStateFlow("")
    val downloadStatus: StateFlow<String> = _downloadStatus.asStateFlow()

    private val _startupErrorMessage = MutableStateFlow("")
    val startupErrorMessage: StateFlow<String> = _startupErrorMessage.asStateFlow()

    private val _favoritos = MutableStateFlow<List<Int>>(emptyList())
    val favoritos: StateFlow<List<Int>> = _favoritos.asStateFlow()

    init {
        // Cargar favoritos reactivamente desde el repositorio
        viewModelScope.launch {
            repository.favoritos.collect {
                _favoritos.value = it
            }
        }
        
        // Si hay cache local, cargarlo inmediatamente para permitir inicio rápido offline
        if (repository.hasCache()) {
            _globalRecipes.value = repository.loadRecipesFromCache()
        }
    }

    fun setLanguage(language: String) {
        _currentLanguage.value = language
        LanguageManager.setLanguage(language)
    }

    fun startSync(isFirstRun: Boolean) {
        viewModelScope.launch {
            _isPreparingData.value = true
            _isDownloadFailed.value = false
            
            val result = repository.checkAndSync(
                currentLanguage = _currentLanguage.value,
                isFirstRun = isFirstRun,
                onProgress = { progress, status ->
                    _downloadProgress.value = progress
                    _downloadStatus.value = status
                }
            )

            when (result) {
                is SyncResult.Success -> {
                    _globalRecipes.value = result.recipes
                    _isUpdateChecked.value = true
                    _isPreparingData.value = false
                }
                is SyncResult.Error -> {
                    _startupErrorMessage.value = result.message
                    if (isFirstRun) {
                        setLanguage("")
                    } else {
                        _globalRecipes.value = repository.loadRecipesFromCache()
                    }
                    _isUpdateChecked.value = true
                    _isDownloadFailed.value = true
                    _isPreparingData.value = false
                }
            }
        }
    }

    fun toggleFavorite(recipeId: Int) {
        val currentFavs = repository.favoritos.value.toMutableList()
        if (currentFavs.contains(recipeId)) {
            currentFavs.remove(recipeId)
        } else {
            currentFavs.add(recipeId)
        }
        repository.saveFavorites(currentFavs)
    }

    fun setFavorites(favorites: List<Int>) {
        repository.saveFavorites(favorites)
    }

    fun setUpdateChecked(checked: Boolean) {
        _isUpdateChecked.value = checked
    }

    fun setPreparingData(preparing: Boolean) {
        _isPreparingData.value = preparing
    }

    fun setDownloadFailed(failed: Boolean) {
        _isDownloadFailed.value = failed
    }

    fun setStartupErrorMessage(message: String) {
        _startupErrorMessage.value = message
    }
}
