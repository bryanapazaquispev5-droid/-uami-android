package com.example.uami.recipes.ui

import android.speech.tts.TextToSpeech
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
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
import com.example.uami.recipes.models.RecipeModel
import com.example.uami.recipes.remote.RecipeApiService
import com.example.uami.ui.theme.*
import com.example.uami.utils.*
import com.example.uami.ui.filters.*
import kotlinx.coroutines.launch
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.graphics.graphicsLayer

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
            subtitle = if (isEs) "Explora nuestras recetas" else "Explore our recipes",
            icon = Icons.AutoMirrored.Rounded.MenuBook,
            onClick = { navController.navigate("recetas_lista") }
        )

        Spacer(Modifier.height(16.dp))

        MenuActionCard(
            title = if (isEs) "MIS FAVORITOS" else "MY FAVORITES",
            subtitle = if (isEs) "Tus recetas guardadas para cocinar" else "Your saved recipes to cook",
            icon = Icons.Rounded.Favorite,
            onClick = { navController.navigate("recetas_favoritos") }
        )
    }
}

@Composable
fun MenuActionCard(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "scale"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clip(RoundedCornerShape(24.dp))
            .shimmerGlow(durationMillis = 2400 + (title.hashCode() % 400)) // Elegant diagonal metal sheen
            .clickable(interactionSource = interactionSource, indication = androidx.compose.foundation.LocalIndication.current) { onClick() },
        color = Surface,
        tonalElevation = 2.dp,
        border = BorderStroke(1.dp, Surface.copy(alpha = 0.8f))
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
                    Icon(
                        icon, 
                        contentDescription = null, 
                        tint = Primary, 
                        modifier = Modifier
                            .size(24.dp)
                            .pulseAnimation(durationMillis = 1800 + (title.hashCode() % 300))
                    )
                }
            }
            Spacer(Modifier.width(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title, 
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold), 
                    color = OnSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = subtitle, 
                    style = MaterialTheme.typography.bodySmall, 
                    color = TextMuted
                )
            }
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenRecipes(
    navController: NavHostController, 
    @Suppress("UNUSED_PARAMETER") servicio: RecipeApiService, 
    favoritos: MutableList<Int>, 
    currentLanguage: MutableState<String>,
    preloadedRecipes: List<RecipeModel> = emptyList(),
    initialSearchQuery: String = "",
    initialCuisine: String = "All",
    initialMealType: String = "All",
    initialDifficulty: String = "All"
) {
    val isEs = currentLanguage.value == "es"
    var allRecipes by remember { mutableStateOf(preloadedRecipes) }
    var isLoading by remember { mutableStateOf(preloadedRecipes.isEmpty()) }
    var isRefreshing by remember { mutableStateOf(false) }
    
    // Cargar preferencia guardada (por defecto Lista Compacta)
    var isListView by remember { mutableStateOf(LanguageManager.isListView()) }
    
    // Estado unificado de filtros
    var filterState by remember(initialSearchQuery, initialCuisine, initialMealType, initialDifficulty) {
        mutableStateOf(
            FilterState(
                searchQuery = initialSearchQuery,
                selectedCuisine = initialCuisine,
                selectedDifficulty = initialDifficulty,
                selectedMealType = initialMealType
            )
        )
    }

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
    
    // Rastrear el índice máximo de receta que ha sido animado en pantalla
    var maxAnimatedIndex by remember(filteredAndSortedRecipes) { mutableIntStateOf(-1) }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(Background).padding(top = 24.dp, start = 8.dp, end = 24.dp, bottom = 12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BouncyPressEffect(squishFactor = 0.72f) { modifier, interactionSource ->
                            IconButton(
                                onClick = { navController.popBackStack() },
                                interactionSource = interactionSource,
                                modifier = modifier
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack, 
                                    contentDescription = null, 
                                    tint = OnBackground,
                                    modifier = Modifier.horizontalSlideAnimation(durationMillis = 1400, dx = 6f)
                                )
                            }
                        }
                        Text(if (isEs) "Recetas" else "Recipes", style = MaterialTheme.typography.headlineMedium, color = OnBackground)
                    }

                    // BOTÓN PARA CAMBIAR VISTA
                    BouncyPressEffect { modifier, interactionSource ->
                        IconButton(
                            onClick = { 
                                isListView = !isListView 
                                LanguageManager.setListView(isListView) // Guardar preferencia
                            },
                            interactionSource = interactionSource,
                            modifier = modifier
                        ) {
                            Icon(
                                imageVector = if (isListView) Icons.Rounded.GridView else Icons.AutoMirrored.Rounded.ViewList,
                                contentDescription = "Toggle View",
                                tint = Primary
                            )
                        }
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
                                BouncyPressEffect { modifier, interactionSource ->
                                    IconButton(
                                        onClick = { filterState = filterState.copy(searchQuery = "") },
                                        interactionSource = interactionSource,
                                        modifier = modifier
                                    ) {
                                        Icon(Icons.Rounded.Close, null, tint = TextMuted)
                                    }
                                }
                            }
                            
                            var showFilterSheet by remember { mutableStateOf(false) }
                            val hasActiveFilters = filterState.selectedCuisine != "All" || 
                                                 filterState.selectedDifficulty != "All" || 
                                                 filterState.selectedMealType != "All" ||
                                                 filterState.sortOrder != "Default"
                            
                            BouncyPressEffect(squishFactor = 0.70f) { modifier, interactionSource ->
                                IconButton(
                                    onClick = { showFilterSheet = true },
                                    interactionSource = interactionSource,
                                    modifier = modifier
                                ) {
                                    BadgedBox(
                                        badge = { if(hasActiveFilters) Badge(containerColor = Primary) }
                                    ) {
                                        Icon(
                                            Icons.Rounded.Tune, 
                                            contentDescription = "Filters", 
                                            tint = if(hasActiveFilters) Primary else TextMuted,
                                            modifier = Modifier.shakeWobbleAnimation()
                                        )
                                    }
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
                                        allRecipes.flatMap { it.mealTypeEn?.split(",") ?: emptyList() }
                                            .map { it.trim() }
                                            .filter { it.isNotEmpty() }
                                            .distinct()
                                            .sorted()
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
                        itemsIndexed(filteredAndSortedRecipes, key = { _, recipe -> recipe.id ?: 0 }) { index, recipe ->
                            val shouldAnimate = index > maxAnimatedIndex
                            AnimateEntryItem(
                                index = index,
                                shouldAnimate = shouldAnimate,
                                onAppear = {
                                    if (index > maxAnimatedIndex) {
                                        maxAnimatedIndex = index
                                    }
                                }
                            ) {
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
                        itemsIndexed(filteredAndSortedRecipes, key = { _, recipe -> recipe.id ?: 0 }) { index, recipe ->
                            val shouldAnimate = index > maxAnimatedIndex
                            AnimateEntryItem(
                                index = index,
                                shouldAnimate = shouldAnimate,
                                onAppear = {
                                    if (index > maxAnimatedIndex) {
                                        maxAnimatedIndex = index
                                    }
                                }
                            ) {
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
    BouncyPressEffect { modifier, interactionSource ->
        val isPressed by interactionSource.collectIsPressedAsState()
        val imgScale by animateFloatAsState(
            targetValue = if (isPressed) 1.16f else 1.0f,
            animationSpec = spring(stiffness = Spring.StiffnessLow),
            label = "imgScale"
        )
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .clickable(interactionSource = interactionSource, indication = androidx.compose.foundation.LocalIndication.current) { 
                    onClick() 
                },
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
                            .clip(RoundedCornerShape(16.dp))
                            .graphicsLayer(scaleX = imgScale, scaleY = imgScale),
                        contentScale = ContentScale.Crop
                    )
                Surface(
                    modifier = Modifier.align(Alignment.BottomStart).padding(4.dp),
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(Modifier.padding(horizontal = 4.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Star, null, tint = Primary, modifier = Modifier.size(10.dp).pulseAnimation(durationMillis = 1200))
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
                    Icon(Icons.Rounded.Timer, null, tint = TextMuted, modifier = Modifier.size(12.dp).pulseAnimation(durationMillis = 1500))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "${(recipe.prepTimeMinutes ?: 0) + (recipe.cookTimeMinutes ?: 0)} min",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            }

            HeartBurstButton(
                isFav = isFav,
                onFavToggle = onFavToggle,
                modifier = Modifier.size(40.dp),
                iconSize = 22.dp,
                activeColor = Color.Red,
                inactiveColor = TextMuted.copy(alpha = 0.5f)
            )
        }
    }
}
}

@Composable
fun RecipeCardPremium(recipe: RecipeModel, isFav: Boolean, onFavToggle: () -> Unit, currentLanguage: MutableState<String>, onClick: () -> Unit) {
    val lang = currentLanguage.value
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "scale"
    )
    val imgScale by animateFloatAsState(
        targetValue = if (isPressed) 1.16f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "imgScale"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clip(RoundedCornerShape(24.dp))
            .clickable(interactionSource = interactionSource, indication = androidx.compose.foundation.LocalIndication.current) { onClick() },
        color = Surface,
        tonalElevation = 2.dp
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(130.dp)) {
                AsyncImage(
                    model = recipe.image,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(scaleX = imgScale, scaleY = imgScale),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.15f), Color.Black.copy(alpha = 0.45f))
                            )
                        )
                )
                
                // Top-Left: Cuisine Badge Pill
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .bobbingAnimation(durationMillis = 1800, dy = 4f),
                    color = Primary,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = translateText(recipe.cuisine, lang).uppercase(),
                        color = OnPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                // Top-Right: Favorite Button (Spins & Pulses)
                HeartBurstButton(
                    isFav = isFav,
                    onFavToggle = onFavToggle,
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(32.dp),
                    iconSize = 18.dp,
                    activeColor = Color.Red,
                    inactiveColor = Color.White,
                    backgroundColor = Background.copy(alpha = 0.6f)
                )

                // Bottom-Left: Rating Badge
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                        .bobbingAnimation(durationMillis = 1600, dy = 3f),
                    color = Background.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Star, null, tint = Primary, modifier = Modifier.size(12.dp).pulseAnimation(durationMillis = 1200))
                        Spacer(Modifier.width(4.dp))
                        Text("${recipe.rating}", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Column(Modifier.padding(14.dp)) {
                // Meal Type Badge
                Surface(
                    color = Secondary.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .padding(bottom = 6.dp)
                        .bobbingAnimation(durationMillis = 1500, dy = 2f)
                ) {
                    Text(
                        text = translateText(recipe.mealType, lang).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = Secondary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 9.sp
                    )
                }

                // Title (Exactly 2 lines for uniform grid height)
                Text(
                    text = translateText(recipe.name, lang),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.ExtraBold, fontSize = 15.sp),
                    maxLines = 2,
                    minLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = OnSurface
                )
                
                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Time
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Timer, null, tint = TextMuted, modifier = Modifier.size(14.dp).pulseAnimation(durationMillis = 1500))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "${(recipe.prepTimeMinutes ?: 0) + (recipe.cookTimeMinutes ?: 0)} min", 
                            style = MaterialTheme.typography.bodySmall, 
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                    // Difficulty
                    Text(
                        text = translateText(recipe.difficulty, lang),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenRecipeDetail(navController: NavHostController, @Suppress("UNUSED_PARAMETER") servicio: RecipeApiService, id: Int, favoritos: MutableList<Int>, currentLanguage: MutableState<String>, preloadedRecipes: List<RecipeModel>) {
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
                    modifier = Modifier
                        .statusBarsPadding()
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(color = Color.Black.copy(alpha = 0.35f), shape = CircleShape) {
                        BouncyPressEffect(squishFactor = 0.72f) { modifier, interactionSource ->
                            IconButton(
                                onClick = { navController.popBackStack() },
                                interactionSource = interactionSource,
                                modifier = modifier
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack, 
                                    contentDescription = null, 
                                    tint = Color.White,
                                    modifier = Modifier.horizontalSlideAnimation(durationMillis = 1400, dx = 6f)
                                )
                            }
                        }
                    }
                    
                    HeartBurstButton(
                        isFav = isFav,
                        onFavToggle = {
                            if (isFav) favoritos.remove(recipe?.id)
                            else favoritos.add(recipe?.id ?: 0)
                        },
                        modifier = Modifier.size(40.dp),
                        iconSize = 22.dp,
                        activeColor = Color.Red,
                        inactiveColor = Color.White,
                        backgroundColor = Color.Black.copy(alpha = 0.35f)
                    )
                }
            }
            
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-30).dp),
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                color = Background
            ) {
                Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp)) {
                    Text(
                        text = translateText(recipe?.name, currentLanguage.value),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 28.sp,
                            lineHeight = 36.sp
                        ),
                        color = OnBackground
                    )

                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.bobbingAnimation(durationMillis = 1600, dy = 4f),
                            color = Primary.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Primary.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = translateText(recipe?.cuisine, currentLanguage.value).uppercase(),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Primary)
                            )
                        }
                        Surface(
                            modifier = Modifier.bobbingAnimation(durationMillis = 2000, dy = 4f),
                            color = Secondary.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Secondary.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = translateText(recipe?.mealType, currentLanguage.value).uppercase(),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Secondary)
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))
                    
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Surface,
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, Surface.copy(alpha = 0.8f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DetailBadge(
                                icon = Icons.Rounded.Timer, 
                                label = if(isEs) "Tiempo" else "Time", 
                                value = "${(recipe?.prepTimeMinutes ?: 0) + (recipe?.cookTimeMinutes ?: 0)}m"
                            )
                            Box(modifier = Modifier.width(1.dp).height(28.dp).background(Primary.copy(alpha = 0.2f)))
                            DetailBadge(
                                icon = Icons.Rounded.SignalCellularAlt, 
                                label = if(isEs) "Dificultad" else "Difficulty", 
                                value = translateText(recipe?.difficulty, currentLanguage.value)
                            )
                            Box(modifier = Modifier.width(1.dp).height(28.dp).background(Primary.copy(alpha = 0.2f)))
                            DetailBadge(
                                icon = Icons.Rounded.LocalFireDepartment, 
                                label = if(isEs) "Calorías" else "Calories", 
                                value = "450 kcal"
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    BouncyPressEffect(squishFactor = 0.80f) { modifier, interactionSource ->
                        val isPressed by interactionSource.collectIsPressedAsState()
                        val iconRotation by animateFloatAsState(
                            targetValue = if (isPressed) 360f else 0f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                            label = "iconRotation"
                        )
                        Button(
                            onClick = { navController.navigate("cookingMode/${recipe?.id}") },
                            colors = ButtonDefaults.buttonColors(containerColor = Secondary),
                            shape = RoundedCornerShape(20.dp),
                            interactionSource = interactionSource,
                            modifier = modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .shimmerGlow(durationMillis = 2000)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.OutdoorGrill, 
                                    contentDescription = null, 
                                    modifier = Modifier
                                        .size(22.dp)
                                        .graphicsLayer(rotationZ = iconRotation), 
                                    tint = OnPrimary
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = if (isEs) "INICIAR MODO COCINA" else "START COOKING MODE", 
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = OnPrimary
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(36.dp))
                    Text(
                        text = if(isEs) "Ingredientes" else "Ingredients", 
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold), 
                        color = OnBackground
                    )
                    Spacer(Modifier.height(16.dp))
                    recipe?.ingredients?.forEach { ing -> IngredientRow(ing) }

                    Spacer(Modifier.height(36.dp))
                    Text(
                        text = if(isEs) "Instrucciones" else "Instructions", 
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold), 
                        color = OnBackground
                    )
                    Spacer(Modifier.height(16.dp))
                    recipe?.instructions?.forEachIndexed { idx, step -> StepRow(idx + 1, step) }
                    
                    Spacer(Modifier.height(80.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ScreenCookingMode(
    navController: NavHostController, 
    @Suppress("UNUSED_PARAMETER") servicio: RecipeApiService, 
    id: Int,
    tts: TextToSpeech?,
    @Suppress("UNUSED_PARAMETER") onSpeechFinished: MutableState<(() -> Unit)?>,
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
                BouncyPressEffect(squishFactor = 0.72f) { modifier, interactionSource ->
                    IconButton(
                        onClick = { navController.popBackStack() },
                        interactionSource = interactionSource,
                        modifier = modifier
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack, 
                            contentDescription = null, 
                            tint = OnBackground,
                            modifier = Modifier.horizontalSlideAnimation(durationMillis = 1400, dx = 6f)
                        )
                    }
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
                    BouncyPressEffect { modifier, interactionSource ->
                        IconButton(
                            onClick = { 
                                coroutineScope.launch { if(pagerState.currentPage > 0) pagerState.animateScrollToPage(pagerState.currentPage - 1) } 
                            }, 
                            enabled = pagerState.currentPage > 0,
                            interactionSource = interactionSource,
                            modifier = modifier
                        ) {
                            Icon(Icons.Rounded.ChevronLeft, null, tint = if(pagerState.currentPage > 0) Primary else TextMuted, modifier = Modifier.size(32.dp))
                        }
                    }

                    BouncyPressEffect(squishFactor = 0.68f) { modifier, interactionSource ->
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
                            shape = CircleShape,
                            interactionSource = interactionSource,
                            modifier = modifier
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.VolumeUp, 
                                contentDescription = null,
                                modifier = Modifier.shakeWobbleAnimation(animDuration = 1300, maxRotation = 16f)
                            )
                        }
                    }

                    BouncyPressEffect { modifier, interactionSource ->
                        IconButton(
                            onClick = { 
                                coroutineScope.launch { if(pagerState.currentPage < steps.size) pagerState.animateScrollToPage(pagerState.currentPage + 1) } 
                            }, 
                            enabled = pagerState.currentPage < steps.size,
                            interactionSource = interactionSource,
                            modifier = modifier
                        ) {
                            Icon(Icons.Rounded.ChevronRight, null, tint = if(pagerState.currentPage < steps.size) Primary else TextMuted, modifier = Modifier.size(32.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun IntroStep(@Suppress("UNUSED_PARAMETER") recipe: RecipeModel, lang: String) {
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
fun ScreenFavorites(navController: NavHostController, @Suppress("UNUSED_PARAMETER") servicio: RecipeApiService, favoritos: List<Int>, currentLanguage: MutableState<String>, preloadedRecipes: List<RecipeModel>) {
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
                BouncyPressEffect { modifier, interactionSource ->
                    IconButton(
                        onClick = { navController.popBackStack() },
                        interactionSource = interactionSource,
                        modifier = modifier
                    ) { 
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = OnBackground) 
                    }
                }
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
    val duration = when(icon) {
        Icons.Rounded.LocalFireDepartment -> 1100
        Icons.Rounded.Timer -> 1600
        else -> 2200
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            icon, 
            null, 
            tint = Primary, 
            modifier = Modifier
                .size(24.dp)
                .pulseAnimation(durationMillis = duration)
        )
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
        Spacer(Modifier.height(2.dp))
        Text(value, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = OnBackground)
    }
}

@Composable
fun IngredientRow(text: String) {
    var isChecked by remember { mutableStateOf(false) }
    val checkScale by animateFloatAsState(
        targetValue = if (isChecked) 1.25f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMedium),
        label = "checkScale"
    )
    val animatedColor by animateColorAsState(
        targetValue = if (isChecked) Primary else Primary.copy(alpha = 0.15f),
        label = "animatedColor"
    )
    val checkIconColor by animateColorAsState(
        targetValue = if (isChecked) OnPrimary else Primary,
        label = "checkIconColor"
    )
    val textAlpha by animateFloatAsState(
        targetValue = if (isChecked) 0.5f else 1.0f,
        label = "textAlpha"
    )

    BouncyPressEffect { modifier, interactionSource ->
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clickable(
                    interactionSource = interactionSource,
                    indication = androidx.compose.foundation.LocalIndication.current
                ) { isChecked = !isChecked },
            color = Surface.copy(alpha = 0.5f),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Surface.copy(alpha = 0.8f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(24.dp).graphicsLayer(scaleX = checkScale, scaleY = checkScale),
                    shape = CircleShape,
                    color = animatedColor
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = checkIconColor,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Spacer(Modifier.width(16.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        textDecoration = if (isChecked) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                    ),
                    color = OnSurface.copy(alpha = textAlpha)
                )
            }
        }
    }
}

@Composable
fun StepRow(index: Int, text: String) {
    var isDone by remember { mutableStateOf(false) }
    val stepScale by animateFloatAsState(
        targetValue = if (isDone) 1.20f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMedium),
        label = "stepScale"
    )
    val stepColor by animateColorAsState(
        targetValue = if (isDone) Secondary else Primary,
        label = "stepColor"
    )
    val textAlpha by animateFloatAsState(
        targetValue = if (isDone) 0.5f else 1.0f,
        label = "textAlpha"
    )

    BouncyPressEffect { modifier, interactionSource ->
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clickable(
                    interactionSource = interactionSource,
                    indication = androidx.compose.foundation.LocalIndication.current
                ) { isDone = !isDone },
            color = Surface,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, if (isDone) Secondary.copy(alpha = 0.3f) else Primary.copy(alpha = 0.05f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Surface(
                    modifier = Modifier.size(36.dp).graphicsLayer(scaleX = stepScale, scaleY = stepScale),
                    shape = CircleShape,
                    color = stepColor,
                    tonalElevation = 2.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isDone) {
                            Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        } else {
                            Text(
                                text = "$index",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = OnPrimary
                            )
                        }
                    }
                }
                Spacer(Modifier.width(16.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 26.sp),
                    color = OnSurface.copy(alpha = textAlpha)
                )
            }
        }
    }
}

@Suppress("UNUSED")
@Composable
fun PaginationControls(currentPage: Int, totalPages: Int, currentLanguage: MutableState<String>, onPageChange: (Int) -> Unit) {
    val isEs = currentLanguage.value == "es"
    Surface(modifier = Modifier.fillMaxWidth(), color = Background, tonalElevation = 4.dp) {
        Row(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp).navigationBarsPadding(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(if (isEs) "Página $currentPage de $totalPages" else "Page $currentPage of $totalPages", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BouncyPressEffect { modifier, interactionSource ->
                    FilledTonalIconButton(
                        onClick = { onPageChange(currentPage - 1) }, 
                        enabled = currentPage > 1, 
                        colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = Surface),
                        interactionSource = interactionSource,
                        modifier = modifier
                    ) {
                        Icon(Icons.Rounded.ChevronLeft, null, tint = if(currentPage > 1) Primary else TextMuted)
                    }
                }
                BouncyPressEffect { modifier, interactionSource ->
                    FilledTonalIconButton(
                        onClick = { onPageChange(currentPage + 1) }, 
                        enabled = currentPage < totalPages, 
                        colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = Surface),
                        interactionSource = interactionSource,
                        modifier = modifier
                    ) {
                        Icon(Icons.Rounded.ChevronRight, null, tint = if(currentPage < totalPages) Primary else TextMuted)
                    }
                }
            }
        }
    }
}

@Composable
fun AnimateEntryItem(
    index: Int,
    shouldAnimate: Boolean,
    onAppear: () -> Unit,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(!shouldAnimate) }
    LaunchedEffect(Unit) {
        onAppear()
        if (shouldAnimate) {
            // Stagger much faster (15ms delay per item, max 120ms to avoid scroll delays)
            kotlinx.coroutines.delay((index % 8) * 15L)
            visible = true
        }
    }

    val animatedAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = "alpha"
    )
    val animatedOffsetY by animateDpAsState(
        targetValue = if (visible) 0.dp else 20.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "offsetY"
    )

    Box(
        modifier = Modifier
            .graphicsLayer(alpha = animatedAlpha)
            .offset(y = animatedOffsetY)
    ) {
        content()
    }
}


