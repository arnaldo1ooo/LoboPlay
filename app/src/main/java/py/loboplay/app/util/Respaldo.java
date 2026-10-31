package py.loboplay.app.util;

import android.content.Context;
import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import py.loboplay.app.data.AppDatabase;
import py.loboplay.app.data.IconoHelper;
import py.loboplay.app.data.Sitio;
import py.loboplay.app.data.SitioDao;

// Guarda y recupera la lista de sitios en un archivo JSON.
public final class Respaldo {

    private Respaldo() {
    }

    // Escribe la lista de sitios en el archivo elegido y devuelve cuantos guardo.
    public static int exportar(Context ctx, Uri destino) throws Exception {
        List<Sitio> sitios = AppDatabase.get(ctx).sitioDao().lista();
        JSONArray arreglo = new JSONArray();
        for (Sitio s : sitios) {
            JSONObject o = new JSONObject();
            o.put("nombre", s.nombre);
            o.put("url", s.url);
            o.put("favorito", s.favorito);
            o.put("escritorio", s.escritorio);
            o.put("pantallaCompleta", s.pantallaCompleta);
            if (s.paginaUrl != null) o.put("pagina", s.paginaUrl);
            arreglo.put(o);
        }
        JSONObject raiz = new JSONObject();
        raiz.put("app", "LoboPlay");
        raiz.put("sitios", arreglo);
        try (OutputStream out = ctx.getContentResolver().openOutputStream(destino, "wt")) {
            if (out == null) throw new IllegalStateException("sin destino");
            out.write(raiz.toString(2).getBytes("UTF-8"));
        }
        return sitios.size();
    }

    // Lee un archivo de respaldo, agrega los sitios nuevos y devuelve cuantos agrego.
    public static int importar(Context ctx, Uri origen) throws Exception {
        String texto;
        try (InputStream in = ctx.getContentResolver().openInputStream(origen)) {
            if (in == null) throw new IllegalStateException("sin archivo");
            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int n;
            while ((n = in.read(buffer)) > 0) salida.write(buffer, 0, n);
            texto = salida.toString("UTF-8");
        }

        JSONArray arreglo = texto.trim().startsWith("[")
                ? new JSONArray(texto)
                : new JSONObject(texto).getJSONArray("sitios");

        SitioDao dao = AppDatabase.get(ctx).sitioDao();
        Set<String> existentes = new HashSet<>();
        for (Sitio s : dao.lista()) existentes.add(clave(s.url));

        List<Long> nuevos = new ArrayList<>();
        long siguienteOrden = dao.maxOrden();
        for (int i = 0; i < arreglo.length(); i++) {
            JSONObject o = arreglo.optJSONObject(i);
            if (o == null) continue;
            String nombre = o.optString("nombre", "").trim();
            String url = o.optString("url", "").trim();
            if (nombre.isEmpty() || !url.toLowerCase(Locale.ROOT).matches("^https?://.+")) continue;
            if (!existentes.add(clave(url))) continue;
            Sitio s = new Sitio();
            s.nombre = nombre;
            s.url = url;
            s.favorito = o.optBoolean("favorito", false);
            s.escritorio = o.optBoolean("escritorio", false);
            s.pantallaCompleta = o.optBoolean("pantallaCompleta", false);
            String pagina = o.optString("pagina", "").trim();
            s.paginaUrl = pagina.toLowerCase(Locale.ROOT).matches("^https?://.+") ? pagina : null;
            s.orden = ++siguienteOrden;
            nuevos.add(dao.insertar(s));
        }

        // Los iconos se bajan despues, sin hacer esperar al usuario.
        final Context app = ctx.getApplicationContext();
        for (long id : nuevos) {
            Hilos.IO.execute(() -> IconoHelper.refrescarIconoWeb(app, id));
        }
        return nuevos.size();
    }

    // Normaliza una direccion para detectar repetidas.
    private static String clave(String url) {
        String u = url == null ? "" : url.trim().toLowerCase(Locale.ROOT);
        while (u.endsWith("/")) u = u.substring(0, u.length() - 1);
        return u;
    }
}
