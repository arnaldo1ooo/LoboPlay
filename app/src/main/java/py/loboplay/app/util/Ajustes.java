package py.loboplay.app.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

// Lee y guarda las opciones de la app.
public final class Ajustes {
    public static final String EXTERNO = "abrir_externo";
    public static final String PIP = "pip_activado";
    public static final String CURSOR_TV = "cursor_tv";
    private static final String PRECARGADO = "precargado";
    private static final String PENDIENTES = "verificar_pendientes";

    private Ajustes() {
    }

    private static SharedPreferences prefs(Context c) {
        return PreferenceManager.getDefaultSharedPreferences(c);
    }

    // Dice si los sitios se abren en el navegador externo.
    public static boolean abrirExterno(Context c) {
        return prefs(c).getBoolean(EXTERNO, false);
    }

    // Dice si la imagen en imagen esta permitida.
    public static boolean pipActivado(Context c) {
        return prefs(c).getBoolean(PIP, true);
    }

    // Dice si se usa el cursor con el control remoto en la TV.
    public static boolean cursorTv(Context c) {
        return prefs(c).getBoolean(CURSOR_TV, true);
    }

    // Tema elegido: "sistema", "claro" u "oscuro".
    public static int modoTema(Context c) {
        return aModo(prefs(c).getString("tema", "sistema"));
    }

    public static int aModo(String valor) {
        if ("claro".equals(valor)) return androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO;
        if ("oscuro".equals(valor)) return androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES;
        return androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
    }

    // Dice si el brillo se cambia deslizando por el lado izquierdo del reproductor.
    public static boolean gestoBrillo(Context c) {
        return prefs(c).getBoolean("gesto_brillo", true);
    }

    // Dice si el volumen se cambia deslizando por el lado derecho del reproductor.
    public static boolean gestoVolumen(Context c) {
        return prefs(c).getBoolean("gesto_volumen", true);
    }

    public static boolean gestoVolumenVisto(Context c) {
        return prefs(c).getBoolean("gesto_volumen_visto", false);
    }

    public static void marcarGestoVolumenVisto(Context c) {
        prefs(c).edit().putBoolean("gesto_volumen_visto", true).apply();
    }

    public static boolean gestoBrilloVisto(Context c) {
        return prefs(c).getBoolean("gesto_brillo_visto", false);
    }

    public static void marcarGestoBrilloVisto(Context c) {
        prefs(c).edit().putBoolean("gesto_brillo_visto", true).apply();
    }

    // Dice si ya se cargaron los canales iniciales.
    public static boolean yaPrecargado(Context c) {
        return prefs(c).getBoolean(PRECARGADO, false);
    }

    // Canales precargados a los que todavia se les busca el video (ids separados por coma).
    public static java.util.List<Long> pendientesVerificar(Context c) {
        java.util.List<Long> ids = new java.util.ArrayList<>();
        String t = prefs(c).getString(PENDIENTES, "");
        for (String x : t.split(",")) {
            try {
                if (!x.trim().isEmpty()) ids.add(Long.parseLong(x.trim()));
            } catch (NumberFormatException ignorado) {
            }
        }
        return ids;
    }

    public static void guardarPendientes(Context c, java.util.List<Long> ids) {
        StringBuilder sb = new StringBuilder();
        for (long id : ids) sb.append(sb.length() == 0 ? "" : ",").append(id);
        prefs(c).edit().putString(PENDIENTES, sb.toString()).apply();
    }

    public static void quitarPendiente(Context c, long id) {
        java.util.List<Long> ids = pendientesVerificar(c);
        ids.remove(Long.valueOf(id));
        guardarPendientes(c, ids);
    }

    private static final long ESPERA_BUSQUEDA_MS = 12L * 60 * 60 * 1000;   // 12 horas

    // Un sitio sin video se vuelve a intentar al abrirlo, pero no mas de una vez cada 12 horas.
    public static boolean puedeBuscarVideo(Context c, long id) {
        long ultimo = prefs(c).getLong("intento_video_" + id, 0);
        return System.currentTimeMillis() - ultimo > ESPERA_BUSQUEDA_MS;
    }

    public static void marcarIntentoVideo(Context c, long id) {
        prefs(c).edit().putLong("intento_video_" + id, System.currentTimeMillis()).apply();
    }

    // Marca que los canales iniciales ya se cargaron.
    public static void marcarPrecargado(Context c) {
        prefs(c).edit().putBoolean(PRECARGADO, true).apply();
    }
}
