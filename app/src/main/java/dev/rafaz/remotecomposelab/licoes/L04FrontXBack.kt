package dev.rafaz.remotecomposelab.licoes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.rafaz.remotecomposelab.remoto.ClienteDeDocumentos
import dev.rafaz.remotecomposelab.remoto.lembrarDocumento
import dev.rafaz.remotecomposelab.ui.BlocoCodigo
import dev.rafaz.remotecomposelab.ui.Cores
import dev.rafaz.remotecomposelab.ui.Destaque
import dev.rafaz.remotecomposelab.ui.Explicacao
import dev.rafaz.remotecomposelab.ui.LinhaMetrica
import dev.rafaz.remotecomposelab.ui.Palco
import kotlinx.coroutines.launch

/**
 * AULA 04 — Front × Back de verdade
 *
 * Nas aulas anteriores o "transporte" era encenado: os bytes nasciam e
 * morriam dentro do mesmo processo. Aqui eles atravessam a rede de fato,
 * vindos de um servidor Ktor que **não tem uma linha de Android**.
 */
@Composable
fun L04FrontXBack() {
    val escopo = rememberCoroutineScope()

    var documento by remember { mutableStateOf<RemoteDocument?>(null) }
    var tamanho by remember { mutableStateOf(0) }
    var origem by remember { mutableStateOf("") }
    var erro by remember { mutableStateOf<String?>(null) }
    var carregando by remember { mutableStateOf(false) }

    fun buscar(caminho: String, rotulo: String) {
        escopo.launch {
            carregando = true
            erro = null
            runCatching { ClienteDeDocumentos.buscar(caminho) }
                .onSuccess { bytes ->
                    tamanho = bytes.size
                    origem = rotulo
                    val doc = RemoteDocument(bytes)
                    // Diagnóstico: o documento sabe o próprio tamanho?
                    // Um documento 0x0 renderiza uma tela em branco sem
                    // reclamar de nada — foi assim que perdemos meia hora.
                    android.util.Log.i(
                        "RemoteComposeLab",
                        "doc ${doc.width}x${doc.height} | stats=" +
                            doc.stats.joinToString(" ; "),
                    )
                    documento = doc
                }
                .onFailure { erro = it.message ?: it.toString() }
            carregando = false
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {

        Explicacao(
            "Até agora eu te devia uma. As Aulas 01 a 03 mostravam o documento " +
                "indo e voltando dentro do mesmo processo — honesto como " +
                "demonstração do conceito, mas não era rede.\n\n" +
                "Agora é. O módulo :server deste repositório é um Ktor em JVM " +
                "pura. Ele gera os documentos e devolve por HTTP.",
        )

        Destaque(
            "O servidor NÃO tem Android no classpath. Ele depende de exatamente " +
                "três artefatos: remote-core, remote-creation-core e " +
                "remote-creation-jvm — os três são JAR puro. Era teoria em " +
                "docs/02; agora está rodando.",
            cor = Cores.Escrita,
        )

        Explicacao("Antes de tocar nos botões, suba o servidor:")
        BlocoCodigo(".\\gradlew.bat :server:run")

        Explicacao(
            "Repare no que o endpoint devolve. Não é JSON descrevendo uma tela — " +
                "é a tela:",
        )
        BlocoCodigo(
            """
            GET /documento/boas-vindas?nome=Rafael

            HTTP/1.1 200 OK
            Content-Type: application/octet-stream

            <396 bytes executáveis>
            """,
        )

        Destaque(
            "Chegar até aqui deu trabalho: foram quatro armadilhas seguidas, " +
                "todas com o mesmo sintoma — tela em branco, sem erro nenhum. " +
                "Estão documentadas uma a uma em docs/03-diario-de-bordo.md, " +
                "porque é onde mora o conteúdo que não existe em outro lugar.",
            cor = Cores.Alerta,
        )

        // ── EXPERIMENTO DE CONTROLE ──────────────────────────────────────
        // O MESMO conteúdo, gerado no aparelho. Serve de referência: se este
        // aparece e o do servidor não, a diferença está nos bytes, não no
        // player nem na tela.
        val local = lembrarDocumento {
            RemoteColumn(
                modifier = RemoteModifier
                    .fillMaxWidth()
                    .background(Color(0xFF1B3A4B))
                    .padding(20.rdp),
            ) {
                RemoteText("Olá, Rafael!", fontSize = 22.rsp, color = RemoteColor(Color(0xFF7FDBFF)))
                RemoteText(
                    "Gerado AQUI, no aparelho.",
                    fontSize = 14.rsp,
                    color = RemoteColor(Color(0xFFBFE9FF)),
                )
            }
        }
        LaunchedEffect(local) {
            local?.let {
                android.util.Log.i(
                    "RemoteComposeLab",
                    "LOCAL ${it.documento.width}x${it.documento.height} " +
                        "(${it.tamanhoBytes} bytes) | stats=" +
                        it.documento.stats.joinToString(" ; "),
                )
            }
        }
        if (local != null) {
            Palco("Controle: gerado no aparelho", corBorda = Cores.Escrita) {
                RemoteComposePlayer(
                    document = local.documento,
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = { buscar("/documento/boas-vindas?nome=Rafael", "boas-vindas") },
                enabled = !carregando,
                modifier = Modifier.weight(1f),
            ) { Text("Boas-vindas") }

            OutlinedButton(
                onClick = { buscar("/documento/promocao", "promoção") },
                enabled = !carregando,
                modifier = Modifier.weight(1f),
            ) { Text("Promoção") }
        }

        when {
            carregando -> Text("buscando no servidor…", color = Cores.TextoFraco, fontSize = 13.sp)

            erro != null -> {
                Destaque(
                    "Falhou: $erro\n\n" +
                        "Quase sempre é uma destas três coisas: o servidor não " +
                        "está no ar (.\\gradlew.bat :server:run), você está num " +
                        "aparelho físico em vez do emulador (aí 10.0.2.2 não " +
                        "existe — use o IP da sua máquina), ou o firewall do " +
                        "Windows bloqueou a porta 8080.",
                    cor = Cores.Alerta,
                )
            }

            documento != null -> {
                Palco("Veio do servidor · $origem") {
                    // width/height explícitos: o documento vindo da JVM se
                    // declara 0x0 (ver diário de bordo), então dizemos ao
                    // player qual área ele deve usar.
                    // Posicional de propósito: os dois inteiros após o
                    // modifier são largura e altura, e a lib não expõe os
                    // nomes desses parâmetros de forma utilizável.
                    RemoteComposePlayer(
                        documento!!,
                        Modifier.fillMaxWidth().height(150.dp),
                        1080,
                        600,
                    )
                }
                Column(Modifier.fillMaxWidth()) {
                    LinhaMetrica("recebido", "$tamanho bytes")
                    LinhaMetrica("origem", ClienteDeDocumentos.BASE)
                    LinhaMetrica("content-type", "application/octet-stream")
                }
            }
        }

        Destaque(
            "Agora a parte divertida. Com o app aberto nesta tela, mude a " +
                "promoção pelo terminal e toque em \"Promoção\" de novo. A " +
                "interface muda sem recompilar, sem reinstalar, sem passar pela " +
                "loja.",
        )

        BlocoCodigo(
            """
            # muda a promoção
            curl -X PUT http://localhost:8080/promocao ^
              -H "Content-Type: application/json" ^
              -d "{\"chamada\":\"Frete grátis\",\"descricao\":\"Hoje só\",\"preco\":\"R$ 0\"}"

            # desliga a promoção
            curl -X DELETE http://localhost:8080/promocao
            """,
        )

        Explicacao(
            "Quando você desliga a promoção, o servidor devolve um documento " +
                "diferente — não um campo nulo para o app interpretar. Quem " +
                "decidiu o que mostrar foi o servidor, inteiramente.",
        )

        Destaque(
            "Compare com o SDUI de JSON: lá, \"promoção desligada\" seria um " +
                "campo que o app precisa saber tratar, e mostrar algo diferente " +
                "exigiria que o app já tivesse aquele componente pronto. Aqui o " +
                "servidor manda um desenho novo. O app não sabe nem que existe o " +
                "conceito de promoção — e é justamente por isso que ele nunca " +
                "precisa de release.",
            cor = Cores.Leitura,
        )
    }
}
