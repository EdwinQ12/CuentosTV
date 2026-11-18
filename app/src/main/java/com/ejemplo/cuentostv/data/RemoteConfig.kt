package com.ejemplo.cuentostv.data

object RemoteConfig {
    private const val CONTENT_BASE =
        "https://raw.githubusercontent.com/EdwinQ12/CuentosTV/main/tv-content"

    const val CATALOG_URL = "$CONTENT_BASE/catalog.json"

    fun storyUrl(file: String): String = "$CONTENT_BASE/stories/$file"

    fun imageUrl(file: String): String = "$CONTENT_BASE/images/$file"

    fun audioUrl(file: String): String = "$CONTENT_BASE/audio/$file"
}
