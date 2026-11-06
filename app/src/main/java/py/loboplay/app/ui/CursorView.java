package py.loboplay.app.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

// Flecha que se mueve con el control remoto sobre la pagina.
public class CursorView extends View {
    private final Paint relleno = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borde = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path flecha = new Path();
    private float x;
    private float y;
    private boolean centrado = false;

    public CursorView(Context contexto) {
        super(contexto);
        float d = getResources().getDisplayMetrics().density;
        relleno.setColor(Color.WHITE);
        borde.setColor(Color.BLACK);
        borde.setStyle(Paint.Style.STROKE);
        borde.setStrokeWidth(2 * d);
        borde.setStrokeJoin(Paint.Join.ROUND);
        flecha.moveTo(0, 0);
        flecha.lineTo(0, 24 * d);
        flecha.lineTo(6 * d, 18 * d);
        flecha.lineTo(11 * d, 29 * d);
        flecha.lineTo(16 * d, 27 * d);
        flecha.lineTo(11 * d, 16 * d);
        flecha.lineTo(19 * d, 16 * d);
        flecha.close();
        setFocusable(false);
    }

    @Override
    protected void onSizeChanged(int ancho, int alto, int anchoViejo, int altoViejo) {
        if (!centrado && ancho > 0 && alto > 0) {
            x = ancho / 2f;
            y = alto / 2f;
            centrado = true;
        }
    }

    // Mueve la flecha sin salirse de la pantalla.
    public void mover(float dx, float dy) {
        x = Math.max(0, Math.min(getWidth(), x + dx));
        y = Math.max(0, Math.min(getHeight(), y + dy));
        invalidate();
    }

    public float getPosX() {
        return x;
    }

    public float getPosY() {
        return y;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.save();
        canvas.translate(x, y);
        canvas.drawPath(flecha, relleno);
        canvas.drawPath(flecha, borde);
        canvas.restore();
    }
}
