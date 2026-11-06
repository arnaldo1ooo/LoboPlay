package py.loboplay.app.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.Map;

// Envia los comentarios a un formulario de Google. Si no hay internet, los guarda y los reintenta despues.
public final class Comentarios {
    private static final String FORMULARIO =
            "https://docs.google.com/forms/d/e/1FAIpQLSfR3GPwdjL3uMHxXjA0Vuwr3JQaCFttYAUMTassjufEafGK1w/formResponse";

    // Codigos de cada campo del formulario.
    public static final String TIPO = "entry.2126958350";
    public static final String CANAL = "entry.1245988968";
    public static final String MENSAJE = "entry.647324269";
    public static final String CONTACTO = "entry.597668868";
    public static final String VERSION = "entry.1057244427";
    public static final String DISPOSITIVO = "entry.441666054";
    public static final String DATOS_CANAL = "entry.1502928404";

    private static final String PREFS = "comentarios";
    private static final String PENDIENTES = "pendientes";
    private static final int MAX_PENDIENTES = 20;
    private static final int MAX_CAMPO = 1500;

    private static long ultimoEnvio;
    private static boolean reintentando;

    private Comentarios() {
    }

    // Versiones y modelo que se adjuntan solos.
    public static void agregarDatosTecnicos(Context ctx, Map<String, String> campos, boolean tv) {
        String version = "?";
        try {
            PackageInfo info = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0);
            version = info.versionName;
        } catch (Exception ignorado) {
            // se deja el signo de pregunta
        }
        campos.put(VERSION, version);
        campos.put(DISPOSITIVO, Build.MANUFACTURER + " " + Build.MODEL + " · Android " + Build.VERSION.RELEASE
                + " · " + (tv ? "TV" : "Teléfono"));
    }

    // Evita enviar varios comentarios seguidos (llamar desde el hilo principal).
    public static boolean puedeEnviarAhora() {
        long ahora = System.currentTimeMillis();
        if (ahora - ultimoEnvio < 10000) return false;
        ultimoEnvio = ahora;
        return true;
    }

    // Intenta enviar; si no puede, lo deja en cola. Devuelve true si llego al formulario.
    public static boolean enviarOGuardar(Context ctx, Map<String, String> campos) {
        if (enviar(campos)) return true;
        guardar(ctx.getApplicationContext(), campos);
        return false;
    }

    // Reintenta los comentarios que quedaron pendientes (hilo de fondo).
    public static void reintentarPendientes(Context ctx) {
        Context app = ctx.getApplicationContext();
        synchronized (Comentarios.class) {
            if (reintentando) return;
            reintentando = true;
        }
        try {
            SharedPreferences sp = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            JSONArray lista = new JSONArray(sp.getString(PENDIENTES, "[]"));
            if (lista.length() == 0) return;
            JSONArray quedan = new JSONArray();
            boolean sinRed = false;
            for (int i = 0; i < lista.length(); i++) {
                JSONObject o = lista.getJSONObject(i);
                if (sinRed) {
                    quedan.put(o);
                    continue;
                }
                Map<String, String> campos = new LinkedHashMap<>();
                java.util.Iterator<String> claves = o.keys();
                while (claves.hasNext()) {
                    String k = claves.next();
                    campos.put(k, o.optString(k));
                }
                if (!enviar(campos)) {
                    sinRed = true;
                    quedan.put(o);
                }
            }
            sp.edit().putString(PENDIENTES, quedan.toString()).apply();
        } catch (Exception ignorado) {
            // se reintenta la proxima vez
        } finally {
            reintentando = false;
        }
    }

    private static boolean enviar(Map<String, String> campos) {
        HttpURLConnection con = null;
        try {
            StringBuilder cuerpo = new StringBuilder();
            for (Map.Entry<String, String> e : campos.entrySet()) {
                String v = e.getValue() == null ? "" : e.getValue();
                if (v.length() > MAX_CAMPO) v = v.substring(0, MAX_CAMPO);
                if (cuerpo.length() > 0) cuerpo.append('&');
                cuerpo.append(URLEncoder.encode(e.getKey(), "UTF-8")).append('=')
                        .append(URLEncoder.encode(v, "UTF-8"));
            }
            byte[] datos = cuerpo.toString().getBytes("UTF-8");
            con = (HttpURLConnection) new URL(FORMULARIO).openConnection();
            con.setConnectTimeout(10000);
            con.setReadTimeout(10000);
            con.setInstanceFollowRedirects(false);
            con.setDoOutput(true);
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            try (OutputStream out = con.getOutputStream()) {
                out.write(datos);
            }
            int codigo = con.getResponseCode();
            return codigo >= 200 && codigo < 400;
        } catch (Exception e) {
            return false;
        } finally {
            if (con != null) con.disconnect();
        }
    }

    private static synchronized void guardar(Context app, Map<String, String> campos) {
        try {
            SharedPreferences sp = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            JSONArray lista = new JSONArray(sp.getString(PENDIENTES, "[]"));
            JSONObject o = new JSONObject();
            for (Map.Entry<String, String> e : campos.entrySet()) o.put(e.getKey(), e.getValue());
            lista.put(o);
            JSONArray recorte = new JSONArray();
            for (int i = Math.max(0, lista.length() - MAX_PENDIENTES); i < lista.length(); i++) {
                recorte.put(lista.get(i));
            }
            sp.edit().putString(PENDIENTES, recorte.toString()).apply();
        } catch (Exception ignorado) {
            // sin cola no hay nada mas que hacer
        }
    }
}
