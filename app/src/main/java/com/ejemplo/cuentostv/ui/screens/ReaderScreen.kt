@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)

package com.ejemplo.cuentostv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.Text
import com.ejemplo.cuentostv.data.TextScale
import com.ejemplo.cuentostv.ui.CuentosUiState
import com.ejemplo.cuentostv.ui.Narrator
import com.ejemplo.cuentostv.ui.theme.StoryBackground
import com.ejemplo.cuentostv.ui.theme.TvMuted
import com.ejemplo.cuentostv.ui.theme.TvSurface
import com.ejemplo.cuentostv.ui.theme.TvText
import com.ejemplo.cuentostv.ui.theme.accentColor

@Composable
fun ReaderScreen(
    state: CuentosUiState,
    narrator: Narrator,
    onChoose: (String) -> Unit,
    onRestart: () -> Unit,
    onHome: () -> Unit,
    onBack: () -> Unit,
    onToggleNarration: () -> Unit
) {
    val item = state.currentItem ?: return
    val node = state.currentNode
    val firstAction = remember { FocusRequester() }
    val narrationEnabled = state.settings.narrationEnabled
    val largeText = state.settings.textScale == TextScale.LARGE

    LaunchedEffect(state.nodeId, narrationEnabled) {
        if (node != null && narrationEnabled) {
            val ending = node.endingTitle
            narrator.speak(
                if (ending != null) "$ending. ${node.text}" else node.text
            )
        } else {
            narrator.stop()
        }
    }

    LaunchedEffect(state.nodeId) {
        if (node != null) {
            runCatching { firstAction.requestFocus() }
        }
    }

    DisposableEffect(Unit) {
        onDispose { narrator.stop() }
    }

    StoryBackground(theme = state.currentTheme) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 64.dp, end = 64.dp, top = 28.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.summary.title,
                        color = TvText,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (node?.ending == true) "Final del cuento" else "Toma una decisión",
                        color = TvMuted,
                        fontSize = 15.sp
                    )
                }
                Button(onClick = onToggleNarration) {
                    Text(
                        text = if (narrationEnabled) "Voz: Sí" else "Voz: No",
                        fontSize = 17.sp
                    )
                }
                Button(onClick = onBack, modifier = Modifier.padding(start = 12.dp)) {
                    Text("Volver", fontSize = 17.sp)
                }
            }

            if (node == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Cargando cuento...", color = TvText, fontSize = 26.sp)
                }
                return@Column
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 64.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = TvSurface.copy(alpha = 0.78f),
                                shape = RoundedCornerShape(22.dp)
                            )
                            .padding(horizontal = 28.dp, vertical = 22.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (node.ending && node.endingTitle != null) {
                                Text(
                                    text = node.endingTitle,
                                    color = item.summary.theme.accentColor(),
                                    fontSize = if (largeText) 34.sp else 30.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = node.text,
                                color = TvText,
                                fontSize = if (largeText) 32.sp else 26.sp,
                                lineHeight = if (largeText) 42.sp else 36.sp
                            )
                        }
                    }
                }

                if (node.ending) {
                    item {
                        Button(
                            onClick = onRestart,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(58.dp)
                                .focusRequester(firstAction)
                        ) {
                            Text("Volver a empezar", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    item {
                        Button(
                            onClick = onHome,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(58.dp)
                        ) {
                            Text("Elegir otro cuento", fontSize = 20.sp)
                        }
                    }
                } else {
                    itemsIndexed(node.choices) { index, choice ->
                        Button(
                            onClick = { onChoose(choice.next) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(62.dp)
                                .then(
                                    if (index == 0) Modifier.focusRequester(firstAction) else Modifier
                                )
                        ) {
                            Text(
                                text = "${index + 1}. ${choice.text}",
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                item {
                    Text(
                        text = "Narración: ${if (narrationEnabled) "activada" else "desactivada"} · " +
                            "Texto: ${if (largeText) "grande" else "normal"}",
                        color = TvMuted,
                        fontSize = 14.sp
                    )
                    if (narrationEnabled && !narrator.isAvailable) {
                        Text(
                            text = "Este televisor no tiene una voz en español instalada.",
                            color = TvMuted,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}
