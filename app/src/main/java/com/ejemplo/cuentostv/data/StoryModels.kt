package com.ejemplo.cuentostv.data

import kotlinx.serialization.Serializable

@Serializable
enum class StoryTheme {
    FOREST,
    NIGHT,
    SEA,
    CASTLE,
    SPACE
}

@Serializable
data class StoryCatalog(
    val schemaVersion: Int = 1,
    val stories: List<StorySummary> = emptyList()
)

@Serializable
data class StorySummary(
    val id: String,
    val title: String,
    val description: String,
    val age: String = "6+",
    val theme: StoryTheme = StoryTheme.FOREST,
    val file: String,
    val version: Int = 1,
    val cover: String? = null
)

@Serializable
data class Story(
    val schemaVersion: Int = 1,
    val id: String,
    val title: String,
    val start: String,
    val nodes: Map<String, StoryNode> = emptyMap()
)

@Serializable
data class StoryNode(
    val text: String,
    val theme: StoryTheme? = null,
    val choices: List<StoryChoice> = emptyList(),
    val ending: Boolean = false,
    val endingTitle: String? = null,
    val image: String? = null
)

@Serializable
data class StoryChoice(
    val text: String,
    val next: String
)

data class StoryItem(
    val summary: StorySummary,
    val remote: Boolean
)
