package py.loboplay.app.util;

import android.net.Uri;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// Paises para organizar los canales (codigo de dos letras, nombre y bandera).
public final class Paises {
    public static final class Pais {
        public final String codigo;
        public final String nombre;

        Pais(String codigo, String nombre) {
            this.codigo = codigo;
            this.nombre = nombre;
        }

        // Bandera + nombre, por ejemplo: "🇵🇾 Paraguay".
        public String etiqueta() {
            return Paises.bandera(codigo) + " " + nombre;
        }
    }

    private static final String[][] LISTA = {
            {"AR", "Argentina"}, {"BO", "Bolivia"}, {"BR", "Brasil"}, {"CL", "Chile"},
            {"CO", "Colombia"}, {"CR", "Costa Rica"}, {"CU", "Cuba"}, {"EC", "Ecuador"},
            {"SV", "El Salvador"}, {"ES", "España"}, {"US", "Estados Unidos"}, {"GT", "Guatemala"},
            {"HN", "Honduras"}, {"MX", "México"}, {"NI", "Nicaragua"}, {"PA", "Panamá"},
            {"PY", "Paraguay"}, {"PE", "Perú"}, {"DO", "República Dominicana"},
            {"UY", "Uruguay"}, {"VE", "Venezuela"}
    };

    private Paises() {
    }

    // Todos los paises disponibles, en orden alfabetico.
    public static List<Pais> todos() {
        List<Pais> lista = new ArrayList<>();
        for (String[] p : LISTA) lista.add(new Pais(p[0], p[1]));
        return lista;
    }

    public static String nombre(String codigo) {
        if (codigo == null) return null;
        for (String[] p : LISTA) {
            if (p[0].equals(codigo)) return p[1];
        }
        return codigo;
    }

    // Emoji de bandera a partir del codigo (PY -> bandera de Paraguay).
    public static String bandera(String codigo) {
        if (codigo == null || codigo.length() != 2) return "🌐";
        StringBuilder sb = new StringBuilder();
        for (char c : codigo.toUpperCase(Locale.ROOT).toCharArray()) {
            if (c < 'A' || c > 'Z') return "🌐";
            sb.appendCodePoint(0x1F1E6 + (c - 'A'));
        }
        return sb.toString();
    }

    public static String etiqueta(String codigo) {
        return codigo == null ? null : bandera(codigo) + " " + nombre(codigo);
    }

    // Adivina el pais por el final del dominio (.com.py -> PY). Devuelve null si no se sabe.
    public static String deDominio(String url) {
        String host = url == null ? null : Uri.parse(url).getHost();
        if (host == null) return null;
        int punto = host.lastIndexOf('.');
        if (punto < 0) return null;
        String fin = host.substring(punto + 1).toUpperCase(Locale.ROOT);
        for (String[] p : LISTA) {
            if (p[0].equals(fin)) return fin;
        }
        return null;
    }
}
