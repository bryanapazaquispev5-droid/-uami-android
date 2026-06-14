package com.example.uami.recipes.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.uami.R
import com.example.uami.recipes.models.RecipeModel
import com.example.uami.ui.theme.*
import com.example.uami.utils.*
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModelProvider
import androidx.activity.ComponentActivity
import com.example.uami.recipes.data.RecipeRepository
import com.example.uami.recipes.viewmodel.NutritionistViewModel
import com.example.uami.recipes.viewmodel.ViewModelFactory
import com.example.uami.recipes.viewmodel.ReviewsViewModel
import com.example.uami.findActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenNutritionist(
    currentLanguage: MutableState<String>,
    allRecipes: List<RecipeModel>,
    favoriteIds: List<Int>,
    onNavigateToRecipe: (Int) -> Unit,
    reviewsViewModel: ReviewsViewModel,
    onNavigateToProfile: () -> Unit
) {
    val context = LocalContext.current
    val isEs = currentLanguage.value == "es"
    val currentUser by reviewsViewModel.currentUser.collectAsState()

    // Obtener el ViewModel de la IA de nutrición
    val activity = remember(context) { context.findActivity()!! }
    val repository = remember { RecipeRepository(context.applicationContext) }
    val factory = remember { ViewModelFactory(repository, context.applicationContext) }
    val nutritionistViewModel = remember(activity) {
        ViewModelProvider(activity, factory)[NutritionistViewModel::class.java]
    }

    val dietPlan by nutritionistViewModel.dietPlan.collectAsState()
    val isGenerating by nutritionistViewModel.isGenerating.collectAsState()
    val isModelReady by nutritionistViewModel.isModelReady.collectAsState()
    val isInitializing by nutritionistViewModel.isInitializing.collectAsState()
    val downloadProgress by nutritionistViewModel.downloadProgress.collectAsState()

    // Cargar plan guardado en primer renderizado si el usuario está logueado
    LaunchedEffect(allRecipes, currentUser) {
        if (currentUser != null && allRecipes.isNotEmpty()) {
            nutritionistViewModel.loadSavedPlan(allRecipes)
        }
    }

    if (currentUser == null) {
        Scaffold(
            topBar = {
                Column(modifier = Modifier.background(Background).statusBarsPadding().padding(top = 16.dp, bottom = 8.dp)) {
                    Text(
                        text = if (isEs) "Plan Semanal IA" else "AI Weekly Plan",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                        color = OnBackground,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            },
            containerColor = Background
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    color = Surface,
                    border = BorderStroke(1.dp, Primary.copy(alpha = 0.15f))
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = "Lock",
                            tint = Primary,
                            modifier = Modifier
                                .size(64.dp)
                                .pulseAnimation(durationMillis = 2000)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = if (isEs) "Función Exclusiva para Chefs ⭐" else "Exclusive Feature for Chefs ⭐",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                            color = OnBackground,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = if (isEs) {
                                "Para poder generar un plan nutricional semanal personalizado con IA, necesitas estar registrado e iniciar sesión."
                            } else {
                                "To generate a customized weekly meal plan using AI, you need to be registered and signed in."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(24.dp))

                        BouncyPressEffect { modifier, interactionSource ->
                            Button(
                                onClick = onNavigateToProfile,
                                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                                shape = RoundedCornerShape(16.dp),
                                modifier = modifier
                                    .fillMaxWidth()
                                    .clickable(interactionSource = interactionSource, indication = null) {},
                                contentPadding = PaddingValues(vertical = 14.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.Login, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = if (isEs) "Iniciar Sesión / Registrarse" else "Sign In / Register",
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 15.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    } else {
        Scaffold(
            topBar = {
                Column(modifier = Modifier.background(Background).statusBarsPadding().padding(top = 16.dp, bottom = 8.dp)) {
                    Text(
                        text = if (isEs) "Plan Semanal IA" else "AI Weekly Plan",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                        color = OnBackground,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    Text(
                        text = if (isEs) "Planificación nutricional 100% Local con IA" else "100% Offline meal planning with AI",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                    )
                }
            },
            containerColor = Background
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                DietPlanTab(
                    isEs = isEs,
                    dietPlan = dietPlan,
                    onGeneratePlan = {
                        nutritionistViewModel.generateDietPlan(allRecipes, favoriteIds, isEs)
                    },
                    onNavigateToRecipe = onNavigateToRecipe
                )
            
            if (isGenerating) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.85f))
                        .clickable(enabled = false) {}, // Intercept clicks
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        if (isInitializing) {
                            if (downloadProgress >= 0f) {
                                LinearProgressIndicator(
                                    progress = { downloadProgress },
                                    color = Primary,
                                    trackColor = Surface,
                                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(10.dp))
                                )
                                Spacer(Modifier.height(24.dp))
                                Text(
                                    text = if (isEs) "Copiando IA Local al teléfono..." else "Copying Local AI to device...",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = OnBackground,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = if (isEs) "${(downloadProgress * 100).toInt()}% completado" else "${(downloadProgress * 100).toInt()}% completed",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextMuted,
                                    textAlign = TextAlign.Center
                                )
                            } else {
                                CircularProgressIndicator(
                                    color = Primary,
                                    modifier = Modifier.size(56.dp)
                                )
                                Spacer(Modifier.height(24.dp))
                                Text(
                                    text = if (isEs) "Inicializando IA Local..." else "Initializing Local AI...",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = OnBackground,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = if (isEs) "Cargando modelo en memoria..." else "Loading model into memory...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextMuted,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            Surface(
                                modifier = Modifier.size(100.dp).pulseAnimation(durationMillis = 1500, scaleRange = 0.15f),
                                shape = CircleShape,
                                color = Primary.copy(alpha = 0.15f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.AutoAwesome, null, tint = Primary, modifier = Modifier.size(48.dp))
                                }
                            }
                            Spacer(Modifier.height(24.dp))
                            Text(
                                text = if (isEs) "Generando Plan Semanal con IA..." else "Generating Weekly Plan with AI...",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = OnBackground,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = if (isEs) "La IA está seleccionando tus mejores recetas y diseñando un menú balanceado..." else "AI is selecting your best recipes and designing a balanced menu...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMuted,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}
}

// Estructura auxiliar para guardar caché del plan
data class SavedDayPlanIds(
    val dayName: String,
    val breakfastId: Int,
    val lunchId: Int,
    val dinnerId: Int
)

@Composable
fun DietPlanTab(
    isEs: Boolean,
    dietPlan: List<DietDayPlan>,
    onGeneratePlan: () -> Unit,
    onNavigateToRecipe: (Int) -> Unit
) {
    if (dietPlan.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier.size(100.dp),
                shape = CircleShape,
                color = Primary.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.RestaurantMenu, null, tint = Primary, modifier = Modifier.size(48.dp))
                }
            }
            Spacer(Modifier.height(24.dp))
            Text(
                text = if (isEs) "Genera tu Plan Nutricional" else "Generate Your Nutrition Plan",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = OnBackground,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (isEs) "Analizaremos las recetas que te gustan para armarte un menú saludable semanal balanceado con IA local." else "We will analyze your liked recipes to build a balanced weekly healthy menu with local AI.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(32.dp))
            
            BouncyPressEffect(squishFactor = 0.75f) { modifier, interactionSource ->
                Button(
                    onClick = onGeneratePlan,
                    interactionSource = interactionSource,
                    modifier = modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .shimmerGlow(durationMillis = 2000),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Icon(Icons.Rounded.AutoAwesome, null, tint = OnPrimary)
                    Spacer(Modifier.width(12.dp))
                    Text(if (isEs) "GENERAR MENÚ SEMANAL" else "GENERATE WEEKLY MENU", color = OnPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (isEs) "Tu Plan Semanal" else "Your Weekly Plan",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = OnBackground
                )
                TextButton(onClick = onGeneratePlan) {
                    Icon(Icons.Rounded.Refresh, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (isEs) "Regenerar" else "Regenerate")
                }
            }
            
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(dietPlan) { dayPlan ->
                    DayPlanCard(dayPlan, isEs, onNavigateToRecipe)
                }
            }
        }
    }
}

@Composable
fun DayPlanCard(plan: DietDayPlan, isEs: Boolean, onNavigateToRecipe: (Int) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Surface,
        border = BorderStroke(1.dp, Surface.copy(alpha = 0.8f)),
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = plan.dayName.uppercase(),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                color = Secondary
            )
            Spacer(Modifier.height(12.dp))
            
            // Comidas
            MealRow(if (isEs) "Desayuno" else "Breakfast", plan.breakfast, onNavigateToRecipe)
            HorizontalDivider(color = Background.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 12.dp))
            MealRow(if (isEs) "Almuerzo" else "Lunch", plan.lunch, onNavigateToRecipe)
            HorizontalDivider(color = Background.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 12.dp))
            MealRow(if (isEs) "Cena" else "Dinner", plan.dinner, onNavigateToRecipe)
        }
    }
}

@Composable
fun MealRow(label: String, recipe: RecipeModel, onNavigateToRecipe: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigateToRecipe(recipe.id ?: 0) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = recipe.image,
            contentDescription = null,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Background),
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
            Text(
                recipe.name ?: "",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = OnSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(Icons.Rounded.ChevronRight, null, tint = TextMuted, modifier = Modifier.size(20.dp))
    }
}
