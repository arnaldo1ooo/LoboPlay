package py.loboplay.app.ui;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import py.loboplay.app.R;
import py.loboplay.app.util.BloqueadorAnuncios;
import py.loboplay.app.util.Hilos;
import py.loboplay.app.util.Respaldo;

// Opciones: navegacion y respaldo de la lista.
public class AjustesFragment extends PreferenceFragmentCompat {

    private final ActivityResultLauncher<String> crearArchivo = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("application/json"), uri -> {
                if (uri != null) exportar(uri);
            });

    private final ActivityResultLauncher<String[]> abrirArchivo = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) importar(uri);
            });

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferencias, rootKey);

        alTocar("exportar", () -> avisarSiFalla(() -> crearArchivo.launch("loboplay-sitios.json")));
        alTocar("importar", () -> avisarSiFalla(() -> abrirArchivo.launch(new String[]{"*/*"})));
        alTocar("actualizar_anuncios", this::actualizarAnuncios);
        alTocar("comentarios", () -> startActivity(
                new android.content.Intent(requireContext(), ComentariosActivity.class)));
    }

    // Asocia una accion al toque de una opcion.
    private void alTocar(String clave, Runnable accion) {
        Preference p = findPreference(clave);
        if (p != null) {
            p.setOnPreferenceClickListener(x -> {
                accion.run();
                return true;
            });
        }
    }

    // Avisa si el dispositivo no tiene un selector de archivos.
    private void avisarSiFalla(Runnable accion) {
        try {
            accion.run();
        } catch (Exception e) {
            Toast.makeText(requireContext(), R.string.sin_selector, Toast.LENGTH_LONG).show();
        }
    }

    // Descarga una lista de anuncios mas completa.
    private void actualizarAnuncios() {
        final Context app = requireContext().getApplicationContext();
        mensaje(app, "Actualizando la lista…");
        Hilos.IO.execute(() -> {
            try {
                int n = BloqueadorAnuncios.actualizar(app);
                mensaje(app, "Lista actualizada: " + n + " dominios");
            } catch (Exception e) {
                mensaje(app, "No se pudo actualizar la lista");
            }
        });
    }

    // Guarda la lista en el archivo elegido.
    private void exportar(Uri destino) {
        final Context app = requireContext().getApplicationContext();
        Hilos.IO.execute(() -> {
            try {
                int n = Respaldo.exportar(app, destino);
                mensaje(app, "Lista guardada: " + n + " sitios");
            } catch (Exception e) {
                mensaje(app, "No se pudo guardar el archivo");
            }
        });
    }

    // Agrega los sitios nuevos del archivo elegido.
    private void importar(Uri origen) {
        final Context app = requireContext().getApplicationContext();
        Hilos.IO.execute(() -> {
            try {
                int n = Respaldo.importar(app, origen);
                mensaje(app, n == 0 ? "No hay sitios nuevos para agregar" : "Sitios agregados: " + n);
            } catch (Exception e) {
                mensaje(app, "El archivo no es válido");
            }
        });
    }

    // Muestra un aviso desde cualquier hilo.
    private void mensaje(Context app, String texto) {
        new Handler(Looper.getMainLooper()).post(() ->
                Toast.makeText(app, texto, Toast.LENGTH_LONG).show());
    }
}
