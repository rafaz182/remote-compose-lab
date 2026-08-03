package dev.rafaz.remotecomposelab.lessons

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.fillMaxWidth
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rsp
import androidx.compose.remote.player.compose.impl.RemoteComposePlayer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.rafaz.remotecomposelab.remote.rememberDocument
import dev.rafaz.remotecomposelab.ui.CodeBlock
import dev.rafaz.remotecomposelab.ui.Palette
import dev.rafaz.remotecomposelab.ui.Callout
import dev.rafaz.remotecomposelab.ui.Explanation
import dev.rafaz.remotecomposelab.ui.Stage
import androidx.compose.material3.Text as TextoNormal

/**
 * AULA 01 — Olá, Remote Compose
 *
 * Objetivo: ver o ciclo completo funcionando e perceber que ele tem uma etapa
 * a mais do que o Compose que você já conhece.
 */
@Composable
fun L01HelloRemoteCompose() {
    Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(18.dp)) {

        Explanation(
            "No Compose que você já usa, escrever a UI e mostrá-la na tela são a " +
                "mesma coisa: você chama Text(\"Olá\") e o texto aparece. Não existe " +
                "nada no meio.\n\n" +
                "No Remote Compose existe algo no meio — e esse algo é o ponto " +
                "inteiro da tecnologia.",
        )

        CodeBlock(
            """
            Compose comum:
                Text("Olá")  ────────────────────────────▶  pixels

            Remote Compose:
                RemoteText("Olá")  ──▶  ByteArray  ──▶  player  ──▶  pixels
                     escrita          o documento      leitura
            """,
        )

        Explanation(
            "Aquele ByteArray no meio é um documento: um valor de verdade, que " +
                "você pode guardar em disco, mandar por HTTP ou empurrar para um " +
                "relógio. É isso que uma @Composable comum nunca conseguiu ser.",
        )

        // ── O lado esquerdo da ponte: Compose comum, para comparar ─────────
        Stage("Compose comum (o que você já conhece)", borderColor = Palette.TextMuted) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF2E2E3A))
                    .padding(20.dp),
            ) {
                TextoNormal("Olá, Compose!", color = Color.White, fontSize = 22.sp)
                TextoNormal(
                    "Isto foi desenhado direto na tela.",
                    color = Color(0xFFB9B9CC),
                    fontSize = 14.sp,
                )
            }
        }

        // ── O lado direito: o MESMO visual, mas atravessando um documento ──
        //
        // Repare que a estrutura do código é quase idêntica. A troca é
        // sistemática: Column → RemoteColumn, Text → RemoteText,
        // Modifier → RemoteModifier, 20.dp → 20.rdp, 22.sp → 22.rsp.
        val doc = rememberDocument {
            RemoteColumn(
                modifier = RemoteModifier
                    .fillMaxWidth()
                    .background(Color(0xFF2E2E3A))
                    .padding(20.rdp),
            ) {
                RemoteText(
                    "Olá, Remote Compose!",
                    fontSize = 22.rsp,
                    color = RemoteColor(Color.White),
                )
                RemoteText(
                    "Isto virou bytes e voltou.",
                    fontSize = 14.rsp,
                    color = RemoteColor(Color(0xFFB9B9CC)),
                )
            }
        }

        Stage("Remote Compose (atravessou um documento)") {
            if (doc == null) {
                TextoNormal("gravando o documento…", color = Palette.TextMuted, fontSize = 13.sp)
            } else {
                RemoteComposePlayer(
                    document = doc.document,
                    modifier = Modifier.fillMaxWidth().height(110.dp),
                )
            }
        }

        Callout(
            "Os dois quadros acima parecem iguais — e é exatamente esse o ponto. " +
                "O de baixo não foi desenhado pelo seu código: ele foi gravado como " +
                "documento e depois EXECUTADO por um player. Seu app poderia nunca " +
                "ter visto esse conteúdo antes.",
        )

        if (doc != null) {
            Explanation("O tamanho do documento que acabou de ser gerado:")
            CodeBlock("${doc.sizeInBytes} bytes\n\nprimeiros bytes:\n${doc.hex(24)}")
        }
    }
}
