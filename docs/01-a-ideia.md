# A ideia: por que um formato binário, e não JSON?

## O problema que todo Server-Driven UI tenta resolver

Você quer mudar a interface do app sem passar pela loja. Trocar um banner,
testar um layout, corrigir um texto, ligar uma promoção. O ciclo normal —
codar, buildar, subir, esperar review, esperar o usuário atualizar — leva dias
ou semanas.

A resposta clássica da indústria é mandar **JSON**:

```json
{ "type": "button", "label": "Comprar", "style": "primary" }
```

E funciona. Mas repare no que está acontecendo de verdade: esse JSON não
descreve uma interface. Ele descreve um **pedido** para uma interface que já
existe dentro do app. O `"type": "button"` só vira um botão porque alguém, em
algum momento, escreveu no app:

```kotlin
"button" -> MeuBotao(label = node.label, estilo = node.style)
```

Ou seja: **o servidor só consegue pedir o que o app já sabe fazer**. Isso tem
três consequências que doem com o tempo:

1. **Todo componente novo exige release.** Quer um carrossel? Não adianta mandar
   `"type": "carousel"` — o app não conhece. Volta pra loja.
2. **O contrato tem que ser combinado dos dois lados.** Cada campo novo é uma
   negociação entre time de backend e time de mobile, e um risco de versão
   antiga quebrar.
3. **Cada plataforma reimplementa tudo.** O renderizador de Android e o de iOS
   precisam interpretar o mesmo JSON com o mesmo resultado visual — e nunca
   ficam realmente iguais.

## A aposta diferente do Remote Compose

Remote Compose não manda uma descrição. Ele manda as **operações de desenho e
layout**.

O documento não diz "aqui vai um botão". Ele diz, em bytes, algo mais parecido
com:

```
DesenheRetânguloArredondado(raio=12, cor=#6750A4)
DesenheTexto("Comprar", ancorado ao centro, tamanho=16)
ÁreaDeClique(id=42)
```

A diferença é categórica. O player **não precisa conhecer o seu design system**,
porque não existe "botão" para ele conhecer — existe forma, texto e área de
clique. É a mesma jogada conceitual do PDF: um leitor de PDF não sabe o que é
"nota fiscal" nem "currículo", ele sabe desenhar.

Isso destrava o item 1 lá de cima: um componente que o app nunca viu funciona,
porque ele não chega como conceito, chega como desenho.

## Então é só um formato de desenho?

Não — e é aqui que fica interessante.

Se fosse só desenho, o documento seria uma imagem glorificada: rígido,
incapaz de se adaptar à tela, incapaz de reagir a toque. Remote Compose vai
além em três pontos:

**1. Tem motor de layout.** O documento carrega `Column`, `Row`, `Box`,
arranjos e alinhamentos de verdade. O cálculo de posição acontece **no player**,
com a largura real do dispositivo dele. Por isso `RemoteDp` existe: o documento
carrega a *fórmula*, não o resultado.

**2. Tem expressões.** Valores dentro do documento podem ser contas —
inclusive contas sobre o tempo. É o que permite animação sem o app mandar
quadro por quadro: o documento diz "esta opacidade é uma função do relógio", e
o player avalia isso a cada frame.

**3. Tem estado e ações.** Áreas clicáveis, mudanças de valor, ações nomeadas
que o app hospedeiro pode capturar. O documento é interativo por si só.

Ou seja: é menos "imagem" e mais **bytecode de interface**. Um programinha que
descreve como se desenhar e como reagir.

## O preço

Nada disso é de graça, e vale enxergar os custos com clareza:

- **É opaco.** Um JSON você abre num editor e entende. Um documento binário
  exige ferramenta. Depurar é mais difícil.
- **É alpha.** A API muda entre versões, e muda de verdade.
- **É Android-only, hoje.** Ver [`02-anatomia-dos-artefatos.md`](02-anatomia-dos-artefatos.md).
- **É mais abstrato.** Você tem que aprender um segundo conjunto de tipos
  (`RemoteModifier`, `RemoteDp`, `RemoteFloat`) e entender por que ele existe.

## Por que isso importa para quem quer fazer SDUI próprio

Mesmo que você nunca use Remote Compose em produção, a arquitetura dele é uma
aula:

- separar **escrita** de **leitura** com um artefato serializável no meio;
- mandar **operações**, não conceitos, para não acoplar servidor e app;
- deixar o **layout ser resolvido no destino**, com fórmulas em vez de valores;
- expressar **animação como função do tempo**, não como sequência de estados.

São quatro decisões que qualquer SDUI sério acaba tendo que tomar. Aqui elas
estão tomadas por gente que projeta framework de UI para viver — dá para
aprender de graça com as escolhas deles.
