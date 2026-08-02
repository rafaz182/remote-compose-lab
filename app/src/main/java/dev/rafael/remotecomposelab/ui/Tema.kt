package dev.rafael.remotecomposelab.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Paleta do laboratório.
 *
 * Duas cores carregam significado ao longo de TODAS as aulas — vale memorizar:
 *
 *  - [Escrita] (violeta) marca tudo que pertence ao lado da **criação** do
 *    documento: `captureSingleRemoteDocument`, `RemoteColumn`, `RemoteText`.
 *  - [Leitura] (ciano) marca tudo que pertence ao lado do **player**:
 *    `RemoteDocument`, `RemoteComposePlayer`.
 *
 * Sempre que você vir violeta e ciano na mesma tela, estamos mostrando os dois
 * lados da ponte.
 */
object Cores {
    val Escrita = Color(0xFFB388FF)
    val Leitura = Color(0xFF4DD0E1)
    val Bytes = Color(0xFF80CBC4)
    val Fundo = Color(0xFF101014)
    val Superficie = Color(0xFF1A1A21)
    val SuperficieAlta = Color(0xFF23232C)
    val Texto = Color(0xFFE8E8F0)
    val TextoFraco = Color(0xFF9A9AAE)
    val Alerta = Color(0xFFFFB74D)
}

@Composable
fun TemaLaboratorio(conteudo: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Cores.Escrita,
            secondary = Cores.Leitura,
            background = Cores.Fundo,
            surface = Cores.Superficie,
            onBackground = Cores.Texto,
            onSurface = Cores.Texto,
        ),
        content = conteudo,
    )
}
