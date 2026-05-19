package com.example.lab09

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.lab09.models.PostModel
import com.example.lab09.utils.OnDeviceTranslator
import com.example.lab09.utils.translatePostsListAsync
import com.example.lab09.utils.translatePostAsync
import com.example.lab09.remote.PostApiService
import com.example.lab09.ui.theme.*

@Composable
fun ScreenPosts(navController: NavHostController, servicio: PostApiService, currentLanguage: MutableState<String>) {
    val listaPosts = remember { mutableStateListOf<PostModel>() }
    val nombresUsuarios = remember { mutableStateMapOf<Int, String>() }
    val isEs = currentLanguage.value == "es"
    
    LaunchedEffect(currentLanguage.value) {
        try {
            val usuarios = servicio.getUsers()
            usuarios.forEach { nombresUsuarios[it.id] = it.name }
            
            val listadoRaw = servicio.getUserPosts()
            val listadoFinal = if (isEs) {
                translatePostsListAsync(listadoRaw, "es")
            } else {
                listadoRaw
            }
            listaPosts.clear()
            listaPosts.addAll(listadoFinal)
        } catch (e: Exception) {
            Log.e("POSTS", "Error: ${e.message}")
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Background).statusBarsPadding()) {
        Text(
            if (isEs) "Comunidad" else "Community",
            modifier = Modifier.padding(24.dp),
            style = MaterialTheme.typography.headlineMedium,
            color = OnBackground
        )
        
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(listaPosts) { item ->
                val nombreAutor = nombresUsuarios[item.userId] ?: (if (isEs) "Cargando..." else "Loading...")
                PostCard(item, nombreAutor, currentLanguage) {
                    navController.navigate("postsVer/${item.id}")
                }
            }
        }
    }
}

@Composable
fun PostCard(post: PostModel, nombreAutor: String, currentLanguage: MutableState<String>, onClick: () -> Unit) {
    val isEs = currentLanguage.value == "es"
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() },
        color = Surface,
        tonalElevation = 2.dp
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            AsyncImage(
                model = "https://i.pravatar.cc/150?u=${post.userId}",
                contentDescription = null,
                modifier = Modifier
                    .size(45.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    text = nombreAutor,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, color = Primary)
                )
                Text(
                    text = post.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold, color = OnSurface),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.ChatBubbleOutline, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (isEs) "Ver detalles" else "View details", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                }
            }
        }
    }
}

@Composable
fun ScreenPost(navController: NavHostController, servicio: PostApiService, id: Int, currentLanguage: MutableState<String>) {
    var post by remember { mutableStateOf<PostModel?>(null) }
    var nombreAutor by remember { mutableStateOf("") }
    val isEs = currentLanguage.value == "es"
    val scrollState = rememberScrollState()
    
    LaunchedEffect(id, currentLanguage.value) {
        post = null 
        nombreAutor = if (isEs) "Cargando..." else "Loading..."
        try { 
            val listaCompleta = servicio.getUserPosts()
            val p = listaCompleta.find { it.id == id }
            
            if (p != null) {
                post = if (isEs) translatePostAsync(p, "es") else p
            }
            
            val usuarios = servicio.getUsers()
            nombreAutor = usuarios.find { it.id == p?.userId }?.name ?: (if (isEs) "Usuario Desconocido" else "Unknown User")
        } catch (e: Exception) { 
            Log.e("POST_DETAIL", "Error: ${e.message}")
        }
    }
    
    // Contenedor base sin Scaffold para evitar márgenes superiores
    Box(modifier = Modifier.fillMaxSize().background(Background)) {
        if (post != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                // Header que inicia desde el borde real de la pantalla
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .background(
                            Brush.verticalGradient(
                                listOf(Primary.copy(alpha = 0.25f), Color.Transparent)
                            )
                        )
                        .padding(bottom = 24.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Botón de Volver con padding solo para la barra de estado
                        Surface(
                            onClick = { navController.popBackStack() },
                            color = Color.Black.copy(alpha = 0.3f),
                            shape = CircleShape,
                            modifier = Modifier
                                .statusBarsPadding()
                                .padding(start = 16.dp, top = 8.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Rounded.ArrowBack, 
                                contentDescription = if (isEs) "Volver" else "Back",
                                modifier = Modifier.padding(8.dp),
                                tint = Color.White
                            )
                        }

                        // Contenido del título pegado lo más arriba posible
                        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
                            Surface(
                                color = Primary,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    if (isEs) "ARTÍCULO DE COMUNIDAD" else "COMMUNITY ARTICLE",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.2.sp
                                    ),
                                    color = OnPrimary
                                )
                            }
                            
                            Spacer(Modifier.height(12.dp))
                            
                            Text(
                                text = post!!.title,
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    lineHeight = 34.sp,
                                    letterSpacing = (-0.5).sp
                                ),
                                color = OnBackground
                            )
                        }
                    }
                }

                // Resto del contenido
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp)
                ) {
                    // Fila de Autor
                    Surface(
                        color = Surface,
                        shape = RoundedCornerShape(24.dp),
                        tonalElevation = 4.dp,
                        shadowElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = "https://i.pravatar.cc/150?u=${post!!.userId}",
                                contentDescription = null,
                                modifier = Modifier.size(52.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text(nombreAutor, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = OnSurface)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.Verified, null, tint = Primary, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(if (isEs) "Autor Gourmet" else "Gourmet Author", style = MaterialTheme.typography.bodySmall, color = Primary)
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(32.dp))

                    // Cuerpo del Post
                    Surface(
                        color = Surface.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(32.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Surface.copy(alpha = 0.8f))
                    ) {
                        Column(modifier = Modifier.padding(28.dp)) {
                            Icon(Icons.Rounded.FormatQuote, null, tint = Primary.copy(alpha = 0.3f), modifier = Modifier.size(40.dp).offset(x = (-10).dp))
                            Text(
                                text = post!!.body,
                                style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 32.sp, fontSize = 18.sp),
                                color = OnSurface.copy(alpha = 0.9f)
                            )
                            Spacer(Modifier.height(24.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                Icon(Icons.Rounded.FormatQuote, null, tint = Primary.copy(alpha = 0.3f), modifier = Modifier.size(40.dp).graphicsLayer(rotationZ = 180f))
                            }
                        }
                    }

                    Spacer(Modifier.height(32.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        DetailChip(Icons.Rounded.Numbers, "Post #${post!!.id}")
                        DetailChip(Icons.Rounded.Tag, if (isEs) "Comunidad" else "Community")
                    }

                    Spacer(Modifier.height(32.dp))
                    HorizontalDivider(color = Surface, thickness = 1.dp)
                    Spacer(Modifier.height(24.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                        InteractionItem(Icons.Rounded.FavoriteBorder, "${150 + post!!.id * 7}")
                        InteractionItem(Icons.Rounded.ChatBubbleOutline, "${post!!.commentCount ?: 0}")
                        InteractionItem(Icons.Rounded.Share, if (isEs) "Compartir" else "Share")
                    }
                    
                    Spacer(Modifier.height(40.dp))
                    
                    Text(
                        if (isEs) "Comentarios (${post!!.commentCount ?: 0})" else "Comments (${post!!.commentCount ?: 0})", 
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), 
                        color = OnBackground
                    )
                    Spacer(Modifier.height(16.dp))
                    
                    val numComentarios = post!!.commentCount ?: 0
                    if (numComentarios > 0) {
                        val listaSimulada = if (isEs) {
                            listOf(
                                "¡Excelente publicación! Me ha servido mucho.",
                                "Muy bien explicado el tema, gracias.",
                                "Interesante punto de vista sobre esto.",
                                "Me gustaría saber más al respecto.",
                                "Gran aporte para la comunidad Gourmet."
                            )
                        } else {
                            listOf(
                                "Excellent post! It has helped me a lot.",
                                "Very well explained topic, thanks.",
                                "Interesting point of view on this.",
                                "I would like to know more about it.",
                                "Great contribution to the Gourmet community."
                            )
                        }
                        repeat(minOf(numComentarios, 5)) { index ->
                            CommentItem(
                                user = if (isEs) "Usuario Gourmet ${index + 1}" else "Gourmet User ${index + 1}", 
                                text = listaSimulada[index % listaSimulada.size]
                            )
                            Spacer(Modifier.height(12.dp))
                        }
                        if (numComentarios > 5) {
                            Text(
                                if (isEs) "Ver los ${numComentarios - 5} comentarios restantes..." else "See the ${numComentarios - 5} remaining comments...", 
                                style = MaterialTheme.typography.labelMedium, 
                                color = Primary, 
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    } else {
                        Text(if (isEs) "No hay comentarios aún." else "No comments yet.", color = TextMuted, style = MaterialTheme.typography.bodyMedium)
                    }
                    
                    Spacer(Modifier.height(100.dp))
                }
            }
        } else {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Primary)
        }
    }
}

@Composable
fun CommentItem(user: String, text: String) {
    Surface(color = Surface, shape = RoundedCornerShape(16.dp), tonalElevation = 1.dp) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            Text(user, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = Primary)
            Spacer(Modifier.height(4.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium, color = OnSurface.copy(alpha = 0.8f))
        }
    }
}

@Composable
fun DetailChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Surface(color = Surface, shape = RoundedCornerShape(12.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Primary.copy(alpha = 0.2f))) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Primary, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = OnSurface)
        }
    }
}

@Composable
fun InteractionItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = TextMuted, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
    }
}
