package com.ejemplo.cuentostv.data

object StoryValidator {

    fun validate(story: Story): List<String> {
        val errors = mutableListOf<String>()

        if (story.nodes.isEmpty()) {
            errors += "El cuento no tiene escenas."
            return errors
        }
        if (story.start !in story.nodes) {
            errors += "La escena inicial '${story.start}' no existe."
        }

        story.nodes.forEach { (nodeId, node) ->
            if (!node.ending && node.choices.isEmpty()) {
                errors += "La escena '$nodeId' no tiene decisiones ni final."
            }
            node.choices.forEach { choice ->
                if (choice.next !in story.nodes) {
                    errors += "La escena '$nodeId' apunta a '${choice.next}', que no existe."
                }
            }
        }

        if (story.nodes.values.none { it.ending }) {
            errors += "El cuento no tiene ningún final."
        }

        if (story.start in story.nodes) {
            val visited = mutableSetOf<String>()
            val pending = ArrayDeque<String>()
            pending.add(story.start)
            while (pending.isNotEmpty()) {
                val nodeId = pending.removeFirst()
                if (!visited.add(nodeId)) continue
                story.nodes[nodeId]?.choices?.forEach { choice ->
                    if (choice.next in story.nodes) pending.add(choice.next)
                }
            }
            val unreachable = story.nodes.keys - visited
            if (unreachable.isNotEmpty()) {
                errors += "Escenas inalcanzables: ${unreachable.joinToString(", ")}."
            }
        }

        return errors
    }

    fun isPlayable(story: Story): Boolean = validate(story).isEmpty()
}
