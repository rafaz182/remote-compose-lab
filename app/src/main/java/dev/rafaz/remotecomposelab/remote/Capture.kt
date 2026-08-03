package dev.rafaz.remotecomposelab.remote

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
class CapturedDocument(val bytes: ByteArray) {
    val document: RemoteDocument = RemoteDocument(bytes)

    val sizeInBytes: Int get() = bytes.size

    /** Largura/altura que o documento declara para si mesmo. */
    val width: Int get() = document.width
    val height: Int get() = document.height

    /** Primeiros bytes em hexadecimal — para a aula que disseca o formato. */
    fun hex(quantidade: Int = 64): String =
        bytes.take(quantidade).joinToString(" ") { "%02X".format(it) }
}

/**
 * Grava um documento a partir de conteúdo Remote Compose e devolve o resultado.
 *
 * Repare no detalhe mais importante desta função: o parâmetro [content] é uma
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
fun rememberDocument(
    vararg keys: Any?,
    content: @Composable () -> Unit,
): CapturedDocument? {
    val context = LocalContext.current
    var result by remember { mutableStateOf<CapturedDocument?>(null) }

    // As `keys` permitem regravar o documento quando algo de fora muda.
    // Sem elas, o documento seria gravado uma única vez e nunca mais.
    LaunchedEffect(*keys) {
        val captured = captureSingleRemoteDocument(context, content = content)
        result = CapturedDocument(captured.bytes)
    }

    return result
}
