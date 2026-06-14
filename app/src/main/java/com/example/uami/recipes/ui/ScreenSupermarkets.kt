package com.example.uami.recipes.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
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
import com.example.uami.utils.shimmerGlow
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch

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

@Composable
fun ScreenSupermarkets(
    navController: NavHostController,
    currentLanguage: MutableState<String>
) {
    val isEs = currentLanguage.value == "es"
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

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
    val arequipaCenter = LatLng(-16.409047, -71.537451)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(arequipaCenter, 14f)
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

    var selectedSupermarket by remember { mutableStateOf<Supermarket?>(null) }
    val lazyListState = rememberLazyListState()

    // Automatically scroll bottom list when a marker is clicked
    LaunchedEffect(selectedSupermarket) {
        selectedSupermarket?.let { supermarket ->
            val index = ArequipaSupermarkets.indexOfFirst { it.id == supermarket.id }
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
            ArequipaSupermarkets.forEach { supermarket ->
                val isSelected = selectedSupermarket?.id == supermarket.id
                Marker(
                    state = rememberMarkerState(position = supermarket.location),
                    title = supermarket.name,
                    snippet = supermarket.type,
                    onClick = {
                        selectedSupermarket = supermarket
                        coroutineScope.launch {
                            cameraPositionState.animate(
                                CameraUpdateFactory.newLatLngZoom(supermarket.location, 16f)
                            )
                        }
                        true
                    }
                )
            }
        }

        // 2. Custom header overlay
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Background.copy(alpha = 0.9f),
                            Background.copy(alpha = 0.5f),
                            Color.Transparent
                        )
                    )
                )
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    color = Surface.copy(alpha = 0.8f),
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
                            fontSize = 22.sp
                        ),
                        color = OnBackground
                    )
                    Text(
                        text = if (isEs) "Encuentra supermercados en Arequipa" else "Find supermarkets in Arequipa",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            }
        }

        // 3. Horizontal Bottom Slider for Supermarkets
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
        ) {
            LazyRow(
                state = lazyListState,
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                itemsIndexed(ArequipaSupermarkets) { _, supermarket ->
                    val isSelected = selectedSupermarket?.id == supermarket.id
                    SupermarketCard(
                        supermarket = supermarket,
                        isSelected = isSelected,
                        isEs = isEs,
                        onClick = {
                            selectedSupermarket = supermarket
                            coroutineScope.launch {
                                cameraPositionState.animate(
                                    CameraUpdateFactory.newLatLngZoom(supermarket.location, 16f)
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
        }
    }
}

@Composable
fun SupermarketCard(
    supermarket: Supermarket,
    isSelected: Boolean,
    isEs: Boolean,
    onClick: () -> Unit,
    onNavigate: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "cardScale"
    )

    Surface(
        modifier = Modifier
            .width(280.dp)
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clip(RoundedCornerShape(24.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.foundation.LocalIndication.current
            ) { onClick() }
            .then(
                if (isSelected) Modifier.shimmerGlow(durationMillis = 2500, glowColor = Primary.copy(alpha = 0.2f))
                else Modifier
            ),
        color = Surface.copy(alpha = 0.85f),
        tonalElevation = 6.dp,
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) Primary else Surface.copy(alpha = 0.8f)
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
                Surface(
                    color = if (supermarket.type.contains("Premium")) Secondary.copy(alpha = 0.2f) else Primary.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = supermarket.type.uppercase(),
                        color = if (supermarket.type.contains("Premium")) Secondary else Primary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Star,
                        contentDescription = "Rating",
                        tint = Primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
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
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = OnSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = supermarket.address,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.height(32.dp)
            )

            Spacer(Modifier.height(12.dp))

            BouncyPressEffect(squishFactor = 0.85f) { modifier, buttonInteractionSource ->
                Button(
                    onClick = onNavigate,
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    shape = RoundedCornerShape(14.dp),
                    interactionSource = buttonInteractionSource,
                    modifier = modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Directions,
                            contentDescription = null,
                            tint = OnPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (isEs) "CÓMO LLEGAR" else "GET DIRECTIONS",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = OnPrimary
                        )
                    }
                }
            }
        }
    }
}
