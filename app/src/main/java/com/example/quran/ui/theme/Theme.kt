package com.example.quran.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = PrimaryGreen,          // Top bars, main buttons
    onPrimary = Color.White,         // Text on top of green
    secondary = SecondaryGold,       // Floating buttons, selection marks
    onSecondary = Color.Black,
    background = SoftCream,          // The main screen color (Eye-friendly)
    surface = SoftCream,             // Ensure background consistency
    onBackground = DarkCharcoal,     // Main text color
    onSurface = DarkCharcoal
)

@Composable
fun QuranTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Use PrimaryGreen for the status bar
            window.statusBarColor = colorScheme.primary.toArgb()
            // Since the status bar is dark green, use light icons (false)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
