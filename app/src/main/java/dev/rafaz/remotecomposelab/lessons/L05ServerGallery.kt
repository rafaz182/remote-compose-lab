package dev.rafaz.remotecomposelab.lessons

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.remote.core.CalendarSystemClock
import androidx.compose.remote.player.compose.impl.RemoteComposePlayer
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.rafaz.remotecomposelab.remote.DocumentClient
import dev.rafaz.remotecomposelab.remote.RemoteScreen
import dev.rafaz.remotecomposelab.ui.CodeBlock
import dev.rafaz.remotecomposelab.ui.Palette
import dev.rafaz.remotecomposelab.ui.Callout
import dev.rafaz.remotecomposelab.ui.Explanation
import dev.rafaz.remotecomposelab.ui.MetricRow
import dev.rafaz.remotecomposelab.ui.Stage
import dev.rafaz.remotecomposelab.ui.SectionTitle
import kotlinx.coroutines.launch

/**
 * AULA 05 — A galeria do servidor
 *
 * Cinco telas de complexidade crescente, todas desenhadas no backend. E o
 * caminho de volta: eventos saindo do documento para o app.
 */
@Composable
fun L05ServerGallery() {
    val scope = rememberCoroutineScope()

    var SCREENS by remember { mutableStateOf<List<RemoteScreen>>(emptyList()) }
    var selected by remember { mutableStateOf<RemoteScreen?>(null) }
    var document by remember { mutableStateOf<RemoteDocument?>(null) }
    var size by remember { mutableStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }

    // O histórico de ações que o DOCUMENTO mandou para o APP.
    val receivedActions = remember { mutableStateListOf<String>() }

    fun loadCatalog() {
        scope.launch {
            runCatching { DocumentClient.fetchScreens() }
                .onSuccess { SCREENS = it; error = null }
                .onFailure { error = it.message ?: it.toString() }
        }
    }

    fun open(screen: RemoteScreen) {
        scope.launch {
            runCatching { DocumentClient.fetch("/documento/tela/${screen.id}") }
                .onSuccess { bytes ->
                    size = bytes.size
                    selected = screen
                    val doc = RemoteDocument(bytes)
                    // Diagnóstico: `stats` nomeia as operações presentes no
                    // documento. É a forma mais direta de responder "o que o
                    // servidor gravou de verdade aqui dentro?".
                    android.util.Log.i(
                        "RemoteComposeLab",
                        "[${screen.id}] ${doc.width}x${doc.height} :: " +
                            doc.stats.joinToString(" ; "),
                    )
                    document = doc
                    receivedActions.clear()
                    error = null
                }
                .onFailure { error = it.message ?: it.toString() }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {

        Explanation(
            "Cinco telas, todas desenhadas no servidor, em ordem de " +
                "dificuldade. Nenhum componente delas existe no código deste " +
                "app — nem cartão, nem lista, nem botão.\n\n" +
                "Suba o servidor e carregue o catálogo:",
        )
        CodeBlock(".\\gradlew.bat :server:run")

        Button(onClick = { loadCatalog() }, modifier = Modifier.fillMaxWidth()) {
            Text(if (SCREENS.isEmpty()) "Carregar catálogo" else "Recarregar catálogo")
        }

        error?.let {
            Callout(
                "Falhou: $it\n\nO servidor está no ar? (.\\gradlew.bat :server:run)",
                color = Palette.Warning,
            )
        }

        // ── O menu vem em JSON; as telas vêm em bytes ────────────────────
        if (SCREENS.isNotEmpty()) {
            SectionTitle("Telas disponíveis", Palette.Write)
            SCREENS.forEach { screen ->
                val active = selected?.id == screen.id
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (active) Palette.Write.copy(alpha = 0.16f) else Palette.Surface)
                        .border(
                            1.dp,
                            if (active) Palette.Write else Palette.SurfaceHigh,
                            RoundedCornerShape(12.dp),
                        )
                        .clickable { open(screen) }
                        .padding(14.dp),
                ) {
                    Text(screen.title, color = Palette.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        screen.teaches,
                        color = Palette.TextMuted,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                Box(Modifier.height(8.dp))
            }
        }

        // ── O palco ──────────────────────────────────────────────────────
        document?.let { doc ->
            Stage("Renderizado do documento · ${selected?.title}") {
                RemoteComposePlayer(
                    doc,
                    Modifier.fillMaxWidth().height(stageHeight(selected?.id)),
                    1080,
                    // A altura vem do CATÁLOGO, não de um `when` no app.
                    selected?.height ?: 400,
                    // O relógio que o documento consulta. Ele é obrigatório
                    // porque o formato prevê valores que dependem do TEMPO —
                    // animações, contadores, watch faces. Nenhuma das nossas
                    // telas usa ainda, mas o parâmetro não é opcional.
                    CalendarSystemClock(),
                    // O CAMINHO DE VOLTA: toda ação nomeada que o documento
                    // disparar cai aqui. É o único ponto de contato entre o
                    // conteúdo remoto e o seu código.
                    { name, value, _ ->
                        receivedActions.add(if (value == null) name else "$name  (valor=$value)")
                    },
                )
            }
            Column(Modifier.fillMaxWidth()) {
                MetricRow("documento", "$size bytes")
                MetricRow("origem", "${DocumentClient.BASE}/documento/tela/${selected?.id}")
            }
        }

        // ── O que o documento mandou de volta ────────────────────────────
        if (selected?.id == "interativo") {
            Explanation(
                "Toque nos botões acima. Cada um dispara uma ação nomeada — uma " +
                    "string — que aparece na lista abaixo. Este app não sabe o " +
                    "que é um SKU, não sabe o que é \"favoritar\". Ele só recebe " +
                    "texto e decide o que fazer.",
            )

            SectionTitle("Ações recebidas do documento", Palette.Read)
            if (receivedActions.isEmpty()) {
                Text(
                    "(nenhuma ainda — toque nos botões)",
                    color = Palette.TextMuted,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 6.dp),
                )
            } else {
                receivedActions.forEach { action ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Palette.Read.copy(alpha = 0.12f))
                            .padding(10.dp),
                    ) {
                        Text(
                            action,
                            color = Palette.Read,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                }
            }

            CodeBlock(
                """
                // no SERVIDOR, ao desenhar o botão:
                Box(
                    modifier = Modifier
                        .background(ACENTO)
                        .onClick { hostAction("comprar:sku-1042") }
                ) { Text("Comprar agora") }

                // no APP, ao tocar o documento:
                RemoteComposePlayer(doc, …) { nome, valor, _ ->
                    // nome == "comprar:sku-1042"
                }
                """,
            )

            Callout(
                "Repare no contrato: é uma STRING. Nada de schema, nada de DTO, " +
                    "nada de versão. O servidor inventa nomes; o app reage aos " +
                    "que conhece e ignora com segurança os que não conhece. É o " +
                    "mesmo padrão de deep link — e tem a mesma virtude, que é " +
                    "acoplamento quase zero.",
                color = Palette.Read,
            )
        }

        if (document != null) {
            Callout(
                "Todas as cinco telas usam os mesmos poucos primitivos: Column, " +
                    "Row, Box, Text e modificadores. Não existe \"componente " +
                    "cartão\", não existe \"componente lista\", não existe " +
                    "\"componente divisória\" — divisória é um Box de 2px de " +
                    "altura. O formato só conhece formas, e isso é justamente o " +
                    "que permite ele desenhar coisas que o app nunca viu.",
                color = Palette.Write,
            )
        }
    }
}

/**
 * Altura da MOLDURA no app — quanto espaço da nossa tela o palco ocupa.
 *
 * Não confundir com `TelaRemota.altura`, que é a altura de referência do
 * DOCUMENTO e vem do servidor. Esta aqui é decisão de layout do app: quanto
 * espaço eu quero dar para exibir aquilo.
 */
private fun stageHeight(id: String?) = when (id) {
    "produtos" -> 340.dp
    "recibo", "interativo", "contador" -> 300.dp
    else -> 220.dp
}
