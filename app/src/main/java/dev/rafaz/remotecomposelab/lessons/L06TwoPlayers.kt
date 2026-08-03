package dev.rafaz.remotecomposelab.lessons

import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.remote.core.CalendarSystemClock
import androidx.compose.remote.player.compose.impl.RemoteComposePlayer
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import dev.rafaz.remotecomposelab.remote.DocumentClient
import dev.rafaz.remotecomposelab.ui.CodeBlock
import dev.rafaz.remotecomposelab.ui.Palette
import dev.rafaz.remotecomposelab.ui.Callout
import dev.rafaz.remotecomposelab.ui.Explanation
import dev.rafaz.remotecomposelab.ui.Stage
import kotlinx.coroutines.launch
import androidx.compose.remote.player.view.RemoteComposePlayer as viewPlayer

/**
 * AULA 06 — O experimento dos dois players
 *
 * Esta aula não ensina um recurso da tecnologia. Ensina uma **técnica de
 * investigação**, e por acaso resolve um mistério que ficou aberto.
 *
 * ── O mistério ────────────────────────────────────────────────────────
 *
 * Um documento pode conter valores e ações que os alteram:
 *
 *     val valor = PonteRc.valorFloat(writer, 1f)
 *     Box(Modifier.onClick { setValue(valor, 99f) })
 *
 * No nosso app, tocar nesse botão **não muda nada**. E não há erro: o
 * documento carrega, desenha, o clique é registrado. Simplesmente o valor
 * não muda.
 *
 * Já sabíamos, por eliminação, que:
 *   • as operações SÃO gravadas (o `doc.stats` mostra `FloatExpression : 2`)
 *   • o clique CHEGA (o mesmo padrão com `hostAction` funciona)
 *   • o texto REAVALIA (o relógio da tela anterior anda sozinho)
 *
 * Tudo funciona, menos a combinação.
 *
 * ── A técnica: trocar UMA variável ────────────────────────────────────
 *
 * Existem dois players no Remote Compose:
 *
 *   `remote-player-view`    → uma View clássica do Android (a camada de baixo)
 *   `remote-player-compose` → um @Composable que ENVELOPA aquela View
 *
 * Se o mesmo documento, com os mesmos bytes, se comportar diferente nos dois,
 * o problema está na camada que os distingue. Se se comportar igual, está
 * embaixo, no formato ou no core.
 *
 * É a mesma lógica do experimento de controle que usamos para achar a raiz
 * duplicada — só que agora a variável trocada é o player.
 */
@Composable
fun L06TwoPlayers() {
    val scope = rememberCoroutineScope()
    var bytes by remember { mutableStateOf<ByteArray?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    // Guardamos a instância da View para poder falar com ela depois.
    var viewPlayer by remember { mutableStateOf<viewPlayer?>(null) }
    var stateUpdaterResult by remember { mutableStateOf<String?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {

        Explanation(
            "Os dois quadros abaixo recebem EXATAMENTE os mesmos bytes — o " +
                "mesmo documento, baixado uma vez só. A única diferença é quem " +
                "os executa.\n\n" +
                "Toque no botão verde de cada um. Se um mudar o número e o " +
                "outro não, descobrimos onde está o defeito.",
        )

        CodeBlock(
            """
            // no servidor, dentro do documento:
            val valor = PonteRc.valorFloat(writer, 1f)
            Text(createTextFromFloat(valor, 3, 0, 0))
            Box(Modifier….onClick { setValue(valor, 99f) })
            """,
        )

        Button(
            onClick = {
                scope.launch {
                    runCatching { DocumentClient.fetch("/documento/tela/contador") }
                        .onSuccess { bytes = it; error = null }
                        .onFailure { error = it.message ?: it.toString() }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text(if (bytes == null) "Baixar o documento" else "Baixar de novo") }

        error?.let {
            Callout("Falhou: $it\n\nO servidor está no ar?", color = Palette.Warning)
        }

        bytes?.let { b ->
            // ── Player A: o de Compose (o que usamos em todas as aulas) ──
            Stage("A · player de Compose", borderColor = Palette.Read) {
                RemoteComposePlayer(
                    RemoteDocument(b),
                    Modifier.fillMaxWidth().height(200.dp),
                    1080,
                    520,
                    CalendarSystemClock(),
                )
            }

            // ── Player B: a View clássica, por baixo do de Compose ───────
            //
            // `AndroidView` é a ponte do Compose para o mundo de Views. O
            // bloco `factory` roda uma vez e cria a View; o `update` roda a
            // cada recomposição.
            //
            // Repare que aqui NÃO existe wrapper nenhum: é o player original
            // do AndroidX, recebendo os bytes crus.
            Stage("B · player de View (a camada de baixo)", borderColor = Palette.Write) {
                AndroidView(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    factory = { context ->
                        viewPlayer(context).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT,
                            )
                            setDocument(b)
                            viewPlayer = this
                        }
                    },
                    update = { player -> player.setDocument(b) },
                )
            }

            Explanation(
                "Toque nos botões +1 e −1 dentro de cada quadro. O número " +
                    "deveria mudar.",
            )

            // ── O terceiro caminho: mudar o valor DE FORA ────────────────
            //
            // O player de View expõe `getStateUpdater()`, que permite ao APP
            // alterar um valor NOMEADO dentro de um documento já carregado —
            // sem baixar nada de novo.
            //
            // É a porta dos fundos entre os dois mundos, e serve de teste
            // decisivo: se o valor mudar por aqui, então a ligação
            // valor → texto funciona, e o problema está só na execução da
            // ação de dentro do documento.
            Button(
                onClick = {
                    runCatching {
                        viewPlayer?.stateUpdater?.setUserLocalFloat("contador", 99f)
                        stateUpdaterResult = "chamado — olhe o quadro B"
                    }.onFailure { stateUpdaterResult = "falhou: ${it.message}" }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Mudar para 99 de FORA (StateUpdater)") }

            stateUpdaterResult?.let {
                Text(it, color = Palette.Bytes, fontSize = 13.sp)
            }

            CodeBlock(
                """
                // no APP, sem baixar documento novo:
                playerDeView.stateUpdater
                    .setUserLocalFloat("contador", 99f)
                """,
            )

            Callout(
                "RESULTADO: nenhum dos três caminhos muda o valor.\n\n" +
                    "• botão dentro do documento, player de Compose → nada\n" +
                    "• botão dentro do documento, player de View → nada\n" +
                    "• StateUpdater chamado pelo app → nada, e sem exceção\n\n" +
                    "Isso REFUTA a hipótese que eu tinha: não é o invólucro de " +
                    "Compose. Os dois players se comportam igual, então a " +
                    "diferença entre eles não é a causa.",
                color = Palette.Warning,
            )

            Callout(
                "O que sobra, e é bem mais provável: o valor não está sendo " +
                    "REGISTRADO com o nome/domínio que esses mecanismos " +
                    "procuram. Repare que `StateUpdater` tem um método " +
                    "`getUserDomainString(nome)` — isso sugere que nomes são " +
                    "qualificados por domínio, e que `named(\"contador\")` no " +
                    "servidor talvez não caia no domínio \"user\" que o " +
                    "`setUserLocalFloat` procura.\n\n" +
                    "Ou seja: a suspeita agora recai sobre o MEU código, não " +
                    "sobre a biblioteca. É uma conclusão menos glamourosa e " +
                    "mais provável — e foi o experimento que a produziu.",
                color = Palette.Write,
            )
        }

        Callout(
            "A lição que fica, independente do resultado: quando um sistema " +
                "tem duas implementações da mesma coisa, você ganhou um " +
                "experimento de graça. Rodar o mesmo dado nas duas e comparar " +
                "custa minutos e elimina metade das hipóteses.",
        )
    }
}
