package com.example.lab09

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.lab09.ejercicio1.remote.RecipeApiService
import com.example.lab09.ejercicio1.remote.MealDbApiService
import com.example.lab09.ejercicio1.models.RecipeModel
import com.example.lab09.ui.theme.*
import com.example.lab09.utils.*
import com.example.lab09.ejercicio1.ui.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

import android.speech.tts.TextToSpeech
import java.util.Locale
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Inicializar Utilidades
        LanguageManager.init(this)
        OnDeviceTranslator.init(this)
        
        // Inicializar TTS
        tts = TextToSpeech(this, this)

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Primary,
                    onPrimary = OnPrimary,
                    secondary = Secondary,
                    background = Background,
                    surface = Surface,
                    onBackground = OnBackground,
                    onSurface = OnSurface
                ),
                typography = AppTypography
            ) {
                ProgPrincipal9(tts)
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val localeES = Locale.forLanguageTag("es-ES")
            tts?.language = localeES
            
            // Intentar buscar una voz de alta calidad (network voice)
            try {
                val voices = tts?.voices
                val bestVoice = voices?.find { 
                    it.locale.language == "es" && it.name.contains("network", true) 
                } ?: voices?.find { it.locale.language == "es" }
                
                bestVoice?.let { tts?.voice = it }
            } catch (_: Exception) {
                // Si falla la selección de voz avanzada, se queda con la por defecto
            }

            // Ajustes para que suene menos robótico
            tts?.setPitch(1.05f)        // Un poquito más agudo para que suene amigable
            tts?.setSpeechRate(0.95f)    // Un poquito más lento para que se entienda mejor
        }
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}

@Composable
fun ProgPrincipal9(tts: TextToSpeech?) {
    val onSpeechFinished = remember { mutableStateOf<(() -> Unit)?>(null) }
    val currentLanguage = remember { 
        mutableStateOf(LanguageManager.getLanguage()) 
    }
    
    // Estado global para las recetas pre-cargadas
    var globalRecipes by remember { mutableStateOf<List<RecipeModel>>(emptyList()) }
    var isPreparingData by remember { mutableStateOf(false) }
    
    // Guardar el idioma cuando cambie
    LaunchedEffect(currentLanguage.value) {
        LanguageManager.setLanguage(currentLanguage.value)
        val locale = if (currentLanguage.value == "es") Locale.forLanguageTag("es-ES") else Locale.US
        tts?.language = locale
    }

    val urlBaseRecipes = "https://dummyjson.com/"
    val retrofitRecipes = Retrofit.Builder().baseUrl(urlBaseRecipes)
        .addConverterFactory(GsonConverterFactory.create()).build()
    val servicioRecipes = retrofitRecipes.create(RecipeApiService::class.java)

    val urlBaseMealDB = "https://www.themealdb.com/api/json/v1/1/"
    val retrofitMealDB = Retrofit.Builder().baseUrl(urlBaseMealDB)
        .addConverterFactory(GsonConverterFactory.create()).build()
    val servicioMealDB = retrofitMealDB.create(com.example.lab09.ejercicio1.remote.MealDbApiService::class.java)

    // Lógica de Pre-carga y Traducción (Optimizado)
    LaunchedEffect(currentLanguage.value) {
        if (currentLanguage.value.isNotEmpty()) {
            try {
                // 1. Carga inicial de datos
                val response1 = servicioRecipes.getRecipes(limit = 50, skip = 0)
                val recipes1 = response1.recipes ?: emptyList()
                val response2 = servicioMealDB.getRecipes(limit = 50, skip = 0)
                val recipes2 = response2.recipes ?: emptyList()
                val rawRecipes = recipes1 + recipes2

                // 2. Traducción IA (con el sistema de caché interno)
                globalRecipes = if (currentLanguage.value == "es") {
                    translateRecipesListAsync(rawRecipes, "es")
                } else {
                    rawRecipes
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                // Finalizar pantalla de carga si estaba activa
                isPreparingData = false
            }
        }
    }

    val navController = rememberNavController()
    val context = LocalContext.current
    val favoriteManager = remember { FavoriteManager(context) }
    val favoritos = remember { 
        mutableStateListOf<Int>().apply { 
            addAll(favoriteManager.loadFavorites()) 
        } 
    }

    // Persistir cambios automáticamente cada vez que la lista cambie
    LaunchedEffect(favoritos.toList()) {
        favoriteManager.saveFavorites(favoritos)
    }

    Scaffold(
        bottomBar = { 
            // Ocultar barra de navegación si no se ha elegido idioma o si se está cargando
            if (currentLanguage.value.isNotEmpty() && !isPreparingData) {
                CustomBottomBar(navController) 
            }
        },
        containerColor = Background
    ) { paddingValues ->
        when {
            currentLanguage.value.isEmpty() -> {
                LanguageSelectionScreen(currentLanguage) {
                    isPreparingData = true
                }
            }
            isPreparingData -> {
                PreparingDataScreen(currentLanguage.value)
            }
            else -> {
                Contenido(paddingValues, navController, servicioRecipes, servicioMealDB, favoritos, tts, onSpeechFinished, currentLanguage, globalRecipes)
            }
        }
    }
}

@Composable
fun PreparingDataScreen(lang: String) {
    val isEs = lang == "es"
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    
    // Ciclo de mensajes divertidos
    val messages = if (isEs) listOf(
        "Afilando los cuchillos...",
        "Precalentando el horno...",
        "Sazonando las recetas...",
        "IA de Google cocinando...",
        "Emplatando la experiencia..."
    ) else listOf(
        "Sharpening the knives...",
        "Preheating the oven...",
        "Seasoning the recipes...",
        "Google IA is cooking...",
        "Plating the experience..."
    )
    
    var currentMessageIndex by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while(true) {
            kotlinx.coroutines.delay(2000)
            currentMessageIndex = (currentMessageIndex + 1) % messages.size
        }
    }

    // Animación de escala para el chef
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    // Animación de flotación suave
    val offsetY by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offset"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Background, Surface)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Contenedor del Chef con Efecto de "Vapor"
            Box(contentAlignment = Alignment.TopCenter) {
                // Simulación de Vapor (3 círculos animados)
                repeat(3) { i ->
                    val steamAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.6f,
                        targetValue = 0f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1500, delayMillis = i * 500, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "steamAlpha$i"
                    )
                    val steamOffset by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = -100f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1500, delayMillis = i * 500, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "steamOffset$i"
                    )
                    
                    Box(
                        modifier = Modifier
                            .offset(y = steamOffset.dp, x = (i * 20 - 20).dp)
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = steamAlpha))
                    )
                }

                Image(
                    painter = painterResource(id = R.drawable.ic_chef_loading),
                    contentDescription = null,
                    modifier = Modifier
                        .size(160.dp)
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationY = offsetY
                        )
                )
            }
            
            Spacer(Modifier.height(40.dp))
            
            // Barra de progreso más sutil
            LinearProgressIndicator(
                color = Primary,
                trackColor = Surface,
                modifier = Modifier
                    .width(200.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(10.dp))
            )
            
            Spacer(Modifier.height(32.dp))
            
            // Texto principal animado por el cambio de índice
            androidx.compose.animation.AnimatedContent(
                targetState = messages[currentMessageIndex],
                transitionSpec = {
                    (fadeIn() + slideInVertically { it }).togetherWith(fadeOut() + slideOutVertically { -it })
                },
                label = "message"
            ) { text ->
                Text(
                    text = text,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    ),
                    color = OnBackground,
                    textAlign = TextAlign.Center
                )
            }
            
            Spacer(Modifier.height(12.dp))
            
            Text(
                if(isEs) "Configurando tu cocina personal" else "Setting up your personal kitchen",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun LanguageSelectionScreen(currentLanguage: MutableState<String>, onContinue: () -> Unit) {
    var selectedTemp by remember { mutableStateOf("") }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Rounded.Language,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = Primary
            )
            Spacer(Modifier.height(24.dp))
            Text(
                "Welcome / Bienvenido",
                style = MaterialTheme.typography.headlineMedium,
                color = OnBackground,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Choose your language / Elige tu idioma",
                style = MaterialTheme.typography.bodyLarge,
                color = TextMuted
            )
            Spacer(Modifier.height(48.dp))
            
            // Opción Inglés
            Surface(
                onClick = { selectedTemp = "en" },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = if (selectedTemp == "en") Primary.copy(alpha = 0.1f) else Surface,
                border = if (selectedTemp == "en") BorderStroke(2.dp, Primary) else null
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (selectedTemp == "en"),
                        onClick = { selectedTemp = "en" },
                        colors = RadioButtonDefaults.colors(selectedColor = Primary)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("English", color = OnBackground, fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            // Opción Español
            Surface(
                onClick = { selectedTemp = "es" },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = if (selectedTemp == "es") Primary.copy(alpha = 0.1f) else Surface,
                border = if (selectedTemp == "es") BorderStroke(2.dp, Primary) else null
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (selectedTemp == "es"),
                        onClick = { selectedTemp = "es" },
                        colors = RadioButtonDefaults.colors(selectedColor = Primary)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Español", color = OnBackground, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(48.dp))

            // Botón de confirmación (solo habilitado si hay algo seleccionado)
            Button(
                onClick = { 
                    if (selectedTemp.isNotEmpty()) {
                        currentLanguage.value = selectedTemp
                        LanguageManager.setFirstRunCompleted()
                        onContinue()
                    }
                },
                enabled = selectedTemp.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary,
                    disabledContainerColor = Surface
                )
            ) {
                Text(
                    if (selectedTemp == "es") "CONTINUAR" else "CONTINUE", 
                    color = if (selectedTemp.isNotEmpty()) OnPrimary else TextMuted,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun CustomBottomBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Surface(
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(32.dp)),
        color = Surface.copy(alpha = 0.95f),
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem(
                icon = Icons.Rounded.Explore,
                label = "Explorar",
                selected = currentRoute == "inicio",
                onClick = { navController.navigate("inicio") }
            )
            BottomNavItem(
                icon = Icons.Rounded.RestaurantMenu,
                label = "Recetas",
                selected = currentRoute == "recetas",
                onClick = { navController.navigate("recetas") }
            )
        }
    }
}

@Composable
fun BottomNavItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    val containerColor = if (selected) Primary else Color.Transparent
    val contentColor = if (selected) OnPrimary else TextMuted

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(containerColor)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = label, tint = contentColor, modifier = Modifier.size(22.dp))
            AnimatedVisibility(visible = selected) {
                Text(
                    text = label,
                    modifier = Modifier.padding(start = 8.dp),
                    color = contentColor,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@Composable
fun Contenido(
    pv: PaddingValues,
    navController: NavHostController,
    servicioRecipes: RecipeApiService,
    servicioMealDB: MealDbApiService,
    favoritos: MutableList<Int>,
    tts: TextToSpeech?,
    onSpeechFinished: MutableState<(() -> Unit)?>,
    currentLanguage: MutableState<String>,
    preloadedRecipes: List<RecipeModel>
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = pv.calculateBottomPadding())
    ) {
        NavHost(
            navController = navController,
            startDestination = "inicio",
            enterTransition = {
                fadeIn(animationSpec = tween(500)) + 
                scaleIn(initialScale = 0.8f, animationSpec = tween(500))
            },
            exitTransition = {
                fadeOut(animationSpec = tween(500)) + 
                scaleOut(targetScale = 1.1f, animationSpec = tween(500))
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(500)) + 
                scaleIn(initialScale = 1.1f, animationSpec = tween(500))
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(500)) + 
                scaleOut(targetScale = 0.8f, animationSpec = tween(500))
            }
        ) {
            composable("inicio") { ScreenInicio() }
            composable("recetas") { ScreenRecipeMenu(navController, currentLanguage) }
            composable("recetas_lista") { ScreenRecipes(navController, servicioRecipes, servicioMealDB, favoritos, currentLanguage, preloadedRecipes) }
            composable("recetas_favoritos") { ScreenFavorites(navController, servicioRecipes, servicioMealDB, favoritos, currentLanguage) }
            composable("recipeDetail/{id}", arguments = listOf(
                navArgument("id") { type = NavType.IntType }
            )) {
                val id = it.arguments?.getInt("id") ?: 0
                ScreenRecipeDetail(navController, servicioRecipes, servicioMealDB, id, favoritos, currentLanguage)
            }
            composable("cookingMode/{id}", arguments = listOf(
                navArgument("id") { type = NavType.IntType }
            )) {
                val id = it.arguments?.getInt("id") ?: 0
                ScreenCookingMode(navController, servicioRecipes, servicioMealDB, id, tts, onSpeechFinished, currentLanguage)
            }
        }
    }
}

@Composable
fun ScreenInicio() {
    val scrollState = rememberScrollState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
            .verticalScroll(scrollState)
            .padding(24.dp)
    ) {
        // Hero Section
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Primary.copy(alpha = 0.1f),
            shape = RoundedCornerShape(32.dp),
            border = BorderStroke(1.dp, Primary.copy(alpha = 0.2f))
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    modifier = Modifier.size(80.dp),
                    shape = CircleShape,
                    color = Primary
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = OnPrimary, modifier = Modifier.size(40.dp))
                    }
                }
                Spacer(Modifier.height(24.dp))
                Text(
                    "Gourmet Premium",
                    style = MaterialTheme.typography.headlineMedium,
                    color = OnBackground,
                    textAlign = TextAlign.Center
                )
                Text(
                    "Tu portal gastronómico de alta gama",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextMuted,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        // Categories Section
        Text(
            "Categorías Populares",
            style = MaterialTheme.typography.titleLarge,
            color = OnBackground,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CategoryItem(Icons.Rounded.BakeryDining, "Desayuno", Modifier.weight(1f))
            CategoryItem(Icons.Rounded.LunchDining, "Almuerzo", Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CategoryItem(Icons.Rounded.DinnerDining, "Cena", Modifier.weight(1f))
            CategoryItem(Icons.Rounded.Icecream, "Postres", Modifier.weight(1f))
        }

        Spacer(Modifier.height(32.dp))

        // Quick Stats
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Surface,
            shape = RoundedCornerShape(24.dp)
        ) {
            Row(
                modifier = Modifier.padding(24.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                StatItem("500+", "Recetas")
                StatItem("12k", "Usuarios")
                StatItem("4.9", "Rating")
            }
        }
        
        Spacer(Modifier.height(100.dp)) // Padding for bottom bar
    }
}

@Composable
fun CategoryItem(icon: ImageVector, label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = Surface,
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = Primary, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(8.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = OnSurface)
        }
    }
}

@Composable
fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = Primary)
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
    }
}
