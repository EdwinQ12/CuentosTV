@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)

package com.ejemplo.cuentostv.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.ejemplo.cuentostv.data.StoryTheme
import com.ejemplo.cuentostv.ui.TtsVoice
import com.ejemplo.cuentostv.ui.theme.StoryBackground
import com.ejemplo.cuentostv.ui.theme.TvAccent
import com.ejemplo.cuentostv.ui.theme.TvMuted
import com.ejemplo.cuentostv.ui.theme.TvText

@Composable
fun VoicesScreen(
    voices: List<TtsVoice>,
    selected: String?,
    onSelect: (String?) -> Unit,
    onTest: () -> Unit,
    onSystemSettings: () -> Unit,
    onBack: () -> Unit
) {
    val firstFocus = remember { FocusRequester() }

    LaunchedEffect(voices) {
        runCatching { firstFocus.requestFocus() }
    }

    StoryBackground(theme = StoryTheme.NIGHT) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 64.dp, vertical = 34.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    text = "Voz de la narración",
                    color = TvText,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Voces en español disponibles en este televisor.",
                    color = TvMuted,
                    fontSize = 16.sp
                )
            }

            item {
                Button(
                    onClick = {
                        onSelect(null)
                        onTest()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .focusRequester(firstFocus)
                ) {
                    Text(
                        text = if (selected == null) "✓ Automática (voz del sistema)" else "Automática (voz del sistema)",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (voices.isEmpty()) {
                item {
                    Text(
                        text = "No se encontraron voces en español. Puedes instalar una voz " +
                            "desde los ajustes de voz del sistema.",
                        color = TvMuted,
                        fontSize = 17.sp
                    )
                }
            } else {
                items(voices, key = { it.name }) { voice ->
                    Button(
                        onClick = {
                            onSelect(voice.name)
                            onTest()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text(
                            text = buildString {
                                if (selected == voice.name) append("✓ ")
                                append(voice.label)
                                if (voice.online) append("  ·  En línea")
                            },
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            item {
                Button(
                    onClick = onTest,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text("Probar la voz seleccionada", fontSize = 18.sp)
                }
            }

            item {
                Button(
                    onClick = onSystemSettings,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text("Abrir ajustes de voz del televisor", fontSize = 18.sp)
                }
            }

            item {
                Button(
                    onClick = onBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text("Volver a ajustes", fontSize = 18.sp)
                }
            }

            item {
                Text(
                    text = if (selected == null) {
                        "La app usará la voz que el televisor tenga configurada por defecto."
                    } else {
                        "La voz elegida se probará al seleccionarla y se guardará para los cuentos."
                    },
                    color = TvAccent,
                    fontSize = 14.sp
                )
            }
        }
    }
}
