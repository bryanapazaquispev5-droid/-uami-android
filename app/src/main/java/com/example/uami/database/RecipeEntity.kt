package com.example.uami.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.uami.recipes.models.RecipeModel

@Entity(tableName = "recipes")
data class RecipeEntity(
    @PrimaryKey val id: Int,
    val name: String?,
    val ingredients: List<String>?,
    val instructions: List<String>?,
    val prepTimeMinutes: Int?,
    val cookTimeMinutes: Int?,
    val difficulty: String?,
    val cuisine: String?,
    val mealType: String?,
    val image: String?,
    val rating: Double?,
    val difficultyEn: String?,
    val cuisineEn: String?,
    val mealTypeEn: String?
) {
    fun toModel() = RecipeModel(
        id = id,
        name = name,
        ingredients = ingredients,
        instructions = instructions,
        prepTimeMinutes = prepTimeMinutes,
        cookTimeMinutes = cookTimeMinutes,
        difficulty = difficulty,
        cuisine = cuisine,
        mealType = mealType,
        image = image,
        rating = rating,
        difficultyEn = difficultyEn,
        cuisineEn = cuisineEn,
        mealTypeEn = mealTypeEn
    )

    companion object {
        fun fromModel(model: RecipeModel) = RecipeEntity(
            id = model.id ?: 0,
            name = model.name,
            ingredients = model.ingredients,
            instructions = model.instructions,
            prepTimeMinutes = model.prepTimeMinutes,
            cookTimeMinutes = model.cookTimeMinutes,
            difficulty = model.difficulty,
            cuisine = model.cuisine,
            mealType = model.mealType,
            image = model.image,
            rating = model.rating,
            difficultyEn = model.difficultyEn,
            cuisineEn = model.cuisineEn,
            mealTypeEn = model.mealTypeEn
        )
    }
}
