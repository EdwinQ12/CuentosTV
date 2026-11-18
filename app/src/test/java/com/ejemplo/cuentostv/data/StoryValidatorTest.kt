package com.ejemplo.cuentostv.data

import java.io.File
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StoryValidatorTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun loadAssetStory(id: String): Story {
        val file = File("src/main/assets/stories/$id.json")
        return json.decodeFromString(Story.serializer(), file.readText())
    }

    private fun node(
        text: String = "Texto",
        choices: List<StoryChoice> = emptyList(),
        ending: Boolean = false
    ) = StoryNode(text = text, choices = choices, ending = ending)

    @Test
    fun `cuento valido no tiene errores`() {
        val story = Story(
            id = "test",
            title = "Cuento de prueba",
            start = "inicio",
            nodes = mapOf(
                "inicio" to node(choices = listOf(StoryChoice("Seguir", "final"))),
                "final" to node(ending = true)
            )
        )

        assertTrue(StoryValidator.validate(story).isEmpty())
        assertTrue(StoryValidator.isPlayable(story))
    }

    @Test
    fun `escena inicial inexistente`() {
        val story = Story(
            id = "test",
            title = "Cuento",
            start = "no-existe",
            nodes = mapOf("final" to node(ending = true))
        )

        val errors = StoryValidator.validate(story)
        assertTrue(errors.any { it.contains("escena inicial") })
        assertFalse(StoryValidator.isPlayable(story))
    }

    @Test
    fun `escena sin decisiones ni final`() {
        val story = Story(
            id = "test",
            title = "Cuento",
            start = "inicio",
            nodes = mapOf(
                "inicio" to node(),
                "final" to node(ending = true)
            )
        )

        val errors = StoryValidator.validate(story)
        assertTrue(errors.any { it.contains("no tiene decisiones") })
    }

    @Test
    fun `decision apunta a escena inexistente`() {
        val story = Story(
            id = "test",
            title = "Cuento",
            start = "inicio",
            nodes = mapOf(
                "inicio" to node(choices = listOf(StoryChoice("Seguir", "fantasma")))
            )
        )

        val errors = StoryValidator.validate(story)
        assertTrue(errors.any { it.contains("fantasma") })
    }

    @Test
    fun `cuento sin finales`() {
        val story = Story(
            id = "test",
            title = "Cuento",
            start = "inicio",
            nodes = mapOf(
                "inicio" to node(choices = listOf(StoryChoice("Volver", "inicio")))
            )
        )

        val errors = StoryValidator.validate(story)
        assertTrue(errors.any { it.contains("ningún final") })
    }

    @Test
    fun `escena inalcanzable`() {
        val story = Story(
            id = "test",
            title = "Cuento",
            start = "inicio",
            nodes = mapOf(
                "inicio" to node(choices = listOf(StoryChoice("Seguir", "final"))),
                "final" to node(ending = true),
                "perdida" to node(ending = true)
            )
        )

        val errors = StoryValidator.validate(story)
        assertTrue(errors.any { it.contains("inalcanzables") && it.contains("perdida") })
    }

    @Test
    fun `cuento vacio reporta falta de escenas`() {
        val story = Story(id = "test", title = "Cuento", start = "inicio")

        assertEquals(
            listOf("El cuento no tiene escenas."),
            StoryValidator.validate(story)
        )
    }

    @Test
    fun `cuento remoto de ejemplo es jugable`() {
        val file = File("../tv-content/stories/reloj.json")
        assertTrue("Falta tv-content/stories/reloj.json", file.exists())
        val story = json.decodeFromString(Story.serializer(), file.readText())
        assertTrue(StoryValidator.validate(story).toString(), StoryValidator.isPlayable(story))
    }

    @Test
    fun `cuentos locales incluidos son jugables`() {
        listOf("bosque", "luna", "mar").forEach { id ->
            val story = loadAssetStory(id)
            assertTrue("El cuento local '$id' no es válido", StoryValidator.isPlayable(story))
        }
    }
}
