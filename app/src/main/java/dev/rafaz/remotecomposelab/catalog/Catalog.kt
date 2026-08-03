package dev.rafaz.remotecomposelab.Catalog

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
import dev.rafaz.remotecomposelab.ui.Palette

/**
 * Navegação do laboratório.
 *
 * De propósito NÃO usamos uma biblioteca de navegação: são duas telas, e um
 * `mutableStateOf` resolve. Menos cerimônia = menos ruído entre você e o
 * assunto de verdade, que é o Remote Compose.
 */
@Composable
fun Catalog(modifier: Modifier = Modifier) {
    var openLesson by remember { mutableStateOf<Lesson?>(null) }

    // Botão "voltar" do Android fecha a aula em vez de fechar o app.
    BackHandler(enabled = openLesson != null) { openLesson = null }

    val lesson = openLesson
    if (lesson == null) {
        LessonList(modifier, onOpen = { openLesson = it })
    } else {
        LessonScreen(lesson, modifier, onBack = { openLesson = null })
    }
}

@Composable
private fun LessonList(modifier: Modifier = Modifier, onOpen: (Lesson) -> Unit) {
    LazyColumn(
        modifier = modifier.fillMaxSize().background(Palette.Background),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(Modifier.padding(bottom = 10.dp)) {
                Text(
                    "Remote Compose Lab",
                    color = Palette.TextPrimary,
                    fontSize = 27.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "androidx.compose.remote · 1.0.0-alpha16",
                    color = Palette.Write,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Text(
                    "Um laboratório para entender o Server-Driven UI oficial do " +
                        "Google — não pelo \"como usar\", mas pelo que ele realmente é " +
                        "por dentro.",
                    color = Palette.TextMuted,
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }

        items(LESSONS) { Lesson ->
            LessonCard(Lesson) { onOpen(Lesson) }
        }

        item {
            Text(
                "Mais aulas a caminho — combinamos o rumo juntos.",
                color = Palette.TextMuted,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 14.dp),
            )
        }
    }
}

@Composable
private fun LessonCard(Lesson: Lesson, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Palette.Surface)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Palette.Write.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "%02d".format(Lesson.number),
                color = Palette.Write,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
            )
        }
        Column(Modifier.padding(start = 14.dp)) {
            Text(Lesson.title, color = Palette.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(
                Lesson.summary,
                color = Palette.TextMuted,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
    }
}

@Composable
private fun LessonScreen(Lesson: Lesson, modifier: Modifier = Modifier, onBack: () -> Unit) {
    Column(modifier.fillMaxSize().background(Palette.Background)) {
        Row(
            Modifier.fillMaxWidth().padding(start = 4.dp, top = 8.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", tint = Palette.TextPrimary)
            }
            Column {
                Text(
                    "AULA %02d".format(Lesson.number),
                    color = Palette.Write,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.4.sp,
                )
                Text(Lesson.title, color = Palette.TextPrimary, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            }
        }

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Lesson.content()
            androidx.compose.foundation.layout.Spacer(Modifier.size(40.dp))
        }
    }
}
