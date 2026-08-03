package dev.rafaz.remotecomposelab.server

import androidx.compose.remote.creation.JvmRcPlatformServices
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.dsl.RcProfile
import androidx.compose.remote.creation.dsl.createRcBuffer
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.creation.profile.RemoteComposeWriterFactory

/**
 * SONDA — o que é isso?
 *
 * "Sonda" aqui tem o mesmo sentido de sonda espacial ou sonda de perfuração:
 * um programinha descartável que a gente **manda para dentro de território
 * desconhecido só para trazer informação de volta**. Ela não faz parte do
 * produto; existe para responder uma pergunta.
 *
 * A pergunta desta sonda era: *"qual `apiLevel` o formato aceita?"*
 *
 * Não havia constante pública, não havia documentação, não havia exemplo. Em
 * vez de ficar lendo o código da biblioteca tentando deduzir, escrevemos um
 * laço que **testa todos os valores plausíveis e reporta quais funcionam**.
 * A resposta apareceu em segundos: 6 é o piso.
 *
 * Por que ela ficou no repositório em vez de ser apagada?
 *
 * Porque a pergunta vai voltar. Quando a biblioteca subir de versão, rodar a
 * sonda de novo responde na hora se o piso mudou. Uma sonda é barata de
 * escrever e continua rendendo depois — é o oposto de uma investigação manual,
 * que você tem que refazer do zero toda vez.
 *
 * Este arquivo é a demonstração de uma técnica de depuração, não código de
 * produção. A técnica está explicada em `docs/04-depuracao-do-backend.md`.
 *
 * Rode com:  .\gradlew.bat :server:runProbe
 */
fun main() {
    val platform = JvmRcPlatformServices()
    val factory = RemoteComposeWriterFactory { info, profile, obj ->
        RemoteComposeWriter(info, "", profile, obj)
    }

    println("varrendo apiLevel x profileMask...")
    for (api in 0..8) {
        for (mask in 0..2) {
            val result = runCatching {
                createRcBuffer(RcProfile(Profile(api, mask, platform, factory), true)) {
                    RcRoot { Column { Text("Olá do servidor") } }
                }
            }
            result.onSuccess { bytes ->
                println("  OK   api=$api mask=$mask -> ${bytes.size} bytes")
            }.onFailure { e ->
                println("  FALHA api=$api mask=$mask -> ${e.message}")
            }
        }
    }
}
