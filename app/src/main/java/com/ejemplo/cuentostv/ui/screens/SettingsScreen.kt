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
    onToggleNarration: () -> Unit,
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
            contentPadding = PaddingValues(horizontal = 72.dp, vertical = 48.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            item {
                Text(
                    text = "Ajustes",
                    color = TvText,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Personaliza la lectura de los cuentos.",
                    color = TvMuted,
                    fontSize = 20.sp
                )
            }

            item {
                Button(
                    onClick = onToggleNarration,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(68.dp)
                        .focusRequester(firstFocus)
                ) {
                    Text(
                        text = "Narración: ${if (settings.narrationEnabled) "Activada" else "Desactivada"}",
                        fontSize = 23.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            item {
                Button(
                    onClick = onToggleTextScale,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(68.dp)
                ) {
                    Text(
                        text = "Texto: ${if (settings.textScale == TextScale.LARGE) "Grande" else "Normal"}",
                        fontSize = 23.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            item {
                Button(
                    onClick = onBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                ) {
                    Text("Volver al inicio", fontSize = 21.sp)
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "La narración usa el motor de voz (TTS) del dispositivo. " +
                            "Si el televisor no tiene una voz en español, puede leer con otra voz o no estar disponible.",
                        color = TvMuted,
                        fontSize = 17.sp
                    )
                    Text(
                        text = "El progreso y los ajustes se guardan localmente. " +
                            "Los cuentos en línea se descargan y se guardan para poder leerlos sin conexión.",
                        color = TvMuted,
                        fontSize = 17.sp
                    )
                }
            }
        }
    }
}
