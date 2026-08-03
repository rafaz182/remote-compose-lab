# Remote Compose Lab

Um laboratório de estudo sobre **Remote Compose**, o Server-Driven UI **oficial
do Google** (`androidx.compose.remote`).

> Não confunda com `io.github.utsmannn:compose-remote-layout`. Aquela é uma
> biblioteca da comunidade que transforma JSON em Compose. Esta aqui é outra
> coisa, bem mais profunda — e é a oficial.

## A ideia em cinco segundos

```
Compose comum:
    Text("Olá")  ───────────────────────────────▶  pixels

Remote Compose:
    RemoteText("Olá")  ──▶  ByteArray  ──▶  player  ──▶  pixels
         escrita            o documento     leitura
```

Aquele `ByteArray` no meio é o ponto inteiro da tecnologia. Ele é um **documento
binário autocontido**, com motor de layout e expressões próprias, que pode ser
salvo, transmitido e executado por um processo que nunca viu o seu código.

## Como rodar

Pré-requisitos: JDK 17+, Android SDK com `platforms;android-37.1`, e um
dispositivo/emulador com **API 29 ou superior**.

### O caminho curto

Precisa de **dois terminais**: um para o servidor, outro para o app.

```powershell
# terminal 1 — o backend (fica rodando)
.\gradlew.bat :server:run

# terminal 2 — compila e instala o app
.\gradlew.bat :app:installDebug
```

Depois abra o app, vá na Aula 04 ou 05 e toque em carregar.

### Comandos, um por um

| O que | Comando |
|---|---|
| Subir o servidor (porta 8080) | `.\gradlew.bat :server:run` |
| Compilar o app | `.\gradlew.bat :app:assembleDebug` |
| Compilar **e** instalar | `.\gradlew.bat :app:installDebug` |
| Ver a tabela de opcodes e dumps hex | `.\gradlew.bat :server:runDissect` |
| Descobrir o `apiLevel` aceito | `.\gradlew.bat :server:runProbe` |
| Limpar tudo | `.\gradlew.bat clean` |

No Linux/macOS troque `.\gradlew.bat` por `./gradlew`.

### Emulador pela linha de comando

Se preferir não abrir o Android Studio só para subir o emulador:

```powershell
# onde mora o SDK (ajuste se o seu estiver em outro lugar)
$SDK = "$env:LOCALAPPDATA\Android\Sdk"

# quais emuladores existem
& "$SDK\emulator\emulator.exe" -list-avds

# subir um deles (em segundo plano, janela minimizada)
Start-Process "$SDK\emulator\emulator.exe" -ArgumentList "-avd","Pixel_9" -WindowStyle Minimized

# esperar ficar pronto de verdade (não basta a janela aparecer)
do { Start-Sleep 3 } until ((& "$SDK\platform-tools\adb.exe" shell getprop sys.boot_completed).Trim() -eq "1")

# conferir
& "$SDK\platform-tools\adb.exe" devices
```

### Testar o servidor sem o app

```powershell
# índice: lista todos os endpoints
curl http://localhost:8080/

# catálogo de telas (JSON)
curl http://localhost:8080/telas

# uma tela (bytes — só o tamanho interessa aqui)
curl -s -o NUL -w "%{size_download} bytes`n" http://localhost:8080/documento/tela/produtos

# mudar a promoção da Aula 04 em tempo real
curl -X PUT http://localhost:8080/promocao -H "Content-Type: application/json" `
  -d '{\"chamada\":\"Frete grátis\",\"descricao\":\"Hoje só\",\"preco\":\"R$ 0\"}'

# desligar a promoção
curl -X DELETE http://localhost:8080/promocao
```

### Se der errado

| Sintoma | Causa provável |
|---|---|
| App mostra "Falhou: Connection refused" | Servidor não está no ar, ou subiu em `localhost` em vez de `0.0.0.0` |
| `No connected devices!` | Emulador caiu — veja a seção do emulador acima |
| `ClassNotFoundException` ao rodar `:server:run` | Cache do Gradle desalinhado: `.\gradlew.bat :server:clean` e tente de novo |
| App instala mas crasha ao abrir | Tema não-AppCompat (ver `res/values/themes.xml`) |

Ou simplesmente abra a pasta no Android Studio, que reconhece os dois módulos.

## Estrutura

| Pasta | O que tem |
|---|---|
| `app/src/main/java/.../lessons/` | Uma aula por arquivo. É aqui que está o conteúdo. |
| `app/src/main/java/.../remote/` | Grava documentos (`rememberDocument`) e busca no servidor. |
| `app/src/main/java/.../ui/` | Casca didática: palco, blocos de código, tema. Compose **comum**. |
| `app/src/main/java/.../catalog/` | Lista de aulas e navegação. |
| `server/src/main/kotlin/` | Ktor + geração de documentos em JVM pura. |
| `server/src/main/java/` | `RcBridge.java` — três linhas que contornam um `internal` do Kotlin. |
| `docs/` | Teoria longa, que não caberia num comentário. |
| `CLAUDE.md` | Contexto e regras do repositório para o agente. |

## Currículo

1. **Olá, Remote Compose** — o ciclo completo, lado a lado com Compose comum.
2. **O documento é o produto** — os bytes como entrega; simulamos o transporte.
3. **Por que um Modifier paralelo?** — a pergunta que explica a biblioteca.
4. **Front × Back de verdade** — um Ktor em JVM pura gera a tela e entrega por HTTP.
5. **A galeria do servidor** — sete telas desenhadas no backend, e eventos voltando ao app.
6. **O experimento dos dois players** — mesmo documento, dois executores; a técnica que isola um defeito.

O que vem depois está mapeado em [`docs/roteiro.md`](docs/roteiro.md),
derivado dos 172 opcodes do formato.

## As classes do SDK

A documentação gerada do `androidx.compose.remote` é escassa — nomes de método
sem explicação e quase nenhum exemplo. Por isso mantemos um mapa próprio de
**todas as classes que este projeto usa**, com o que cada uma faz, com quem
conversa e quais pegadinhas tem:

**→ [`docs/referencia/classes-do-sdk.md`](docs/referencia/classes-do-sdk.md)**

Resumo do elenco principal:

| Lado | Classe | Papel |
|---|---|---|
| servidor | `RemoteComposeWriter` | acumula operações e entrega os bytes |
| servidor | `RcScope` | a DSL: `Column`, `Row`, `Text`, `Modifier` |
| servidor | `RcFloat` | um **valor** dentro do documento (não é número — é fórmula) |
| servidor | `CreationDisplayInfo` | tamanho de referência; **esquecer = tela em branco** |
| servidor | `Profile` | capacidades do destino; exige `apiLevel ≥ 6` |
| cliente | `RemoteDocument` | `ByteArray` → documento executável; expõe `stats` |
| cliente | `RemoteComposePlayer` | o player (existe em duas versões: Compose e View) |
| cliente | `captureSingleRemoteDocument` | grava documento a partir de `@Composable` |
| cliente | `RemoteModifier` / `rdp` / `rsp` | os gêmeos graváveis de `Modifier` / `dp` / `sp` |

Se você só for ler uma coisa de lá, leia a **tabela de decisão**: existem três
`Modifier` diferentes neste projeto, e saber qual usar quando resolve boa parte
da confusão inicial.

## Leia também

- [`docs/referencia/fontes.md`](docs/referencia/fontes.md) — todas as fontes, e os comandos para verificar tudo você mesmo.
- [`docs/trilha/01-a-ideia.md`](docs/trilha/01-a-ideia.md) — por que um formato binário, e não JSON.
- [`docs/referencia/artefatos.md`](docs/referencia/artefatos.md) — os 11 artefatos em detalhe, grafo de dependências, e o que o empacotamento decide sobre plataformas.
- [`docs/diario/01-montando-o-projeto.md`](docs/diario/01-montando-o-projeto.md) — os erros de build reais que enfrentamos.
- [`docs/diario/02-backend-em-jvm-pura.md`](docs/diario/02-backend-em-jvm-pura.md) — cinco problemas, um sintoma só, e a metodologia de depuração sem mensagem de erro.
- [`docs/trilha/02-o-formato-por-dentro.md`](docs/trilha/02-o-formato-por-dentro.md) — dissecando o formato binário byte a byte, com análise diferencial.
- [`docs/trilha/03-o-servidor.md`](docs/trilha/03-o-servidor.md) — Ktor do zero, e onde acaba o backend e começa o motor de documentos.
- [`docs/roteiro.md`](docs/roteiro.md) — o mapa do que ainda dá para aprender, derivado dos 172 opcodes do formato.
- [`docs/referencia/operando-o-emulador.md`](docs/referencia/operando-o-emulador.md) — instalar, tocar, fotografar e depurar o app pela linha de comando, com `adb`.
- [`docs/referencia/classes-do-sdk.md`](docs/referencia/classes-do-sdk.md) — cada classe do SDK que usamos: o que faz, com quem conversa e onde estão as pegadinhas.

## Front × Back

O módulo `:server` é um Ktor em **JVM pura** que gera os documentos e os serve
por HTTP. Ele depende de exatamente três artefatos — `remote-core`,
`remote-creation-core` e `remote-creation-jvm` — e **nenhum deles é Android**.

```
GET /documento/boas-vindas?nome=Rafael
Content-Type: application/octet-stream
396 bytes
```

Não é JSON descrevendo uma tela. É a tela.

```powershell
.\gradlew.bat :server:run      # sobe o servidor na porta 8080
```

Com o app na Aula 04, mude a promoção pelo terminal e veja a interface mudar
sem recompilar nada:

```powershell
curl -X DELETE http://localhost:8080/promocao
```

> Chegar até aqui custou quatro armadilhas seguidas, todas com o mesmo
> sintoma: tela em branco, HTTP 200, log limpo. Estão documentadas uma a uma
> em [`docs/diario/01-montando-o-projeto.md`](docs/diario/01-montando-o-projeto.md) — é o conteúdo
> mais valioso deste repositório, porque não existe em nenhum outro lugar.

## O que este projeto NÃO tem

- **Não há iOS nem Desktop**, e não haverá: a tecnologia não suporta
  (ver [`docs/02`](docs/referencia/artefatos.md)).
- O "transporte" das Aulas 01–03 é **simulado** dentro do mesmo processo. Só a
  Aula 04 usa rede de verdade.

## Estado da tecnologia

`1.0.0-alpha16`, publicada em 29/07/2026. **Alpha de verdade**: a API muda entre
versões (a alpha16 removeu operadores de comparação de `RemoteFloat`, a alpha13
renomeou `RemoteBitmap`). Não use em produção ainda — use para se antecipar.
