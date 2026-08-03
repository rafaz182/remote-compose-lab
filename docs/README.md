# Documentação — por onde começar

Estes documentos têm **quatro naturezas diferentes**, e misturá-las foi o que
tornou a leitura confusa antes. Cada pasta é um jeito de ler:

| Pasta | Para quê | Como se lê |
|---|---|---|
| [`trilha/`](trilha/) | **Aprender** os conceitos | em ordem, uma vez |
| [`referencia/`](referencia/) | **Consultar** quando travar | fora de ordem, aos pedaços |
| [`diario/`](diario/) | **Acompanhar** o que aconteceu | cronológico, inclui o que deu errado |
| [`roteiro.md`](roteiro.md) | **Planejar** o que vem depois | painel, muda sempre |

---

## Se é a sua primeira vez aqui

Leia nesta ordem. São três documentos, umas duas horas.

**1 · [A ideia](trilha/01-a-ideia.md)** — por que um formato binário e não JSON.

Responde a pergunta que todo mundo faz primeiro: *"isso não é só Server-Driven
UI com outro nome?"*. Não é, e a diferença é categórica — o servidor não manda
uma descrição da tela, manda as operações de desenho. Comece por aqui mesmo que
você já conheça SDUI.

**2 · [O formato por dentro](trilha/02-o-formato-por-dentro.md)** — dissecando os bytes.

Ensina a ler um dump hexadecimal do zero (hex, big-endian, IEEE 754, strings com
prefixo) e depois decodifica um documento inteiro, byte a byte. É o documento
que tira o formato do lugar de "caixa-preta mágica".

**3 · [O servidor](trilha/03-o-servidor.md)** — Ktor do zero e a fronteira.

Ensina Ktor para quem não conhece, e mostra onde acaba o backend e começa o
motor de documentos — os dois se encontram em **uma linha de código**.

Depois disso, rode o app (`.\gradlew.bat :app:installDebug`) e siga as aulas.
O código das aulas é a continuação natural da trilha.

---

## Quando você travar

| Travou em… | Vá para |
|---|---|
| "que classe é essa? o que ela faz?" | [`referencia/classes-do-sdk.md`](referencia/classes-do-sdk.md) |
| "qual artefato preciso? por que não tem iOS?" | [`referencia/artefatos.md`](referencia/artefatos.md) |
| "como instalo/toco/tiro print sem o mouse?" | [`referencia/operando-o-emulador.md`](referencia/operando-o-emulador.md) |
| "de onde veio essa afirmação?" | [`referencia/fontes.md`](referencia/fontes.md) |

O mais usado é o **classes-do-sdk** — a documentação gerada da biblioteca é
escassa, e ali cada classe tem o que faz, com quem conversa e qual é a
pegadinha. Se for ler só uma seção, leia a *tabela de decisão*: existem três
`Modifier` diferentes convivendo neste projeto.

---

## O diário

Registro cronológico do que foi tentado — **inclusive o que não funcionou**.

**[1 · Montando o projeto](diario/01-montando-o-projeto.md)** — seis erros de
build, do AGP 9 ao `minSdk` que a documentação oficial erra. Curto.

**[2 · Backend em JVM pura](diario/02-backend-em-jvm-pura.md)** — cinco
problemas, todos com o mesmo sintoma: tela em branco, HTTP 200, log limpo.
É um estudo de caso sobre **depurar quando o sistema não reclama de nada**, e
provavelmente o melhor conteúdo daqui. Termina em vitória.

**[3 · Estado remoto](diario/03-estado-remoto.md)** — seis hipóteses, cinco
descartadas, um achado real e insuficiente. **Não termina em vitória** — é uma
investigação em aberto, e está aqui por isso.

> Se você quiser entender *como se investiga* uma biblioteca sem documentação,
> o diário vale mais que a trilha. A trilha ensina o que a tecnologia é; o
> diário ensina o que fazer quando ela não colabora.

---

## O que vem depois

**[`roteiro.md`](roteiro.md)** — o mapa do que ainda dá para aprender, derivado
das **172 operações** extraídas por reflexão do formato. Cada item aponta os
opcodes que provam que a capacidade existe no binário, e está marcado se o
comportamento foi verificado ou se é só o nome do opcode.

É de lá que sai a resposta para "e agora, o que eu estudo?".

---

## Uma nota sobre confiabilidade

Nem tudo aqui tem o mesmo peso, e vale saber distinguir:

- **Verificado rodando** — tem screenshot, medição ou saída de comando junto.
- **Verificado no artefato** — veio de `javap`, POM ou manifesto. É fato sobre
  o binário, não sobre o comportamento.
- **Hipótese** — está marcado como tal. Já erramos uma feio e o registro do erro
  ficou (ver o diário 3).

Quando documentação oficial e artefato discordarem, o artefato ganha. Foi assim
que descobrimos que o `minSdk` real é 29, e não 23.
