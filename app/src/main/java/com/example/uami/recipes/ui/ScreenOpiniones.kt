package com.example.uami.recipes.ui

import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.uami.recipes.models.ReviewModel
import com.example.uami.recipes.viewmodel.ReviewsViewModel
import com.example.uami.ui.theme.Background
import com.example.uami.ui.theme.Primary
import com.example.uami.ui.theme.Secondary
import com.example.uami.ui.theme.Surface
import com.example.uami.ui.theme.TextMuted
import com.example.uami.ui.theme.OnBackground
import com.example.uami.utils.BouncyPressEffect
import com.example.uami.utils.shimmerGlow
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@Composable
fun ScreenOpiniones(
    onBack: () -> Unit,
    onNavigateToLogin: () -> Unit,
    currentLanguage: MutableState<String>,
    reviewsViewModel: ReviewsViewModel
) {
    val currentUser by reviewsViewModel.currentUser.collectAsState()
    val reviews by reviewsViewModel.reviews.collectAsState()
    val isLoadingReviews by reviewsViewModel.isLoadingReviews.collectAsState()
    val isPosting by reviewsViewModel.isPosting.collectAsState()

    val context = LocalContext.current
    val isEs = currentLanguage.value == "es"

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
                text = if (isEs) "Opiniones de la Comunidad" else "Community Reviews",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- RATING SUMMARY DASHBOARD ---
            if (reviews.isNotEmpty()) {
                item {
                    CommunitySummaryCard(isEs = isEs, reviews = reviews)
                }
            }

            // --- ACTION FORM / PROMPT ---
            item {
                if (currentUser == null) {
                    SignInRequiredCard(
                        isEs = isEs,
                        onSignInClick = onNavigateToLogin
                    )
                } else {
                    WriteReviewForm(
                        isEs = isEs,
                        isPosting = isPosting,
                        onPostReview = { comment, rating ->
                            reviewsViewModel.postReview(
                                comment = comment,
                                rating = rating,
                                onSuccess = {
                                    Toast.makeText(context, if (isEs) "¡Reseña publicada!" else "Review posted!", Toast.LENGTH_SHORT).show()
                                },
                                onFailure = { e ->
                                    Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                }
                            )
                        }
                    )
                }
            }

            // --- REVIEWS TITLE ---
            item {
                Text(
                    text = if (isEs) "Comentarios de los Chefs 🍳" else "Chefs' Comments 🍳",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // --- REVIEWS LIST ---
            if (isLoadingReviews && reviews.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(150.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Primary)
                    }
                }
            } else if (reviews.isEmpty()) {
                item {
                    EmptyReviewsCard(isEs = isEs)
                }
            } else {
                items(reviews, key = { it.id }) { review ->
                    ReviewItem(
                        review = review,
                        currentUserId = currentUser?.uid ?: "",
                        isEs = isEs,
                        onDeleteClick = {
                            reviewsViewModel.deleteReview(
                                reviewId = review.id,
                                onSuccess = {
                                    Toast.makeText(context, if (isEs) "Reseña eliminada" else "Review deleted", Toast.LENGTH_SHORT).show()
                                },
                                onFailure = { e ->
                                    Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                }
                            )
                        },
                        onLikeClick = {
                            if (currentUser != null) {
                                reviewsViewModel.toggleLikeReview(review.id, currentUser!!.uid)
                            } else {
                                Toast.makeText(context, if (isEs) "Inicia sesión para reaccionar" else "Sign in to react", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CommunitySummaryCard(isEs: Boolean, reviews: List<ReviewModel>) {
    val averageRating = remember(reviews) {
        if (reviews.isEmpty()) 0f else reviews.map { it.rating }.average().toFloat()
    }
    val count = reviews.size

    var triggerAnim by remember { mutableStateOf(false) }
    LaunchedEffect(reviews) {
        triggerAnim = true
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .shimmerGlow(durationMillis = 4000),
        shape = RoundedCornerShape(28.dp),
        color = Surface,
        border = BorderStroke(1.dp, Primary.copy(alpha = 0.15f)),
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = String.format(Locale.getDefault(), "%.1f", averageRating),
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Black
                    ),
                    color = Primary
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    for (i in 1..5) {
                        val active = i <= averageRating.roundToInt()
                        Icon(
                            imageVector = Icons.Rounded.Star,
                            contentDescription = null,
                            tint = if (active) Color(0xFFFFD700) else TextMuted.copy(alpha = 0.3f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                Text(
                    text = if (isEs) "$count opiniones" else "$count reviews",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }

            Divider(
                modifier = Modifier
                    .height(80.dp)
                    .width(1.dp)
                    .padding(horizontal = 8.dp),
                color = Primary.copy(alpha = 0.1f)
            )

            // Ratings distribution bars
            Column(
                modifier = Modifier
                    .weight(1.5f)
                    .padding(start = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (star in 5 downTo 1) {
                    val starCount = reviews.count { it.rating == star }
                    val ratio = if (count == 0) 0f else starCount.toFloat() / count

                    val animatedRatio by animateFloatAsState(
                        targetValue = if (triggerAnim) ratio else 0f,
                        animationSpec = tween(durationMillis = 800, easing = LinearOutSlowInEasing),
                        label = "starRatio_$star"
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "$star",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = OnBackground,
                            modifier = Modifier.width(12.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Rounded.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(8.dp))

                        // Progress bar
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(CircleShape)
                                .background(Primary.copy(alpha = 0.1f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(animatedRatio)
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(Primary, Secondary)
                                        ),
                                        CircleShape
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SignInRequiredCard(isEs: Boolean, onSignInClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = Surface,
        border = BorderStroke(1.dp, Primary.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Rounded.RateReview,
                contentDescription = "Review",
                tint = Primary,
                modifier = Modifier.size(52.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = if (isEs) "¿Quieres dejar tu opinión? ⭐" else "Want to share your feedback? ⭐",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                color = OnBackground
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (isEs) {
                    "Inicia sesión con tu cuenta de Chef para poder calificar con estrellas y escribir un comentario."
                } else {
                    "Sign in with your Chef account to give a star rating and leave a review."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(20.dp))

            BouncyPressEffect(squishFactor = 0.94f) { modifier, interactionSource ->
                Button(
                    onClick = onSignInClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    shape = RoundedCornerShape(16.dp),
                    modifier = modifier
                        .fillMaxWidth()
                        .clickable(interactionSource = interactionSource, indication = null) {},
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Login, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (isEs) "Iniciar Sesión de Chef" else "Sign In as Chef",
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

@Composable
fun WriteReviewForm(
    isEs: Boolean,
    isPosting: Boolean,
    onPostReview: (String, Int) -> Unit
) {
    var rating by remember { mutableStateOf(5) }
    var comment by remember { mutableStateOf("") }
    val maxChars = 200

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = Surface,
        border = BorderStroke(1.dp, Primary.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = if (isEs) "¿Qué te parece Uami? ⭐" else "What do you think of Uami? ⭐",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                color = OnBackground
            )
            Spacer(Modifier.height(16.dp))

            // Star Rating Selector (Bouncy & Interactive)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                for (i in 1..5) {
                    val isSelected = i <= rating
                    val starScale by animateFloatAsState(
                        targetValue = if (isSelected) 1.25f else 1.0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "starScale"
                    )
                    Icon(
                        imageVector = if (isSelected) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                        contentDescription = "$i Stars",
                        tint = if (isSelected) Color(0xFFFFD700) else TextMuted.copy(alpha = 0.4f),
                        modifier = Modifier
                            .size(38.dp)
                            .graphicsLayer(scaleX = starScale, scaleY = starScale)
                            .clickable { rating = i }
                            .padding(horizontal = 4.dp)
                    )
                }
            }
            Spacer(Modifier.height(20.dp))

            // Comment text area
            OutlinedTextField(
                value = comment,
                onValueChange = { if (it.length <= maxChars) comment = it },
                label = { Text(if (isEs) "Escribe tu opinión sobre la app..." else "Write your app review...") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = Primary.copy(alpha = 0.3f),
                    focusedLabelColor = Primary
                ),
                maxLines = 4,
                supportingText = {
                    Text(
                        text = "${comment.length} / $maxChars",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End,
                        color = if (comment.length >= maxChars) Color.Red else TextMuted
                    )
                }
            )
            Spacer(Modifier.height(16.dp))

            BouncyPressEffect(squishFactor = 0.94f) { modifier, interactionSource ->
                Button(
                    onClick = {
                        if (comment.trim().isNotEmpty()) {
                            onPostReview(comment, rating)
                            comment = ""
                            rating = 5
                        }
                    },
                    enabled = comment.trim().isNotEmpty() && !isPosting,
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    shape = RoundedCornerShape(18.dp),
                    modifier = modifier
                        .fillMaxWidth()
                        .clickable(interactionSource = interactionSource, indication = null) {},
                    contentPadding = PaddingValues(vertical = 14.dp)
                ) {
                    if (isPosting) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                    } else {
                        Text(
                            text = if (isEs) "Publicar Comentario" else "Post Comment",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReviewItem(
    review: ReviewModel,
    currentUserId: String,
    isEs: Boolean,
    onDeleteClick: () -> Unit,
    onLikeClick: () -> Unit
) {
    val dateString = remember(review.timestamp) {
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        sdf.format(Date(review.timestamp))
    }

    // Cascading entrance animation
    val alpha = remember { Animatable(0f) }
    val translateY = remember { Animatable(50f) }
    LaunchedEffect(Unit) {
        alpha.animateTo(1f, animationSpec = tween(500, easing = LinearOutSlowInEasing))
    }
    LaunchedEffect(Unit) {
        translateY.animateTo(0f, animationSpec = tween(500, easing = LinearOutSlowInEasing))
    }

    val isLiked = review.likedBy.contains(currentUserId)
    val heartScale by animateFloatAsState(
        targetValue = if (isLiked) 1.25f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "heartScale"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer(
                alpha = alpha.value,
                translationY = translateY.value
            ),
        shape = RoundedCornerShape(24.dp),
        color = Surface.copy(alpha = 0.7f),
        border = BorderStroke(1.dp, Primary.copy(alpha = 0.12f)),
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // If rating is 5, draw a small glowing halo
                Box(contentAlignment = Alignment.Center) {
                    if (review.rating == 5) {
                        val infiniteTransition = rememberInfiniteTransition(label = "haloRotation")
                        val rotationAngle by infiniteTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = 360f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(6000, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "rotation"
                        )
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.sweepGradient(
                                        listOf(Primary, Secondary, Color(0xFFFFD700), Primary)
                                    )
                                )
                                .graphicsLayer(rotationZ = rotationAngle)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Surface)
                    ) {
                        UserAvatar(
                            photoUrl = review.userPhotoUrl,
                            modifier = Modifier
                                .size(40.dp)
                                .align(Alignment.Center)
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = review.userName,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = OnBackground
                    )
                    Text(
                        text = dateString,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = TextMuted
                    )
                }

                // Star rating representation
                Row {
                    for (i in 1..5) {
                        Icon(
                            imageVector = if (i <= review.rating) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                            contentDescription = null,
                            tint = if (i <= review.rating) Color(0xFFFFD700) else TextMuted.copy(alpha = 0.2f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(
                text = review.comment,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                color = OnBackground
            )

            Spacer(Modifier.height(12.dp))
            Divider(color = Primary.copy(alpha = 0.05f))
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Delete button on the left (if it's the current user's review)
                if (review.userId == currentUserId) {
                    BouncyPressEffect(squishFactor = 0.85f) { modifier, interactionSource ->
                        IconButton(
                            onClick = onDeleteClick,
                            modifier = modifier
                                .size(36.dp)
                                .background(Color.Red.copy(alpha = 0.1f), CircleShape)
                                .clickable(interactionSource = interactionSource, indication = null) { onDeleteClick() }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = "Eliminar",
                                tint = Color.Red.copy(alpha = 0.8f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                } else {
                    Spacer(Modifier.width(1.dp))
                }

                // Heart reaction button on the right
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isLiked) Color.Red.copy(alpha = 0.08f) else Color.Transparent)
                        .clickable { onLikeClick() }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = "Reaccionar",
                        tint = if (isLiked) Color.Red else TextMuted,
                        modifier = Modifier
                            .size(18.dp)
                            .graphicsLayer(scaleX = heartScale, scaleY = heartScale)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (review.likesCount == 1) {
                            if (isEs) "1 me gusta" else "1 like"
                        } else {
                            if (isEs) "${review.likesCount} me gustas" else "${review.likesCount} likes"
                        },
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = if (isLiked) Color.Red else TextMuted
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyReviewsCard(isEs: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Surface.copy(alpha = 0.3f),
        border = BorderStroke(1.dp, Primary.copy(alpha = 0.05f))
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Rounded.Forum,
                contentDescription = "Empty Reviews",
                tint = TextMuted,
                modifier = Modifier.size(52.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = if (isEs) "Aún no hay comentarios" else "No comments yet",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = OnBackground
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (isEs) "Sé el primero en calificar y dejar tu opinión." else "Be the first to rate and leave your opinion.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}
