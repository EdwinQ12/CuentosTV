# Esquema de los cuentos

Los cuentos son archivos JSON que se cargan desde `assets/stories` o desde el
catálogo remoto. Antes de mostrarse pasan por `StoryValidator`; si el grafo está
roto, el cuento no se abre.

## Catálogo (`catalog.json`)

```json
{
  "schemaVersion": 1,
  "stories": [
    {
      "id": "bosque",
      "title": "El bosque de las luces",
      "description": "Una luz azul te guía entre árboles...",
      "age": "6+",
      "theme": "FOREST",
      "file": "bosque.json",
      "version": 2,
      "cover": "bosque.jpg"
    }
  ]
}
```

| Campo | Tipo | Obligatorio | Descripción |
| --- | --- | --- | --- |
| `schemaVersion` | entero | no (1) | Versión del formato |
| `stories[].id` | texto | sí | Identificador único, sin espacios |
| `stories[].title` | texto | sí | Título visible |
| `stories[].description` | texto | sí | Texto de la tarjeta |
| `stories[].age` | texto | no (`6+`) | Edad recomendada |
| `stories[].theme` | enum | no (`FOREST`) | `FOREST`, `NIGHT`, `SEA`, `CASTLE`, `SPACE` |
| `stories[].file` | texto | sí | Archivo del cuento, relativo a `stories/` |
| `stories[].version` | entero | no (1) | Versión del contenido |
| `stories[].cover` | texto | no | Imagen de portada, relativa a `images/` |

## Cuento

```json
{
  "schemaVersion": 1,
  "id": "bosque",
  "title": "El bosque de las luces",
  "start": "inicio",
  "nodes": {
    "inicio": { "...": "..." }
  }
}
```

| Campo | Tipo | Obligatorio | Descripción |
| --- | --- | --- | --- |
| `schemaVersion` | entero | no (1) | Versión del formato |
| `id` | texto | sí | Debe coincidir con el catálogo |
| `title` | texto | sí | Título |
| `start` | texto | sí | ID del nodo inicial; debe existir |
| `nodes` | objeto | sí | Mapa `id -> escena` |

## Escena (`nodes`)

```json
{
  "text": "La luz te lleva hasta un río...",
  "theme": "FOREST",
  "choices": [
    { "text": "Cruzar el puente", "next": "puente" },
    { "text": "Seguir la orilla", "next": "orilla" }
  ],
  "ending": false,
  "endingTitle": null
}
```

| Campo | Tipo | Obligatorio | Descripción |
| --- | --- | --- | --- |
| `text` | texto | sí | Texto que se lee y se narra |
| `theme` | enum | no | Tema visual de la escena |
| `image` | texto | no | Ilustración de la escena, relativa a `images/` |
| `choices` | lista | si no es final | Entre 2 y 4 opciones recomendadas |
| `choices[].text` | texto | sí | Texto del botón |
| `choices[].next` | texto | sí | ID de la escena destino; debe existir |
| `ending` | booleano | no (`false`) | Marca la escena como final |
| `endingTitle` | texto | no | Título grande del final |

## Imágenes

- **Locales:** `app/src/main/assets/stories/images/<archivo>`
- **Remotas:** `tv-content/images/<archivo>`; la app las descarga y guarda en
  `filesDir/remote/images` para leer sin conexión.
- Se recomiendan JPG de 1280×720 (calidad 85–90).
- El campo es opcional: sin imagen se usa el degradado del tema.
- El nombre debe ser un archivo simple; no se admiten rutas con carpetas.

La app valida en las pruebas que todas las imágenes referenciadas existan.

## Reglas de validación

Un cuento es jugable cuando:

1. Tiene al menos una escena.
2. `start` existe en `nodes`.
3. Cada escena tiene decisiones o está marcada como final.
4. Todas las decisiones apuntan a escenas existentes.
5. Existe al menos un final.
6. Todas las escenas son alcanzables desde `start`.

Las pruebas de `StoryValidatorTest` comprueban cada regla con casos rotos y con
los cuentos incluidos.

## Consejos de escritura

- Los cuentos incluidos tienen 17–19 escenas y 9–10 finales; apunta a esa
  profundidad si quieres una sesión de 10–15 minutos.
- Usa entre 2 y 4 decisiones por escena para que el control remoto no canse.
- Mantén los textos de 1 a 3 frases: se leen en voz alta y se ven desde el sofá.
- Escribe IDs cortos y sin acentos (`inicio`, `cueva`, `final-luna`).
- Evita ciclos sin salida: el validador no los bloquea, pero la historia puede
  no terminar.
- Reutiliza el tema de la escena para reforzar el ambiente.

## Publicar contenido remoto

1. Copia el cuento a `tv-content/stories/`.
2. Añade su entrada a `tv-content/catalog.json`.
3. Sube los cambios a la rama `main` del repositorio configurado en
   `RemoteConfig.kt`.
4. En la app, pulsa **Actualizar cuentos**.

Si un cuento remoto no descarga o no valida, la app conserva los cuentos que ya
tenía guardados y muestra un aviso.
