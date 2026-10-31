# LoboPlay

App Android (celular y Android TV) para guardar tus sitios favoritos en un menú.

## Qué hace
- Menú en grilla con búsqueda, favoritos y navegación con control remoto.
- Agregar, editar y eliminar sitios a mano (pulsación larga sobre un sitio = opciones).
- Canales de TV abierta precargados la primera vez (archivo `app/src/main/assets/canales.json`).
- Icono de cada sitio: por defecto el de la propia página; se puede cambiar por uno propio.
- Bloqueador de anuncios siempre activo (lista en `app/src/main/assets/anuncios.txt`). En los sitios que no reproducen si lo detectan, se desactiva con "No bloquear anuncios en este sitio" (Agregar/Editar).
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

## Pantalla completa automática (canales)
- Nuevo interruptor por sitio: "Reproducir en pantalla completa al abrir" (Agregar/Editar sitio).
- Al abrir el sitio se muestra el logo, la app busca el reproductor, lo reproduce y lo pasa a pantalla completa sola.
- Atrás cierra el canal y vuelve al menú. Si no encuentra el reproductor en unos 40 segundos, muestra la página normal.
- Los canales precargados vienen con esta opción activada. La opción se guarda también en el respaldo.
- El reproductor conserva sus botones (silencio, pantalla completa, calidad). La app pasa a horizontal solo al tocar pantalla completa. Atrás sale de la pantalla completa y, estando en el canal, lo cierra.
- Interruptor por sitio "No bloquear anuncios en este sitio" (SNT lo trae activado, porque su página no reproduce si detecta el bloqueador).
- Campo opcional "Página del canal": si la dirección principal es solo el reproductor (por ejemplo `desdepylabs.com` para Telefuturo), la página del canal se usa para buscar el nombre y el icono.

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
