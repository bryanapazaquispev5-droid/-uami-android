package com.example.lab09

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
import com.example.lab09.ejercicio1.remote.RecipeApiService
import com.example.lab09.ejercicio1.models.RecipeModel
import com.example.lab09.utils.RecipeCacheManager
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
    
    // Estado global para las recetas pre-cargadas
    var globalRecipes by remember { mutableStateOf<List<RecipeModel>>(emptyList()) }
    var isPreparingData by remember { mutableStateOf(false) }
    var isDownloadFailed by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var downloadStatus by remember { mutableStateOf("") }
    var startupErrorMessage by remember { mutableStateOf("") }
    
    // Guardar el idioma cuando cambie
    LaunchedEffect(currentLanguage.value) {
        LanguageManager.setLanguage(currentLanguage.value)
        val locale = if (currentLanguage.value == "es") Locale.forLanguageTag("es-ES") else Locale.US
        tts?.language = locale
    }

    val navController = rememberNavController()
    val context = LocalContext.current
    val favoriteManager = remember { FavoriteManager(context) }
    val cacheManager = remember { RecipeCacheManager(context) }
    
    val favoritos = remember { 
        mutableStateListOf<Int>().apply { 
            addAll(favoriteManager.loadFavorites()) 
        } 
    }

    val urlBaseLocal = "https://recetasc24.loca.lt/"
    
    // Configurar OkHttp para saltar la advertencia de Localtunnel
    val okHttpClient = okhttp3.OkHttpClient.Builder().addInterceptor { chain ->
        val request = chain.request().newBuilder()
            .addHeader("Bypass-Tunnel-Reminder", "true")
            .addHeader("User-Agent", "Mozilla/5.0")
            .build()
        chain.proceed(request)
    }.build()

    val retrofitRecipes = Retrofit.Builder().baseUrl(urlBaseLocal)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create()).build()
    val servicioRecipes = retrofitRecipes.create(RecipeApiService::class.java)

    // Lógica de Pre-carga, Traducción y Caché
    LaunchedEffect(currentLanguage.value, isPreparingData, isDownloadFailed) {
        if (currentLanguage.value.isNotEmpty()) {
            val isEs = currentLanguage.value == "es"
            
            // Si hay caché válido, no estamos descargando ni hay fallo, cargamos directo y entramos a la app
            if (cacheManager.hasCache() && !isPreparingData && !isDownloadFailed) {
                globalRecipes = cacheManager.loadRecipes()
                Log.d("OFFLINE", "Cargado desde caché existente")
            } 
            // Modo Descarga Activa
            else if (isPreparingData) {
                isDownloadFailed = false
                
                // 0. Verificar Conexión a Internet
                if (!isInternetAvailable(context)) {
                    startupErrorMessage = if (isEs) "No hay conexión a internet. Verifica tu red." else "No internet connection. Check your network."
                    isDownloadFailed = true
                    isPreparingData = false
                    return@LaunchedEffect
                }

                try {
                    Log.d("OFFLINE", "Iniciando descarga completa y traducción...")
                    downloadProgress = 0.1f
                    downloadStatus = if (isEs) "Conectando con la API..." else "Connecting to API..."
                    
                    // 1. Descargar de la API
                    val rawRecipes = servicioRecipes.getRecipes()
                    if (rawRecipes.isEmpty()) throw Exception("API_EMPTY")

                    downloadProgress = 0.2f
                    downloadStatus = if (isEs) "Traduciendo recetas..." else "Translating recipes..."

                    // 2. Traducción IA
                    var processedRecipes = if (isEs) {
                        translateRecipesListAsync(rawRecipes, "es")
                    } else {
                        rawRecipes
                    }
                    
                    // 3. Descargar imágenes para que funcionen 100% offline
                    val total = processedRecipes.size
                    processedRecipes = processedRecipes.mapIndexed { index, recipe ->
                        // VERIFICACIÓN CONTINUA
                        if (!isInternetAvailable(context)) throw Exception("INTERNET_LOST")

                        downloadProgress = 0.2f + (0.7f * index / total)
                        downloadStatus = if (isEs) "Descargando imágenes... ($index/$total)" else "Downloading images... ($index/$total)"
                        
                        var newImage = recipe.image
                        if (recipe.image != null && recipe.image.startsWith("http")) {
                            val fileName = "img_${recipe.id}.jpg"
                            val file = java.io.File(context.filesDir, fileName)

                            // REANUDAR: Si ya existe, saltar
                            if (file.exists() && file.length() > 0) {
                                newImage = "file://${file.absolutePath}"
                            } else {
                                try {
                                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                        val request = okhttp3.Request.Builder().url(recipe.image).build()
                                        val response = okHttpClient.newCall(request).execute()
                                        if (response.isSuccessful) {
                                            val bytes = response.body?.bytes()
                                            if (bytes != null) {
                                                file.writeBytes(bytes)
                                                newImage = "file://${file.absolutePath}"
                                            }
                                        }
                                    }
                                } catch (e: Exception) { throw Exception("INTERNET_LOST") }
                            }
                        }
                        recipe.copy(image = newImage)
                    }

                    downloadProgress = 0.95f
                    downloadStatus = if (isEs) "Guardando base de datos..." else "Saving database..."
                    
                    globalRecipes = processedRecipes
                    
                    // 4. Guardar en Caché solo al terminar todo al 100%
                    if (globalRecipes.isNotEmpty()) {
                        cacheManager.saveRecipes(globalRecipes)
                    }
                    
                    downloadProgress = 1.0f
                    downloadStatus = if (isEs) "¡Listo para cocinar!" else "Ready to cook!"
                    
                    kotlinx.coroutines.delay(1000)
                    isPreparingData = false // Fin de la pantalla de carga (Exitoso)
                    
                } catch (e: Exception) {
                    Log.e("OFFLINE", "Error en sync: ${e.message}")
                    if (!cacheManager.hasCache()) {
                        // Fallo catastrófico y no hay backup. Se queda en la pantalla de carga bloqueado.
                        startupErrorMessage = if (isEs) "Ups, algo salió mal al descargar los datos. Por favor, reinténtalo." else "Oops, something went wrong downloading data. Please try again."
                        isDownloadFailed = true
                        isPreparingData = false
                    } else {
                        // Falló la actualización, pero ya hay un caché viejo que sirve
                        globalRecipes = cacheManager.loadRecipes()
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
            // Ocultar barra de navegación si no hay caché completo (obligando a pasar por el loading screen)
            if (currentLanguage.value.isNotEmpty() && cacheManager.hasCache() && !isPreparingData && !isDownloadFailed) {
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
            !cacheManager.hasCache() || isPreparingData || isDownloadFailed -> {
                // BLOQUEO OBLIGATORIO: Si no hay caché, nunca entra a la App.
                
                // Si entra aquí sin estar descargando ni fallando (ej. reabrió la app), forzamos la descarga automática
                LaunchedEffect(Unit) {
                    if (!cacheManager.hasCache() && !isPreparingData && !isDownloadFailed) {
                        isPreparingData = true
                    }
                }
                
                PreparingDataScreen(currentLanguage.value, downloadProgress, downloadStatus, isDownloadFailed, startupErrorMessage) {
                    // Botón Reintentar
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
fun PreparingDataScreen(lang: String, progress: Float, statusText: String, isFailed: Boolean = false, errorMessage: String = "", onRetry: () -> Unit = {}) {
    val isEs = lang == "es"
    val infiniteTransition = rememberInfiniteTransition(label = "loading")

    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

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
            .background(Brush.verticalGradient(colors = listOf(Background, Surface))),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            Box(contentAlignment = Alignment.TopCenter) {
                if (!isFailed) {
                    repeat(3) { i ->
                        val steamAlpha by infiniteTransition.animateFloat(
                            initialValue = 0.6f, targetValue = 0f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1500, delayMillis = i * 500, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            ), label = "steamAlpha$i"
                        )
                        val steamOffset by infiniteTransition.animateFloat(
                            initialValue = 0f, targetValue = -100f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1500, delayMillis = i * 500, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            ), label = "steamOffset$i"
                        )
                        Box(modifier = Modifier.offset(y = steamOffset.dp, x = (i * 20 - 20).dp).size(12.dp).clip(CircleShape).background(Color.White.copy(alpha = steamAlpha)))
                    }
                }
                Image(
                    painter = painterResource(id = R.drawable.ic_chef_loading),
                    contentDescription = null,
                    modifier = Modifier.size(160.dp).graphicsLayer(scaleX = scale, scaleY = scale, translationY = offsetY)
                )
            }
            
            Spacer(Modifier.height(40.dp))
            
            if (isFailed) {
                Surface(
                    color = Color.Red.copy(alpha = 0.1f), shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f)), modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.ErrorOutline, contentDescription = "Error", tint = Color.Red)
                        Spacer(Modifier.width(12.dp))
                        Text(text = errorMessage, color = Color.Red, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = onRetry, modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null, tint = OnPrimary)
                    Spacer(Modifier.width(8.dp))
                    Text(if (isEs) "REINTENTAR DESCARGA" else "RETRY DOWNLOAD", color = OnPrimary, fontWeight = FontWeight.Bold)
                }
            } else {
                Text(text = "${(progress * 100).toInt()}%", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold), color = Primary)
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(progress = { progress }, color = Primary, trackColor = Surface, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(10.dp)))
                Spacer(Modifier.height(24.dp))
                Text(text = statusText.ifEmpty { if (isEs) "Conectando..." else "Connecting..." }, style = MaterialTheme.typography.titleMedium, color = OnBackground, textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                Text(if (isEs) "Configurando tu cocina offline" else "Setting up your offline kitchen", style = MaterialTheme.typography.bodyMedium, color = TextMuted, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun LanguageSelectionScreen(currentLanguage: MutableState<String>, errorMessage: String = "", onContinue: () -> Unit) {
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
            
            if (errorMessage.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
                Surface(
                    color = Color.Red.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.ErrorOutline, contentDescription = "Error", tint = Color.Red)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = errorMessage,
                            color = Color.Red,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            Spacer(Modifier.height(32.dp))
            
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
        Text(value, style = MaterialTheme.typography.titleLarge, color = Primary)
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
    }
}
