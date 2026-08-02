package dev.rafaz.remotecomposelab.server

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable
import java.util.concurrent.atomic.AtomicReference

/**
 * O servidor.
 *
 * O que ele faz de diferente de qualquer outro backend: os endpoints sob
 * `/documento` não devolvem JSON descrevendo uma tela. Devolvem a **tela**,
 * como bytes executáveis.
 *
 * (Curiosidade que custou uma build: escrever `/documento` seguido de asterisco
 * aqui dentro quebra a compilação. Comentários de bloco em Kotlin ANINHAM, e
 * essa sequência abre um comentário interno que nunca fecha.)
 *
 *     Content-Type: application/octet-stream
 *
 * Suba com:  .\gradlew.bat :server:run
 */

/**
 * Estado da promoção, em memória.
 *
 * Um `AtomicReference` num objeto global seria imperdoável em produção — aqui
 * é proposital: queremos que você mude a UI do app por `curl`, num terminal,
 * e veja a tela mudar sem recompilar nada.
 */
private val promocaoAtual = AtomicReference<Promocao?>(
    Promocao(
        chamada = "Leve 3, pague 2",
        descricao = "Só até domingo, em toda a loja.",
        preco = "R$ 49,90",
    ),
)

@Serializable
data class PromocaoDto(
    val chamada: String,
    val descricao: String,
    val preco: String,
)

@Serializable
data class Indice(
    val servico: String,
    val versaoRemoteCompose: String,
    val apiLevelDoFormato: Int,
    val endpoints: List<String>,
)

@Serializable
data class TelaDto(val id: String, val titulo: String, val ensina: String)

/**
 * O catálogo de telas ricas, em ordem de dificuldade.
 *
 * Os dados de exemplo moram aqui, no servidor. Trocar qualquer coisa nesta
 * lista muda o app de todo mundo, sem release — que é o ponto inteiro.
 */
private val TELAS: Map<String, Pair<TelaDto, () -> ByteArray>> = linkedMapOf(
    "perfil" to (
        TelaDto("perfil", "Cartão de perfil", "Row, alinhamento vertical e clip circular") to
            { telaCartaoPerfil("Rafael Ramos", "Desenvolvedor Android", "RR") }
        ),
    "metricas" to (
        TelaDto("metricas", "Painel de métricas", "weight: dividir espaço proporcionalmente") to
            {
                telaPainelMetricas(
                    listOf(
                        Metrica("Vendas", "1.284", "+12%", 0xFF6BCB77.toInt()),
                        Metrica("Devoluções", "37", "-4%", 0xFF4DD0E1.toInt()),
                        Metrica("Ticket médio", "R$ 89", "+3%", 0xFF9B7BFF.toInt()),
                    ),
                )
            }
        ),
    "produtos" to (
        TelaDto("produtos", "Lista de produtos", "gerar UI a partir de dados do servidor") to
            {
                telaListaProdutos(
                    listOf(
                        Produto("Teclado mecânico", "Periféricos", "R$ 349,00"),
                        Produto("Monitor 27\"", "Monitores", "R$ 1.299,00", emPromocao = true),
                        Produto("Cadeira ergonômica", "Móveis", "R$ 2.150,00"),
                        Produto("Webcam 1080p", "Periféricos", "R$ 279,00", emPromocao = true),
                    ),
                )
            }
        ),
    "recibo" to (
        TelaDto("recibo", "Recibo", "spaceBetween: alinhar rótulo e valor sem calcular largura") to
            {
                telaRecibo(
                    listOf(
                        "Monitor 27\"" to "R$ 1.299,00",
                        "Webcam 1080p" to "R$ 279,00",
                        "Frete" to "R$ 0,00",
                        "Desconto" to "− R$ 120,00",
                    ),
                    total = "R$ 1.458,00",
                )
            }
        ),
    "interativo" to (
        TelaDto("interativo", "Botões interativos", "eventos: o documento conversa com o app") to
            { telaInterativa() }
        ),
)

fun main() {
    // host = "0.0.0.0" e não "localhost": o emulador do Android é outra
    // máquina virtual. Ele alcança o host pelo IP mágico 10.0.2.2, mas só se
    // o servidor estiver ouvindo em todas as interfaces.
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        install(ContentNegotiation) { json() }
        install(CallLogging)

        routing {
            get("/") {
                call.respond(
                    Indice(
                        servico = "Remote Compose Lab — servidor de documentos",
                        versaoRemoteCompose = "1.0.0-alpha16",
                        apiLevelDoFormato = API_LEVEL_MINIMO,
                        endpoints = listOf(
                            "GET  /documento/boas-vindas?nome=Rafael   -> bytes do documento",
                            "GET  /documento/promocao                  -> bytes do documento",
                            "GET  /telas                               -> catálogo de telas (JSON)",
                            "GET  /documento/tela/{id}                 -> bytes da tela",
                            "GET  /promocao                            -> estado atual (JSON)",
                            "PUT  /promocao                            -> muda a promoção (JSON)",
                            "DELETE /promocao                          -> desliga a promoção",
                        ),
                    ),
                )
            }

            // ── Os dois endpoints que importam ────────────────────────────
            //
            // Devolvem `application/octet-stream`. Não há schema, não há
            // contrato de campos, não há versionamento de DTO. O app não
            // precisa saber o que tem dentro — ele só toca.

            get("/documento/boas-vindas") {
                val nome = call.request.queryParameters["nome"] ?: "visitante"
                call.respondBytes(
                    documentoBoasVindas(nome),
                    ContentType.Application.OctetStream,
                )
            }

            get("/documento/promocao") {
                call.respondBytes(
                    documentoPromocao(promocaoAtual.get()),
                    ContentType.Application.OctetStream,
                )
            }

            // ── Catálogo de telas ricas ──────────────────────────────────
            //
            // Repare no par de endpoints. `/telas` devolve JSON — é metadado,
            // serve para o app montar um menu. `/documento/tela/{id}` devolve
            // bytes — é a tela em si.
            //
            // Essa divisão é o desenho certo para um sistema real: JSON para
            // o que o app precisa ENTENDER, documento para o que ele precisa
            // apenas MOSTRAR.

            get("/telas") {
                call.respond(TELAS.values.map { it.first })
            }

            get("/documento/tela/{id}") {
                val id = call.parameters["id"]
                val tela = TELAS[id]
                if (tela == null) {
                    call.respond(HttpStatusCode.NotFound, mapOf("erro" to "tela '$id' não existe"))
                } else {
                    call.respondBytes(tela.second(), ContentType.Application.OctetStream)
                }
            }

            // ── Controle da promoção, para você brincar via curl ──────────

            get("/promocao") {
                val p = promocaoAtual.get()
                if (p == null) {
                    call.respond(mapOf("ativa" to false))
                } else {
                    call.respond(PromocaoDto(p.chamada, p.descricao, p.preco))
                }
            }

            put("/promocao") {
                val dto = call.receive<PromocaoDto>()
                promocaoAtual.set(Promocao(dto.chamada, dto.descricao, dto.preco))
                call.respond(mapOf("ok" to true, "ativa" to true))
            }

            delete("/promocao") {
                promocaoAtual.set(null)
                call.respond(mapOf("ok" to true, "ativa" to false))
            }
        }
    }.start(wait = true)
}
