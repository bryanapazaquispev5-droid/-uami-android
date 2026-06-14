package com.example.uami.utils

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.PagerState

@OptIn(ExperimentalFoundationApi::class)
fun Modifier.pageCurlTransition(
    page: Int,
    pagerState: PagerState
): Modifier = graphicsLayer {
    val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
    
    if (pageOffset < -1.5f || pageOffset > 1.5f) {
        alpha = 0f
    } else {
        // Cancel default horizontal pager scroll offset
        val spacingPx = with(this) { 16.dp.toPx() }
        translationX = -pageOffset * (size.width + spacingPx)
        
        if (pageOffset > 0f) {
            // Page is exiting / peeling down (moving forward)
            // Pivot around top-left corner to simulate top-left tape/pin
            transformOrigin = TransformOrigin(0f, 0f)
            
            // Swing down clockwise
            rotationZ = pageOffset * 55f
            
            // Translate/peel downwards
            translationY = pageOffset * size.height * 1.25f
            
            // Fade out as it curls away
            alpha = (1f - pageOffset * 1.5f).coerceIn(0f, 1f)
            
            // Scale down slightly
            val scale = 1f - pageOffset * 0.15f
            scaleX = scale
            scaleY = scale
        } else if (pageOffset < 0f) {
            // Page is entering from behind
            val progress = 1f + pageOffset // 0 to 1
            
            scaleX = 0.88f + progress * 0.12f
            scaleY = 0.88f + progress * 0.12f
            alpha = (0.4f + progress * 0.6f).coerceIn(0f, 1f)
            translationY = 0f
            rotationZ = 0f
        } else {
            scaleX = 1f
            scaleY = 1f
            alpha = 1f
            translationY = 0f
            rotationZ = 0f
        }
    }
}

@Composable
fun BouncyPressEffect(
    squishFactor: Float = 0.76f, // 24% scale reduction
    content: @Composable (Modifier, MutableInteractionSource) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    // Jelly effect: when pressed, squishes horizontally (shrinks) and stretches vertically (grows)
    val scaleX by animateFloatAsState(
        targetValue = if (isPressed) squishFactor else 1.0f,
        animationSpec = spring(
            dampingRatio = 0.42f, // Bouncy spring
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "pressScaleX"
    )
    val scaleY by animateFloatAsState(
        targetValue = if (isPressed) 1.0f + (1.0f - squishFactor) * 0.4f else 1.0f,
        animationSpec = spring(
            dampingRatio = 0.38f, // Extra springy offset
            stiffness = Spring.StiffnessMedium
        ),
        label = "pressScaleY"
    )
    content(Modifier.graphicsLayer(scaleX = scaleX, scaleY = scaleY), interactionSource)
}

fun Modifier.pulseAnimation(durationMillis: Int = 1000, scaleRange: Float = 0.25f): Modifier = composed {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.0f + scaleRange,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    graphicsLayer(scaleX = scale, scaleY = scale)
}

fun Modifier.heartbeatAnimation(animDuration: Int = 1200): Modifier = composed {
    val infiniteTransition = rememberInfiniteTransition(label = "heartbeat")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.28f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = animDuration
                1.0f at 0 with FastOutSlowInEasing
                1.28f at (animDuration * 0.18f).toInt() with FastOutSlowInEasing
                1.08f at (animDuration * 0.36f).toInt() with FastOutSlowInEasing
                1.28f at (animDuration * 0.54f).toInt() with FastOutSlowInEasing
                1.0f at (animDuration * 0.80f).toInt() with FastOutSlowInEasing
                1.0f at animDuration with FastOutSlowInEasing
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "scale"
    )
    graphicsLayer(scaleX = scale, scaleY = scale)
}

fun Modifier.bobbingAnimation(durationMillis: Int = 1500, dy: Float = 12f): Modifier = composed {
    val infiniteTransition = rememberInfiniteTransition(label = "bob")
    val translationY by infiniteTransition.animateFloat(
        initialValue = -dy,
        targetValue = dy,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "translationY"
    )
    graphicsLayer(translationY = translationY)
}

fun Modifier.horizontalSlideAnimation(durationMillis: Int = 1200, dx: Float = 8f): Modifier = composed {
    val infiniteTransition = rememberInfiniteTransition(label = "slide")
    val translationX by infiniteTransition.animateFloat(
        initialValue = -dx,
        targetValue = dx,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "translationX"
    )
    graphicsLayer(translationX = translationX)
}

fun Modifier.shakeWobbleAnimation(animDuration: Int = 1500, maxRotation: Float = 14f): Modifier = composed {
    val infiniteTransition = rememberInfiniteTransition(label = "shakeWobble")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = animDuration
                0f at 0 with FastOutLinearInEasing
                -maxRotation at (animDuration * 0.12f).toInt() with FastOutSlowInEasing
                maxRotation at (animDuration * 0.24f).toInt() with FastOutSlowInEasing
                -maxRotation at (animDuration * 0.36f).toInt() with FastOutSlowInEasing
                maxRotation at (animDuration * 0.48f).toInt() with FastOutSlowInEasing
                0f at (animDuration * 0.60f).toInt() with FastOutSlowInEasing
                0f at animDuration with FastOutSlowInEasing
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    graphicsLayer(rotationZ = rotation)
}

enum class ParticleType {
    CIRCLE,
    HEART,
    SQUARE
}

// Particle class to describe explosive elements
data class RecipeParticle(
    val angle: Double,
    val speed: Float,
    val color: Color,
    val delay: Int,
    val size: Int,
    val type: ParticleType
)

@Composable
fun HeartBurstButton(
    isFav: Boolean,
    onFavToggle: () -> Unit,
    modifier: Modifier = Modifier,
    iconSize: Dp = 20.dp,
    activeColor: Color = Color.Red,
    inactiveColor: Color = Color.White,
    backgroundColor: Color = Color.Transparent // Optional circular background behind the icon
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    // Jelly physics for the heartbeat favorite button click
    val scaleX by animateFloatAsState(
        targetValue = if (isPressed) 0.70f else if (isFav) 1.25f else 1.0f,
        animationSpec = spring(dampingRatio = 0.42f, stiffness = Spring.StiffnessMediumLow),
        label = "burstX"
    )
    val scaleY by animateFloatAsState(
        targetValue = if (isPressed) 1.30f else if (isFav) 1.25f else 1.0f,
        animationSpec = spring(dampingRatio = 0.38f, stiffness = Spring.StiffnessMedium),
        label = "burstY"
    )
    
    // Particle burst state (only trigger manually, ignore initial composition load)
    var triggerBurst by remember { mutableStateOf(false) }
    var isFirstComposition by remember { mutableStateOf(true) }
    LaunchedEffect(isFav) {
        if (isFirstComposition) {
            isFirstComposition = false
            return@LaunchedEffect
        }
        if (isFav) {
            triggerBurst = true
            delay(1400) // Increased timeout to let slow particles fade out completely
            triggerBurst = false
        }
    }
    
    // Dense, festive, and colorful gourmet confetti particles
    val particles = remember {
        List(28) {
            val angle = Math.random() * 2 * Math.PI
            val speed = 35f + (Math.random() * 85f).toFloat() // Disperse much further out!
            val color = when ((0..4).random()) {
                0 -> Color(0xFFFF2A6D) // Hot neon rose
                1 -> Color(0xFF05D9E8) // Bright turqouise cyan
                2 -> Color(0xFFFFD200) // Sun gold yellow
                3 -> Color(0xFF9B51E0) // Bright neón violet
                else -> Color(0xFFFF5E3A) // Neon coral/orange
            }
            val size = (5..10).random()
            val type = when ((0..2).random()) {
                0 -> ParticleType.CIRCLE
                1 -> ParticleType.HEART
                else -> ParticleType.SQUARE
            }
            RecipeParticle(angle, speed, color, (Math.random() * 150).toInt(), size, type)
        }
    }
    
    Box(
        modifier = modifier
            .graphicsLayer(scaleX = scaleX, scaleY = scaleY)
            .then(
                if (backgroundColor != Color.Transparent) {
                    Modifier.background(backgroundColor, CircleShape)
                } else Modifier
            )
            .clickable(interactionSource = interactionSource, indication = null) {
                onFavToggle()
            },
        contentAlignment = Alignment.Center
    ) {
        // Particle burst rendering
        if (triggerBurst) {
            particles.forEach { p ->
                val animState = remember { Animatable(0f) }
                LaunchedEffect(triggerBurst) {
                    animState.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(
                            durationMillis = 1150, // Much slower animation to appreciate float and heart shapes
                            delayMillis = p.delay,
                            easing = FastOutSlowInEasing
                        )
                    )
                }
                
                val progress = animState.value
                if (progress < 1f) {
                    // Confetti physics: Radial dispersion + gravity drop over progress
                    val distance = p.speed * progress
                    val dx = (distance * Math.cos(p.angle)).toFloat()
                    val gravityOffset = 30f * progress * progress // Falls downward over time
                    val dy = (distance * Math.sin(p.angle)).toFloat() + gravityOffset
                    
                    val pScale = (1f - progress) * 1.6f
                    val alpha = 1f - progress
                    
                    // Spin particles in 3D space for high-end shimmer sparkles
                    val spinZ = progress * (360f + p.speed * 3f)
                    val spinX = progress * (180f + p.speed * 2f)
                    
                    val particleModifier = Modifier
                        .offset(x = dx.dp, y = dy.dp)
                        .graphicsLayer(
                            scaleX = pScale, 
                            scaleY = pScale, 
                            alpha = alpha,
                            rotationZ = spinZ,
                            rotationX = spinX
                        )
                    
                    when (p.type) {
                        ParticleType.HEART -> {
                            Icon(
                                imageVector = Icons.Rounded.Favorite,
                                contentDescription = null,
                                tint = p.color,
                                modifier = particleModifier.size(p.size.dp)
                            )
                        }
                        ParticleType.SQUARE -> {
                            Box(
                                modifier = particleModifier
                                    .size(p.size.dp)
                                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(1.5.dp))
                                    .background(p.color)
                            )
                        }
                        ParticleType.CIRCLE -> {
                            Box(
                                modifier = particleModifier
                                    .size(p.size.dp)
                                    .clip(CircleShape)
                                    .background(p.color)
                            )
                        }
                    }
                }
            }
        }
        
        Icon(
            imageVector = if (isFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
            contentDescription = "Favorito",
            tint = if (isFav) activeColor else inactiveColor,
            modifier = Modifier
                .size(iconSize)
                .then(if (isFav) Modifier.heartbeatAnimation(animDuration = 1000) else Modifier)
        )
    }
}

// Custom metallic/holographic diagonal sweep sheen modifier for awaiting interaction
fun Modifier.shimmerGlow(
    durationMillis: Int = 2200,
    glowColor: Color = Color.White.copy(alpha = 0.28f)
): Modifier = composed {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmerGlow")
    
    val progress by infiniteTransition.animateFloat(
        initialValue = -0.6f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )
    
    drawWithContent {
        drawContent() // Draw the actual composable
        
        val width = size.width
        val height = size.height
        val xOffset = width * progress
        
        val gradientBrush = Brush.linearGradient(
            colors = listOf(
                Color.Transparent,
                glowColor,
                Color.Transparent
            ),
            start = Offset(xOffset - 120f, 0f),
            end = Offset(xOffset + 120f, height)
        )
        
        drawRect(brush = gradientBrush)
    }
}
