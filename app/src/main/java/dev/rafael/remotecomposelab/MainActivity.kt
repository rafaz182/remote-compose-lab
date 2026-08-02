package dev.rafael.remotecomposelab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.Modifier
import dev.rafael.remotecomposelab.catalogo.Catalogo
import dev.rafael.remotecomposelab.ui.TemaLaboratorio

/**
 * Entrada do laboratório.
 *
 * DETALHE QUE JÁ CUSTOU CRASH: o tema declarado no AndroidManifest precisa
 * herdar de AppCompat. O `remote-creation-compose` depende de
 * androidx.appcompat e o player infla Views do Android por baixo dos panos —
 * com um tema que não seja AppCompat, o app morre ao inflar. Veja
 * `res/values/themes.xml`.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TemaLaboratorio {
                Catalogo(
                    // Empurra o conteúdo para fora da status bar / gesture bar.
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.systemBars),
                )
            }
        }
    }
}
