package dev.rafaz.remotecomposelab.licoes

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
import dev.rafaz.remotecomposelab.remoto.ClienteDeDocumentos
import dev.rafaz.remotecomposelab.remoto.TelaRemota
import dev.rafaz.remotecomposelab.ui.BlocoCodigo
import dev.rafaz.remotecomposelab.ui.Cores
import dev.rafaz.remotecomposelab.ui.Destaque
import dev.rafaz.remotecomposelab.ui.Explicacao
import dev.rafaz.remotecomposelab.ui.LinhaMetrica
import dev.rafaz.remotecomposelab.ui.Palco
import dev.rafaz.remotecomposelab.ui.TituloSecao
import kotlinx.coroutines.launch

/**
 * AULA 05 — A galeria do servidor
 *
 * Cinco telas de complexidade crescente, todas desenhadas no backend. E o
 * caminho de volta: eventos saindo do documento para o app.
 */
@Composable
fun L05GaleriaDoServidor() {
    val escopo = rememberCoroutineScope()

    var telas by remember { mutableStateOf<List<TelaRemota>>(emptyList()) }
    var selecionada by remember { mutableStateOf<TelaRemota?>(null) }
    var documento by remember { mutableStateOf<RemoteDocument?>(null) }
    var tamanho by remember { mutableStateOf(0) }
    var erro by remember { mutableStateOf<String?>(null) }

    // O histórico de ações que o DOCUMENTO mandou para o APP.
    val acoesRecebidas = remember { mutableStateListOf<String>() }

    fun carregarCatalogo() {
        escopo.launch {
            runCatching { ClienteDeDocumentos.buscarTelas() }
                .onSuccess { telas = it; erro = null }
                .onFailure { erro = it.message ?: it.toString() }
        }
    }

    fun abrir(tela: TelaRemota) {
        escopo.launch {
            runCatching { ClienteDeDocumentos.buscar("/documento/tela/${tela.id}") }
                .onSuccess { bytes ->
                    tamanho = bytes.size
                    selecionada = tela
                    documento = RemoteDocument(bytes)
                    acoesRecebidas.clear()
                    erro = null
                }
                .onFailure { erro = it.message ?: it.toString() }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {

        Explicacao(
            "Cinco telas, todas desenhadas no servidor, em ordem de " +
                "dificuldade. Nenhum componente delas existe no código deste " +
                "app — nem cartão, nem lista, nem botão.\n\n" +
                "Suba o servidor e carregue o catálogo:",
        )
        BlocoCodigo(".\\gradlew.bat :server:run")

        Button(onClick = { carregarCatalogo() }, modifier = Modifier.fillMaxWidth()) {
            Text(if (telas.isEmpty()) "Carregar catálogo" else "Recarregar catálogo")
        }

        erro?.let {
            Destaque(
                "Falhou: $it\n\nO servidor está no ar? (.\\gradlew.bat :server:run)",
                cor = Cores.Alerta,
            )
        }

        // ── O menu vem em JSON; as telas vêm em bytes ────────────────────
        if (telas.isNotEmpty()) {
            TituloSecao("Telas disponíveis", Cores.Escrita)
            telas.forEach { tela ->
                val ativa = selecionada?.id == tela.id
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (ativa) Cores.Escrita.copy(alpha = 0.16f) else Cores.Superficie)
                        .border(
                            1.dp,
                            if (ativa) Cores.Escrita else Cores.SuperficieAlta,
                            RoundedCornerShape(12.dp),
                        )
                        .clickable { abrir(tela) }
                        .padding(14.dp),
                ) {
                    Text(tela.titulo, color = Cores.Texto, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        tela.ensina,
                        color = Cores.TextoFraco,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                Box(Modifier.height(8.dp))
            }
        }

        // ── O palco ──────────────────────────────────────────────────────
        documento?.let { doc ->
            Palco("Renderizado do documento · ${selecionada?.titulo}") {
                RemoteComposePlayer(
                    doc,
                    Modifier.fillMaxWidth().height(alturaDoPalco(selecionada?.id)),
                    1080,
                    // A altura vem do CATÁLOGO, não de um `when` no app.
                    selecionada?.altura ?: 400,
                    // O relógio que o documento consulta. Ele é obrigatório
                    // porque o formato prevê valores que dependem do TEMPO —
                    // animações, contadores, watch faces. Nenhuma das nossas
                    // telas usa ainda, mas o parâmetro não é opcional.
                    CalendarSystemClock(),
                    // O CAMINHO DE VOLTA: toda ação nomeada que o documento
                    // disparar cai aqui. É o único ponto de contato entre o
                    // conteúdo remoto e o seu código.
                    { nome, valor, _ ->
                        acoesRecebidas.add(if (valor == null) nome else "$nome  (valor=$valor)")
                    },
                )
            }
            Column(Modifier.fillMaxWidth()) {
                LinhaMetrica("documento", "$tamanho bytes")
                LinhaMetrica("origem", "${ClienteDeDocumentos.BASE}/documento/tela/${selecionada?.id}")
            }
        }

        // ── O que o documento mandou de volta ────────────────────────────
        if (selecionada?.id == "interativo") {
            Explicacao(
                "Toque nos botões acima. Cada um dispara uma ação nomeada — uma " +
                    "string — que aparece na lista abaixo. Este app não sabe o " +
                    "que é um SKU, não sabe o que é \"favoritar\". Ele só recebe " +
                    "texto e decide o que fazer.",
            )

            TituloSecao("Ações recebidas do documento", Cores.Leitura)
            if (acoesRecebidas.isEmpty()) {
                Text(
                    "(nenhuma ainda — toque nos botões)",
                    color = Cores.TextoFraco,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 6.dp),
                )
            } else {
                acoesRecebidas.forEach { acao ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Cores.Leitura.copy(alpha = 0.12f))
                            .padding(10.dp),
                    ) {
                        Text(
                            acao,
                            color = Cores.Leitura,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                }
            }

            BlocoCodigo(
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

            Destaque(
                "Repare no contrato: é uma STRING. Nada de schema, nada de DTO, " +
                    "nada de versão. O servidor inventa nomes; o app reage aos " +
                    "que conhece e ignora com segurança os que não conhece. É o " +
                    "mesmo padrão de deep link — e tem a mesma virtude, que é " +
                    "acoplamento quase zero.",
                cor = Cores.Leitura,
            )
        }

        if (documento != null) {
            Destaque(
                "Todas as cinco telas usam os mesmos poucos primitivos: Column, " +
                    "Row, Box, Text e modificadores. Não existe \"componente " +
                    "cartão\", não existe \"componente lista\", não existe " +
                    "\"componente divisória\" — divisória é um Box de 2px de " +
                    "altura. O formato só conhece formas, e isso é justamente o " +
                    "que permite ele desenhar coisas que o app nunca viu.",
                cor = Cores.Escrita,
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
private fun alturaDoPalco(id: String?) = when (id) {
    "produtos" -> 340.dp
    "recibo", "interativo", "contador" -> 300.dp
    else -> 220.dp
}
