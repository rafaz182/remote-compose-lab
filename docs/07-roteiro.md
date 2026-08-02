# Roteiro: tudo que ainda dá para aprender

Este é o mapa do território. Ele não saiu de um índice de documentação — a
documentação oficial do Remote Compose não tem índice. Saiu de duas fontes
concretas:

- a **tabela de 172 opcodes**, extraída por reflexão da classe `Operations`
  (`.\gradlew.bat :server:runDissecar`);
- os **pacotes públicos** dos artefatos, listados com `javap`.

Ou seja: cada tópico abaixo corresponde a capacidade que **existe no
binário**. Onde eu ainda não verifiquei o comportamento, está marcado.

Legenda: ✅ feito · 🔜 próximo · 🔬 existe mas não verifiquei

---

## A. Fundamentos — ✅ concluído

| # | Tópico | Onde |
|---|---|---|
| ✅ | O ciclo: escrever → bytes → player → pixels | Aula 01 |
| ✅ | O documento como produto transportável | Aula 02 |
| ✅ | Por que existe um `RemoteModifier` paralelo | Aula 03 |
| ✅ | Front × Back real, com backend JVM | Aula 04 |
| ✅ | O formato binário por dentro | `docs/05` |
| ✅ | Arquitetura do servidor | `docs/06` |

---

## B. Tela viva — o que falta para uma UI de verdade

São estes quatro que transformam "desenho bonito" em "interface". **É aqui que
eu sugiro continuar.**

### B1. Eventos e ações 🔜

Como o documento reage a toque e conversa de volta com o app hospedeiro.

Opcodes que provam que existe: `CLICK_AREA` (64), `MODIFIER_CLICK` (59),
`MODIFIER_MULTI_CLICK` (83), `MODIFIER_TOUCH_DOWN` (219), `MODIFIER_TOUCH_UP`
(220), `MODIFIER_TOUCH_CANCEL` (225), `HOST_ACTION` (209), `HOST_NAMED_ACTION`
(210), `HOST_METADATA_ACTION` (216), `RUN_ACTION` (236).

Na API de criação: `hostAction`, `combinedAction`, `PendingIntentAction`,
`LambdaAction`, `ScrollAction`. No player: `onNamedAction`.

O detalhe interessante: existe `PendingIntentAction`. Um documento pode disparar
uma *intent* do Android — ou seja, abrir outra tela, outro app. Isso levanta
uma pergunta de segurança óbvia que vale um parágrafo em qualquer artigo sério:
**o que acontece se o documento vier de uma fonte não confiável?**

### B2. Estado remoto 🔜

Valores que vivem **dentro** do documento e podem mudar sem regravá-lo.

Opcodes: `DATA_FLOAT` (80), `DATA_INT` (140), `DATA_BOOLEAN` (143), `DATA_LONG`
(148), `NAMED_VARIABLE` (137), `COMPONENT_VALUE` (150), `UPDATE` (195).

Na API: `RemoteFloat`, `RemoteInt`, `RemoteLong`, `RemoteString`, `RemoteColor`,
`RemoteBoolean`, `RemoteEnum`, `rememberNamedRemoteDp`.

E do lado do player, a peça que muda tudo: o pacote
`androidx.compose.remote.player.core.state` traz `StateUpdater`, `PlayerState`,
`RcFloat`/`RcInt`/`RcString`. Isso significa que **dá para alterar valores de um
documento já carregado, sem baixar outro**. Um contador, um placar, um preço
que atualiza — sem tráfego.

### B3. Expressões 🔜

O ponto em que o documento deixa de descrever e passa a **calcular**.

Opcodes: `INTEGER_EXPRESSION` (144), `COLOR_EXPRESSIONS` (134),
`TOUCH_EXPRESSION` (157), `PATH_EXPRESSION` (193), `MATRIX_EXPRESSION` (187),
`TEXT_FROM_FLOAT` (135), `TEXT_LOOKUP` (151).

`TOUCH_EXPRESSION` é o mais revelador: significa que a **posição do dedo** pode
ser variável de entrada de uma fórmula avaliada pelo player. Um arrastar
suave, sem nenhuma ida ao app.

### B4. Animações 🔜

Opcodes: `ANIMATION_SPEC` (14), `ANIMATED_FLOAT` (81), mais a família de
easing (`EASING_CUBIC_*`, `EASING_EASE_OUT_BOUNCE`, `EASING_EASE_OUT_ELASTIC`)
e `ATTRIBUTE_TIME` (172).

A ideia: o documento diz *"esta opacidade é uma função do relógio"* e o player
avalia a cada quadro. É o que permite **watch faces** — e é a razão de a
tecnologia existir do jeito que existe.

---

## C. Layout e conteúdo

### C1. Layout além de Column 🔬

`LAYOUT_BOX` (202), `LAYOUT_ROW` (203), `LAYOUT_CANVAS` (205), `LAYOUT_FIT_BOX`
(176), `LAYOUT_IMAGE` (234), `LAYOUT_STATE` (217), `LAYOUT_COMPUTE` (238),
`LAYOUT_COLLAPSIBLE_ROW` (230) e `LAYOUT_COLLAPSIBLE_COLUMN` (233).

Os "collapsible" não têm equivalente no Compose comum — são containers que
encolhem conteúdo quando falta espaço. Vale investigar.

### C2. Texto avançado 🔬

`TEXT_MERGE` (136), `TEXT_LOOKUP` (151), `TEXT_MEASURE` (155), `TEXT_LENGTH`
(156), `TEXT_SUBTEXT` (182), `TEXT_TRANSFORM` (199), `MODIFIER_MARQUEE` (228),
`DATA_FONT` (189), `DATA_BITMAP_FONT` (167).

`TEXT_FROM_FLOAT` + `TEXT_MERGE` permitem montar strings **dentro** do
documento: "restam {n} itens" com o `n` calculado no player.

### C3. Imagens 🔬

`LOAD_BITMAP` (4), `DATA_BITMAP` (101), `DRAW_BITMAP_SCALED` (149),
`DRAW_TO_BITMAP` (190), mais `BitmapLoader` no player e
`rememberRemoteImageBitmap(url)` na criação.

Pergunta prática: a imagem viaja **dentro** do documento (inflando os bytes) ou
por URL (exigindo rede do player)? As duas coisas parecem possíveis, e a escolha
muda completamente o dimensionamento de um sistema real.

### C4. Desenho customizado 🔬

`DATA_PATH` (123), `DRAW_PATH` (124), `PATH_CREATE` (159), `PATH_ADD` (160),
`PATH_COMBINE` (175), `DRAW_TWEEN_PATH` (125), `DATA_SHADER` (45), mais
gradientes (`RemoteLinearGradient`, `RemoteRadialGradient`, `RemoteSweepGradient`)
e `RemoteCanvas`.

`DRAW_TWEEN_PATH` é interpolação entre dois caminhos — *morphing* de forma
avaliado no player.

### C5. Temas e cores 🔬

`THEME` (63), `COLOR_THEME` (196), `COLOR_CONSTANT` (138), `ATTRIBUTE_COLOR`
(180), e `getNamedColors()` no `RemoteDocument`.

Isto responde uma pergunta de produto importante: **o mesmo documento consegue
se adaptar a tema claro e escuro?** Se sim, o servidor não precisa saber o tema
do usuário.

---

## D. O território estranho

Aqui estão as capacidades que me fizeram levantar a hipótese de que isto é mais
que um formato de documento.

### D1. Controle de fluxo — a hipótese da máquina virtual 🔬

`CONDITIONAL_OPERATIONS` (178), `LOOP_START` (215), `FUNCTION_DEFINE` (168),
`FUNCTION_CALL` (166), `REM` (185).

Condicional, laço, definição e chamada de função. Esse é vocabulário de
**linguagem de programação**, não de formato de arquivo.

**Aviso honesto:** eu só vi os nomes dos opcodes. Não executei nenhum, e não sei
se a API pública sequer os expõe. Pode ser uma VM completa; podem ser
mecanismos internos. É a investigação de maior potencial e maior incerteza do
roteiro.

### D2. Partículas e impulsos 🔬

`PARTICLE_DEFINE` (161), `PARTICLE_PROCESS` (162), `PARTICLE_LOOP` (163),
`PARTICLE_COMPARE` (194), `IMPULSE_START` (164), `IMPULSE_PROCESS` (165).

Um sistema de partículas dentro do formato de documento. Confeitos caindo,
fogos, efeito de curtida — tudo executado pelo player.

### D3. Som e háptica 🔬

`PLAY_SOUND` (141), `DATA_SOUND` (169), `SOUND_EXPRESSION` (206),
`HAPTIC_FEEDBACK` (177), e `loadSound(int, byte[])` no `CoreDocument`.

**Um documento pode carregar áudio e fazer o aparelho vibrar.** Isso é bem além
do que "server-driven UI" sugere, e é o tipo de detalhe que rende um artigo
sozinho.

### D4. Tempo e sensores 🔬

`ATTRIBUTE_TIME` (172), `WAKE_IN` (191), e `hasSensorListeners(int[])` no
`RemoteDocument`.

`WAKE_IN` sugere que o documento pode pedir para ser reavaliado depois de um
intervalo — de novo, comportamento de programa, não de arquivo.

### D5. Acessibilidade 🔬

`ACCESSIBILITY_SEMANTICS`, `ROOT_CONTENT_DESCRIPTION` (103), e o pacote
`androidx.compose.remote.core.semantics`.

Pergunta séria de produto: uma UI que o app não conhece consegue ser acessível?
Se o TalkBack funciona sobre um documento remoto, é um argumento forte a favor
da tecnologia. Se não funciona, é um impeditivo de adoção.

---

## E. Ecossistema e produção

| Tópico | Do que se trata |
|---|---|
| **Componentes customizados** 🔬 | `ComposeCustomSupport` no player, `LAYOUT_CUSTOM` (93). O documento invoca um componente que **o app** implementa — o meio-termo entre desenho puro e design system. |
| **Preview no Android Studio** 🔬 | `remote-tooling-preview`: `RemoteContentPreview`, `RemoteDocumentPreview`. |
| **Testes** 🔬 | `remote-testing`, sobre `ui-test-junit4`. Como testar UI que não está no seu código? |
| **Perfis e compatibilidade** 🔬 | `Profile`, `getSupportedOperations()`. Servir documento mais simples para player antigo. |
| **O caminho JSON** 🔬 | `RemoteComposeJsonParser` no `remote-creation-core`. Existe JSON → documento oficial, quase sem divulgação. Interessante para CMS. |
| **Cache e CDN** | O documento é imutável e autocontido — perfeito para borda. Ver `docs/06`, Parte 6. |
| **Segurança** | Documento é código executável vindo da rede. Quem pode servir? Como assinar? Ninguém está falando disso ainda. |

---

## F. O objetivo final

### Player próprio, sobre `PaintContext` 🔬

`remote-core` é JAR puro e contém `PaintContext` **abstrato** mais o motor de
layout inteiro. O player do Android é só uma implementação dela sobre `Canvas`
(`AndroidPaintContext`).

Implementar um `PaintContext` sobre o `DrawScope` do Compose daria um player de
Desktop — e o layout viria de graça.

É o caminho mais longo do roteiro (172 operações; cobriríamos um subconjunto) e
o mais alinhado ao seu objetivo de escrever SDUI próprio. Ver `docs/02`.

---

## Como eu ordenaria

Se o objetivo é **entender a tecnologia**: B1 → B2 → B3 → B4. Os quatro juntos
completam o modelo mental, e cada um depende do anterior.

Se o objetivo é **material de artigo**: D3 (som e háptica) e D1 (controle de
fluxo) são os mais surpreendentes e os menos escritos. Nenhum dos dois exige
muito código para demonstrar.

Se o objetivo é **portfólio**: F, o player próprio. É o que separa "estudei uma
biblioteca" de "entendi como se constrói uma".
