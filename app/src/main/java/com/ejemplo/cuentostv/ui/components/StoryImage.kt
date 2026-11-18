package com.ejemplo.cuentostv.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.ejemplo.cuentostv.data.StoryItem

@Composable
fun StoryImage(
    item: StoryItem,
    name: String?,
    loadImage: suspend (StoryItem, String) -> Bitmap?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    fallback: @Composable BoxScope.() -> Unit = {}
) {
    val bitmap by produceState<Bitmap?>(
        initialValue = null,
        item.summary.id,
        item.remote,
        name
    ) {
        value = name?.let { runCatching { loadImage(item, it) }.getOrNull() }
    }

    val current = bitmap
    if (current != null) {
        Image(
            bitmap = current.asImageBitmap(),
            contentDescription = null,
            modifier = modifier,
            contentScale = contentScale
        )
    } else {
        Box(modifier = modifier) {
            fallback()
        }
    }
}
