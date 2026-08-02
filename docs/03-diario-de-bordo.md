# Diário de bordo: os erros reais que enfrentamos

Este arquivo é diferente dos outros. Ele não ensina Remote Compose — ele
registra **as pedras do caminho**, na ordem em que apareceram, montando este
projeto do zero em agosto de 2026.

Existe por dois motivos. Primeiro, porque você vai reencontrar essas pedras. Segundo, porque um erro de build bem lido ensina mais sobre um ecossistema do
que três posts de blog.

---

## 1. "The 'org.jetbrains.kotlin.android' plugin is no longer required"

**O que aconteceu:** montamos o projeto do jeito que todo tutorial ensina —
plugin do AGP + plugin do Kotlin Android + plugin do Compose. A build recusou na
hora.

**O que estava acontecendo:** o **AGP 9.0 passou a trazer Kotlin embutido**.
Aplicar o plugin de Kotlin manualmente virou erro, não aviso.

**A correção:** remover `org.jetbrains.kotlin.android`.

**O que isso ensina:** praticamente todo material sobre Android que você
encontrar hoje é pré-AGP 9 e vai te mandar fazer algo que não compila mais. Vale
saber disso antes de perder uma tarde.

---

## 2. "...but the Compose Compiler Gradle plugin is required"

**O que aconteceu:** removemos os dois plugins de Kotlin. A build passou a
reclamar da falta do compilador do Compose.

**O que estava acontecendo:** o AGP embute o *Kotlin*, mas **não** aplica o
plugin do *compilador do Compose*. Esse continua sendo seu.

**A pegadinha:** a versão dele precisa casar com a do Kotlin embutido no AGP.
Descobrimos qual é lendo o POM do próprio AGP:

```powershell
# AGP 9.3.1 -> org.jetbrains.kotlin:kotlin-gradle-plugin:2.2.10
```

**A correção:** aplicar `org.jetbrains.kotlin.plugin.compose` na versão `2.2.10`.

---

## 3. "requires libraries and applications to compile against version 37"

**O que aconteceu:** o Compose BOM `2026.06.01` exige `compileSdk 37`, e a
máquina só tinha até a 36.

**A correção:** `sdkmanager "platforms;android-37.1"` e `compileSdk = 37`.

**O que isso ensina:** `compileSdk`, `targetSdk` e `minSdk` são três coisas
independentes. Subir o `compileSdk` só diz "posso enxergar APIs novas" — não
muda comportamento em runtime nem restringe dispositivos.

---

## 4. O `minSdk` que a documentação erra

Este é o achado mais valioso do diário.

As notas de versão do Remote Compose dizem, na alpha04:

> *"Lowered minSdk to 23 by avoiding java.time dependency"*

Colocamos `minSdk = 24` por folga. A build quebrou **duas vezes seguidas**:

```
uses-sdk:minSdkVersion 24 cannot be smaller than version 26
  declared in library [androidx.compose.remote:remote-player-view]
```

Subimos para 26. Quebrou de novo:

```
uses-sdk:minSdkVersion 26 cannot be smaller than version 29
  declared in library [androidx.compose.remote:remote-creation-compose]
```

**A realidade:** o merge de manifesto usa o **maior** `minSdk` entre todas as
dependências. `remote-creation-compose` declara **29**. Logo:

> **Remote Compose, na prática, exige Android 10 (API 29) ou superior.**

Seis níveis de API acima do que a nota de versão sugere. Se você planejou
suporte com base na documentação, replaneje.

**O que isso ensina:** a regra que virou lei neste repositório — *o artefato é a
verdade, a documentação é a intenção*. Quando as duas discordarem, o artefato
ganha, porque é ele que o Gradle vai ler.

---

## 5. O tema precisa ser AppCompat

**O que aconteceu:** esse não chegou a quebrar, porque prevenimos — mas vale o
registro.

`remote-creation-compose` depende de `androidx.appcompat`, e o player infla
`View`s do Android por baixo dos panos. Com um tema que não herde de AppCompat,
o app morre na hora de inflar.

**A prevenção:** `res/values/themes.xml` herda de
`Theme.AppCompat.DayNight.NoActionBar`.

---

## 6. A sutileza que quase virou aula errada

**O que aconteceu:** a Aula 02 mostrava "largura declarada" e "altura declarada"
do documento. Rodando no emulador, os números **mudaram entre uma captura de
tela e outra**: `1080 × 2424` virou `910 × 263`.

**O que estava acontecendo:** `RemoteDocument.width` e `.height` **não são
propriedades dos bytes**. São o resultado da última medição feita pelo player.

**Por que isso é ótimo:** o "erro" é a tese da tecnologia se manifestando. O
documento não carrega um tamanho pronto — ele carrega instruções de layout que
são resolvidas *no destino*. Quem decide a largura é o player, com a tela que
ele tem.

A aula foi reescrita para dizer exatamente isso, e o susto virou conteúdo.

---

---

# Parte 2 — construindo o backend

O `:server` gera documentos Remote Compose em **JVM pura**. Isso funciona, e é
API oficial. Mas não há um único exemplo publicado de como fazer, e o caminho
tem quatro armadilhas em sequência.

**Todas as quatro têm o mesmo sintoma: tela em branco, sem erro, sem log.**
É o pior tipo de bug — o servidor responde 200, o app baixa os bytes, o player
carrega sem exceção, e nada aparece.

---

## 7. `Unsupported API level` — e nenhuma constante para consultar

Para gravar um documento é preciso um `Profile`, e `Profile` exige um
`apiLevel`. Não existe constante pública com o valor certo, não está na
documentação, não está em nenhum exemplo. Qualquer chute abaixo de 6 explode:

```
java.lang.RuntimeException: Unsupported API level 5
    at androidx.compose.remote.core.operations.Header.apply(Header.java:459)
```

**Como resolvemos:** varrendo. Escrevemos uma sonda que testa `apiLevel` de 0 a
8 contra máscaras de operação de 0 a 2, e imprime o que passa:

```
FALHA api=5 mask=0 -> Unsupported API level 5
OK    api=6 mask=0 -> 146 bytes
OK    api=7 mask=0 -> 142 bytes
```

**Resposta: 6 é o piso.** A sonda ficou no repositório (`Sonda.kt`) — se a
biblioteca subir de versão e o piso mudar, é só rodar de novo.

---

## 8. Duas raízes, documento 0×0

Escrever `documento { RcRoot { Column { ... } } }` parece o mais natural do
mundo. Compila, gera bytes válidos, o app baixa sem erro. Tela em branco.

**Como diagnosticamos:** instrumentando o app para imprimir o que o *player*
via, e comparando com um documento que funcionava (gerado no próprio aparelho):

```
LOCAL     910x315 | RootLayoutComponent : 1 ; ColumnLayout : 1 ; CoreText : 2
SERVIDOR    0x0   | RootLayoutComponent : 2 ; ColumnLayout : 1 ; TextLayout : 3
```

A linha `RootLayoutComponent : 2` foi a pista inteira. `createRcBuffer` **já
cria** o componente raiz; o `RcRoot` explícito criava um segundo. O motor de
layout não resolve duas raízes, o documento termina 0×0, e área zero desenha
zero.

> Lição de projeto, para quem for escrever SDUI próprio: **falhe alto**. Duas
> raízes deveriam ser um erro em tempo de escrita, não uma tela vazia em tempo
> de execução.

---

## 9. O tamanho não vem de onde parece

Removida a raiz duplicada, o documento continuou 0×0.

Primeira tentativa: passar `HTag(Header.DOC_WIDTH, 1080)`. Os valores até
aparecem no dump hexadecimal (dá para ver `04 38` = 1080), mas o `CoreDocument`
seguia reportando 0×0 — **não é de lá que ele lê**.

O tamanho vem do `CreationDisplayInfo` entregue ao `RemoteComposeWriter`. E o
atalho `createRcBuffer` não permite informá-lo: ele instancia o writer
internamente, com display info zerado. Dá para ver isso no rastro de pilha da
falha do item 7 — `createRcBufferInternal` chama o construtor do writer direto.

**A correção:** abandonar `createRcBuffer` e construir o writer à mão, com o
construtor que aceita `CreationDisplayInfo(largura, altura, densidade)`.

Faz sentido, quando se pensa: no Android, `captureSingleRemoteDocument` sabe o
tamanho da tela e preenche isso por você. Numa JVM não existe tela — então é
você quem informa.

---

## 10. A classe que o Kotlin esconde

Construir o writer à mão exige `RcScopeImpl`, que é `internal` no Kotlin:

```
e: Cannot access 'class RcScopeImpl : RcScope': it is internal in file.
```

`internal` é uma convenção do **compilador Kotlin**, não da JVM. No bytecode a
classe é `public`. Java não conhece a convenção e enxerga a classe
normalmente — então uma ponte de três linhas em Java resolve
(`server/src/main/java/.../PonteRc.java`).

Não é gambiarra de runtime: é usar uma API que o Kotlin esconde porque ela
ainda não é considerada estável. **O risco é real** e está anotado no arquivo:
sendo API interna de uma biblioteca alpha, pode quebrar em qualquer versão.

---

## 11. `buffer()` devolve 1 MB

Última pedra, e a mais engraçada. Com tudo funcionando, o endpoint passou a
responder:

```
boas-vindas: 1048576 bytes
```

Exatamente 1 MiB. `writer.buffer()` devolve o **array de apoio inteiro** —
pré-alocado, quase todo zeros. O que você quer é `writer.encodeToByteArray()`,
que devolve só a parte escrita: **396 bytes**.

Um servidor em produção teria mandado 1 MB por requisição sem ninguém notar,
porque o documento funciona igual — só vem com 1 milhão de zeros de brinde.

---

## Resultado

```
GET http://10.0.2.2:8080/documento/boas-vindas?nome=Rafael
Content-Type: application/octet-stream
396 bytes
```

Renderizado no emulador por um player que nunca viu o código do servidor. O
servidor depende de exatamente três artefatos — `remote-core`,
`remote-creation-core`, `remote-creation-jvm` — e nenhum deles é Android.

## Moral

Onze problemas. A maioria teria sido evitada com documentação correta —
nenhum teria sido evitado sem **rodar de verdade**.

E repare no padrão da Parte 2: as quatro armadilhas do backend produziram o
**mesmo sintoma idêntico** — tela em branco, HTTP 200, log limpo. Nenhuma delas
teria sido encontrada lendo código. Foram encontradas comparando um caso que
funcionava com um que não funcionava, e imprimindo o que o player realmente
enxergava.

> Se você levar uma técnica deste diário, leve essa: **quando não há erro,
> construa um controle**. Gere o mesmo conteúdo pelo caminho que funciona,
> coloque os dois lado a lado, e compare o estado interno. `RootLayoutComponent
> : 2` contra `: 1` resolveu em segundos o que horas de leitura de código não
> resolveriam.

Alpha é assim. Se você quer chegar cedo numa tecnologia, o preço de entrada é
esse — e a recompensa é conhecer os buracos que o resto do mercado só vai
descobrir daqui a um ano.
