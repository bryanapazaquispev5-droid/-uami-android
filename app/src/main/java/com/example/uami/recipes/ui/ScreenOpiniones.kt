package com.example.uami.recipes.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.uami.recipes.models.ReviewModel
import com.example.uami.recipes.viewmodel.ReviewsViewModel
import com.example.uami.recipes.viewmodel.ViewModelFactory
import com.example.uami.ui.theme.Background
import com.example.uami.ui.theme.Primary
import com.example.uami.ui.theme.Surface
import com.example.uami.ui.theme.TextMuted
import java.text.SimpleDateFormat
import java.util.*

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
                    text = if (isEs) "Comentarios de los Chefs" else "Chefs' Comments",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(vertical = 8.dp)
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
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SignInRequiredCard(isEs: Boolean, onSignInClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Surface,
        border = BorderStroke(1.dp, Primary.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Rounded.RateReview,
                contentDescription = "Review",
                tint = Primary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = if (isEs) "¿Quieres dejar tu opinión? ⭐" else "Want to share your feedback? ⭐",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (isEs) {
                    "Inicia sesión con tu cuenta de Chef para poder calificar con estrellas y escribir un comentario."
                } else {
                    "Sign in with your Chef account to give a star rating and leave a review."
                },
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onSignInClick,
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Login, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (isEs) "Iniciar Sesión de Chef" else "Sign In as Chef",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
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
        shape = RoundedCornerShape(24.dp),
        color = Surface,
        border = BorderStroke(1.dp, Primary.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = if (isEs) "¿Qué te parece Uami? ⭐" else "What do you think of Uami? ⭐",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(12.dp))

            // Star Rating Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                for (i in 1..5) {
                    Icon(
                        imageVector = if (i <= rating) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                        contentDescription = "$i Stars",
                        tint = if (i <= rating) Primary else TextMuted,
                        modifier = Modifier
                            .size(36.dp)
                            .clickable { rating = i }
                            .padding(horizontal = 4.dp)
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

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
            Spacer(Modifier.height(12.dp))

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
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                if (isPosting) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                } else {
                    Text(
                        text = if (isEs) "Publicar Comentario" else "Post Comment",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun ReviewItem(
    review: ReviewModel,
    currentUserId: String,
    onDeleteClick: () -> Unit
) {
    val dateString = remember(review.timestamp) {
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        sdf.format(Date(review.timestamp))
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Surface.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, Primary.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                UserAvatar(
                    photoUrl = review.userPhotoUrl,
                    modifier = Modifier.size(40.dp)
                )

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = review.userName,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = dateString,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                        color = TextMuted
                    )
                }

                // Star rating representation
                Row {
                    for (i in 1..5) {
                        Icon(
                            imageVector = if (i <= review.rating) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                            contentDescription = null,
                            tint = if (i <= review.rating) Primary else TextMuted.copy(alpha = 0.3f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            Text(
                text = review.comment,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            // Delete option for own comments
            if (review.userId == currentUserId) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = onDeleteClick, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = "Eliminar",
                            tint = Color.Red.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyReviewsCard(isEs: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Surface.copy(alpha = 0.3f),
        border = BorderStroke(1.dp, Primary.copy(alpha = 0.05f))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Rounded.Forum,
                contentDescription = "Empty Reviews",
                tint = TextMuted,
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = if (isEs) "Aún no hay comentarios" else "No comments yet",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = if (isEs) "Sé el primero en calificar y dejar tu opinión." else "Be the first to rate and leave your opinion.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}
