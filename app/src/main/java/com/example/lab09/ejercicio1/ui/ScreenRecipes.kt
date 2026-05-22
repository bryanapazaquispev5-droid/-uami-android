package com.example.lab09.ejercicio1.ui

import android.os.Bundle
import androidx.compose.foundation.lazy.grid.GridItemSpan
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.lab09.utils.translateText
import com.example.lab09.utils.translateRecipesListAsync
import com.example.lab09.utils.translateRecipeAsync
import coil.compose.rememberAsyncImagePainter
import com.example.lab09.ejercicio1.models.RecipeModel
import com.example.lab09.ejercicio1.remote.RecipeApiService
import com.example.lab09.ejercicio1.remote.MealDbApiService
import com.example.lab09.ui.theme.*
import java.util.Locale
import android.speech.tts.TextToSpeech

@Composable
fun ScreenRecipeMenu(navController: NavHostController, currentLanguage: MutableState<String>) {
    val isEs = currentLanguage.value == "es"
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Rounded.Restaurant,
            contentDescription = null,
            tint = Primary,
            modifier = Modifier.size(80.dp)
        )
        Spacer(Modifier.height(24.dp))
        Text(
            if (isEs) "Libro de Cocina" else "Cookbook",
            style = MaterialTheme.typography.headlineMedium,
            color = OnBackground
        )
        Text(
            if (isEs) "Explora cientos de recetas profesionales" else "Explore hundreds of professional recipes",
            style = MaterialTheme.typography.bodyLarge,
            color = TextMuted,
            textAlign = TextAlign.Center
        )
        
        Spacer(Modifier.height(48.dp))
        
        Button(
            onClick = { navController.navigate("recetas_lista") },
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Primary),
            shape = RoundedCornerShape(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Rounded.MenuBook, contentDescription = null, tint = OnPrimary)
                Spacer(Modifier.width(12.dp))
                Text(
                    if (isEs) "VER TODAS LAS RECETAS" else "VIEW ALL RECIPES", 
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), 
                    color = OnPrimary
                )
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        OutlinedButton(
            onClick = { navController.navigate("recetas_favoritos") },
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, Primary.copy(alpha = 0.3f))
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Favorite, contentDescription = null, tint = Primary)
                Spacer(Modifier.width(12.dp))
                Text(if (isEs) "MIS FAVORITOS" else "MY FAVORITES", color = Primary)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenRecipes(
    navController: NavHostController, 
    servicio: RecipeApiService, 
    servicioMealDB: MealDbApiService, 
    favoritos: MutableList<Int>, 
    currentLanguage: MutableState<String>,
    preloadedRecipes: List<RecipeModel> = emptyList()
) {
    val isEs = currentLanguage.value == "es"
    var allRecipes by remember { mutableStateOf(preloadedRecipes) }
    var isLoading by remember { mutableStateOf(preloadedRecipes.isEmpty()) }
    var isRefreshing by remember { mutableStateOf(false) }
    var currentPage by remember { mutableIntStateOf(1) }
    var searchQuery by remember { mutableStateOf("") }
    val limit = 8
    
    var sortOrder by remember { mutableStateOf("Default") } 
    var selectedDifficulty by remember { mutableStateOf("All") } 
    var selectedCuisine by remember { mutableStateOf("All") }

    // Cargar todas las recetas de ambas fuentes (SOLO si no hay pre-cargadas o si se refresca)
    LaunchedEffect(isRefreshing, currentLanguage.value) {
        if (allRecipes.isNotEmpty() && !isRefreshing) return@LaunchedEffect
        
        isLoading = true
        try {
            // 1. Obtener de DummyJSON
            val response1 = servicio.getRecipes(limit = 50, skip = 0)
            val recipes1 = response1.recipes ?: emptyList()
            
            // 2. Obtener de TheMealDB
            val response2 = servicioMealDB.getRecipes(limit = 50, skip = 0)
            val recipes2 = response2.recipes ?: emptyList()
            
            // 3. Combinar
            val rawRecipes = recipes1 + recipes2
            
            allRecipes = if (currentLanguage.value == "es") {
                translateRecipesListAsync(rawRecipes, "es")
            } else {
                rawRecipes
            }
        } catch (e: Exception) {
            Log.e("RECIPES_UI", "Error: ${e.message}")
        } finally {
            isLoading = false
            isRefreshing = false
        }
    }

    // Resetear a la página 1 cuando cambian los filtros o la búsqueda
    LaunchedEffect(searchQuery, selectedDifficulty, selectedCuisine, sortOrder) {
        currentPage = 1
    }

    // 1. Filtrar y Ordenar la lista completa
    val filteredAndSortedRecipes = remember(allRecipes, searchQuery, sortOrder, selectedDifficulty, selectedCuisine) {
        var result = allRecipes
        
        // Búsqueda por texto
        if (searchQuery.isNotEmpty()) {
            result = result.filter { 
                it.name?.contains(searchQuery, ignoreCase = true) == true ||
                it.cuisine?.contains(searchQuery, ignoreCase = true) == true
            }
        }

        // Filtro de Dificultad
        if (selectedDifficulty != "All") {
            result = result.filter { it.difficultyEn?.equals(selectedDifficulty, ignoreCase = true) == true }
        }
        
        // Filtro de Cocina
        if (selectedCuisine != "All") {
            result = result.filter { it.cuisineEn?.equals(selectedCuisine, ignoreCase = true) == true }
        }

        // Ordenamiento
        when (sortOrder) {
            "A-Z" -> result.sortedBy { it.name }
            "Z-A" -> result.sortedByDescending { it.name }
            "Rating" -> result.sortedByDescending { it.rating }
            "Time" -> result.sortedBy { (it.prepTimeMinutes ?: 0) + (it.cookTimeMinutes ?: 0) }
            "Ingredients" -> result.sortedByDescending { it.ingredients?.size ?: 0 }
            else -> result
        }
    }

    // 2. Calcular paginación sobre la lista filtrada
    val totalFiltered = filteredAndSortedRecipes.size
    val totalPages = if (totalFiltered > 0) (totalFiltered + limit - 1) / limit else 1
    
    // 3. Obtener solo las recetas de la página actual
    val recipesToDisplay = remember(filteredAndSortedRecipes, currentPage) {
        val start = (currentPage - 1) * limit
        val end = minOf(start + limit, totalFiltered)
        if (start < totalFiltered) filteredAndSortedRecipes.subList(start, end)
        else emptyList()
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(Background).padding(top = 24.dp, start = 8.dp, end = 24.dp, bottom = 12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = OnBackground)
                        }
                        Text(if (isEs) "Recetas" else "Recipes", style = MaterialTheme.typography.headlineMedium, color = OnBackground)
                    }
                    
                    // Menú de Filtro
                    var showSortMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(
                                Icons.Rounded.FilterList, 
                                null, 
                                tint = if (sortOrder == "Default") OnBackground else Primary
                            )
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false },
                            modifier = Modifier.background(Surface)
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (isEs) "Por defecto" else "Default", color = OnSurface) },
                                onClick = { sortOrder = "Default"; showSortMenu = false },
                                leadingIcon = { Icon(Icons.AutoMirrored.Rounded.Sort, null, tint = Primary) }
                            )
                            DropdownMenuItem(
                                text = { Text(if (isEs) "Nombre A-Z" else "Name A-Z", color = OnSurface) },
                                onClick = { sortOrder = "A-Z"; showSortMenu = false },
                                leadingIcon = { Icon(Icons.Rounded.SortByAlpha, null, tint = Primary) }
                            )
                            DropdownMenuItem(
                                text = { Text(if (isEs) "Nombre Z-A" else "Name Z-A", color = OnSurface) },
                                onClick = { sortOrder = "Z-A"; showSortMenu = false },
                                leadingIcon = { Icon(Icons.Rounded.SortByAlpha, null, tint = Primary) }
                            )
                            DropdownMenuItem(
                                text = { Text(if (isEs) "Mejor valorados" else "Top Rated", color = OnSurface) },
                                onClick = { sortOrder = "Rating"; showSortMenu = false },
                                leadingIcon = { Icon(Icons.Rounded.Star, null, tint = Primary) }
                            )
                            DropdownMenuItem(
                                text = { Text(if (isEs) "Menor tiempo" else "Fastest", color = OnSurface) },
                                onClick = { sortOrder = "Time"; showSortMenu = false },
                                leadingIcon = { Icon(Icons.Rounded.Timer, null, tint = Primary) }
                            )
                            DropdownMenuItem(
                                text = { Text(if (isEs) "Más ingredientes" else "Most Ingredients", color = OnSurface) },
                                onClick = { sortOrder = "Ingredients"; showSortMenu = false },
                                leadingIcon = { Icon(Icons.Rounded.Kitchen, null, tint = Primary) }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    placeholder = { Text(if(isEs) "Buscar receta..." else "Search recipe...", color = TextMuted) },
                    leadingIcon = { Icon(Icons.Rounded.Search, null, tint = Primary) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Rounded.Close, null, tint = TextMuted)
                            }
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = Surface,
                        focusedContainerColor = Surface,
                        unfocusedContainerColor = Surface
                    ),
                    singleLine = true
                )

                Spacer(Modifier.height(12.dp))

                // Scrollable Cuisines
                val cuisines = remember(allRecipes) {
                    val list = allRecipes.mapNotNull { it.cuisineEn }.distinct().sorted().toMutableList()
                    list.add(0, "All")
                    list
                }
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    cuisines.forEach { cuisine ->
                        val selected = selectedCuisine == cuisine
                        FilterChip(
                            selected = selected,
                            onClick = { selectedCuisine = cuisine },
                            label = { 
                                val labelText = if(cuisine == "All") (if(isEs) "Cocinas" else "Cuisines") else translateText(cuisine, currentLanguage.value)
                                Text(labelText) 
                            },
                            leadingIcon = { if(selected) Icon(Icons.Rounded.Check, null, Modifier.size(16.dp)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Secondary.copy(alpha = 0.2f),
                                selectedLabelColor = Secondary,
                                selectedLeadingIconColor = Secondary,
                                containerColor = Surface,
                                labelColor = TextMuted
                            ),
                            border = null,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Chips de Dificultad
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val difficulties = remember(allRecipes) {
                        val list = allRecipes.mapNotNull { it.difficultyEn }.distinct().sorted().toMutableList()
                        list.add(0, "All")
                        list
                    }
                    difficulties.forEach { diff ->
                        val selected = selectedDifficulty == diff
                        FilterChip(
                            selected = selected,
                            onClick = { selectedDifficulty = diff },
                            label = { 
                                val labelText = if(diff == "All") (if(isEs) "Dificultad" else "Difficulty") else translateText(diff, currentLanguage.value)
                                Text(labelText) 
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Primary,
                                selectedLabelColor = OnPrimary,
                                containerColor = Surface,
                                labelColor = TextMuted
                            ),
                            border = null,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }
        },
        bottomBar = {
            PaginationControls(
                currentPage = currentPage,
                totalPages = totalPages,
                currentLanguage = currentLanguage,
                onPageChange = { currentPage = it }
            )
        },
        containerColor = Background
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { isRefreshing = true },
            modifier = Modifier.padding(padding).fillMaxSize()
        ) {
            if (isLoading && !isRefreshing) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Primary)
                }
            } else if (recipesToDisplay.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No se encontraron recetas", color = TextMuted)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(recipesToDisplay, key = { it.id ?: 0 }) { recipe ->
                        RecipeCardPremium(
                            recipe = recipe,
                            isFav = favoritos.contains(recipe.id),
                            onFavToggle = {
                                if (favoritos.contains(recipe.id)) {
                                    recipe.id?.let { favoritos.remove(it) }
                                } else {
                                    favoritos.add(recipe.id ?: 0)
                                }
                            },
                            currentLanguage = currentLanguage,
                            onClick = { navController.navigate("recipeDetail/${recipe.id ?: 0}") }
                        )
                    }
                    item(span = { GridItemSpan(2) }) {
                        Spacer(Modifier.height(80.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ScreenFavorites(navController: NavHostController, servicio: RecipeApiService, servicioMealDB: MealDbApiService, favoritos: List<Int>, currentLanguage: MutableState<String>) {
    val isEs = currentLanguage.value == "es"
    var listaFavoritos by remember { mutableStateOf<List<RecipeModel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(favoritos.size, currentLanguage.value) {
        isLoading = true
        val listadoRaw = mutableListOf<RecipeModel>()
        favoritos.forEach { id ->
            try {
                // Intentar en ambas fuentes (DummyJSON y TheMealDB)
                val response1 = servicio.getRecipeById(id)
                val recipe1 = response1.recipes?.firstOrNull()
                
                if (recipe1 != null) {
                    listadoRaw.add(recipe1)
                } else {
                    val response2 = servicioMealDB.getRecipeById(id)
                    val recipe2 = response2.recipes?.firstOrNull()
                    if (recipe2 != null) listadoRaw.add(recipe2)
                }
            } catch (e: Exception) { 
                // Si falla en una, intentar en la otra
                try {
                    val response2 = servicioMealDB.getRecipeById(id)
                    val recipe2 = response2.recipes?.firstOrNull()
                    if (recipe2 != null) listadoRaw.add(recipe2)
                } catch (e2: Exception) { e2.printStackTrace() }
            }
        }
        
        listaFavoritos = if (currentLanguage.value == "es") {
            translateRecipesListAsync(listadoRaw, "es")
        } else {
            listadoRaw
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(Background).padding(top = 24.dp, start = 8.dp, end = 24.dp, bottom = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = OnBackground)
                    }
                    Text(if (isEs) "Mis Favoritos" else "My Favorites", style = MaterialTheme.typography.headlineMedium, color = OnBackground)
                }
            }
        },
        containerColor = Background
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
        } else if (listaFavoritos.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(if (isEs) "No tienes recetas favoritas aún" else "You don't have favorite recipes yet", color = TextMuted)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize().padding(padding)
            ) {
                items(listaFavoritos, key = { it.id ?: 0 }) { recipe ->
                    RecipeCardPremium(
                        recipe = recipe,
                        isFav = true,
                        onFavToggle = {
                            if (favoritos is MutableList) {
                                recipe.id?.let { favoritos.remove(it) }
                            }
                        },
                        currentLanguage = currentLanguage,
                        onClick = { navController.navigate("recipeDetail/${recipe.id ?: 0}") }
                    )
                }
            }
        }
    }
}

@Composable
fun PaginationControls(currentPage: Int, totalPages: Int, currentLanguage: MutableState<String>, onPageChange: (Int) -> Unit) {
    val isEs = currentLanguage.value == "es"
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
        color = Surface,
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { if (currentPage > 1) onPageChange(currentPage - 1) },
                enabled = currentPage > 1,
                modifier = Modifier.background(if (currentPage > 1) Primary else Background, CircleShape)
            ) { Icon(Icons.Rounded.ChevronLeft, null, tint = if (currentPage > 1) OnPrimary else TextMuted) }

            Text(
                if (isEs) "Página $currentPage de $totalPages" else "Page $currentPage of $totalPages", 
                style = MaterialTheme.typography.labelLarge, 
                color = OnSurface
            )

            IconButton(
                onClick = { if (currentPage < totalPages) onPageChange(currentPage + 1) },
                enabled = currentPage < totalPages,
                modifier = Modifier.background(if (currentPage < totalPages) Primary else Background, CircleShape)
            ) { Icon(Icons.Rounded.ChevronRight, null, tint = if (currentPage < totalPages) OnPrimary else TextMuted) }
        }
    }
}

@Composable
fun RecipeCardPremium(
    recipe: RecipeModel,
    isFav: Boolean = false,
    onFavToggle: () -> Unit = {},
    currentLanguage: MutableState<String>,
    onClick: () -> Unit
) {
    val lang = currentLanguage.value
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Column {
            Box {
                Image(
                    painter = rememberAsyncImagePainter(model = recipe.image),
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(28.dp)),
                    contentScale = ContentScale.Crop
                )
                
                // Botón de Favorito
                Surface(
                    modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
                    color = Color.Black.copy(alpha = 0.5f),
                    shape = CircleShape
                ) {
                    IconButton(onClick = onFavToggle, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            tint = if (isFav) Color.Red else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Surface(
                    modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Star, null, tint = Primary, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("${recipe.rating}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Column(Modifier.padding(16.dp)) {
                Text(
                    translateText(recipe.cuisine, lang).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Primary,
                    fontWeight = FontWeight.Black
                )
                Text(
                    translateText(recipe.name, lang),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = OnSurface
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Timer, null, tint = TextMuted, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("${(recipe.prepTimeMinutes ?: 0) + (recipe.cookTimeMinutes ?: 0)} min", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenRecipeDetail(navController: NavHostController, servicio: RecipeApiService, servicioMealDB: MealDbApiService, id: Int, favoritos: MutableList<Int>, currentLanguage: MutableState<String>) {
    var recipe by remember { mutableStateOf<RecipeModel?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(id, currentLanguage.value) {
        isLoading = true
        try { 
            // Intentar buscar el ID en ambas APIs
            val response1 = servicio.getRecipeById(id)
            val rawRecipe1 = response1.recipes?.firstOrNull()
            
            val finalRecipe = if (rawRecipe1 != null && rawRecipe1.name != "Receta sin nombre") {
                rawRecipe1
            } else {
                val response2 = servicioMealDB.getRecipeById(id)
                response2.recipes?.firstOrNull()
            }

            recipe = if (currentLanguage.value == "es" && finalRecipe != null) {
                translateRecipeAsync(finalRecipe, "es")
            } else {
                finalRecipe
            }
        } catch (e: Exception) { 
            // Reintento en la segunda API si la primera falla catastróficamente
            try {
                val response2 = servicioMealDB.getRecipeById(id)
                val finalRecipe = response2.recipes?.firstOrNull()
                recipe = if (currentLanguage.value == "es" && finalRecipe != null) {
                    translateRecipeAsync(finalRecipe, "es")
                } else {
                    finalRecipe
                }
            } catch (e2: Exception) { e2.printStackTrace() }
        }
        finally { isLoading = false }
    }

    if (isLoading) {
        Box(Modifier.fillMaxSize().background(Background), contentAlignment = Alignment.Center) { 
            CircularProgressIndicator(color = Primary) 
        }
    } else if (recipe != null) {
        val isFav = favoritos.contains(recipe?.id)
        
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).background(Background)) {
            Box {
                Image(
                    painter = rememberAsyncImagePainter(model = recipe?.image),
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(400.dp),
                    contentScale = ContentScale.Crop
                )
                Box(modifier = Modifier.fillMaxWidth().height(400.dp).background(Brush.verticalGradient(listOf(Color.Transparent, Background))))
                
                // Botones superiores en detalle
                Row(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = { navController.popBackStack() },
                        color = Background.copy(alpha = 0.5f), 
                        shape = CircleShape
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack, 
                            null, 
                            tint = Color.White, 
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                    
                    Surface(color = Background.copy(alpha = 0.5f), shape = CircleShape) {
                        IconButton(onClick = {
                            if (isFav) favoritos.remove(recipe?.id)
                            else favoritos.add(recipe?.id ?: 0)
                        }) {
                            Icon(
                                imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = null,
                                tint = if (isFav) Color.Red else Color.White
                            )
                        }
                    }
                }
            }
            
            Column(modifier = Modifier.offset(y = (-40).dp).padding(horizontal = 24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(color = Primary, shape = RoundedCornerShape(12.dp)) {
                        Text(
                            recipe?.cuisine ?: "Internacional",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = OnPrimary)
                        )
                    }

                    Button(
                        onClick = { navController.navigate("cookingMode/${recipe?.id}") },
                        colors = ButtonDefaults.buttonColors(containerColor = Secondary),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.OutdoorGrill, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("MODO COCINA", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text(
                    translateText(recipe?.name, currentLanguage.value),
                    style = MaterialTheme.typography.headlineMedium,
                    color = OnBackground
                )
                
                Spacer(Modifier.height(32.dp))
                
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    val isEs = currentLanguage.value == "es"
                    DetailBadge(Icons.Rounded.Timer, if(isEs) "Tiempo" else "Time", "${(recipe?.prepTimeMinutes ?: 0) + (recipe?.cookTimeMinutes ?: 0)}m")
                    DetailBadge(Icons.Rounded.SignalCellularAlt, if(isEs) "Nivel" else "Level", translateText(recipe?.difficulty, currentLanguage.value))
                    DetailBadge(Icons.Rounded.LocalFireDepartment, if(isEs) "Calorías" else "Calories", "450 kcal")
                }
                
                Spacer(Modifier.height(40.dp))
                
                SectionTitle(if(currentLanguage.value == "es") "Ingredientes" else "Ingredients")
                recipe?.ingredients?.forEach { IngredientRow(translateText(it, currentLanguage.value)) }
                
                Spacer(Modifier.height(40.dp))
                
                SectionTitle(if(currentLanguage.value == "es") "Preparación" else "Preparation")
                recipe?.instructions?.forEachIndexed { i, s -> StepRow(i + 1, translateText(s, currentLanguage.value)) }
                
                Spacer(Modifier.height(100.dp))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ScreenCookingMode(
    navController: NavHostController, 
    servicio: RecipeApiService, 
    servicioMealDB: MealDbApiService,
    id: Int,
    tts: TextToSpeech?,
    onSpeechFinished: MutableState<(() -> Unit)?>,
    currentLanguage: MutableState<String>
) {
    var recipe by remember { mutableStateOf<RecipeModel?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(id, currentLanguage.value) {
        isLoading = true
        try { 
            val response1 = servicio.getRecipeById(id)
            val rawRecipe1 = response1.recipes?.firstOrNull()
            
            val finalRecipe = if (rawRecipe1 != null && rawRecipe1.name != "Receta sin nombre") {
                rawRecipe1
            } else {
                val response2 = servicioMealDB.getRecipeById(id)
                response2.recipes?.firstOrNull()
            }

            recipe = if (currentLanguage.value == "es" && finalRecipe != null) {
                translateRecipeAsync(finalRecipe, "es")
            } else {
                finalRecipe
            }
        } catch (e: Exception) { 
            try {
                val response2 = servicioMealDB.getRecipeById(id)
                val finalRecipe = response2.recipes?.firstOrNull()
                recipe = if (currentLanguage.value == "es" && finalRecipe != null) {
                    translateRecipeAsync(finalRecipe, "es")
                } else {
                    finalRecipe
                }
            } catch (e2: Exception) { e2.printStackTrace() }
        }
        finally { isLoading = false }
    }

    if (isLoading) {
        Box(Modifier.fillMaxSize().background(Background), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Primary)
        }
    } else if (recipe != null) {
        val instructions = recipe?.instructions ?: emptyList()
        val ingredients = recipe?.ingredients ?: emptyList()
        val pagerState = rememberPagerState(pageCount = { instructions.size + 1 })

        val frasesMotivacionales = if (currentLanguage.value == "es") {
            listOf(
                "¡Hoy vas a crear una obra maestra!",
                "El ingrediente secreto siempre es el amor.",
                "¡A cocinar se ha dicho, Chef!",
                "Tu cocina, tus reglas. ¡A disfrutar!",
                "Huele a que algo delicioso está en camino."
            )
        } else {
            listOf(
                "Today you are going to create a masterpiece!",
                "The secret ingredient is always love.",
                "Let's get cooking, Chef!",
                "Your kitchen, your rules. Enjoy!",
                "It smells like something delicious is on the way."
            )
        }
        val fraseBienvenida = remember { frasesMotivacionales.random() }

        // Control de Narración Automática
        LaunchedEffect(pagerState.currentPage, currentLanguage.value) {
            tts?.stop()
            val index = pagerState.currentPage
            val isEs = currentLanguage.value == "es"
            
            if (index == 0) {
                val intro = if (isEs) {
                    "$fraseBienvenida. Empecemos revisando los ingredientes."
                } else {
                    "$fraseBienvenida. Let's start by reviewing the ingredients."
                }
                tts?.speak(intro, TextToSpeech.QUEUE_FLUSH, null, null)
            } else {
                val total = instructions.size
                val textoInstruccion = instructions[index - 1]
                
                val transicion = if (isEs) {
                    when(index) {
                        1 -> "¡Muy bien! Empecemos con el primer paso. "
                        total -> "¡Ya casi terminamos! El último paso es: "
                        else -> "Siguiente paso, número $index. "
                    }
                } else {
                    when(index) {
                        1 -> "Alright! Let's start with the first step. "
                        total -> "Almost done! The final step is: "
                        else -> "Next step, number $index. "
                    }
                }
                
                tts?.speak("$transicion $textoInstruccion", TextToSpeech.QUEUE_FLUSH, null, null)
            }
        }

        Column(modifier = Modifier.fillMaxSize().background(Background)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { 
                    tts?.stop()
                    navController.popBackStack() 
                }) {
                    Icon(Icons.Rounded.Close, null, tint = OnBackground)
                }
                Column {
                    Text(
                        if (currentLanguage.value == "es") "Modo Cocina" else "Cooking Mode", 
                        style = MaterialTheme.typography.labelSmall, 
                        color = Primary
                    )
                    Text(
                        recipe?.name ?: "",
                        style = MaterialTheme.typography.titleMedium,
                        color = OnBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 32.dp),
                pageSpacing = 16.dp
            ) { pageIndex ->
                if (pageIndex == 0) {
                    ShoppingListCard(ingredients, tts, pagerState, onSpeechFinished, currentLanguage)
                } else {
                    CookingStepCard(
                        stepNumber = pageIndex,
                        instruction = instructions[pageIndex - 1],
                        totalSteps = instructions.size,
                        currentLanguage = currentLanguage
                    )
                }
            }

            // Footer / Progress
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp, top = 16.dp).navigationBarsPadding(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(instructions.size + 1) { index ->
                    val color = if (pagerState.currentPage == index) Primary else Surface
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (pagerState.currentPage == index) 10.dp else 6.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ShoppingListCard(
    ingredients: List<String>, 
    tts: TextToSpeech? = null,
    pagerState: PagerState? = null,
    onSpeechFinished: MutableState<(() -> Unit)?>? = null,
    currentLanguage: MutableState<String>
) {
    val checkedState = remember { mutableStateMapOf<Int, Boolean>() }
    val scope = rememberCoroutineScope()
    val isEs = currentLanguage.value == "es"
    
    val frasesCompletado = if (isEs) {
        listOf("¡Listo!", "Entendido.", "Ya lo tenemos.", "Perfecto.")
    } else {
        listOf("Got it!", "Checked.", "Ready.", "Done.")
    }

    Surface(
        modifier = Modifier.fillMaxSize().padding(vertical = 32.dp),
        color = Surface,
        shape = RoundedCornerShape(32.dp),
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.ShoppingCart, null, tint = Primary, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(12.dp))
                Text(
                    if (isEs) "LISTA DE COMPRAS" else "SHOPPING LIST",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                    color = OnSurface
                )
            }
            
            Text(
                if (isEs) "Marca los ingredientes que ya tienes" else "Mark the ingredients you already have",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
                modifier = Modifier.padding(bottom = 24.dp, top = 4.dp)
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(ingredients.size) { index ->
                    val isChecked = checkedState[index] ?: false
                    IngredientCheckItem(
                        text = ingredients[index],
                        isChecked = isChecked,
                        onCheckedChange = { nuevoEstado -> 
                            checkedState[index] = nuevoEstado
                            if (nuevoEstado) {
                                val frase = frasesCompletado.random()
                                tts?.speak(frase, TextToSpeech.QUEUE_FLUSH, null, null)
                            }
                        }
                    )
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            val totalChecked = checkedState.values.count { it }
            val completado = if (ingredients.isNotEmpty()) totalChecked == ingredients.size else false
            
            // Si termina todo, que lo celebre y pase de página automáticamente
            LaunchedEffect(completado) {
                if (completado) {
                    delay(500)
                    
                    onSpeechFinished?.value = {
                        scope.launch {
                            delay(500)
                            pagerState?.animateScrollToPage(1)
                        }
                    }
                    
                    val congrats = if (isEs) {
                        "¡Excelente! Ya tenemos todos los ingredientes listos. Empecemos a cocinar."
                    } else {
                        "Excellent! We have all the ingredients ready. Let's start cooking."
                    }

                    tts?.speak(
                        congrats,
                        TextToSpeech.QUEUE_FLUSH,
                        Bundle().apply { 
                            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "FINAL_SHOPPING") 
                        },
                        "FINAL_SHOPPING"
                    )
                }
            }

            LinearProgressIndicator(
                progress = { if (ingredients.isNotEmpty()) totalChecked.toFloat() / ingredients.size else 0f },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                color = if (completado) Secondary else Primary,
                trackColor = Background
            )
            Text(
                if (isEs) "Progreso: $totalChecked de ${ingredients.size}" else "Progress: $totalChecked of ${ingredients.size}",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                modifier = Modifier.padding(top = 8.dp).align(Alignment.End)
            )
        }
    }
}

@Composable
fun IngredientCheckItem(text: String, isChecked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Surface(
        onClick = { onCheckedChange(!isChecked) },
        color = if (isChecked) Secondary.copy(alpha = 0.1f) else Background.copy(alpha = 0.3f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            1.dp, 
            if (isChecked) Secondary.copy(alpha = 0.5f) else Color.Transparent
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isChecked,
                onCheckedChange = onCheckedChange,
                colors = CheckboxDefaults.colors(
                    checkedColor = Secondary,
                    uncheckedColor = TextMuted,
                    checkmarkColor = OnPrimary
                )
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge.copy(
                    textDecoration = if (isChecked) TextDecoration.LineThrough else null
                ),
                color = if (isChecked) TextMuted else OnSurface
            )
        }
    }
}

@Composable
fun CookingStepCard(
    stepNumber: Int, 
    instruction: String, 
    totalSteps: Int,
    currentLanguage: MutableState<String>
) {
    val isEs = currentLanguage.value == "es"
    
    Surface(
        modifier = Modifier.fillMaxSize().padding(vertical = 32.dp),
        color = Surface,
        shape = RoundedCornerShape(32.dp),
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(32.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Primary.copy(alpha = 0.1f),
                    shape = CircleShape,
                    border = BorderStroke(2.dp, Primary)
                ) {
                    Text(
                        if (isEs) "PASO $stepNumber / $totalSteps" else "STEP $stepNumber / $totalSteps",
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                        color = Primary
                    )
                }

                // Indicador visual de narración activa
                Icon(
                    Icons.Rounded.RecordVoiceOver,
                    null,
                    tint = Primary.copy(alpha = 0.6f),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(Modifier.height(48.dp))

            Text(
                text = instruction,
                style = MaterialTheme.typography.headlineMedium.copy(
                    lineHeight = 42.sp,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = OnSurface,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(48.dp))

            // Lógica de temporizador si se detectan minutos en el texto
            val minutes = remember(instruction) {
                val regex = """(\d+)\s*(minutos|min|minutes)""".toRegex(RegexOption.IGNORE_CASE)
                regex.find(instruction)?.groupValues?.get(1)?.toIntOrNull()
            }

            if (minutes != null) {
                CookingTimer(minutes, currentLanguage)
            }
        }
    }
}

@Composable
fun CookingTimer(initialMinutes: Int, currentLanguage: MutableState<String>) {
    val isEs = currentLanguage.value == "es"
    var timeLeft by remember { mutableIntStateOf(initialMinutes * 60) }
    var isRunning by remember { mutableStateOf(false) }

    LaunchedEffect(isRunning, timeLeft) {
        if (isRunning && timeLeft > 0) {
            delay(1000L)
            timeLeft -= 1
        } else if (timeLeft == 0) {
            isRunning = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Background.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val minutes = timeLeft / 60
        val seconds = timeLeft % 60
        Text(
            text = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds),
            style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Black),
            color = if (timeLeft == 0) Secondary else Primary
        )
        Text(
            text = if (timeLeft == 0) (if (isEs) "¡LISTO!" else "DONE!") else (if (isEs) "TEMPORIZADOR" else "TIMER"),
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted
        )
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { isRunning = !isRunning },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRunning) Color.Red.copy(alpha = 0.2f) else Secondary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(if (isRunning) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null)
                Spacer(Modifier.width(8.dp))
                Text(if (isRunning) (if (isEs) "PAUSAR" else "PAUSE") else (if (isEs) "INICIAR" else "START"))
            }
            
            OutlinedButton(
                onClick = { 
                    timeLeft = initialMinutes * 60
                    isRunning = false
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Rounded.Refresh, null, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun DetailBadge(icon: ImageVector, label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = Primary, modifier = Modifier.size(24.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
        Text(value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = OnSurface)
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleLarge,
        color = OnBackground,
        modifier = Modifier.padding(bottom = 16.dp)
    )
}

@Composable
fun IngredientRow(text: String) {
    Row(
        modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth().background(Surface, RoundedCornerShape(16.dp)).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(8.dp).background(Primary, CircleShape))
        Spacer(Modifier.width(16.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = OnSurface)
    }
}

@Composable
fun StepRow(index: Int, text: String) {
    Row(modifier = Modifier.padding(vertical = 12.dp)) {
        Surface(
            modifier = Modifier.size(32.dp),
            shape = CircleShape,
            color = Surface,
            border = BorderStroke(1.dp, Primary.copy(alpha = 0.3f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("$index", style = MaterialTheme.typography.labelMedium, color = Primary)
            }
        }
        Spacer(Modifier.width(16.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 26.sp), color = OnSurface)
    }
}
