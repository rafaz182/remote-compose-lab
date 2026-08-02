package dev.rafael.remotecomposelab.remoto

import androidx.compose.remote.creation.compose.capture.captureSingleRemoteDocument
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/**
 * O resultado de gravar um documento Remote Compose.
 *
 * Guardamos os **bytes** junto com o [RemoteDocument] de propósito: os bytes são
 * a coisa real, o produto da tecnologia. O [RemoteDocument] é só o objeto que o
 * player usa para executá-los.
 *
 * Se você levar uma única ideia deste projeto, leve esta: **o artefato é um
 * `ByteArray`**. Ele pode ser salvo em disco, enviado por HTTP, guardado no
 * Firebase Remote Config ou empurrado para um relógio. Compose comum não tem
 * equivalente disso — uma `@Composable` não é um valor que você transporta.
 */
class DocumentoRemoto(val bytes: ByteArray) {
    val documento: RemoteDocument = RemoteDocument(bytes)

    val tamanhoBytes: Int get() = bytes.size

    /** Largura/altura que o documento declara para si mesmo. */
    val largura: Int get() = documento.width
    val altura: Int get() = documento.height

    /** Primeiros bytes em hexadecimal — para a aula que disseca o formato. */
    fun hex(quantidade: Int = 64): String =
        bytes.take(quantidade).joinToString(" ") { "%02X".format(it) }
}

/**
 * Grava um documento a partir de conteúdo Remote Compose e devolve o resultado.
 *
 * Repare no detalhe mais importante desta função: o parâmetro [conteudo] é uma
 * `@Composable`, mas ela **não é renderizada na sua tela**. Ela é executada numa
 * composição paralela, "fora da tela", cujo único produto são bytes.
 *
 * É por isso que `captureSingleRemoteDocument` é uma função `suspend` e precisa
 * de um `Context`: por baixo, o AndroidX monta uma composição descartável só
 * para gravar as operações.
 *
 * Devolve `null` enquanto a gravação não terminou — daí o `?` no tipo.
 */
@Composable
fun lembrarDocumento(
    vararg chaves: Any?,
    conteudo: @Composable () -> Unit,
): DocumentoRemoto? {
    val context = LocalContext.current
    var resultado by remember { mutableStateOf<DocumentoRemoto?>(null) }

    // As `chaves` permitem regravar o documento quando algo de fora muda.
    // Sem elas, o documento seria gravado uma única vez e nunca mais.
    LaunchedEffect(*chaves) {
        val capturado = captureSingleRemoteDocument(context, content = conteudo)
        resultado = DocumentoRemoto(capturado.bytes)
    }

    return resultado
}
