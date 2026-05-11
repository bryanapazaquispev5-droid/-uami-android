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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.lab09.models.PostModel
import com.example.lab09.remote.PostApiService
import com.example.lab09.ui.theme.*

@Composable
fun ScreenPosts(navController: NavHostController, servicio: PostApiService) {
    val listaPosts: SnapshotStateList<PostModel> = remember { mutableStateListOf() }
    
    LaunchedEffect(Unit) {
        try {
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
                PostCard(item) {
                    navController.navigate("postsVer/${item.id}")
                }
            }
        }
    }
}

@Composable
fun PostCard(post: PostModel, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() },
        color = Surface,
        tonalElevation = 2.dp
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = Primary.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Person, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    text = "Usuario ${post.userId}",
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
    
    LaunchedEffect(Unit) {
        try { post = servicio.getUserPostById(id) } catch (e: Exception) { e.printStackTrace() }
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
                        Surface(shape = CircleShape, color = Primary, modifier = Modifier.size(48.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.Person, contentDescription = null, tint = OnPrimary)
                            }
                        }
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text("Autor", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            Text("Usuario ${post!!.userId}", style = MaterialTheme.typography.titleMedium, color = OnBackground)
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
                            label = { Text("ID: ${post!!.id}") },
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
