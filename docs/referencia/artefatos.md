# Anatomia dos artefatos: o que o empacotamento revela

Esta página existe porque uma pergunta aparentemente burocrática — "esse
artefato é `.jar` ou `.aar`?" — responde sozinha as perguntas mais importantes
sobre a tecnologia.

## O mapa

Todos publicados **apenas no Google Maven** (`dl.google.com`), nunca no Maven
Central. Versão analisada: `1.0.0-alpha16` (11 artefatos).

### Visão macro em 30 segundos

```
                        ┌───────────────────────────────┐
                        │       remote-core  (jar)      │
   TODO MUNDO           │  o formato + motor de layout  │
   DEPENDE DISTO  ─────▶│  + PaintContext (abstrato)    │
                        └───────────────┬───────────────┘
                                        │
              ┌─────────────────────────┴────────────────────────┐
              │                                                  │
     ESCRITA (creation)                                 LEITURA (player)
     "como produzir bytes"                            "como executar bytes"
              │                                                  │
   ┌──────────┴──────────┐                          ┌────────────┴───────────┐
   │                     │                          │                        │
creation-core (jar)  creation-compose (aar)    player-core (aar)     player-view (aar)
DSL imperativa       DSL @Composable           AndroidPaintContext   player como View
+ parser JSON        RemoteColumn/RemoteText   RemoteDocument                │
   │                                                  │                      │
creation-jvm (jar)   ◀── porta do BACKEND        player-compose (aar) ◀──────┘
creation-android(aar)                            player como @Composable
```

Três blocos, e a linha divisória entre eles é a coisa mais importante a
memorizar: **o formato é portátil; a escrita quase toda também; só a leitura é
presa ao Android.**

### Detalhamento

#### `remote-core` — **jar**, ~960 KB, 292 classes públicas

O coração. É a especificação executável do formato, e não depende de Android.

| Pacote | O que vive ali |
|---|---|
| `core` | `CoreDocument`, `WireBuffer`, `RemoteComposeBuffer`, `RecordingRemoteComposeBuffer`, `PaintContext`, `RemoteContext`, `RcPlatformServices`, `Operation`/`Operations` |
| `core.operations` | ~150 operações concretas: `DrawText`, `DrawRoundRect`, `DrawPath`, `ClipRect`, `ColorExpression`, `ClickArea`, `BitmapData`… |
| `core.operations.layout` | O **motor de layout**, com `managers` (Column, Row, Box, Flow), `measure`, `policies`, `modifiers`, `animation` |
| `core.operations.utilities` | Avaliador de expressões, `easing`, `touch` |
| `core.semantics` | Acessibilidade |
| `core.serialize` | Serialização das operações |

Ler este artefato é ler o design da tecnologia. `PaintContext` sendo **abstrato**
aqui é o detalhe arquitetural que abre a porta para players próprios.

#### `remote-creation-core` — **jar**, ~545 KB, 223 classes

Escrita **sem Compose runtime**. Existem duas formas de autoria aqui, e isso
surpreende quem só conhece a DSL `@Composable`:

- **DSL imperativa** (`creation.dsl`): escopos `RcColumnScope`, `RcRowScope`,
  `RcBoxScope`, `RcCanvasScope`, `RcPaintScope`, mais tipos `RcFloat`, `RcColor`,
  `RcText`, `RcTextStyle`. Mais próximo de "desenhar" do que de "declarar".
- **Front-end JSON** (`creation.json`): `RemoteComposeJsonParser`,
  `JsonComponentParser`, `DefaultComponentParsers`, `ExpressionParser`. Sim —
  **existe um caminho JSON → documento** dentro do artefato oficial. Isso é
  pouquíssimo divulgado e é altamente relevante para quem quer um CMS.

Também mora aqui `RemoteComposeWriter`, `RemoteComposeContext`, o sistema de
`Profile` (perfis de capacidade do destino) e as `actions`.

#### `remote-creation-jvm` — **jar**, ~19 KB, **6 classes**

O artefato mais enganoso da lista pelo tamanho. Contém só
`JvmRcPlatformServices` e uma implementação de `RemotePath` para JVM.

É **puro plugue de plataforma**: dá ao `creation-core` o que ele precisa para
rodar fora do Android. São 19 KB que decidem se um backend consegue ou não gerar
documentos. É a porta do Front × Back.

#### `remote-creation-android` — aar

O equivalente do anterior, para Android (usa `androidx.graphics:graphics-path`).

#### `remote-creation` — aar

Fachada. Não tem conteúdo próprio relevante: só junta `creation-core` +
`creation-android` num único `implementation`.

#### `remote-creation-compose` — aar, 306 classes

A DSL que este laboratório usa. É a maior superfície de API do conjunto.

| Pacote | O que vive ali |
|---|---|
| `.layout` | `RemoteColumn`, `RemoteRow`, `RemoteBox`, `RemoteText`, `RemoteImage`, `RemoteSpacer`, `RemoteFlowRow`, `RemoteCollapsibleColumn/Row`, `RemoteCanvas` |
| `.modifier` | ~30 modificadores: `padding`, `background`, `size`, `clickable`, `border`, `clip`, `scroll`, `marquee`, `graphicsLayer`, `zIndex`… |
| `.state` | `RemoteFloat`, `RemoteInt`, `RemoteLong`, `RemoteString`, `RemoteColor`, `RemoteDp`, `RemoteBoolean`, `RemoteEnum`, `RemoteImageBitmap` |
| `.capture` | `captureSingleRemoteDocument`, `captureRemoteDocument` (devolve `Flow<ByteArray>`), `RemoteComposeCreationState` |
| `.action` | `hostAction`, `valueChange`, `combinedAction`, `PendingIntentAction`, `LambdaAction`, `ScrollAction` |
| `.shaders` / `.shapes` / `.vector` / `.painter` | Gradientes, formas, vetores, painters remotos |
| `.widgets` | Suporte a widgets / Glance |

Depende de `appcompat`, `activity`, `savedstate` e `lifecycle-viewmodel` — é por
isso que o tema do app precisa ser AppCompat, e é a razão do `minSdk 29`.

#### `remote-player-core` — aar

O runtime da leitura.

| Pacote | O que vive ali |
|---|---|
| `player.core` | `RemoteDocument` — a classe que transforma `ByteArray` em algo executável |
| `.platform` | **`AndroidPaintContext`** (a implementação concreta do `PaintContext`), `AndroidRemoteContext`, `BitmapLoader`, `TypefaceResolver`, `AndroidComputedTextLayout`, `AndroidEdgeEffect` |
| `.state` | `StateUpdater`, `PlayerState`, `RcFloat`/`RcInt`/`RcString`/`RcColor`, `RemoteDomains` — o canal para **mudar valores dentro de um documento já carregado** |
| `.action` | `NamedActionHandler` — como o documento fala com o app hospedeiro |

O pacote `.state` é o que permite atualizar um documento em execução sem
regravá-lo. Guardar essa informação evita a conclusão errada de que "todo
estado exige novo documento".

#### `remote-player-view` — aar

O player como `View` clássica (`RemoteComposePlayer` estende View). É a base do
player de Compose — não é um caminho alternativo, é a camada de baixo.

#### `remote-player-compose` — aar, 19 classes

O que usamos. `RemoteDocumentPlayer` (recebe `CoreDocument`) e
`RemoteComposePlayer` (recebe `RemoteDocument`). Tem também
`ComposeCustomSupport`, para registrar componentes customizados que o documento
pode invocar.

Importante: ele **embrulha** `remote-player-view`. Não é uma reimplementação em
Compose puro — por baixo ainda há uma View e um `Canvas` do Android.

#### `remote-tooling-preview` — aar

`RemoteContentPreview`, `RemoteDocumentPreview`, `RemotePreviewWrapper`,
`RemoteComponentPreview`. Permite `@Preview` de conteúdo remoto no Android
Studio, fechando o ciclo escrita→leitura dentro do IDE.

#### `remote-testing` — aar

Só existe a partir da alpha010. Traz utilitários sobre `ui-test-junit4` para
asserções em documentos renderizados.

### Grafo de dependências (quem puxa quem)

```
remote-core
  ├── remote-creation-core
  │     ├── remote-creation-jvm          (JVM puro)
  │     ├── remote-creation-android
  │     │     └── remote-creation
  │     │           └── remote-creation-compose
  │     └── (usado direto por tooling-preview)
  └── remote-player-core
        └── remote-player-view
              └── remote-player-compose
                    └── remote-tooling-preview
```

Consequência prática para um projeto real: você quase nunca declara os 11.
Um app que só **renderiza** precisa de `remote-player-compose` (que arrasta
core, player-core e player-view). Um app que **cria e renderiza** — como este
laboratório — precisa dele mais `remote-creation-compose`.

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
