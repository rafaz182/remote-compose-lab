# Remote Compose Lab — contexto para o agente

Projeto de **estudo** de `androidx.compose.remote` (Remote Compose **oficial do
Google**). Não é código de produção: o valor está na explicação.

## Regras deste repositório

1. **Português.** Código, comentários, docs e commits.
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
└── licoes/                # uma aula por arquivo, L01..Lxx
docs/                      # texto longo: teoria que não cabe em comentário
```

## Como validar

```powershell
.\gradlew.bat :app:assembleDebug        # compila
.\gradlew.bat :app:installDebug         # instala no emulador/dispositivo
```

AVD disponível na máquina do Rafael: `Pixel_9`.
