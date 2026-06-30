package mx.utng.ich.safecare.wearable.presentation.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import androidx.wear.compose.ui.tooling.preview.WearPreviewFontScales
import mx.utng.ich.safecare.designsystem.theme.backgroundLight
import mx.utng.ich.safecare.designsystem.theme.onPrimaryLight
import mx.utng.ich.safecare.designsystem.theme.onSurfaceLight
import mx.utng.ich.safecare.designsystem.theme.outlineVariantLight
import mx.utng.ich.safecare.designsystem.theme.primaryContainerLight
import mx.utng.ich.safecare.designsystem.theme.primaryLight
import mx.utng.ich.safecare.designsystem.theme.primaryLightMediumContrast
import mx.utng.ich.safecare.designsystem.theme.surfaceContainerLowLight
import mx.utng.ich.safecare.designsystem.theme.surfaceContainerLowestLight
import mx.utng.ich.safecare.wearable.presentation.theme.SafeCareTheme

@Composable
fun WearHomeScreen(
    uiState: WearHomeUiState = WearHomeUiState(),
    onPanicButtonLongPress: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    val infiniteTransition = rememberInfiniteTransition(label = "SosPulse")

    val ringScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SosRingScale"
    )

    val ringAlpha by infiniteTransition.animateFloat(
        initialValue = 0.28f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SosRingAlpha"
    )

    SafeCareTheme {
        AppScaffold {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(surfaceContainerLowestLight, backgroundLight)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Bot\u00f3n de p\u00e1nico",
                        style = MaterialTheme.typography.titleSmall,
                        color = onSurfaceLight,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    SosPulseButton(
                        text = uiState.panicButtonText,
                        ringScale = ringScale,
                        ringAlpha = ringAlpha,
                        onLongPress = onPanicButtonLongPress
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    PanicInstructions()

                    Spacer(modifier = Modifier.height(12.dp))

                    StatusPill()
                }
            }
        }
    }
}

@Composable
private fun SosPulseButton(
    text: String,
    ringScale: Float,
    ringAlpha: Float,
    onLongPress: () -> Unit
) {
    Box(
        modifier = Modifier.size(126.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(114.dp)
                .clip(CircleShape)
                .background(primaryContainerLight.copy(alpha = 0.36f))
        )

        Box(
            modifier = Modifier
                .size(94.dp)
                .clip(CircleShape)
                .background(primaryContainerLight.copy(alpha = 0.72f))
        )

        Box(
            modifier = Modifier
                .size(82.dp)
                .graphicsLayer {
                    scaleX = ringScale
                    scaleY = ringScale
                    alpha = ringAlpha
                }
                .clip(CircleShape)
                .background(primaryLight)
        )

        Box(
            modifier = Modifier
                .size(78.dp)
                .shadow(6.dp, CircleShape)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(primaryLight, primaryLightMediumContrast)
                    )
                )
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = {
                            onLongPress()
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = onPrimaryLight,
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun PanicInstructions() {
    Text(
        text = buildAnnotatedString {
            append("Presiona y mant\u00e9n\npresionado ")
            withStyle(
                SpanStyle(
                    color = primaryLightMediumContrast,
                    fontWeight = FontWeight.Bold
                )
            ) {
                append("3 segundos")
            }
            append("\npara enviar alerta")
        },
        style = MaterialTheme.typography.bodySmall,
        color = onSurfaceLight,
        textAlign = TextAlign.Center,
        fontSize = 11.sp,
        lineHeight = 13.sp
    )
}

@Composable
private fun StatusPill() {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(surfaceContainerLowLight)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Wifi,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = primaryLightMediumContrast
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "En l\u00ednea",
                fontSize = 10.sp,
                color = onSurfaceLight,
                fontWeight = FontWeight.Medium
            )
        }

        Box(
            modifier = Modifier
                .width(1.dp)
                .height(14.dp)
                .background(outlineVariantLight)
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.BatteryFull,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = onSurfaceLight
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "85%",
                fontSize = 10.sp,
                color = onSurfaceLight,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@WearPreviewDevices
@WearPreviewFontScales
@Composable
fun WearHomeScreenPreview() {
    WearHomeScreen()
}
