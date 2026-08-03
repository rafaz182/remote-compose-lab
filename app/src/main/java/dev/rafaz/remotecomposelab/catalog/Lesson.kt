package dev.rafaz.remotecomposelab.Catalog

import androidx.compose.runtime.Composable
import dev.rafaz.remotecomposelab.lessons.L01HelloRemoteCompose
import dev.rafaz.remotecomposelab.lessons.L02TheDocumentIsTheProduct
import dev.rafaz.remotecomposelab.lessons.L03WhyAParallelModifier
import dev.rafaz.remotecomposelab.lessons.L04FrontVsBack
import dev.rafaz.remotecomposelab.lessons.L05ServerGallery
import dev.rafaz.remotecomposelab.lessons.L06TwoPlayers

/**
 * Uma aula do laboratório.
 *
 * Guardar a UI como `@Composable () -> Unit` dentro de um dado é um truque
 * simples e muito útil: o catálogo vira uma LISTA, e adicionar uma aula nova
 * passa a ser uma linha só — sem mexer em navegação.
 */
data class Lesson(
    val number: Int,
    val title: String,
    val summary: String,
    val content: @Composable () -> Unit,
)

/**
 * O currículo, em ordem. Cada aula assume as anteriores.
 */
val LESSONS: List<Lesson> = listOf(
    Lesson(
        number = 1,
        title = "Olá, Remote Compose",
        summary = "O ciclo completo: escrever → bytes → player → pixels.",
        content = { L01HelloRemoteCompose() },
    ),
    Lesson(
        number = 2,
        title = "O documento é o produto",
        summary = "Os bytes não são detalhe: são a entrega. Simulamos o transporte.",
        content = { L02TheDocumentIsTheProduct() },
    ),
    Lesson(
        number = 3,
        title = "Por que um Modifier paralelo?",
        summary = "A pergunta que incomoda todo mundo — e a resposta que explica a lib.",
        content = { L03WhyAParallelModifier() },
    ),
    Lesson(
        number = 4,
        title = "Front × Back de verdade",
        summary = "Um Ktor em JVM pura gera a tela e entrega por HTTP. Rede real.",
        content = { L04FrontVsBack() },
    ),
    Lesson(
        number = 5,
        title = "A galeria do servidor",
        summary = "Cinco telas ricas desenhadas no backend — e eventos voltando ao app.",
        content = { L05ServerGallery() },
    ),
    Lesson(
        number = 6,
        title = "O experimento dos dois players",
        summary = "Mesmo documento, dois executores — a técnica que isola um defeito.",
        content = { L06TwoPlayers() },
    ),
)
