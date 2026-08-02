package dev.rafaz.remotecomposelab.remoto

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.statement.readRawBytes
import io.ktor.http.isSuccess
import io.ktor.client.statement.HttpResponse

/**
 * Busca documentos Remote Compose no módulo `:server`.
 *
 * Repare no quanto este arquivo é modesto. É proposital, e é a tese
 * aparecendo: **não existe camada de parsing aqui.** Nenhum DTO, nenhum
 * `@Serializable`, nenhum `when (tipo)` mapeando strings para componentes.
 *
 * Num Server-Driven UI baseado em JSON, este arquivo teria centenas de
 * linhas — um modelo para cada componente, um parser, um renderizador, e a
 * eterna negociação de contrato com o time de backend. Aqui a resposta HTTP
 * já É a tela.
 */
object ClienteDeDocumentos {

    /**
     * 10.0.2.2 é como o emulador do Android enxerga o `localhost` da sua
     * máquina. De dentro do emulador, "localhost" seria o próprio emulador.
     *
     * Em dispositivo físico, troque pelo IP da sua máquina na rede local
     * (algo como 192.168.0.x) e garanta que o firewall libera a porta 8080.
     */
    const val BASE = "http://10.0.2.2:8080"

    private val http = HttpClient(OkHttp)

    /** Devolve os bytes crus do documento, ou lança em caso de erro. */
    suspend fun buscar(caminho: String): ByteArray {
        val resposta: HttpResponse = http.get("$BASE$caminho")
        if (!resposta.status.isSuccess()) {
            error("servidor respondeu ${resposta.status}")
        }
        return resposta.readRawBytes()
    }
}
