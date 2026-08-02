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

## Moral

Cinco dos seis problemas acima teriam sido evitados se a documentação estivesse
correta e atualizada. Nenhum deles teria sido evitado sem **rodar a build de
verdade**.

Alpha é assim. Se você quer chegar cedo numa tecnologia, o preço de entrada é
esse — e a recompensa é conhecer os buracos que o resto do mercado só vai
descobrir daqui a um ano.
