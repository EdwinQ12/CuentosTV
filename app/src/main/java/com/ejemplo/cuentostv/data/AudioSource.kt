package com.ejemplo.cuentostv.data

import java.io.File

sealed interface AudioSource {
    data class Asset(val path: String) : AudioSource
    data class LocalFile(val file: File) : AudioSource
}
