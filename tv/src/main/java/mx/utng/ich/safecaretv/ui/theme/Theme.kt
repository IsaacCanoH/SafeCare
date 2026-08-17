package mx.utng.ich.safecaretv.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = SafePurple,
    secondary = SafeNavyLight,
    background = SafeBackground,
    surface = SafeSurface,
    onPrimary = SafeSurface,
    onSecondary = SafeSurface,
    onBackground = SafeText,
    onSurface = SafeText,
    error = SafeError
)

@Composable
fun SafeCareTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
