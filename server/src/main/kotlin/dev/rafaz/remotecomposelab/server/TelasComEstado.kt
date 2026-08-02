package dev.rafaz.remotecomposelab.server

import androidx.compose.remote.creation.dsl.Modifier
import androidx.compose.remote.creation.dsl.RcRowHorizontalPositioning
import androidx.compose.remote.creation.dsl.RcScope
import androidx.compose.remote.creation.dsl.RcSp
import androidx.compose.remote.creation.dsl.RcVerticalPositioning
import androidx.compose.remote.creation.dsl.background
import androidx.compose.remote.creation.dsl.clip
import androidx.compose.remote.creation.dsl.fillMaxWidth
import androidx.compose.remote.creation.dsl.height
import androidx.compose.remote.creation.dsl.horizontalWeight
import androidx.compose.remote.creation.dsl.onClick
import androidx.compose.remote.creation.dsl.padding
// O operador `+` entre Float e RcFloat é uma extensão de nível superior da
// biblioteca. Sem este import, o Kotlin só enxerga o `plus` normal de Float e
// reclama que nenhum candidato serve — mensagem confusa para um import faltando.
import androidx.compose.remote.creation.dsl.plus
import androidx.compose.remote.creation.modifiers.RoundedRectShape

/**
 * ═══════════════════════════════════════════════════════════════════════
 *  ESTADO REMOTO — o documento deixa de ser desenho e vira PROGRAMA
 * ═══════════════════════════════════════════════════════════════════════
 *
 * Até aqui, tudo que o servidor gerava era estático: uma fotografia. Para
 * mudar qualquer coisa, era preciso gerar um documento novo e baixá-lo.
 *
 * Estas duas telas quebram essa premissa.
 *
 * O documento pode conter **valores** (`RcFloat`) e **expressões** sobre eles.
 * Quem avalia essas expressões é o player, a cada quadro. Consequência direta
 * e bem estranha na primeira vez que se vê:
 *
 *   ► o contador incrementa SEM tocar na rede
 *   ► o relógio anda SEM o app fazer nada
 *
 * Nenhum byte trafega depois que o documento chega. O app nem fica sabendo.
 *
 * ── O que torna isso possível ──────────────────────────────────────────
 *
 * `RcFloat` **não é um número**. É a referência a um espaço de valor dentro
 * do documento. Quando você escreve `1f + contador`, não está somando: está
 * gravando a *fórmula* "um mais o valor daquele espaço". O resultado é
 * calculado no destino.
 *
 * É a mesma ideia de uma célula de planilha: `=A1+1` não guarda um número,
 * guarda uma relação.
 */

private const val FUNDO_E = 0xFF14141B.toInt()
private const val CARTAO_E = 0xFF1E1E2A.toInt()
private const val ACENTO_E = 0xFF9B7BFF.toInt()
private const val CIANO_E = 0xFF4DD0E1.toInt()
private const val VERDE_E = 0xFF6BCB77.toInt()
private const val TEXTO_E = 0xFFE8E8F0.toInt()
private const val FRACO_E = 0xFF9A9AAE.toInt()

// ═════════════════════════════════════════════════════════════════
// 1. CONTADOR — estado que muda sem rede
// ═════════════════════════════════════════════════════════════════

/**
 * Um contador com botões de + e −.
 *
 * Repare no que **não** existe aqui: nenhuma chamada de rede, nenhum
 * `hostAction`, nenhum callback para o app. O toque muda um valor que vive
 * dentro do documento, e o texto que mostra esse valor se atualiza sozinho.
 *
 * Três peças fazem isso funcionar:
 *
 * 1. `RcFloat(writer, 0f)` — declara o espaço de valor, com 0 inicial.
 *
 * 2. `named(contador, "contador")` — dá um nome a ele. Nomear é opcional para
 *    o funcionamento interno, mas é o que permite ao APP hospedeiro encontrar
 *    e alterar esse valor depois, via `StateUpdater`. É a porta dos fundos
 *    entre os dois mundos.
 *
 * 3. `onClick { setValue(contador, 1f + contador) }` — grava no documento a
 *    instrução "ao tocar, atribua a este espaço o valor dele mais um".
 *
 * Note a ordem em `1f + contador`. A biblioteca oferece `Float.plus(RcFloat)`,
 * e não o contrário — então soma-se o literal ao remoto, nunca o inverso.
 * Para subtrair, o truque é somar um negativo: `(-1f) + contador`.
 */
fun telaContador(): ByteArray = documento(largura = 1080, altura = 520) { writer ->
    // O espaço de valor. A partir daqui, `contador` é uma REFERÊNCIA, não um
    // número — some, multiplique, compare: tudo vira fórmula gravada.
    val contador = PonteRc.valorFloat(writer, 7f).named("contador")

    Column(modifier = Modifier.fillMaxWidth().background(FUNDO_E).padding(28f)) {
        Text("CONTADOR", fontSize = RcSp(16f), color = ACENTO_E)
        Text(
            "O valor vive dentro do documento.",
            fontSize = RcSp(17f),
            color = FRACO_E,
        )
        Box(modifier = Modifier.height(20f)) {}

        // O visor. `createTextFromFloat` converte o valor numérico em texto
        // exibível — e essa conversão também acontece no player, a cada
        // mudança. Os três números são: dígitos antes da vírgula, dígitos
        // depois, e flags de preenchimento.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(CARTAO_E)
                .clip(RoundedRectShape(20f, 20f, 20f, 20f))
                .padding(28f),
        ) {
            Text(
                createTextFromFloat(contador, 3, 0, 0),
                fontSize = RcSp(64f),
                color = CIANO_E,
            )
        }

        Box(modifier = Modifier.height(20f)) {}

        Row(
            Modifier.fillMaxWidth(),
            RcRowHorizontalPositioning.SpaceBetween,
            RcVerticalPositioning.Center,
        ) {
            botaoEstado("−  1", 0xFFFF6B6B.toInt()) { setValue(contador, (-1f) + contador) }
            botaoEstado("zerar", FRACO_E) { setValue(contador, 0f) }
            botaoEstado("+  1", VERDE_E) { setValue(contador, 1f + contador) }
        }

        Box(modifier = Modifier.height(18f)) {}
        Text(
            "Nenhum byte trafega ao tocar. O app nem fica sabendo.",
            fontSize = RcSp(15f),
            color = FRACO_E,
        )
    }
}

// ═════════════════════════════════════════════════════════════════
// 1b. REPRODUÇÃO MÍNIMA — para isolar por que `setValue` não funciona
// ═════════════════════════════════════════════════════════════════

/**
 * A menor tela possível que exercita `setValue`.
 *
 * Existe só como instrumento de diagnóstico. O contador não funciona, e há
 * três suspeitos: `named()`, a aritmética (`1f + contador`) e o próprio
 * `setValue`. Esta tela remove os dois primeiros:
 *
 *   - sem `named()`
 *   - sem expressão: atribui a constante 99
 *   - um único botão, sem Row nem weight
 *
 * Se o número virar 99 aqui, o problema está em `named()` ou na aritmética.
 * Se continuar 1, o problema é o `setValue` em si.
 *
 * É a mesma disciplina de sempre: quando algo não funciona e não há erro,
 * reduza até sobrar uma variável só.
 */
fun telaTesteEstado(): ByteArray = documento(largura = 1080, altura = 380) { writer ->
    val valor = PonteRc.valorFloat(writer, 1f)

    Column(modifier = Modifier.fillMaxWidth().background(FUNDO_E).padding(28f)) {
        Text("REPRODUÇÃO MÍNIMA", fontSize = RcSp(16f), color = ACENTO_E)
        Text("sem named(), sem aritmética", fontSize = RcSp(16f), color = FRACO_E)
        Box(modifier = Modifier.height(16f)) {}

        Text(createTextFromFloat(valor, 3, 0, 0), fontSize = RcSp(72f), color = CIANO_E)

        Box(modifier = Modifier.height(16f)) {}
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(VERDE_E)
                .clip(RoundedRectShape(14f, 14f, 14f, 14f))
                .padding(22f)
                .onClick { setValue(valor, 99f) },
        ) {
            Text("virar 99", fontSize = RcSp(26f), color = 0xFF14141B.toInt())
        }
    }
}

// ═════════════════════════════════════════════════════════════════
// 2. RELÓGIO — o documento lê o tempo sozinho
// ═════════════════════════════════════════════════════════════════

/**
 * Um relógio que anda, gerado uma única vez no servidor.
 *
 * Esta é a tela que mais deixa claro por que Remote Compose existe do jeito
 * que existe. O servidor não manda a hora — ele manda a **instrução de
 * consultar a hora**. Funções como `hour()`, `minutes()` e `seconds()`
 * devolvem `RcFloat` ligados ao relógio do dispositivo, avaliados a cada
 * quadro pelo player.
 *
 * Consequência prática: este documento poderia ser gerado hoje, ficar em
 * cache seis meses num CDN, e ainda assim mostrar a hora certa. Ele não
 * contém nenhum horário — contém a fórmula.
 *
 * É exatamente esse mecanismo que torna a tecnologia adequada a **watch
 * faces**, que é de onde ela vem.
 *
 * (E é também a antessala das animações: se o tempo é um valor comum, então
 * qualquer propriedade visual pode ser função dele.)
 */
fun telaRelogio(): ByteArray = documento(largura = 1080, altura = 460) {
    Column(modifier = Modifier.fillMaxWidth().background(FUNDO_E).padding(28f)) {
        Text("RELÓGIO", fontSize = RcSp(16f), color = ACENTO_E)
        Text(
            "Gerado uma vez. Anda sozinho.",
            fontSize = RcSp(17f),
            color = FRACO_E,
        )
        Box(modifier = Modifier.height(22f)) {}

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(CARTAO_E)
                .clip(RoundedRectShape(20f, 20f, 20f, 20f))
                .padding(26f),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                RcRowHorizontalPositioning.Center,
                RcVerticalPositioning.Center,
            ) {
                // Cada peça é uma leitura do relógio do APARELHO, não do
                // servidor. `2` dígitos antes da vírgula e flag de
                // preenchimento com zero deixam "07" em vez de "7".
                Text(createTextFromFloat(hour(), 2, 0, PAD_ZERO), fontSize = RcSp(72f), color = TEXTO_E)
                Text(":", fontSize = RcSp(72f), color = ACENTO_E)
                Text(createTextFromFloat(minutes(), 2, 0, PAD_ZERO), fontSize = RcSp(72f), color = TEXTO_E)
                Text(":", fontSize = RcSp(72f), color = ACENTO_E)
                Text(createTextFromFloat(seconds(), 2, 0, PAD_ZERO), fontSize = RcSp(72f), color = CIANO_E)
            }
        }

        Box(modifier = Modifier.height(20f)) {}
        Text(
            "O servidor não mandou a hora — mandou a instrução de consultá-la.",
            fontSize = RcSp(15f),
            color = FRACO_E,
        )
    }
}

/**
 * Flag de preenchimento com zero à esquerda.
 *
 * Vem de `RemoteComposeBuffer.PAD_PRE_ZERO`. Repetimos a constante aqui
 * porque o valor é estável e assim a tela fica legível — mas se um dia o
 * relógio mostrar " 7" em vez de "07", é aqui que se investiga.
 */
private const val PAD_ZERO = 2

/** Um botão que altera estado interno em vez de avisar o app. */
private fun RcScope.botaoEstado(
    texto: String,
    cor: Int,
    acao: androidx.compose.remote.creation.dsl.RcActionScope.() -> Unit,
) {
    Box(
        modifier = Modifier
            .horizontalWeight(1f)
            .background(cor)
            .clip(RoundedRectShape(14f, 14f, 14f, 14f))
            .padding(20f)
            .onClick { acao() },
    ) {
        Text(texto, fontSize = RcSp(24f), color = 0xFF14141B.toInt())
    }
}
