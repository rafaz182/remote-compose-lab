package dev.rafaz.remotecomposelab.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Componentes de apoio às aulas.
 *
 * Tudo aqui é Compose COMUM — nada de Remote Compose. É de propósito: manter a
 * casca didática e o objeto de estudo bem separados evita a confusão mais comum
 * de quem começa, que é misturar `Text` com `RemoteText` sem perceber.
 */

/** Bloco de texto explicativo. O "professor falando". */
@Composable
fun Explanation(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = Palette.TextPrimary,
        fontSize = 15.sp,
        lineHeight = 23.sp,
        modifier = modifier.fillMaxWidth(),
    )
}

/** Destaque para uma ideia que vale levar embora. */
@Composable
fun Callout(text: String, color: Color = Palette.Warning, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.10f))
            .padding(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            Modifier
                .padding(top = 6.dp, end = 12.dp)
                .background(color, RoundedCornerShape(2.dp))
                .fillMaxWidth(0f),
        )
        Text(
            text = text,
            color = color,
            fontSize = 14.sp,
            lineHeight = 21.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

/** Título de seção dentro de uma aula. */
@Composable
fun SectionTitle(text: String, color: Color = Palette.TextPrimary, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        color = color,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.4.sp,
        modifier = modifier,
    )
}

/**
 * Trecho de código. Rola na horizontal em vez de quebrar linha — código
 * quebrado em lugar errado engana mais do que ajuda.
 */
@Composable
fun CodeBlock(codigo: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF0B0B0F))
            .border(1.dp, Color(0xFF2A2A36), RoundedCornerShape(10.dp))
            .horizontalScroll(rememberScrollState())
            .padding(14.dp),
    ) {
        Text(
            text = codigo.trimIndent(),
            color = Color(0xFFCFCFE4),
            fontSize = 12.5.sp,
            lineHeight = 19.sp,
            fontFamily = FontFamily.Monospace,
        )
    }
}

/**
 * O "palco": a moldura onde o conteúdo renderizado aparece.
 *
 * A borda tem cor semântica ([Cores.Leitura], ciano) porque tudo que aparece
 * dentro de um palco foi produzido pelo **player** a partir de um documento —
 * nunca é Compose comum.
 */
@Composable
fun Stage(
    label: String,
    modifier: Modifier = Modifier,
    borderColor: Color = Palette.Read,
    content: @Composable () -> Unit,
) {
    Column(modifier.fillMaxWidth()) {
        SectionTitle(label, borderColor)
        Box(
            Modifier
                .padding(top = 8.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Palette.SurfaceHigh)
                .border(1.dp, borderColor.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                .padding(12.dp),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}

/** Uma linha "rótulo → valor", para métricas do documento. */
@Composable
fun MetricRow(label: String, value: String, color: Color = Palette.Bytes) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = Palette.TextMuted, fontSize = 13.sp)
        Text(
            value,
            color = color,
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
        )
    }
}
