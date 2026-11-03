package py.loboplay.app.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.ViewGroup;
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
    private static final long LIMITE_MS = 20000;

    // Reproductores que no se pueden abrir sueltos o que no son el video del canal.
    private static final String[] IGNORAR = {
            "youtube.com", "youtu.be", "facebook.com", "twitter.com", "x.com", "instagram.com",
            "google.com", "googletagmanager.com", "doubleclick.net", "disqus.com", "addthis.com",
            "recaptcha", "gstatic.com", "tiktok.com", "whatsapp.com"
    };

    // Busca iframes, <video> y reproductores de Dailymotion escritos en el codigo de la pagina.
    private static final String JS =
            "(function(){var r={f:[],v:[],d:[],m:[]};" +
            "function lpT(s,q,a){[].push.apply(a,q.querySelectorAll(s));[].slice.call(q.querySelectorAll('*')).forEach(function(e){if(e.shadowRoot)lpT(s,e.shadowRoot,a);});return a;}" +
            "function z(d){try{d.querySelectorAll('iframe').forEach(function(i){var b=i.getBoundingClientRect();r.f.push({s:i.src||i.getAttribute('data-src')||'',w:b.width,h:b.height});});" +
            "d.querySelectorAll('video').forEach(function(v){r.v.push(v.currentSrc||v.src||'');});}catch(e){}}" +
            "z(document);" +
            "var h=document.documentElement.innerHTML.replace(/\\\\\\//g,'/').replace(/\\\\u002F/gi,'/').replace(/&amp;/g,'&');" +
            "try{var m=h.match(/https?:\\/\\/(?:[a-z0-9-]+\\.)?dailymotion\\.com\\/player\\/[A-Za-z0-9]+\\.html\\?[^\"'\\\\\\s<>]*/gi)||[];" +
            "m.forEach(function(x){if(r.d.indexOf(x)<0)r.d.push(x);});}catch(e){}" +
            "try{function am(x){if(x&&/\\.m3u8(\\?|$)/i.test(x)&&r.m.indexOf(x)<0)r.m.push(x);}" +
            "lpT('*',document,[]).forEach(function(e){['src','data-src','data-srcset','srcset','data-stream','data-url','data-hls','href'].forEach(function(a){var v=e.getAttribute&&e.getAttribute(a);if(v)v.split(',').forEach(function(y){am(y.trim().split(' ')[0]);});});});" +
            "(h.match(/https?:\\/\\/[^\"'\\\\\\s<>)]+\\.m3u8[^\"'\\\\\\s<>)]*/gi)||[]).forEach(am);}catch(e){}" +
            "try{var W=window.innerWidth,H=window.innerHeight,best=null,ba=0;" +
            "lpT('video,ned-player,iframe,[class*=player]',document,[]).forEach(function(e){var b=e.getBoundingClientRect(),a=b.width*b.height;" +
            "if(b.width>=150&&b.height>=90&&b.width<=W+2&&b.height<=H*0.9&&a>ba){ba=a;best={x:b.left+b.width/2,y:b.top+b.height/2};}});" +
            "if(best)r.p=best;}catch(e){}" +
            "return JSON.stringify(r);})()";

    private final Handler manejador = new Handler(Looper.getMainLooper());
    private final Escucha escucha;
    private final String pagina;
    private WebView web;
    private boolean terminado = false;
    private long inicio;
    private int sondeos;
    private boolean cargada;   // termino la carga inicial: desde ahi la pagina no puede llevarnos a otra

    private volatile String flujo;       // .m3u8 estable visto en las peticiones
    private volatile String flujoTemporal; // .m3u8 con firma que caduca (solo informativo)

    private final Runnable sondeo = this::sondear;

    private final boolean bloquear;   // bloquear anuncios mientras se busca (segun el ajuste del sitio)

    private DetectorVideo(String pagina, boolean bloquear, Escucha escucha) {
        this.bloquear = bloquear;
        this.pagina = pagina;
        this.escucha = escucha;
    }

    // Empieza a buscar (llamar desde el hilo principal). Devuelve el detector para poder cancelarlo.
    @SuppressLint("SetJavaScriptEnabled")
    // bloquear: igual que el interruptor del sitio (algunos sitios no cargan el video si ven el bloqueador).
    public static DetectorVideo buscar(Context ctx, String pagina, boolean bloquear, Escucha escucha) {
        DetectorVideo d = new DetectorVideo(pagina, bloquear, escucha);
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
        // Con tamano de pantalla (y puesto debajo de la app, invisible), para que la pagina arme el
        // reproductor como en un celular y se le puedan hacer toques.
        DisplayMetrics dm = ctx.getResources().getDisplayMetrics();
        if (ctx instanceof Activity) {
            web.setAlpha(0f);
            ((ViewGroup) ((Activity) ctx).getWindow().getDecorView()).addView(web, 0,
                    new ViewGroup.LayoutParams(dm.widthPixels, dm.heightPixels));
        } else {
            web.measure(android.view.View.MeasureSpec.makeMeasureSpec(dm.widthPixels, android.view.View.MeasureSpec.EXACTLY),
                    android.view.View.MeasureSpec.makeMeasureSpec(dm.heightPixels, android.view.View.MeasureSpec.EXACTLY));
            web.layout(0, 0, dm.widthPixels, dm.heightPixels);
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
                if (bloquear && BloqueadorAnuncios.esAnuncio(host)) {
                    return new WebResourceResponse("text/plain", "utf-8", new ByteArrayInputStream(new byte[0]));
                }
                anotarFlujo(u);
                return null;
            }

            @Override
            public void onPageFinished(WebView v, String url) {
                cargada = true;
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest req) {
                String esquema = req.getUrl().getScheme();
                if (esquema == null || !(esquema.equals("http") || esquema.equals("https"))) return true;
                // Despues de cargar, un toque no debe llevarnos a otra pagina (por ejemplo un anuncio).
                return cargada && req.isForMainFrame();
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
                byte[] buf = new byte[4096];
                int total = 0;
                int n;
                while (total < buf.length && (n = in.read(buf, total, buf.length - total)) > 0) total += n;
                if (total <= 0) return false;
                String t = new String(buf, 0, total, "UTF-8").trim();
                // Lista real: trae canales/fragmentos y no un aviso de error (ej. "#EXT-X-ERROR: Stream open failed").
                return t.startsWith("#EXTM3U") && !t.contains("#EXT-X-ERROR")
                        && (t.contains("#EXT-X-STREAM-INF") || t.contains("#EXTINF"));
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
        final int tic = ++sondeos;
        web.evaluateJavascript(JS, valor -> {
            if (terminado) return;
            // Muchos reproductores (como el de SNT) solo cargan el video cuando se toca play.
            if (tic == 2 || tic == 5) tocarReproductor(valor);
            probarListasDeLaPagina(valor);
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

    private final java.util.Set<String> probadas = new java.util.HashSet<>();

    // Listas .m3u8 escritas en la pagina (atributos src o codigo): se comprueban y la primera valida gana.
    private void probarListasDeLaPagina(String valor) {
        try {
            if (valor == null || valor.equals("null") || flujo != null) return;
            JSONArray m = new JSONObject(new org.json.JSONTokener(valor).nextValue().toString()).optJSONArray("m");
            if (m == null) return;
            for (int i = 0; i < m.length() && i < 6; i++) {
                final String u;
                try {
                    u = new URL(new URL(pagina), m.getString(i)).toString();
                } catch (Exception e) {
                    continue;
                }
                if (!probadas.add(u) || esTemporal(Uri.parse(u))) continue;
                Hilos.RED.execute(() -> {
                    if (!terminado && flujo == null && esListaHls(u)) flujo = u;
                });
            }
        } catch (Exception ignorar) {
        }
    }

    // Toca el centro del reproductor mas grande de la pagina, como lo haria el usuario.
    private void tocarReproductor(String valor) {
        try {
            if (valor == null || web == null) return;
            JSONObject p = new JSONObject(new org.json.JSONTokener(valor).nextValue().toString()).optJSONObject("p");
            if (p == null) return;
            float d = web.getResources().getDisplayMetrics().density;
            final float x = (float) p.getDouble("x") * d;
            final float y = (float) p.getDouble("y") * d;
            long t = android.os.SystemClock.uptimeMillis();
            MotionEvent abajo = MotionEvent.obtain(t, t, MotionEvent.ACTION_DOWN, x, y, 0);
            web.dispatchTouchEvent(abajo);
            abajo.recycle();
            manejador.postDelayed(() -> {
                if (web == null || terminado) return;
                long t2 = android.os.SystemClock.uptimeMillis();
                MotionEvent arriba = MotionEvent.obtain(t2, t2, MotionEvent.ACTION_UP, x, y, 0);
                web.dispatchTouchEvent(arriba);
                arriba.recycle();
            }, 60);
        } catch (Exception ignorar) {
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
                if (web.getParent() instanceof ViewGroup) ((ViewGroup) web.getParent()).removeView(web);
                web.destroy();
            } catch (Exception ignorar) {
            }
            web = null;
        }
    }
}
