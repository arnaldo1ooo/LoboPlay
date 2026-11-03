# LoboPlay

App Android (celular y Android TV) para guardar tus sitios favoritos en un menú.

## Qué hace
- Menú en grilla con búsqueda, favoritos y navegación con control remoto.
- Agregar, editar y eliminar sitios a mano (pulsación larga sobre un sitio = opciones).
- Canales de TV abierta precargados la primera vez (archivo `app/src/main/assets/canales.json`).
- Icono de cada sitio: por defecto el de la propia página; se puede cambiar por uno propio.
- Bloqueador de anuncios siempre activo (lista en `app/src/main/assets/anuncios.txt`). En los sitios que no reproducen si lo detectan, se desactiva apagando "Bloquear anuncios en este sitio" (Agregar/Editar).
- Video a pantalla completa dentro del navegador interno.
- Exportar e importar la lista en un archivo JSON (Ajustes > Respaldo). Al importar no se repiten los sitios que ya tienes. Algunas TV no traen selector de archivos; en ese caso la app lo avisa.

## Cómo levantarlo (Android Studio)
1. Instala Android Studio (la versión actual, gratis).
2. Abre Android Studio > Open > elige la carpeta `LoboPlay`.
3. Espera a que termine "Gradle sync" (la primera vez descarga cosas, puede tardar unos minutos).
4. Arriba, en el selector de dispositivos, elige uno:
   - Tu celular con "Depuración USB" activada, o
   - Un emulador: Device Manager > Create Device > "Phone" o "Television (1080p)".
5. Presiona el botón verde de Play (Run).

## Probar en tu TV real
1. En la TV: Ajustes > Acerca de > pulsa 7 veces en "Compilación" para activar opciones de desarrollador.
2. Activa "Depuración por USB" o "Depuración por red".
3. En Android Studio: Device Manager > Pair devices over Wi-Fi, o `adb connect IP_DE_LA_TV:5555`.
4. Elige la TV en el selector y presiona Run.

## Generar el APK para instalar
Build > Build Bundle(s) / APK(s) > Build APK(s). Queda en `app/build/outputs/apk/debug/`.

## Antes de publicar
- Revisa que cada URL de `canales.json` sea la página oficial del canal.
- Cambia `applicationId` en `app/build.gradle` si quieres otro identificador.

## Versión 1.4.0 — ahora es LoboPlay
- Nuevo nombre: **LoboPlay** (proyecto, paquete `py.loboplay.app`, tema y archivo de respaldo `loboplay-sitios.json`).
- Nuevo logo (lobo con botón de play) en el icono de la app, el banner de Android TV y el menú principal.
- Colores nuevos acordes al logo: azul eléctrico y cian; el tema oscuro usa azul noche.
- Importante: al cambiar el identificador (`py.loboplay.app`) Android lo instala como una app nueva. Para pasar tus sitios desde JokerPy, usa Ajustes > Respaldo > Exportar en la versión vieja e Importar en esta.
- Los archivos del logo están en la carpeta `diseno/` (icono de 512 px para tiendas y logo original).

## Canales y video
- Se quitó la opción "Reproducir en pantalla completa al abrir": los canales con video detectado ya abren el reproductor directo. La app pasa a horizontal solo al tocar pantalla completa; Atrás sale de la pantalla completa y luego cierra el canal.
- Interruptor por sitio "Bloquear anuncios en este sitio" (activado por defecto; SNT lo trae apagado, porque su página no reproduce si detecta el bloqueador).
- Cada sitio guarda la página (para el nombre y el icono) y, aparte, el video detectado. Si hay video se abre directo; si no, se abre la página.

## Canales con dirección directa del video (.m3u8)
- Si la dirección del video termina en `.m3u8`, la app la reproduce con una página propia: sin anuncios, sin la página del canal y con los botones de video del sistema. La página del sitio sirve de origen.
- Usa la librería hls.js (licencia Apache 2.0): si existe `app/src/main/assets/hls.min.js` la usa desde ahí y, si no, la baja de internet (jsDelivr). Si tampoco puede, prueba la reproducción nativa de Android. Si algo falla, el motivo se muestra en la parte de arriba del video.
- Los reproductores de Dailymotion (como el de Trece) se abren dentro de una página propia con el origen del canal, para que el reproductor los acepte.
- SNT usa esta forma. Si el canal cambia su dirección de video, edita el sitio y pega la nueva (en Chrome: F12 > Network > filtrar `m3u8`).

## Novedades de la versión 1.1
- Diseño más moderno: tarjetas redondeadas, logo en el menú, botón flotante "Agregar sitio", chips Todos/Favoritos y buscador redondeado.
- El título LoboPlay ya no se corta (los botones de texto pasaron a íconos).
- Botón de retroceder en el navegador interno (con recargar y cerrar), y flecha de volver en Agregar y Ajustes.
- Tema oscuro automático y foco más visible con el control remoto.

## Novedades de la versión 1.2
- Al escribir o pegar la dirección en "Agregar sitio", la app busca la página y sugiere el título y el icono (queda editable).
- Si no eliges un icono propio, al guardar se usa el icono de la página que ya se había bajado.

## Novedades de la versión 1.3
- Modo escritorio por sitio (interruptor en Agregar/Editar sitio).
- Reordenar: ícono de flechas arriba (arrastrar con pulsación larga) o "Mover antes/después" en el menú del sitio (sirve con control remoto).
- Chip "Recientes" con los últimos sitios abiertos.
- Imagen en imagen en el navegador (botón en la barra y automática al salir con un video en marcha). Se desactiva en Ajustes > Navegación.
- Ajustes > Navegación > "Actualizar lista de anuncios": descarga una lista más completa.
- Cursor con el control remoto en TV: flechas mueven el cursor, OK toca, el botón de cursor (o la tecla Menú) lo activa o desactiva. Al llegar arriba del todo, la flecha arriba pasa a la barra.
- La base de datos se actualiza sola: no se pierden los sitios que ya tienes.

## Versión 1.3.1
- Se quitó la opción de PIN.

## Versión 1.3.2
- Corregido: la barra de estado y los botones del sistema ya no tapan el título, los íconos ni el botón Agregar sitio.

## Correcciones recientes
- Agregar sitio: la búsqueda del título e icono ya no deja la pantalla esperando. Tiene tiempo límite, se cancela al guardar y, si no hay nombre, se usa el de la dirección.
- Agregar sitio: ahora se ve una barra de carga bajo la dirección y un círculo girando sobre el icono mientras se busca el título.
- Márgenes de las barras del sistema: ahora usa EdgeToEdge de AndroidX (sin métodos obsoletos).

## Países y detección de video
- Los canales se organizan por país: en el menú hay una fila de banderas (Todos los países, Paraguay, ...) que sale de los países de los sitios guardados. En `canales.json` se pone `"pais": "PY"`.
- Al agregar un sitio, la app abre la página en un navegador oculto y busca el video (lista `.m3u8`, reproductor incrustado o `<video>`). El campo "Video detectado" está bloqueado y se llena solo; si no se encuentra, el sitio se abre con su página.
- El país se sugiere por el final de la dirección (`.com.py` = Paraguay) y se puede cambiar.
- En `canales.json`: `url` es la página del canal y `video` (opcional) el video directo.
- En "Agregar sitio", el botón "Probar video" reproduce el video detectado (sin sonido) en un recuadro. Si no se reproduce, "No usar este video" lo quita y el sitio se abre con su página.
- Si el video falla o cambió, el botón de lupa de la barra del navegador (y el aviso "No se pudo reproducir el video") y el botón "Volver a detectar video" de la pantalla de edición vuelven a buscarlo en la página; si hay uno nuevo, se guarda y se abre.

## Canales de otros países y verificación
- `canales.json` incluye canales abiertos de países de habla hispana y portuguesa (Argentina, Bolivia, Brasil, Chile, Ecuador y Paraguay). Solo quedaron los canales cuyo video la app logró detectar en la verificación; el resto se descartó, con `url` = página oficial en vivo y `pais`.
- Estos canales vienen sin `video`: la primera vez que se abre la app, `VerificadorCanales` busca el video de cada uno en segundo plano (uno por vez, hasta ~16 s cada uno), lo guarda si lo encuentra y al final muestra un informe (✓ detectado / ✗ se abre la página). Si cierras la app antes de terminar, retoma lo pendiente la próxima vez.
- Los países de habla portuguesa (Portugal, Angola, Mozambique, Cabo Verde, Guinea-Bisáu, Santo Tomé y Príncipe, Timor Oriental) y Guinea Ecuatorial y Puerto Rico se agregaron a la lista.
- Los canales que solo transmiten por YouTube no se pueden detectar: se abren con su página.
- Si la lista de un video `.m3u8` falla (por ejemplo, el canal cambió su dirección), la app lo busca de nuevo sola, una vez por apertura; si encuentra otro, lo guarda y lo abre.
- El detector de video toca el reproductor de la página (como si el usuario diera play) porque algunos, como el de SNT, solo cargan el video después del toque. Corre invisible y con el tamaño de la pantalla.
- El detector también busca listas .m3u8 escritas en la página (atributos del reproductor o código) y solo acepta una si trae canales o fragmentos y no un aviso de error. SNT ya no trae el video escrito: se detecta al abrir la app.
- SNT viene con el bloqueo de anuncios apagado (su página no carga el video si detecta el bloqueador). La búsqueda de video respeta ese interruptor de cada sitio.
- Al abrir un sitio sin video, la app intenta detectarlo primero (con Cancelar para abrir la página enseguida). Se reintenta como máximo cada 12 horas por sitio.
