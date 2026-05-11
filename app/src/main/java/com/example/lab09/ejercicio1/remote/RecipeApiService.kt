package com.example.lab09.ejercicio1.remote

import com.example.lab09.ejercicio1.models.RecipeModel
import com.example.lab09.ejercicio1.models.RecipeResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface RecipeApiService {
    @GET("recipes")
    suspend fun getRecipes(
        @Query("limit") limit: Int,
        @Query("skip") skip: Int
    ): RecipeResponse

    @GET("recipes/{id}")
    suspend fun getRecipeById(@Path("id") id: Int): RecipeModel

    @GET("recipes/search")
    suspend fun searchRecipes(
        @Query("q") query: String
    ): RecipeResponse
}
