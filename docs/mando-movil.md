# Mando desde el teléfono

La TV incluye un mando web. No hay que instalar nada en el teléfono: la propia TV
sirve una página y el teléfono la abre en el navegador.

## Uso

1. En la TV: **Ajustes → Vincular teléfono**.
2. En el teléfono: conéctalo a la **misma red Wi-Fi** que la TV.
3. Escanea el **QR** o abre la dirección que aparece (por ejemplo
   `http://192.168.1.50:8765`).
4. Escribe el **código de 4 dígitos** que muestra la TV.
5. El teléfono muestra el cuento, las decisiones y los controles.

Desde el teléfono se puede:

- Elegir un cuento de la biblioteca.
- Empezar, continuar o reiniciar un cuento.
- Leer el texto de cada escena y elegir una decisión.
- Pausar, seguir o repetir la narración.
- Activar o desactivar la voz.
- Cambiar el tamaño del texto y la velocidad.
- Volver atrás o al inicio.

La TV sigue siendo la que narra y guarda el progreso. El teléfono solo envía
órdenes.

## Cómo funciona

```
Teléfono  ── HTTP ──►  TV (NanoHTTPD, puerto 8765)
   ▲                        │
   └──── estado JSON ◄──────┘
```

- `GET /` → página del mando (`app/src/main/assets/remote/index.html`).
- `POST /api/pair` → `{ "code": "1234" }`; devuelve un token.
- `GET /api/state?token=...` → cuento actual, texto, opciones y estado de audio.
- `POST /api/action` → `{ "token": "...", "action": "choose", "index": 0 }`.
- `GET /api/info` → nombre y estado de vinculación.

Acciones disponibles: `open_story`, `start_new`, `continue`, `choose`, `back`,
`home`, `restart`, `settings`, `toggle_narration`, `toggle_text`, `cycle_rate`,
`audio_toggle`.

El servidor se inicia al abrir la app y se detiene al cerrarla. El código se
genera al arrancar y solo se acepta un teléfono a la vez; cada vinculación crea
un token nuevo.

## Seguridad y límites

- Solo escucha en la red local; no se abre a internet.
- El token es obligatorio para leer el estado y enviar acciones.
- El código de 4 dígitos es la única barrera: úsalo en una red de confianza.
- Redes de invitados con aislamiento de clientes impiden la conexión.
- No sincroniza progreso entre dispositivos; eso requeriría una nube.

## Probar en el emulador

El emulador está detrás de NAT, así que el teléfono no puede llegar a su IP
directamente. Para probar desde el PC:

```powershell
adb -s emulator-5558 forward tcp:8765 tcp:8765
```

Abre `http://localhost:8765` en el navegador del PC, escribe el código que
muestra la TV y prueba las decisiones. La TV reacciona igual que con el control
remoto.

## Cambiar el puerto o la página

- El puerto se elige en `RemoteServer.kt` (intenta 8765–8769 si está ocupado).
- La página del mando está en `app/src/main/assets/remote/index.html`.
- El estado que recibe el teléfono se arma en `RemoteStateMapper.kt`.

## Problemas frecuentes

| Síntoma | Causa |
| --- | --- |
| La página no abre | TV y teléfono en redes distintas, o TV sin IP local |
| "Código incorrecto" | El código cambió al reiniciar la app; mira la TV |
| El teléfono dejó de responder | La app se cerró o se reinició; vuelve a vincular |
| Funciona en el PC pero no en el teléfono | En el emulador usa `adb forward`; en una TV real usa su IP |
| No aparece el QR | La TV no tiene red local; revisa la conexión Wi-Fi/Ethernet |
