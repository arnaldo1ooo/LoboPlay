package py.loboplay.app.util;

import android.graphics.Color;
import android.view.View;

import androidx.activity.ComponentActivity;
import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

// Evita que la barra de estado y los botones del sistema tapen la pantalla.
public final class Bordes {

    private Bordes() {
    }

    // Pantalla completa con barras transparentes y un margen igual al de las barras, la camara y el teclado.
    // Con fondoNegro los iconos de las barras salen claros; si no, siguen el tema claro u oscuro.
    public static void aplicar(ComponentActivity actividad, boolean fondoNegro) {
        SystemBarStyle estilo = fondoNegro
                ? SystemBarStyle.dark(Color.TRANSPARENT)
                : SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT);
        EdgeToEdge.enable(actividad, estilo, estilo);

        View raiz = actividad.findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(raiz, (vista, insets) -> {
            Insets m = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout()
                    | WindowInsetsCompat.Type.ime());
            vista.setPadding(m.left, m.top, m.right, m.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
        ViewCompat.requestApplyInsets(raiz);
    }
}
