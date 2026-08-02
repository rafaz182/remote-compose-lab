package dev.rafael.remotecomposelab.licoes

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
import dev.rafael.remotecomposelab.remoto.lembrarDocumento
import dev.rafael.remotecomposelab.ui.BlocoCodigo
import dev.rafael.remotecomposelab.ui.Cores
import dev.rafael.remotecomposelab.ui.Destaque
import dev.rafael.remotecomposelab.ui.Explicacao
import dev.rafael.remotecomposelab.ui.Palco
import androidx.compose.material3.Text as TextoNormal

/**
 * AULA 01 — Olá, Remote Compose
 *
 * Objetivo: ver o ciclo completo funcionando e perceber que ele tem uma etapa
 * a mais do que o Compose que você já conhece.
 */
@Composable
fun L01OlaRemoteCompose() {
    Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(18.dp)) {

        Explicacao(
            "No Compose que você já usa, escrever a UI e mostrá-la na tela são a " +
                "mesma coisa: você chama Text(\"Olá\") e o texto aparece. Não existe " +
                "nada no meio.\n\n" +
                "No Remote Compose existe algo no meio — e esse algo é o ponto " +
                "inteiro da tecnologia.",
        )

        BlocoCodigo(
            """
            Compose comum:
                Text("Olá")  ────────────────────────────▶  pixels

            Remote Compose:
                RemoteText("Olá")  ──▶  ByteArray  ──▶  player  ──▶  pixels
                     escrita          o documento      leitura
            """,
        )

        Explicacao(
            "Aquele ByteArray no meio é um documento: um valor de verdade, que " +
                "você pode guardar em disco, mandar por HTTP ou empurrar para um " +
                "relógio. É isso que uma @Composable comum nunca conseguiu ser.",
        )

        // ── O lado esquerdo da ponte: Compose comum, para comparar ─────────
        Palco("Compose comum (o que você já conhece)", corBorda = Cores.TextoFraco) {
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
        val doc = lembrarDocumento {
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

        Palco("Remote Compose (atravessou um documento)") {
            if (doc == null) {
                TextoNormal("gravando o documento…", color = Cores.TextoFraco, fontSize = 13.sp)
            } else {
                RemoteComposePlayer(
                    document = doc.documento,
                    modifier = Modifier.fillMaxWidth().height(110.dp),
                )
            }
        }

        Destaque(
            "Os dois quadros acima parecem iguais — e é exatamente esse o ponto. " +
                "O de baixo não foi desenhado pelo seu código: ele foi gravado como " +
                "documento e depois EXECUTADO por um player. Seu app poderia nunca " +
                "ter visto esse conteúdo antes.",
        )

        if (doc != null) {
            Explicacao("O tamanho do documento que acabou de ser gerado:")
            BlocoCodigo("${doc.tamanhoBytes} bytes\n\nprimeiros bytes:\n${doc.hex(24)}")
        }
    }
}
