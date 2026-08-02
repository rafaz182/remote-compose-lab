# Depurando o backend: cinco problemas, um sintoma só

Gerar documentos Remote Compose num servidor JVM **funciona** e é API oficial.
Mas não existe um único exemplo publicado de como fazer, e o caminho tem cinco
armadilhas em sequência.

O que torna esta página interessante não é a lista de problemas — é que
**quatro dos cinco produziram exatamente o mesmo sintoma**:

> Tela em branco. HTTP 200. Nenhuma exceção. Log limpo.

Sem mensagem de erro, as técnicas normais de depuração não servem. Não há
*stack trace* para ler, não há linha para colocar *breakpoint*, não há string
para pesquisar no Google. Este documento é, na prática, um estudo de caso
sobre **como depurar quando o sistema não reclama de nada**.

Cada problema abaixo segue a mesma estrutura: **Sintoma → Causa → Metodologia →
Resultado**, mais a técnica generalizável que dá para levar para outros
projetos.

---

## Antes de tudo: por que "sem erro" é pior que "com erro"

Quando algo lança exceção, o sistema te entrega meio caminho andado: o local, a
pilha, muitas vezes a causa. Quando algo apenas **não acontece**, você tem zero
informação — e o instinto errado é começar a ler código procurando o defeito a
olho nu.

Ler código para achar bug invisível é caro e pouco confiável. As três técnicas
que realmente funcionaram aqui foram outras:

| Técnica | Quando usar |
|---|---|
| **Varredura** | O parâmetro certo existe mas você não sabe qual é |
| **Controle** | Existe um caminho que funciona e outro que não |
| **Instrumentação do consumidor** | Você não sabe o que o outro lado está enxergando |

Nenhuma delas exige entender o código da biblioteca. Todas produzem evidência.

---

## Problema 1 — `Unsupported API level`

### Sintoma

Este foi o único que gritou. Ao gerar o primeiro documento:

```
java.lang.RuntimeException: Unsupported API level 1
    at androidx.compose.remote.core.operations.Header.apply(Header.java:459)
    at androidx.compose.remote.core.RemoteComposeBuffer.addHeader(...)
    at androidx.compose.remote.creation.RemoteComposeWriter.<init>(...)
```

### Causa

Para gravar um documento é preciso um `Profile`, e o construtor de `Profile`
exige um `apiLevel`. Não existe constante pública com o valor válido, não está
na documentação, não está em nenhum exemplo. Eu havia chutado `1`.

### Metodologia — **varredura**

Quando o espaço de valores é pequeno e a validação é barata, não pesquise:
**teste todos**. Escrevi uma sonda que percorre `apiLevel` de 0 a 8 contra
máscaras de operação de 0 a 2, capturando a exceção de cada combinação:

```kotlin
for (api in 0..8) {
    for (mask in 0..2) {
        runCatching {
            createRcBuffer(RcProfile(Profile(api, mask, plataforma, fabrica))) {
                RcRoot { Column { Text("teste") } }
            }
        }.onSuccess  { println("  OK   api=$api mask=$mask -> ${it.size} bytes") }
         .onFailure  { println("  FALHA api=$api mask=$mask -> ${it.message}") }
    }
}
```

Saída:

```
FALHA api=5 mask=0 -> Unsupported API level 5
OK    api=6 mask=0 -> 146 bytes
OK    api=7 mask=0 -> 142 bytes
```

### Resultado

**`apiLevel = 6` é o piso.** A sonda ficou no repositório
(`server/src/main/kotlin/.../Sonda.kt`): quando a biblioteca subir de versão,
rodar de novo responde em segundos se o piso mudou.

> **Técnica generalizável:** transforme "qual é o valor certo?" numa pergunta
> executável. Trinta segundos escrevendo um laço economizam uma hora
> vasculhando fonte — e o laço fica no repositório respondendo de novo no
> futuro.

---

## Problema 2 — documento 0×0 com duas raízes

### Sintoma

Servidor responde 200 com 402 bytes. App baixa os 402 bytes. Player carrega
sem exceção. **Nada aparece.**

### Causa

O código era o mais natural do mundo:

```kotlin
documento { RcRoot { Column { ... } } }
```

Só que `createRcBuffer` **já cria** o componente raiz. O `RcRoot` explícito
criava um segundo. O motor de layout não resolve duas raízes, o documento
termina com dimensão 0×0, e área zero desenha zero.

### Metodologia — **controle**

Aqui está a virada da depuração inteira. Como não havia erro para investigar,
construí um **controle**: o mesmo conteúdo visual, gerado pelo caminho que
comprovadamente funciona (dentro do próprio app, com `captureSingleRemoteDocument`),
renderizado logo acima do que vinha do servidor.

Dois documentos, mesmo conteúdo, mesma tela, mesmo player. Um funciona, o outro
não. **Toda diferença observável entre eles é suspeita.**

Aí bastou imprimir o estado interno dos dois. `RemoteDocument` expõe
`getStats()`, que lista as operações do documento:

```kotlin
Log.i("RemoteComposeLab",
    "doc ${doc.width}x${doc.height} | stats=" + doc.stats.joinToString(" ; "))
```

Resultado lado a lado:

```
LOCAL     910x315 | RootLayoutComponent : 1 ; ColumnLayout : 1 ; CoreText : 2
SERVIDOR    0x0   | RootLayoutComponent : 2 ; ColumnLayout : 1 ; TextLayout : 3
```

A linha `RootLayoutComponent : 2` contra `: 1` entregou o problema em segundos.

### Resultado

Removido o `RcRoot`. As estatísticas passaram a bater (`RootLayoutComponent : 1`).

> **Técnica generalizável:** quando não há erro, **construa um controle**. Um
> caso que funciona, ao lado de um que não funciona, com o máximo de variáveis
> em comum. Depurar vira comparar, e comparar é muito mais barato que deduzir.
>
> Corolário: procure a API de introspecção da biblioteca **antes** de precisar
> dela. `getStats()` não está documentado em lugar nenhum — foi achado
> vasculhando `javap` do `RemoteDocument`. Vale o hábito de listar os métodos
> públicos de uma classe central logo no começo do projeto.

---

## Problema 3 — o tamanho não vem de onde parece

### Sintoma

Raiz duplicada resolvida, estatísticas iguais às do controle. **Documento
continuou 0×0. Tela continuou em branco.**

### Causa

Um documento carrega as próprias dimensões. Sem elas nasce 0×0. No Android
isso nunca acontece porque `captureSingleRemoteDocument` recebe um
`RemoteCreationDisplayInfo` com o tamanho da tela e preenche por você. Numa JVM
não existe tela — então é você quem informa. E o atalho `createRcBuffer` não
oferece onde informar.

### Metodologia — **seguir o rastro de pilha de outro erro**

Esta foi a parte mais bonita da investigação, e a lição é contraintuitiva:
**a exceção do Problema 1 continha a resposta do Problema 3.**

Releia o rastro lá de cima:

```
at androidx.compose.remote.creation.RemoteComposeWriter.<init>(RemoteComposeWriter.java:251)
at androidx.compose.remote.creation.dsl.RcDocCreatorKt.createRcBufferInternal(RcDocCreator.kt:73)
```

`createRcBufferInternal` chama o **construtor do writer diretamente**. Ou seja:
`createRcBuffer` instancia o `RemoteComposeWriter` por conta própria, com o
display info que ele quiser — e não há parâmetro para intervir.

Antes de chegar nisso eu tinha tentado o caminho errado: passar
`HTag(Header.DOC_WIDTH, 1080)`. Os valores até apareceram nos bytes (dá para
ver `04 38` = 1080 no dump hexadecimal), mas o `CoreDocument` seguia reportando
0×0 — **não é de lá que ele lê**. Foi um beco sem saída útil: provou que
escrever no cabeçalho e o documento *ter* dimensão são coisas diferentes.

### Resultado

Abandonar `createRcBuffer` e construir o writer à mão, com o construtor que
aceita `CreationDisplayInfo`:

```kotlin
val writer = RemoteComposeWriter(
    CreationDisplayInfo(largura, altura, densidade),
    "", profile, null,
)
```

> **Técnica generalizável:** guarde os rastros de pilha de erros que você já
> resolveu. Eles são um mapa gratuito das entranhas da biblioteca — mostram
> quem chama quem, e num projeto sem documentação isso vale ouro.
>
> Segundo corolário: quando uma correção "quase funciona" (o valor aparece nos
> bytes mas o comportamento não muda), você provavelmente escreveu no lugar
> certo pelo caminho errado. Pare de insistir e procure quem *lê* aquele dado.

---

## Problema 4 — a classe que o Kotlin esconde

### Sintoma

Erro de compilação, finalmente:

```
e: Cannot access 'class RcScopeImpl : RcScope': it is internal in file.
```

### Causa

Construir o writer à mão exige `RcScopeImpl` para criar o escopo da DSL. Ela é
marcada `internal` no Kotlin.

### Metodologia — **conhecer a diferença entre a linguagem e a plataforma**

`internal` é uma convenção do **compilador Kotlin**, registrada nos metadados
do módulo. Na JVM ela não existe: no bytecode a classe é `public`. Dá para
confirmar com `javap`, que lê o bytecode e ignora metadados de Kotlin:

```powershell
javap -public -classpath remote-creation-core.jar `
  androidx.compose.remote.creation.dsl.RcScopeImpl

# public androidx.compose.remote.creation.dsl.RcScopeImpl(RemoteComposeWriter);
```

O construtor está lá, público. Só o compilador Kotlin se recusa a enxergá-lo.
Java não conhece a convenção.

### Resultado

Uma ponte de três linhas em Java (`server/src/main/java/.../PonteRc.java`):

```java
public static byte[] escrever(RemoteComposeWriter writer, Function1<RcScope, Unit> conteudo) {
    RcScopeImpl escopo = new RcScopeImpl(writer);
    escopo.RcRoot(conteudo);
    return writer.encodeToByteArray();
}
```

**Risco assumido e anotado no arquivo:** é API interna de biblioteca alpha.
`internal` sinaliza "isto pode mudar sem aviso". Se o `:server` parar de
compilar após um upgrade, comece a investigar por aqui.

> **Técnica generalizável:** saiba o que sua linguagem esconde e o que a
> plataforma realmente impõe. `internal` do Kotlin, `private` de módulo Java,
> `@RestrictTo` do AndroidX — são portas fechadas por convenção, não por
> tranca. Abri-las é legítimo quando você sabe o custo; o erro é abrir sem
> registrar o risco.

---

## Problema 5 — 1 MiB de zeros

### Sintoma

Tudo funcionando, e o endpoint respondendo:

```
boas-vindas: 1048576 bytes
```

Exatamente 1 MiB. E o documento renderizava normalmente.

### Causa

`writer.buffer()` devolve o **array de apoio inteiro** — pré-alocado com 1 MiB,
quase todo zeros. Os bytes úteis estão no começo; o resto é enchimento.

### Metodologia — **desconfiar de número redondo**

Não houve investigação sofisticada aqui: 1048576 é 2²⁰. Números exatamente
redondos em potência de dois quase nunca são o tamanho real de um dado — são o
tamanho de um *buffer*. Bastou procurar, entre os métodos do writer, um que
soubesse onde o conteúdo termina:

```
public byte[] buffer();              // o array inteiro
public int bufferSize();             // quanto foi realmente escrito
public byte[] encodeToByteArray();   // <- este
```

### Resultado

`encodeToByteArray()` → **396 bytes**.

Repare no que teria acontecido sem essa observação: nada quebraria. O documento
funciona igual. Um servidor em produção mandaria 1 MB por requisição, para
sempre, e ninguém notaria até a conta de banda chegar.

> **Técnica generalizável:** trate tamanhos como sinal, não como detalhe.
> Potências de dois exatas denunciam buffer; tamanho que não muda quando o
> conteúdo muda denuncia cache ou constante. **O bug que não quebra nada é o
> que fica mais tempo em produção** — e só o número esquisito te avisa.

---

## Resultado final

```
GET http://10.0.2.2:8080/documento/boas-vindas?nome=Rafael
Content-Type: application/octet-stream

396 bytes
```

Renderizado no emulador por um player que nunca viu o código do servidor. O
`:server` depende de exatamente três artefatos — `remote-core`,
`remote-creation-core` e `remote-creation-jvm` — e **nenhum deles é Android**.

## O resumo da metodologia

Se você tirar só uma coisa desta página:

1. **Sem erro? Construa um controle.** Duas execuções quase idênticas, uma boa
   e uma ruim, e compare o estado interno. Foi o que resolveu o caso mais
   difícil, em segundos.
2. **Não sabe o valor? Varra o espaço.** Um laço que testa e reporta vale mais
   que uma hora lendo fonte, e fica no repositório.
3. **Guarde os rastros de pilha.** Num projeto sem documentação, eles são o
   mapa das entranhas — o erro de hoje explica o bug de amanhã.
4. **Números redondos são suspeitos.** 1048576 não é um tamanho, é um buffer.
5. **Procure a API de introspecção cedo.** `getStats()` não está documentado em
   lugar nenhum e foi o que virou o jogo.

Para ir além da estatística e olhar os bytes de verdade, siga para
**[`05-lendo-os-bytes.md`](05-lendo-os-bytes.md)**.
