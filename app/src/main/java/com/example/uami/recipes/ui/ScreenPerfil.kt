package com.example.uami.recipes.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import com.example.uami.utils.BouncyPressEffect
import com.example.uami.utils.shimmerGlow
import com.example.uami.utils.pulseAnimation
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.uami.recipes.viewmodel.ReviewsViewModel
import com.example.uami.recipes.viewmodel.ViewModelFactory
import com.example.uami.ui.theme.Background
import com.example.uami.ui.theme.Primary
import com.example.uami.ui.theme.Secondary
import com.example.uami.ui.theme.Surface
import com.example.uami.ui.theme.TextMuted
import com.example.uami.ui.theme.OnBackground

// Predefined Chef Avatars
data class AvatarItem(val id: String, val icon: ImageVector, val gradient: Brush, val nameEs: String, val nameEn: String)

val chefAvatars = listOf(
    AvatarItem("avatar_1", Icons.Rounded.OutdoorGrill, Brush.linearGradient(listOf(Color(0xFFFF416C), Color(0xFFFF4B2B))), "Parrillero", "Grill Master"),
    AvatarItem("avatar_2", Icons.Rounded.Spa, Brush.linearGradient(listOf(Color(0xFF11998e), Color(0xFF38ef7d))), "Saludable", "Healthy Chef"),
    AvatarItem("avatar_3", Icons.Rounded.Cake, Brush.linearGradient(listOf(Color(0xFF8A2387), Color(0xFFE94057))), "Pastelero", "Pastry Chef"),
    AvatarItem("avatar_4", Icons.Rounded.LocalPizza, Brush.linearGradient(listOf(Color(0xFF00c6ff), Color(0xFF0072ff))), "Pizzero", "Pizza Chef"),
    AvatarItem("avatar_5", Icons.Rounded.Restaurant, Brush.linearGradient(listOf(Color(0xFFF12711), Color(0xFFF5AF19))), "Gourmet", "Gourmet Chef"),
    AvatarItem("avatar_6", Icons.Rounded.LocalCafe, Brush.linearGradient(listOf(Color(0xFF8e2de2), Color(0xFF4a00e0))), "Barista", "Coffee Master")
)

@Composable
fun ChefAvatar(
    avatarId: String,
    modifier: Modifier = Modifier
) {
    val avatar = chefAvatars.find { it.id == avatarId } ?: chefAvatars.first()
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(avatar.gradient),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = avatar.icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.fillMaxSize(0.55f)
        )
    }
}

@Composable
fun UserAvatar(
    photoUrl: String,
    modifier: Modifier = Modifier
) {
    if (photoUrl.startsWith("avatar_")) {
        ChefAvatar(avatarId = photoUrl, modifier = modifier)
    } else if (photoUrl.startsWith("data:image/") || (photoUrl.isNotEmpty() && !photoUrl.startsWith("http"))) {
        val bitmap = remember(photoUrl) {
            val cleanBase64 = if (photoUrl.startsWith("data:image/")) {
                photoUrl.substringAfter("base64,")
            } else {
                photoUrl
            }
            try {
                val decodedBytes = Base64.decode(cleanBase64, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
            } catch (e: Exception) {
                null
            }
        }
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Custom Avatar",
                modifier = modifier
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = modifier
                    .clip(CircleShape)
                    .background(Primary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Person,
                    contentDescription = "Default Avatar",
                    tint = Primary,
                    modifier = Modifier.fillMaxSize(0.55f)
                )
            }
        }
    } else if (photoUrl.isNotEmpty()) {
        AsyncImage(
            model = photoUrl,
            contentDescription = "Avatar",
            modifier = modifier
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = modifier
                .clip(CircleShape)
                .background(Primary.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Person,
                contentDescription = "Default Avatar",
                tint = Primary,
                modifier = Modifier.fillMaxSize(0.55f)
            )
        }
    }
}

@Composable
fun ScreenPerfil(
    onBack: () -> Unit,
    currentLanguage: MutableState<String>,
    reviewsViewModel: ReviewsViewModel,
    favoritosSize: Int
) {
    val currentUser by reviewsViewModel.currentUser.collectAsState()
    val userProfile by reviewsViewModel.userProfile.collectAsState()
    val isPosting by reviewsViewModel.isPosting.collectAsState()
    val reviews by reviewsViewModel.reviews.collectAsState()

    val context = LocalContext.current
    val isEs = currentLanguage.value == "es"

    val userReviewsCount = remember(reviews, currentUser) {
        val uid = currentUser?.uid
        if (uid != null) reviews.count { it.userId == uid } else 0
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
    ) {
        // --- TOP BAR ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Rounded.ArrowBack,
                    contentDescription = "Volver",
                    tint = Primary
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (isEs) "Mi Perfil de Chef" else "My Chef Profile",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            if (currentUser == null) {
                AuthCard(
                    isEs = isEs,
                    isUploading = isPosting,
                    onLoginSubmit = { email, password ->
                        reviewsViewModel.signInWithEmailAndPassword(
                            email = email,
                            password = password,
                            onSuccess = {
                                Toast.makeText(context, if (isEs) "¡Sesión iniciada!" else "Signed in successfully!", Toast.LENGTH_SHORT).show()
                            },
                            onFailure = { e ->
                                Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    onRegisterSubmit = { email, password, displayName, avatarId, customUri ->
                        if (customUri != null) {
                            reviewsViewModel.registerWithEmailAndPasswordAndCustomPhoto(
                                context = context,
                                email = email,
                                password = password,
                                displayName = displayName,
                                imageUri = customUri,
                                onSuccess = {
                                    Toast.makeText(context, if (isEs) "¡Registro exitoso!" else "Registered successfully!", Toast.LENGTH_SHORT).show()
                                },
                                onFailure = { e ->
                                    Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                }
                            )
                        } else {
                            reviewsViewModel.registerWithEmailAndPassword(
                                email = email,
                                password = password,
                                displayName = displayName,
                                avatarId = avatarId,
                                onSuccess = {
                                    Toast.makeText(context, if (isEs) "¡Registro exitoso!" else "Registered successfully!", Toast.LENGTH_SHORT).show()
                                },
                                onFailure = { e ->
                                    Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                }
                            )
                        }
                    }
                )
            } else {
                ProfileDetailCard(
                    isEs = isEs,
                    userName = userProfile?.displayName ?: currentUser?.displayName ?: "Chef",
                    userEmail = currentUser?.email ?: "",
                    userPhotoUrl = userProfile?.photoUrl ?: currentUser?.photoUrl?.toString() ?: "avatar_1",
                    userReviewsCount = userReviewsCount,
                    favoritosSize = favoritosSize,
                    isUploading = isPosting,
                    onSignOutClick = {
                        reviewsViewModel.signOut()
                        Toast.makeText(context, if (isEs) "Sesión cerrada" else "Signed out", Toast.LENGTH_SHORT).show()
                    },
                    onUpdateProfile = { newName, newAvatar ->
                        reviewsViewModel.updateProfile(
                            displayName = newName,
                            avatarId = newAvatar,
                            onSuccess = {
                                Toast.makeText(context, if (isEs) "Perfil actualizado" else "Profile updated", Toast.LENGTH_SHORT).show()
                            },
                            onFailure = { e ->
                                Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    onUploadCustomPhoto = { newName, localUri ->
                        reviewsViewModel.uploadProfileImageAndEdit(
                            context = context,
                            displayName = newName,
                            imageUri = localUri,
                            onSuccess = {
                                Toast.makeText(context, if (isEs) "¡Foto de perfil subida!" else "Profile photo uploaded!", Toast.LENGTH_SHORT).show()
                            },
                            onFailure = { e ->
                                Toast.makeText(context, "Error de subida: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                            }
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun AuthCard(
    isEs: Boolean,
    isUploading: Boolean,
    onLoginSubmit: (String, String) -> Unit,
    onRegisterSubmit: (String, String, String, String, Uri?) -> Unit
) {
    var isRegisterMode by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var selectedAvatarId by remember { mutableStateOf("avatar_1") }
    var registerPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var showPassword by remember { mutableStateOf(false) }

    val registerImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            registerPhotoUri = uri
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = Surface,
        border = BorderStroke(1.dp, Primary.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = if (isRegisterMode) Icons.Rounded.PersonAdd else Icons.Rounded.AccountCircle,
                contentDescription = "Auth",
                tint = Primary,
                modifier = Modifier.size(56.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = if (isRegisterMode) {
                    if (isEs) "Crea tu Cuenta Chef" else "Create Chef Account"
                } else {
                    if (isEs) "Inicia Sesión" else "Sign In"
                },
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))

            // Registration specific fields
            AnimatedVisibility(visible = isRegisterMode) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Chef Avatar Selection title
                    Text(
                        text = if (isEs) "Elige tu foto de perfil de Chef:" else "Choose your Chef Profile Photo:",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Avatar Preview (either predefined or custom photo)
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .align(Alignment.CenterHorizontally)
                            .padding(bottom = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (registerPhotoUri != null) {
                            AsyncImage(
                                model = registerPhotoUri,
                                contentDescription = "Selected Photo",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .border(BorderStroke(2.dp, Primary), CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            ChefAvatar(
                                avatarId = selectedAvatarId,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .border(BorderStroke(2.dp, Primary), CircleShape)
                            )
                        }
                    }

                    // Predefined avatars grid
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        chefAvatars.forEach { avatar ->
                            val isSelected = selectedAvatarId == avatar.id && registerPhotoUri == null
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .clickable(enabled = !isUploading) {
                                        selectedAvatarId = avatar.id
                                        registerPhotoUri = null
                                    }
                                    .border(
                                        width = if (isSelected) 3.dp else 0.dp,
                                        color = if (isSelected) Primary else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .padding(if (isSelected) 2.dp else 0.dp)
                            ) {
                                ChefAvatar(avatarId = avatar.id, modifier = Modifier.fillMaxSize())
                            }
                        }
                    }

                    // Gallery Picker Button for Registration
                    OutlinedButton(
                        onClick = {
                            registerImagePickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        enabled = !isUploading,
                        border = BorderStroke(1.dp, Primary.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(bottom = 16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (registerPhotoUri != null) {
                                    if (isEs) "¡Foto seleccionada! 📷" else "Photo selected! 📷"
                                } else {
                                    if (isEs) "Subir mi propia foto 📷" else "Upload my own photo 📷"
                                },
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        enabled = !isUploading,
                        label = { Text(if (isEs) "Nombre de Chef (ej: Ivan)" else "Chef Name (e.g., Ivan)") },
                        leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null, tint = Primary) },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = Primary.copy(alpha = 0.3f),
                            focusedLabelColor = Primary
                        ),
                        singleLine = true
                    )
                }
            }

            // Email Field
            OutlinedTextField(
                value = email,
                onValueChange = { email = it.trim() },
                enabled = !isUploading,
                label = { Text(if (isEs) "Correo electrónico" else "Email Address") },
                leadingIcon = { Icon(Icons.Rounded.Mail, contentDescription = null, tint = Primary) },
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = Primary.copy(alpha = 0.3f),
                    focusedLabelColor = Primary
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true
            )

            // Password Field
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                enabled = !isUploading,
                label = { Text(if (isEs) "Contraseña" else "Password") },
                leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null, tint = Primary) },
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }, enabled = !isUploading) {
                        Icon(
                            imageVector = if (showPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                            contentDescription = null,
                            tint = TextMuted
                        )
                    }
                },
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = Primary.copy(alpha = 0.3f),
                    focusedLabelColor = Primary
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true
            )

            Button(
                onClick = {
                    if (isRegisterMode) {
                        if (email.isNotEmpty() && password.isNotEmpty() && displayName.isNotEmpty()) {
                            onRegisterSubmit(email, password, displayName, selectedAvatarId, registerPhotoUri)
                        }
                    } else {
                        if (email.isNotEmpty() && password.isNotEmpty()) {
                            onLoginSubmit(email, password)
                        }
                    }
                },
                enabled = email.isNotEmpty() && password.isNotEmpty() && (!isRegisterMode || displayName.isNotEmpty()) && !isUploading,
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(vertical = 14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isUploading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                } else {
                    Text(
                        text = if (isRegisterMode) {
                            if (isEs) "Registrarse y Entrar" else "Register & Enter"
                        } else {
                            if (isEs) "Entrar" else "Sign In"
                        },
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Toggle Auth Mode
            Text(
                text = if (isRegisterMode) {
                    if (isEs) "¿Ya tienes cuenta? Inicia sesión aquí" else "Already have an account? Sign In"
                } else {
                    if (isEs) "¿Eres nuevo? Regístrate aquí" else "New here? Register a Chef Account"
                },
                color = Primary,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier
                    .clickable(enabled = !isUploading) { isRegisterMode = !isRegisterMode }
                    .padding(8.dp)
            )
        }
    }
}

@Composable
fun ProfileDetailCard(
    isEs: Boolean,
    userName: String,
    userEmail: String,
    userPhotoUrl: String,
    userReviewsCount: Int,
    favoritosSize: Int,
    isUploading: Boolean,
    onSignOutClick: () -> Unit,
    onUpdateProfile: (String, String) -> Unit,
    onUploadCustomPhoto: (String, Uri) -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf(userName) }
    var editAvatarId by remember { mutableStateOf(if (userPhotoUrl.startsWith("avatar_")) userPhotoUrl else "avatar_1") }
    var customPhotoUri by remember { mutableStateOf<Uri?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            customPhotoUri = uri
        }
    }

    val totalScore = userReviewsCount * 2 + favoritosSize
    val chefRankName = remember(totalScore, isEs) {
        when {
            totalScore >= 12 -> if (isEs) "Chef Ejecutivo 👑" else "Executive Chef 👑"
            totalScore >= 6 -> if (isEs) "Sous Chef 🍕" else "Sous Chef 🍕"
            totalScore >= 2 -> if (isEs) "Chef de Partie 🍳" else "Chef de Partie 🍳"
            else -> if (isEs) "Chef Novato 🥚" else "Novice Chef 🥚"
        }
    }

    val chefRankGradient = remember(totalScore) {
        when {
            totalScore >= 12 -> Brush.linearGradient(listOf(Color(0xFFFFD700), Color(0xFFFFA500)))
            totalScore >= 6 -> Brush.linearGradient(listOf(Color(0xFF8E2DE2), Color(0xFF4A00E0)))
            totalScore >= 2 -> Brush.linearGradient(listOf(Color(0xFF11998E), Color(0xFF38EF7D)))
            else -> Brush.linearGradient(listOf(Color(0xFF8A2387), Color(0xFFE94057)))
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp)),
        shape = RoundedCornerShape(32.dp),
        color = Surface,
        border = BorderStroke(1.dp, Primary.copy(alpha = 0.15f)),
        tonalElevation = 6.dp
    ) {
        Crossfade(targetState = isEditing, label = "profileCardMode") { editing ->
            if (!editing) {
                // VIEW MODE
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top banner background
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Primary, Color(0xFFF27121), Secondary)
                                )
                            )
                            .shimmerGlow(durationMillis = 3500),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isEs) "¡BIENVENIDO CHEF!" else "WELCOME CHEF!",
                            color = Color.White.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            letterSpacing = 2.sp
                        )
                    }

                    // Avatar overlapping the banner
                    Box(
                        modifier = Modifier
                            .offset(y = (-45).dp)
                            .size(105.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Rotating glowing ring
                        val infiniteTransition = rememberInfiniteTransition(label = "avatarRing")
                        val rotationAngle by infiniteTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = 360f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(6000, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "rotationAngle"
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(
                                    Brush.sweepGradient(
                                        colors = listOf(Primary, Secondary, Primary),
                                        center = Offset.Unspecified
                                    )
                                )
                                .graphicsLayer(rotationZ = rotationAngle)
                        )

                        // Inner background spacer to make the ring thin
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(3.dp)
                                .background(Surface, CircleShape)
                        )

                        // The actual avatar
                        UserAvatar(
                            photoUrl = userPhotoUrl,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(6.dp)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(y = (-30).dp)
                            .padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = userName,
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                            color = OnBackground,
                            textAlign = TextAlign.Center
                        )

                        Spacer(Modifier.height(6.dp))

                        // Chef Rank Badge
                        Box(
                            modifier = Modifier
                                .background(chefRankGradient, shape = RoundedCornerShape(12.dp))
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = chefRankName,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }

                        Text(
                            text = userEmail,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        // Gamified stats cards grid
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            StatCard(
                                icon = Icons.Rounded.Favorite,
                                value = favoritosSize.toString(),
                                label = if (isEs) "Favoritas" else "Favorites",
                                color = Color(0xFFFF4B2B),
                                modifier = Modifier.weight(1f)
                            )

                            StatCard(
                                icon = Icons.Rounded.Comment,
                                value = userReviewsCount.toString(),
                                label = if (isEs) "Reseñas" else "Reviews",
                                color = Color(0xFF00C6FF),
                                modifier = Modifier.weight(1f)
                            )

                            StatCard(
                                icon = Icons.Rounded.WorkspacePremium,
                                value = if (isEs) "Nivel ${totalScore / 3 + 1}" else "Level ${totalScore / 3 + 1}",
                                label = if (isEs) "Puntos: $totalScore" else "Score: $totalScore",
                                color = Color(0xFFFFD700),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        // View Mode Buttons
                        BouncyPressEffect(squishFactor = 0.94f) { modifier, interactionSource ->
                            Button(
                                onClick = {
                                    editName = userName
                                    editAvatarId = if (userPhotoUrl.startsWith("avatar_")) userPhotoUrl else "avatar_1"
                                    customPhotoUri = null
                                    isEditing = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                                shape = RoundedCornerShape(18.dp),
                                modifier = modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                                    .clickable(interactionSource = interactionSource, indication = null) {},
                                contentPadding = PaddingValues(vertical = 14.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(if (isEs) "Editar Perfil" else "Edit Profile", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                            }
                        }

                        BouncyPressEffect(squishFactor = 0.94f) { modifier, interactionSource ->
                            OutlinedButton(
                                onClick = onSignOutClick,
                                border = BorderStroke(1.2.dp, Color(0xFFE94057).copy(alpha = 0.8f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE94057)),
                                shape = RoundedCornerShape(18.dp),
                                modifier = modifier
                                    .fillMaxWidth()
                                    .clickable(interactionSource = interactionSource, indication = null) {},
                                contentPadding = PaddingValues(vertical = 14.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.Logout, contentDescription = null, tint = Color(0xFFE94057), modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(if (isEs) "Cerrar Sesión" else "Sign Out", color = Color(0xFFE94057), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                            }
                        }
                    }
                }
            } else {
                // EDIT MODE
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isEs) "Editar tu Perfil" else "Edit Your Profile",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                        color = OnBackground,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Avatar preview bubble with glow border
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .pulseAnimation(durationMillis = 2500, scaleRange = 0.04f),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(Primary, Secondary)
                                    )
                                )
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(3.dp)
                                .background(Surface, CircleShape)
                        )

                        if (customPhotoUri != null) {
                            AsyncImage(
                                model = customPhotoUri,
                                contentDescription = "Selected Local Image",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(6.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            UserAvatar(
                                photoUrl = if (editAvatarId.startsWith("avatar_")) editAvatarId else userPhotoUrl,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(6.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = if (isEs) "Elige tu avatar o sube una foto:" else "Choose your avatar or upload a photo:",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = TextMuted,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )

                    // Predefined Chef Avatars Row with scale highlighting
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        chefAvatars.forEach { avatar ->
                            val isSelected = editAvatarId == avatar.id && customPhotoUri == null
                            val avatarScale by animateFloatAsState(
                                targetValue = if (isSelected) 1.25f else 1.0f,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                                label = "avatarScale"
                            )
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .graphicsLayer(scaleX = avatarScale, scaleY = avatarScale)
                                    .clip(CircleShape)
                                    .clickable {
                                        editAvatarId = avatar.id
                                        customPhotoUri = null
                                    }
                                    .border(
                                        width = if (isSelected) 2.5.dp else 0.dp,
                                        color = if (isSelected) Primary else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .padding(if (isSelected) 2.dp else 0.dp)
                            ) {
                                ChefAvatar(avatarId = avatar.id, modifier = Modifier.fillMaxSize())
                            }
                        }
                    }

                    // Gallery Picker button
                    BouncyPressEffect(squishFactor = 0.94f) { modifier, interactionSource ->
                        OutlinedButton(
                            onClick = {
                                imagePickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            border = BorderStroke(1.2.dp, Primary.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            modifier = modifier
                                .padding(bottom = 20.dp)
                                .clickable(interactionSource = interactionSource, indication = null) {}
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = if (customPhotoUri != null) {
                                        if (isEs) "¡Foto seleccionada! 📷" else "Photo selected! 📷"
                                    } else {
                                        if (isEs) "Subir mi propia foto 📷" else "Upload my own photo 📷"
                                    },
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = Primary
                                )
                            }
                        }
                    }

                    // Display Name input
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text(if (isEs) "Nombre de Chef" else "Chef Name") },
                        leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null, tint = Primary) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = Primary.copy(alpha = 0.3f),
                            focusedLabelColor = Primary
                        ),
                        singleLine = true
                    )

                    // Save and Cancel buttons
                    BouncyPressEffect(squishFactor = 0.94f) { modifier, interactionSource ->
                        Button(
                            onClick = {
                                if (editName.trim().isNotEmpty()) {
                                    val uri = customPhotoUri
                                    if (uri != null) {
                                        onUploadCustomPhoto(editName, uri)
                                    } else {
                                        onUpdateProfile(editName, editAvatarId)
                                    }
                                    isEditing = false
                                }
                            },
                            enabled = editName.trim().isNotEmpty() && !isUploading,
                            colors = ButtonDefaults.buttonColors(containerColor = Primary),
                            shape = RoundedCornerShape(18.dp),
                            modifier = modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .clickable(interactionSource = interactionSource, indication = null) {},
                            contentPadding = PaddingValues(vertical = 14.dp)
                        ) {
                            if (isUploading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                            } else {
                                Text(if (isEs) "Guardar Cambios" else "Save Changes", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }

                    BouncyPressEffect(squishFactor = 0.94f) { modifier, interactionSource ->
                        OutlinedButton(
                            onClick = { isEditing = false },
                            enabled = !isUploading,
                            border = BorderStroke(1.2.dp, Primary.copy(alpha = 0.3f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary),
                            shape = RoundedCornerShape(18.dp),
                            modifier = modifier
                                .fillMaxWidth()
                                .clickable(interactionSource = interactionSource, indication = null) {},
                            contentPadding = PaddingValues(vertical = 14.dp)
                        ) {
                            Text(if (isEs) "Cancelar" else "Cancel", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RowScope.StatCard(
    icon: ImageVector,
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    BouncyPressEffect(squishFactor = 0.90f) { bouncyModifier, interactionSource ->
        Surface(
            modifier = modifier
                .then(bouncyModifier)
                .clickable(interactionSource = interactionSource, indication = null) {},
            shape = RoundedCornerShape(18.dp),
            color = Surface.copy(alpha = 0.5f),
            border = BorderStroke(1.dp, Primary.copy(alpha = 0.15f))
        ) {
            Column(
                modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                    color = OnBackground,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                    color = TextMuted,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
