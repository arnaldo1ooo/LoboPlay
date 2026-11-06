package py.loboplay.app.util;

import android.content.Context;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

// Reconoce direcciones de publicidad para bloquearlas.
public final class BloqueadorAnuncios {
    private static final String LISTA_URL =
            "https://pgl.yoyo.org/adservers/serverlist.php?hostformat=nohtml&showintro=0&mimetype=plaintext";
    private static final String ARCHIVO_EXTRA = "anuncios_extra.txt";
    private static Set<String> dominios;

    private BloqueadorAnuncios() {
    }

    // Carga la lista de dominios una sola vez (la de la app y la descargada).
    public static synchronized void cargar(Context ctx) {
        if (dominios != null) return;
        Set<String> lista = new HashSet<>();
        try (InputStream in = ctx.getAssets().open("anuncios.txt")) {
            leer(in, lista);
        } catch (Exception ignorado) {
            // sin lista propia se sigue con la descargada
        }
        File extra = new File(ctx.getFilesDir(), ARCHIVO_EXTRA);
        if (extra.exists()) {
            try (InputStream in = new FileInputStream(extra)) {
                leer(in, lista);
            } catch (Exception ignorado) {
                // archivo danado: se ignora
            }
        }
        dominios = lista;
    }

    // Vuelve a leer las listas (despues de actualizar).
    public static synchronized void recargar(Context ctx) {
        dominios = null;
        cargar(ctx);
    }

    // Lee un dominio por linea.
    private static void leer(InputStream in, Set<String> destino) throws Exception {
        try (BufferedReader lector = new BufferedReader(new InputStreamReader(in, "UTF-8"))) {
            String linea;
            while ((linea = lector.readLine()) != null) {
                linea = linea.trim().toLowerCase(Locale.ROOT);
                if (!linea.isEmpty() && !linea.startsWith("#")) destino.add(linea);
            }
        }
    }

    // Descarga una lista nueva de dominios y devuelve cuantos hay.
    public static int actualizar(Context ctx) throws Exception {
        Set<String> nuevos = new HashSet<>();
        HttpURLConnection con = (HttpURLConnection) new URL(LISTA_URL).openConnection();
        con.setConnectTimeout(10000);
        con.setReadTimeout(15000);
        try (BufferedReader lector = new BufferedReader(new InputStreamReader(con.getInputStream(), "UTF-8"))) {
            String linea;
            while ((linea = lector.readLine()) != null) {
                int comentario = linea.indexOf('#');
                if (comentario >= 0) linea = linea.substring(0, comentario);
                String[] partes = linea.trim().split("\\s+");
                String dominio = partes[partes.length - 1].toLowerCase(Locale.ROOT);
                if (dominio.contains(".") && !dominio.contains("/") && !dominio.contains(":")) {
                    nuevos.add(dominio);
                }
            }
        } finally {
            con.disconnect();
        }
        // Una lista muy corta es senal de error: no se pisa la anterior.
        if (nuevos.size() < 100) throw new IllegalStateException("lista incompleta");

        File temporal = new File(ctx.getFilesDir(), ARCHIVO_EXTRA + ".tmp");
        try (FileOutputStream out = new FileOutputStream(temporal)) {
            StringBuilder texto = new StringBuilder();
            for (String d : nuevos) texto.append(d).append('\n');
            out.write(texto.toString().getBytes("UTF-8"));
        }
        File destino = new File(ctx.getFilesDir(), ARCHIVO_EXTRA);
        if (destino.exists() && !destino.delete()) throw new IllegalStateException("no se pudo reemplazar");
        if (!temporal.renameTo(destino)) throw new IllegalStateException("no se pudo guardar");
        recargar(ctx);
        return nuevos.size();
    }

    // Dice si la direccion pertenece a un dominio de publicidad.
    public static boolean esAnuncio(String host) {
        if (dominios == null || host == null) return false;
        String h = host.toLowerCase(Locale.ROOT);
        while (true) {
            if (dominios.contains(h)) return true;
            int punto = h.indexOf('.');
            if (punto < 0) return false;
            h = h.substring(punto + 1);
        }
    }
}
