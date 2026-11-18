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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.Text
import com.ejemplo.cuentostv.ui.CuentosUiState
import com.ejemplo.cuentostv.ui.theme.StoryBackground
import com.ejemplo.cuentostv.ui.theme.TvAccent
import com.ejemplo.cuentostv.ui.theme.TvMuted
import com.ejemplo.cuentostv.ui.theme.TvSurface
import com.ejemplo.cuentostv.ui.theme.TvText
import com.ejemplo.cuentostv.ui.theme.accentColor
import com.ejemplo.cuentostv.ui.theme.gradientColors

@Composable
fun DetailScreen(
    state: CuentosUiState,
    onStart: () -> Unit,
    onRestart: () -> Unit,
    onBack: () -> Unit
) {
    val item = state.currentItem ?: return
    val firstAction = remember { FocusRequester() }
    val hasProgress = state.progress[item.summary.id] != null
    val storyReady = state.story != null

    LaunchedEffect(storyReady) {
        if (storyReady) runCatching { firstAction.requestFocus() }
    }

    StoryBackground(theme = item.summary.theme) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 64.dp, vertical = 44.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            item {
                Text(
                    text = if (item.remote) "Cuento en línea" else "Cuento local",
                    color = item.summary.theme.accentColor(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = item.summary.title,
                    color = TvText,
                    fontSize = 50.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.summary.age,
                        color = TvAccent,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "  ·  ${state.story?.nodes?.size ?: 0} escenas",
                        color = TvMuted,
                        fontSize = 18.sp
                    )
                }
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = TvSurface.copy(alpha = 0.72f),
                            shape = RoundedCornerShape(22.dp)
                        )
                        .padding(28.dp)
                ) {
                    Text(
                        text = item.summary.description,
                        color = TvText,
                        fontSize = 23.sp,
                        lineHeight = 32.sp
                    )
                }
            }

            if (!storyReady) {
                item {
                    Text(
                        text = if (state.isLoading) "Cargando cuento..." else "No se pudo cargar el cuento.",
                        color = TvMuted,
                        fontSize = 22.sp
                    )
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (hasProgress && storyReady) {
                        Button(
                            onClick = onStart,
                            modifier = Modifier
                                .height(64.dp)
                                .focusRequester(firstAction)
                        ) {
                            Text("Continuar", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = onRestart,
                        modifier = Modifier
                            .height(64.dp)
                            .then(
                                if (!hasProgress) Modifier.focusRequester(firstAction) else Modifier
                            ),
                        enabled = storyReady
                    ) {
                        Text(
                            text = if (hasProgress) "Empezar de nuevo" else "Empezar",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = onBack,
                        modifier = Modifier.height(64.dp)
                    ) {
                        Text("Volver", fontSize = 20.sp)
                    }
                }
            }

            item {
                Text(
                    text = "Usa las flechas del control remoto para moverte y Enter para elegir.",
                    color = TvMuted,
                    fontSize = 17.sp
                )
            }
        }
    }
}
