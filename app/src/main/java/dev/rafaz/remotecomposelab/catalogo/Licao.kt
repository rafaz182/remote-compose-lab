package dev.rafaz.remotecomposelab.catalogo

import androidx.compose.runtime.Composable
import dev.rafaz.remotecomposelab.licoes.L01OlaRemoteCompose
import dev.rafaz.remotecomposelab.licoes.L02ODocumentoEOProduto
import dev.rafaz.remotecomposelab.licoes.L03PorQueUmModifierParalelo
import dev.rafaz.remotecomposelab.licoes.L04FrontXBack
import dev.rafaz.remotecomposelab.licoes.L05GaleriaDoServidor

/**
 * Uma aula do laboratório.
 *
 * Guardar a UI como `@Composable () -> Unit` dentro de um dado é um truque
 * simples e muito útil: o catálogo vira uma LISTA, e adicionar uma aula nova
 * passa a ser uma linha só — sem mexer em navegação.
 */
data class Licao(
    val numero: Int,
    val titulo: String,
    val resumo: String,
    val conteudo: @Composable () -> Unit,
)

/**
 * O currículo, em ordem. Cada aula assume as anteriores.
 */
val LICOES: List<Licao> = listOf(
    Licao(
        numero = 1,
        titulo = "Olá, Remote Compose",
        resumo = "O ciclo completo: escrever → bytes → player → pixels.",
        conteudo = { L01OlaRemoteCompose() },
    ),
    Licao(
        numero = 2,
        titulo = "O documento é o produto",
        resumo = "Os bytes não são detalhe: são a entrega. Simulamos o transporte.",
        conteudo = { L02ODocumentoEOProduto() },
    ),
    Licao(
        numero = 3,
        titulo = "Por que um Modifier paralelo?",
        resumo = "A pergunta que incomoda todo mundo — e a resposta que explica a lib.",
        conteudo = { L03PorQueUmModifierParalelo() },
    ),
    Licao(
        numero = 4,
        titulo = "Front × Back de verdade",
        resumo = "Um Ktor em JVM pura gera a tela e entrega por HTTP. Rede real.",
        conteudo = { L04FrontXBack() },
    ),
    Licao(
        numero = 5,
        titulo = "A galeria do servidor",
        resumo = "Cinco telas ricas desenhadas no backend — e eventos voltando ao app.",
        conteudo = { L05GaleriaDoServidor() },
    ),
)
