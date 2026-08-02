package dev.rafaz.remotecomposelab.licoes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteRow
import androidx.compose.remote.creation.compose.layout.RemoteSpacer
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.fillMaxWidth
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rsp
import androidx.compose.remote.player.compose.impl.RemoteComposePlayer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.rafaz.remotecomposelab.remoto.lembrarDocumento
import dev.rafaz.remotecomposelab.ui.BlocoCodigo
import dev.rafaz.remotecomposelab.ui.Cores
import dev.rafaz.remotecomposelab.ui.Destaque
import dev.rafaz.remotecomposelab.ui.Explicacao
import dev.rafaz.remotecomposelab.ui.Palco

/**
 * AULA 03 — Por que existe um Modifier paralelo?
 *
 * Esta é a pergunta que mais incomoda quem chega no Remote Compose:
 * "por que não deixaram eu usar o Modifier normal?".
 *
 * A resposta é bonita e explica o resto da biblioteca.
 */
@Composable
fun L03PorQueUmModifierParalelo() {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {

        Explicacao(
            "Você já reparou que quase tudo tem um gêmeo com prefixo Remote: " +
                "Column/RemoteColumn, Text/RemoteText, Modifier/RemoteModifier, " +
                "dp/rdp, sp/rsp.\n\n" +
                "Não é frescura de nomenclatura. É uma consequência inevitável.",
        )

        Destaque(
            "Um Modifier normal EXECUTA. Um RemoteModifier é GRAVADO.",
            cor = Cores.Escrita,
        )

        Explicacao(
            "Pense no que Modifier.padding(16.dp) realmente é: um objeto que, na " +
                "hora do layout, roda código Kotlin dentro do seu processo para " +
                "encolher as restrições de medida. Ele só existe enquanto o seu app " +
                "está vivo.\n\n" +
                "Agora pense no que precisa acontecer no Remote Compose: esse padding " +
                "tem que virar bytes, viajar, e ser aplicado por um player que talvez " +
                "esteja em OUTRO processo, ou até em outro dispositivo. Não dá para " +
                "serializar um objeto que é, essencialmente, um pedaço de código.",
        )

        BlocoCodigo(
            """
            Modifier.padding(16.dp)
                └─▶ objeto vivo na memória, roda no SEU processo

            RemoteModifier.padding(16.rdp)
                └─▶ vira uma OPERAÇÃO no documento:
                      [PaddingModifier  start=16 top=16 end=16 bottom=16]
                    e é o player quem a interpreta, do outro lado
            """,
        )

        Explicacao(
            "É a mesma razão pela qual existe rdp em vez de dp. Um Dp é um valor " +
                "fixo. Um RemoteDp pode ser uma EXPRESSÃO que o player calcula na " +
                "hora — dependendo do tamanho da tela dele, não da sua. O documento " +
                "não sabe onde vai ser executado, então ele carrega a fórmula em vez " +
                "do resultado.",
        )

        // ── Demonstração: modificadores compostos, gravados no documento ────
        val doc = lembrarDocumento {
            RemoteColumn(
                modifier = RemoteModifier
                    .fillMaxWidth()
                    .background(Color(0xFF16161E))
                    .padding(16.rdp),
            ) {
                RemoteText(
                    "RemoteModifier em ação",
                    fontSize = 16.rsp,
                    color = RemoteColor(Color(0xFFB388FF)),
                )

                RemoteSpacer(RemoteModifier.size(12.rdp))

                // Uma fileira de quadrados coloridos: cada um é só um RemoteBox
                // com size + background. Tudo isso vira operação no documento.
                RemoteRow(modifier = RemoteModifier.fillMaxWidth()) {
                    listOf(
                        Color(0xFFE57373),
                        Color(0xFF81C784),
                        Color(0xFF64B5F6),
                        Color(0xFFFFD54F),
                    ).forEach { cor ->
                        RemoteBox(
                            modifier = RemoteModifier
                                .size(44.rdp)
                                .background(cor),
                        )
                        RemoteSpacer(RemoteModifier.size(8.rdp))
                    }
                }

                RemoteSpacer(RemoteModifier.size(12.rdp))

                RemoteText(
                    "Nenhum destes quadrados existe como View.",
                    fontSize = 13.rsp,
                    color = RemoteColor(Color(0xFF9A9AAE)),
                )
            }
        }

        Palco("Gravado e executado pelo player") {
            if (doc == null) {
                Text("gravando…", color = Cores.TextoFraco, fontSize = 13.sp)
            } else {
                RemoteComposePlayer(
                    document = doc.documento,
                    modifier = Modifier.fillMaxWidth().height(180.dp),
                )
            }
        }

        if (doc != null) {
            Explicacao(
                "Aqueles quatro quadrados custaram ${doc.tamanhoBytes} bytes no total " +
                    "— documento inteiro, incluindo os textos. Documentos são baratos, " +
                    "e isso é proposital: eles foram feitos para trafegar.",
            )
        }

        Destaque(
            "Regra prática para não se perder: se o valor precisa SOBREVIVER à " +
                "serialização, ele tem uma versão Remote. Se ele só existe na sua " +
                "tela, use o tipo normal. Quando você misturar os dois sem querer, o " +
                "compilador reclama — e agora você sabe o que ele está te dizendo.",
        )
    }
}
