# A arquitetura do `:server`, explicada do zero

O módulo `:server` faz duas coisas que não têm nada a ver uma com a outra:

1. **serve HTTP** — isso é Ktor, e é backend comum, igual a qualquer API REST;
2. **gera documentos Remote Compose** — isso é o motor, e não tem nada de web.

Elas se encontram em exatamente **uma linha de código**. Esta página separa as
duas com clareza, assumindo que você não conhece Ktor.

---

## O mapa do módulo

```
server/
├── build.gradle.kts
└── src/main/
    ├── kotlin/dev/rafaz/remotecomposelab/server/
    │   ├── Server.kt          ← [HTTP]        Ktor: rotas, portas, JSON
    │   ├── Documents.kt        ← [MOTOR]       a UI que o servidor desenha
    │   ├── RemoteComposeJvm.kt  ← [MOTOR]       encanamento do Remote Compose
    │   ├── Probe.kt             ← [FERRAMENTA]  investigação, não é produto
    │   └── Dissect.kt          ← [FERRAMENTA]  investigação, não é produto
    └── java/dev/rafaz/remotecomposelab/server/
        └── RcBridge.java         ← [MOTOR]       contorna um `internal` do Kotlin
```

Três categorias, e vale gravar a diferença:

| Categoria | O que é | Se você trocasse por outra coisa |
|---|---|---|
| **HTTP** | Ktor. Recebe requisição, devolve resposta. | Trocar por Spring Boot não mudaria uma linha do motor. |
| **MOTOR** | Geração de documentos Remote Compose. | Funciona sem servidor nenhum — dá para gravar num arquivo. |
| **FERRAMENTA** | Programas descartáveis de investigação. | Apagar não afeta nada em execução. |

A fronteira entre HTTP e MOTOR é **uma chamada de função**. Guarde isso: é o
sinal de que o acoplamento está saudável.

---

# Parte 1 — Ktor do zero

Ktor é o framework web da JetBrains, em Kotlin. Se você vem de Spring, a maior
diferença é filosófica: **Ktor não usa anotações nem injeção de dependência**.
Você monta o servidor chamando funções.

## O menor servidor possível

```kotlin
fun main() {
    embeddedServer(Netty, port = 8080) {
        routing {
            get("/") {
                call.respondText("olá")
            }
        }
    }.start(wait = true)
}
```

Cinco conceitos aí dentro:

### `embeddedServer`

"Embedded" = embutido. O servidor **roda dentro do seu programa**, iniciado por
um `main()` comum. Não existe um Tomcat externo em que você faz *deploy* de um
`.war`. Você gera um `.jar` e executa.

### `Netty`

O **engine**: quem realmente fala TCP e HTTP com o mundo. Ktor é uma camada
sobre um engine, e você escolhe qual. Netty é assíncrono e o mais comum para
servidor. Existem outros (CIO, Jetty, Tomcat). Trocar o engine é trocar essa
palavra e a dependência no Gradle.

### O bloco `{ ... }` depois da porta

É o **módulo da aplicação**: a configuração do servidor. Tudo que estiver ali
descreve como ele se comporta.

### `routing { }`

Onde você declara as rotas. Dentro dele, `get`, `post`, `put`, `delete` recebem
um caminho e um bloco.

### `.start(wait = true)`

Sobe o servidor. `wait = true` significa "bloqueie esta thread para sempre" —
senão o `main()` terminaria e o processo morreria junto.

## O que o nosso `Server.kt` acrescenta

```kotlin
embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
    install(ContentNegotiation) { json() }
    install(CallLogging)
    routing { /* ... */ }
}.start(wait = true)
```

### `host = "0.0.0.0"` — e por que não `localhost`

Este detalhe já causou muita confusão em quem testa com emulador.

- `localhost` (ou `127.0.0.1`) faz o servidor aceitar conexões **só da própria
  máquina**.
- `0.0.0.0` significa "escute em **todas** as interfaces de rede".

O emulador do Android é, para efeitos práticos, **outra máquina**. Ele alcança o
seu computador pelo IP mágico `10.0.2.2`, mas isso só funciona se o servidor
estiver ouvindo em todas as interfaces. Com `localhost`, o app recebe "connection
refused" e você perde meia hora achando que o problema é no cliente.

```
   ┌──────────────────────────┐
   │  seu PC                  │
   │                          │
   │   Ktor :8080             │
   │   ouvindo em 0.0.0.0     │
   │        ▲                 │
   │        │ 10.0.2.2:8080   │
   │   ┌────┴──────────────┐  │
   │   │ emulador Android  │  │
   │   │ (VM separada)     │  │
   │   └───────────────────┘  │
   └──────────────────────────┘
```

### `install(...)` — os plugins

Plugin em Ktor é um comportamento que se **intercala** no processamento de toda
requisição. Você liga o que precisa; nada vem por padrão. Usamos dois:

**`ContentNegotiation` com `json()`** — ensina o Ktor a converter objetos Kotlin
em JSON e vice-versa, usando `kotlinx.serialization`. É o que faz isto funcionar:

```kotlin
call.respond(PromoDto("Leve 3", "só até domingo", "R$ 49,90"))
// vira:  {"chamada":"Leve 3","descricao":"só até domingo","preco":"R$ 49,90"}
```

Sem esse plugin, `respond` com um objeto arbitrário dá erro em tempo de
execução. E é ele que permite o caminho inverso, no `PUT`:

```kotlin
val dto = call.receive<PromoDto>()   // JSON do corpo -> objeto Kotlin
```

Para isso funcionar, a classe precisa ser marcada:

```kotlin
@Serializable
data class PromoDto(val chamada: String, val descricao: String, val preco: String)
```

O `@Serializable` é do `kotlinx.serialization`, e é por isso que o módulo aplica
o plugin `kotlin-serialization` no Gradle — ele **gera código** de leitura e
escrita em tempo de compilação. Sem reflexão, o que faz o servidor subir rápido.

**`CallLogging`** — imprime cada requisição no console. Puro conforto de
desenvolvimento.

### `call` — o objeto da requisição

Dentro de um bloco de rota, `call` representa a requisição atual. Dele saem:

```kotlin
call.request.queryParameters["nome"]   // ?nome=Rafael
call.receive<PromoDto>()            // corpo da requisição
call.respond(objeto)                   // resposta como JSON
call.respondText("texto")              // resposta como texto
call.respondBytes(bytes, tipo)         // resposta binária  ← a nossa
```

### Por que os blocos de rota são `suspend`

Cada bloco de rota é uma função `suspend` de Kotlin. Isso significa que ele pode
**pausar sem travar a thread** — durante uma consulta a banco, por exemplo.

É o modelo de concorrência do Ktor: em vez de uma thread por requisição
(o modelo clássico de servlets), poucas threads atendem muitas requisições
intercaladas. É também por isso que `call.receive()` é `suspend`: ler o corpo
da requisição pode precisar esperar pela rede.

No nosso caso não há espera nenhuma — gerar um documento é puro cálculo em
memória, e retorna em microssegundos.

---

# Parte 2 — A fronteira

Aqui está o ponto mais importante desta página. Este é o endpoint inteiro:

```kotlin
get("/documento/boas-vindas") {
    val nome = call.request.queryParameters["nome"] ?: "visitante"
    call.respondBytes(
        welcomeDocument(nome),              // ← A FRONTEIRA
        ContentType.Application.OctetStream,
    )
}
```

- Tudo **em volta** é Ktor: pegar o parâmetro, definir o content-type, responder.
- `welcomeDocument(nome)` é o motor. Ele devolve um `ByteArray` e não sabe
  que existe HTTP.

Prova de que a separação é real: aquela função funcionaria igual dentro de um
teste, de um `main()`, de um job que grava em disco, ou de um Lambda da AWS.

```kotlin
File("home.rc").writeBytes(welcomeDocument("Rafael"))   // funciona
```

### `ContentType.Application.OctetStream`

`application/octet-stream` é o content-type de "bytes genéricos, sem formato
declarado". É o que se usa quando não existe um MIME type específico.

Vale reparar no contraste com um backend comum:

| Backend REST comum | Nosso backend |
|---|---|
| `Content-Type: application/json` | `Content-Type: application/octet-stream` |
| corpo = **descrição** de dados | corpo = **a tela** |
| cliente precisa de DTO e parser | cliente só repassa ao player |
| campo novo = negociar contrato | campo novo não existe |

---

# Parte 3 — O motor de documentos

Três arquivos, com papéis bem distintos.

## `RemoteComposeJvm.kt` — o encanamento

É a infraestrutura que, no Android, a biblioteca `remote-creation-compose` te dá
pronta, e que na JVM você precisa montar à mão. Expõe uma única função útil:

```kotlin
fun documento(largura: Int, altura: Int, densidade: Int, conteudo: RcScope.() -> Unit): ByteArray
```

Por dentro ela monta quatro peças:

```
JvmRcPlatformServices ──┐
                        ├──> Profile ──┐
RemoteComposeWriterFactory ─┘          │
                                       ├──> RemoteComposeWriter ──> bytes
CreationDisplayInfo ───────────────────┘
```

| Peça | Papel |
|---|---|
| `JvmRcPlatformServices` | Diz ao motor como desenhar path e ler imagem **nesta JVM**. É o artefato `remote-creation-jvm`, de 19 KB, sem o qual nada disso existe. |
| `RemoteComposeWriterFactory` | Uma fábrica: como instanciar o escritor. |
| `Profile` | As capacidades do **destino**: `apiLevel` e famílias de operação suportadas. É o mecanismo de compatibilidade da tecnologia. |
| `CreationDisplayInfo` | Largura, altura e densidade de referência. **Esquecer isto = tela em branco** (ver `docs/04`). |
| `RemoteComposeWriter` | O escritor. Recebe as chamadas da DSL e vai empilhando operações num buffer. |

Repare no tipo do último parâmetro: `RcScope.() -> Unit`. Isso é uma **função de
extensão como parâmetro**, o truque que faz DSLs em Kotlin. Dentro daquele
bloco, você chama `Column`, `Text`, `Box` como se fossem funções soltas — quando
na verdade são métodos de um `RcScope` que o Kotlin injeta como `this` invisível.

É o mesmo mecanismo do `routing { get(...) }` do Ktor. Uma vez que você
reconhece o padrão, quase todo DSL de Kotlin fica legível.

## `RcBridge.java` — três linhas em Java

Existe por um motivo específico e vale entender, porque é um truque útil em
geral: `RcScopeImpl` é marcada `internal` no Kotlin, mas `internal` é uma
convenção do **compilador Kotlin**, não da JVM. No bytecode a classe é pública,
e Java a enxerga normalmente.

Não é gambiarra de runtime — é usar uma API que o Kotlin esconde porque ainda
não a considera estável. O risco está anotado no arquivo: sendo API interna de
biblioteca alpha, pode quebrar em qualquer upgrade.

## `Documents.kt` — a interface propriamente dita

Este é o arquivo que "desenha". Ele não sabe o que é HTTP nem o que é
`RemoteComposeWriter` — só usa a DSL:

```kotlin
fun welcomeDocument(nome: String): ByteArray = documento(largura = 1080, altura = 400) {
    Column(
        modifier = Modifier.fillMaxWidth().background(FUNDO_CARTAO).padding(20f),
    ) {
        Text("Olá, $nome!", fontSize = RcSp(22f), color = TITULO)
    }
}
```

Num projeto real, é aqui que moraria a lógica de negócio da apresentação:
segmentação de usuário, teste A/B, calendário de campanhas. E é aqui que
Server-Driven UI ganha o jogo — porque **mudar esta função é mudar o app de
todo mundo**, sem release.

---

# Parte 4 — O ciclo completo de uma requisição

Juntando tudo, o que acontece quando você toca em "Boas-vindas" no app:

```
 APP (Android)                          SERVIDOR (JVM pura)
 ─────────────                          ───────────────────
 DocumentClient.buscar(...)
   │
   │  GET http://10.0.2.2:8080/documento/boas-vindas?nome=Rafael
   ├───────────────────────────────────────▶
   │                                     Netty aceita a conexão
   │                                       │
   │                                     Ktor casa a rota "/documento/boas-vindas"
   │                                       │
   │                                     lê queryParameters["nome"] = "Rafael"
   │                                       │
   │                            ╔══════════▼══════════╗
   │                            ║  FRONTEIRA          ║
   │                            ║  welcomeDocument║
   │                            ╚══════════▼══════════╝
   │                                       │
   │                                     monta Profile + Writer
   │                                       │
   │                                     executa a DSL: Column, Text, Text, Text
   │                                     (cada chamada empilha operações)
   │                                       │
   │                                     writer.encodeToByteArray() -> 396 bytes
   │                                       │
   │  200 OK  application/octet-stream     │
   ◀───────────────────────────────────────┤
   │  396 bytes
   │
 RemoteDocument(bytes)
   │
 RemoteComposePlayer(document = doc)
   │
 pixels na tela
```

O detalhe que vale sublinhar: **em nenhum momento o app interpretou nada**. Ele
não sabe que existe uma `Column`, não sabe que há três textos, não sabe o que é
uma promoção. Ele recebeu bytes e entregou ao player.

---

# Parte 5 — As ferramentas de investigação

Estes dois arquivos **não fazem parte do produto**. Eles existem para responder
perguntas sobre a biblioteca, e ficaram no repositório porque as perguntas
voltam a cada nova versão.

## `Probe.kt` — descobrir por varredura

**"Sonda"** no sentido de sonda espacial: um programa descartável que a gente
manda para dentro de território desconhecido só para trazer informação de volta.

A pergunta dela era: *qual `apiLevel` o formato aceita?* Não havia constante
pública nem documentação. Em vez de deduzir lendo código, ela **testa todos os
valores plausíveis e reporta quais funcionam**:

```
FALHA api=5 mask=0 -> Unsupported API level 5
OK    api=6 mask=0 -> 146 bytes
```

```powershell
.\gradlew.bat :server:runProbe
```

## `Dissect.kt` — enxergar o formato

Imprime a tabela das 172 operações (lida por reflexão, então nunca defasa),
dumps hexadecimais anotados e **análise diferencial** — gera pares de documentos
que diferem em um parâmetro só e mostra quais bytes mudaram.

```powershell
.\gradlew.bat :server:runDissect
```

Foi assim que provamos onde ficam largura e altura, que a densidade não é
gravada, e que cor vira quatro floats. Detalhado em
[`trilha/02-o-formato-por-dentro.md`](../trilha/02-o-formato-por-dentro.md).

---

# Parte 6 — O que mudaria num servidor de produção

Este servidor é didático. Se fosse para valer:

| Hoje | Em produção |
|---|---|
| `AtomicReference` global guardando a promoção | banco de dados ou feature-flag service |
| Documento gerado a cada requisição | cache — o mesmo documento serve milhares de usuários |
| Sem `ETag` / `Cache-Control` | cliente revalidaria em vez de baixar sempre |
| Sem versionamento de `apiLevel` | negociar por `User-Agent` ou header, servindo documento mais simples a players antigos |
| Sem autenticação | o de sempre |
| Sem observabilidade | métricas de tamanho de documento e tempo de geração |

Repare que **nenhuma dessas mudanças toca no motor**. São todas do lado HTTP —
exatamente o benefício de a fronteira ser uma linha só.

Uma consequência específica de Remote Compose vale destaque: como o documento é
um `ByteArray` imutável e autocontido, ele é **perfeito para cache e CDN**. Não
tem sessão, não tem estado, não depende de quem pede. Um `/documento/home`
poderia ficar em cache de borda e ser servido milhões de vezes sem tocar no seu
servidor — algo que um JSON personalizado por usuário nunca consegue.
