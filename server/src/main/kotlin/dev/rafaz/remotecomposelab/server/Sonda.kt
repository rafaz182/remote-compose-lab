package dev.rafaz.remotecomposelab.server

import androidx.compose.remote.creation.JvmRcPlatformServices
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.dsl.RcProfile
import androidx.compose.remote.creation.dsl.createRcBuffer
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.creation.profile.RemoteComposeWriterFactory

/**
 * Sonda descartável: descobre qual `apiLevel` o formato aceita e prova que dá
 * para gerar um documento Remote Compose em JVM pura, sem Android no classpath.
 *
 * Rode com:  .\gradlew.bat :server:runSonda
 */
fun main() {
    val plataforma = JvmRcPlatformServices()
    val fabrica = RemoteComposeWriterFactory { info, profile, obj ->
        RemoteComposeWriter(info, "", profile, obj)
    }

    println("varrendo apiLevel x profileMask...")
    for (api in 0..8) {
        for (mask in 0..2) {
            val resultado = runCatching {
                createRcBuffer(RcProfile(Profile(api, mask, plataforma, fabrica), true)) {
                    RcRoot { Column { Text("Olá do servidor") } }
                }
            }
            resultado.onSuccess { bytes ->
                println("  OK   api=$api mask=$mask -> ${bytes.size} bytes")
            }.onFailure { e ->
                println("  FALHA api=$api mask=$mask -> ${e.message}")
            }
        }
    }
}
