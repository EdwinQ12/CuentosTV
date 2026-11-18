@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)

package com.ejemplo.cuentostv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.Card
import androidx.tv.material3.Text
import com.ejemplo.cuentostv.data.StoryItem
import com.ejemplo.cuentostv.data.StoryTheme
import com.ejemplo.cuentostv.ui.CuentosUiState
import com.ejemplo.cuentostv.ui.theme.StoryBackground
import com.ejemplo.cuentostv.ui.theme.TvAccent
import com.ejemplo.cuentostv.ui.theme.TvBackground
import com.ejemplo.cuentostv.ui.theme.TvMuted
import com.ejemplo.cuentostv.ui.theme.TvSurface
import com.ejemplo.cuentostv.ui.theme.TvText
import com.ejemplo.cuentostv.ui.theme.accentColor
import com.ejemplo.cuentostv.ui.theme.gradientColors

@Composable
fun HomeScreen(
    state: CuentosUiState,
    onStory: (String) -> Unit,
    onContinue: () -> Unit,
    onRefresh: () -> Unit,
    onSettings: () -> Unit
) {
    val firstFocus = remember { FocusRequester() }
    val continueItem = state.stories.firstOrNull { item ->
        item.summary.id == state.lastStoryId && state.progress[item.summary.id] != null
    }

    LaunchedEffect(state.isLoading, state.stories, continueItem) {
        if (!state.isLoading && state.stories.isNotEmpty()) {
            runCatching { firstFocus.requestFocus() }
        }
    }

    StoryBackground(theme = StoryTheme.SPACE) {
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Cargando cuentos...", color = TvText, fontSize = 28.sp)
            }
            return@StoryBackground
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 48.dp, vertical = 26.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "CuentosTV",
                            color = TvText,
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Historias que se eligen con el control remoto",
                            color = TvMuted,
                            fontSize = 17.sp
                        )
                    }
                    Button(onClick = onSettings) {
                        Text("Ajustes", fontSize = 17.sp)
                    }
                }
            }

            if (continueItem != null) {
                item {
                    ContinueCard(
                        item = continueItem,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(112.dp)
                            .focusRequester(firstFocus),
                        onClick = onContinue
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Biblioteca",
                            color = TvText,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (state.remoteAvailable) {
                                "${state.stories.size} cuentos · local y en línea"
                            } else {
                                "${state.stories.size} cuentos locales"
                            },
                            color = TvMuted,
                            fontSize = 15.sp
                        )
                    }
                    Button(
                        onClick = onRefresh,
                        enabled = !state.isRefreshing
                    ) {
                        Text(
                            text = if (state.isRefreshing) "Actualizando..." else "Actualizar cuentos",
                            fontSize = 15.sp
                        )
                    }
                }
            }

            if (state.stories.isEmpty()) {
                item {
                    Text(
                        text = "No hay cuentos disponibles. Revisa los archivos locales.",
                        color = TvMuted,
                        fontSize = 24.sp
                    )
                }
            } else {
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        itemsIndexed(
                            items = state.stories,
                            key = { _, item -> item.summary.id }
                        ) { index, item ->
                            StoryCard(
                                item = item,
                                hasProgress = state.progress[item.summary.id] != null,
                                modifier = Modifier
                                    .width(280.dp)
                                    .height(156.dp)
                                    .then(
                                        if (index == 0 && continueItem == null) {
                                            Modifier.focusRequester(firstFocus)
                                        } else {
                                            Modifier
                                        }
                                    ),
                                onClick = { onStory(item.summary.id) }
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Elige un cuento, toma decisiones y descubre un final. " +
                        "Si activas la narración, la TV leerá la historia en voz alta.",
                    color = TvMuted,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun ContinueCard(
    item: StoryItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(TvSurface, item.summary.theme.gradientColors().first())
                    )
                )
                .padding(22.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Continuar leyendo",
                        color = item.summary.theme.accentColor(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = item.summary.title,
                        color = TvText,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Sigue la historia desde donde la dejaste",
                        color = TvMuted,
                        fontSize = 15.sp
                    )
                }
                Text(
                    text = "→",
                    color = TvAccent,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun StoryCard(
    item: StoryItem,
    hasProgress: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(item.summary.theme.gradientColors()))
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.summary.age,
                        color = item.summary.theme.accentColor(),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.weight(1f))
                    if (item.remote) {
                        Text(
                            text = "En línea",
                            color = TvAccent,
                            fontSize = 12.sp
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = item.summary.title,
                    color = TvText,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (hasProgress) "Continuar" else item.summary.description,
                    color = TvMuted,
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
