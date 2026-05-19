package com.example.lab09

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.lab09.ejercicio1.remote.RecipeApiService
import com.example.lab09.ejercicio1.ui.ScreenRecipeDetail
import com.example.lab09.ejercicio1.ui.ScreenRecipes
import com.example.lab09.ejercicio1.ui.ScreenRecipeMenu
import com.example.lab09.ejercicio1.ui.ScreenFavorites
import com.example.lab09.remote.PostApiService
import com.example.lab09.ui.theme.*
import com.example.lab09.utils.FavoriteManager
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

import android.speech.tts.TextToSpeech
import java.util.Locale

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Inicializar Utilidades
        com.example.lab09.utils.LanguageManager.init(this)
        com.example.lab09.utils.OnDeviceTranslator.init(this)
        
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
            val localeES = Locale("es", "ES")
            tts?.language = localeES
            
            // Intentar buscar una voz de alta calidad (network voice)
            try {
                val voices = tts?.voices
                val bestVoice = voices?.find { 
                    it.locale.language == "es" && it.name.contains("network", true) 
                } ?: voices?.find { it.locale.language == "es" }
                
                bestVoice?.let { tts?.voice = it }
            } catch (e: Exception) {
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
        mutableStateOf(com.example.lab09.utils.LanguageManager.getLanguage()) 
    }
    
    // Guardar el idioma cuando cambie
    LaunchedEffect(currentLanguage.value) {
        com.example.lab09.utils.LanguageManager.setLanguage(currentLanguage.value)
        val locale = if (currentLanguage.value == "es") Locale("es", "ES") else Locale.US
        tts?.language = locale
        // ... rest of voice logic
    }

    val urlBasePosts = "https://json-placeholder.mock.beeceptor.com/"
    val retrofitPosts = Retrofit.Builder().baseUrl(urlBasePosts)
        .addConverterFactory(GsonConverterFactory.create()).build()
    val servicioPosts = retrofitPosts.create(PostApiService::class.java)

    val urlBaseRecipes = "https://dummyjson.com/"
    val retrofitRecipes = Retrofit.Builder().baseUrl(urlBaseRecipes)
        .addConverterFactory(GsonConverterFactory.create()).build()
    val servicioRecipes = retrofitRecipes.create(RecipeApiService::class.java)

    // Pre-traducción en segundo plano para evitar esperas
    LaunchedEffect(Unit) {
        if (currentLanguage.value == "es") {
            try {
                val response = servicioRecipes.getRecipes(limit = 10, skip = 0)
                response.recipes?.forEach { recipe ->
                    com.example.lab09.utils.translateRecipeAsync(recipe, "es")
                }
            } catch (e: Exception) {}
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
        bottomBar = { CustomBottomBar(navController) },
        topBar = { LanguageToggle(currentLanguage) },
        containerColor = Background
    ) { paddingValues ->
        Contenido(paddingValues, navController, servicioPosts, servicioRecipes, favoritos, tts, onSpeechFinished, currentLanguage)
    }
}

@Composable
fun LanguageToggle(currentLanguage: MutableState<String>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = Surface,
            shape = RoundedCornerShape(12.dp),
            onClick = { 
                currentLanguage.value = if (currentLanguage.value == "es") "en" else "es" 
            }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.Language, 
                    null, 
                    tint = Primary, 
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (currentLanguage.value == "es") "ESPAÑOL" else "ENGLISH",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = OnSurface
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
                icon = Icons.Rounded.Favorite,
                label = "Feed",
                selected = currentRoute == "posts",
                onClick = { navController.navigate("posts") }
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
    servicioPosts: PostApiService,
    servicioRecipes: RecipeApiService,
    favoritos: MutableList<Int>,
    tts: android.speech.tts.TextToSpeech?,
    onSpeechFinished: MutableState<(() -> Unit)?>,
    currentLanguage: MutableState<String>
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = pv.calculateBottomPadding())
    ) {
        NavHost(
            navController = navController,
            startDestination = "inicio"
        ) {
            composable("inicio") { ScreenInicio() }
            composable("posts") { ScreenPosts(navController, servicioPosts, currentLanguage) }
            composable("postsVer/{id}", arguments = listOf(
                navArgument("id") { type = NavType.IntType }
            )) {
                val id = it.arguments?.getInt("id") ?: 0
                ScreenPost(navController, servicioPosts, id, currentLanguage)
            }
            composable("recetas") { ScreenRecipeMenu(navController, currentLanguage) }
            composable("recetas_lista") { ScreenRecipes(navController, servicioRecipes, favoritos, currentLanguage) }
            composable("recetas_favoritos") { ScreenFavorites(navController, servicioRecipes, favoritos, currentLanguage) }
            composable("recipeDetail/{id}", arguments = listOf(
                navArgument("id") { type = NavType.IntType }
            )) {
                val id = it.arguments?.getInt("id") ?: 0
                ScreenRecipeDetail(navController, servicioRecipes, id, favoritos, currentLanguage)
            }
            composable("cookingMode/{id}", arguments = listOf(
                navArgument("id") { type = NavType.IntType }
            )) {
                val id = it.arguments?.getInt("id") ?: 0
                com.example.lab09.ejercicio1.ui.ScreenCookingMode(navController, servicioRecipes, id, tts, onSpeechFinished, currentLanguage)
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
            border = androidx.compose.foundation.BorderStroke(1.dp, Primary.copy(alpha = 0.2f))
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
