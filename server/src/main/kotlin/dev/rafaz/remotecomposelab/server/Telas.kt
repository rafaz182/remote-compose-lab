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
private const val FUNDO = 0xFF14141B.toInt()
private const val CARTAO = 0xFF1E1E2A.toInt()
private const val CARTAO_ALTO = 0xFF272736.toInt()
private const val ACENTO = 0xFF9B7BFF.toInt()
private const val CIANO = 0xFF4DD0E1.toInt()
private const val VERDE = 0xFF6BCB77.toInt()
private const val VERMELHO = 0xFFFF6B6B.toInt()
private const val TEXTO = 0xFFE8E8F0.toInt()
private const val TEXTO_FRACO = 0xFF9A9AAE.toInt()

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
fun telaCartaoPerfil(nome: String, cargo: String, iniciais: String): ByteArray =
    documento(largura = 1080, altura = 300) {
        Column(modifier = Modifier.fillMaxWidth().background(FUNDO).padding(24f)) {
            rotulo("PERFIL")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CARTAO)
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
                            .background(ACENTO)
                            .clip(CircleShape()),
                    ) {
                        Text(iniciais, fontSize = RcSp(34f), color = 0xFF1A1A1A.toInt())
                    }
                    Box(modifier = Modifier.width(20f)) {}
                    Column {
                        Text(nome, fontSize = RcSp(30f), color = TEXTO)
                        Text(cargo, fontSize = RcSp(18f), color = TEXTO_FRACO)
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
fun telaPainelMetricas(metricas: List<Metrica>): ByteArray =
    documento(largura = 1080, altura = 320) {
        Column(modifier = Modifier.fillMaxWidth().background(FUNDO).padding(24f)) {
            rotulo("MÉTRICAS DE HOJE")
            Row(modifier = Modifier.fillMaxWidth()) {
                metricas.forEachIndexed { indice, m ->
                    Column(
                        modifier = Modifier
                            .horizontalWeight(1f)
                            .background(CARTAO)
                            .clip(RoundedRectShape(18f, 18f, 18f, 18f))
                            .padding(18f),
                    ) {
                        Text(m.valor, fontSize = RcSp(34f), color = m.cor)
                        Text(m.titulo, fontSize = RcSp(15f), color = TEXTO_FRACO)
                        Text(m.variacao, fontSize = RcSp(15f), color = m.cor)
                    }
                    // Espaçador entre os cartões, exceto depois do último.
                    if (indice < metricas.size - 1) {
                        Box(modifier = Modifier.width(14f)) {}
                    }
                }
            }
        }
    }

data class Metrica(val titulo: String, val valor: String, val variacao: String, val cor: Int)

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
fun telaListaProdutos(produtos: List<Produto>): ByteArray =
    documento(largura = 1080, altura = 190 + produtos.size * 130) {
        Column(modifier = Modifier.fillMaxWidth().background(FUNDO).padding(24f)) {
            rotulo("CATÁLOGO  ·  ${produtos.size} ITENS")

            produtos.forEach { p ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(CARTAO)
                        .clip(RoundedRectShape(16f, 16f, 16f, 16f))
                        .padding(18f),
                    RcRowHorizontalPositioning.SpaceBetween,
                    RcVerticalPositioning.Center,
                ) {
                    Column {
                        Text(p.nome, fontSize = RcSp(24f), color = TEXTO)
                        Text(p.categoria, fontSize = RcSp(15f), color = TEXTO_FRACO)
                    }
                    Column {
                        Text(p.preco, fontSize = RcSp(26f), color = if (p.emPromocao) VERDE else CIANO)
                        if (p.emPromocao) {
                            Text("promoção", fontSize = RcSp(14f), color = VERDE)
                        }
                    }
                }
                Box(modifier = Modifier.height(12f)) {}
            }
        }
    }

data class Produto(
    val nome: String,
    val categoria: String,
    val preco: String,
    val emPromocao: Boolean = false,
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
fun telaRecibo(itens: List<Pair<String, String>>, total: String): ByteArray =
    documento(largura = 1080, altura = 240 + itens.size * 70) {
        Column(modifier = Modifier.fillMaxWidth().background(FUNDO).padding(24f)) {
            rotulo("RECIBO")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CARTAO)
                    .clip(RoundedRectShape(20f, 20f, 20f, 20f))
                    .padding(22f),
            ) {
                itens.forEach { (descricao, valor) ->
                    linhaEntre(descricao, valor, RcSp(20f), TEXTO_FRACO, TEXTO)
                    Box(modifier = Modifier.height(10f)) {}
                }

                // "Divisória": um Box de 2px de altura ocupando a largura toda.
                // De novo — não existe componente Divider. Existe retângulo.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2f)
                        .background(CARTAO_ALTO),
                ) {}
                Box(modifier = Modifier.height(14f)) {}

                linhaEntre("TOTAL", total, RcSp(28f), TEXTO, ACENTO)
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
fun telaInterativa(): ByteArray =
    documento(largura = 1080, altura = 560) {
        Column(modifier = Modifier.fillMaxWidth().background(FUNDO).padding(24f)) {
            rotulo("TOQUE NOS BOTÕES")
            Text(
                "Cada toque manda uma string para o app.",
                fontSize = RcSp(18f),
                color = TEXTO_FRACO,
            )
            Box(modifier = Modifier.height(18f)) {}

            botao("Comprar agora", ACENTO, "comprar:sku-1042")
            Box(modifier = Modifier.height(12f)) {}
            botao("Adicionar aos favoritos", CIANO, "favoritar:sku-1042")
            Box(modifier = Modifier.height(12f)) {}
            botao("Denunciar anúncio", VERMELHO, "denunciar:sku-1042")

            Box(modifier = Modifier.height(18f)) {}
            Text(
                "O app não sabe o que é um SKU. Ele só recebe o texto.",
                fontSize = RcSp(15f),
                color = TEXTO_FRACO,
            )
        }
    }

// ───────────────────────── auxiliares ─────────────────────────
//
// Funções normais de Kotlin. É isto que torna a DSL de servidor tão
// confortável: componentização é só extrair função, sem cerimônia nenhuma.

/** Um botão: Box colorido, arredondado e clicável. */
private fun RcScope.botao(texto: String, cor: Int, acao: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(cor)
            .clip(RoundedRectShape(14f, 14f, 14f, 14f))
            .padding(18f)
            .onClick { hostAction(acao) },
    ) {
        Text(texto, fontSize = RcSp(22f), color = 0xFF14141B.toInt())
    }
}

/** Rótulo de seção. */
private fun RcScope.rotulo(texto: String) {
    Text(texto, fontSize = RcSp(16f), color = ACENTO)
    Box(modifier = Modifier.height(14f)) {}
}

/** Uma linha "rótulo à esquerda, valor à direita". */
private fun RcColumnScope.linhaEntre(
    esquerda: String,
    direita: String,
    tamanho: RcSp,
    corEsquerda: Int,
    corDireita: Int,
) {
    Row(
        Modifier.fillMaxWidth(),
        RcRowHorizontalPositioning.SpaceBetween,
    ) {
        Text(esquerda, fontSize = tamanho, color = corEsquerda)
        Text(direita, fontSize = tamanho, color = corDireita)
    }
}
