@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)

package com.ejemplo.cuentostv.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.Text
import com.ejemplo.cuentostv.data.StoryItem
import com.ejemplo.cuentostv.ui.CuentosUiState
import com.ejemplo.cuentostv.ui.components.StoryImage
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
    loadImage: suspend (StoryItem, String) -> Bitmap?,
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
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 56.dp, vertical = 36.dp),
            horizontalArrangement = Arrangement.spacedBy(36.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = if (item.remote) "Cuento en línea" else "Cuento local",
                    color = item.summary.theme.accentColor(),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = item.summary.title,
                    color = TvText,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.summary.age,
                        color = TvAccent,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "  ·  ${state.story?.nodes?.size ?: 0} escenas",
                        color = TvMuted,
                        fontSize = 16.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = TvSurface.copy(alpha = 0.72f),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 22.dp, vertical = 16.dp)
                ) {
                    Text(
                        text = item.summary.description,
                        color = TvText,
                        fontSize = 19.sp,
                        lineHeight = 26.sp
                    )
                }

                if (!storyReady) {
                    Text(
                        text = if (state.isLoading) "Cargando cuento..." else "No se pudo cargar el cuento.",
                        color = TvMuted,
                        fontSize = 18.sp
                    )
                }

                Spacer(Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    if (hasProgress && storyReady) {
                        Button(
                            onClick = onStart,
                            modifier = Modifier
                                .height(56.dp)
                                .focusRequester(firstAction)
                        ) {
                            Text("Continuar", fontSize = 19.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = onRestart,
                        modifier = Modifier
                            .height(56.dp)
                            .then(
                                if (!hasProgress) Modifier.focusRequester(firstAction) else Modifier
                            ),
                        enabled = storyReady
                    ) {
                        Text(
                            text = if (hasProgress) "Empezar de nuevo" else "Empezar",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = onBack,
                        modifier = Modifier.height(56.dp)
                    ) {
                        Text("Volver", fontSize = 18.sp)
                    }
                }

                Text(
                    text = "Usa las flechas del control remoto para moverte y Enter para elegir.",
                    color = TvMuted,
                    fontSize = 14.sp
                )
            }

            Box(
                modifier = Modifier
                    .width(360.dp)
                    .height(203.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Brush.verticalGradient(item.summary.theme.gradientColors()))
            ) {
                StoryImage(
                    item = item,
                    name = item.summary.cover,
                    loadImage = loadImage,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                ) {
                    Box(Modifier.fillMaxSize())
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color(0x4405030F))
                            )
                        )
                )
            }
        }
    }
}
