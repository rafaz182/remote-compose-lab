# Remote Compose Lab — contexto para o agente

Projeto de **estudo** de `androidx.compose.remote` (Remote Compose **oficial do
Google**). Não é código de produção: o valor está na explicação.

## Regras deste repositório

1. **Código em inglês, texto em português.**
   - Identificadores — classes, funções, variáveis, parâmetros, constantes,
     nomes de arquivo e de pacote — **sempre em inglês**.
   - Comentários, KDoc, documentação, mensagens de commit e todo o texto
     didático exibido no app — **em português**.
   - Uma string que o usuário lê é conteúdo, e fica em português. Uma string
     que é chave, rota ou identificador é código. As rotas HTTP são a exceção
     histórica: `/documento/tela/{id}` ficou em português porque já é contrato
     publicado entre os dois módulos.
2. **Comentário explica o *porquê*.** Se um comentário só repete o que a linha
   faz, apague-o. Registrar *qual erro de build nos trouxe até aqui* é conteúdo
   de primeira classe — várias decisões neste repo vieram de erro real.
3. **Nunca confie na documentação sobre o artefato.** As notas de versão do
   Remote Compose já nos enganaram (ver `minSdk` abaixo). Verifique no POM, no
   `AndroidManifest.xml` do AAR ou com `javap`.
4. **Cada aula precisa rodar.** Nada de trecho ilustrativo que não compila.

## Fatos que já custaram build quebrada

- **`minSdk` = 29.** A doc diz 23. Os manifestos reais dizem
  `remote-player-view` → 26 e `remote-creation-compose` → 29. Vence o maior.
- **`compileSdk` = 37** (instale `platforms;android-37.1`). Exigência do
  Compose BOM 2026.06.01.
- **AGP 9 tem Kotlin embutido.** Aplicar `org.jetbrains.kotlin.android` quebra
  a build. Mas `org.jetbrains.kotlin.plugin.compose` **continua obrigatório**,
  e precisa da mesma versão do Kotlin embutido no AGP (hoje 2.2.10).
- **Artefatos só existem no Google Maven**, não no Maven Central.
- **O tema tem que herdar de AppCompat** — o player infla Views por baixo.

## O ciclo da tecnologia (o coração do projeto)

```
     ESCRITA                    TRANSPORTE              LEITURA
captureSingleRemoteDocument                        RemoteDocument(bytes)
  { RemoteColumn {          →   ByteArray      →           ↓
      RemoteText(...) } }       (documento)        RemoteComposePlayer
```

Remote Compose **não** é "JSON → Compose". É um **formato de documento binário**
com motor de layout e expressões próprias. Você grava um documento; um *player*
o executa na superfície de destino.

## Anatomia dos artefatos (decora isto)

| Artefato | Pacote | Papel |
|---|---|---|
| `remote-core` | **jar** | O formato. `CoreDocument`, `WireBuffer`, `PaintContext` (abstrato), motor de layout. |
| `remote-creation-core` | **jar** | Escrita sem Compose (`RemoteComposeWriter`). É o que um backend usaria. |
| `remote-creation-compose` | aar | Escrita com DSL `@Composable`: `RemoteColumn`, `RemoteText`, `RemoteModifier`. |
| `remote-player-core` / `-view` / `-compose` | aar | Leitura e renderização. `AndroidPaintContext` implementa `PaintContext`. |

Consequência: **iOS é impossível** (não há `.klib`) e **Desktop não tem player**
— renderizar fora do Android exigiria implementar `PaintContext` por conta
própria. Isso está registrado como ideia futura, não como pendência.

## Estrutura

```
app/src/main/java/dev/rafaz/remotecomposelab/
├── MainActivity.kt        # entrada; hospeda o catálogo
├── catalogo/              # modelo das aulas, lista e casca de cada aula
├── ui/                    # tema e componentes didáticos reutilizáveis
├── remoto/                # grava documentos e busca no :server
└── licoes/                # uma aula por arquivo, L01..Lxx
server/src/main/kotlin/    # Ktor + geração de documentos em JVM pura
server/src/main/java/      # RcBridge.java — contorna um `internal` do Kotlin
docs/                      # texto longo: teoria que não cabe em comentário
```

## Gerar documentos no backend (JVM pura) — o que já sabemos

Custou quatro armadilhas seguidas, todas com o mesmo sintoma (tela em branco,
HTTP 200, log limpo). Não repita:

1. `Profile` precisa de `apiLevel >= 6`. Não há constante pública; descobrimos
   varrendo (ver `server/.../Probe.kt`).
2. **Não** envolva o conteúdo em `RcRoot { }` se usar `createRcBuffer` — ele já
   cria a raiz. Duas raízes ⇒ documento 0×0.
3. O tamanho vem do `CreationDisplayInfo` passado ao `RemoteComposeWriter`,
   **não** de `HTag(Header.DOC_WIDTH, ...)`. Como `createRcBuffer` não deixa
   informá-lo, construímos o writer à mão.
4. `RcScopeImpl` é `internal` no Kotlin mas `public` no bytecode — por isso a
   ponte em Java.
5. Use `writer.encodeToByteArray()`, nunca `writer.buffer()` — este devolve o
   array de apoio inteiro (1 MiB de zeros).

**Técnica de depuração que funcionou:** quando não há erro, construa um
controle. Gere o mesmo conteúdo pelo caminho que funciona (no aparelho),
coloque lado a lado, e compare `documento.stats`. Detalhado em
`docs/04-depuracao-do-backend.md`.

## Ferramentas de investigação

```powershell
.\gradlew.bat :server:runProbe      # varre apiLevel x profileMask
.\gradlew.bat :server:runDissect   # tabela de opcodes + hex dump + diferencial
```

`runDissect` é a mais útil: imprime as 172 operações do formato (lidas por
reflexão, nunca defasadas), dumps hexadecimais anotados, e **análise
diferencial** — gera pares de documentos que diferem em um parâmetro e mostra
quais bytes mudaram. Foi como provamos onde ficam largura e altura, que a
densidade não é gravada, e que cor é serializada como quatro floats.
Ver `docs/05-lendo-os-bytes.md`.

## Como validar

```powershell
.\gradlew.bat :app:assembleDebug        # compila
.\gradlew.bat :app:installDebug         # instala no emulador/dispositivo
```

AVD disponível na máquina do Rafael: `Pixel_9`.
