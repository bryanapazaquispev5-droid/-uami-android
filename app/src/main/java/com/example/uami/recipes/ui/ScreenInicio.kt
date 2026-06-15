package com.example.uami.recipes.ui

import android.widget.Toast
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.uami.recipes.models.RecipeModel
import com.example.uami.recipes.viewmodel.ReviewsViewModel
import com.example.uami.recipes.viewmodel.ViewModelFactory
import com.example.uami.ui.theme.*
import com.example.uami.utils.*
import java.util.Calendar

@Composable
fun ScreenInicio(
    navController: NavHostController,
    currentLanguage: MutableState<String>,
    preloadedRecipes: List<RecipeModel>,
    favoritos: MutableList<Int>,
    factory: ViewModelFactory
) {
    val reviewsViewModel: ReviewsViewModel = viewModel(factory = factory)
    val userProfile by reviewsViewModel.userProfile.collectAsState()
    val currentUser by reviewsViewModel.currentUser.collectAsState()
    val isEs = currentLanguage.value == "es"
    val scrollState = rememberScrollState()

    var showRecommendationDialog by remember { mutableStateOf(false) }

    LaunchedEffect(currentUser) {
        if (currentUser == null) {
            if (LanguageManager.isFirstRun()) {
                LanguageManager.setFirstRunCompleted()
                reviewsViewModel.hasShownLoginPrompt = true
                navController.navigate("perfil")
            } else if (!reviewsViewModel.hasShownLoginPrompt) {
                showRecommendationDialog = true
                reviewsViewModel.hasShownLoginPrompt = true
            }
        }
    }

    // 1. Saludo por hora del día
    val greeting = remember {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        if (isEs) {
            when (hour) {
                in 6..11 -> "¡Buenos días, Chef!"
                in 12..17 -> "¡Buenas tardes, Chef!"
                else -> "¡Buenas noches, Chef!"
            }
        } else {
            when (hour) {
                in 6..11 -> "Good morning, Chef!"
                in 12..17 -> "Good afternoon, Chef!"
                else -> "Good evening, Chef!"
            }
        }
    }

    // 2. Selección de receta del día (semi-aleatoria basada en el día actual)
    val recetaDelDia = remember(preloadedRecipes) {
        if (preloadedRecipes.isNotEmpty()) {
            val calendar = Calendar.getInstance()
            val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
            preloadedRecipes[dayOfYear % preloadedRecipes.size]
        } else null
    }

    // 3. Selección de recetas recomendadas (con IA Local cuando esté disponible)
    val context = LocalContext.current
    val nutritionistManager = remember { NutritionistAIManager(context) }
    val isModelReady by nutritionistManager.isModelReady.collectAsState()
    var hasGeneratedAI by remember { mutableStateOf(false) }

    var recetasDestacadas by remember(preloadedRecipes) {
        mutableStateOf(
            if (preloadedRecipes.isNotEmpty()) {
                val favorites = preloadedRecipes.filter { favoritos.contains(it.id) }
                val recommendations = favorites.take(6).toMutableList()
                if (recommendations.size < 6) {
                    val nonFavorites = preloadedRecipes.filter { !favoritos.contains(it.id) }
                    recommendations.addAll(nonFavorites.take(6 - recommendations.size))
                }
                recommendations.ifEmpty { preloadedRecipes.take(6) }
            } else emptyList()
        )
    }

    LaunchedEffect(preloadedRecipes, favoritos, isModelReady) {
        if (preloadedRecipes.isNotEmpty() && isModelReady && !hasGeneratedAI) {
            hasGeneratedAI = true
            nutritionistManager.initializeLLM()
            recetasDestacadas = nutritionistManager.generateAIRecommendations(preloadedRecipes, favoritos, isEs)
        }
    }

    // 4. Conteo de recetas por categorías
    val countBreakfast = remember(preloadedRecipes) {
        preloadedRecipes.count { 
            val mte = it.mealTypeEn?.lowercase() ?: ""
            val mt = it.mealType?.lowercase() ?: ""
            mte.contains("breakfast") || mt.contains("desayuno")
        }
    }
    val countLunch = remember(preloadedRecipes) {
        preloadedRecipes.count { 
            val mte = it.mealTypeEn?.lowercase() ?: ""
            val mt = it.mealType?.lowercase() ?: ""
            mte.contains("lunch") || mt.contains("almuerzo")
        }
    }
    val countDinner = remember(preloadedRecipes) {
        preloadedRecipes.count { 
            val mte = it.mealTypeEn?.lowercase() ?: ""
            val mt = it.mealType?.lowercase() ?: ""
            mte.contains("dinner") || mt.contains("cena")
        }
    }
    val countDessert = remember(preloadedRecipes) {
        preloadedRecipes.count { 
            val mte = it.mealTypeEn?.lowercase() ?: ""
            val mt = it.mealType?.lowercase() ?: ""
            mte.contains("dessert") || mt.contains("postre")
        }
    }
    val countBeverage = remember(preloadedRecipes) {
        preloadedRecipes.count { 
            val mte = it.mealTypeEn?.lowercase() ?: ""
            val mt = it.mealType?.lowercase() ?: ""
            mte.contains("beverage") || mt.contains("bebida")
        }
    }
    val countAppetizer = remember(preloadedRecipes) {
        preloadedRecipes.count { 
            val mte = it.mealTypeEn?.lowercase() ?: ""
            val mt = it.mealType?.lowercase() ?: ""
            mte.contains("appetizer") || mt.contains("entrada")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
            .verticalScroll(scrollState)
            .padding(24.dp)
    ) {
        // --- HEADER ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = greeting,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 26.sp
                    ),
                    color = OnBackground
                )
                Text(
                    text = if (isEs) "Encuentra tu receta ideal hoy" else "Find your ideal recipe today",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextMuted
                )
            }
            // Avatar decorativo premium
            BouncyPressEffect { modifier, interactionSource ->
                Surface(
                    modifier = modifier
                        .size(50.dp)
                        .clickable(interactionSource = interactionSource, indication = androidx.compose.foundation.LocalIndication.current) {
                            navController.navigate("perfil")
                        },
                    shape = CircleShape,
                    color = Primary.copy(alpha = 0.15f),
                    border = BorderStroke(1.5.dp, Primary)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        UserAvatar(
                            photoUrl = userProfile?.photoUrl ?: "",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // --- BUSCADOR ---
        BouncyPressEffect { modifier, interactionSource ->
            Surface(
                modifier = modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .clickable(interactionSource = interactionSource, indication = androidx.compose.foundation.LocalIndication.current) { 
                        navController.navigate("recetas_lista") 
                    },
                color = Surface,
                border = BorderStroke(1.dp, Surface)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = "Buscar",
                        tint = Primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(
                        text = if (isEs) "Buscar recetas, ingredientes..." else "Search recipes, ingredients...",
                        color = TextMuted,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        Spacer(Modifier.height(28.dp))

        // --- RECETA DEL DÍA ---
        if (recetaDelDia != null) {
            Text(
                text = if (isEs) "Receta del Día" else "Recipe of the Day",
                style = MaterialTheme.typography.titleLarge,
                color = OnBackground,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            val dayCardInteractionSource = remember { MutableInteractionSource() }
            val isDayCardPressed by dayCardInteractionSource.collectIsPressedAsState()
            val dayCardScale by animateFloatAsState(
                targetValue = if (isDayCardPressed) 0.90f else 1.0f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                label = "dayCardScale"
            )

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer(scaleX = dayCardScale, scaleY = dayCardScale)
                    .clip(RoundedCornerShape(28.dp))
                    .shimmerGlow(durationMillis = 3000) // Beautiful glowing sheen in wait/idle state
                    .clickable(interactionSource = dayCardInteractionSource, indication = androidx.compose.foundation.LocalIndication.current) { 
                        navController.navigate("recipeDetail/${recetaDelDia.id ?: 0}") 
                    },
                color = Surface,
                tonalElevation = 2.dp
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    ) {
                        // Imagen de receta
                        if (!recetaDelDia.image.isNullOrEmpty()) {
                            AsyncImage(
                                model = recetaDelDia.image,
                                contentDescription = recetaDelDia.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Primary.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.RestaurantMenu, null, tint = Primary, modifier = Modifier.size(48.dp))
                            }
                        }

                        // Gradient overlay para oscurecer la base de la imagen
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                                        startY = 100f
                                    )
                                )
                        )

                        // Badges arriba (Difficulty & Favorites)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Background.copy(alpha = 0.8f)
                            ) {
                                Text(
                                    text = translateText(recetaDelDia.difficulty, currentLanguage.value).uppercase(),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Primary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                            
                            // Botón de Favorito Premium
                            val isFav = favoritos.contains(recetaDelDia.id)
                            HeartBurstButton(
                                isFav = isFav,
                                onFavToggle = {
                                    if (com.google.firebase.auth.FirebaseAuth.getInstance().currentUser == null) {
                                        Toast.makeText(context, if (isEs) "Inicia sesión para guardar favoritos" else "Sign in to save favorites", Toast.LENGTH_SHORT).show()
                                        navController.navigate("perfil")
                                    } else {
                                        if (isFav) {
                                            recetaDelDia.id?.let { favoritos.remove(it) }
                                        } else {
                                            favoritos.add(recetaDelDia.id ?: 0)
                                        }
                                    }
                                },
                                modifier = Modifier.size(36.dp),
                                iconSize = 20.dp,
                                activeColor = Color.Red,
                                inactiveColor = OnBackground,
                                backgroundColor = Background.copy(alpha = 0.8f)
                            )
                        }
                    }

                    // Información
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = recetaDelDia.name ?: "",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 20.sp),
                            color = OnSurface
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.AccessTime, null, tint = Primary, modifier = Modifier.size(16.dp).pulseAnimation(durationMillis = 1500))
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "${(recetaDelDia.prepTimeMinutes ?: 0) + (recetaDelDia.cookTimeMinutes ?: 0)} min",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.RestaurantMenu, null, tint = Primary, modifier = Modifier.size(16.dp).pulseAnimation(durationMillis = 2000))
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = translateText(recetaDelDia.cuisine, currentLanguage.value),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(28.dp))

        // --- SECCIÓN CATEGORÍAS POPULARES ---
        Text(
            text = if (isEs) "Categorías Populares" else "Popular Categories",
            style = MaterialTheme.typography.titleLarge,
            color = OnBackground,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CategoryItem(
                icon = Icons.Rounded.BakeryDining,
                label = if (isEs) "Desayuno" else "Breakfast",
                subtitle = if (isEs) "$countBreakfast recetas" else "$countBreakfast recipes",
                modifier = Modifier.weight(1f)
            ) {
                navController.navigate("recetas_lista?mealType=Breakfast")
            }
            CategoryItem(
                icon = Icons.Rounded.LunchDining,
                label = if (isEs) "Almuerzo" else "Lunch",
                subtitle = if (isEs) "$countLunch recetas" else "$countLunch recipes",
                modifier = Modifier.weight(1f)
            ) {
                navController.navigate("recetas_lista?mealType=Lunch")
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CategoryItem(
                icon = Icons.Rounded.DinnerDining,
                label = if (isEs) "Cena" else "Dinner",
                subtitle = if (isEs) "$countDinner recetas" else "$countDinner recipes",
                modifier = Modifier.weight(1f)
            ) {
                navController.navigate("recetas_lista?mealType=Dinner")
            }
            CategoryItem(
                icon = Icons.Rounded.Icecream,
                label = if (isEs) "Postres" else "Desserts",
                subtitle = if (isEs) "$countDessert recetas" else "$countDessert recipes",
                modifier = Modifier.weight(1f)
            ) {
                navController.navigate("recetas_lista?mealType=Dessert")
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CategoryItem(
                icon = Icons.Rounded.LocalDrink,
                label = if (isEs) "Bebidas" else "Beverages",
                subtitle = if (isEs) "$countBeverage recetas" else "$countBeverage recipes",
                modifier = Modifier.weight(1f)
            ) {
                navController.navigate("recetas_lista?mealType=Beverage")
            }
            CategoryItem(
                icon = Icons.Rounded.Fastfood,
                label = if (isEs) "Entradas" else "Appetizers",
                subtitle = if (isEs) "$countAppetizer recetas" else "$countAppetizer recipes",
                modifier = Modifier.weight(1f)
            ) {
                navController.navigate("recetas_lista?mealType=Appetizer")
            }
        }
        Spacer(Modifier.height(28.dp))

        // --- MAPA DE SUPERMERCADOS ---
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .clickable {
                    navController.navigate("supermercados")
                },
            color = Surface,
            border = BorderStroke(1.dp, Primary.copy(alpha = 0.15f))
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(Primary.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Map,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(26.dp).bobbingAnimation(durationMillis = 2000, dy = 3f)
                    )
                }
                
                Spacer(Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isEs) "¿Faltan ingredientes?" else "Missing ingredients?",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = OnSurface
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = if (isEs) "Ver supermercados y mercados en Arequipa" else "Find nearby supermarkets and markets in Arequipa",
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

        Spacer(Modifier.height(20.dp))

        // --- SECCIÓN DE OPINIONES Y COMENTARIOS ---
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .clickable {
                    navController.navigate("opiniones")
                },
            color = Surface,
            border = BorderStroke(1.dp, Primary.copy(alpha = 0.15f))
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(Primary.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Forum,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(26.dp).bobbingAnimation(durationMillis = 2000, dy = 3f)
                    )
                }
                
                Spacer(Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isEs) "Opiniones de la Comunidad" else "Community Reviews",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = OnSurface
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = if (isEs) "Califica la app y comparte comentarios con otros Chefs" else "Rate the app and share feedback with other Chefs",
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

        Spacer(Modifier.height(28.dp))

        // --- RECOMENDADAS PARA TI (CAROUSEL) ---
        if (recetasDestacadas.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isEs) "Recomendadas para ti" else "Recommended for you",
                    style = MaterialTheme.typography.titleLarge,
                    color = OnBackground
                )
                BouncyPressEffect { modifier, interactionSource ->
                    Text(
                        text = if (isEs) "Ver todas" else "View all",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = Primary,
                        modifier = modifier.clickable(interactionSource = interactionSource, indication = null) { 
                            navController.navigate("recetas_lista") 
                        }
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(recetasDestacadas, key = { it.id ?: 0 }) { recipe ->
                    FeaturedRecipeCard(
                        recipe = recipe,
                        isFav = favoritos.contains(recipe.id),
                        onFavToggle = {
                            if (com.google.firebase.auth.FirebaseAuth.getInstance().currentUser == null) {
                                Toast.makeText(context, if (isEs) "Inicia sesión para guardar favoritos" else "Sign in to save favorites", Toast.LENGTH_SHORT).show()
                                navController.navigate("perfil")
                            } else {
                                if (favoritos.contains(recipe.id)) {
                                    recipe.id?.let { favoritos.remove(it) }
                                } else {
                                    favoritos.add(recipe.id ?: 0)
                                }
                            }
                        },
                        currentLanguage = currentLanguage.value,
                        onClick = { navController.navigate("recipeDetail/${recipe.id ?: 0}") }
                    )
                }
            }
        }

        Spacer(Modifier.height(28.dp))

        // --- ESTADÍSTICAS REALES ---
        Text(
            text = if (isEs) "Resumen del Catálogo" else "Catalog Summary",
            style = MaterialTheme.typography.titleLarge,
            color = OnBackground,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Surface,
            shape = RoundedCornerShape(24.dp)
        ) {
            val totalRecetas = preloadedRecipes.size
            val avgTime = remember(preloadedRecipes) {
                if (preloadedRecipes.isNotEmpty()) {
                    preloadedRecipes.map { (it.prepTimeMinutes ?: 0) + (it.cookTimeMinutes ?: 0) }.average().toInt()
                } else 0
            }
            val totalFavs = favoritos.size

            Row(
                modifier = Modifier.padding(24.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatItem(totalRecetas.toString(), if (isEs) "Recetas" else "Recipes")
                StatItem("$avgTime min", if (isEs) "Tiempo Prom." else "Avg. Time")
                StatItem(totalFavs.toString(), if (isEs) "Favoritos" else "Favorites")
            }
        }

        Spacer(Modifier.height(100.dp)) // Padding for bottom bar
    }

    if (showRecommendationDialog) {
        AlertDialog(
            onDismissRequest = { showRecommendationDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Restaurant,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = if (isEs) "¡Únete a la Cocina Uami! 🍳" else "Join Uami Kitchen! 🍳",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black)
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = if (isEs) {
                            "Te recomendamos iniciar sesión o registrarte como Chef para poder:"
                        } else {
                            "We recommend signing in or registering as a Chef to:"
                        },
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = OnBackground
                    )
                    Spacer(Modifier.height(12.dp))
                    val bullets = if (isEs) {
                        listOf(
                            "☁️ Sincronizar tus favoritos en la nube en tiempo real.",
                            "💬 Escribir opiniones y puntuar tus recetas preferidas.",
                            "❤️ Dar 'Me Gusta' a las opiniones de otros chefs.",
                            "🧑‍🍳 Personalizar tu nombre y foto de perfil."
                        )
                    } else {
                        listOf(
                            "☁️ Sync your favorites in the cloud in real-time.",
                            "💬 Leave reviews and rate your favorite recipes.",
                            "❤️ React and 'Like' comments from other chefs.",
                            "🧑‍🍳 Customize your name and profile picture."
                        )
                    }
                    bullets.forEach { bullet ->
                        Row(
                            modifier = Modifier.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = bullet,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMuted
                            )
                        }
                    }
                }
            },
            confirmButton = {
                BouncyPressEffect { modifier, interactionSource ->
                    Button(
                        onClick = {
                            showRecommendationDialog = false
                            navController.navigate("perfil")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        shape = RoundedCornerShape(14.dp),
                        modifier = modifier.clickable(interactionSource = interactionSource, indication = null) {}
                    ) {
                        Text(
                            text = if (isEs) "Registrarse / Iniciar Sesión" else "Register / Sign In",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showRecommendationDialog = false }
                ) {
                    Text(
                        text = if (isEs) "Continuar como Invitado" else "Continue as Guest",
                        color = TextMuted,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            shape = RoundedCornerShape(28.dp),
            containerColor = Surface,
            tonalElevation = 8.dp
        )
    }
}

@Composable
fun CategoryItem(
    icon: ImageVector, 
    label: String, 
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.80f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "scale"
    )
    val iconScale by animateFloatAsState(
        targetValue = if (isPressed) 1.35f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMedium),
        label = "iconScale"
    )
    val iconRotation by animateFloatAsState(
        targetValue = if (isPressed) 20f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMedium),
        label = "iconRotation"
    )

    Surface(
        modifier = modifier
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clip(RoundedCornerShape(20.dp))
            .clickable(interactionSource = interactionSource, indication = androidx.compose.foundation.LocalIndication.current) { onClick() },
        color = Surface,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon, 
                contentDescription = null, 
                tint = Primary, 
                modifier = Modifier
                    .size(28.dp)
                    .graphicsLayer(scaleX = iconScale, scaleY = iconScale, rotationZ = iconRotation)
                    .bobbingAnimation(durationMillis = 1600 + (label.hashCode() % 400), dy = 5f)
            )
            Spacer(Modifier.height(8.dp))
            Text(label, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = OnSurface)
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = TextMuted
                )
            }
        }
    }
}

@Composable
fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black), color = Primary)
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = TextMuted)
    }
}

@Composable
fun FeaturedRecipeCard(
    recipe: RecipeModel,
    isFav: Boolean,
    onFavToggle: () -> Unit,
    currentLanguage: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "scale"
    )
    val imgScale by animateFloatAsState(
        targetValue = if (isPressed) 1.15f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "imgScale"
    )

    Surface(
        modifier = Modifier
            .width(200.dp)
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clip(RoundedCornerShape(24.dp))
            .clickable(interactionSource = interactionSource, indication = androidx.compose.foundation.LocalIndication.current) { onClick() },
        color = Surface,
        tonalElevation = 2.dp
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                if (!recipe.image.isNullOrEmpty()) {
                    AsyncImage(
                        model = recipe.image,
                        contentDescription = recipe.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(scaleX = imgScale, scaleY = imgScale)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.RestaurantMenu, null, tint = Primary, modifier = Modifier.size(36.dp).pulseAnimation())
                    }
                }

                HeartBurstButton(
                    isFav = isFav,
                    onFavToggle = onFavToggle,
                    modifier = Modifier
                        .padding(12.dp)
                        .size(32.dp)
                        .align(Alignment.TopEnd),
                    iconSize = 16.dp,
                    activeColor = Color.Red,
                    inactiveColor = OnBackground,
                    backgroundColor = Background.copy(alpha = 0.8f)
                )
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = recipe.name ?: "",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = OnSurface
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = translateText(recipe.difficulty, currentLanguage),
                        style = MaterialTheme.typography.bodySmall,
                        color = Primary
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.AccessTime, null, tint = TextMuted, modifier = Modifier.size(12.dp).pulseAnimation(durationMillis = 1600))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "${(recipe.prepTimeMinutes ?: 0) + (recipe.cookTimeMinutes ?: 0)} min",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }
            }
        }
    }
}
