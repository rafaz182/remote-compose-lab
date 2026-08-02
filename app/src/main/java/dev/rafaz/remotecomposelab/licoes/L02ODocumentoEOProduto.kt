package dev.rafaz.remotecomposelab.licoes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
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
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.rafaz.remotecomposelab.remoto.lembrarDocumento
import dev.rafaz.remotecomposelab.ui.BlocoCodigo
import dev.rafaz.remotecomposelab.ui.Cores
import dev.rafaz.remotecomposelab.ui.Destaque
import dev.rafaz.remotecomposelab.ui.Explicacao
import dev.rafaz.remotecomposelab.ui.LinhaMetrica
import dev.rafaz.remotecomposelab.ui.Palco

/**
 * AULA 02 — O documento é o produto
 *
 * Objetivo: parar de enxergar o ByteArray como detalhe de implementação e
 * começar a enxergá-lo como a entrega.
 *
 * A demonstração central: pegamos os bytes, jogamos fora o documento original,
 * e reconstruímos um documento NOVO a partir apenas dos bytes. É exatamente o
 * que aconteceria se eles tivessem vindo pela rede.
 */
@Composable
fun L02ODocumentoEOProduto() {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {

        Explicacao(
            "Na aula anterior o documento apareceu de passagem. Agora ele é o " +
                "assunto.\n\n" +
                "Um documento Remote Compose é um ByteArray autocontido. Ele carrega " +
                "não só \"que texto mostrar\", mas as próprias OPERAÇÕES de layout e " +
                "desenho — uma espécie de bytecode de interface.",
        )

        val original = lembrarDocumento {
            RemoteColumn(
                modifier = RemoteModifier
                    .fillMaxWidth()
                    .background(Color(0xFF1B3A4B))
                    .padding(18.rdp),
            ) {
                RemoteText(
                    "Sou um documento.",
                    fontSize = 20.rsp,
                    color = RemoteColor(Color(0xFF7FDBFF)),
                )
                RemoteText(
                    "Vim de um ByteArray.",
                    fontSize = 14.rsp,
                    color = RemoteColor(Color(0xFFBFE9FF)),
                )
            }
        }

        if (original == null) {
            Text("gravando…", color = Cores.TextoFraco, fontSize = 13.sp)
            return@Column
        }

        Palco("Documento original") {
            RemoteComposePlayer(
                document = original.documento,
                modifier = Modifier.fillMaxWidth().height(100.dp),
            )
        }

        // ── A demonstração: simular o transporte pela rede ──────────────────
        //
        // `original.bytes` é o único elo. Repare que NÃO reutilizamos o objeto
        // RemoteDocument: construímos um novo a partir dos bytes crus, como o
        // app de um usuário faria após um download.
        var recebido by remember { mutableStateOf<RemoteDocument?>(null) }

        Explicacao(
            "Abaixo, o botão faz o que um app real faria ao receber uma resposta " +
                "HTTP: pega os bytes crus e constrói um documento novinho, sem " +
                "acesso nenhum ao código que o gerou.",
        )

        BlocoCodigo(
            """
            // no servidor (ou no build, ou num CMS)
            val bytes: ByteArray = documento.bytes

            //  ...atravessa a rede...

            // no app do usuário
            val doc = RemoteDocument(bytes)   // ← só isso
            RemoteComposePlayer(document = doc)
            """,
        )

        Button(
            onClick = { recebido = RemoteDocument(original.bytes) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (recebido == null) "Receber os bytes e renderizar" else "Receber de novo")
        }

        if (recebido != null) {
            Palco("Reconstruído SÓ a partir dos bytes", corBorda = Cores.Bytes) {
                RemoteComposePlayer(
                    document = recebido!!,
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                )
            }
        }

        Destaque(
            "Pare um segundo aqui. O quadro acima foi renderizado por um objeto " +
                "que só recebeu bytes. Nenhuma classe, nenhuma @Composable, nenhum " +
                "recurso do seu app participou. É por isso que Remote Compose " +
                "consegue atravessar processos e dispositivos.",
        )

        Explicacao("Anatomia do documento que geramos:")

        Column(
            Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            LinhaMetrica("tamanho em bytes", "${original.tamanhoBytes}")
            LinhaMetrica("largura medida", "${original.largura} px")
            LinhaMetrica("altura medida", "${original.altura} px")
        }

        Destaque(
            "Pegadinha que vale ouro: só o \"tamanho em bytes\" é uma propriedade " +
                "dos bytes. Largura e altura NÃO são — elas mudam conforme o player " +
                "mede o documento. Role a tela para cima e para baixo e volte aqui: " +
                "os números mudam.\n\n" +
                "Isso não é bug, é a tese da tecnologia aparecendo na prática. O " +
                "documento não carrega um tamanho pronto; ele carrega instruções de " +
                "layout que são RESOLVIDAS no destino. Quem decide a largura é o " +
                "player, com a tela que ele tem — não você, na hora de gravar.",
            cor = Cores.Leitura,
        )

        Explicacao(
            "E estes são os primeiros bytes, em hexadecimal. Não decore nada aqui " +
                "— o objetivo é só você ver com os próprios olhos que a sua UI virou " +
                "um blob binário:",
        )
        BlocoCodigo(original.hex(48))

        Destaque(
            "Compare com Server-Driven UI baseado em JSON: lá o servidor manda uma " +
                "DESCRIÇÃO que o app precisa saber interpretar (\"type\": \"button\" só " +
                "funciona se o app já tiver um botão pronto). Aqui o documento " +
                "carrega as operações de desenho em si. O player não precisa " +
                "conhecer o seu design system.",
            cor = Cores.Escrita,
        )
    }
}
