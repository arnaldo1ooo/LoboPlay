package py.loboplay.app.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

// Abre la pagina en un navegador oculto y busca el video: una lista .m3u8, un reproductor
// incrustado (iframe) o la direccion del <video>. Si no encuentra nada devuelve null
// y el sitio se abre con su pagina.
public final class DetectorVideo {
    public interface Escucha {
        // video = direccion encontrada, o null si no se pudo obtener.
        void alTerminar(String video);
    }

    private static final long SONDEO_MS = 1500;
    private static final long LIMITE_MS = 16000;

    // Reproductores que no se pueden abrir sueltos o que no son el video del canal.
    private static final String[] IGNORAR = {
            "youtube.com", "youtu.be", "facebook.com", "twitter.com", "x.com", "instagram.com",
            "google.com", "googletagmanager.com", "doubleclick.net", "disqus.com", "addthis.com",
            "recaptcha", "gstatic.com", "tiktok.com", "whatsapp.com"
    };

    // Busca iframes, <video> y reproductores de Dailymotion escritos en el codigo de la pagina.
    private static final String JS =
            "(function(){var r={f:[],v:[],d:[]};" +
            "function z(d){try{d.querySelectorAll('iframe').forEach(function(i){var b=i.getBoundingClientRect();r.f.push({s:i.src||i.getAttribute('data-src')||'',w:b.width,h:b.height});});" +
            "d.querySelectorAll('video').forEach(function(v){r.v.push(v.currentSrc||v.src||'');});}catch(e){}}" +
            "z(document);" +
            "try{var h=document.documentElement.innerHTML.replace(/\\\\\\//g,'/').replace(/\\\\u002F/gi,'/').replace(/&amp;/g,'&');" +
            "var m=h.match(/https?:\\/\\/(?:[a-z0-9-]+\\.)?dailymotion\\.com\\/player\\/[A-Za-z0-9]+\\.html\\?[^\"'\\\\\\s<>]*/gi)||[];" +
            "m.forEach(function(x){if(r.d.indexOf(x)<0)r.d.push(x);});}catch(e){}" +
            "return JSON.stringify(r);})()";

    private final Handler manejador = new Handler(Looper.getMainLooper());
    private final Escucha escucha;
    private final String pagina;
    private WebView web;
    private boolean terminado = false;
    private long inicio;

    private volatile String flujo;       // .m3u8 estable visto en las peticiones
    private volatile String flujoTemporal; // .m3u8 con firma que caduca (solo informativo)

    private final Runnable sondeo = this::sondear;

    private DetectorVideo(String pagina, Escucha escucha) {
        this.pagina = pagina;
        this.escucha = escucha;
    }

    // Empieza a buscar (llamar desde el hilo principal). Devuelve el detector para poder cancelarlo.
    @SuppressLint("SetJavaScriptEnabled")
    public static DetectorVideo buscar(Context ctx, String pagina, Escucha escucha) {
        DetectorVideo d = new DetectorVideo(pagina, escucha);
        d.iniciar(ctx);
        return d;
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void iniciar(Context ctx) {
        inicio = android.os.SystemClock.uptimeMillis();
        try {
            web = new WebView(ctx);
        } catch (Exception e) {
            terminar(null);
            return;
        }
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setBlockNetworkImage(true); // no hace falta bajar imagenes para buscar el video
        web.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest req) {
                Uri u = req.getUrl();
                String host = u.getHost();
                if (BloqueadorAnuncios.esAnuncio(host)) {
                    return new WebResourceResponse("text/plain", "utf-8", new ByteArrayInputStream(new byte[0]));
                }
                anotarFlujo(u);
                return null;
            }

            @Override
            public void onPageStarted(WebView v, String url, Bitmap icono) {
                // La pagina no debe sacarnos de aqui con ventanas ni aplicaciones.
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest req) {
                String esquema = req.getUrl().getScheme();
                return esquema == null || !(esquema.equals("http") || esquema.equals("https"));
            }
        });
        web.loadUrl(pagina);
        manejador.postDelayed(sondeo, SONDEO_MS * 2);
    }

    // Guarda las listas .m3u8 que la pagina va pidiendo.
    private void anotarFlujo(Uri u) {
        String ruta = u.getPath();
        if (ruta == null || !ruta.toLowerCase(Locale.ROOT).endsWith(".m3u8")) return;
        String texto = u.toString();
        if (esTemporal(u)) {
            if (flujoTemporal == null) flujoTemporal = texto;
        } else if (flujo == null) {
            // Solo vale si de verdad es una lista HLS (a veces es un anuncio o una pagina de error).
            Hilos.RED.execute(() -> {
                if (!terminado && flujo == null && esListaHls(texto)) flujo = texto;
            });
        }
    }

    // Baja el comienzo de la direccion y mira que sea una lista HLS (empieza con #EXTM3U).
    private boolean esListaHls(String direccion) {
        HttpURLConnection con = null;
        try {
            con = (HttpURLConnection) new URL(direccion).openConnection();
            con.setConnectTimeout(6000);
            con.setReadTimeout(6000);
            con.setRequestProperty("Referer", pagina);
            con.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 Chrome/120 Mobile Safari/537.36");
            if (con.getResponseCode() != 200) return false;
            try (InputStream in = con.getInputStream()) {
                byte[] buf = new byte[256];
                int n = in.read(buf);
                if (n <= 0) return false;
                return new String(buf, 0, n, "UTF-8").trim().startsWith("#EXTM3U");
            }
        } catch (Exception e) {
            return false;
        } finally {
            if (con != null) con.disconnect();
        }
    }

    // Direcciones con firma que caduca: no sirven para guardarlas.
    private static boolean esTemporal(Uri u) {
        String q = u.getEncodedQuery();
        if (q == null) return false;
        String t = q.toLowerCase(Locale.ROOT);
        return t.contains("token") || t.contains("expires") || t.contains("signature")
                || t.contains("hdnts") || t.contains("hdntl") || t.contains("sig=")
                || t.contains("exp=") || t.contains("policy=") || t.contains("key-pair-id");
    }

    private void sondear() {
        if (terminado) return;
        if (flujo != null) {
            terminar(flujo);
            return;
        }
        long pasado = android.os.SystemClock.uptimeMillis() - inicio;
        final boolean ultimo = pasado >= LIMITE_MS;
        web.evaluateJavascript(JS, valor -> {
            if (terminado) return;
            String hallado = flujo != null ? flujo : elegir(valor);
            // Un reproductor incrustado se acepta al final por si antes aparece un .m3u8 directo.
            if (flujo != null) terminar(flujo);
            else if (ultimo) terminar(hallado);
        });
        if (ultimo) {
            // Por si el JS no responde.
            manejador.postDelayed(() -> terminar(flujo), 2000);
        } else {
            manejador.postDelayed(sondeo, SONDEO_MS);
        }
    }

    // De lo que devolvio la pagina elige el mejor candidato (iframe grande, luego <video>).
    private String elegir(String valor) {
        try {
            if (valor == null || valor.equals("null")) return null;
            String json = new org.json.JSONTokener(valor).nextValue().toString();
            JSONObject o = new JSONObject(json);
            JSONArray d = o.optJSONArray("d");
            if (d != null && d.length() > 0) return d.getString(0);
            String mejor = null;
            double area = 0;
            JSONArray f = o.getJSONArray("f");
            for (int i = 0; i < f.length(); i++) {
                JSONObject e = f.getJSONObject(i);
                String src = e.optString("s", "");
                if (!src.toLowerCase(Locale.ROOT).startsWith("http") || ignorado(src)) continue;
                double a = e.optDouble("w", 0) * e.optDouble("h", 0);
                if (e.optDouble("w", 0) < 200 || e.optDouble("h", 0) < 110) continue;
                if (a > area) {
                    area = a;
                    mejor = src;
                }
            }
            if (mejor != null) return mejor;
            JSONArray v = o.getJSONArray("v");
            for (int i = 0; i < v.length(); i++) {
                String src = v.optString(i, "");
                if (!src.toLowerCase(Locale.ROOT).startsWith("http")) continue; // blob: no sirve
                Uri u = Uri.parse(src);
                if (!esTemporal(u)) return src;
            }
        } catch (Exception ignorar) {
        }
        return null;
    }

    private static boolean ignorado(String src) {
        String host = Uri.parse(src).getHost();
        if (host == null) return true;
        host = host.toLowerCase(Locale.ROOT);
        if (BloqueadorAnuncios.esAnuncio(host)) return true;
        for (String x : IGNORAR) {
            if (host.equals(x) || host.endsWith("." + x) || host.contains(x)) return true;
        }
        return false;
    }

    // Ya termino (con resultado, cancelado o por un fallo al iniciar).
    public boolean finalizado() {
        return terminado;
    }

    // Cancela la busqueda sin avisar.
    public void cancelar() {
        if (terminado) return;
        terminado = true;
        liberar();
    }

    private void terminar(String video) {
        if (terminado) return;
        terminado = true;
        liberar();
        escucha.alTerminar(video);
    }

    private void liberar() {
        manejador.removeCallbacksAndMessages(null);
        if (web != null) {
            try {
                web.stopLoading();
                web.loadUrl("about:blank");
                web.destroy();
            } catch (Exception ignorar) {
            }
            web = null;
        }
    }
}
