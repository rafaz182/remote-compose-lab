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

O `:server` deste repositório prova isso na prática: é um Ktor em **JVM pura**
que desenha telas e as entrega por HTTP.

```
GET /documento/tela/produtos
Content-Type: application/octet-stream
2003 bytes
```

Não é JSON descrevendo uma tela. É a tela.

## Como rodar

Pré-requisitos: JDK 17+, Android SDK com `platforms;android-37.1`, e um
dispositivo/emulador com **API 29 ou superior**.

Precisa de **dois terminais**:

```powershell
# terminal 1 — o backend (fica rodando)
.\gradlew.bat :server:run

# terminal 2 — compila e instala o app
.\gradlew.bat :app:installDebug
```

Abra o app e vá na Aula 05. No Linux/macOS troque `.\gradlew.bat` por `./gradlew`.

| Comando | O que faz |
|---|---|
| `:server:run` | sobe o backend na porta 8080 |
| `:app:installDebug` | compila e instala o app |
| `:server:runDissect` | tabela de opcodes + dump hexadecimal de documentos |
| `:server:runProbe` | descobre qual `apiLevel` o formato aceita |

Para subir o emulador sem abrir o Android Studio, instalar, tocar e tirar print
pela linha de comando: [`docs/referencia/operando-o-emulador.md`](docs/referencia/operando-o-emulador.md).

### Vendo a UI mudar sem recompilar

Com o app aberto na Aula 04, mude a promoção pelo terminal:

```powershell
curl -X DELETE http://localhost:8080/promocao
```

Toque em "Promoção" de novo. A interface muda — sem release, sem loja.

### Se der errado

| Sintoma | Causa provável |
|---|---|
| "Falhou: Connection refused" | servidor fora do ar, ou subiu em `localhost` em vez de `0.0.0.0` |
| `No connected devices!` | emulador caiu |
| `ClassNotFoundException` no `:server:run` | cache do Gradle desalinhado: `.\gradlew.bat :server:clean` |
| app instala mas crasha ao abrir | tema não-AppCompat (ver `res/values/themes.xml`) |

## Documentação

**→ Comece pelo índice: [`docs/README.md`](docs/README.md)**

Está organizada por **como se lê**: [`trilha/`](docs/trilha/) para aprender em
ordem, [`referencia/`](docs/referencia/) para consultar, [`diario/`](docs/diario/)
para acompanhar o que foi tentado — inclusive o que falhou — e
[`roteiro.md`](docs/roteiro.md) para o que vem depois.

Se você tem duas horas e quer entender a tecnologia, leia a trilha na ordem.
Se quer entender **como se investiga uma biblioteca sem documentação**, o
diário vale mais.

## Estrutura

| Pasta | O que tem |
|---|---|
| `app/src/main/java/.../lessons/` | Uma aula por arquivo. É aqui que está o conteúdo. |
| `app/src/main/java/.../remote/` | Grava documentos (`rememberDocument`) e busca no servidor. |
| `app/src/main/java/.../ui/` | Casca didática: palco, blocos de código, tema. Compose **comum**. |
| `app/src/main/java/.../catalog/` | Lista de aulas e navegação. |
| `server/src/main/kotlin/` | Ktor + geração de documentos em JVM pura. |
| `server/src/main/java/` | `RcBridge.java` — contorna um `internal` do Kotlin. |
| `docs/` | Trilha, referência e diário. |
| `CLAUDE.md` | Contexto e regras do repositório para o agente. |

## Currículo

1. **Olá, Remote Compose** — o ciclo completo, lado a lado com Compose comum.
2. **O documento é o produto** — os bytes como entrega; simulamos o transporte.
3. **Por que um Modifier paralelo?** — a pergunta que explica a biblioteca.
4. **Front × Back de verdade** — um Ktor em JVM pura gera a tela e entrega por HTTP.
5. **A galeria do servidor** — sete telas desenhadas no backend, e eventos voltando ao app.
6. **O experimento dos dois players** — mesmo documento, dois executores; a técnica que isola um defeito.

## O que este projeto NÃO tem

- **Não há iOS nem Desktop**, e não haverá: a tecnologia não suporta — não
  existe um único `.klib` publicado
  (ver [`docs/referencia/artefatos.md`](docs/referencia/artefatos.md)).
- O "transporte" das Aulas 01–03 é **simulado** dentro do mesmo processo. Só a
  Aula 04 em diante usa rede de verdade.
- **Estado remoto ainda não funciona.** `setValue` e `StateUpdater` não surtem
  efeito, e a investigação está em aberto —
  [`docs/diario/03-estado-remoto.md`](docs/diario/03-estado-remoto.md).

## Estado da tecnologia

`1.0.0-alpha16`, publicada em 29/07/2026. **Alpha de verdade**: a API muda entre
versões (a alpha16 removeu operadores de comparação de `RemoteFloat`, a alpha13
renomeou `RemoteBitmap`). Não use em produção ainda — use para se antecipar.

Boa parte do que este repositório afirma foi apurada **nos artefatos**, não na
documentação — que já nos enganou mais de uma vez. O `minSdk` real, por exemplo,
é 29 e não 23. Ver [`docs/referencia/fontes.md`](docs/referencia/fontes.md).
