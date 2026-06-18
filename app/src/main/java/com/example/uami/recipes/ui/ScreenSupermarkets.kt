package com.example.uami.recipes.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import com.example.uami.ui.theme.Background
import com.example.uami.ui.theme.Primary
import com.example.uami.ui.theme.Secondary
import com.example.uami.ui.theme.Surface
import com.example.uami.ui.theme.TextMuted
import com.example.uami.ui.theme.OnBackground
import com.example.uami.ui.theme.OnSurface
import com.example.uami.ui.theme.OnPrimary
import com.example.uami.utils.BouncyPressEffect
import com.example.uami.utils.pulseAnimation
import com.example.uami.utils.shimmerGlow
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch
import kotlin.math.*

data class Supermarket(
    val id: Int,
    val name: String,
    val address: String,
    val location: LatLng,
    val type: String,
    val rating: Float
)

val ArequipaSupermarkets = listOf(
    Supermarket(
        1,
        "Plaza Vea La Marina",
        "Av. La Marina 300, Yanahuara / Cercado, Arequipa",
        LatLng(-16.4061, -71.5414),
        "Supermercado",
        4.3f
    ),
    Supermarket(
        2,
        "Metro Lambramani",
        "Centro Comercial Parque Lambramani, Av. Lambramani, Arequipa",
        LatLng(-16.4172, -71.5273),
        "Supermercado",
        4.2f
    ),
    Supermarket(
        3,
        "Tottus Mall Aventura",
        "Av. Porongoche 500, Paucarpata, Arequipa",
        LatLng(-16.4223, -71.5165),
        "Supermercado",
        4.4f
    ),
    Supermarket(
        4,
        "Franco Supermercados Emmel",
        "Av. Emmel 113, Yanahuara, Arequipa",
        LatLng(-16.3905, -71.5432),
        "Supermercado Premium",
        4.5f
    ),
    Supermarket(
        5,
        "Franco Supermercados Dolores",
        "Av. Dolores s/n, José Luis Bustamante y Rivero, Arequipa",
        LatLng(-16.4251, -71.5255),
        "Supermercado Premium",
        4.5f
    ),
    Supermarket(
        6,
        "Mercado San Camilo",
        "Calle San Camilo, Centro Histórico, Arequipa",
        LatLng(-16.4038, -71.5362),
        "Mercado Tradicional",
        4.6f
    ),
    Supermarket(
        7,
        "Tiendas Mass Yanahuara",
        "Av. Ejército 415, Yanahuara, Arequipa",
        LatLng(-16.3912, -71.5419),
        "Tienda de Conveniencia",
        4.1f
    ),
    Supermarket(
        8,
        "Tiendas Mass Umacollo",
        "Calle Garcilaso de la Vega 204, Umacollo, Arequipa",
        LatLng(-16.3995, -71.5453),
        "Tienda de Conveniencia",
        4.0f
    ),
    Supermarket(
        9,
        "Tiendas Mass Cayma",
        "Av. Cayma 512, Cayma, Arequipa",
        LatLng(-16.3768, -71.5428),
        "Tienda de Conveniencia",
        4.2f
    ),
    Supermarket(
        10,
        "Tambo Cayma",
        "Av. Ejército 702, Cayma, Arequipa",
        LatLng(-16.3792, -71.5434),
        "Tienda de Conveniencia",
        4.3f
    ),
    Supermarket(
        11,
        "Tambo Av. Venezuela",
        "Av. Venezuela 810, Cercado, Arequipa",
        LatLng(-16.4069, -71.5244),
        "Tienda de Conveniencia",
        4.1f
    )
)

enum class StoreFilter {
    ALL, SUPERMARKET, TRADITIONAL, CONVENIENCE
}

// Distance helper using Haversine formula
fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371.0 // Earth radius in km
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return r * c
}

// Draw a custom glowing colored marker programmatically
fun createCustomMarkerBitmap(type: String, isSelected: Boolean): BitmapDescriptor {
    val size = if (isSelected) 100 else 75
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    
    val paint = Paint().apply {
        isAntiAlias = true
    }
    
    // Choose theme color based on store type
    val color = when {
        type.contains("Premium") -> 0xFFF43F5E.toInt() // Coral Secondary
        type.contains("Tradicional") -> 0xFF10B981.toInt() // Emerald Green
        type.contains("Conveniencia") -> 0xFF8B5CF6.toInt() // Violet Purple
        else -> 0xFFF59E0B.toInt() // Amber Primary
    }
    
    // 1. Semi-transparent outer glowing ring (only when selected or active)
    val alphaGlow = if (isSelected) 0x55000000 else 0x18000000
    val glowColor = (color and 0x00FFFFFF) or alphaGlow
    paint.color = glowColor
    canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
    
    // 2. White outer border ring
    paint.color = 0xFFFFFFFF.toInt()
    canvas.drawCircle(size / 2f, size / 2f, size / 3.0f, paint)
    
    // 3. Inner colored core circle
    paint.color = color
    canvas.drawCircle(size / 2f, size / 2f, size / 3.5f, paint)
    
    // 4. Tiny central core dot
    paint.color = 0xFFFFFFFF.toInt()
    canvas.drawCircle(size / 2f, size / 2f, size / 7f, paint)
    
    return BitmapDescriptorFactory.fromBitmap(bitmap)
}

@Composable
fun SupermarketMarker(
    supermarket: Supermarket,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val markerState = rememberMarkerState(position = supermarket.location)
    val customIcon = remember(supermarket.type, isSelected) {
        createCustomMarkerBitmap(supermarket.type, isSelected)
    }
    
    Marker(
        state = markerState,
        title = supermarket.name,
        icon = customIcon,
        snippet = supermarket.type,
        onClick = {
            onClick()
            true
        }
    )
}

@Composable
fun ScreenSupermarkets(
    navController: NavHostController,
    currentLanguage: MutableState<String>
) {
    val isEs = currentLanguage.value == "es"
    val context = LocalContext.current

    val isUserLoggedIn = remember { com.example.uami.utils.AuthManager.isUserLoggedIn() }

    if (!isUserLoggedIn) {
        Scaffold(
            topBar = {
                Column(modifier = Modifier.background(Background).statusBarsPadding().padding(top = 16.dp, bottom = 8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        BouncyPressEffect(squishFactor = 0.72f) { modifier, interactionSource ->
                            IconButton(
                                onClick = { navController.popBackStack() },
                                interactionSource = interactionSource,
                                modifier = modifier
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = null,
                                    tint = OnBackground
                                )
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (isEs) "Buscar Tiendas" else "Search Stores",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                            color = OnBackground
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
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = if (isEs) {
                                "Para buscar tiendas, mercados y supermercados cercanos en Arequipa, necesitas estar registrado e iniciar sesión."
                            } else {
                                "To search for nearby stores, markets, and supermarkets in Arequipa, you need to be registered and signed in."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(Modifier.height(24.dp))

                        BouncyPressEffect { modifier, interactionSource ->
                            Button(
                                onClick = { navController.navigate("perfil") },
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
        return
    }

    val coroutineScope = rememberCoroutineScope()

    // Default reference position: Arequipa Plaza de Armas
    val arequipaPlaza = LatLng(-16.3988, -71.5369)

    // Location permission state
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // Camera position state
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(arequipaPlaza, 14.5f)
    }

    // Map UI and properties
    val uiSettings = remember {
        MapUiSettings(
            myLocationButtonEnabled = true,
            zoomControlsEnabled = false,
            mapToolbarEnabled = false
        )
    }

    val properties = remember(hasLocationPermission) {
        MapProperties(
            isMyLocationEnabled = hasLocationPermission,
            mapStyleOptions = MapStyleOptions(DARK_MAP_STYLE)
        )
    }

    var selectedFilter by remember { mutableStateOf(StoreFilter.ALL) }
    var selectedSupermarket by remember { mutableStateOf<Supermarket?>(null) }
    val lazyListState = rememberLazyListState()

    // Filter Arequipa Supermarkets list based on selected filter chip
    val filteredSupermarkets = remember(selectedFilter) {
        ArequipaSupermarkets.filter { store ->
            when (selectedFilter) {
                StoreFilter.ALL -> true
                StoreFilter.SUPERMARKET -> store.type.contains("Supermercado")
                StoreFilter.TRADITIONAL -> store.type.contains("Tradicional")
                StoreFilter.CONVENIENCE -> store.type.contains("Conveniencia")
            }
        }
    }

    // Automatically scroll bottom list when a marker is clicked
    LaunchedEffect(selectedSupermarket, filteredSupermarkets) {
        selectedSupermarket?.let { supermarket ->
            val index = filteredSupermarkets.indexOfFirst { it.id == supermarket.id }
            if (index != -1) {
                lazyListState.animateScrollToItem(index)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // 1. Google Map View
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = properties,
            uiSettings = uiSettings,
            onMapClick = {
                selectedSupermarket = null
            }
        ) {
            filteredSupermarkets.forEach { supermarket ->
                val isSelected = selectedSupermarket?.id == supermarket.id
                SupermarketMarker(
                    supermarket = supermarket,
                    isSelected = isSelected,
                    onClick = {
                        selectedSupermarket = supermarket
                        coroutineScope.launch {
                            cameraPositionState.animate(
                                CameraUpdateFactory.newLatLngZoom(supermarket.location, 16f)
                            )
                        }
                    }
                )
            }
        }

        // 2. Custom Floating Header Panel
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Background.copy(alpha = 0.95f),
                            Background.copy(alpha = 0.8f),
                            Color.Transparent
                        )
                    )
                )
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        color = Surface.copy(alpha = 0.85f),
                        shape = CircleShape,
                        border = BorderStroke(1.dp, Primary.copy(alpha = 0.15f)),
                        modifier = Modifier.size(44.dp)
                    ) {
                        BouncyPressEffect(squishFactor = 0.72f) { modifier, interactionSource ->
                            IconButton(
                                onClick = { navController.popBackStack() },
                                interactionSource = interactionSource,
                                modifier = modifier
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = if (isEs) "Atrás" else "Back",
                                    tint = OnBackground
                                )
                            }
                        }
                    }

                    Spacer(Modifier.width(16.dp))

                    Column {
                        Text(
                            text = if (isEs) "Tiendas e Ingredientes" else "Stores & Ingredients",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 21.sp
                            ),
                            color = OnBackground
                        )
                        Text(
                            text = if (isEs) "Explora tiendas de abarrotes en Arequipa" else "Explore grocery stores in Arequipa",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Horizontal Filter Chips Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StoreFilter.values().forEach { filter ->
                        val isSelected = selectedFilter == filter
                        val label = when (filter) {
                            StoreFilter.ALL -> if (isEs) "Todos" else "All"
                            StoreFilter.SUPERMARKET -> if (isEs) "Supermercados" else "Supermarkets"
                            StoreFilter.TRADITIONAL -> if (isEs) "Mercados" else "Markets"
                            StoreFilter.CONVENIENCE -> if (isEs) "Conveniencia" else "Convenience"
                        }
                        
                        val chipColor = when (filter) {
                            StoreFilter.SUPERMARKET -> Secondary
                            StoreFilter.TRADITIONAL -> Color(0xFF10B981) // Emerald Green
                            StoreFilter.CONVENIENCE -> Color(0xFF8B5CF6) // Violet Purple
                            else -> Primary
                        }

                        BouncyPressEffect(squishFactor = 0.88f) { modifier, interactionSource ->
                            Surface(
                                modifier = modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable(interactionSource = interactionSource, indication = null) {
                                        selectedFilter = filter
                                        selectedSupermarket = null
                                    },
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) chipColor else Surface.copy(alpha = 0.8f),
                                border = BorderStroke(1.dp, if (isSelected) chipColor else Surface.copy(alpha = 0.4f)),
                                tonalElevation = 2.dp
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) Color.White else OnSurface,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Bottom horizontal cards carousel overlay
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 20.dp)
        ) {
            if (filteredSupermarkets.isNotEmpty()) {
                LazyRow(
                    state = lazyListState,
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    itemsIndexed(filteredSupermarkets, key = { _, item -> item.id }) { _, supermarket ->
                        val isSelected = selectedSupermarket?.id == supermarket.id
                        
                        // Compute distance to Plaza de Armas as default or center reference
                        val dist = calculateDistance(
                            arequipaPlaza.latitude, arequipaPlaza.longitude,
                            supermarket.location.latitude, supermarket.location.longitude
                        )

                        SupermarketCard(
                            supermarket = supermarket,
                            isSelected = isSelected,
                            isEs = isEs,
                            distanceKm = dist,
                            onClick = {
                                selectedSupermarket = supermarket
                                coroutineScope.launch {
                                    cameraPositionState.animate(
                                        CameraUpdateFactory.newLatLngZoom(supermarket.location, 16.5f)
                                    )
                                }
                            },
                            onNavigate = {
                                try {
                                    val intentUri = Uri.parse("google.navigation:q=${supermarket.location.latitude},${supermarket.location.longitude}")
                                    val mapIntent = Intent(Intent.ACTION_VIEW, intentUri)
                                    mapIntent.setPackage("com.google.android.apps.maps")
                                    context.startActivity(mapIntent)
                                } catch (e: Exception) {
                                    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=${supermarket.location.latitude},${supermarket.location.longitude}"))
                                    context.startActivity(webIntent)
                                }
                            }
                        )
                    }
                }
            } else {
                // Empty state card
                Surface(
                    color = Surface.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                ) {
                    Box(
                        modifier = Modifier.padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isEs) "No se encontraron tiendas para este filtro" else "No stores found for this filter",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SupermarketCard(
    supermarket: Supermarket,
    isSelected: Boolean,
    isEs: Boolean,
    distanceKm: Double,
    onClick: () -> Unit,
    onNavigate: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "cardScale"
    )

    // Store type visual descriptors
    val typeColor = when {
        supermarket.type.contains("Premium") -> Secondary
        supermarket.type.contains("Tradicional") -> Color(0xFF10B981) // Emerald Green
        supermarket.type.contains("Conveniencia") -> Color(0xFF8B5CF6) // Violet Purple
        else -> Primary
    }

    val typeIcon = when {
        supermarket.type.contains("Premium") -> Icons.Rounded.LocalMall
        supermarket.type.contains("Tradicional") -> Icons.Rounded.Storefront
        supermarket.type.contains("Conveniencia") -> Icons.Rounded.Store
        else -> Icons.Rounded.ShoppingCart
    }

    Surface(
        modifier = Modifier
            .width(290.dp)
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clip(RoundedCornerShape(24.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.foundation.LocalIndication.current
            ) { onClick() }
            .then(
                if (isSelected) Modifier.shimmerGlow(durationMillis = 2200, glowColor = typeColor.copy(alpha = 0.22f))
                else Modifier
            ),
        shape = RoundedCornerShape(24.dp),
        color = Surface.copy(alpha = 0.88f),
        tonalElevation = 6.dp,
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) typeColor else Surface.copy(alpha = 0.6f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(typeColor.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = typeIcon,
                            contentDescription = null,
                            tint = typeColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        color = typeColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = supermarket.type.uppercase(),
                            color = typeColor,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Star,
                        contentDescription = "Rating",
                        tint = Primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text = supermarket.rating.toString(),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = OnSurface
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = supermarket.name,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp),
                color = OnSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Place,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = supermarket.address,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Distance Tag
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Radar,
                        contentDescription = null,
                        tint = typeColor,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = if (isEs) {
                            "A ${String.format("%.1f", distanceKm)} km del centro"
                        } else {
                            "${String.format("%.1f", distanceKm)} km from center"
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp
                        ),
                        color = TextMuted
                    )
                }

                // Hours Tag
                val schedule = when {
                    supermarket.name.contains("Tambo") -> if (isEs) "Abierto 24h" else "Open 24h"
                    supermarket.name.contains("Mass") -> if (isEs) "8:00 AM - 10:00 PM" else "8:00 AM - 10:00 PM"
                    supermarket.name.contains("Sancamilo") || supermarket.name.contains("Camilo") -> if (isEs) "6:00 AM - 6:00 PM" else "6:00 AM - 6:00 PM"
                    else -> if (isEs) "8:00 AM - 10:00 PM" else "8:00 AM - 10:00 PM"
                }
                
                Surface(
                    color = Surface.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = schedule,
                        color = TextMuted,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Directions Button
            BouncyPressEffect(squishFactor = 0.85f) { modifier, buttonInteractionSource ->
                Button(
                    onClick = onNavigate,
                    colors = ButtonDefaults.buttonColors(containerColor = typeColor),
                    shape = RoundedCornerShape(12.dp),
                    interactionSource = buttonInteractionSource,
                    modifier = modifier.fillMaxWidth().height(38.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Directions,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (isEs) "CÓMO LLEGAR" else "GET DIRECTIONS",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

private const val DARK_MAP_STYLE = """
[
  {
    "elementType": "geometry",
    "stylers": [
      {
        "color": "#0f172a"
      }
    ]
  },
  {
    "elementType": "labels.text.fill",
    "stylers": [
      {
        "color": "#94a3b8"
      }
    ]
  },
  {
    "elementType": "labels.text.stroke",
    "stylers": [
      {
        "color": "#0f172a"
      }
    ]
  },
  {
    "featureType": "administrative",
    "elementType": "geometry",
    "stylers": [
      {
        "color": "#334155"
      }
    ]
  },
  {
    "featureType": "poi",
    "elementType": "labels.text.fill",
    "stylers": [
      {
        "color": "#94a3b8"
      }
    ]
  },
  {
    "featureType": "poi.park",
    "elementType": "geometry",
    "stylers": [
      {
        "color": "#1e293b"
      }
    ]
  },
  {
    "featureType": "road",
    "elementType": "geometry.fill",
    "stylers": [
      {
        "color": "#1e293b"
      }
    ]
  },
  {
    "featureType": "road",
    "elementType": "labels.text.fill",
    "stylers": [
      {
        "color": "#64748b"
      }
    ]
  },
  {
    "featureType": "road.highway",
    "elementType": "geometry",
    "stylers": [
      {
        "color": "#334155"
      }
    ]
  },
  {
    "featureType": "water",
    "elementType": "geometry",
    "stylers": [
      {
        "color": "#020617"
      }
    ]
  }
]
"""
