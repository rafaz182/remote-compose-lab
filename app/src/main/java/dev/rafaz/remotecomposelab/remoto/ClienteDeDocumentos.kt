package dev.rafaz.remotecomposelab.remoto

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.statement.readRawBytes
import io.ktor.http.isSuccess
import io.ktor.client.statement.HttpResponse
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

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

    /**
     * Busca o CATÁLOGO de telas — e este vem em JSON, não em bytes.
     *
     * A divisão é proposital e é o desenho certo para um sistema real:
     *
     *   JSON      -> para o que o app precisa ENTENDER (montar um menu,
     *                decidir navegação, tomar decisão de produto)
     *   documento -> para o que o app precisa apenas MOSTRAR
     *
     * Server-Driven UI não significa "tudo vira documento". Significa saber
     * onde traçar a linha.
     */
    suspend fun buscarTelas(): List<TelaRemota> {
        val corpo = String(buscar("/telas"), Charsets.UTF_8)
        return json.decodeFromString(corpo)
    }

    private val json = Json { ignoreUnknownKeys = true }
}

/**
 * Metadado de uma tela disponível no servidor.
 *
 * [altura] é a altura de referência que o documento declara. O player precisa
 * dela para escalar o conteúdo — e ela vem do servidor de propósito. Antes o
 * app tinha um `when (id)` com as alturas chumbadas, e isso quebrava sempre
 * que o servidor mudava uma tela.
 */
@Serializable
data class TelaRemota(
    val id: String,
    val titulo: String,
    val ensina: String,
    val altura: Int,
)
