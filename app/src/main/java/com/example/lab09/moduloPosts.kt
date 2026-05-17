package com.example.lab09

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.lab09.models.PostModel
import com.example.lab09.remote.PostApiService
import com.example.lab09.ui.theme.*

@Composable
fun ScreenPosts(navController: NavHostController, servicio: PostApiService) {
    val listaPosts: SnapshotStateList<PostModel> = remember { mutableStateListOf() }
    val nombresUsuarios = remember { mutableStateMapOf<Int, String>() }
    
    LaunchedEffect(Unit) {
        try {
            // 1. Cargar usuarios para tener sus nombres
            val usuarios = servicio.getUsers()
            usuarios.forEach { nombresUsuarios[it.id] = it.name }
            
            // 2. Cargar los posts
            val listado = servicio.getUserPosts()
            listado.forEach { listaPosts.add(it) }
        } catch (e: Exception) {
            Log.e("POSTS", "Error: ${e.message}")
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Background)) {
        Text(
            "Comunidad",
            modifier = Modifier.padding(24.dp),
            style = MaterialTheme.typography.headlineMedium,
            color = OnBackground
        )
        
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(listaPosts) { item ->
                val nombreAutor = nombresUsuarios[item.userId] ?: "Cargando..."
                PostCard(item, nombreAutor) {
                    navController.navigate("postsVer/${item.id}")
                }
            }
        }
    }
}

@Composable
fun PostCard(post: PostModel, nombreAutor: String, onClick: () -> Unit) {
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
                    Text("Ver detalles", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenPost(navController: NavHostController, servicio: PostApiService, id: Int) {
    var post by remember { mutableStateOf<PostModel?>(null) }
    var nombreAutor by remember { mutableStateOf("Cargando...") }
    
    LaunchedEffect(Unit) {
        try { 
            val p = servicio.getUserPostById(id)
            post = p
            // Intentar buscar el nombre del autor por su ID
            val usuarios = servicio.getUsers()
            nombreAutor = usuarios.find { it.id == p.userId }?.name ?: "Desconocido"
        } catch (e: Exception) { e.printStackTrace() }
    }
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Detalle del Post", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Background,
                    titleContentColor = OnBackground,
                    navigationIconContentColor = OnBackground
                )
            )
        },
        containerColor = Background
    ) { pv ->
        Box(modifier = Modifier.padding(pv).fillMaxSize()) {
            if (post != null) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = "https://i.pravatar.cc/150?u=${post!!.userId}",
                            contentDescription = null,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text("Autor", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            Text(nombreAutor, style = MaterialTheme.typography.titleMedium, color = OnBackground)
                        }
                    }
                    
                    Spacer(Modifier.height(32.dp))
                    
                    Text(
                        text = post!!.title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = OnBackground
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Surface,
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text(
                            text = post!!.body,
                            modifier = Modifier.padding(24.dp),
                            style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 28.sp),
                            color = OnSurface
                        )
                    }
                    
                    Spacer(Modifier.height(24.dp))
                    
                    Row {
                        AssistChip(
                            onClick = {},
                            label = { Text("ID de Post: ${post!!.id}") },
                            leadingIcon = { Icon(Icons.Rounded.Tag, null, modifier = Modifier.size(16.dp)) },
                            colors = AssistChipDefaults.assistChipColors(labelColor = TextMuted)
                        )
                    }
                }
            } else {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Primary)
            }
        }
    }
}
