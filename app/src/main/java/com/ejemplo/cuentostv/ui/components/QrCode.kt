package com.ejemplo.cuentostv.ui.components

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

@Composable
fun QrCode(
    content: String,
    modifier: Modifier = Modifier,
    size: Dp = 190.dp
) {
    val bitmap = remember(content) {
        runCatching {
            val dimension = 512
            val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, dimension, dimension)
            val pixels = IntArray(dimension * dimension)
            for (y in 0 until dimension) {
                val offset = y * dimension
                for (x in 0 until dimension) {
                    pixels[offset + x] = if (matrix[x, y]) Color.BLACK else Color.WHITE
                }
            }
            Bitmap.createBitmap(dimension, dimension, Bitmap.Config.ARGB_8888).apply {
                setPixels(pixels, 0, dimension, 0, 0, dimension, dimension)
            }
        }.getOrNull()
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Código QR para vincular el teléfono",
            modifier = modifier
                .size(size)
                .background(androidx.compose.ui.graphics.Color.White)
                .padding(6.dp),
            contentScale = ContentScale.Fit
        )
    }
}
