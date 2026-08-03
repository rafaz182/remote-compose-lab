# Lendo os bytes: como dissecar um documento Remote Compose

Até agora tratamos o documento como uma caixa-preta: entra `RemoteText`, sai
`ByteArray`. Esta página abre a caixa.

O objetivo não é você decorar o formato — é você **perder o medo dele**. Um
formato binário parece impenetrável até você perceber que são só números
enfileirados segundo uma regra, e que existe método para descobrir a regra sem
ter o código-fonte.

Tudo aqui foi verificado com a ferramenta do repositório:

```powershell
.\gradlew.bat :server:runDissect
```

---

## Parte 1 — Como se lê um dump hexadecimal

Um *hex dump* é a forma clássica de olhar bytes. Três colunas:

```
00000010  64 00 00 00 32 00 00 00 00 00 00 00 00 C8 FF FF  |d...2...........|
└──┬───┘  └───────────────────┬────────────────────────┘  └───────┬──────┘
 offset            os 16 bytes, em hexadecimal                  ASCII
```

- **Offset**: a posição do primeiro byte da linha, em hexadecimal. `00000010` =
  byte 16. Cada linha avança 16 bytes.
- **Hex**: cada byte como dois dígitos hexadecimais, de `00` a `FF`.
- **ASCII**: cada byte interpretado como caractere, quando é imprimível. O resto
  vira `.`.

### Por que hexadecimal?

Porque **um byte cabe exatamente em dois dígitos hex**. Um byte tem 8 bits =
256 valores possíveis; dois dígitos hexadecimais também dão 256 (16 × 16). A
correspondência é perfeita: cada dígito hex é exatamente 4 bits.

```
byte 0x4B  =  0100 1011  =  75 em decimal
       └┬┘     └─┬┘└─┬┘
        │        4    B
        └── dois dígitos, oito bits
```

Em decimal isso não funciona: 75 não te diz nada sobre os bits. Em hex, `4B`
te diz na hora que o nibble alto é 4 e o baixo é B.

### A coluna ASCII é a sua bússola

Ela parece decorativa e é a parte mais útil. Strings aparecem em texto claro no
meio do binário, e servem de âncora:

```
00000080  00 00 00 0D 4F 6C C3 A1 2C 20 52 61 66 61 65 6C  |....Ol.., Rafael|
```

Achou `Rafael` no meio dos bytes? Então você sabe onde está no documento. A
partir de uma âncora dessas dá para navegar para trás e para frente.

---

## Parte 2 — Os quatro tipos que você precisa conhecer

### Inteiro de 32 bits, big-endian

Quatro bytes formando um número. **Big-endian** significa que o byte mais
significativo vem primeiro — a mesma ordem em que escrevemos números.

```
00 00 04 38
└─────┬────┘
0x00000438  =  4×256 + 3×16 + 8  =  1080
```

Remote Compose usa big-endian, que é também a convenção de rede. Nem todo
formato usa: arquivos do mundo Intel costumam ser *little-endian*, com os bytes
ao contrário. Se um número parece absurdo, tente ler invertido — é o erro mais
comum de quem começa.

### Float de 32 bits (IEEE 754)

Aqui a coisa fica menos óbvia. Um float não é o número escrito em binário: são
três campos empacotados.

```
41 A0 00 00
0100 0001 1010 0000 0000 0000 0000 0000
│└───┬──┘ └────────────┬─────────────┘
│  expoente          mantissa
sinal
```

- **sinal** (1 bit): 0 = positivo
- **expoente** (8 bits): `1000 0011` = 131. Subtraia 127 → **4**
- **mantissa** (23 bits): `010 0000 …` = 0.25, e soma-se 1 implícito → **1.25**

Valor = 1.25 × 2⁴ = **20.0** — que é exatamente o `padding(20f)` do nosso
documento.

Na prática você não faz essa conta à mão. Mas vale reconhecer o **padrão
visual**: floats comuns têm assinaturas que dá para aprender de vista.

| Bytes | Valor |
|---|---|
| `00 00 00 00` | 0.0 |
| `3F 80 00 00` | 1.0 |
| `41 A0 00 00` | 20.0 |
| `41 B0 00 00` | 22.0 |
| `42 C8 00 00` | 100.0 |

Regra de bolso: se um bloco de 4 bytes começa com `3F`, `40`, `41` ou `42`, é
quase certamente um float positivo entre 0.5 e 100.

### Strings

Remote Compose grava strings **prefixadas por tamanho**: um inteiro com o
número de bytes, depois os bytes em UTF-8.

```
00 00 00 0D 4F 6C C3 A1 2C 20 52 61 66 61 65 6C 21
└────┬────┘ └──────────────────┬────────────────┘
  13 bytes         "Olá, Rafael!" em UTF-8
```

Repare: **13 bytes para 12 caracteres**. O `á` ocupa dois bytes em UTF-8
(`C3 A1`). Contar caracteres e contar bytes são coisas diferentes, e formatos
binários contam bytes.

### Opcodes

Um byte que diz "o que vem a seguir". É o coração de qualquer formato deste
tipo, e a próxima seção é toda sobre isso.

---

## Parte 3 — A tabela de opcodes

Um documento Remote Compose é uma **sequência de operações**. Cada operação
começa por um número que a identifica, e esse número tem nome na classe
`Operations` do `remote-core`.

A ferramenta lê essa tabela por reflexão, então ela nunca fica defasada:

```kotlin
Operations::class.java.declaredFields
    .filter { Modifier.isStatic(it.modifiers) && it.type == Int::class.javaPrimitiveType }
    .map { it.name to it.get(null) as Int }
    .sortedBy { it.second }
```

São **172 operações** na `1.0.0-alpha16`. As que aparecem nos nossos
documentos:

| Código | Hex | Nome | O que é |
|---|---|---|---|
| 0 | `00` | `HEADER` | cabeçalho do documento |
| 16 | `10` | `MODIFIER_WIDTH` | modificador de largura |
| 55 | `37` | `MODIFIER_BACKGROUND` | cor/pincel de fundo |
| 58 | `3A` | `MODIFIER_PADDING` | espaçamento interno |
| 102 | `66` | `DATA_TEXT` | uma string armazenada |
| 200 | `C8` | `LAYOUT_ROOT` | componente raiz |
| 201 | `C9` | `LAYOUT_CONTENT` | conteúdo de um componente |
| 204 | `CC` | `LAYOUT_COLUMN` | uma Column |
| 208 | `D0` | `LAYOUT_TEXT` | um Text |
| 214 | `D6` | `CONTAINER_END` | fim de um container |

Só de olhar essa tabela já dá para entender o formato: **desenho, layout,
modificadores, dados e ações, tudo achatado numa fita de operações.** É a
mesma ideia de um bytecode de máquina virtual.

---

## Parte 4 — Um documento inteiro, byte a byte

Este é o menor documento possível: 100×50, sem conteúdo nenhum.

```kotlin
documento(largura = 100, altura = 50) { }
```

**35 bytes:**

```
00000000  00 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00
00000010  64 00 00 00 32 00 00 00 00 00 00 00 00 C8 FF FF
00000020  FF FE D6
```

Decodificado:

```
offset  bytes           significado
──────────────────────────────────────────────────────────────────
  0     00              opcode HEADER
  1-12  00 00 00 01 …   campos de versão (ver "o que falta", abaixo)
 13-16  00 00 00 64     LARGURA  = 100      ← provado por diferencial
 17-20  00 00 00 32     ALTURA   = 50       ← provado por diferencial
 21-28  00 × 8          campos ainda não decodificados
──────────────────────────────────────────────────────────────────
 29     C8              opcode LAYOUT_ROOT (200)
 30-33  FF FF FF FE     id = -2
──────────────────────────────────────────────────────────────────
 34     D6              opcode CONTAINER_END (214)
```

**O cabeçalho ocupa os bytes 0 a 28 — 29 bytes.** E isso confere com o que o
player relata sobre o mesmo documento:

```
stats = Header : 1:29 ; …
        └── 1 operação de header, ocupando 29 bytes
```

Duas fontes independentes concordando é o que transforma "acho que é isso" em
"é isso".

Repare também na **estrutura de árvore achatada**: `LAYOUT_ROOT` abre,
`CONTAINER_END` fecha. Não há aninhamento físico nos bytes — a hierarquia é
reconstruída pelo leitor a partir dos marcadores de abertura e fechamento.
É exatamente como HTML funciona, só que com um byte em vez de `<div>`.

---

## Parte 5 — A técnica: análise diferencial

Aqui está o método que transforma adivinhação em prova.

**A ideia:** gere dois documentos que diferem em **exatamente um** parâmetro e
veja quais bytes mudaram. Se eu mudo só a largura e só os bytes 13–16 mudam,
então a largura mora nos bytes 13–16. Não é interpretação, é evidência.

```kotlin
val a = documento(largura = 100, altura = 50) { }
val b = documento(largura = 300, altura = 50) { }

(0 until minOf(a.size, b.size))
    .filter { a[it] != b[it] }
    .forEach { println("offset %2d: %02X -> %02X".format(it, a[it], b[it])) }
```

### Resultado 1 — onde mora a largura

```
--- largura 100 -> 300 ---
  offset 15: 00 -> 01
  offset 16: 64 -> 2C
```

Só dois bytes mudaram, e `0x012C` = 300. Note que os bytes 13 e 14 não mudaram
porque continuam zero nos dois casos — o inteiro tem 4 bytes, mas só os dois
últimos precisaram mudar. **Provado: largura nos bytes 13–16.**

### Resultado 2 — e a altura

```
--- altura 50 -> 999 ---
  offset 19: 00 -> 03
  offset 20: 32 -> E7
```

`0x03E7` = 999. **Provado: altura nos bytes 17–20.**

### Resultado 3 — o achado inesperado

```
--- densidade 420 -> 160 ---
  NENHUM byte mudou
```

A densidade que passamos ao `CreationDisplayInfo` **não é gravada no
documento**. Ela é usada durante a criação, para converter unidades, mas não
sobrevive à serialização.

Isso não é um detalhe menor — é coerente com a tese da tecnologia: o documento
carrega *fórmulas de layout*, e quem resolve densidade é o player, com a tela
que ele tem. Descobrimos isso sem ler uma linha do código da biblioteca.

### Resultado 4 — como uma cor é gravada

```
--- cor de fundo #FF000000 -> #FFFF0000 ---
  offset 72: 00 -> 3F
  offset 73: 00 -> 80
```

`00 00 00 00` virou `3F 80 00 00`. Reconhece? É **0.0f virando 1.0f**.

Ou seja: a cor **não** é gravada como o inteiro ARGB que você escreveu. É
gravada como **quatro floats** — R, G, B, A, cada um de 0.0 a 1.0. Só o canal
vermelho mudou entre preto e vermelho puro, e só ele mudou nos bytes.

Confirmando no documento real, cujo fundo é `#FF1B3A4B`:

```
00000050  00 3D D8 D8 D9 3E 68 E8 E9 3E 96 96 97 3F 80 00
             └────┬───┘ └────┬───┘ └────┬───┘ └────┬───┘
              0.10588      0.22745     0.29412      1.0
               × 255        × 255       × 255
               = 27         = 58        = 75        alpha
               = 0x1B       = 0x3A      = 0x4B
```

`#FF` `1B` `3A` `4B` — bate exatamente. **Cor é float, não int.**

### Resultado 5 — por que diffs binários enganam

Trocar `"Rafael"` por `"Ana"` reportou **mais de 200 bytes diferentes** num
documento de 396.

Isso não significa que a mudança foi grande. Significa que a string encurtou 3
bytes e **tudo depois dela deslizou 3 posições**. A comparação byte-a-byte
perde o alinhamento e passa a acusar diferença em quase tudo.

O único byte realmente informativo foi:

```
offset 131: 0D -> 0A     (13 bytes -> 10 bytes)
```

o prefixo de tamanho da string.

> **Lição prática:** em formato de tamanho variável, só compare documentos de
> **mesmo tamanho**. Se o tamanho mudou, o diff bruto não serve — foi por isso
> que os experimentos de largura, altura, densidade e cor foram desenhados para
> não alterar o comprimento.

---

## Parte 6 — Lendo o documento real

Agora dá para percorrer os 396 bytes do documento de boas-vindas e reconhecer
tudo. Trechos comentados:

```
00000000  00 ...                          HEADER
0000000D  00 00 04 38                     largura = 1080
00000011  00 00 01 90                     altura  = 400
0000001D  C8 FF FF FF FE                  LAYOUT_ROOT
00000022  CC ...                          LAYOUT_COLUMN
00000034  10 ...                          MODIFIER_WIDTH
0000003F  37 ...                          MODIFIER_BACKGROUND
00000051  3D D8 D8 D9  3E 68 E8 E9        R=0.106  G=0.227
          3E 96 96 97  3F 80 00 00        B=0.294  A=1.0     -> #FF1B3A4B
0000005D  3A ...                          MODIFIER_PADDING
00000061  41 A0 00 00 × 4                 20.0 nos quatro lados
00000074  C9 ...                          LAYOUT_CONTENT
0000007D  66                              DATA_TEXT
0000007E  00 00 00 2A                     id da string = 42
00000082  00 00 00 0D                     tamanho = 13 bytes
00000086  4F 6C C3 A1 2C 20 52 61 …       "Olá, Rafael!"  (UTF-8)
00000091  D0 ...                          LAYOUT_TEXT
0000009C  FF 7F DB FF                     cor do texto #FF7FDBFF
000000A0  41 B0 00 00                     fontSize = 22.0
…
00000186  D6 D6 D6 D6 D6                  CONTAINER_END × 5
```

Vale reparar em três coisas:

**1. Strings ficam separadas do layout.** O `DATA_TEXT` armazena a string com
um id (42); depois o `LAYOUT_TEXT` a referencia. Isso permite reaproveitar o
mesmo texto em vários lugares sem duplicar bytes — e é o que torna possível
trocar o valor de uma string num documento já carregado.

**2. Nem toda cor é float.** O fundo virou quatro floats (porque veio de um
modificador), mas a cor do texto aparece como `FF 7F DB FF` — o inteiro ARGB
direto. Caminhos diferentes da API gravam de formas diferentes. Formatos reais
são assim: cheios de exceções pragmáticas.

**3. A sequência de `D6` no fim** são os `CONTAINER_END` fechando, de dentro
para fora, todos os containers abertos. Cinco fechamentos para
root → column → content → text × 2.

---

## Parte 7 — O que isso ensina sobre projetar formatos binários

Você quer escrever seu próprio Server-Driven UI. Então o valor real desta
página não é conhecer o formato do Google — é enxergar as **decisões de projeto**
que ele tomou:

| Decisão | Por que | Custo |
|---|---|---|
| Opcode de 1 byte | 256 operações cabem; leitura trivial | teto de 256 (já usaram 172) |
| Big-endian | convenção de rede, legível no dump | irrelevante hoje |
| Strings com prefixo de tamanho | não precisa varrer procurando terminador | diff binário quebra |
| Strings em tabela, referenciadas por id | reuso e atualização em runtime | uma indireção |
| Cor como 4 floats | interpolação e animação diretas | 16 bytes em vez de 4 |
| Árvore achatada com marcador de fim | leitura em passada única, sem recursão | erro de balanceamento é silencioso |
| Dimensões no cabeçalho | player sabe o tamanho antes de desenhar | esquecer = tela em branco muda |

Aquela última linha é a que nos custou horas — veja
[`04-depuracao-do-backend.md`](04-depuracao-do-backend.md). E é um erro de
projeto do formato, não nosso: **um documento sem dimensão deveria se recusar a
nascer.**

---

## Parte 8 — Exercícios

A ferramenta está no repositório, então dá para continuar a investigação.
Ficaram bytes sem decodificar — os offsets 1 a 12 e 21 a 28 do cabeçalho.

1. **Onde mora o `apiLevel`?** Parametrize `documento()` para aceitar o
   apiLevel, gere um documento com 6 e outro com 7, e compare. Cuidado: o
   tamanho pode mudar.
2. **O que há nos bytes 21–28?** Estão zerados em todos os nossos documentos.
   Descubra o que os faz mudar — comece por `Header.DOC_DESIRED_FPS` e
   `Header.DOC_CONTENT_DESCRIPTION`.
3. **Quanto custa cada componente?** Gere documentos com 1, 2 e 3 textos e
   descubra o custo marginal de um `Text`. É um dado ótimo para artigo.
4. **Compare os dois geradores.** Um documento equivalente feito no app
   (`captureSingleRemoteDocument`) tem cabeçalho de 57 bytes; o do servidor tem
   29. O que o Android grava a mais?

## As ferramentas

```powershell
.\gradlew.bat :server:runDissect   # tabela de opcodes + dumps + diferencial
.\gradlew.bat :server:runProbe      # varredura de apiLevel
```

Código em `server/src/main/kotlin/.../Dissect.kt`. São ~100 linhas —
vale ler, porque a técnica vale para qualquer formato binário, não só este.
