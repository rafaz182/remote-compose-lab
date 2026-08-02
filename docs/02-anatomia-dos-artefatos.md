# Anatomia dos artefatos: o que o empacotamento revela

Esta página existe porque uma pergunta aparentemente burocrática — "esse
artefato é `.jar` ou `.aar`?" — responde sozinha as perguntas mais importantes
sobre a tecnologia.

## O mapa

Todos publicados **apenas no Google Maven** (`dl.google.com`), nunca no Maven
Central. Versão analisada: `1.0.0-alpha16`.

| Artefato | Pacote | Papel |
|---|---|---|
| `remote-core` | **jar** | O formato. `CoreDocument`, `WireBuffer`, `PaintContext`, motor de layout. |
| `remote-creation-core` | **jar** | Escrita sem Compose: DSL imperativa `RemoteComposeWriter`, escopos `Rc*`. |
| `remote-creation-jvm` | **jar** | Cola de plataforma para JVM (`JvmRcPlatformServices`). |
| `remote-creation-android` | aar | Cola de plataforma para Android. |
| `remote-creation` | aar | Fachada que junta creation-core + creation-android. |
| `remote-creation-compose` | aar | A DSL `@Composable`: `RemoteColumn`, `RemoteText`, `RemoteModifier`. |
| `remote-player-core` | aar | Runtime do player. Contém `AndroidPaintContext`. |
| `remote-player-view` | aar | Player como `View` do Android. |
| `remote-player-compose` | aar | Player como `@Composable`. |
| `remote-tooling-preview` | aar | `@Preview` de conteúdo remoto no Android Studio. |
| `remote-testing` | aar | Utilidades de teste. |

## Leitura 1: a escrita não precisa do Android

Repare que `remote-core`, `remote-creation-core` e `remote-creation-jvm` são
**JAR puro**. Não dependem de Android nenhum.

Isso significa uma coisa concreta e poderosa: **um backend em Kotlin/JVM pode
gerar documentos Remote Compose**. Não é gambiarra nem engenharia reversa — a
presença de `JvmRcPlatformServices` mostra que isso é um caso de uso previsto
pelo time do AndroidX.

Um servidor Ktor poderia montar a home do seu app e devolver bytes prontos.
O app só tocaria o documento. É o Server-Driven UI no sentido mais literal
possível.

> Neste laboratório escolhemos focar só em Android, então não exercitamos isso.
> Mas fica registrado: a porta existe e está destrancada.

## Leitura 2: a leitura precisa

Agora repare que **todos** os `remote-player-*` são `.aar`. Sem exceção.

O player depende do `Canvas` do Android, de `View`, de `Typeface`. Ele é a ponte
entre o formato (portátil) e uma superfície de desenho concreta (não portátil).

Consequências diretas, e não há como contorná-las hoje:

- **Não existe player para Desktop/JVM.**
- **Não existe absolutamente nada para iOS.** Não há um único `.klib`
  Kotlin/Native publicado. Não é questão de faltar documentação: o artefato não
  existe.

Ou seja: hoje, Remote Compose **escreve em qualquer lugar, mas só toca no
Android**.

## Leitura 3: o caminho para o seu próprio SDUI

Aqui está a parte mais interessante, e o motivo de esta página existir.

`remote-core` — o JAR puro — contém uma classe chamada `PaintContext`. Ela é
**abstrata**. E `remote-player-core` contém `AndroidPaintContext`, que a
implementa usando o `Canvas` do Android.

Junte isso ao fato de que o **motor de layout inteiro**
(`androidx.compose.remote.core.operations.layout.*`, com `managers`, `measure` e
`policies`) também está no JAR puro, e o desenho fica claro:

```
                 remote-core  (portátil, JVM puro)
                 ├── formato do documento
                 ├── motor de layout
                 └── PaintContext  ← abstrato
                            │
              ┌─────────────┴──────────────┐
              │                            │
     AndroidPaintContext            (não existe ainda)
     desenha em Canvas               desenharia em
     — existe hoje                   DrawScope, Skia, o que for
```

O player do Android não é mágico. Ele é **uma implementação de uma interface de
desenho**. Escrever um `PaintContext` sobre o `DrawScope` do Compose
Multiplatform daria um player de Desktop — e o layout viria de graça, porque já
está no JAR portátil.

Isso é trabalho de verdade (o formato tem centenas de operações, e você cobriria
um subconjunto). Mas é exatamente o tipo de exercício que ensina Server-Driven
UI de dentro para fora — que é o objetivo final deste estudo.

## Como conferir tudo isso você mesmo

Nada aqui veio de documentação. Veio de olhar os artefatos:

```powershell
# empacotamento e dependências
Invoke-WebRequest "https://dl.google.com/dl/android/maven2/androidx/compose/remote/remote-core/1.0.0-alpha16/remote-core-1.0.0-alpha16.pom"

# quais classes existem lá dentro
javap -public -classpath remote-core.jar androidx.compose.remote.core.PaintContext
```

Guarde o hábito: **o artefato é a verdade, a documentação é a intenção.**
Foi assim que descobrimos que o `minSdk` real é 29, e não 23 como as notas de
versão afirmam — ver [`03-diario-de-bordo.md`](03-diario-de-bordo.md).
