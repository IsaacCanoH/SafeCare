package mx.utng.ich.safecare.wearable.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import androidx.wear.compose.ui.tooling.preview.WearPreviewFontScales
import mx.utng.ich.safecare.designsystem.theme.errorLight
import mx.utng.ich.safecare.designsystem.theme.onErrorLight
import mx.utng.ich.safecare.wearable.presentation.theme.SafeCareTheme

@Composable
fun WearHomeScreen(
    uiState: WearHomeUiState = WearHomeUiState(),
    onGetStatus: () -> Unit = {}
) {
    SafeCareTheme {
        AppScaffold {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(155.dp)
                            .clip(CircleShape)
                            .background(errorLight)
                            .clickable {
                                onGetStatus()
                            },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = uiState.panicButtonText,
                        color = onErrorLight,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@WearPreviewDevices
@WearPreviewFontScales
@Composable
fun WearHomeScreenPreview() {
    WearHomeScreen()
}