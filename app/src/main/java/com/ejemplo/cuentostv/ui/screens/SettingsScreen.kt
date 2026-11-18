@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)

package com.ejemplo.cuentostv.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.Text
import com.ejemplo.cuentostv.data.AppSettings
import com.ejemplo.cuentostv.data.StoryTheme
import com.ejemplo.cuentostv.data.TextScale
import com.ejemplo.cuentostv.ui.theme.StoryBackground
import com.ejemplo.cuentostv.ui.theme.TvMuted
import com.ejemplo.cuentostv.ui.theme.TvText

@Composable
fun SettingsScreen(
    settings: AppSettings,
    voiceLabel: String,
    onToggleNarration: () -> Unit,
    onOpenVoices: () -> Unit,
    onCycleRate: () -> Unit,
    onTestVoice: () -> Unit,
    onToggleTextScale: () -> Unit,
    onBack: () -> Unit
) {
    val firstFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        runCatching { firstFocus.requestFocus() }
    }

    StoryBackground(theme = StoryTheme.CASTLE) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 64.dp, vertical = 34.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Ajustes",
                    color = TvText,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Personaliza la lectura y la voz de los cuentos.",
                    color = TvMuted,
                    fontSize = 17.sp
                )
            }

            item {
                Button(
                    onClick = onToggleNarration,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .focusRequester(firstFocus)
                ) {
                    Text(
                        text = "Narración: ${if (settings.narrationEnabled) "Activada" else "Desactivada"}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            item {
                Button(
                    onClick = onOpenVoices,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    Text(
                        text = "Voz: $voiceLabel",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            item {
                Button(
                    onClick = onCycleRate,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    Text(
                        text = "Velocidad: ${settings.speechRate.label}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            item {
                Button(
                    onClick = onTestVoice,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    Text("Probar narración", fontSize = 19.sp)
                }
            }

            item {
                Button(
                    onClick = onToggleTextScale,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    Text(
                        text = "Texto: ${if (settings.textScale == TextScale.LARGE) "Grande" else "Normal"}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            item {
                Button(
                    onClick = onBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text("Volver al inicio", fontSize = 19.sp)
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "La narración usa el motor de voz (TTS) del dispositivo. " +
                            "Si el televisor no tiene una voz en español, la app lo avisa.",
                        color = TvMuted,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "El progreso y los ajustes se guardan localmente. " +
                            "Los cuentos e imágenes en línea se guardan para leer sin conexión.",
                        color = TvMuted,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
