package py.loboplay.app.util;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import java.util.List;

import py.loboplay.app.data.AppDatabase;
import py.loboplay.app.data.Sitio;

// Busca, uno por uno y en segundo plano, el video de los canales que vienen de fabrica sin video.
// Los que no se detectan quedan abriendo su pagina. Al terminar entrega un informe.
public final class VerificadorCanales {
    public interface Fin {
        void alTerminar(String informe);
    }

    private static final Handler PRINCIPAL = new Handler(Looper.getMainLooper());
    private static boolean corriendo;
    private static DetectorVideo detector;

    private VerificadorCanales() {
    }

    // Empieza si hay canales pendientes (llamar desde el hilo principal).
    public static void iniciar(Activity act, Fin fin) {
        if (corriendo) return;
        Context app = act.getApplicationContext();
        List<Long> ids = Ajustes.pendientesVerificar(app);
        if (ids.isEmpty()) return;
        corriendo = true;
        Toast.makeText(act, "Buscando el video de los canales… puede tardar unos minutos", Toast.LENGTH_LONG).show();
        siguiente(act, app, ids, 0, new StringBuilder(), new int[]{0, 0}, fin);
    }

    // Detiene la busqueda (al cerrar la pantalla); lo pendiente se retoma la proxima vez.
    public static void cancelar() {
        corriendo = false;
        if (detector != null) detector.cancelar();
        detector = null;
    }

    private static boolean muerta(Activity act) {
        return act.isFinishing() || act.isDestroyed();
    }

    private static void siguiente(Activity act, Context app, List<Long> ids, int i, StringBuilder informe,
                                  int[] cuenta, Fin fin) {
        if (muerta(act)) {
            corriendo = false;
            return;
        }
        if (i >= ids.size()) {
            corriendo = false;
            String resumen = cuenta[0] + " de " + (cuenta[0] + cuenta[1]) + " canales con video detectado.\n\n" + informe
                    + "\nLos que no se detectaron se abren con su página. En Editar > Probar video puedes comprobar los detectados.";
            fin.alTerminar(resumen);
            return;
        }
        final long id = ids.get(i);
        Hilos.IO.execute(() -> {
            final Sitio s = AppDatabase.get(app).sitioDao().buscar(id);
            PRINCIPAL.post(() -> {
                if (muerta(act)) {
                    corriendo = false;
                    return;
                }
                if (s == null || (s.videoUrl != null && !s.videoUrl.isEmpty())) {
                    Ajustes.quitarPendiente(app, id);
                    siguiente(act, app, ids, i + 1, informe, cuenta, fin);
                    return;
                }
                DetectorVideo d = DetectorVideo.buscar(act, s.url, s.bloquearAnuncios, video -> {
                    detector = null;
                    if (muerta(act)) {
                        corriendo = false;
                        return;
                    }
                    if (video != null) {
                        s.videoUrl = video;
                        Hilos.IO.execute(() -> AppDatabase.get(app).sitioDao().actualizar(s));
                        cuenta[0]++;
                        informe.append("✓ ").append(s.nombre).append('\n');
                    } else {
                        cuenta[1]++;
                        informe.append("✗ ").append(s.nombre).append(" (se abre la página)\n");
                    }
                    Ajustes.quitarPendiente(app, id);
                    siguiente(act, app, ids, i + 1, informe, cuenta, fin);
                });
                detector = d.finalizado() ? null : d;
            });
        });
    }
}
