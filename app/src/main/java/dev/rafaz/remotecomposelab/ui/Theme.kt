package dev.rafaz.remotecomposelab.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Paleta do laboratório.
 *
 * Duas cores carregam significado ao longo de TODAS as aulas — vale memorizar:
 *
 *  - [Write] (violeta) marca tudo que pertence ao lado da **criação** do
 *    documento: `captureSingleRemoteDocument`, `RemoteColumn`, `RemoteText`.
 *  - [Read] (ciano) marca tudo que pertence ao lado do **player**:
 *    `RemoteDocument`, `RemoteComposePlayer`.
 *
 * Sempre que você vir violeta e ciano na mesma tela, estamos mostrando os dois
 * lados da ponte.
 */
object Palette {
    val Write = Color(0xFFB388FF)
    val Read = Color(0xFF4DD0E1)
    val Bytes = Color(0xFF80CBC4)
    val Background = Color(0xFF101014)
    val Surface = Color(0xFF1A1A21)
    val SurfaceHigh = Color(0xFF23232C)
    val TextPrimary = Color(0xFFE8E8F0)
    val TextMuted = Color(0xFF9A9AAE)
    val Warning = Color(0xFFFFB74D)
}

@Composable
fun LabTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Palette.Write,
            secondary = Palette.Read,
            background = Palette.Background,
            surface = Palette.Surface,
            onBackground = Palette.TextPrimary,
            onSurface = Palette.TextPrimary,
        ),
        content = content,
    )
}
