package com.ejemplo.cuentostv.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.MaterialTheme
import com.ejemplo.cuentostv.data.StoryTheme

val TvBackground = Color(0xFF0B0814)
val TvSurface = Color(0xFF1A1033)
val TvAccent = Color(0xFFFFD166)
val TvText = Color(0xFFF7F1E3)
val TvMuted = Color(0xFFB9B0D0)

fun StoryTheme.gradientColors(): List<Color> = when (this) {
    StoryTheme.FOREST -> listOf(Color(0xFF071F1B), Color(0xFF0E3B32), Color(0xFF1A1033))
    StoryTheme.NIGHT -> listOf(Color(0xFF070A20), Color(0xFF141B4D), Color(0xFF2B1B55))
    StoryTheme.SEA -> listOf(Color(0xFF031B2B), Color(0xFF064663), Color(0xFF0B2545))
    StoryTheme.CASTLE -> listOf(Color(0xFF1A0B2E), Color(0xFF3D1B55), Color(0xFF0B0814))
    StoryTheme.SPACE -> listOf(Color(0xFF03010A), Color(0xFF1B103D), Color(0xFF3A1C71))
}

fun StoryTheme.accentColor(): Color = when (this) {
    StoryTheme.FOREST -> Color(0xFF9BE7C4)
    StoryTheme.NIGHT -> Color(0xFFFFD166)
    StoryTheme.SEA -> Color(0xFF8ED8F8)
    StoryTheme.CASTLE -> Color(0xFFFFB4D8)
    StoryTheme.SPACE -> Color(0xFFFFD166)
}

@Composable
fun CuentosTvTheme(content: @Composable () -> Unit) {
    MaterialTheme {
        content()
    }
}

@Composable
fun StoryBackground(
    theme: StoryTheme,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(theme.gradientColors())),
        content = content
    )
}
