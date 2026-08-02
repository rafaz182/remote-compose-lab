package dev.rafaz.remotecomposelab.catalogo

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.rafaz.remotecomposelab.ui.Cores

/**
 * Navegação do laboratório.
 *
 * De propósito NÃO usamos uma biblioteca de navegação: são duas telas, e um
 * `mutableStateOf` resolve. Menos cerimônia = menos ruído entre você e o
 * assunto de verdade, que é o Remote Compose.
 */
@Composable
fun Catalogo(modifier: Modifier = Modifier) {
    var aulaAberta by remember { mutableStateOf<Licao?>(null) }

    // Botão "voltar" do Android fecha a aula em vez de fechar o app.
    BackHandler(enabled = aulaAberta != null) { aulaAberta = null }

    val aula = aulaAberta
    if (aula == null) {
        ListaDeAulas(modifier, onAbrir = { aulaAberta = it })
    } else {
        TelaDaAula(aula, modifier, onVoltar = { aulaAberta = null })
    }
}

@Composable
private fun ListaDeAulas(modifier: Modifier = Modifier, onAbrir: (Licao) -> Unit) {
    LazyColumn(
        modifier = modifier.fillMaxSize().background(Cores.Fundo),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(Modifier.padding(bottom = 10.dp)) {
                Text(
                    "Remote Compose Lab",
                    color = Cores.Texto,
                    fontSize = 27.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "androidx.compose.remote · 1.0.0-alpha16",
                    color = Cores.Escrita,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Text(
                    "Um laboratório para entender o Server-Driven UI oficial do " +
                        "Google — não pelo \"como usar\", mas pelo que ele realmente é " +
                        "por dentro.",
                    color = Cores.TextoFraco,
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }

        items(LICOES) { licao ->
            CartaoDaAula(licao) { onAbrir(licao) }
        }

        item {
            Text(
                "Mais aulas a caminho — combinamos o rumo juntos.",
                color = Cores.TextoFraco,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 14.dp),
            )
        }
    }
}

@Composable
private fun CartaoDaAula(licao: Licao, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Cores.Superficie)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Cores.Escrita.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "%02d".format(licao.numero),
                color = Cores.Escrita,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
            )
        }
        Column(Modifier.padding(start = 14.dp)) {
            Text(licao.titulo, color = Cores.Texto, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(
                licao.resumo,
                color = Cores.TextoFraco,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
    }
}

@Composable
private fun TelaDaAula(licao: Licao, modifier: Modifier = Modifier, onVoltar: () -> Unit) {
    Column(modifier.fillMaxSize().background(Cores.Fundo)) {
        Row(
            Modifier.fillMaxWidth().padding(start = 4.dp, top = 8.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onVoltar) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", tint = Cores.Texto)
            }
            Column {
                Text(
                    "AULA %02d".format(licao.numero),
                    color = Cores.Escrita,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.4.sp,
                )
                Text(licao.titulo, color = Cores.Texto, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            }
        }

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            licao.conteudo()
            androidx.compose.foundation.layout.Spacer(Modifier.size(40.dp))
        }
    }
}
