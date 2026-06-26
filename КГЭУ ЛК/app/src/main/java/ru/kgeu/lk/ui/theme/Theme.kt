package ru.kgeu.lk.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = KgeuAccent,
    onPrimary = Color.White,
    primaryContainer = KgeuAccentContainer,
    onPrimaryContainer = KgeuOnAccentContainer,
    secondary = KgeuBlueLight,
    onSecondary = Color.White,
    background = KgeuBackground,
    onBackground = Color(0xFF101418),
    surface = KgeuSurface,
    onSurface = Color(0xFF101418),
    surfaceVariant = KgeuSurfaceVariant,
    onSurfaceVariant = KgeuOnSurfaceMuted,
    outline = KgeuOutline,
    outlineVariant = KgeuOutline,
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
)

@Composable
fun KgeuTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        shapes = AppShapes,
        content = content,
    )
}
