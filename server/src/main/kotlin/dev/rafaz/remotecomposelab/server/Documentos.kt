package dev.rafaz.remotecomposelab.server

import androidx.compose.remote.creation.dsl.Modifier
import androidx.compose.remote.creation.dsl.RcSp
import androidx.compose.remote.creation.dsl.background
import androidx.compose.remote.creation.dsl.fillMaxWidth
import androidx.compose.remote.creation.dsl.padding

/**
 * O catálogo de documentos que o servidor sabe gerar.
 *
 * ESTE É O ARQUIVO QUE PROVA A TESE. Tudo aqui é interface de usuário —
 * cores, espaçamentos, textos, hierarquia — escrita num servidor, sem
 * nenhuma linha de Android por perto. O aplicativo que vai exibir isto não
 * conhece nenhum destes componentes.
 *
 * ─────────────────────────────────────────────────────────────────────────
 * NÃO ENVOLVA O CONTEÚDO EM `RcRoot { }`.
 *
 * Parece natural escrever `documento { RcRoot { Column { ... } } }` — e
 * compila, e gera bytes, e o app baixa sem erro. E renderiza uma tela em
 * branco, sem uma única linha de log.
 *
 * `createRcBuffer` JÁ cria o componente raiz. Um `RcRoot` explícito cria uma
 * SEGUNDA raiz, o motor de layout não sabe resolver duas, e o documento
 * termina com dimensão 0x0. Área zero desenha zero.
 *
 * Só descobrimos comparando as estatísticas de um documento que funcionava
 * (gerado no app) com um que não funcionava (gerado aqui):
 *
 *     local    -> 910x315 | RootLayoutComponent : 1  ✅
 *     servidor ->   0x0   | RootLayoutComponent : 2  ❌
 *
 * A linha `RootLayoutComponent : 2` foi a pista inteira.
 * ─────────────────────────────────────────────────────────────────────────
 */

// Paleta. Cores aqui são Int no formato ARGB.
private const val FUNDO_CARTAO = 0xFF1B3A4B.toInt()
private const val FUNDO_PROMO = 0xFF4A1B2E.toInt()
private const val TITULO = 0xFF7FDBFF.toInt()
private const val TITULO_PROMO = 0xFFFF8FA3.toInt()
private const val CORPO = 0xFFBFE9FF.toInt()

/**
 * Boas-vindas personalizadas.
 *
 * Repare que [nome] é um parâmetro do SERVIDOR. O texto é interpolado antes
 * de virar bytes — o app recebe o documento já personalizado e não faz ideia
 * de que existiu um template.
 */
fun documentoBoasVindas(nome: String): ByteArray = documento(largura = 1080, altura = 400) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(FUNDO_CARTAO)
            .padding(20f),
    ) {
        Text("Olá, $nome!", fontSize = RcSp(22f), color = TITULO)
        Text(
            "Esta tela foi gerada por um servidor Ktor,",
            fontSize = RcSp(14f),
            color = CORPO,
        )
        Text(
            "em JVM pura, sem Android nenhum.",
            fontSize = RcSp(14f),
            color = CORPO,
        )
    }
}

/**
 * Um banner promocional que o servidor liga e desliga em tempo real.
 *
 * É a demonstração clássica de Server-Driven UI: o app não precisa de release
 * novo para a promoção aparecer, sumir ou mudar de texto.
 */
fun documentoPromocao(promo: Promocao?): ByteArray = documento(largura = 1080, altura = 400) {
    if (promo == null) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(FUNDO_CARTAO)
                .padding(20f),
        ) {
            Text("Nenhuma promoção ativa", fontSize = RcSp(16f), color = CORPO)
            Text(
                "O servidor decidiu não mostrar nada agora.",
                fontSize = RcSp(13f),
                color = CORPO,
            )
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(FUNDO_PROMO)
                .padding(20f),
        ) {
            Text(promo.chamada, fontSize = RcSp(20f), color = TITULO_PROMO)
            Text(promo.descricao, fontSize = RcSp(14f), color = CORPO)
            Text("por ${promo.preco}", fontSize = RcSp(17f), color = TITULO_PROMO)
        }
    }
}

/** Estado da promoção, mantido em memória. Trocável por HTTP em tempo real. */
data class Promocao(
    val chamada: String,
    val descricao: String,
    val preco: String,
)
