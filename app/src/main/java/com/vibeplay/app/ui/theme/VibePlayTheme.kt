package com.vibeplay.app.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.vibeplay.core.model.VibeThemeMode

val BackgroundDark = Color(0xFF121214)
val ContainerSurface = Color(0xFF1E1E24)
val ContainerBorder = Color(0xFF2A2A35)
val TextPrimary = Color(0xFFE4E4E7)
val TextMuted = Color(0xFF9CA3AF)

// Teal (Default)
val TealPrimary = Color(0xFF06B6D4)
val AmberSecondary = Color(0xFFF59E0B)
val EmeraldTertiary = Color(0xFF10B981)

// Themes
val ThemeAmberPrimary = Color(0xFFF59E0B)
val ThemeLofiPrimary = Color(0xFFC084FC)
val ThemeInkPrimary = Color(0xFF6FFBBE)

@Composable
fun VibePlayTheme(
    mode: VibeThemeMode = VibeThemeMode.TEAL,
    content: @Composable () -> Unit
) {
    val primaryColor = when (mode) {
        VibeThemeMode.TEAL -> TealPrimary
        VibeThemeMode.AMBER -> ThemeAmberPrimary
        VibeThemeMode.LOFI -> ThemeLofiPrimary
        VibeThemeMode.INK -> ThemeInkPrimary
    }

    val darkColorScheme = darkColorScheme(
        primary = primaryColor,
        secondary = AmberSecondary,
        tertiary = EmeraldTertiary,
        background = BackgroundDark,
        surface = ContainerSurface,
        onPrimary = Color.Black,
        onSecondary = Color.Black,
        onBackground = TextPrimary,
        onSurface = TextPrimary,
        outline = ContainerBorder
    )

    MaterialTheme(
        colorScheme = darkColorScheme,
        content = content
    )
}
