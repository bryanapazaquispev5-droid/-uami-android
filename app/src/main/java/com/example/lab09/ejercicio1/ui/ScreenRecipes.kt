package com.example.lab09.ejercicio1.ui

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.example.lab09.ejercicio1.models.RecipeModel
import com.example.lab09.ejercicio1.remote.RecipeApiService
import com.example.lab09.ui.theme.*

@Composable
fun ScreenRecipeMenu(navController: NavHostController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Rounded.Restaurant,
            contentDescription = null,
            tint = Primary,
            modifier = Modifier.size(80.dp)
        )
        Spacer(Modifier.height(24.dp))
        Text(
            "Libro de Cocina",
            style = MaterialTheme.typography.headlineMedium,
            color = OnBackground
        )
        Text(
            "Explora cientos de recetas profesionales",
            style = MaterialTheme.typography.bodyLarge,
            color = TextMuted,
            textAlign = TextAlign.Center
        )
        
        Spacer(Modifier.height(48.dp))
        
        Button(
            onClick = { navController.navigate("recetas_lista") },
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Primary),
            shape = RoundedCornerShape(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.MenuBook, contentDescription = null, tint = OnPrimary)
                Spacer(Modifier.width(12.dp))
                Text("VER TODAS LAS RECETAS", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = OnPrimary)
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        OutlinedButton(
            onClick = { navController.navigate("recetas_favoritos") },
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Primary.copy(alpha = 0.3f))
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Favorite, contentDescription = null, tint = Primary)
                Spacer(Modifier.width(12.dp))
                Text("MIS FAVORITOS", color = Primary)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenRecipes(navController: NavHostController, servicio: RecipeApiService, favoritos: MutableList<Int>) {
    var listaRecipes by remember { mutableStateOf<List<RecipeModel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    var currentPage by remember { mutableIntStateOf(1) }
    val limit = 8
    var totalRecipes by remember { mutableIntStateOf(0) }

    LaunchedEffect(currentPage, isRefreshing) {
        isLoading = true
        try {
            val skip = (currentPage - 1) * limit
            val response = servicio.getRecipes(limit, skip)
            listaRecipes = response.recipes ?: emptyList()
            totalRecipes = response.total ?: 0
        } catch (e: Exception) {
            Log.e("RECIPES_UI", "Error: ${e.message}")
        } finally {
            isLoading = false
            isRefreshing = false
        }
    }

    val totalPages = if (totalRecipes > 0) (totalRecipes + limit - 1) / limit else 1

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(Background).padding(top = 24.dp, start = 8.dp, end = 24.dp, bottom = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Rounded.ArrowBack, null, tint = OnBackground)
                    }
                    Text("Recetas", style = MaterialTheme.typography.headlineMedium, color = OnBackground)
                }
                Text("    Inspiración para tu cocina", style = MaterialTheme.typography.bodyLarge, color = TextMuted, modifier = Modifier.padding(start = 40.dp))
            }
        },
        bottomBar = {
            PaginationControls(
                currentPage = currentPage,
                totalPages = totalPages,
                onPageChange = { currentPage = it }
            )
        },
        containerColor = Background
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { isRefreshing = true },
            modifier = Modifier.padding(padding).fillMaxSize()
        ) {
            if (isLoading && !isRefreshing) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Primary)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(listaRecipes, key = { it.id ?: 0 }) { recipe ->
                        RecipeCardPremium(
                            recipe = recipe,
                            isFav = favoritos.contains(recipe.id),
                            onFavToggle = {
                                if (favoritos.contains(recipe.id)) favoritos.remove(recipe.id)
                                else favoritos.add(recipe.id ?: 0)
                            },
                            onClick = { navController.navigate("recipeDetail/${recipe.id ?: 0}") }
                        )
                    }
                    item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                        Spacer(Modifier.height(80.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ScreenFavorites(navController: NavHostController, servicio: RecipeApiService, favoritos: List<Int>) {
    var listaFavoritos by remember { mutableStateOf<List<RecipeModel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(favoritos.size) {
        isLoading = true
        val listado = mutableListOf<RecipeModel>()
        favoritos.forEach { id ->
            try {
                val recipe = servicio.getRecipeById(id)
                listado.add(recipe)
            } catch (e: Exception) { e.printStackTrace() }
        }
        listaFavoritos = listado
        isLoading = false
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(Background).padding(top = 24.dp, start = 8.dp, end = 24.dp, bottom = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Rounded.ArrowBack, null, tint = OnBackground)
                    }
                    Text("Mis Favoritos", style = MaterialTheme.typography.headlineMedium, color = OnBackground)
                }
            }
        },
        containerColor = Background
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
        } else if (listaFavoritos.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No tienes recetas favoritas aún", color = TextMuted)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize().padding(padding)
            ) {
                items(listaFavoritos, key = { it.id ?: 0 }) { recipe ->
                    RecipeCardPremium(
                        recipe = recipe,
                        isFav = true,
                        onFavToggle = { /* En favoritos solo mostrar, o quitar si se desea */ },
                        onClick = { navController.navigate("recipeDetail/${recipe.id ?: 0}") }
                    )
                }
            }
        }
    }
}

@Composable
fun PaginationControls(currentPage: Int, totalPages: Int, onPageChange: (Int) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
        color = Surface,
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { if (currentPage > 1) onPageChange(currentPage - 1) },
                enabled = currentPage > 1,
                modifier = Modifier.background(if (currentPage > 1) Primary else Background, CircleShape)
            ) { Icon(Icons.Rounded.ChevronLeft, null, tint = if (currentPage > 1) OnPrimary else TextMuted) }

            Text("Página $currentPage de $totalPages", style = MaterialTheme.typography.labelLarge, color = OnSurface)

            IconButton(
                onClick = { if (currentPage < totalPages) onPageChange(currentPage + 1) },
                enabled = currentPage < totalPages,
                modifier = Modifier.background(if (currentPage < totalPages) Primary else Background, CircleShape)
            ) { Icon(Icons.Rounded.ChevronRight, null, tint = if (currentPage < totalPages) OnPrimary else TextMuted) }
        }
    }
}

@Composable
fun RecipeCardPremium(
    recipe: RecipeModel,
    isFav: Boolean = false,
    onFavToggle: () -> Unit = {},
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Column {
            Box {
                Image(
                    painter = rememberAsyncImagePainter(model = recipe.image),
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(28.dp)),
                    contentScale = ContentScale.Crop
                )
                
                // Botón de Favorito
                Surface(
                    modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
                    color = Color.Black.copy(alpha = 0.5f),
                    shape = CircleShape
                ) {
                    IconButton(onClick = onFavToggle, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            tint = if (isFav) Color.Red else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Surface(
                    modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Star, null, tint = Primary, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("${recipe.rating}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Column(Modifier.padding(16.dp)) {
                Text(
                    recipe.cuisine?.uppercase() ?: "VARIADA",
                    style = MaterialTheme.typography.labelSmall,
                    color = Primary,
                    fontWeight = FontWeight.Black
                )
                Text(
                    recipe.name ?: "",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = OnSurface
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Timer, null, tint = TextMuted, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("${(recipe.prepTimeMinutes ?: 0) + (recipe.cookTimeMinutes ?: 0)} min", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenRecipeDetail(servicio: RecipeApiService, id: Int, favoritos: MutableList<Int>) {
    var recipe by remember { mutableStateOf<RecipeModel?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(id) {
        try { recipe = servicio.getRecipeById(id) } catch (e: Exception) { e.printStackTrace() }
        finally { isLoading = false }
    }

    if (isLoading) {
        Box(Modifier.fillMaxSize().background(Background), contentAlignment = Alignment.Center) { 
            CircularProgressIndicator(color = Primary) 
        }
    } else if (recipe != null) {
        val isFav = favoritos.contains(recipe?.id)
        
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).background(Background)) {
            Box {
                Image(
                    painter = rememberAsyncImagePainter(model = recipe?.image),
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(400.dp),
                    contentScale = ContentScale.Crop
                )
                Box(modifier = Modifier.fillMaxWidth().height(400.dp).background(Brush.verticalGradient(listOf(Color.Transparent, Background))))
                
                // Botones superiores en detalle
                Row(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(color = Background.copy(alpha = 0.5f), shape = CircleShape) {
                        // El botón de volver lo maneja el sistema o se puede añadir aquí
                    }
                    
                    Surface(color = Background.copy(alpha = 0.5f), shape = CircleShape) {
                        IconButton(onClick = {
                            if (isFav) favoritos.remove(recipe?.id)
                            else favoritos.add(recipe?.id ?: 0)
                        }) {
                            Icon(
                                imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = null,
                                tint = if (isFav) Color.Red else Color.White
                            )
                        }
                    }
                }
            }
            
            Column(modifier = Modifier.offset(y = (-40).dp).padding(horizontal = 24.dp)) {
                Surface(color = Primary, shape = RoundedCornerShape(12.dp)) {
                    Text(
                        recipe?.cuisine ?: "Internacional",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = OnPrimary)
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    recipe?.name ?: "",
                    style = MaterialTheme.typography.headlineMedium,
                    color = OnBackground
                )
                
                Spacer(Modifier.height(32.dp))
                
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    DetailBadge(Icons.Rounded.Timer, "Tiempo", "${recipe?.prepTimeMinutes!! + recipe?.cookTimeMinutes!!}m")
                    DetailBadge(Icons.Rounded.SignalCellularAlt, "Nivel", recipe?.difficulty ?: "Medio")
                    DetailBadge(Icons.Rounded.LocalFireDepartment, "Calorías", "450 kcal")
                }
                
                Spacer(Modifier.height(40.dp))
                
                SectionTitle("Ingredientes")
                recipe?.ingredients?.forEach { IngredientRow(it) }
                
                Spacer(Modifier.height(40.dp))
                
                SectionTitle("Preparación")
                recipe?.instructions?.forEachIndexed { i, s -> StepRow(i + 1, s) }
                
                Spacer(Modifier.height(100.dp))
            }
        }
    }
}

@Composable
fun DetailBadge(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = Primary, modifier = Modifier.size(24.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
        Text(value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = OnSurface)
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleLarge,
        color = OnBackground,
        modifier = Modifier.padding(bottom = 16.dp)
    )
}

@Composable
fun IngredientRow(text: String) {
    Row(
        modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth().background(Surface, RoundedCornerShape(16.dp)).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(8.dp).background(Primary, CircleShape))
        Spacer(Modifier.width(16.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = OnSurface)
    }
}

@Composable
fun StepRow(index: Int, text: String) {
    Row(modifier = Modifier.padding(vertical = 12.dp)) {
        Surface(
            modifier = Modifier.size(32.dp),
            shape = CircleShape,
            color = Surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, Primary.copy(alpha = 0.3f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("$index", style = MaterialTheme.typography.labelMedium, color = Primary)
            }
        }
        Spacer(Modifier.width(16.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 26.sp), color = OnSurface)
    }
}
