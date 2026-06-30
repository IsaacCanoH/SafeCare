package mx.utng.ich.safecare.wearable.presentation.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import mx.utng.ich.safecare.designsystem.theme.backgroundLight
import mx.utng.ich.safecare.designsystem.theme.errorContainerLight
import mx.utng.ich.safecare.designsystem.theme.errorLightMediumContrast
import mx.utng.ich.safecare.designsystem.theme.onBackgroundLight
import mx.utng.ich.safecare.designsystem.theme.onErrorContainerLight
import mx.utng.ich.safecare.designsystem.theme.onPrimaryLight
import mx.utng.ich.safecare.designsystem.theme.primaryContainerLight
import mx.utng.ich.safecare.designsystem.theme.primaryLightMediumContrast
import mx.utng.ich.safecare.designsystem.theme.surfaceContainerLowestLight
import mx.utng.ich.safecare.wearable.presentation.theme.SafeCareTheme

@Composable
fun WearAlertScreen(
    message: String = "Saliste de zona segura",
    address: String = "Zona segura",
    onDismiss: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    val isSafeZoneExit = message == "Saliste de zona segura"
    val titleText = if (isSafeZoneExit) {
        "Saliste de\nzona segura"
    } else {
        message
    }
    val bodyText = if (isSafeZoneExit) {
        "Hemos detectado que\nsaliste de la zona segura."
    } else {
        "Revisa la zona segura configurada."
    }

    SafeCareTheme {
        AppScaffold {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                backgroundLight,
                                errorContainerLight.copy(alpha = 0.62f),
                                surfaceContainerLowestLight
                            )
                        )
                    )
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                onDismiss()
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    AlertShieldIcon(
                        modifier = Modifier.size(48.dp),
                        color = errorLightMediumContrast
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = titleText,
                        color = onErrorContainerLight,
                        style = MaterialTheme.typography.titleLarge,
                        fontSize = 20.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = bodyText,
                        color = onBackgroundLight,
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        lineHeight = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    AddressPill(address = address)

                    Spacer(modifier = Modifier.height(14.dp))

                    DismissButton(onDismiss = onDismiss)
                }
            }
        }
    }
}

@Composable
private fun AlertShieldIcon(
    modifier: Modifier = Modifier,
    color: Color
) {
    Canvas(modifier = modifier) {
        val strokeWidth = 3.dp.toPx()
        val shieldPath = Path().apply {
            moveTo(size.width * 0.5f, size.height * 0.08f)
            lineTo(size.width * 0.82f, size.height * 0.2f)
            lineTo(size.width * 0.82f, size.height * 0.48f)
            quadraticTo(
                size.width * 0.82f,
                size.height * 0.72f,
                size.width * 0.5f,
                size.height * 0.9f
            )
            quadraticTo(
                size.width * 0.18f,
                size.height * 0.72f,
                size.width * 0.18f,
                size.height * 0.48f
            )
            lineTo(size.width * 0.18f, size.height * 0.2f)
            close()
        }

        drawPath(
            path = shieldPath,
            color = color,
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
        drawLine(
            color = color,
            start = Offset(size.width * 0.5f, size.height * 0.36f),
            end = Offset(size.width * 0.5f, size.height * 0.56f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawCircle(
            color = color,
            radius = strokeWidth * 0.8f,
            center = Offset(size.width * 0.5f, size.height * 0.68f)
        )
    }
}

@Composable
private fun AddressPill(address: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(primaryContainerLight.copy(alpha = 0.42f))
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(primaryLightMediumContrast),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = onPrimaryLight,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = address,
            color = onBackgroundLight,
            fontSize = 10.sp,
            lineHeight = 12.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun DismissButton(onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(primaryLightMediumContrast)
            .clickable {
                onDismiss()
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Entendido",
            color = onPrimaryLight,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
    }
}

@WearPreviewDevices
@Composable
fun WearAlertScreenPreview() {
    WearAlertScreen(
        address = "Av. Siempre Viva 123, Col. Centro, Ciudad"
    )
}
