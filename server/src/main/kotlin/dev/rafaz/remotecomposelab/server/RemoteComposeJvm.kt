package dev.rafaz.remotecomposelab.server

import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.creation.CreationDisplayInfo
import androidx.compose.remote.creation.JvmRcPlatformServices
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.dsl.RcScope
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.creation.profile.RemoteComposeWriterFactory

/**
 * A ponte entre "Remote Compose existe" e "meu servidor gera Remote Compose".
 *
 * Este arquivo é curto, mas é o coração do backend. Ele monta à mão a
 * infraestrutura que, do lado Android, o `remote-creation-compose` te dá
 * pronta — e que ninguém documentou para JVM.
 */

/**
 * O `apiLevel` mínimo que o formato aceita.
 *
 * COMO CHEGAMOS NESTE NÚMERO: varrendo. Ele não está em constante pública, não
 * está na documentação e não está em nenhum exemplo. Passar qualquer valor de
 * 0 a 5 explode em tempo de execução com:
 *
 *     java.lang.RuntimeException: Unsupported API level 5
 *         at androidx.compose.remote.core.operations.Header.apply(Header.java:459)
 *
 * A partir de 6 funciona. Ver `Sonda.kt`, que reproduz a varredura.
 * Se a build quebrar aqui após subir a versão da biblioteca, rode a sonda de
 * novo — provavelmente o piso subiu.
 */
const val API_LEVEL_MINIMO = 6

/** Densidade de referência. 420 dpi ≈ a de um telefone moderno. */
private const val DENSIDADE_PADRAO = 420

/**
 * Grava um documento e devolve os bytes prontos para trafegar.
 *
 * Compare com o lado Android:
 *
 *     Android:  captureSingleRemoteDocument(context) { RemoteColumn { ... } }
 *     Servidor: documento { RcRoot { Column { ... } } }
 *
 * Mesma tecnologia, mesmo formato de saída, DSLs diferentes. A de cá é
 * imperativa e não precisa do runtime do Compose — por isso roda num servidor.
 *
 * ─────────────────────────────────────────────────────────────────────────
 * A ARMADILHA QUE NOS CUSTOU UMA TELA EM BRANCO
 *
 * A primeira versão não informava tamanho nenhum. O resultado foi o pior tipo
 * de bug: o servidor gerava 402 bytes válidos, o app baixava os 402 bytes sem
 * erro, o player carregava sem exceção — e desenhava NADA. Log limpo, zero
 * pistas.
 *
 * O diagnóstico veio de instrumentar o app e imprimir o que o player via:
 *
 *     doc 0x0 | stats=number of operations : 14 ; Header : 1:29 ;
 *     ComponentModifiers : 4:0 ; RootLayoutComponent : 2:10 ;
 *     ColumnLayout : 1:21 ; TextData : 3:114 ; TextLayout : 3:135
 *
 * Ou seja: as 14 operações estavam TODAS lá — coluna, três textos, tudo. O
 * documento só não tinha tamanho. E área zero desenha zero.
 *
 * A primeira tentativa de correção foi passar `HTag(Header.DOC_WIDTH, ...)`.
 * Os valores até apareceram nos bytes (dá para ver `04 38` = 1080 no dump),
 * mas o `CoreDocument` continuou reportando 0x0 — não é de lá que ele lê.
 *
 * O tamanho vem do [CreationDisplayInfo] entregue ao `RemoteComposeWriter`.
 * Como quem constrói o writer é a **factory** do `Profile`, é lá que ele
 * precisa ser injetado — e é por isso que o perfil é montado por documento,
 * e não uma vez só.
 *
 * Lição para quem for escrever SDUI próprio: **falhe alto**. Um documento sem
 * dimensão deveria recusar-se a nascer, não render uma tela em branco.
 * ─────────────────────────────────────────────────────────────────────────
 *
 * @param largura   largura de referência do documento, em pixels
 * @param altura    altura de referência do documento, em pixels
 * @param densidade densidade de referência, em dpi
 */
fun documento(
    largura: Int = 1080,
    altura: Int = 600,
    densidade: Int = DENSIDADE_PADRAO,
    conteudo: RcScope.(RemoteComposeWriter) -> Unit,
): ByteArray {
    val plataforma = JvmRcPlatformServices()
    val fabrica = RemoteComposeWriterFactory { info, profile, obj ->
        RemoteComposeWriter(info, "", profile, obj)
    }
    val profile = Profile(API_LEVEL_MINIMO, 0, plataforma, fabrica)

    // O construtor que recebe CreationDisplayInfo é o único que preenche
    // largura, altura e densidade no cabeçalho do documento.
    val writer = RemoteComposeWriter(
        CreationDisplayInfo(largura, altura, densidade),
        "",
        profile,
        null,
    )

    // A ponte em Java existe porque `RcScopeImpl` é `internal` no Kotlin.
    // Ela também cria o RcRoot — quem o criava antes era o `createRcBuffer`,
    // que deixamos de usar. Veja PonteRc.java para o porquê completo.
    //
    // Passamos o `writer` para o conteúdo porque criar um VALOR remoto
    // (`RcFloat(writer, 0f)`) exige uma referência a ele. Telas que não usam
    // estado simplesmente ignoram o parâmetro.
    return PonteRc.escrever(writer) { escopo -> escopo.conteudo(writer) }
}

/*
 * Nota sobre o `Profile`, construído lá em cima:
 *
 * ele descreve as capacidades do DESTINO — qual nível do formato ele entende e
 * quais famílias de operação suporta. É o mecanismo de compatibilidade da
 * tecnologia: um servidor poderia servir documentos mais simples para players
 * antigos.
 *
 * Os quatro argumentos:
 *   1. apiLevel            — nível do formato (ver API_LEVEL_MINIMO)
 *   2. operationsProfiles  — máscara de famílias de operação (0 = padrão)
 *   3. platform            — quem sabe desenhar path e ler imagem NESTA JVM
 *   4. factory             — como instanciar o escritor
 *
 * O item 3 só existe porque `remote-creation-jvm` foi publicado. Sem aqueles
 * 19 KB, este arquivo inteiro seria impossível.
 */
