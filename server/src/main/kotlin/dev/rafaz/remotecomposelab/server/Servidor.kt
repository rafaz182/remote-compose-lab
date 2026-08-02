package dev.rafaz.remotecomposelab.server

import io.ktor.http.ContentType
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
