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

import com.example.uami.recipes.models.RecipeModel
import com.example.uami.utils.RecipeCacheManager
import com.example.uami.ui.theme.*
import com.example.uami.utils.*
import com.example.uami.recipes.ui.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import androidx.lifecycle.ViewModelProvider
import com.example.uami.recipes.data.RecipeRepository
import com.example.uami.recipes.viewmodel.MainViewModel
import com.example.uami.recipes.viewmodel.ViewModelFactory
import com.example.uami.recipes.viewmodel.ReviewsViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

import android.speech.tts.TextToSpeech
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging
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
import coil.compose.AsyncImage
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState

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

        // Solicitar permisos de notificación en Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        // Obtener y loguear el token de Firebase Cloud Messaging
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FCM_TOKEN", "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }
            val token = task.result
            Log.d("FCM_TOKEN", "Firebase Cloud Messaging Token: $token")
            val prefs = getSharedPreferences("uami_prefs", Context.MODE_PRIVATE)
            prefs.edit().putString("fcm_token", token).apply()
        }

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
                UamiApp(tts)
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

fun Context.findActivity(): ComponentActivity? {
    var currentContext = this
    while (currentContext is android.content.ContextWrapper) {
        if (currentContext is ComponentActivity) {
            return currentContext
        }
        currentContext = currentContext.baseContext
    }
    return null
}

@Composable
fun UamiApp(tts: TextToSpeech?) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity()!! }
    val repository = remember { RecipeRepository(context.applicationContext) }
    val factory = remember { ViewModelFactory(repository, context.applicationContext) }
    val mainViewModel = remember(activity) {
        ViewModelProvider(activity, factory)[MainViewModel::class.java]
    }
    val reviewsViewModel = remember(activity) {
        ViewModelProvider(activity, factory)[ReviewsViewModel::class.java]
    }

    val currentLanguageState by mainViewModel.currentLanguage.collectAsState()
    val globalRecipesState by mainViewModel.globalRecipes.collectAsState()
    val isPreparingDataState by mainViewModel.isPreparingData.collectAsState()
    val isDownloadFailedState by mainViewModel.isDownloadFailed.collectAsState()
    val isUpdateCheckedState by mainViewModel.isUpdateChecked.collectAsState()
    val downloadProgressState by mainViewModel.downloadProgress.collectAsState()
    val downloadStatusState by mainViewModel.downloadStatus.collectAsState()
    val startupErrorMessageState by mainViewModel.startupErrorMessage.collectAsState()
    val favoritosState by mainViewModel.favoritos.collectAsState()
    val hasLocalCacheState by mainViewModel.hasLocalCache.collectAsState()

    val onSpeechFinished = remember { mutableStateOf<(() -> Unit)?>(null) }
    val currentLanguageLocal = remember { mutableStateOf(currentLanguageState) }
    
    // Sync local currentLanguageLocal with mainViewModel's currentLanguage
    LaunchedEffect(currentLanguageState) {
        if (currentLanguageLocal.value != currentLanguageState) {
            currentLanguageLocal.value = currentLanguageState
        }
    }
    LaunchedEffect(currentLanguageLocal.value) {
        if (currentLanguageLocal.value != currentLanguageState) {
            mainViewModel.setLanguage(currentLanguageLocal.value)
        }
    }

    // Sync favorites list with ViewModel state (View-only observation)
    val favoritosLocal = remember { mutableStateListOf<Int>() }
    LaunchedEffect(favoritosState) {
        if (favoritosLocal.toList() != favoritosState) {
            favoritosLocal.clear()
            favoritosLocal.addAll(favoritosState)
        }
    }

    val navController = rememberNavController()

    // Guardar el idioma cuando cambie
    LaunchedEffect(currentLanguageState) {
        if (currentLanguageState.isNotEmpty()) {
            tts?.language = if (currentLanguageState == "es") Locale.forLanguageTag("es-ES") else Locale.US
        }
    }

    // Lógica de Sincronización Modular
    LaunchedEffect(currentLanguageState, isPreparingDataState, isDownloadFailedState, hasLocalCacheState) {
        if (currentLanguageState.isNotEmpty()) {
            // Caso 1: Ya hay datos locales y NO hemos buscado actualizaciones aún
            if (hasLocalCacheState && !isPreparingDataState && !isDownloadFailedState && !isUpdateCheckedState) {
                Log.d("OFFLINE", "Cargado desde caché existente")
                if (isInternetAvailable(context)) {
                    mainViewModel.setPreparingData(true)
                } else {
                    mainViewModel.setUpdateChecked(true)
                }
            } 
            // Caso 2: Proceso de Sincronización Activo
            else if (isPreparingDataState) {
                val isFirstRun = !hasLocalCacheState
                mainViewModel.startSync(isFirstRun)
            }
        }
    }

    Scaffold(
        bottomBar = { 
            if (currentLanguageState.isNotEmpty() && hasLocalCacheState && !isPreparingDataState && !isDownloadFailedState) {
                CustomBottomBar(navController, currentLanguageLocal) 
            }
        },
        containerColor = Background
    ) { paddingValues ->
        when {
            currentLanguageState.isEmpty() -> {
                LanguageSelectionScreen(currentLanguageLocal, startupErrorMessageState) {
                    mainViewModel.setStartupErrorMessage("")
                    mainViewModel.setPreparingData(true)
                }
            }
            !hasLocalCacheState || isPreparingDataState || isDownloadFailedState -> {
                LaunchedEffect(Unit) {
                    if (!hasLocalCacheState && !isPreparingDataState && !isDownloadFailedState) {
                        mainViewModel.setPreparingData(true)
                    }
                }
                PreparingDataScreen(currentLanguageState, downloadProgressState, downloadStatusState, isDownloadFailedState, startupErrorMessageState) {
                    mainViewModel.setDownloadFailed(false)
                    mainViewModel.setStartupErrorMessage("")
                    mainViewModel.setPreparingData(true)
                }
            }
            else -> {
                UamiNavHost(paddingValues, navController, favoritosLocal, tts, onSpeechFinished, currentLanguageLocal, globalRecipesState, factory, reviewsViewModel)
            }
        }
    }
}

@Composable
fun CustomBottomBar(navController: NavHostController, currentLanguage: MutableState<String>) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "inicio"
    
    val currentTabRoot = when {
        currentRoute == "nutriologo" -> "nutriologo"
        currentRoute == "recetas" || currentRoute.startsWith("recetas") || currentRoute.startsWith("recipeDetail") || currentRoute.startsWith("cookingMode") || currentRoute == "supermercados" -> "recetas"
        else -> "inicio"
    }

    val selectedIndex = when (currentTabRoot) {
        "nutriologo" -> 2
        "recetas" -> 1
        else -> 0
    }

    // Anti-spam configuration (Debounce + launchSingleTop)
    var lastClickTime by remember { mutableStateOf(0L) }
    val safeNavigate = { targetRoute: String ->
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastClickTime > 300) {
            lastClickTime = currentTime
            if (currentTabRoot == targetRoute) {
                // Si ya estamos en el flujo de la pestaña destino, hacer pop hasta la raíz para limpiar pantallas hijas
                navController.popBackStack(targetRoute, inclusive = false)
            } else {
                // Si cambiamos de pestaña, ir al destino raíz y no restaurar estado de sub-pantallas para ir siempre al inicio
                navController.navigate(targetRoute) {
                    popUpTo(navController.graph.startDestinationId) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = false
                }
            }
        }
    }

    Surface(
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(32.dp)),
        shape = RoundedCornerShape(32.dp), // Fix corners shadow rendering
        color = Surface.copy(alpha = 0.92f), // Sleek semi-transparent glass
        tonalElevation = 10.dp,
        border = BorderStroke(1.dp, Primary.copy(alpha = 0.15f)) // Premium border stroke
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .fillMaxWidth()
        ) {
            val containerWidth = maxWidth
            val itemWidth = containerWidth / 3

            // Elastic sliding background pill
            val pillOffset by animateDpAsState(
                targetValue = itemWidth * selectedIndex,
                animationSpec = spring(
                    dampingRatio = 0.62f, // Bouncy feel
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "pillOffset"
            )

            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = pillOffset)
                    .width(itemWidth)
                    .height(44.dp)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Primary, Secondary) // Premium color sweep
                        )
                    )
            )

            Row(
                modifier = Modifier.fillMaxWidth().align(Alignment.Center),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    BottomNavItem(
                        icon = Icons.Rounded.Explore,
                        label = if (currentLanguage.value == "es") "Explorar" else "Explore",
                        selected = selectedIndex == 0,
                        onClick = { safeNavigate("inicio") }
                    )
                }
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    BottomNavItem(
                        icon = Icons.Rounded.RestaurantMenu,
                        label = if (currentLanguage.value == "es") "Recetas" else "Recipes",
                        selected = selectedIndex == 1,
                        onClick = { safeNavigate("recetas") }
                    )
                }
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    BottomNavItem(
                        icon = Icons.Rounded.Psychology,
                        label = if (currentLanguage.value == "es") "Nutriólogo" else "Nutrition",
                        selected = selectedIndex == 2,
                        onClick = { safeNavigate("nutriologo") }
                    )
                }
            }
        }
    }
}

@Composable
fun BottomNavItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    val contentColor by animateColorAsState(
        targetValue = if (selected) OnPrimary else TextMuted,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "contentColor"
    )
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.25f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMedium),
        label = "iconScale"
    )
    val iconRotation by animateFloatAsState(
        targetValue = if (selected) 360f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "iconRotation"
    )

    // Dynamic elastic jump offset that returns exactly to 0.0f
    val iconOffsetY = remember { Animatable(0f) }
    LaunchedEffect(selected) {
        if (selected) {
            // Jump up to -8dp
            iconOffsetY.animateTo(
                targetValue = -8f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
            )
            // Land back smoothly to 0f
            iconOffsetY.animateTo(
                targetValue = 0f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMedium)
            )
        }
    }

    BouncyPressEffect(squishFactor = 0.80f) { modifier, interactionSource ->
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(20.dp))
                .clickable(interactionSource = interactionSource, indication = null) { onClick() }
                .padding(horizontal = 8.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    icon, 
                    contentDescription = label, 
                    tint = contentColor, 
                    modifier = Modifier
                        .size(22.dp)
                        .offset(y = iconOffsetY.value.dp) // Offset is dynamic and settles perfectly back to 0
                        .graphicsLayer(scaleX = iconScale, scaleY = iconScale, rotationZ = iconRotation)
                )
                AnimatedVisibility(
                    visible = selected,
                    enter = fadeIn() + expandHorizontally(),
                    exit = fadeOut() + shrinkHorizontally()
                ) {
                    Text(
                        text = label,
                        modifier = Modifier.padding(start = 4.dp),
                        color = contentColor,
                        maxLines = 1,
                        softWrap = false,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.2.sp,
                            fontSize = 12.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun UamiNavHost(
    pv: PaddingValues,
    navController: NavHostController,
    favoritos: MutableList<Int>,
    tts: TextToSpeech?,
    onSpeechFinished: MutableState<(() -> Unit)?>,
    currentLanguage: MutableState<String>,
    preloadedRecipes: List<RecipeModel>,
    factory: ViewModelFactory,
    reviewsViewModel: ReviewsViewModel
) {
    val activity = LocalContext.current.findActivity()!!

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = pv.calculateBottomPadding())
    ) {
        NavHost(
            navController = navController,
            startDestination = "inicio",
            enterTransition = {
                fadeIn(animationSpec = tween(300)) + 
                scaleIn(initialScale = 0.94f, animationSpec = tween(300))
            },
            exitTransition = {
                fadeOut(animationSpec = tween(300)) + 
                scaleOut(targetScale = 1.06f, animationSpec = tween(300))
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(300)) + 
                scaleIn(initialScale = 1.06f, animationSpec = tween(300))
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(300)) + 
                scaleOut(targetScale = 0.94f, animationSpec = tween(300))
            }
        ) {
            composable("inicio") { 
                ScreenInicio(
                    navController = navController, 
                    currentLanguage = currentLanguage, 
                    preloadedRecipes = preloadedRecipes, 
                    favoritos = favoritos,
                    factory = factory
                ) 
            }
            composable("recetas") { ScreenRecipeMenu(navController, currentLanguage) }
            composable(
                "recetas_lista?searchQuery={searchQuery}&cuisine={cuisine}&mealType={mealType}&difficulty={difficulty}",
                arguments = listOf(
                    navArgument("searchQuery") { nullable = true; type = NavType.StringType },
                    navArgument("cuisine") { nullable = true; type = NavType.StringType },
                    navArgument("mealType") { nullable = true; type = NavType.StringType },
                    navArgument("difficulty") { nullable = true; type = NavType.StringType }
                )
            ) { backStackEntry ->
                val searchQuery = backStackEntry.arguments?.getString("searchQuery") ?: ""
                val cuisine = backStackEntry.arguments?.getString("cuisine") ?: "All"
                val mealType = backStackEntry.arguments?.getString("mealType") ?: "All"
                val difficulty = backStackEntry.arguments?.getString("difficulty") ?: "All"
                ScreenRecipes(
                    navController = navController,
                    favoritos = favoritos,
                    currentLanguage = currentLanguage,
                    preloadedRecipes = preloadedRecipes,
                    initialSearchQuery = searchQuery,
                    initialCuisine = cuisine,
                    initialMealType = mealType,
                    initialDifficulty = difficulty
                )
            }
            composable("recetas_favoritos") { ScreenFavorites(navController, favoritos, currentLanguage, preloadedRecipes) }
            composable("recipeDetail/{id}", arguments = listOf(
                navArgument("id") { type = NavType.IntType }
            )) {
                val id = it.arguments?.getInt("id") ?: 0
                ScreenRecipeDetail(navController, id, favoritos, currentLanguage, preloadedRecipes)
            }
            composable("cookingMode/{id}", arguments = listOf(
                navArgument("id") { type = NavType.IntType }
            )) {
                val id = it.arguments?.getInt("id") ?: 0
                ScreenCookingMode(navController, id, tts, onSpeechFinished, currentLanguage, preloadedRecipes)
            }
            composable("nutriologo") {
                ScreenNutritionist(
                    currentLanguage = currentLanguage,
                    allRecipes = preloadedRecipes,
                    favoriteIds = favoritos,
                    onNavigateToRecipe = { recipeId ->
                        navController.navigate("recipeDetail/$recipeId")
                    },
                    reviewsViewModel = reviewsViewModel,
                    onNavigateToProfile = {
                        navController.navigate("perfil")
                    }
                )
            }
            composable("supermercados") {
                ScreenSupermarkets(navController, currentLanguage)
            }
            composable("opiniones") {
                ScreenOpiniones(
                    onBack = { navController.popBackStack() },
                    onNavigateToLogin = { navController.navigate("perfil") },
                    currentLanguage = currentLanguage,
                    reviewsViewModel = reviewsViewModel
                )
            }
            composable("perfil") {
                ScreenPerfil(
                    onBack = { navController.popBackStack() },
                    currentLanguage = currentLanguage,
                    reviewsViewModel = reviewsViewModel,
                    favoritosSize = favoritos.size
                )
            }
        }
    }
}

