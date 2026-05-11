package com.example.lab09

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.lab09.models.PostModel
import com.example.lab09.remote.PostApiService

@Composable
fun ScreenPosts(navController: NavHostController, servicio: PostApiService) {
    val listaPosts: SnapshotStateList<PostModel> = remember { mutableStateListOf() }
    
    LaunchedEffect(Unit) {
        try {
            val listado = servicio.getUserPosts()
            listado.forEach { listaPosts.add(it) }
        } catch (e: Exception) {
            Log.e("POSTS", "Error al obtener posts: ${e.message}")
        }
    }

    LazyColumn {
        items(listaPosts) { item ->
            Row(modifier = Modifier.padding(8.dp)) {
                Text(text = item.id.toString(), Modifier.weight(0.05f), textAlign = TextAlign.End)
                Spacer(Modifier.padding(horizontal = 1.dp))
                Text(text = item.title, Modifier.weight(0.7f))
                IconButton(
                    onClick = {
                        navController.navigate("postsVer/${item.id}")
                        Log.e("POSTS", "ID = ${item.id}")
                    },
                    Modifier.weight(0.1f)
                ) {
                    Icon(imageVector = Icons.Outlined.Search, contentDescription = "Ver")
                }
            }
        }
    }
}

@Composable
fun ScreenPost(navController: NavHostController, servicio: PostApiService, id: Int) {
    var post by remember { mutableStateOf<PostModel?>(null) }
    
    LaunchedEffect(Unit) {
        try {
            val xpost = servicio.getUserPostById(id)
            post = xpost
        } catch (e: Exception) {
            Log.e("POSTS", "Error al obtener post $id: ${e.message}")
        }
    }
    
    Column(
        Modifier
            .padding(8.dp)
            .fillMaxSize()
    ) {
        if (post != null) {
            OutlinedTextField(
                value = post!!.id.toString(),
                onValueChange = {},
                label = { Text("id") },
                readOnly = true
            )
            OutlinedTextField(
                value = post!!.userId.toString(),
                onValueChange = {},
                label = { Text("userId") },
                readOnly = true
            )
            OutlinedTextField(
                value = post!!.title,
                onValueChange = {},
                label = { Text("title") },
                readOnly = true
            )
            OutlinedTextField(
                value = post!!.body,
                onValueChange = {},
                label = { Text("body") },
                readOnly = true
            )
        } else {
            Text("Cargando...")
        }
    }
}
