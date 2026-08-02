# Operando o emulador pela linha de comando

Durante todo este projeto o app foi instalado, aberto, tocado e fotografado
**sem ninguém encostar no mouse**. Isso não é truque de ferramenta especial —
é só `adb`, que já vem no Android SDK que você tem instalado.

Vale aprender por três motivos práticos: dá para verificar uma mudança sem
interromper o que você está fazendo, dá para reproduzir um bug exatamente igual
dez vezes seguidas, e dá para automatizar num script o roteiro chato de "abrir
o app, ir na terceira tela, tocar no botão".

Todos os comandos abaixo foram usados de verdade neste repositório.

---

## 0. Onde fica o `adb`

```powershell
$ADB = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $ADB devices
```

Se listar `emulator-5554   device`, está tudo certo. Se aparecer `offline` ou
lista vazia, o emulador não terminou de subir.

> Daqui em diante, `$ADB` é essa variável.

---

## 1. Ligar o emulador e **esperar de verdade**

O erro clássico é achar que o emulador está pronto quando a janela aparece. Não
está: o Android ainda está bootando por um bom tempo depois disso.

```powershell
$SDK = "$env:LOCALAPPDATA\Android\Sdk"

& "$SDK\emulator\emulator.exe" -list-avds          # quais existem
Start-Process "$SDK\emulator\emulator.exe" -ArgumentList "-avd","Pixel_9" -WindowStyle Minimized
```

A espera correta consulta uma propriedade do sistema:

```powershell
do { Start-Sleep 3 } until ((& $ADB shell getprop sys.boot_completed).Trim() -eq "1")
Write-Host "pronto"
```

`sys.boot_completed` só vira `1` quando o sistema terminou de subir. É o mesmo
sinal que o Android Studio usa.

---

## 2. Instalar e abrir

```powershell
# instalar (o -r reinstala por cima, preservando os dados)
& $ADB install -r app\build\outputs\apk\debug\app-debug.apk

# abrir uma Activity específica
& $ADB shell am start -n "dev.rafaz.remotecomposelab/.MainActivity"

# fechar o app (útil para testar do zero)
& $ADB shell am force-stop dev.rafaz.remotecomposelab

# desinstalar
& $ADB uninstall dev.rafaz.remotecomposelab
```

Na prática o `.\gradlew.bat :app:installDebug` já faz o `install` por você — o
comando manual serve quando você quer instalar um APK que já existe.

---

## 3. Ver a tela

```powershell
& $ADB exec-out screencap -p > tela.png
```

`exec-out` é o detalhe importante: ele manda os bytes **crus** para a saída.
O antigo `adb shell screencap -p > x.png` corrompia o PNG no Windows, porque o
shell convertia quebras de linha no meio do binário.

Gravar vídeo também dá:

```powershell
& $ADB shell screenrecord --time-limit 10 /sdcard/v.mp4
& $ADB pull /sdcard/v.mp4 .
```

---

## 4. Tocar, deslizar, digitar

```powershell
& $ADB shell input tap 538 1099                  # toque em x=538 y=1099
& $ADB shell input swipe 540 1600 540 600 350    # deslizar (x1 y1 x2 y2 duração_ms)
& $ADB shell input text "ola"                    # digitar
& $ADB shell input keyevent KEYCODE_BACK         # botão voltar
& $ADB shell input keyevent KEYCODE_HOME         # home
```

### O problema das coordenadas

Coordenadas são em **pixels reais do aparelho**. Um Pixel 9 tem 1080×2424.

Quando você tira um screenshot e olha numa ferramenta que o redimensiona, o que
você mede **não** é o que o `adb` espera. Se a imagem foi exibida a 891 de
largura e o aparelho tem 1080, o fator é `1080 / 891 ≈ 1,21`:

```
x_real = x_medido × 1,21
y_real = y_medido × 1,21
```

Errar esse fator é a causa nº 1 de "toquei e não aconteceu nada".

Para descobrir a resolução:

```powershell
& $ADB shell wm size        # Physical size: 1080x2424
& $ADB shell wm density     # Physical density: 420
```

### Duas armadilhas que me pegaram neste projeto

**Deslizar perto da borda de baixo vira gesto do sistema.** Um
`input swipe 540 2350 540 1400` não rolou a tela — o Android interpretou como o
gesto de "ir para a home" e fechou o app. Comece o deslize bem longe da borda
inferior.

**Componentes clicáveis engolem o deslize.** Deslizar em cima de um cartão
clicável às vezes não rola nada. A saída é deslizar por uma faixa neutra — a
margem esquerda funciona bem:

```powershell
& $ADB shell input swipe 60 1800 60 700 350
```

---

## 5. Ver o que o app está dizendo

```powershell
& $ADB logcat -c                                 # limpa o histórico
# ... faça a ação ...
& $ADB logcat -d -s RemoteComposeLab             # só a nossa tag
& $ADB logcat -d -v brief *:E                    # só erros do sistema todo
```

O fluxo `-c` (limpar) → agir → `-d` (despejar) é o que torna o log legível: sem
limpar antes, você recebe milhares de linhas antigas.

Foi exatamente assim que descobrimos que um documento vinha `0x0`:

```kotlin
android.util.Log.i("RemoteComposeLab", "doc ${doc.width}x${doc.height} | stats=${doc.stats...}")
```

```powershell
& $ADB logcat -d -s RemoteComposeLab
# I RemoteComposeLab: doc 0x0 | stats=... RootLayoutComponent : 2 ...
```

---

## 6. Coisas do aparelho

```powershell
& $ADB shell getprop ro.build.version.sdk        # nível de API
& $ADB shell date "+%H:%M:%S"                    # relógio do aparelho
& $ADB shell getprop persist.sys.timezone        # fuso
```

O relógio foi útil aqui: comparamos a hora do aparelho com a hora que o
documento Remote Compose desenhava, e descobrimos que elas **não batem** — uma
questão que ficou em aberto no roteiro.

---

## 7. Rede: o emulador é outra máquina

Este é o conceito que mais confunde. O emulador é uma máquina virtual com a
própria pilha de rede.

```
  localhost (dentro do emulador)  ->  o próprio emulador
  10.0.2.2                        ->  o SEU computador
```

Por isso o app usa `http://10.0.2.2:8080`, e por isso o servidor precisa ouvir
em `0.0.0.0` e não em `localhost`.

Em **aparelho físico** nada disso vale: use o IP da sua máquina na rede local
(`ipconfig`), e libere a porta no firewall.

Alternativa que evita o assunto todo — redirecionar a porta:

```powershell
& $ADB reverse tcp:8080 tcp:8080     # a porta 8080 do emulador vira a sua
```

Com isso o app poderia usar `localhost:8080` normalmente. Não usamos aqui de
propósito: `10.0.2.2` deixa a topologia visível, e o objetivo é didático.

---

## 8. Juntando tudo: um roteiro completo

Este é o formato que usei o projeto inteiro — instalar, abrir, navegar, tocar,
fotografar e checar erros, tudo de uma vez:

```powershell
$ADB = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"

.\gradlew.bat :app:installDebug

& $ADB logcat -c
& $ADB shell am start -n "dev.rafaz.remotecomposelab/.MainActivity"
Start-Sleep 5

& $ADB shell input tap 538 1880        # abre a Aula 05
Start-Sleep 3
& $ADB shell input tap 538 975         # "Carregar catálogo"
Start-Sleep 4

& $ADB exec-out screencap -p > passo1.png

& $ADB logcat -d -v brief *:E | Select-String "AndroidRuntime|FATAL"
```

Os `Start-Sleep` são feios mas necessários: `adb` não espera a UI assentar. Se
um passo falhar de forma intermitente, aumente a pausa antes dele.

---

## Quando isto não basta

Para testes de verdade — que rodam no CI e não dependem de coordenada fixa —
o certo é Espresso ou Compose UI Test, que localizam elementos por texto e
semântica em vez de pixel.

O que está nesta página é outra coisa: **ferramenta de inspeção durante o
desenvolvimento**. Serve para olhar rápido, reproduzir um bug e tirar print de
documentação. Para isso, é imbatível de simples.
