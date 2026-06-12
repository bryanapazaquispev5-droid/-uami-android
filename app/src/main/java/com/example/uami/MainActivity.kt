package com.example.uami

import android.util.Log
import android.os.Bundle
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
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
import com.example.uami.ejercicio1.remote.RecipeApiService
import com.example.uami.ejercicio1.models.RecipeModel
import com.example.uami.utils.RecipeCacheManager
import com.example.uami.ui.theme.*
import com.example.uami.utils.*
import com.example.uami.ejercicio1.ui.*
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

import com.example.uami.sync.UpdateManager
import com.example.uami.sync.SyncResult
import com.example.uami.ui.screens.*

fun isInternetAvailable(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = connectivityManager.activeNetwork ?: return false
    val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
    return when {
        activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
        activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
        activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
        else -> false
    }
}

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
    
    // Estado global para las recetas
    var globalRecipes by remember { mutableStateOf<List<RecipeModel>>(emptyList()) }
    var isPreparingData by remember { mutableStateOf(false) }
    var isDownloadFailed by remember { mutableStateOf(false) }
    var isUpdateChecked by remember { mutableStateOf(false) } // NUEVO: Evita bucles infinitos
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var downloadStatus by remember { mutableStateOf("") }
    var startupErrorMessage by remember { mutableStateOf("") }
    
    // Configuración Inicial
    val context = LocalContext.current
    val favoriteManager = remember { FavoriteManager(context) }
    val cacheManager = remember { RecipeCacheManager(context) }
    val navController = rememberNavController()
    
    val favoritos = remember { 
        mutableStateListOf<Int>().apply { addAll(favoriteManager.loadFavorites()) } 
    }

    val okHttpClient = remember {
        okhttp3.OkHttpClient.Builder().addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("Bypass-Tunnel-Reminder", "true")
                .addHeader("User-Agent", "Mozilla/5.0")
                .build()
            chain.proceed(request)
        }.build()
    }

    val servicioRecipes = remember {
        Retrofit.Builder().baseUrl("https://recetasc24.loca.lt/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create()).build()
            .create(RecipeApiService::class.java)
    }

    val updateManager = remember { UpdateManager(context, servicioRecipes, cacheManager, okHttpClient) }
    
    // Guardar el idioma cuando cambie
    LaunchedEffect(currentLanguage.value) {
        if (currentLanguage.value.isNotEmpty()) {
            LanguageManager.setLanguage(currentLanguage.value)
            tts?.language = if (currentLanguage.value == "es") Locale.forLanguageTag("es-ES") else Locale.US
        }
    }

    // Lógica de Sincronización Modular
    LaunchedEffect(currentLanguage.value, isPreparingData, isDownloadFailed) {
        if (currentLanguage.value.isNotEmpty()) {
            // Caso 1: Ya hay datos locales y NO hemos buscado actualizaciones aún
            if (cacheManager.hasCache() && !isPreparingData && !isDownloadFailed && !isUpdateChecked) {
                globalRecipes = cacheManager.loadRecipes()
                Log.d("OFFLINE", "Cargado desde caché existente")
                
                if (isInternetAvailable(context)) {
                    isPreparingData = true // Lanzar comprobación de red
                } else {
                    isUpdateChecked = true // No hay red, abrimos con lo que hay
                }
            } 
            // Caso 2: Proceso de Sincronización Activo
            else if (isPreparingData) {
                isDownloadFailed = false
                val isFirstRun = !cacheManager.hasCache()
                
                val result = updateManager.checkAndSync(
                    currentLanguage = currentLanguage.value,
                    isFirstRun = isFirstRun,
                    onProgress = { progress, status ->
                        downloadProgress = progress
                        downloadStatus = status
                    }
                )

                when (result) {
                    is SyncResult.Success -> {
                        globalRecipes = result.recipes
                        isUpdateChecked = true 
                        isPreparingData = false
                    }
                    is SyncResult.Error -> {
                        startupErrorMessage = result.message
                        if (isFirstRun) {
                            currentLanguage.value = ""
                            LanguageManager.setLanguage("")
                        } else {
                            globalRecipes = cacheManager.loadRecipes()
                        }
                        isUpdateChecked = true 
                        isDownloadFailed = true
                        isPreparingData = false
                    }
                }
            }
        }
    }

    // Persistir cambios automáticamente cada vez que la lista cambie
    LaunchedEffect(favoritos.toList()) {
        favoriteManager.saveFavorites(favoritos)
    }

    Scaffold(
        bottomBar = { 
            if (currentLanguage.value.isNotEmpty() && cacheManager.hasCache() && !isPreparingData && !isDownloadFailed) {
                CustomBottomBar(navController) 
            }
        },
        containerColor = Background
    ) { paddingValues ->
        when {
            currentLanguage.value.isEmpty() -> {
                LanguageSelectionScreen(currentLanguage, startupErrorMessage) {
                    startupErrorMessage = ""
                    isPreparingData = true
                }
            }
            !cacheManager.hasCache() || isPreparingData || isDownloadFailed -> {
                LaunchedEffect(Unit) {
                    if (!cacheManager.hasCache() && !isPreparingData && !isDownloadFailed) {
                        isPreparingData = true
                    }
                }
                PreparingDataScreen(currentLanguage.value, downloadProgress, downloadStatus, isDownloadFailed, startupErrorMessage) {
                    isDownloadFailed = false
                    startupErrorMessage = ""
                    isPreparingData = true
                }
            }
            else -> {
                Contenido(paddingValues, navController, servicioRecipes, favoritos, tts, onSpeechFinished, currentLanguage, globalRecipes)
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
            composable("recetas_lista") { ScreenRecipes(navController, servicioRecipes, favoritos, currentLanguage, preloadedRecipes) }
            composable("recetas_favoritos") { ScreenFavorites(navController, servicioRecipes, favoritos, currentLanguage, preloadedRecipes) }
            composable("recipeDetail/{id}", arguments = listOf(
                navArgument("id") { type = NavType.IntType }
            )) {
                val id = it.arguments?.getInt("id") ?: 0
                ScreenRecipeDetail(navController, servicioRecipes, id, favoritos, currentLanguage, preloadedRecipes)
            }
            composable("cookingMode/{id}", arguments = listOf(
                navArgument("id") { type = NavType.IntType }
            )) {
                val id = it.arguments?.getInt("id") ?: 0
                ScreenCookingMode(navController, servicioRecipes, id, tts, onSpeechFinished, currentLanguage, preloadedRecipes)
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
        Text(text = value, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black), color = Primary)
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = TextMuted)
    }
}
