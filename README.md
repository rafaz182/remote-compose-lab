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

```powershell
.\gradlew.bat :app:installDebug
```

Ou abra a pasta no Android Studio e rode normalmente.

## Estrutura

| Pasta | O que tem |
|---|---|
| `app/src/main/java/.../licoes/` | Uma aula por arquivo. É aqui que está o conteúdo. |
| `app/src/main/java/.../remoto/` | O helper que grava documentos (`lembrarDocumento`). |
| `app/src/main/java/.../ui/` | Casca didática: palco, blocos de código, tema. Compose **comum**. |
| `app/src/main/java/.../catalogo/` | Lista de aulas e navegação. |
| `docs/` | Teoria longa, que não caberia num comentário. |
| `CLAUDE.md` | Contexto e regras do repositório para o agente. |

## Currículo

1. **Olá, Remote Compose** — o ciclo completo, lado a lado com Compose comum.
2. **O documento é o produto** — os bytes como entrega; simulamos o transporte.
3. **Por que um Modifier paralelo?** — a pergunta que explica a biblioteca.

Mais aulas vêm por aí — o rumo é combinado a cada passo.

## Leia também

- [`docs/00-referencias.md`](docs/00-referencias.md) — todas as fontes, e os comandos para verificar tudo você mesmo.
- [`docs/01-a-ideia.md`](docs/01-a-ideia.md) — por que um formato binário, e não JSON.
- [`docs/02-anatomia-dos-artefatos.md`](docs/02-anatomia-dos-artefatos.md) — os 11 artefatos em detalhe, grafo de dependências, e o que o empacotamento decide sobre plataformas.
- [`docs/03-diario-de-bordo.md`](docs/03-diario-de-bordo.md) — os erros reais que enfrentamos montando este projeto.

## O que este projeto ainda NÃO tem

Vale dizer em voz alta, porque é a primeira pergunta que um leitor faz:

- **Não há backend.** O "transporte" da Aula 02 é simulado dentro do mesmo
  processo — nenhum byte trafega em rede. É honesto como demonstração do
  conceito, mas não é Front × Back.
- **Não há iOS nem Desktop**, e não haverá: a tecnologia não suporta
  (ver [`docs/02`](docs/02-anatomia-dos-artefatos.md)).

O backend é o próximo passo do projeto.

## Estado da tecnologia

`1.0.0-alpha16`, publicada em 29/07/2026. **Alpha de verdade**: a API muda entre
versões (a alpha16 removeu operadores de comparação de `RemoteFloat`, a alpha13
renomeou `RemoteBitmap`). Não use em produção ainda — use para se antecipar.
