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
- Pantalla de bienvenida (logo, nombre y lema) al abrir la app.
- TV: la selección del control remoto ya no se pierde cuando cambian los datos (la lista usa DiffUtil y los chips de países solo se rehacen si cambian los países).

## Iconos y bienvenida (ajuste)
- La pantalla de bienvenida ahora tiene elevación, así el botón "Agregar sitio" ya no se ve encima.
- `IconoHelper.completarFaltantes` reintenta el icono de los sitios sin icono (al abrir la app y al terminar la verificación), con tiempos de espera mayores.
- Se quitó el diálogo "Verificación de canales"; la verificación sigue en segundo plano.

## Cambiar de canal sin volver a la lista
- Al abrir un canal, el reproductor recibe la lista visible (respeta filtro de país, búsqueda y orden) en `EXTRA_LISTA`.
- Botones anterior/siguiente en la barra superior (solo si hay más de un canal) y, en el control remoto, las teclas CH+/CH− o Page Up/Down.
- Es circular (del último pasa al primero) y marca el canal como reciente. Un canal sin video detectado se abre con su página.

### Cambio de canal con las flechas (TV)
- Video propio (HLS): (reemplazado más abajo: ahora Arriba/Abajo) si la barra está oculta o el cursor activo.
- Página con cursor: empujar el cursor contra el borde izquierdo/derecho de la pantalla pasa al canal anterior/siguiente (arriba/abajo siguen desplazando la página).

## Comentarios (formulario de Google)
- Ajustes > Ayuda > Enviar comentarios, y "Enviar comentario sobre este canal" en el menú de pulsación larga de cada canal.
- Campos: tipo, canal, mensaje (máx. 500, sin enlaces; opcional si hay canal elegido), contacto opcional. Se adjuntan solos versión de la app, Android, modelo, TV/teléfono y, si hay canal, su URL, país y video detectado.
- `util/Comentarios.java` hace el POST a `formResponse` con los códigos `entry.…` (están al inicio del archivo; si se cambian los campos del formulario, actualizarlos ahí). Sin conexión, el comentario queda en cola (máx. 20) y se reintenta al abrir la app. Límite: un envío cada 10 s.

## Brillo con el dedo
- En el reproductor (solo teléfonos/tablets), deslizar arriba o abajo por el tercio izquierdo de la pantalla cambia el brillo de la ventana; muestra el porcentaje y no afecta al brillo del sistema. El nivel se mantiene al cambiar de canal.
- La primera vez que se abre un canal, un panel sobre esa zona explica el gesto (se cierra solo, o al tocarlo).
- Se desactiva en Ajustes > Brillo con el dedo (`gesto_brillo`). Un toque normal en esa zona sigue llegando a la página; solo el arrastre vertical se usa para el brillo.
- Código en `ui/GestoBrillo.java`.

## Barra abajo y cursor según el contenido
- La barra de opciones del reproductor ahora está **abajo**. Se muestra tocando el borde inferior (teléfono), con Abajo en el control remoto (si no hay cursor) o llevando el cursor al fondo de una página que ya no se desplaza. Arriba/abajo del cursor siguen desplazando la página.
- El gesto de brillo ignora la zona de la barra mientras está visible.
- Con video propio (.m3u8) el cursor arranca apagado (Arriba/Abajo cambian de canal); en páginas web sigue encendido según el ajuste "Cursor en TV". El botón de la barra lo activa o apaga.

## TV: arriba/abajo cambian de canal
- Video propio: Arriba = canal siguiente, Abajo = canal anterior (con la barra oculta o el cursor activo). OK muestra la barra (con el foco en "siguiente"); Izquierda/Derecha quedan libres.
- Páginas web con cursor: sin cambios (Arriba/Abajo mueven el cursor; cambiar de canal empujando el cursor contra el borde izquierdo/derecho; Abajo del todo pasa a la barra).
- CH+/CH− y Page Up/Down siguen funcionando siempre.

## Barra fija en vertical y tema claro/oscuro
- En teléfono con la pantalla vertical la barra del reproductor queda siempre visible; en horizontal se esconde sola como antes (`barraFija()` y `onConfigurationChanged` en `NavegadorActivity`). En TV no cambia.
- Ajustes > Apariencia > Tema: "Como el sistema", "Claro" u "Oscuro". `LoboPlayApp` (registrada en el manifiesto) lo aplica al abrir la app y el ajuste lo aplica al instante (`AppCompatDelegate.setDefaultNightMode`).

- Se reemplazó `overridePendingTransition` (obsoleta desde Android 14) por `overrideActivityTransition` en `NavegadorActivity.sinAnimacion()`, con la versión antigua para Android < 14.
