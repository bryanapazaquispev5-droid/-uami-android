package com.example.lab09.ejercicio1.models

import com.google.gson.*
import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import java.lang.reflect.Type

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
        
        // Mapeo para la nueva API Django
        val id = if (obj.has("id") && !obj.get("id").isJsonNull) obj.get("id").asInt else null
        val name = if (obj.has("name") && !obj.get("name").isJsonNull) obj.get("name").asString else "Receta sin nombre"
        
        // Procesar instrucciones (puede venir como String de la API o Array del caché)
        val instructions = mutableListOf<String>()
        if (obj.has("instructions") && !obj.get("instructions").isJsonNull) {
            val element = obj.get("instructions")
            if (element.isJsonArray) {
                element.asJsonArray.forEach { instructions.add(it.asString) }
            } else {
                element.asString.split("\n").filter { it.trim().isNotEmpty() }.forEach { instructions.add(it) }
            }
        }

        // Procesar ingredientes (puede venir como String de la API o Array del caché)
        val ingredients = mutableListOf<String>()
        if (obj.has("ingredients") && !obj.get("ingredients").isJsonNull) {
            val element = obj.get("ingredients")
            if (element.isJsonArray) {
                element.asJsonArray.forEach { ingredients.add(it.asString) }
            } else {
                element.asString.split("\n").filter { it.trim().isNotEmpty() }.forEach { ingredients.add(it) }
            }
        }

        val prepTime = if (obj.has("prepTimeMinutes") && !obj.get("prepTimeMinutes").isJsonNull) obj.get("prepTimeMinutes").asInt 
                       else if (obj.has("prep_time") && !obj.get("prep_time").isJsonNull) obj.get("prep_time").asInt else 15
        
        val cookTime = if (obj.has("cookTimeMinutes") && !obj.get("cookTimeMinutes").isJsonNull) obj.get("cookTimeMinutes").asInt 
                       else if (obj.has("cook_time") && !obj.get("cook_time").isJsonNull) obj.get("cook_time").asInt else 30
                       
        val difficulty = if (obj.has("difficulty") && !obj.get("difficulty").isJsonNull) obj.get("difficulty").asString else "Medium"
        val category = if (obj.has("cuisine") && !obj.get("cuisine").isJsonNull) obj.get("cuisine").asString 
                       else if (obj.has("category") && !obj.get("category").isJsonNull) obj.get("category").asString else "International"
        
        // Manejar URL de imagen (reemplazar localhost/IP por la URL del túnel público de forma segura)
        var image = if (obj.has("image") && !obj.get("image").isJsonNull) obj.get("image").asString else null
        if (image != null) {
            // Extraer solo la parte final de la ruta (ej: /media/recipes/foto.jpg)
            val pathIndex = image.indexOf("/media/")
            if (pathIndex != -1) {
                val imagePath = image.substring(pathIndex)
                // Forzar el uso de la URL pública actual
                image = "https://recetasc24.loca.lt$imagePath"
            }
        }

        return RecipeModel(
            id = id,
            name = name,
            ingredients = ingredients,
            instructions = instructions,
            prepTimeMinutes = prepTime,
            cookTimeMinutes = cookTime,
            difficulty = difficulty,
            cuisine = category,
            image = image,
            rating = 5.0,
            difficultyEn = difficulty,
            cuisineEn = category
        )
    }
}
