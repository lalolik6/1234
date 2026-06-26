package ru.kgeu.lk.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = KgeuBlue,
    onPrimary = Color.White,
    secondary = KgeuAccent,
    background = KgeuBackground,
    surface = KgeuCard,
)

@Composable
fun KgeuTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content,
    )
}
