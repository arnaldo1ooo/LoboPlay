package py.loboplay.app.util;

import android.content.Context;
import android.net.Uri;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Locale;

// Paginas propias que reproducen un video ajeno (lo usan el navegador y la vista previa).
public final class PaginasVideo {
    private PaginasVideo() {
    }

    // Dice si la direccion es un flujo HLS (lista .m3u8).
    public static boolean esFlujoHls(String url) {
        String ruta = Uri.parse(url).getPath();
        return ruta != null && ruta.toLowerCase(Locale.ROOT).endsWith(".m3u8");
    }

    // Dice si la direccion es un reproductor de Dailymotion.
    public static boolean esDailymotion(String url) {
        String host = Uri.parse(url).getHost();
        return host != null && (host.equals("dailymotion.com") || host.endsWith(".dailymotion.com"));
    }

    // Origen que se le muestra al servidor del video: la web del canal, o el propio servidor si no hay.
    public static String baseDe(String url, String pagina) {
        return pagina != null && pagina.toLowerCase(Locale.ROOT).matches("^https?://.+")
                ? pagina : Uri.parse(url).getScheme() + "://" + Uri.parse(url).getHost() + "/";
    }

    // Pagina con el reproductor ajeno dentro de un marco, como lo ve la web del canal.
    public static String marco(String reproductor, boolean silencio) {
        String src = reproductor.contains("autoplay=") ? reproductor
                : reproductor + (reproductor.contains("?") ? "&" : "?") + "autoplay=1";
        if (silencio && !src.contains("mute=")) src += "&mute=1";
        return "<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1'>"
                + "<style>html,body{margin:0;height:100%;background:#000;overflow:hidden}"
                + "iframe{border:0;width:100vw;height:100vh;display:block}</style></head><body>"
                + "<iframe src=" + JSONObject.quote(src).replace("\"", "'") + " allow='autoplay; fullscreen; "
                + "picture-in-picture; encrypted-media; web-share' allowfullscreen "
                + "referrerpolicy='strict-origin-when-cross-origin'></iframe></body></html>";
    }

    // Pagina que reproduce un flujo .m3u8 con hls.js (assets/hls.min.js o, si falta, desde internet).
    // Si la pagina tiene una interfaz "lp" avisa con lp.ok() o lp.fallo(texto) (la usa la vista previa).
    public static String flujo(Context ctx, String flujo, boolean silencio) {
        String hls = leerAssetOpcional(ctx, "hls.min.js").replace("</script", "<\\/script");
        String libreria = hls.trim().isEmpty()
                ? "<script src='https://cdn.jsdelivr.net/npm/hls.js@1.5/dist/hls.min.js'></script>"
                : "<script>" + hls + "</script>";
        return "<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1'>"
                + "<style>html,body{margin:0;height:100%;background:#000;overflow:hidden}"
                + "video{width:100vw;height:100vh;background:#000;object-fit:contain}"
                + "#e{position:fixed;left:0;right:0;top:0;padding:8px;text-align:center;z-index:9;"
                + "color:#9fb6d6;font:13px sans-serif;pointer-events:none}</style></head><body>"
                + "<video id='v' controls autoplay playsinline" + (silencio ? " muted" : "") + "></video><div id='e'></div>"
                + libreria
                + "<script>(function(){var v=document.getElementById('v'),e=document.getElementById('e'),"
                + "src=" + JSONObject.quote(flujo) + ";"
                + "function msg(t){e.textContent=t;}"
                + "function aviso(f,t){try{if(window.lp)lp[f](t||'');}catch(x){}}"
                + "function intentar(){var p=v.play();if(p&&p.catch)p.catch(function(){});}"
                + "function nativo(){v.src=src;intentar();}"
                + "v.addEventListener('playing',function(){msg('');aviso('ok');});"
                + "v.addEventListener('error',function(){var t='No se pudo reproducir (video '+(v.error?v.error.code:'?')+')';msg(t);aviso('fallo',t);});"
                + "if(window.Hls&&Hls.isSupported()){var h=new Hls();h.loadSource(src);h.attachMedia(v);"
                + "h.on(Hls.Events.MANIFEST_PARSED,intentar);"
                + "h.on(Hls.Events.ERROR,function(ev,d){var t='Error: '+d.details+(d.response?' ('+d.response.code+')':'');msg(t);"
                + "if(!d.fatal)return;aviso('fallo',t);"
                + "if(d.type===Hls.ErrorTypes.NETWORK_ERROR)setTimeout(function(){h.startLoad();},2000);"
                + "else if(d.type===Hls.ErrorTypes.MEDIA_ERROR)h.recoverMediaError();"
                + "else{h.destroy();nativo();}});}"
                + "else{msg('Sin hls.js: usando reproduccion nativa');nativo();}})();</script></body></html>";
    }

    // Lee un archivo de assets si existe (si no, devuelve texto vacio).
    private static String leerAssetOpcional(Context ctx, String nombre) {
        try (InputStream in = ctx.getAssets().open(nombre)) {
            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int n;
            while ((n = in.read(buffer)) > 0) salida.write(buffer, 0, n);
            return salida.toString("UTF-8");
        } catch (Exception e) {
            return "";
        }
    }
}
