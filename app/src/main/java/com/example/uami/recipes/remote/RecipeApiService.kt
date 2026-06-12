package com.example.uami.recipes.remote

import com.example.uami.recipes.models.RecipeModel
import retrofit2.http.GET
import retrofit2.http.Path

interface RecipeApiService {
    // Para nuestra nueva API Django
    @GET("recipe")
    suspend fun getRecipes(): List<RecipeModel>

    @GET("recipe/{id}")
    suspend fun getRecipeById(@Path("id") id: Int): RecipeModel
}
