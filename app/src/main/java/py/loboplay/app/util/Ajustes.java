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

    // Dice si ya se cargaron los canales iniciales.
    public static boolean yaPrecargado(Context c) {
        return prefs(c).getBoolean(PRECARGADO, false);
    }

    // Marca que los canales iniciales ya se cargaron.
    public static void marcarPrecargado(Context c) {
        prefs(c).edit().putBoolean(PRECARGADO, true).apply();
    }
}
