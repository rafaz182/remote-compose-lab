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
private const val CARD_BG = 0xFF1B3A4B.toInt()
private const val PROMO_BG = 0xFF4A1B2E.toInt()
private const val TITLE = 0xFF7FDBFF.toInt()
private const val PROMO_TITLE = 0xFFFF8FA3.toInt()
private const val BODY = 0xFFBFE9FF.toInt()

/**
 * Boas-vindas personalizadas.
 *
 * Repare que [name] é um parâmetro do SERVIDOR. O texto é interpolado antes
 * de virar bytes — o app recebe o documento já personalizado e não faz ideia
 * de que existiu um template.
 */
fun welcomeDocument(name: String): ByteArray = document(width = 1080, height = 400) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CARD_BG)
            .padding(20f),
    ) {
        Text("Olá, $name!", fontSize = RcSp(22f), color = TITLE)
        Text(
            "Esta tela foi gerada por um servidor Ktor,",
            fontSize = RcSp(14f),
            color = BODY,
        )
        Text(
            "em JVM pura, sem Android nenhum.",
            fontSize = RcSp(14f),
            color = BODY,
        )
    }
}

/**
 * Um banner promocional que o servidor liga e desliga em tempo real.
 *
 * É a demonstração clássica de Server-Driven UI: o app não precisa de release
 * novo para a promoção aparecer, sumir ou mudar de texto.
 */
fun promoDocument(promo: Promo?): ByteArray = document(width = 1080, height = 400) {
    if (promo == null) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CARD_BG)
                .padding(20f),
        ) {
            Text("Nenhuma promoção ativa", fontSize = RcSp(16f), color = BODY)
            Text(
                "O servidor decidiu não mostrar nada agora.",
                fontSize = RcSp(13f),
                color = BODY,
            )
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(PROMO_BG)
                .padding(20f),
        ) {
            Text(promo.headline, fontSize = RcSp(20f), color = PROMO_TITLE)
            Text(promo.description, fontSize = RcSp(14f), color = BODY)
            Text("por ${promo.price}", fontSize = RcSp(17f), color = PROMO_TITLE)
        }
    }
}

/** Estado da promoção, mantido em memória. Trocável por HTTP em tempo real. */
data class Promo(
    val headline: String,
    val description: String,
    val price: String,
)
