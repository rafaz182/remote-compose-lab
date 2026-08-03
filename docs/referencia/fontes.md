# Referências

Tudo que foi consultado para montar este laboratório, organizado por
confiabilidade. Consulta feita em **01/08/2026**, sobre a versão
`1.0.0-alpha16`.

Está separado assim de propósito: para um artigo técnico, **a origem de cada
afirmação importa**. Boa parte do que este projeto documenta veio da terceira
seção — inspeção direta de artefato — e não de texto publicado por ninguém.

---

## 1. Fontes primárias — oficiais do Google

Foram lidas de ponta a ponta e são a base factual do projeto.

| Fonte | Para quê |
|---|---|
| [Remote Compose — release notes (AndroidX)](https://developer.android.com/jetpack/androidx/releases/compose-remote) | Versões, changelog completo da alpha01 à alpha16, dependências declaradas. **É a única página oficial substancial que existe hoje.** |
| [Índice de artefatos no Google Maven](https://dl.google.com/dl/android/maven2/androidx/compose/remote/group-index.xml) | Lista autoritativa dos 11 artefatos e de todas as versões publicadas. Foi aqui que confirmamos que **não existe artefato iOS/Kotlin-Native**. |
| [AGP 9.0 — release notes](https://developer.android.com/build/releases/agp-9-0-0-release-notes) | Kotlin embutido, mudanças de DSL, propriedades removidas, versões mínimas. Resolveu os itens 1 e 2 do diário de bordo. |
| [Migrar para o Kotlin embutido do AGP](https://developer.android.com/build/migrate-to-built-in-kotlin) | Como configurar `kotlin { compilerOptions }` e como sair do padrão, se precisar. |
| [Versão atual do Gradle](https://services.gradle.org/versions/current) | Endpoint JSON. Foi como fixamos o Gradle 9.6.1. |

### Referência de API

A documentação de API do Remote Compose é gerada, esparsa e sem exemplos — mas é
o único lugar com assinaturas oficiais:

- `androidx.compose.remote.creation` — DSL imperativa
- `androidx.compose.remote.creation.compose.layout` — `RemoteColumn`, `RemoteText`…

> Aviso honesto: em agosto de 2026 essas páginas estavam bem incompletas.
> Foi mais rápido e mais confiável usar `javap` nos artefatos (seção 3).

---

## 2. Fontes secundárias — comunidade

**Não foram usadas como base factual deste projeto.** Ficam registradas porque
são o estado da arte da conversa pública sobre o assunto, e um artigo precisa
saber o que já foi dito.

| Fonte | Observação |
|---|---|
| [Remote Compose: Server-Driven UI — Nativeblocks](https://nativeblocks.io/blog/remote-compose-android-server-driven-ui/) | Um dos poucos textos de comunidade sobre a lib **oficial**. |
| [What is Remote Compose — Dove Letter](https://doveletter.dev/articles/remote-compose) | Visão conceitual. |
| [Remote Compose: The Future of Dynamic UI — Akash Jha (Medium, abr/2026)](https://medium.com/@akashjha/remote-compose-the-future-of-dynamic-ui-a-deep-dive-guide-19f1efed3724) | Guia mais longo encontrado. |

### Não confundir: as outras "Compose Remote"

Este é o erro de rota mais provável para quem pesquisa o assunto — inclusive
aconteceu na primeira hora deste projeto.

| Projeto | O que é | Por que NÃO é o nosso assunto |
|---|---|---|
| [utsmannn/compose-remote-layout](https://github.com/utsmannn/compose-remote-layout) · [docs](https://utsmannn.github.io/compose-remote-layout/) | Lib **da comunidade**, KMP, `io.github.utsmannn`, v0.2.0-alpha01. Transforma **JSON em Compose**. | Nome quase idêntico, proposta totalmente diferente. Suporta iOS/Desktop/Web — que a oficial não suporta. |
| [skydoves/server-driven-compose](https://github.com/skydoves/server-driven-compose) | Demonstração de SDUI com Compose + Firebase. | Padrão JSON clássico, não usa o formato do Google. |

---

## 3. A fonte mais confiável: os próprios artefatos

**A maior parte do que este repositório afirma foi verificada aqui, não em texto
publicado.** Quando documentação e artefato discordaram, o artefato ganhou — e
discordaram mais de uma vez (ver [`diario/01-montando-o-projeto.md`](../diario/01-montando-o-projeto.md)).

Os comandos abaixo são reproduzíveis. Se você for escrever sobre a tecnologia,
rode-os você mesmo em vez de citar este arquivo.

### Descobrir empacotamento e dependências de um artefato

```powershell
$base = "https://dl.google.com/dl/android/maven2/androidx/compose/remote"
$raw = (Invoke-WebRequest "$base/remote-core/1.0.0-alpha16/remote-core-1.0.0-alpha16.pom" -UseBasicParsing).Content
if ($raw -is [byte[]]) { $raw = [System.Text.Encoding]::UTF8.GetString($raw) }
$pom = [xml]$raw
$pom.project.packaging          # jar ou aar -> decide a plataforma
$pom.project.dependencies.dependency | ForEach-Object { "$($_.groupId):$($_.artifactId)" }
```

Foi assim que montamos o grafo de dependências e provamos quais artefatos são
JVM puro.

### Listar as classes de dentro de um JAR/AAR

```powershell
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::OpenRead("remote-core.jar")
$zip.Entries | Where-Object { $_.FullName -like "*.class" } |
  ForEach-Object { $_.FullName -replace '\.class$','' -replace '/','.' } | Sort-Object
$zip.Dispose()
```

Num `.aar`, o código está em `classes.jar` **dentro** do zip — extraia antes.

### Obter assinaturas reais de método

```powershell
javap -public -classpath remote-creation-compose-classes.jar `
  androidx.compose.remote.creation.compose.layout.RemoteTextKt
```

Foi como descobrimos o ciclo `captureSingleRemoteDocument` → `CapturedDocument`
→ `RemoteDocument` → `RemoteComposePlayer` **antes** de escrever a primeira
linha de código.

### Descobrir o `minSdk` de verdade

O `AndroidManifest.xml` de cada `.aar` declara o seu `minSdk`. O merge do AGP usa
o **maior** entre todos. Foi assim que caiu por terra o "minSdk 23" da
documentação — o valor real é **29**.

### Descobrir qual Kotlin o AGP embute

```powershell
# no POM do próprio AGP: org.jetbrains.kotlin:kotlin-gradle-plugin
Invoke-WebRequest "https://dl.google.com/dl/android/maven2/com/android/tools/build/gradle/9.3.1/gradle-9.3.1.pom"
```

---

## 4. Contexto para comparação

Útil para posicionar o assunto num artigo — o que Remote Compose faz de
diferente só fica claro contra o pano de fundo.

- [Compose Multiplatform](https://kotlinlang.org/compose-multiplatform/) — o outro caminho para "Compose em todo lugar", conceitualmente oposto: leva o *runtime* para as plataformas, em vez de levar um *documento*.

---

## Como citar isto num artigo

Se for publicar, o padrão que sugerimos:

- Afirmação sobre **API ou versão** → cite as release notes oficiais.
- Afirmação sobre **plataforma suportada** → cite o `group-index.xml`. É
  incontestável e não envelhece mal.
- Afirmação sobre **comportamento real** (como o `minSdk` 29) → mostre o comando
  e a mensagem de erro. É o conteúdo mais valioso e o mais fácil de defender.
