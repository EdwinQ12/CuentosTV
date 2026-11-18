@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)

package com.ejemplo.cuentostv.ui.screens

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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.Text
import com.ejemplo.cuentostv.data.StoryTheme
import com.ejemplo.cuentostv.remote.RemoteInfo
import com.ejemplo.cuentostv.ui.components.QrCode
import com.ejemplo.cuentostv.ui.theme.StoryBackground
import com.ejemplo.cuentostv.ui.theme.TvAccent
import com.ejemplo.cuentostv.ui.theme.TvMuted
import com.ejemplo.cuentostv.ui.theme.TvSurface
import com.ejemplo.cuentostv.ui.theme.TvText

@Composable
fun PairingScreen(
    info: RemoteInfo,
    onBack: () -> Unit
) {
    val backFocus = remember { FocusRequester() }
    val url = info.url

    LaunchedEffect(Unit) {
        runCatching { backFocus.requestFocus() }
    }

    StoryBackground(theme = StoryTheme.NIGHT) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 56.dp, vertical = 34.dp),
            horizontalArrangement = Arrangement.spacedBy(36.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Vincular teléfono",
                    color = TvText,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Usa el teléfono como mando y elige las decisiones del cuento.",
                    color = TvMuted,
                    fontSize = 16.sp
                )

                Spacer(Modifier.height(2.dp))
                Text(
                    text = "1. Conecta el teléfono a la misma Wi-Fi que la TV.",
                    color = TvMuted,
                    fontSize = 15.sp
                )
                Text(
                    text = "2. Escanea el código QR o abre la dirección en el navegador.",
                    color = TvMuted,
                    fontSize = 15.sp
                )
                Text(
                    text = "3. Escribe el código de 4 dígitos.",
                    color = TvMuted,
                    fontSize = 15.sp
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = TvSurface.copy(alpha = 0.75f),
                            shape = RoundedCornerShape(18.dp)
                        )
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = if (url != null) {
                                url
                            } else {
                                "No se detectó una red local"
                            },
                            color = TvAccent,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Código de vinculación",
                            color = TvMuted,
                            fontSize = 13.sp
                        )
                        Text(
                            text = info.code,
                            color = TvText,
                            fontSize = 52.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 10.sp
                        )
                    }
                }

                Text(
                    text = when {
                        !info.running -> info.error ?: "El mando no está disponible."
                        info.paired -> "Teléfono vinculado. Ya puedes usarlo como mando."
                        else -> "Esperando teléfono..."
                    },
                    color = if (info.paired) TvAccent else TvMuted,
                    fontSize = 15.sp
                )

                Button(
                    onClick = onBack,
                    modifier = Modifier
                        .height(52.dp)
                        .focusRequester(backFocus)
                ) {
                    Text("Volver", fontSize = 18.sp)
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            color = TvSurface.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(22.dp)
                        )
                        .padding(14.dp)
                ) {
                    if (url != null) {
                        QrCode(content = url, size = 190.dp)
                    } else {
                        Box(
                            modifier = Modifier
                                .width(190.dp)
                                .height(190.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Sin red",
                                color = TvMuted,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
                Text(
                    text = "Escanea con la cámara",
                    color = TvMuted,
                    fontSize = 14.sp
                )
            }
        }
    }
}
