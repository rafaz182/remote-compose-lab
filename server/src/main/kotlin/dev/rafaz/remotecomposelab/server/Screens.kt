package dev.rafaz.remotecomposelab.server

import androidx.compose.remote.creation.dsl.Modifier
import androidx.compose.remote.creation.dsl.RcColumnScope
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
import androidx.compose.remote.creation.dsl.size
import androidx.compose.remote.creation.dsl.width
import androidx.compose.remote.creation.modifiers.CircleShape
import androidx.compose.remote.creation.modifiers.RoundedRectShape

/**
 * Telas mais ricas, todas geradas no servidor.
 *
 * A ordem aqui é de dificuldade crescente, e cada uma introduz um conceito:
 *
 *   1. cartao-perfil    -> Row, alinhamento vertical, clip circular
 *   2. painel-metricas  -> peso (weight): dividir espaço proporcionalmente
 *   3. lista-produtos   -> gerar UI a partir de DADOS do servidor
 *   4. recibo           -> spaceBetween, o truque que alinha rótulo e valor
 *   5. interativo       -> eventos: o documento conversa de volta com o app
 *
 * O ponto que atravessa todas: **é Kotlin comum**. `for`, `if`, `map`,
 * funções auxiliares — tudo funciona, porque a DSL é só código rodando no
 * servidor. Não existe linguagem de template, não existe interpretador de
 * expressão no meio. É a diferença mais concreta entre isto e um SDUI de JSON.
 */

// ───────────────────────────── paleta ─────────────────────────────
private const val BG = 0xFF14141B.toInt()
private const val CARD = 0xFF1E1E2A.toInt()
private const val CARD_HIGH = 0xFF272736.toInt()
private const val ACCENT = 0xFF9B7BFF.toInt()
private const val CYAN = 0xFF4DD0E1.toInt()
private const val GREEN = 0xFF6BCB77.toInt()
private const val RED = 0xFFFF6B6B.toInt()
private const val TEXT = 0xFFE8E8F0.toInt()
private const val TEXT_MUTED = 0xFF9A9AAE.toInt()

// ═════════════════════════════════════════════════════════════════
// 1. CARTÃO DE PERFIL — Row, alinhamento e clip
// ═════════════════════════════════════════════════════════════════

/**
 * Introduz três coisas:
 *
 * - **Row**: filhos lado a lado (a Column empilha).
 * - **alinhamento vertical**: `RcVerticalPositioning.Center` faz o avatar e o
 *   texto se alinharem pelo meio, mesmo tendo alturas diferentes.
 * - **clip**: `CircleShape()` recorta o Box num círculo. Repare que não existe
 *   "componente avatar" — é um quadrado colorido recortado. O formato só
 *   conhece formas.
 */
fun profileCardScreen(name: String, role: String, initials: String): ByteArray =
    document(width = 1080, height = 300) {
        Column(modifier = Modifier.fillMaxWidth().background(BG).padding(24f)) {
            label("PERFIL")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CARD)
                    .clip(RoundedRectShape(24f, 24f, 24f, 24f))
                    .padding(20f),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    RcRowHorizontalPositioning.Start,
                    RcVerticalPositioning.Center,
                ) {
                    // O "avatar": um Box quadrado, colorido e recortado em círculo.
                    Box(
                        modifier = Modifier
                            .size(96f)
                            .background(ACCENT)
                            .clip(CircleShape()),
                    ) {
                        Text(initials, fontSize = RcSp(34f), color = 0xFF1A1A1A.toInt())
                    }
                    Box(modifier = Modifier.width(20f)) {}
                    Column {
                        Text(name, fontSize = RcSp(30f), color = TEXT)
                        Text(role, fontSize = RcSp(18f), color = TEXT_MUTED)
                    }
                }
            }
        }
    }

// ═════════════════════════════════════════════════════════════════
// 2. PAINEL DE MÉTRICAS — peso (weight)
// ═════════════════════════════════════════════════════════════════

/**
 * Introduz **weight**, que é o conceito de layout que mais confunde no começo.
 *
 * `horizontalWeight(1f)` significa: "não tenho tamanho próprio; me dê uma fatia
 * proporcional do que sobrar". Três filhos com peso 1 dividem o espaço em três
 * partes iguais — independentemente do tamanho do texto de cada um.
 *
 * É o mesmo `weight` do Compose comum e do `flex-grow` do CSS. A diferença é
 * que aqui a divisão é calculada **no player**, com a largura real do aparelho
 * — o servidor não sabe e não precisa saber quantos pixels cada um vai ter.
 */
fun metricsPanelScreen(metrics: List<Metric>): ByteArray =
    document(width = 1080, height = 320) {
        Column(modifier = Modifier.fillMaxWidth().background(BG).padding(24f)) {
            label("MÉTRICAS DE HOJE")
            Row(modifier = Modifier.fillMaxWidth()) {
                metrics.forEachIndexed { IndexDto, m ->
                    Column(
                        modifier = Modifier
                            .horizontalWeight(1f)
                            .background(CARD)
                            .clip(RoundedRectShape(18f, 18f, 18f, 18f))
                            .padding(18f),
                    ) {
                        Text(m.value, fontSize = RcSp(34f), color = m.color)
                        Text(m.title, fontSize = RcSp(15f), color = TEXT_MUTED)
                        Text(m.change, fontSize = RcSp(15f), color = m.color)
                    }
                    // Espaçador entre os cartões, exceto depois do último.
                    if (IndexDto < metrics.size - 1) {
                        Box(modifier = Modifier.width(14f)) {}
                    }
                }
            }
        }
    }

data class Metric(val title: String, val value: String, val change: String, val color: Int)

// ═════════════════════════════════════════════════════════════════
// 3. LISTA DE PRODUTOS — UI gerada a partir de dados
// ═════════════════════════════════════════════════════════════════

/**
 * A tela que melhor mostra o valor prático da tecnologia.
 *
 * O parâmetro é uma `List<Produto>`. Um `forEach` de Kotlin vira linhas na
 * tela. Se o servidor buscar essa lista num banco, a UI muda sozinha —
 * **sem contrato de API, sem DTO, sem parser no app**.
 *
 * Compare mentalmente com o equivalente em JSON: você precisaria de um schema
 * de produto, uma classe no app, um `LazyColumn` já escrito, um componente de
 * linha já escrito, e qualquer campo novo exigiria release. Aqui o app não
 * sabe o que é um produto.
 */
fun productListScreen(products: List<Product>): ByteArray =
    document(width = 1080, height = 190 + products.size * 130) {
        Column(modifier = Modifier.fillMaxWidth().background(BG).padding(24f)) {
            label("CATÁLOGO  ·  ${products.size} ITENS")

            products.forEach { p ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(CARD)
                        .clip(RoundedRectShape(16f, 16f, 16f, 16f))
                        .padding(18f),
                    RcRowHorizontalPositioning.SpaceBetween,
                    RcVerticalPositioning.Center,
                ) {
                    Column {
                        Text(p.name, fontSize = RcSp(24f), color = TEXT)
                        Text(p.category, fontSize = RcSp(15f), color = TEXT_MUTED)
                    }
                    Column {
                        Text(p.price, fontSize = RcSp(26f), color = if (p.onSale) GREEN else CYAN)
                        if (p.onSale) {
                            Text("promoção", fontSize = RcSp(14f), color = GREEN)
                        }
                    }
                }
                Box(modifier = Modifier.height(12f)) {}
            }
        }
    }

data class Product(
    val name: String,
    val category: String,
    val price: String,
    val onSale: Boolean = false,
)

// ═════════════════════════════════════════════════════════════════
// 4. RECIBO — spaceBetween
// ═════════════════════════════════════════════════════════════════

/**
 * Introduz `SpaceBetween`, o truque de layout mais útil que existe.
 *
 * Numa Row com `SpaceBetween`, o primeiro filho gruda à esquerda, o último à
 * direita, e a sobra vira espaço no meio. É como se alinha rótulo e valor num
 * recibo **sem calcular largura nenhuma** — e funciona igual em qualquer
 * tamanho de tela, porque quem calcula é o player.
 *
 * Sem isso você precisaria de larguras fixas, e larguras fixas quebram no
 * primeiro aparelho diferente.
 */
fun receiptScreen(items: List<Pair<String, String>>, total: String): ByteArray =
    document(width = 1080, height = 240 + items.size * 70) {
        Column(modifier = Modifier.fillMaxWidth().background(BG).padding(24f)) {
            label("RECIBO")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CARD)
                    .clip(RoundedRectShape(20f, 20f, 20f, 20f))
                    .padding(22f),
            ) {
                items.forEach { (description, value) ->
                    spacedRow(description, value, RcSp(20f), TEXT_MUTED, TEXT)
                    Box(modifier = Modifier.height(10f)) {}
                }

                // "Divisória": um Box de 2px de altura ocupando a largura toda.
                // De novo — não existe componente Divider. Existe retângulo.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2f)
                        .background(CARD_HIGH),
                ) {}
                Box(modifier = Modifier.height(14f)) {}

                spacedRow("TOTAL", total, RcSp(28f), TEXT, ACCENT)
            }
        }
    }

// ═════════════════════════════════════════════════════════════════
// 5. INTERATIVO — o documento conversa de volta
// ═════════════════════════════════════════════════════════════════

/**
 * Aqui o documento deixa de ser desenho e vira interface.
 *
 * `onClick { hostAction("...") }` grava uma AÇÃO NOMEADA dentro do documento.
 * Quando o usuário toca, o player avisa o app hospedeiro passando aquela
 * string. O app decide o que fazer — navegar, chamar uma API, registrar
 * analytics.
 *
 * Repare no contrato: é só uma **string**. O app não conhece os botões, não
 * conhece a tela, não conhece os produtos. Ele só sabe reagir a nomes que
 * combinou de antemão — e nomes desconhecidos ele pode ignorar com segurança.
 *
 * É o mesmo padrão de `deep link`, e tem a mesma virtude: acoplamento mínimo.
 */
fun interactiveScreen(): ByteArray =
    document(width = 1080, height = 560) {
        Column(modifier = Modifier.fillMaxWidth().background(BG).padding(24f)) {
            label("TOQUE NOS BOTÕES")
            Text(
                "Cada toque manda uma string para o app.",
                fontSize = RcSp(18f),
                color = TEXT_MUTED,
            )
            Box(modifier = Modifier.height(18f)) {}

            button("Comprar agora", ACCENT, "comprar:sku-1042")
            Box(modifier = Modifier.height(12f)) {}
            button("Adicionar aos favoritos", CYAN, "favoritar:sku-1042")
            Box(modifier = Modifier.height(12f)) {}
            button("Denunciar anúncio", RED, "denunciar:sku-1042")

            Box(modifier = Modifier.height(18f)) {}
            Text(
                "O app não sabe o que é um SKU. Ele só recebe o texto.",
                fontSize = RcSp(15f),
                color = TEXT_MUTED,
            )
        }
    }

// ───────────────────────── auxiliares ─────────────────────────
//
// Funções normais de Kotlin. É isto que torna a DSL de servidor tão
// confortável: componentização é só extrair função, sem cerimônia nenhuma.

/** Um botão: Box colorido, arredondado e clicável. */
private fun RcScope.button(text: String, color: Int, action: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(color)
            .clip(RoundedRectShape(14f, 14f, 14f, 14f))
            .padding(18f)
            .onClick { hostAction(action) },
    ) {
        Text(text, fontSize = RcSp(22f), color = 0xFF14141B.toInt())
    }
}

/** Rótulo de seção. */
private fun RcScope.label(text: String) {
    Text(text, fontSize = RcSp(16f), color = ACCENT)
    Box(modifier = Modifier.height(14f)) {}
}

/** Uma linha "rótulo à esquerda, valor à direita". */
private fun RcColumnScope.spacedRow(
    left: String,
    right: String,
    size: RcSp,
    leftColor: Int,
    rightColor: Int,
) {
    Row(
        Modifier.fillMaxWidth(),
        RcRowHorizontalPositioning.SpaceBetween,
    ) {
        Text(left, fontSize = size, color = leftColor)
        Text(right, fontSize = size, color = rightColor)
    }
}
