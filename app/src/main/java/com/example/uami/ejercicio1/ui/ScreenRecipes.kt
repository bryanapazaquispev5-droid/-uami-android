package com.example.uami.ejercicio1.ui

import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.uami.ejercicio1.models.RecipeModel
import com.example.uami.ejercicio1.remote.RecipeApiService
import com.example.uami.ui.theme.*
import com.example.uami.utils.translateRecipeAsync
import com.example.uami.utils.translateRecipesListAsync
import com.example.uami.utils.translateText
import com.example.uami.ui.filters.*
import kotlinx.coroutines.launch

@Composable
fun ScreenRecipeMenu(navController: NavHostController, currentLanguage: MutableState<String>) {
    val isEs = currentLanguage.value == "es"
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
            .padding(24.dp)
    ) {
        Text(
            if (isEs) "Menú de Recetas" else "Recipe Menu",
            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black),
            color = OnBackground
        )
        Text(
            if (isEs) "Explora nuestro catálogo gourmet" else "Explore our gourmet catalog",
            style = MaterialTheme.typography.bodyLarge,
            color = TextMuted
        )

        Spacer(Modifier.height(40.dp))

        MenuActionCard(
            title = if (isEs) "VER TODO EL CATÁLOGO" else "VIEW ALL CATALOG",
            subtitle = if (isEs) "Explora nuestras 75+ recetas" else "Explore our 75+ recipes",
            icon = Icons.Rounded.MenuBook,
            onClick = { navController.navigate("recetas_lista") }
        )

        Spacer(Modifier.height(16.dp))

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .clickable { navController.navigate("recetas_favoritos") },
            color = Primary.copy(alpha = 0.1f),
            border = BorderStroke(1.dp, Primary.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier.padding(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Favorite, contentDescription = null, tint = Primary)
                Spacer(Modifier.width(12.dp))
                Text(if (isEs) "MIS FAVORITOS" else "MY FAVORITES", color = Primary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun MenuActionCard(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable { onClick() },
        color = Surface,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(16.dp),
                color = Primary.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = Primary)
                }
            }
            Spacer(Modifier.width(20.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold), color = OnSurface)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenRecipes(
    navController: NavHostController, 
    servicio: RecipeApiService, 
    favoritos: MutableList<Int>, 
    currentLanguage: MutableState<String>,
    preloadedRecipes: List<RecipeModel> = emptyList()
) {
    val isEs = currentLanguage.value == "es"
    var allRecipes by remember { mutableStateOf(preloadedRecipes) }
    var isLoading by remember { mutableStateOf(preloadedRecipes.isEmpty()) }
    var isRefreshing by remember { mutableStateOf(false) }
    
    // Cargar preferencia guardada (por defecto Lista Compacta)
    var isListView by remember { mutableStateOf(com.example.uami.utils.LanguageManager.isListView()) }
    
    // Estado unificado de filtros
    var filterState by remember { mutableStateOf(FilterState()) }

    // Sincronizar con datos precargados
    LaunchedEffect(preloadedRecipes) {
        if (preloadedRecipes.isNotEmpty()) {
            allRecipes = preloadedRecipes
            isLoading = false
        }
    }

    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            // Simular refresco para la UI, pero usando datos locales
            kotlinx.coroutines.delay(500)
            isRefreshing = false
        }
    }

    // Aplicar lógica centralizada
    val filteredAndSortedRecipes = remember(allRecipes, filterState) {
        FilterLogic.applyFilters(allRecipes, filterState)
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

                    // BOTÓN PARA CAMBIAR VISTA
                    IconButton(onClick = { 
                        isListView = !isListView 
                        com.example.uami.utils.LanguageManager.setListView(isListView) // Guardar preferencia
                    }) {
                        Icon(
                            imageVector = if (isListView) Icons.Rounded.GridView else Icons.Rounded.ViewList,
                            contentDescription = "Toggle View",
                            tint = Primary
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = filterState.searchQuery,
                    onValueChange = { filterState = filterState.copy(searchQuery = it) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    placeholder = { Text(if(isEs) "Buscar receta..." else "Search recipe...", color = TextMuted) },
                    leadingIcon = { Icon(Icons.Rounded.Search, null, tint = Primary) },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (filterState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { filterState = filterState.copy(searchQuery = "") }) {
                                    Icon(Icons.Rounded.Close, null, tint = TextMuted)
                                }
                            }
                            
                            var showFilterSheet by remember { mutableStateOf(false) }
                            val hasActiveFilters = filterState.selectedCuisine != "All" || 
                                                 filterState.selectedDifficulty != "All" || 
                                                 filterState.selectedMealType != "All" ||
                                                 filterState.sortOrder != "Default"
                            
                            IconButton(onClick = { showFilterSheet = true }) {
                                BadgedBox(
                                    badge = { if(hasActiveFilters) Badge(containerColor = Primary) }
                                ) {
                                    Icon(
                                        Icons.Rounded.Tune, 
                                        contentDescription = "Filters", 
                                        tint = if(hasActiveFilters) Primary else TextMuted
                                    )
                                }
                            }

                            if (showFilterSheet) {
                                FilterBottomSheet(
                                    currentLanguage = currentLanguage,
                                    filterState = filterState,
                                    onFilterChange = { filterState = it },
                                    availableCuisines = remember(allRecipes) {
                                        allRecipes.mapNotNull { it.cuisineEn }.distinct().sorted()
                                    },
                                    availableMealTypes = remember(allRecipes) {
                                        allRecipes.mapNotNull { it.mealTypeEn }.distinct().sorted()
                                    },
                                    onDismiss = { showFilterSheet = false }
                                )
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
            }
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
            } else if (filteredAndSortedRecipes.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(if(isEs) "No se encontraron recetas" else "No recipes found", color = TextMuted)
                }
            } else {
                if (isListView) {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredAndSortedRecipes, key = { it.id ?: 0 }) { recipe ->
                            RecipeRowCompact(
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
                        item { Spacer(Modifier.height(100.dp)) }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredAndSortedRecipes, key = { it.id ?: 0 }) { recipe ->
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
                            Spacer(Modifier.height(100.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecipeRowCompact(recipe: RecipeModel, isFav: Boolean, onFavToggle: () -> Unit, currentLanguage: MutableState<String>, onClick: () -> Unit) {
    val lang = currentLanguage.value
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() },
        color = Surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                AsyncImage(
                    model = recipe.image,
                    contentDescription = null,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
                Surface(
                    modifier = Modifier.align(Alignment.BottomStart).padding(4.dp),
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(Modifier.padding(horizontal = 4.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Star, null, tint = Primary, modifier = Modifier.size(10.dp))
                        Spacer(Modifier.width(2.dp))
                        Text("${recipe.rating}", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    translateText(recipe.mealType, lang).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Secondary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    translateText(recipe.name, lang),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = OnSurface
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        translateText(recipe.cuisine, lang),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.size(3.dp).background(TextMuted, CircleShape))
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Rounded.Timer, null, tint = TextMuted, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "${(recipe.prepTimeMinutes ?: 0) + (recipe.cookTimeMinutes ?: 0)} min",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            }

            IconButton(onClick = onFavToggle, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    tint = if (isFav) Color.Red else TextMuted.copy(alpha = 0.5f),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
fun RecipeCardPremium(recipe: RecipeModel, isFav: Boolean, onFavToggle: () -> Unit, currentLanguage: MutableState<String>, onClick: () -> Unit) {
    val lang = currentLanguage.value
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable { onClick() },
        color = Surface,
        tonalElevation = 2.dp
    ) {
        Column {
            Box {
                AsyncImage(
                    model = recipe.image,
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(140.dp),
                    contentScale = ContentScale.Crop
                )
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                    color = Background.copy(alpha = 0.6f),
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
                    modifier = Modifier.align(Alignment.BottomStart).padding(8.dp),
                    color = Background.copy(alpha = 0.8f),
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        translateText(recipe.cuisine, lang).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = Primary,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        translateText(recipe.mealType, lang).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = Secondary,
                        fontWeight = FontWeight.Bold
                    )
                }
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
fun ScreenRecipeDetail(navController: NavHostController, servicio: RecipeApiService, id: Int, favoritos: MutableList<Int>, currentLanguage: MutableState<String>, preloadedRecipes: List<RecipeModel>) {
    val isEs = currentLanguage.value == "es"
    var recipe by remember { mutableStateOf<RecipeModel?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(id, currentLanguage.value, preloadedRecipes) {
        isLoading = true
        try { 
            val cachedRecipe = preloadedRecipes.find { it.id == id }
            if (cachedRecipe != null) {
                recipe = cachedRecipe
            }
        } catch (e: Exception) { e.printStackTrace() }
        finally { isLoading = false }
    }

    if (isLoading) {
        Box(Modifier.fillMaxSize().background(Background), contentAlignment = Alignment.Center) { 
            CircularProgressIndicator(color = Primary) 
        }
    } else if (recipe != null) {
        val isFav = favoritos.contains(recipe?.id)
        val scrollState = rememberScrollState()

        Column(modifier = Modifier.fillMaxSize().background(Background).verticalScroll(scrollState)) {
            Box(modifier = Modifier.fillMaxWidth().height(350.dp)) {
                AsyncImage(
                    model = recipe?.image,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(modifier = Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color.Transparent, Background))))
                
                Row(
                    modifier = Modifier.statusBarsPadding().fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(color = Background.copy(alpha = 0.5f), shape = CircleShape) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = Color.White)
                        }
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
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(color = Primary, shape = RoundedCornerShape(12.dp)) {
                            Text(
                                translateText(recipe?.cuisine, currentLanguage.value).uppercase(),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = OnPrimary)
                            )
                        }
                        Surface(color = Secondary, shape = RoundedCornerShape(12.dp)) {
                            Text(
                                translateText(recipe?.mealType, currentLanguage.value).uppercase(),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = OnPrimary)
                            )
                        }
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
                    DetailBadge(Icons.Rounded.Timer, if(isEs) "Tiempo" else "Time", "${(recipe?.prepTimeMinutes ?: 0) + (recipe?.cookTimeMinutes ?: 0)}m")
                    DetailBadge(Icons.Rounded.SignalCellularAlt, if(isEs) "Nivel" else "Level", translateText(recipe?.difficulty, currentLanguage.value))
                    DetailBadge(Icons.Rounded.LocalFireDepartment, if(isEs) "Calorías" else "Calories", "450 kcal")
                }

                Spacer(Modifier.height(40.dp))
                Text(if(isEs) "Ingredientes" else "Ingredients", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = OnBackground)
                Spacer(Modifier.height(16.dp))
                recipe?.ingredients?.forEach { ing -> IngredientRow(ing) }

                Spacer(Modifier.height(40.dp))
                Text(if(isEs) "Instrucciones" else "Instructions", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = OnBackground)
                Spacer(Modifier.height(16.dp))
                recipe?.instructions?.forEachIndexed { idx, step -> StepRow(idx + 1, step) }
                
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
    id: Int,
    tts: TextToSpeech?,
    onSpeechFinished: MutableState<(() -> Unit)?>,
    currentLanguage: MutableState<String>,
    preloadedRecipes: List<RecipeModel>
) {
    var recipe by remember { mutableStateOf<RecipeModel?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(id, currentLanguage.value, preloadedRecipes) {
        isLoading = true
        try { 
            val cachedRecipe = preloadedRecipes.find { it.id == id }
            if (cachedRecipe != null) {
                recipe = cachedRecipe
            }
        } catch (e: Exception) { e.printStackTrace() }
        finally { isLoading = false }
    }

    if (isLoading) {
        Box(Modifier.fillMaxSize().background(Background), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Primary) }
    } else if (recipe != null) {
        val isEs = currentLanguage.value == "es"
        val steps = recipe?.instructions ?: emptyList()
        val pagerState = rememberPagerState(pageCount = { steps.size + 1 })
        val coroutineScope = rememberCoroutineScope()

        Column(modifier = Modifier.fillMaxSize().background(Background).statusBarsPadding()) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = OnBackground)
                }
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(if(isEs) "Modo Cocina" else "Cooking Mode", style = MaterialTheme.typography.labelLarge, color = Primary, fontWeight = FontWeight.Bold)
                    Text(translateText(recipe?.name, currentLanguage.value), style = MaterialTheme.typography.titleMedium, color = OnBackground, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            LinearProgressIndicator(
                progress = { pagerState.currentPage.toFloat() / steps.size.toFloat() },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = Primary,
                trackColor = Surface
            )

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(32.dp),
                pageSpacing = 16.dp
            ) { page ->
                if (page == 0) {
                    IntroStep(recipe!!, currentLanguage.value)
                } else {
                    CookingStep(page, steps[page - 1], isEs)
                }
            }
            
            // Voice Control Bar
            Surface(modifier = Modifier.fillMaxWidth(), color = Surface, tonalElevation = 8.dp, shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)) {
                Row(modifier = Modifier.padding(24.dp).navigationBarsPadding(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { 
                        coroutineScope.launch { if(pagerState.currentPage > 0) pagerState.animateScrollToPage(pagerState.currentPage - 1) } 
                    }, enabled = pagerState.currentPage > 0) {
                        Icon(Icons.Rounded.ChevronLeft, null, tint = if(pagerState.currentPage > 0) Primary else TextMuted, modifier = Modifier.size(32.dp))
                    }

                    FloatingActionButton(
                        onClick = { 
                            val textToRead = if(pagerState.currentPage == 0) {
                                if(isEs) "Vamos a preparar ${translateText(recipe?.name, "es")}. ¿Lista para empezar?" 
                                else "Let's prepare ${recipe?.name}. Ready to start?"
                            } else {
                                steps[pagerState.currentPage - 1]
                            }
                            tts?.speak(textToRead, TextToSpeech.QUEUE_FLUSH, null, "step_${pagerState.currentPage}")
                        },
                        containerColor = Primary,
                        contentColor = OnPrimary,
                        shape = CircleShape
                    ) {
                        Icon(Icons.Rounded.VolumeUp, null)
                    }

                    IconButton(onClick = { 
                        coroutineScope.launch { if(pagerState.currentPage < steps.size) pagerState.animateScrollToPage(pagerState.currentPage + 1) } 
                    }, enabled = pagerState.currentPage < steps.size) {
                        Icon(Icons.Rounded.ChevronRight, null, tint = if(pagerState.currentPage < steps.size) Primary else TextMuted, modifier = Modifier.size(32.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun IntroStep(recipe: RecipeModel, lang: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize()) {
        Surface(modifier = Modifier.size(120.dp), shape = CircleShape, color = Primary.copy(alpha = 0.1f)) {
            Box(contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Restaurant, null, tint = Primary, modifier = Modifier.size(48.dp)) }
        }
        Spacer(Modifier.height(32.dp))
        Text(if(lang == "es") "¡Prepárate!" else "Get Ready!", style = MaterialTheme.typography.headlineMedium, color = OnBackground, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Text(
            if(lang == "es") "Asegúrate de tener todos los ingredientes a la mano. Desliza para ver el primer paso." 
            else "Make sure you have all the ingredients ready. Swipe to see the first step.",
            textAlign = TextAlign.Center, color = TextMuted
        )
    }
}

@Composable
fun CookingStep(number: Int, text: String, isEs: Boolean) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
        Text(text = if(isEs) "PASO $number" else "STEP $number", style = MaterialTheme.typography.labelLarge, color = Primary, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(24.dp))
        Text(text = text, style = MaterialTheme.typography.headlineSmall.copy(lineHeight = 36.sp, fontWeight = FontWeight.Medium), color = OnBackground)
    }
}

@Composable
fun ScreenFavorites(navController: NavHostController, servicio: RecipeApiService, favoritos: List<Int>, currentLanguage: MutableState<String>, preloadedRecipes: List<RecipeModel>) {
    val isEs = currentLanguage.value == "es"
    var listaFavoritos by remember { mutableStateOf<List<RecipeModel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(favoritos.size, currentLanguage.value, preloadedRecipes) {
        isLoading = true
        val listadoRaw = mutableListOf<RecipeModel>()
        favoritos.forEach { id ->
            val cachedRecipe = preloadedRecipes.find { it.id == id }
            if (cachedRecipe != null) listadoRaw.add(cachedRecipe)
        }
        listaFavoritos = listadoRaw
        isLoading = false
    }

    Scaffold(
        topBar = {
            Row(modifier = Modifier.statusBarsPadding().fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = OnBackground) }
                Text(if(isEs) "Mis Favoritos" else "My Favorites", style = MaterialTheme.typography.headlineSmall, color = OnBackground, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = Background
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Primary) }
        } else if (listaFavoritos.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(if(isEs) "No tienes recetas favoritas aún" else "You don't have favorite recipes yet", color = TextMuted)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(padding).fillMaxSize()
            ) {
                items(listaFavoritos) { recipe ->
                    RecipeCardPremium(recipe, true, {}, currentLanguage, { navController.navigate("recipeDetail/${recipe.id}") })
                }
            }
        }
    }
}

@Composable
fun DetailBadge(icon: ImageVector, label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = Primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
        Text(value, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = OnBackground)
    }
}

@Composable
fun IngredientRow(text: String) {
    Row(modifier = Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).background(Primary, CircleShape))
        Spacer(Modifier.width(16.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = OnSurface)
    }
}

@Composable
fun StepRow(index: Int, text: String) {
    Row(modifier = Modifier.padding(vertical = 12.dp)) {
        Surface(modifier = Modifier.size(32.dp), shape = CircleShape, color = Surface, border = BorderStroke(1.dp, Primary.copy(alpha = 0.3f))) {
            Box(contentAlignment = Alignment.Center) { Text("$index", style = MaterialTheme.typography.labelMedium, color = Primary) }
        }
        Spacer(Modifier.width(16.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 26.sp), color = OnSurface)
    }
}

@Composable
fun PaginationControls(currentPage: Int, totalPages: Int, currentLanguage: MutableState<String>, onPageChange: (Int) -> Unit) {
    val isEs = currentLanguage.value == "es"
    Surface(modifier = Modifier.fillMaxWidth(), color = Background, tonalElevation = 4.dp) {
        Row(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp).navigationBarsPadding(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(if (isEs) "Página $currentPage de $totalPages" else "Page $currentPage of $totalPages", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalIconButton(onClick = { onPageChange(currentPage - 1) }, enabled = currentPage > 1, colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = Surface)) {
                    Icon(Icons.Rounded.ChevronLeft, null, tint = if(currentPage > 1) Primary else TextMuted)
                }
                FilledTonalIconButton(onClick = { onPageChange(currentPage + 1) }, enabled = currentPage < totalPages, colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = Surface)) {
                    Icon(Icons.Rounded.ChevronRight, null, tint = if(currentPage < totalPages) Primary else TextMuted)
                }
            }
        }
    }
}
