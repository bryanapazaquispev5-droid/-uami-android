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

    var activeTab by remember { mutableIntStateOf(0) } // 0 = Plan, 1 = Chat
    var dietPlan by remember { mutableStateOf<List<DietDayPlan>>(emptyList()) }
    
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
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(Background).statusBarsPadding().padding(top = 16.dp, bottom = 8.dp)) {
                Text(
                    text = if (isEs) "Nutriólogo IA" else "AI Nutritionist",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                    color = OnBackground,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                Text(
                    text = if (isEs) "Planificación y asesoría nutricional 100% Local" else "100% Offline meal planning & advice",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                )
                
                Spacer(Modifier.height(12.dp))
                
                // Tabs de Navegación Estéticos
                TabRow(
                    selectedTabIndex = activeTab,
                    containerColor = Color.Transparent,
                    contentColor = Primary,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Tab(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        text = { Text(if (isEs) "Plan Semanal" else "Weekly Plan", fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        text = { Text(if (isEs) "Chat Nutriólogo" else "AI Chatbot", fontWeight = FontWeight.Bold) }
                    )
                }
            }
        },
        containerColor = Background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (activeTab) {
                0 -> DietPlanTab(
                    isEs = isEs,
                    dietPlan = dietPlan,
                    onGeneratePlan = {
                        val newPlan = nutritionistManager.generateOfflineDietPlan(allRecipes, favoriteIds, isEs)
                        dietPlan = newPlan
                        
                        // Guardar en cache local
                        val savedIds = newPlan.map {
                            SavedDayPlanIds(it.dayName, it.breakfast.id ?: 0, it.lunch.id ?: 0, it.dinner.id ?: 0)
                        }
                        sharedPrefs.edit().putString("saved_diet_plan", com.google.gson.Gson().toJson(savedIds)).apply()
                    },
                    onNavigateToRecipe = onNavigateToRecipe
                )
                1 -> ChatTab(
                    isEs = isEs,
                    nutritionistManager = nutritionistManager,
                    isModelReady = isModelReady,
                    isInitializing = isInitializing,
                    downloadProgress = downloadProgress,
                    favoriteRecipes = allRecipes.filter { favoriteIds.contains(it.id) }
                )
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
                text = if (isEs) "Analizaremos las recetas que te gustan para armarte un menú saludable semanal balanceado." else "We will analyze your liked recipes to build a balanced weekly healthy menu.",
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

// Chat UI Component
data class ChatMessage(
    val text: String,
    val isUser: Boolean
)

@Composable
fun ChatTab(
    isEs: Boolean,
    nutritionistManager: NutritionistAIManager,
    isModelReady: Boolean,
    isInitializing: Boolean,
    downloadProgress: Float,
    favoriteRecipes: List<RecipeModel>
) {
    val scope = rememberCoroutineScope()
    val chatMessages = remember { mutableStateListOf<ChatMessage>() }
    var inputText by remember { mutableStateOf("") }
    var isThinking by remember { mutableStateOf(false) }

    // Selector de archivos nativo
    val modelPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val success = nutritionistManager.importModel(uri)
                if (success) {
                    nutritionistManager.initializeLLM()
                }
            }
        }
    }

    if (!isModelReady) {
        // Pantalla de configuración del modelo local
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier.size(100.dp),
                shape = CircleShape,
                color = Primary.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.DownloadForOffline, 
                        null, 
                        tint = Primary, 
                        modifier = Modifier.size(48.dp).pulseAnimation()
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            Text(
                text = if (isEs) "Requiere Modelo de IA Local" else "Requires Local AI Model",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = OnBackground,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (isEs) {
                    "Para chatear sin internet con el Nutriólogo, necesitas descargar el modelo cuantizado de Google Gemma-2B (~1.3 GB) o importarlo de tus archivos."
                } else {
                    "To chat offline with the Nutritionist, you need to download Google Gemma-2B quantized model (~1.3 GB) or import it from your files."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
                textAlign = TextAlign.Center
            )
            
            Spacer(Modifier.height(32.dp))

            if (downloadProgress >= 0f) {
                // Barra de progreso de descarga
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LinearProgressIndicator(
                        progress = { downloadProgress },
                        color = Primary,
                        trackColor = Surface,
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(10.dp))
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = if (isEs) "Descargando: ${(downloadProgress * 100).toInt()}%" else "Downloading: ${(downloadProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = Primary
                    )
                }
            } else {
                // Botón de descargar
                BouncyPressEffect(squishFactor = 0.8f) { modifier, interactionSource ->
                    Button(
                        onClick = {
                            scope.launch {
                                val success = nutritionistManager.downloadModel()
                                if (success) {
                                    nutritionistManager.initializeLLM()
                                }
                            }
                        },
                        interactionSource = interactionSource,
                        modifier = modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) {
                        Icon(Icons.Rounded.CloudDownload, null, tint = OnPrimary)
                        Spacer(Modifier.width(12.dp))
                        Text(if (isEs) "DESCARGAR MODELO (1.3 GB)" else "DOWNLOAD MODEL (1.3 GB)", color = OnPrimary, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Botón de importar
                BouncyPressEffect(squishFactor = 0.8f) { modifier, interactionSource ->
                    OutlinedButton(
                        onClick = { modelPickerLauncher.launch("application/octet-stream") },
                        interactionSource = interactionSource,
                        modifier = modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Primary)
                    ) {
                        Icon(Icons.Rounded.FolderOpen, null, tint = Primary)
                        Spacer(Modifier.width(12.dp))
                        Text(if (isEs) "IMPORTAR ARCHIVO .BIN LOCAL" else "IMPORT LOCAL .BIN FILE", color = Primary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    } else if (isInitializing) {
        // Cargando LLM local en memoria
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(color = Primary, modifier = Modifier.size(56.dp))
            Spacer(Modifier.height(24.dp))
            Text(
                text = if (isEs) "Inicializando IA Local..." else "Initializing Local AI...",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = OnBackground
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (isEs) "Cargando modelo en la memoria del celular" else "Loading model into device memory",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted
            )
        }
    } else {
        // Asegurar que el LLM está cargado una vez que el modelo esté listo
        LaunchedEffect(Unit) {
            nutritionistManager.initializeLLM()
        }

        // Interfaz de Chat Activa
        val lazyListState = rememberLazyListState()

        Column(modifier = Modifier.fillMaxSize().background(Background)) {
            // Historial de mensajes
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                state = lazyListState,
                contentPadding = PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (chatMessages.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Surface,
                                border = BorderStroke(1.dp, Surface.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth(0.9f)
                            ) {
                                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Rounded.Psychology, null, tint = Primary, modifier = Modifier.size(40.dp))
                                    Spacer(Modifier.height(12.dp))
                                    Text(
                                        text = if (isEs) "Nutriólogo IA local Activo" else "Local AI Nutritionist Active",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = OnSurface
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = if (isEs) "Hazle consultas de nutrición basadas en tus gustos de forma completamente offline." else "Ask nutrition questions based on your tastes offline.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMuted,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }

                items(chatMessages) { msg ->
                    ChatBubble(msg)
                }

                if (isThinking) {
                    item {
                        ThinkingBubble(isEs)
                    }
                }
            }

            // Barra inferior de entrada de texto
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 8.dp,
                color = Surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .navigationBarsPadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text(if (isEs) "Pregúntale al nutriólogo..." else "Ask the nutritionist...", color = TextMuted) },
                        maxLines = 3,
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = Surface.copy(alpha = 0.2f),
                            focusedContainerColor = Background,
                            unfocusedContainerColor = Background
                        )
                    )
                    
                    Spacer(Modifier.width(12.dp))
                    
                    BouncyPressEffect(squishFactor = 0.7f) { modifier, interactionSource ->
                        FloatingActionButton(
                            onClick = {
                                if (inputText.trim().isNotEmpty() && !isThinking) {
                                    val text = inputText.trim()
                                    chatMessages.add(ChatMessage(text, true))
                                    inputText = ""
                                    isThinking = true
                                    
                                    // Scroll automático al fondo
                                    scope.launch {
                                        lazyListState.animateScrollToItem(chatMessages.size)
                                    }

                                    scope.launch {
                                        val reply = nutritionistManager.generateResponse(text, favoriteRecipes, if (isEs) "es" else "en")
                                        chatMessages.add(ChatMessage(reply, false))
                                        isThinking = false
                                        
                                        // Scroll automático al fondo tras recibir respuesta
                                        lazyListState.animateScrollToItem(chatMessages.size)
                                    }
                                }
                            },
                            interactionSource = interactionSource,
                            modifier = modifier.size(48.dp),
                            shape = CircleShape,
                            containerColor = Primary,
                            contentColor = OnPrimary
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.Send, null, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val alignment = if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleColor = if (message.isUser) Primary else Surface
    val textColor = if (message.isUser) OnPrimary else OnSurface
    val shape = if (message.isUser) {
        RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 4.dp)
    } else {
        RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 4.dp, bottomEnd = 20.dp)
    }

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Surface(
            shape = shape,
            color = bubbleColor,
            border = if (!message.isUser) BorderStroke(1.dp, Surface.copy(alpha = 0.5f)) else null,
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            Text(
                text = message.text,
                color = textColor,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Composable
fun ThinkingBubble(isEs: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "dots")
    val dotCount = 3
    val dotFlashing by infiniteTransition.animateValue(
        initialValue = 0,
        targetValue = dotCount,
        typeConverter = Int.VectorConverter,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dotsAnim"
    )

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
        Surface(
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 4.dp, bottomEnd = 20.dp),
            color = Surface,
            border = BorderStroke(1.dp, Surface.copy(alpha = 0.5f)),
            modifier = Modifier.width(120.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(dotCount) { i ->
                    val alpha = if (i == dotFlashing) 1f else 0.3f
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Primary.copy(alpha = alpha))
                    )
                }
            }
        }
    }
}
