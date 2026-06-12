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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenNutritionist(
    currentLanguage: MutableState<String>,
    allRecipes: List<RecipeModel>,
    favoriteIds: List<Int>,
    onNavigateToRecipe: (Int) -> Unit
) {
    val context = LocalContext.current
    val isEs = currentLanguage.value == "es"
    val scope = rememberCoroutineScope()

    // Manager de IA Local
    val nutritionistManager = remember { NutritionistAIManager(context) }
    
    val isModelReady by nutritionistManager.isModelReady.collectAsState()
    val isInitializing by nutritionistManager.isInitializing.collectAsState()
    val downloadProgress by nutritionistManager.downloadProgress.collectAsState()

    var dietPlan by remember { mutableStateOf<List<DietDayPlan>>(emptyList()) }
    var isGenerating by remember { mutableStateOf(false) }
    
    // Cargar plan guardado en caché si existe en SharedPreferences
    val sharedPrefs = remember { context.getSharedPreferences("nutritionist_prefs", Context.MODE_PRIVATE) }
    
    LaunchedEffect(Unit) {
        val savedPlanJson = sharedPrefs.getString("saved_diet_plan", null)
        if (savedPlanJson != null) {
            try {
                // Recuperar plan decodificando los IDs de recetas
                val gson = com.google.gson.Gson()
                val type = object : com.google.gson.reflect.TypeToken<List<SavedDayPlanIds>>() {}.type
                val savedIdsList: List<SavedDayPlanIds> = gson.fromJson(savedPlanJson, type)
                
                dietPlan = savedIdsList.map { savedDay ->
                    DietDayPlan(
                        dayName = savedDay.dayName,
                        breakfast = allRecipes.find { it.id == savedDay.breakfastId } ?: allRecipes.first(),
                        lunch = allRecipes.find { it.id == savedDay.lunchId } ?: allRecipes.first(),
                        dinner = allRecipes.find { it.id == savedDay.dinnerId } ?: allRecipes.first()
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        // Pre-inicializar la IA en background
        nutritionistManager.initializeLLM()
    }

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
                    scope.launch {
                        isGenerating = true
                        
                        // Aseguramos inicializar LLM si es necesario
                        val initialized = nutritionistManager.initializeLLM()
                        val newPlan = if (initialized) {
                            nutritionistManager.generateAIDietPlan(allRecipes, favoriteIds, isEs)
                        } else {
                            nutritionistManager.generateOfflineDietPlan(allRecipes, favoriteIds, isEs)
                        }
                        
                        dietPlan = newPlan
                        
                        // Guardar en cache local
                        val savedIds = newPlan.map {
                            SavedDayPlanIds(it.dayName, it.breakfast.id ?: 0, it.lunch.id ?: 0, it.dinner.id ?: 0)
                        }
                        sharedPrefs.edit().putString("saved_diet_plan", com.google.gson.Gson().toJson(savedIds)).apply()
                        isGenerating = false
                    }
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
