package com.ejemplo.cuentostv.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class StoryRepository(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val remoteDir = File(context.filesDir, "remote")
    private val remoteStoriesDir = File(remoteDir, "stories")
    private val remoteImagesDir = File(remoteDir, "images")
    private val remoteAudioDir = File(remoteDir, "audio")
    private val remoteCatalogFile = File(remoteDir, "catalog.json")

    private val imageCache = object : LruCache<String, Bitmap>(12) {
        override fun sizeOf(key: String, value: Bitmap): Int = 1
    }

    data class RemoteRefresh(
        val items: List<StoryItem>,
        val success: Boolean,
        val message: String
    )

    suspend fun loadInitialItems(): List<StoryItem> = withContext(Dispatchers.IO) {
        merge(loadLocalItems(), loadCachedRemoteItems())
    }

    suspend fun loadLocalItems(): List<StoryItem> = withContext(Dispatchers.IO) {
        val catalog = runCatching {
            val text = context.assets.open("stories/catalog.json")
                .bufferedReader()
                .use { it.readText() }
            json.decodeFromString(StoryCatalog.serializer(), text)
        }.getOrNull() ?: return@withContext emptyList()

        catalog.stories.map { StoryItem(summary = it, remote = false) }
    }

    suspend fun loadCachedRemoteItems(): List<StoryItem> = withContext(Dispatchers.IO) {
        if (!remoteCatalogFile.exists()) return@withContext emptyList()
        val catalog = runCatching {
            json.decodeFromString(
                StoryCatalog.serializer(),
                remoteCatalogFile.readText()
            )
        }.getOrNull() ?: return@withContext emptyList()

        catalog.stories
            .filter { File(remoteStoriesDir, it.file).exists() }
            .map { StoryItem(summary = it, remote = true) }
    }

    suspend fun loadStory(item: StoryItem): Story? = withContext(Dispatchers.IO) {
        val text = if (item.remote) {
            val file = File(remoteStoriesDir, item.summary.file)
            if (!file.exists()) return@withContext null
            runCatching { file.readText() }.getOrNull()
        } else {
            runCatching {
                context.assets.open("stories/${item.summary.file}")
                    .bufferedReader()
                    .use { it.readText() }
            }.getOrNull()
        } ?: return@withContext null

        val story = runCatching {
            json.decodeFromString(Story.serializer(), text)
        }.getOrNull() ?: return@withContext null

        if (StoryValidator.isPlayable(story)) story else null
    }

    suspend fun loadImage(item: StoryItem, name: String): Bitmap? = withContext(Dispatchers.IO) {
        val fileName = safeName(name)
        val key = "${if (item.remote) "r" else "a"}:$fileName"
        imageCache.get(key)?.let { return@withContext it }

        val bitmap = runCatching {
            val bytes = if (item.remote) {
                val file = File(remoteImagesDir, fileName)
                if (!file.exists()) return@withContext null
                file.readBytes()
            } else {
                context.assets.open("stories/images/$fileName").use { it.readBytes() }
            }
            decode(bytes)
        }.getOrNull() ?: return@withContext null

        imageCache.put(key, bitmap)
        bitmap
    }

    suspend fun audioSource(item: StoryItem, name: String): AudioSource? =
        withContext(Dispatchers.IO) {
            val fileName = safeName(name)
            if (item.remote) {
                val file = File(remoteAudioDir, fileName)
                if (file.exists() && file.length() > 0L) {
                    AudioSource.LocalFile(file)
                } else {
                    null
                }
            } else {
                val path = "stories/audio/$fileName"
                val exists = runCatching { context.assets.open(path).close() }.isSuccess
                if (exists) AudioSource.Asset(path) else null
            }
        }

    suspend fun refreshRemote(): RemoteRefresh = withContext(Dispatchers.IO) {
        val catalogText = runCatching { fetchText(RemoteConfig.CATALOG_URL) }.getOrNull()
            ?: return@withContext RemoteRefresh(
                items = loadCachedRemoteItems(),
                success = false,
                message = "No se pudo conectar. Se usarán los cuentos guardados."
            )

        val catalog = runCatching {
            json.decodeFromString(StoryCatalog.serializer(), catalogText)
        }.getOrNull() ?: return@withContext RemoteRefresh(
            items = loadCachedRemoteItems(),
            success = false,
            message = "El catálogo remoto no tiene un formato válido."
        )

        if (catalog.stories.isEmpty()) {
            return@withContext RemoteRefresh(
                items = loadCachedRemoteItems(),
                success = false,
                message = "El catálogo remoto está vacío."
            )
        }

        remoteDir.mkdirs()
        remoteStoriesDir.mkdirs()
        remoteImagesDir.mkdirs()
        remoteAudioDir.mkdirs()

        val valid = mutableListOf<StorySummary>()
        val errors = mutableListOf<String>()

        catalog.stories.forEach { summary ->
            val storyText = runCatching { fetchText(RemoteConfig.storyUrl(summary.file)) }.getOrNull()
            if (storyText == null) {
                errors += summary.title
                return@forEach
            }

            val story = runCatching {
                json.decodeFromString(Story.serializer(), storyText)
            }.getOrNull()

            if (story == null || !StoryValidator.isPlayable(story)) {
                errors += summary.title
                return@forEach
            }

            File(remoteStoriesDir, summary.file).writeText(storyText)
            downloadImages(summary, story)
            downloadAudio(story)
            valid += summary
        }

        if (valid.isEmpty()) {
            return@withContext RemoteRefresh(
                items = loadCachedRemoteItems(),
                success = false,
                message = "No se pudo descargar ningún cuento nuevo."
            )
        }

        val validCatalog = StoryCatalog(
            schemaVersion = catalog.schemaVersion,
            stories = valid
        )
        remoteCatalogFile.writeText(
            json.encodeToString(StoryCatalog.serializer(), validCatalog)
        )

        val message = if (errors.isEmpty()) {
            "Biblioteca actualizada: ${valid.size} cuento(s) en línea."
        } else {
            "Se actualizaron ${valid.size} cuento(s). No se pudieron descargar: ${errors.joinToString(", ")}."
        }

        RemoteRefresh(
            items = valid.map { StoryItem(summary = it, remote = true) },
            success = true,
            message = message
        )
    }

    private fun downloadImages(summary: StorySummary, story: Story) {
        val names = buildSet {
            summary.cover?.let { add(it) }
            story.nodes.values.forEach { node -> node.image?.let { add(it) } }
        }

        names.forEach { name ->
            val target = File(remoteImagesDir, safeName(name))
            if (target.exists() && target.length() > 0L) return@forEach
            runCatching {
                target.writeBytes(fetchBytes(RemoteConfig.imageUrl(safeName(name))))
            }
        }
    }

    private fun downloadAudio(story: Story) {
        val names = story.nodes.values.mapNotNull { it.audio }.toSet()
        names.forEach { name ->
            val target = File(remoteAudioDir, safeName(name))
            if (target.exists() && target.length() > 0L) return@forEach
            runCatching {
                target.writeBytes(fetchBytes(RemoteConfig.audioUrl(safeName(name))))
            }
        }
    }

    private fun decode(bytes: ByteArray): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sample = 1
        while (bounds.outWidth / (sample * 2) >= 1280) sample *= 2

        val options = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    }

    private fun safeName(name: String): String =
        name.substringAfterLast('/').substringAfterLast('\\')

    private fun fetchText(url: String): String =
        fetchBytes(url).toString(Charsets.UTF_8)

    private fun fetchBytes(url: String): ByteArray {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8_000
            readTimeout = 8_000
            requestMethod = "GET"
            instanceFollowRedirects = true
            useCaches = false
            setRequestProperty("User-Agent", "CuentosTV/1.0")
        }

        return try {
            val code = connection.responseCode
            if (code !in 200..299) {
                error("HTTP $code")
            }
            connection.inputStream.use { it.readBytes() }
        } finally {
            connection.disconnect()
        }
    }

    private fun merge(local: List<StoryItem>, remote: List<StoryItem>): List<StoryItem> {
        val seen = mutableSetOf<String>()
        val result = mutableListOf<StoryItem>()
        local.forEach { item ->
            if (seen.add(item.summary.id)) result += item
        }
        remote.forEach { item ->
            if (seen.add(item.summary.id)) result += item
        }
        return result
    }
}
