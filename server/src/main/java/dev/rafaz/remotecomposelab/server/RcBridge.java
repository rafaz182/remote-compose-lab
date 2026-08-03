package dev.rafaz.remotecomposelab.server;

import androidx.compose.remote.creation.RemoteComposeWriter;
import androidx.compose.remote.creation.dsl.RcFloat;
import androidx.compose.remote.creation.dsl.RcScope;
import androidx.compose.remote.creation.dsl.RcScopeImpl;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

/**
 * Uma ponte de três linhas, escrita em Java de propósito.
 *
 * POR QUE ISTO EXISTE
 *
 * Para gerar um documento com dimensões corretas precisamos construir o
 * {@link RemoteComposeWriter} com um {@code CreationDisplayInfo} — e o atalho
 * público {@code createRcBuffer} não permite isso: ele instancia o writer
 * internamente, com display info zerado. Documento sem dimensão renderiza
 * uma tela em branco.
 *
 * O caminho correto passa por {@code RcScopeImpl}, que é marcado
 * {@code internal} no Kotlin. Só que {@code internal} é uma convenção do
 * COMPILADOR Kotlin, não da JVM: no bytecode a classe é {@code public}.
 * Java não conhece essa convenção e enxerga a classe normalmente.
 *
 * Ou seja: este arquivo não burla nada em tempo de execução. Ele só usa uma
 * API que o Kotlin esconde de nós porque ela ainda não é considerada estável.
 *
 * RISCO ASSUMIDO: por ser API interna de uma biblioteca alpha, isto pode
 * quebrar em qualquer versão nova. Se o :server parar de compilar depois de
 * um upgrade, comece a investigar por aqui.
 */
public final class RcBridge {

    private RcBridge() {
    }

    /**
     * Executa {@code conteudo} dentro da raiz do documento e devolve os bytes.
     */
    public static byte[] write(
            RemoteComposeWriter writer,
            Function1<RcScope, Unit> content
    ) {
        RcScopeImpl scope = new RcScopeImpl(writer);
        scope.RcRoot(content);

        // CUIDADO: `writer.buffer()` devolve o array de apoio INTEIRO —
        // 1 MB alocado de uma vez, quase todo zeros. Usá-lo faria o servidor
        // responder 1.048.576 bytes para um documento de 400.
        // `encodeToByteArray()` devolve só a parte realmente escrita.
        return writer.encodeToByteArray();
    }

    /**
     * Cria um VALOR remoto — um espaço de valor dentro do documento.
     *
     * Mesma história do {@link RcScopeImpl}: o construtor
     * {@code RcFloat(RemoteComposeWriter, float)} é {@code internal} no
     * Kotlin, mas {@code public} no bytecode. Java o enxerga.
     *
     * O resultado NÃO é um número. É a referência a um espaço que o player
     * vai avaliar. Somar, multiplicar ou comparar esse objeto no Kotlin
     * grava uma FÓRMULA no documento, não faz uma conta.
     */
    public static RcFloat floatValue(RemoteComposeWriter writer, float initial) {
        return new RcFloat(writer, initial);
    }
}
