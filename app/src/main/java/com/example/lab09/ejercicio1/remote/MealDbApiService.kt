package com.example.lab09.ejercicio1.remote

import com.example.lab09.ejercicio1.models.RecipeResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface MealDbApiService {
    @GET("search.php?s=")
    suspend fun getRecipes(
        @Query("limit") limit: Int,
        @Query("skip") skip: Int
    ): RecipeResponse

    @GET("lookup.php")
    suspend fun getRecipeById(@Query("i") id: Int): RecipeResponse

    @GET("search.php")
    suspend fun searchRecipes(
        @Query("s") query: String
    ): RecipeResponse
}
