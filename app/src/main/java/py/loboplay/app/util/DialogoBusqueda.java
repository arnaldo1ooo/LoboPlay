package py.loboplay.app.util;

import android.content.Context;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import py.loboplay.app.R;

// Ventana que bloquea la pantalla mientras se busca el video. Solo se cierra con Cancelar
// (o cuando el que la abrio la cierra al terminar).
public final class DialogoBusqueda {
    private DialogoBusqueda() {
    }

    public static AlertDialog mostrar(Context ctx, int mensaje, Runnable alCancelar) {
        float d = ctx.getResources().getDisplayMetrics().density;
        LinearLayout fila = new LinearLayout(ctx);
        fila.setOrientation(LinearLayout.HORIZONTAL);
        fila.setGravity(Gravity.CENTER_VERTICAL);
        int margen = (int) (24 * d);
        fila.setPadding(margen, (int) (16 * d), margen, 0);

        CircularProgressIndicator progreso = new CircularProgressIndicator(ctx);
        progreso.setIndeterminate(true);
        fila.addView(progreso);

        TextView texto = new TextView(ctx);
        texto.setText(mensaje);
        texto.setTextSize(15);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMarginStart((int) (16 * d));
        fila.addView(texto, lp);

        AlertDialog dialogo = new MaterialAlertDialogBuilder(ctx)
                .setView(fila)
                .setCancelable(false)
                .setNegativeButton(R.string.cancelar, (x, y) -> alCancelar.run())
                .create();
        dialogo.setCanceledOnTouchOutside(false);
        dialogo.show();
        return dialogo;
    }
}
