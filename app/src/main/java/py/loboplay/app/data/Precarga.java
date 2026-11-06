package py.loboplay.app.data;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import py.loboplay.app.util.Ajustes;
import py.loboplay.app.util.Hilos;

// Carga los canales iniciales la primera vez que se abre la app.
public final class Precarga {

    private Precarga() {
    }

    // Agrega los canales de canales.json si todavia no se hizo. Al terminar (o si ya estaba hecho)
    // avisa en el hilo principal, para que la app busque el video de los canales que no lo traen.
    public static void ejecutarSiHaceFalta(Context ctx, Runnable alTerminar) {
        final Context app = ctx.getApplicationContext();
        final android.os.Handler principal = new android.os.Handler(android.os.Looper.getMainLooper());
        if (Ajustes.yaPrecargado(app)) {
            principal.post(alTerminar);
            return;
        }
        Hilos.IO.execute(() -> {
            try {
                JSONArray canales = new JSONArray(leerAsset(app, "canales.json"));
                SitioDao dao = AppDatabase.get(app).sitioDao();
                List<Long> ids = new ArrayList<>();
                List<Long> sinVideo = new ArrayList<>();
                for (int i = 0; i < canales.length(); i++) {
                    JSONObject o = canales.getJSONObject(i);
                    Sitio s = new Sitio();
                    s.nombre = o.getString("nombre");
                    s.url = o.getString("url");
                    s.bloquearAnuncios = o.optBoolean("bloquearAnuncios", true);
                    s.videoUrl = o.has("video") ? o.getString("video") : null;
                    s.pais = o.has("pais") ? o.getString("pais") : null;
                    s.orden = i + 1;
                    long id = dao.insertar(s);
                    ids.add(id);
                    if (s.videoUrl == null) sinVideo.add(id);
                }
                Ajustes.guardarPendientes(app, sinVideo);
                Ajustes.marcarPrecargado(app);
                principal.post(alTerminar);
                for (long id : ids) IconoHelper.refrescarIconoWeb(app, id);
            } catch (Exception ignorado) {
                // se reintenta la proxima vez
            }
        });
    }

    // Lee un archivo de texto de assets.
    private static String leerAsset(Context ctx, String nombre) throws Exception {
        try (InputStream in = ctx.getAssets().open(nombre)) {
            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            byte[] buffer = new byte[2048];
            int n;
            while ((n = in.read(buffer)) > 0) salida.write(buffer, 0, n);
            return salida.toString("UTF-8");
        }
    }
}
