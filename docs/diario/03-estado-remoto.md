# Estado remoto: seis hipóteses até achar o `flush()`

> **RESOLVIDO EM PARTE — 03/08/2026.** Era `RcFloat.flush()`. Um método público,
> sem uma linha de documentação, que precisa ser chamado para o valor
> efetivamente ser **escrito no documento**. Sem ele, o `setValue` mirava um
> espaço que não existia no buffer — daí o sintoma perfeito: sem erro, sem
> efeito.
>
> ```kotlin
> val value = RcBridge.floatValue(writer, 1f).flush()
> //                                          ^^^^^^^
> ```
>
> **A pista que confirmou antes de abrir o app:** o documento pulou de 615 para
> **628 bytes**. Treze bytes a mais, só por chamar `flush()`. Alguma coisa
> passou a ser gravada que antes não era. Tamanho de arquivo como sinal — a
> mesma técnica que pegou o `writer.buffer()` devolvendo 1 MiB.
>
> **O que ainda não fechou:** com `flush()`, `setValue` com **constante**
> funciona sempre (o botão "zerar", e o `virar 99` da repro mínima). Mas
> `setValue` com **expressão** (`1f + counter`) aplica **uma vez e congela**:
> o contador vai de 7 para 8 e para. A leitura mais provável é que `flush()`
> avalia a expressão no momento da escrita, transformando `1f + counter` na
> constante 8 — e todo toque re-atribui 8.
>
> Ou seja: o problema deixou de ser "estado remoto não funciona" e virou
> "expressão deixa de ser viva depois do flush". Problema menor e muito mais
> preciso.

O texto abaixo é o registro da investigação, na ordem em que aconteceu. Vale
ler mesmo sabendo o final — o valor está no método, não na resposta.

---


Terceiro capítulo do diário. Os dois primeiros ([montando o
projeto](01-montando-o-projeto.md) e [backend em JVM
pura](02-backend-em-jvm-pura.md)) terminam em vitória. **Este não.**

Ele está aqui exatamente por isso: registra uma investigação **em aberto**, com
seis hipóteses testadas — cinco descartadas com evidência, uma que revelou um
achado real mas insuficiente. É o registro honesto de um problema que ainda não
sei resolver.

O plano do que fazer a seguir vive em [oteiro.md](../roteiro.md), item B2.
Aqui fica só o que aconteceu.

---

## O que se queria

Que o documento guardasse um valor e o alterasse sozinho, sem rede:

```kotlin
val counter = RcBridge.floatValue(writer, 7f).named("USER:contador")
Text(createTextFromFloat(counter, 3, 0, 0))
Box(Modifier.onClick { setValue(counter, 1f + counter) })
```

Isso é o que separa "documento = desenho" de "documento = programa".

---

## O que funcionou

Tentado na Aula 05 (telas `contador` e `relogio`). O que ficou provado e o que
não:

**✅ Valores remotos existem e são exibidos.** `RcBridge.floatValue(writer, 7f)`
mais `createTextFromFloat(valor, 3, 0, 0)` desenha o número corretamente. O
construtor de `RcFloat` é `internal` no Kotlin — mesma ponte Java do
`RcScopeImpl`.

**✅ O player reavalia continuamente.** A tela `relogio` usa `hour()`,
`minutes()` e `seconds()`, e os segundos **avançam em tempo real** (47 → 52 em
cinco segundos, verificado por dois screenshots). Isso prova que expressões
ligadas ao tempo são recalculadas quadro a quadro, sem nenhuma participação do
app. É a base das animações (B4).

**❌ `setValue` não surtiu efeito.** Os botões da tela `contador` usam
`onClick { setValue(contador, 1f + contador) }`. O toque chega (o mesmo padrão
com `hostAction` funciona), mas o valor não muda — nem com aritmética, nem com
constante (`setValue(contador, 0f)`). Isolado assim:

| Teste | Resultado |
|---|---|
| valor inicial 7 aparece na tela | ✅ |
| relógio avança sozinho | ✅ |
| `setValue` com expressão | ❌ |
| `setValue` com constante | ❌ |
| nenhuma requisição HTTP nos toques | ✅ (3 antes, 3 depois) |

**❓ Hora não bate.** O aparelho marcava `04:15:03` e o documento desenhava
`4:54:52`. A hora coincide, minutos e segundos não. Ainda sem explicação.

### O que a investigação apurou depois

**Reprodução mínima construída** (tela `teste-estado` em `StatefulScreens.kt`):
um valor, um botão, sem `named()`, sem aritmética, sem Row nem weight.

```kotlin
val valor = RcBridge.floatValue(writer, 1f)
Text(createTextFromFloat(valor, 3, 0, 0), …)          // mostra 1  ✅
Box(Modifier….onClick { setValue(valor, 99f) })        // toque não muda nada ❌
```

**Continua 1.** Isso elimina `named()` e a aritmética como suspeitos.

**As operações estão sendo gravadas.** Comparando `doc.stats` de um documento
que funciona com o que não funciona:

```
[interativo] 38 ops : … TextData 6 ; BoxLayout 8 ; TextLayout 6
[contador]   44 ops : … TextFromFloat 1 ; NamedVariable 1 ; FloatExpression 2 …
```

`FloatExpression : 2` são exatamente as duas expressões dos botões `+1` e `−1`.
Ou seja, o servidor **escreveu** o que deveria. O problema está na execução.

**A hipótese que sobrou.** Repare no contraste:

| Caminho | Resultado |
|---|---|
| valor mudando pelo **tempo** (`seconds()`) → `TextFromFloat` redesenha | ✅ funciona |
| valor mudando por **toque** (`setValue`) → `TextFromFloat` redesenha | ❌ não funciona |
| toque disparando `hostAction` → app recebe | ✅ funciona |

O `TextFromFloat` reavalia (o relógio prova) e o clique é entregue (o
`hostAction` prova). O que não acontece é a **execução da ação de mudança de
valor dentro do documento**.

Isso aponta para: `remote-player-compose` talvez entregue ações ao hospedeiro
mas não execute mutação de estado interno. É hipótese, não fato.

### O experimento dos dois players (Aula 06) — e a hipótese refutada

Montamos o mesmo documento, com os mesmos bytes, em **três** caminhos
diferentes de mutação:

| Caminho | Resultado |
|---|---|
| botão no documento, executado pelo **player de Compose** | ❌ nada |
| botão no documento, executado pelo **player de View** | ❌ nada |
| `StateUpdater.setUserLocalFloat("contador", 99f)` a partir do **app** | ❌ nada, e sem exceção |

**A hipótese anterior está refutada.** Não é o invólucro de Compose: os dois
players se comportam de forma idêntica, então a diferença entre eles não pode
ser a causa. Registro isso aqui porque uma hipótese descartada com evidência
vale tanto quanto uma confirmada — e porque eu a tinha anunciado com confiança
demais.

**A suspeita que sobra é mais mundana, e provavelmente correta: o valor não
está sendo registrado com o nome/domínio que esses mecanismos procuram.**

A pista está na própria API:

```java
public interface StateUpdater {
    void setUserLocalFloat(String, Float);
    static String getUserDomainString(String);   // ← nomes são qualificados
}
```

A existência de `getUserDomainString` sugere que nomes vivem em **domínios**
(há uma classe `RemoteDomains` em `player-core.state`), e que o `named("contador")`
do lado servidor talvez não caia no domínio `user` que o `setUserLocalFloat`
consulta.

Ou seja: a suspeita passou da biblioteca para o **nosso código**. Menos
glamouroso e mais provável.

### A investigação do domínio — achado real, mas insuficiente

Instrumentamos o app para responder a pergunta direta: *que nomes o documento
registrou, e qual chave o `StateUpdater` procura?*

```kotlin
doc.getNamedVariables(NamedVariable.FLOAT_TYPE)   // o que está no documento
StateUpdater.getUserDomainString("contador")      // o que o updater monta
```

Resposta:

```
nomes float no documento         = contador
getUserDomainString("contador")  = USER:contador
```

**Divergiam.** O documento registrava o nome cru; o `setUserLocalFloat` procurava
por `USER:contador`. Nunca se encontravam — e por isso a chamada não dava erro
e também não fazia nada.

Corrigimos no servidor (`named("USER:contador")`) e confirmamos que os dois
lados passaram a bater. **O valor continuou não mudando.**

Achado legítimo e documentado — os domínios (`RemoteDomains.USER` / `SYSTEM`)
existem e importam. Mas não era a causa raiz.

### O que já está eliminado

| Hipótese | Como foi testada | Veredito |
|---|---|---|
| `named()` ou a aritmética atrapalham | repro mínima sem os dois | ❌ eliminada |
| defeito no invólucro de Compose | mesmo documento no player de View | ❌ eliminada |
| nome/domínio não batem | `getNamedVariables` × `getUserDomainString` | ⚠️ era real, corrigido, **insuficiente** |
| `viewPlayer` ou `stateUpdater` nulos | diagnóstico reporta cada elo | ❌ eliminada — ambos não-nulos, sem exceção |
| a View não repinta | `player.invalidate()` explícito | ❌ eliminada |

Cinco hipóteses queimadas, uma correção real no caminho. O `setValue` e o
`StateUpdater` continuam sem efeito.

### O que ainda não foi testado

1. `RcFloat.flush()` — existe na API pública, e nunca descobrimos o que faz.
   É o candidato mais promissor: o nome sugere que o valor precisa ser
   "descarregado" no documento.
2. `player.updateDocument(doc)` em vez de `setDocument` — talvez o caminho de
   atualização seja outro.
3. Procurar o opcode `VALUE_FLOAT_CHANGE_ACTION` (222) no dump binário.
   `doc.stats` não lista ações, então essa checagem tem que ser byte a byte —
   e responderia em definitivo se a ação chega a ser gravada.

> **Nota de método:** o experimento não deu o resultado que eu esperava, e
> mesmo assim foi o mais produtivo até agora — eliminou uma hipótese inteira
> em minutos e apontou a próxima. Experimento bom não é o que confirma; é o
> que separa.

Contexto original do item:

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

