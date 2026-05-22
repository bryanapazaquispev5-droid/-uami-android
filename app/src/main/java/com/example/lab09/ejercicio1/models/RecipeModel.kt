package com.example.lab09.ejercicio1.models

import com.google.gson.*
import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import java.lang.reflect.Type

data class RecipeResponse(
    @SerializedName(value = "meals", alternate = ["recipes"]) val recipes: List<RecipeModel>?
)

@JsonAdapter(RecipeModelDeserializer::class)
data class RecipeModel(
    val id: Int?,
    val name: String?,
    val ingredients: List<String>?,
    val instructions: List<String>?,
    val prepTimeMinutes: Int?,
    val cookTimeMinutes: Int?,
    val difficulty: String?,
    val cuisine: String?,
    val image: String?,
    val rating: Double?,
    val difficultyEn: String? = null,
    val cuisineEn: String? = null
)

class RecipeModelDeserializer : JsonDeserializer<RecipeModel> {
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): RecipeModel {
        val obj = if (json.isJsonObject) json.asJsonObject else JsonObject()
        
        // Mapeo básico de TheMealDB a nuestro modelo
        val idMeal = if (obj.has("idMeal") && !obj.get("idMeal").isJsonNull) obj.get("idMeal").asString else null
        val id = idMeal?.toIntOrNull() ?: if (obj.has("id") && !obj.get("id").isJsonNull) obj.get("id").asInt else null
        
        val name = if (obj.has("strMeal") && !obj.get("strMeal").isJsonNull) obj.get("strMeal").asString else if (obj.has("name") && !obj.get("name").isJsonNull) obj.get("name").asString else "Receta sin nombre"
        val image = if (obj.has("strMealThumb") && !obj.get("strMealThumb").isJsonNull) obj.get("strMealThumb").asString else if (obj.has("image") && !obj.get("image").isJsonNull) obj.get("image").asString else null
        val cuisine = if (obj.has("strArea") && !obj.get("strArea").isJsonNull) obj.get("strArea").asString else if (obj.has("cuisine") && !obj.get("cuisine").isJsonNull) obj.get("cuisine").asString else "International"
        val difficulty = if (obj.has("difficulty") && !obj.get("difficulty").isJsonNull) obj.get("difficulty").asString else "Medium"

        // Procesar instrucciones
        val instructionsRaw = if (obj.has("strInstructions") && !obj.get("strInstructions").isJsonNull) obj.get("strInstructions").asString else null
        val instructions = if (instructionsRaw != null) {
            instructionsRaw.split("\r\n", "\n")
                .filter { it.trim().isNotEmpty() }
        } else {
            val list = mutableListOf<String>()
            if (obj.has("instructions") && obj.get("instructions").isJsonArray) {
                obj.get("instructions").asJsonArray.forEach { list.add(it.asString) }
            }
            list
        }

        // Procesar ingredientes
        val ingredients = mutableListOf<String>()
        for (i in 1..20) {
            val ingKey = "strIngredient$i"
            val measKey = "strMeasure$i"
            if (obj.has(ingKey) && !obj.get(ingKey).isJsonNull) {
                val ingredient = obj.get(ingKey).asString
                val measure = if (obj.has(measKey) && !obj.get(measKey).isJsonNull) obj.get(measKey).asString else ""
                if (ingredient.isNotBlank()) {
                    val fullIngredient = if (measure.isNotBlank()) "$measure $ingredient" else ingredient
                    ingredients.add(fullIngredient)
                }
            }
        }
        
        if (ingredients.isEmpty() && obj.has("ingredients") && obj.get("ingredients").isJsonArray) {
            obj.get("ingredients").asJsonArray.forEach { ingredients.add(it.asString) }
        }

        return RecipeModel(
            id = id,
            name = name,
            ingredients = ingredients,
            instructions = instructions,
            prepTimeMinutes = if (obj.has("prepTimeMinutes") && !obj.get("prepTimeMinutes").isJsonNull) obj.get("prepTimeMinutes").asInt else 15,
            cookTimeMinutes = if (obj.has("cookTimeMinutes") && !obj.get("cookTimeMinutes").isJsonNull) obj.get("cookTimeMinutes").asInt else 20,
            difficulty = difficulty,
            cuisine = cuisine,
            image = image,
            rating = if (obj.has("rating") && !obj.get("rating").isJsonNull) obj.get("rating").asDouble else 4.5,
            difficultyEn = difficulty,
            cuisineEn = cuisine
        )
    }
}
