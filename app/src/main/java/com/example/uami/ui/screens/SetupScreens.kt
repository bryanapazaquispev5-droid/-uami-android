package com.example.uami.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.uami.R
import com.example.uami.ui.theme.*

@Composable
fun PreparingDataScreen(
    lang: String,
    progress: Float,
    statusText: String,
    isFailed: Boolean = false,
    errorMessage: String = "",
    onRetry: () -> Unit = {}
) {
    val isEs = lang == "es"
    val infiniteTransition = rememberInfiniteTransition(label = "loading")

    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val offsetY by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offset"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = listOf(Background, Surface))),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            Box(contentAlignment = Alignment.TopCenter) {
                if (!isFailed) {
                    repeat(3) { i ->
                        val steamAlpha by infiniteTransition.animateFloat(
                            initialValue = 0.6f, targetValue = 0f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1500, delayMillis = i * 500, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            ), label = "steamAlpha$i"
                        )
                        val steamOffset by infiniteTransition.animateFloat(
                            initialValue = 0f, targetValue = -100f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1500, delayMillis = i * 500, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            ), label = "steamOffset$i"
                        )
                        Box(
                            modifier = Modifier
                                .offset(y = steamOffset.dp, x = (i * 20 - 20).dp)
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = steamAlpha))
                        )
                    }
                }
                Image(
                    painter = painterResource(id = R.drawable.ic_chef_loading),
                    contentDescription = null,
                    modifier = Modifier
                        .size(160.dp)
                        .graphicsLayer(scaleX = scale, scaleY = scale, translationY = offsetY)
                )
            }
            
            Spacer(Modifier.height(40.dp))
            
            if (isFailed) {
                Surface(
                    color = Color.Red.copy(alpha = 0.1f), shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f)), modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.ErrorOutline, contentDescription = "Error", tint = Color.Red)
                        Spacer(Modifier.width(12.dp))
                        Text(text = errorMessage, color = Color.Red, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = onRetry, modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null, tint = OnPrimary)
                    Spacer(Modifier.width(8.dp))
                    Text(if (isEs) "REINTENTAR" else "RETRY", color = OnPrimary, fontWeight = FontWeight.Bold)
                }
            } else {
                Text(text = "${(progress * 100).toInt()}%", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold), color = Primary)
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(progress = { progress }, color = Primary, trackColor = Surface, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(10.dp)))
                Spacer(Modifier.height(24.dp))
                Text(text = statusText.ifEmpty { if (isEs) "Conectando..." else "Connecting..." }, style = MaterialTheme.typography.titleMedium, color = OnBackground, textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                Text(if (isEs) "Sincronizando menú offline" else "Syncing offline menu", style = MaterialTheme.typography.bodyMedium, color = TextMuted, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun LanguageSelectionScreen(currentLanguage: MutableState<String>, errorMessage: String = "", onContinue: () -> Unit) {
    var selectedTemp by remember { mutableStateOf("") }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Rounded.Language,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = Primary
            )
            Spacer(Modifier.height(24.dp))
            Text(
                "Welcome / Bienvenido",
                style = MaterialTheme.typography.headlineMedium,
                color = OnBackground,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Choose your language / Elige tu idioma",
                style = MaterialTheme.typography.bodyLarge,
                color = TextMuted
            )
            
            if (errorMessage.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
                Surface(
                    color = Color.Red.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.ErrorOutline, contentDescription = "Error", tint = Color.Red)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = errorMessage,
                            color = Color.Red,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            Spacer(Modifier.height(32.dp))
            
            // Opción Inglés
            Surface(
                onClick = { selectedTemp = "en" },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = if (selectedTemp == "en") Primary.copy(alpha = 0.1f) else Surface,
                border = if (selectedTemp == "en") BorderStroke(2.dp, Primary) else null
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (selectedTemp == "en"),
                        onClick = { selectedTemp = "en" },
                        colors = RadioButtonDefaults.colors(selectedColor = Primary)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("English", color = OnBackground, fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            // Opción Español
            Surface(
                onClick = { selectedTemp = "es" },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = if (selectedTemp == "es") Primary.copy(alpha = 0.1f) else Surface,
                border = if (selectedTemp == "es") BorderStroke(2.dp, Primary) else null
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (selectedTemp == "es"),
                        onClick = { selectedTemp = "es" },
                        colors = RadioButtonDefaults.colors(selectedColor = Primary)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Español", color = OnBackground, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(48.dp))

            Button(
                onClick = { 
                    if (selectedTemp.isNotEmpty()) {
                        currentLanguage.value = selectedTemp
                        onContinue()
                    }
                },
                enabled = selectedTemp.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary,
                    disabledContainerColor = Surface
                )
            ) {
                Text(
                    if (selectedTemp == "es") "CONTINUAR" else "CONTINUE", 
                    color = if (selectedTemp.isNotEmpty()) OnPrimary else TextMuted,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}