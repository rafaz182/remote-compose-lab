# As classes do SDK, uma a uma

A documentação gerada do `androidx.compose.remote` é escassa: nomes de método
sem explicação, quase nenhum exemplo, e nada sobre como as peças conversam
entre si. Esta página é o mapa que faltou.

**Escopo:** só as classes que **este projeto realmente usa**, levantadas a
partir dos `import` do código. Nada de catálogo teórico — se está aqui, você
encontra no repositório.

Legenda de risco:

- 🟢 API estável de uso, bem comportada
- 🟡 exige cuidado, tem pegadinha documentada
- 🔴 API interna ou não documentada; pode sumir numa versão nova

---

## O caminho completo, das duas pontas

```
╔═══════════════════ SERVIDOR (JVM pura, sem Android) ═══════════════════╗
║                                                                        ║
║  JvmRcPlatformServices ──┐                                             ║
║                          ├──> Profile ──┐                              ║
║  RemoteComposeWriterFactory ─┘          │                              ║
║                                          ├──> RemoteComposeWriter      ║
║  CreationDisplayInfo ────────────────────┘            │                ║
║                                                       │                ║
║                            RcScopeImpl(writer) ───────┤                ║
║                                  │                    │                ║
║                            RcScope (a DSL)            │                ║
║                            Column / Row / Box / Text  │                ║
║                            Modifier + RcFloat + RcSp  │                ║
║                                  │                    │                ║
║                                  └───── grava ────────┘                ║
║                                                       │                ║
║                                    writer.encodeToByteArray()          ║
╚═══════════════════════════════════════════════════════╪════════════════╝
                                                        │
                                                   ByteArray
                                              (application/octet-stream)
                                                        │
╔═══════════════════════ CLIENTE (Android) ═════════════╪════════════════╗
║                                                        ▼               ║
║                                        RemoteDocument(bytes)           ║
║                                                  │                     ║
║                            ┌─────────────────────┴──────────────┐      ║
║                            ▼                                    ▼      ║
║              RemoteComposePlayer                    RemoteComposePlayer║
║              (player.compose.impl)                  (player.view)      ║
║              um @Composable                         uma View clássica  ║
║                            │                                    │      ║
║                            └──────────── pixels ────────────────┘      ║
║                                                                        ║
║  O app TAMBÉM pode criar documentos, sem servidor:                     ║
║      captureSingleRemoteDocument { RemoteColumn { RemoteText(…) } }     ║
║      RemoteModifier / RemoteColor / rdp / rsp                          ║
╚════════════════════════════════════════════════════════════════════════╝
```

Repare na simetria: **os dois lados escrevem, só o Android lê.** As DSLs de
escrita são diferentes (imperativa no servidor, `@Composable` no Android), mas
produzem o mesmo formato.

---

# Parte 1 — Lado servidor

Tudo nesta parte vem dos JARs puros: `remote-core`, `remote-creation-core`,
`remote-creation-jvm`. Nenhum Android envolvido.

## A infraestrutura (montada em `RemoteComposeJvm.kt`)

### `JvmRcPlatformServices` 🟢
`androidx.compose.remote.creation` · artefato `remote-creation-jvm`

Implementa `RcPlatformServices`, a interface que o motor usa para pedir coisas
que dependem da plataforma: converter um path em array de floats, ler
dimensões de imagem, registrar log.

**Conversa com:** é entregue ao `Profile`, que a repassa a quem precisar.

**Por que importa:** são 19 KB e 6 classes, mas é o que torna a geração de
documentos possível fora do Android. Sem ela, `remote-creation-core` não sabe
desenhar nem medir nada.

### `Profile` 🟡
`androidx.compose.remote.creation.profile` · `remote-creation-core`

Descreve as capacidades do **destino**: qual nível do formato ele entende e
quais famílias de operação aceita. É o mecanismo de compatibilidade da
tecnologia — em tese, um servidor pode gerar documentos mais simples para
players antigos.

```kotlin
Profile(apiLevel, operationsProfiles, platform, factory)
```

**Pegadinha:** `apiLevel` precisa ser **≥ 6**. Não existe constante pública com
esse valor. Qualquer coisa abaixo estoura em runtime com
`RuntimeException: Unsupported API level N`. Descobrimos varrendo — ver
`Probe.kt`.

### `RemoteComposeWriterFactory` 🟢
`androidx.compose.remote.creation.profile` · `remote-creation-core`

Interface de uma função só: como instanciar um `RemoteComposeWriter`. O
`Profile` guarda uma.

```kotlin
RemoteComposeWriterFactory { info, profile, obj ->
    RemoteComposeWriter(info, "", profile, obj)
}
```

**Detalhe honesto:** no nosso fluxo final essa fábrica quase não é usada —
criamos o writer diretamente. Ela continua ali porque o `Profile` a exige no
construtor.

### `CreationDisplayInfo` 🟡
`androidx.compose.remote.creation` · `remote-creation-core`

Largura, altura e densidade de referência do documento.

```kotlin
CreationDisplayInfo(largura, altura, densidadeDpi)
```

**Pegadinha que custou horas:** é **daqui** que sai o tamanho gravado no
cabeçalho. Não de `HTag(Header.DOC_WIDTH, …)`, como parece natural. Um
documento sem isso nasce 0×0 e renderiza uma tela em branco sem reclamar.
Ver [`diario/02-backend-em-jvm-pura.md`](../diario/02-backend-em-jvm-pura.md).

**Curiosidade medida:** a densidade **não** é gravada no documento — só
largura e altura. Provado por análise diferencial em
[`trilha/02-o-formato-por-dentro.md`](../trilha/02-o-formato-por-dentro.md).

### `RemoteComposeWriter` 🟢
`androidx.compose.remote.creation` · `remote-creation-core`

O escritor. Acumula operações num buffer conforme a DSL é executada, e entrega
os bytes no fim.

**Conversa com:** recebe as chamadas do `RcScope`; entrega bytes ao Ktor.

**Pegadinha:** use `encodeToByteArray()`, **nunca** `buffer()`. O segundo
devolve o array de apoio inteiro — 1 MiB pré-alocado, quase todo zeros. O
endpoint chegou a responder 1.048.576 bytes para um documento de 400.

---

## A DSL de escrita (usada em `Documents.kt`, `Screens.kt`, `StatefulScreens.kt`)

### `RcScope` 🟢 e `RcColumnScope` 🟢
`androidx.compose.remote.creation.dsl` · `remote-creation-core`

O escopo da DSL. Dentro dele você chama `Column`, `Row`, `Box`, `Text`,
`Image`, `Flow`, `CollapsibleColumn`, `Canvas` — e também os criadores de valor
(`hour()`, `minutes()`, `createTextFromFloat()`, `named()`).

`RcColumnScope` e `RcRowScope` são variações que acrescentam o que só faz
sentido dentro daquele container (por exemplo `weight`).

**Como isso funciona em Kotlin:** o parâmetro é declarado como
`RcScope.() -> Unit` — uma *função de extensão como parâmetro*. Dentro do
bloco, `this` é um `RcScope` invisível, e por isso `Column { }` parece uma
função solta. É o mesmo truque do `routing { }` do Ktor.

**Pegadinha:** os parâmetros de posicionamento (`Row(modifier, horizontal,
vertical)`) têm nomes que a biblioteca não expõe de forma utilizável. Passe
**posicionalmente**.

### `RcScopeImpl` 🔴
`androidx.compose.remote.creation.dsl` · `remote-creation-core`

A implementação concreta do escopo. É marcada `internal` no Kotlin, mas
`public` no bytecode — `internal` é convenção do compilador Kotlin, não da JVM.

**Como usamos:** através de `RcBridge.java`, porque Java não conhece a
convenção do Kotlin e enxerga a classe normalmente.

**Risco assumido:** API interna de biblioteca alpha. Se o `:server` parar de
compilar depois de um upgrade, comece a investigar por aqui.

### `Modifier` 🟢
`androidx.compose.remote.creation.dsl` · `remote-creation-core`

O `Modifier` do lado servidor. **Não confundir** com `androidx.compose.ui.Modifier`
(o do Compose comum) nem com `RemoteModifier` (o do lado Android). São três
tipos distintos que fazem a mesma coisa em contextos diferentes.

Extensões que usamos: `background`, `padding`, `clip`, `fillMaxWidth`,
`height`, `width`, `size`, `horizontalWeight`, `onClick`.

**Detalhe de tipo:** aqui as medidas são `Float` cru (`padding(20f)`), não
`Dp`. Cores são `Int` ARGB (`background(0xFF1B3A4B.toInt())`).

### `onClick` 🟡
`androidx.compose.remote.creation.dsl` (extensão de `Modifier`)

Registra uma ação de clique. Dentro do bloco você recebe um `RcActionScope`,
que oferece **duas coisas bem diferentes**:

```kotlin
.onClick { hostAction("comprar:sku-1042") }   // avisa o APP        ✅ funciona
.onClick { setValue(valor, 99f) }             // muda valor INTERNO ❌ ver nota
```

**Estado da arte no projeto:** `hostAction` funciona e está na Aula 05.
`setValue` **não surtiu efeito** em nenhum dos dois players nem via
`StateUpdater` — investigação aberta em [`roteiro.md`](../roteiro.md).

Existem também `onLongClick` e `onDoubleClick`, que não exercitamos.

### `RcFloat` 🟡
`androidx.compose.remote.creation.dsl` · `remote-creation-core`

Um **valor** dentro do documento — e é onde mais gente se confunde.

`RcFloat` **não é um número**. É a referência a um espaço de valor que o player
vai avaliar. Quando você escreve `1f + contador`, não está somando: está
gravando a fórmula *"um mais o valor daquele espaço"*.

A analogia que funciona: é uma **célula de planilha**. `=A1+1` não guarda um
número, guarda uma relação.

**Construção:** o construtor `RcFloat(writer, valor)` é `internal` no Kotlin →
usamos `RcBridge.floatValue(writer, 1f)`.

**Aritmética:** existem **93 funções** sobre `RcFloat` (`sin`, `cos`, `sqrt`,
`pow`, `abs`, `min`, `max`, `exp`, `ceil`…). Os operadores vêm como extensões
de nível superior e **precisam de import explícito**:

```kotlin
import androidx.compose.remote.creation.dsl.plus   // sem isto, erro confuso
```

**Direção importa:** a biblioteca oferece `Float.plus(RcFloat)`, não o
contrário. Escreva `1f + contador`. Para subtrair, some um negativo:
`(-1f) + contador`.

**Fontes de tempo** (todas devolvem `RcFloat`): `hour()`, `minutes()`,
`seconds()`, `continuousSeconds()`, `dayOfWeek()`, `dayOfMonth()`,
`animationTime()`, `touchTime()`. **Verificado:** o player reavalia essas a
cada quadro, sem participação do app.

### `RcSp` 🟢
`androidx.compose.remote.creation.dsl`

Value class sobre `Float` para tamanho de fonte: `fontSize = RcSp(22f)`.
O equivalente servidor do `.sp`.

### `RcRowHorizontalPositioning` / `RcVerticalPositioning` 🟢
`androidx.compose.remote.creation.dsl`

Arranjo e alinhamento. Constantes: `Start`, `Center`, `End`, `SpaceBetween`,
`SpaceEvenly`, `SpaceAround` (as três últimas só nas variantes de Row/Column).

`SpaceBetween` é o mais útil: alinha rótulo à esquerda e valor à direita **sem
calcular largura nenhuma**. É o que faz o recibo funcionar em qualquer tela.

### `RoundedRectShape` / `CircleShape` 🟢
`androidx.compose.remote.creation.modifiers`

Formas para `clip()`. `RoundedRectShape(tl, tr, br, bl)` recebe quatro raios;
`CircleShape()` não recebe nada.

Não existe "componente avatar" — um avatar é um `Box` colorido com
`clip(CircleShape())`. **O formato só conhece formas.**

---

## O que usamos só para investigar

### `Operations` 🟢
`androidx.compose.remote.core` · `remote-core`

A tabela de opcodes: 172 constantes `public static final int` mapeando número →
nome de operação (`LAYOUT_COLUMN = 204`, `DATA_TEXT = 102`…).

**Como usamos:** `Dissect.kt` lê essa tabela **por reflexão**, então ela nunca
fica desatualizada em relação à versão da biblioteca.

### `Header` 🟡
`androidx.compose.remote.core.operations` · `remote-core`

Constantes de tags do cabeçalho: `DOC_WIDTH`, `DOC_HEIGHT`,
`DOC_DENSITY_AT_GENERATION`, `DOC_DESIRED_FPS`…

**Aviso:** tentamos usar essas tags para definir o tamanho do documento e
**não funcionou** — os valores aparecem nos bytes, mas o `CoreDocument` não os
lê de lá. O tamanho vem do `CreationDisplayInfo`.

### `createRcBuffer` / `RcProfile` 🟡
`androidx.compose.remote.creation.dsl`

O atalho oficial para gerar um documento em uma chamada. **Nós abandonamos**,
por dois motivos: ele não permite informar o `CreationDisplayInfo` (documento
nasce 0×0), e ele **já cria o componente raiz** — um `RcRoot` explícito dentro
dele gera duas raízes e quebra o layout.

Sobrevive apenas em `Probe.kt`, onde o tamanho não importa.

---

# Parte 2 — Lado cliente (Android)

## Criar documentos no próprio app

### `captureSingleRemoteDocument` 🟢
`androidx.compose.remote.creation.compose.capture` · `remote-creation-compose`

Função `suspend` que executa um bloco `@Composable` numa composição
**descartável, fora da tela**, cujo único produto são bytes.

```kotlin
val capturado = captureSingleRemoteDocument(context) {
    RemoteColumn { RemoteText("Olá") }
}
capturado.bytes   // ByteArray
```

**Detalhe conceitual importante:** o bloco `@Composable` que você passa **não é
renderizado na sua tela**. É por isso que a função precisa de `Context` e é
`suspend` — por baixo, o AndroidX monta uma composição só para gravar.

**Conversa com:** devolve `CapturedDocument`, que tem `bytes`, `pendingIntents`
e `lambdas`.

Existe também `captureRemoteDocument`, que devolve `Flow<ByteArray>` e reemite
quando o estado muda. Não exercitamos.

### `RemoteColumn`, `RemoteRow`, `RemoteBox`, `RemoteText`, `RemoteSpacer` 🟢
`androidx.compose.remote.creation.compose.layout` · `remote-creation-compose`

Os gêmeos `@Composable` dos componentes. A troca em relação ao Compose comum é
sistemática: `Column` → `RemoteColumn`, `Text` → `RemoteText`.

### `RemoteModifier` 🟢
`androidx.compose.remote.creation.compose.modifier` · `remote-creation-compose`

O `Modifier` do lado Android da escrita. Extensões que usamos: `background`,
`padding`, `fillMaxWidth`, `size`.

**Por que existe um Modifier paralelo:** um `Modifier` comum **executa** — é um
objeto vivo que roda código no seu processo. Um `RemoteModifier` é **gravado**,
vira operação no documento, e é interpretado pelo player do outro lado. Não dá
para serializar um pedaço de código. Aula 03 é inteira sobre isso.

### `RemoteColor`, `rdp`, `rsp` 🟢
`androidx.compose.remote.creation.compose.state` · `remote-creation-compose`

- `RemoteColor(Color.White)` — cor gravável
- `20.rdp` — `RemoteDp`, o gêmeo de `20.dp`
- `22.rsp` — `RemoteTextUnit`, o gêmeo de `22.sp`

**Por que não dá para usar `dp` direto:** um `Dp` é valor fixo. Um `RemoteDp`
pode ser uma **expressão** que o player calcula com a tela **dele**. O documento
carrega a fórmula, não o resultado.

## Ler e renderizar documentos

### `RemoteDocument` 🟢
`androidx.compose.remote.player.core` · `remote-player-core`

Transforma `ByteArray` em algo executável. É a porta de entrada da leitura.

```kotlin
val doc = RemoteDocument(bytes)
```

**Membros úteis para depurar:**

| Membro | O que dá |
|---|---|
| `width` / `height` | dimensões — ⚠️ refletem a **última medição**, não os bytes |
| `stats` | **os nomes das operações do documento** — a melhor ferramenta de diagnóstico que achamos |
| `getNamedColors()` / `getNamedVariables(int)` | o que o documento expõe por nome |

`stats` merece destaque: não está documentado em lugar nenhum e foi o que
resolveu o caso da raiz duplicada, comparando `RootLayoutComponent : 2` contra
`: 1`.

### `RemoteComposePlayer` (Compose) 🟢
`androidx.compose.remote.player.compose.impl` · `remote-player-compose`

O player como `@Composable`. É o que usamos em quase todas as aulas.

```kotlin
RemoteComposePlayer(
    documento,                 // RemoteDocument
    Modifier.fillMaxWidth(),   // Modifier do Compose COMUM
    1080,                      // largura de referência
    520,                       // altura de referência
    CalendarSystemClock(),     // relógio (obrigatório, não-nulo)
    { nome, valor, _ -> },     // ações nomeadas vindas do documento
)
```

**Pegadinhas:**
- os nomes dos parâmetros não são utilizáveis; passe **posicional**;
- o `RemoteClock` é **obrigatório** mesmo sem animação;
- ele **embrulha** a View abaixo — não é reimplementação em Compose puro.

O sexto parâmetro é o **caminho de volta**: toda ação nomeada que o documento
disparar cai ali. É o único ponto de contato entre conteúdo remoto e seu código.

### `RemoteComposePlayer` (View) 🟢
`androidx.compose.remote.player.view` · `remote-player-view`

O mesmo player, como `FrameLayout` clássico. Usamos na Aula 06, dentro de um
`AndroidView`, para rodar o mesmo documento nos dois e comparar.

```kotlin
PlayerDeView(contexto).apply { setDocument(bytes) }
```

**Nome idêntico ao de Compose** — na Aula 06 importamos com
`as PlayerDeView` para não confundir.

**Atenção de build:** vem como dependência transitiva, mas `implementation` não
expõe transitivas em **tempo de compilação**. Para usar direto, declare
`remote-player-view` no `build.gradle.kts`.

### `CalendarSystemClock` 🟢
`androidx.compose.remote.core` · `remote-core`

Implementação concreta de `RemoteClock` — a fonte de tempo que o documento
consulta. Existe porque o formato prevê valores dependentes do tempo.

### `StateUpdater` 🟡
`androidx.compose.remote.player.core.state` · `remote-player-core`

Obtido via `playerDeView.stateUpdater`. Permite ao **app** alterar valores
nomeados de um documento **já carregado**, sem baixar outro.

```kotlin
player.stateUpdater.setUserLocalFloat("contador", 99f)
```

**Estado no projeto:** chamamos, não deu exceção, e **o valor não mudou**. A
pista: existe `StateUpdater.getUserDomainString(nome)`, sugerindo que nomes são
qualificados por **domínio**, e que o `named()` do servidor talvez não caia no
domínio `user`. Investigação aberta em [`roteiro.md`](../roteiro.md).

---

## Tabela de decisão: qual tipo usar?

Confusão frequente, porque há três `Modifier` e vários gêmeos:

| Você está… | Modifier | Medida | Cor |
|---|---|---|---|
| escrevendo UI **normal** do app | `androidx.compose.ui.Modifier` | `20.dp` | `Color(0xFF…)` |
| escrevendo documento **no Android** | `RemoteModifier` | `20.rdp` | `RemoteColor(Color(…))` |
| escrevendo documento **no servidor** | `creation.dsl.Modifier` | `20f` | `0xFF….toInt()` |

Regra que resolve quase tudo: **se o valor precisa sobreviver à serialização,
ele tem uma versão Remote.** Se só existe na sua tela, use o tipo normal.
Quando você misturar sem querer, o compilador reclama — e agora você sabe o que
ele está te dizendo.

---

## Como investigar uma classe você mesmo

Quando esta página não cobrir o que você precisa (vai acontecer — a biblioteca
é grande e alpha), o método que usamos o projeto todo:

```powershell
# 1. baixar o artefato
$base = "https://dl.google.com/dl/android/maven2/androidx/compose/remote"
Invoke-WebRequest "$base/remote-creation-core/1.0.0-alpha16/remote-creation-core-1.0.0-alpha16.jar" -OutFile creation.jar

# 2. ver as assinaturas reais (javap lê o bytecode, ignora o que o Kotlin esconde)
javap -public -classpath creation.jar androidx.compose.remote.creation.dsl.RcScope
```

Detalhes em [`referencia/fontes.md`](../referencia/fontes.md), seção 3.

E o truque mais rápido de todos: escreva a chamada errada de propósito e deixe
o **compilador Kotlin** listar os candidatos. Foi assim que descobrimos a
assinatura de `Text` e que `named` era extensão, não método.
