package dev.rafaz.remotecomposelab.server

import androidx.compose.remote.core.Operations
import androidx.compose.remote.creation.dsl.background

/**
 * Ferramenta de dissecação de documentos.
 *
 * Rode com:  .\gradlew.bat :server:runDissect
 *
 * Ela faz duas coisas:
 *   1. imprime a TABELA DE OPCODES do formato, lida por reflexão da classe
 *      `Operations` do `remote-core` — ou seja, é sempre a tabela da versão
 *      que você está usando, nunca uma cópia desatualizada;
 *   2. imprime um dump hexadecimal anotado de um documento real.
 *
 * O objetivo é didático: tirar o formato do lugar de "blob mágico".
 */
fun main() {
    opcodeTable()
    println()
    annotatedDump("mínimo 100x50", document(width = 100, height = 50) { })
    println()
    annotatedDump("boas-vindas", welcomeDocument("Rafael"))
    println()
    differentialAnalysis()
}

/**
 * Análise diferencial: gera pares de documentos que diferem em UM parâmetro e
 * mostra exatamente quais bytes mudaram.
 *
 * Esta é a técnica que transforma "chutar o que significa cada byte" em
 * "provar o que significa cada byte". Se eu mudo só a largura e só os bytes
 * 13–16 mudam, então a largura mora nos bytes 13–16. Não é opinião.
 */
private fun differentialAnalysis() {
    println("=".repeat(72))
    println("ANÁLISE DIFERENCIAL — o que muda quando mudo UM parâmetro")
    println("=".repeat(72))

    val base = document(width = 100, height = 50) { }

    compare("largura 100 -> 300", base, document(width = 300, height = 50) { })
    compare("altura   50 -> 999", base, document(width = 100, height = 999) { })
    compare(
        "densidade 420 -> 160",
        base,
        document(width = 100, height = 50, density = 160) { },
    )

    // Como uma cor é gravada? Preto puro contra vermelho puro.
    compare(
        "cor de fundo #FF000000 -> #FFFF0000",
        docWithColor(0xFF000000.toInt()),
        docWithColor(0xFFFF0000.toInt()),
    )

    // E um texto? Trocar o nome muda o quê?
    compare(
        "texto \"Rafael\" -> \"Ana\"",
        welcomeDocument("Rafael"),
        welcomeDocument("Ana"),
    )
}

/** Um documento mínimo cuja única característica é a cor de fundo. */
private fun docWithColor(color: Int): ByteArray = document(width = 100, height = 50) {
    Column(
        modifier = androidx.compose.remote.creation.dsl.Modifier
            .background(color),
    ) { }
}

private fun compare(label: String, a: ByteArray, b: ByteArray) {
    println("\n--- $label ---")
    if (a.size != b.size) {
        println("  tamanhos diferentes: ${a.size} vs ${b.size} bytes")
    }
    val diferencas = (0 until minOf(a.size, b.size)).filter { a[it] != b[it] }
    if (diferencas.isEmpty()) {
        println("  NENHUM byte mudou (o parâmetro não é gravado no documento)")
        return
    }
    println("  bytes que mudaram: offsets ${diferencas.joinToString(", ") { "0x%02X (%d)".format(it, it) }}")
    diferencas.forEach { i ->
        println("    offset %2d: %02X -> %02X".format(i, a[i], b[i]))
    }
}

/**
 * A tabela de opcodes, lida por reflexão.
 *
 * `Operations` é uma classe Java cheia de `public static final int`. Cada um
 * é o número que identifica uma operação dentro do documento. Ler por reflexão
 * garante que a tabela nunca fique defasada em relação à biblioteca.
 */
private fun opcodeTable() {
    println("=".repeat(72))
    println("TABELA DE OPCODES  (androidx.compose.remote.core.Operations)")
    println("=".repeat(72))

    val campos = Operations::class.java.declaredFields
        .filter { java.lang.reflect.Modifier.isStatic(it.modifiers) }
        .filter { it.type == Int::class.javaPrimitiveType }
        .onEach { it.isAccessible = true }
        .map { it.name to (it.get(null) as Int) }
        .sortedBy { it.second }

    println("total: ${campos.size} operações\n")
    campos.forEach { (name, codigo) ->
        println("  %4d  0x%04X  %s".format(codigo, codigo, name))
    }
}

/**
 * Dump hexadecimal com offsets e coluna ASCII — o formato clássico do `xxd`.
 *
 * A coluna ASCII é o que torna um dump legível para humanos: strings do
 * documento (os textos da sua UI) aparecem ali em texto claro, e servem de
 * âncora para você se localizar no meio dos bytes.
 */
private fun annotatedDump(label: String, bytes: ByteArray) {
    println("=".repeat(72))
    println("DOCUMENTO \"$label\" — ${bytes.size} bytes")
    println("=".repeat(72))

    bytes.toList().chunked(16).forEachIndexed { linha, grupo ->
        val offset = linha * 16
        val hex = grupo.joinToString(" ") { "%02X".format(it) }.padEnd(47)
        val ascii = grupo.joinToString("") { b ->
            val c = b.toInt().toChar()
            if (c.isLetterOrDigit() || c in " .,!?:;-_/@#()[]{}") c.toString() else "."
        }
        println("%08X  %s  |%s|".format(offset, hex, ascii))
    }
}
