package com.example.uami.utils

import com.example.uami.recipes.models.RecipeModel

fun translateText(text: String?, targetLanguage: String): String {
    if (text == null) return ""
    if (targetLanguage == "en") return text

    // Diccionario MASIVO para cubrir la mayoría de términos de la API dummyjson
    val dictionary = mapOf(
        // Cocinas / Categorías
        "Italian" to "Italiana",
        "Mexican" to "Mexicana",
        "Asian" to "Asiática",
        "Indian" to "India",
        "French" to "Francesa",
        "American" to "Americana",
        "Greek" to "Griega",
        "Japanese" to "Japonesa",
        "Chinese" to "China",
        "Turkish" to "Turca",
        "Mediterranean" to "Mediterránea",
        "Pakistani" to "Pakistaní",
        "Spanish" to "Española",
        "Moroccan" to "Marroquí",
        
        // Tipos de comida / Meal types
        "Breakfast" to "Desayuno",
        "Lunch" to "Almuerzo",
        "Dinner" to "Cena",
        "Dessert" to "Postre",
        "Beverage" to "Bebida",
        "Appetizer" to "Entrada",
        "Snack" to "Snack",
        
        // Niveles
        "Easy" to "Fácil",
        "Medium" to "Media",
        "Hard" to "Difícil",
        
        // Proteínas
        "Chicken" to "Pollo",
        "Beef" to "Carne de res",
        "Pork" to "Cerdo",
        "Fish" to "Pescado",
        "Salmon" to "Salmón",
        "Shrimp" to "Camarones",
        "Prawns" to "Gambas",
        "Eggs" to "Huevos",
        "Egg" to "Huevo",
        "Bacon" to "Tocino",
        "Ham" to "Jamón",
        "Lamb" to "Cordero",
        "Turkey" to "Pavo",
        
        // Vegetales y Frutas
        "Tomato" to "Tomate",
        "Tomatoes" to "Tomates",
        "Onion" to "Cebolla",
        "Garlic" to "Ajo",
        "Potato" to "Papa",
        "Potatoes" to "Papas",
        "Carrot" to "Zanahoria",
        "Carrots" to "Zanahorias",
        "Cucumber" to "Pepino",
        "Lettuce" to "Lechuga",
        "Spinach" to "Espinaca",
        "Mushroom" to "Champiñón",
        "Mushrooms" to "Champiñones",
        "Pepper" to "Pimiento",
        "Peppers" to "Pimientos",
        "Avocado" to "Aguacate",
        "Broccoli" to "Brócoli",
        "Lemon" to "Limón",
        "Lime" to "Limón verde",
        "Apple" to "Manzana",
        "Banana" to "Plátano",
        "Ginger" to "Jengibre",
        
        // Lácteos y Despensa
        "Milk" to "Leche",
        "Cheese" to "Queso",
        "Butter" to "Mantequilla",
        "Cream" to "Crema",
        "Yogurt" to "Yogur",
        "Flour" to "Harina",
        "Sugar" to "Azúcar",
        "Honey" to "Miel",
        "Oil" to "Aceite",
        "Salt" to "Sal",
        "Pepper" to "Pimienta",
        "Water" to "Agua",
        "Rice" to "Arroz",
        "Pasta" to "Pasta",
        "Bread" to "Pan",
        "Vinegar" to "Vinagre",
        "Soy sauce" to "Salsa de soja",
        "Olive oil" to "Aceite de oliva",
        
        // Verbos / Acciones
        "Preheat" to "Precalentar",
        "Bake" to "Hornear",
        "Cook" to "Cocinar",
        "Mix" to "Mezclar",
        "Whisk" to "Batir",
        "Add" to "Añadir",
        "Stir" to "Revolver",
        "Heat" to "Calentar",
        "Boil" to "Hervir",
        "Fry" to "Freír",
        "Serve" to "Servir",
        "Cut" to "Cortar",
        "Chop" to "Picar",
        "Slice" to "Rebanar",
        "Drain" to "Escurrir",
        "Season" to "Sazonar",
        "Grill" to "Asar",
        "Roast" to "Rostizar",
        "Marinate" to "Marinar",
        
        // Medidas y Tiempos
        "minutes" to "minutos",
        "minute" to "minuto",
        "hour" to "hora",
        "hours" to "horas",
        "cup" to "taza",
        "cups" to "tazas",
        "tablespoon" to "cucharada",
        "teaspoon" to "cucharadita",
        "clove" to "diente",
        "cloves" to "dientes",
        "ounce" to "onza",
        "pounds" to "libras",
        "gram" to "gramo",
        "ml" to "ml"
    )

    var translated = text!!
    dictionary.forEach { (en, es) ->
        // Reemplazo inteligente: busca la palabra completa e ignora mayúsculas
        val regex = "\\b$en\\b".toRegex(RegexOption.IGNORE_CASE)
        translated = translated.replace(regex, es)
    }
    
    // Traducciones de títulos comunes de la API dummyjson
    return translated
        .replace("Classic Margherita Pizza", "Pizza Margherita Clásica")
        .replace("Vegetarian Stir-Fry", "Salteado Vegetariano")
        .replace("Chocolate Chip Cookies", "Galletas con Chispas de Chocolate")
        .replace("Chicken Alfredo Pasta", "Pasta Alfredo con Pollo")
        .replace("Japanese Ramen Soup", "Ramen Japonés")
        .replace("Beef and Broccoli", "Carne con Brócoli")
}

// NUEVAS FUNCIONES PARA TRADUCCIÓN AUTOMÁTICA
suspend fun translateRecipeAsync(recipe: RecipeModel, targetLanguage: String): RecipeModel {
    if (targetLanguage != "es") return recipe
    
    return recipe.copy(
        name = OnDeviceTranslator.translate(recipe.name),
        cuisine = OnDeviceTranslator.translate(recipe.cuisine),
        difficulty = OnDeviceTranslator.translate(recipe.difficulty),
        mealType = OnDeviceTranslator.translate(recipe.mealType),
        ingredients = recipe.ingredients?.map { OnDeviceTranslator.translate(it) },
        instructions = recipe.instructions?.map { OnDeviceTranslator.translate(it) },
        difficultyEn = recipe.difficultyEn,
        cuisineEn = recipe.cuisineEn,
        mealTypeEn = recipe.mealTypeEn
    )
}

suspend fun translateRecipesListAsync(recipes: List<RecipeModel>, targetLanguage: String): List<RecipeModel> {
    if (targetLanguage != "es") return recipes
    return recipes.map { translateRecipeAsync(it, targetLanguage) }
}

