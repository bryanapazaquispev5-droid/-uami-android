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
import com.example.uami.recipes.remote.RecipeApiService
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
fun ProgPrincipal9(tts: TextToSpeech?) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity()!! }
    val repository = remember { RecipeRepository(context.applicationContext) }
    val factory = remember { ViewModelFactory(repository, context.applicationContext) }
    val mainViewModel = remember(activity) {
        ViewModelProvider(activity, factory)[MainViewModel::class.java]
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

    // Sync favorites
    val favoritosLocal = remember { mutableStateListOf<Int>() }
    LaunchedEffect(favoritosState) {
        if (favoritosLocal.toList() != favoritosState) {
            favoritosLocal.clear()
            favoritosLocal.addAll(favoritosState)
        }
    }
    LaunchedEffect(favoritosLocal.toList()) {
        if (favoritosLocal.toList() != favoritosState) {
            mainViewModel.setFavorites(favoritosLocal.toList())
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
    LaunchedEffect(currentLanguageState, isPreparingDataState, isDownloadFailedState) {
        if (currentLanguageState.isNotEmpty()) {
            // Caso 1: Ya hay datos locales y NO hemos buscado actualizaciones aún
            if (repository.hasCache() && !isPreparingDataState && !isDownloadFailedState && !isUpdateCheckedState) {
                Log.d("OFFLINE", "Cargado desde caché existente")
                if (isInternetAvailable(context)) {
                    mainViewModel.setPreparingData(true)
                } else {
                    mainViewModel.setUpdateChecked(true)
                }
            } 
            // Caso 2: Proceso de Sincronización Activo
            else if (isPreparingDataState) {
                val isFirstRun = !repository.hasCache()
                mainViewModel.startSync(isFirstRun)
            }
        }
    }

    Scaffold(
        bottomBar = { 
            if (currentLanguageState.isNotEmpty() && repository.hasCache() && !isPreparingDataState && !isDownloadFailedState) {
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
            !repository.hasCache() || isPreparingDataState || isDownloadFailedState -> {
                LaunchedEffect(Unit) {
                    if (!repository.hasCache() && !isPreparingDataState && !isDownloadFailedState) {
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
                Contenido(paddingValues, navController, repository.servicioRecipes, favoritosLocal, tts, onSpeechFinished, currentLanguageLocal, globalRecipesState, factory)
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
fun Contenido(
    pv: PaddingValues,
    navController: NavHostController,
    servicioRecipes: RecipeApiService,
    favoritos: MutableList<Int>,
    tts: TextToSpeech?,
    onSpeechFinished: MutableState<(() -> Unit)?>,
    currentLanguage: MutableState<String>,
    preloadedRecipes: List<RecipeModel>,
    factory: ViewModelFactory
) {
    val activity = LocalContext.current.findActivity()!!
    val reviewsViewModel: ReviewsViewModel = viewModel(viewModelStoreOwner = activity, factory = factory)

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
                    servicio = servicioRecipes,
                    favoritos = favoritos,
                    currentLanguage = currentLanguage,
                    preloadedRecipes = preloadedRecipes,
                    initialSearchQuery = searchQuery,
                    initialCuisine = cuisine,
                    initialMealType = mealType,
                    initialDifficulty = difficulty
                )
            }
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
            composable("nutriologo") {
                ScreenNutritionist(
                    currentLanguage = currentLanguage,
                    allRecipes = preloadedRecipes,
                    favoriteIds = favoritos,
                    onNavigateToRecipe = { recipeId ->
                        navController.navigate("recipeDetail/$recipeId")
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
    val isEs = currentLanguage.value == "es"
    val scrollState = rememberScrollState()

    // 1. Saludo por hora del día
    val greeting = remember {
        val calendar = java.util.Calendar.getInstance()
        val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
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
            val calendar = java.util.Calendar.getInstance()
            val dayOfYear = calendar.get(java.util.Calendar.DAY_OF_YEAR)
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
                                    if (isFav) {
                                        recetaDelDia.id?.let { favoritos.remove(it) }
                                    } else {
                                        favoritos.add(recetaDelDia.id ?: 0)
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
                            if (favoritos.contains(recipe.id)) {
                                recipe.id?.let { favoritos.remove(it) }
                            } else {
                                favoritos.add(recipe.id ?: 0)
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
                StatItem("$totalRecetas", if (isEs) "Recetas" else "Recipes")
                StatItem("$avgTime min", if (isEs) "Tiempo Prom." else "Avg. Time")
                StatItem("$totalFavs", if (isEs) "Favoritos" else "Favorites")
            }
        }

        Spacer(Modifier.height(100.dp)) // Padding for bottom bar
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
