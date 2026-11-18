# Cómo agregar más cuentos

Hay dos formas de ampliar la biblioteca. La remota no requiere recompilar la app.

## Opción A — Cuento local (viene dentro del APK)

Ideal para los cuentos principales, porque siempre están disponibles sin internet.

1. Crea el archivo del cuento en:

   ```
   app/src/main/assets/stories/mi-cuento.json
   ```

2. Copia su portada y sus ilustraciones en:

   ```
   app/src/main/assets/stories/images/
   ```

3. Agrega la entrada al catálogo
   `app/src/main/assets/stories/catalog.json`:

   ```json
   {
     "id": "mi-cuento",
     "title": "Mi cuento",
     "description": "Una aventura nueva.",
     "age": "6+",
     "theme": "FOREST",
     "file": "mi-cuento.json",
     "version": 1,
     "cover": "mi-portada.jpg"
   }
   ```

4. Comprueba que el cuento sea válido y que las imágenes existan:

   ```powershell
   .\gradlew.bat :app:testDebugUnitTest
   ```

5. Compila e instala:

   ```powershell
   .\gradlew.bat :app:assembleDebug
   adb -s emulator-5558 install -r app\build\outputs\apk\debug\app-debug.apk
   ```

## Opción B — Cuento remoto (sin recompilar)

Ideal para agregar contenido después de publicar la app.

1. Crea el cuento en:

   ```
   tv-content/stories/mi-cuento.json
   ```

2. Copia las imágenes en:

   ```
   tv-content/images/
   ```

3. Agrega la entrada a `tv-content/catalog.json` con `cover` y `version`.

4. Sube los cambios a la rama `main` del repositorio configurado en
   `RemoteConfig.kt`:

   ```powershell
   git add tv-content
   git commit -m "Nuevo cuento: Mi cuento"
   git push
   ```

5. En la TV, abre CuentosTV y pulsa **Actualizar cuentos**.

La app descarga el catálogo, valida el cuento, descarga sus imágenes y lo guarda
en el dispositivo. Desde ese momento se puede leer sin conexión.

### Reglas que debe cumplir el cuento

Antes de guardarlo, `StoryValidator` comprueba:

1. Que exista al menos una escena.
2. Que `start` exista.
3. Que cada escena tenga decisiones o sea un final.
4. Que todas las decisiones apunten a escenas existentes.
5. Que exista al menos un final.
6. Que todas las escenas sean alcanzables desde el inicio.

Si algo falla, el cuento se descarta y la app conserva los que ya tenía. El
mensaje de actualización indica cuántos se guardaron y cuáles fallaron.

## Imágenes recomendadas

| Uso | Tamaño | Formato |
| --- | --- | --- |
| Portada | 1280×720 | JPG (calidad 85–90) |
| Ilustración de escena | 1280×720 | JPG (calidad 85–90) |

- Usa el mismo nombre de archivo en el JSON y en la carpeta.
- Evita carpetas dentro de `images/`: el nombre debe ser un archivo simple.
- Las imágenes se recortan para llenar el espacio, así que deja márgenes.
- Si falta una imagen, la app muestra el degradado del tema; el cuento sigue
  funcionando.

Para regenerar las ilustraciones incluidas:

```powershell
powershell -ExecutionPolicy Bypass -File tools\generar-ilustraciones.ps1
```

El script escribe en `assets/stories/images` y en `tv-content/images`.

## Versionar un cuento existente

- Cambia el contenido del JSON.
- Sube `version` en el catálogo (por ejemplo de 1 a 2).
- Si cambian las imágenes, usa nombres nuevos o sube el APK/contenido remoto.

La app siempre descarga la versión publicada del catálogo al pulsar
**Actualizar cuentos**; no hay actualización automática en segundo plano.

## Problemas frecuentes

| Síntoma | Causa probable |
| --- | --- |
| "No se pudo descargar ningún cuento nuevo" | JSON inválido, archivo faltante o URL incorrecta |
| El cuento no aparece en la biblioteca | El `id` ya existe como cuento local (los locales tienen prioridad) |
| Aparece el degradado en vez de la imagen | Nombre de imagen distinto o descarga fallida; pulsa Actualizar otra vez |
| Una voz no suena | La voz es "En línea" y no hay internet, o el motor TTS no la tiene instalada |
| El cuento termina de golpe | Falta un final alcanzable o una escena quedó sin decisiones |
