package py.loboplay.app.ui;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import py.loboplay.app.R;
import py.loboplay.app.data.Sitio;

// Dibuja la grilla de sitios del menu principal.
public class SitioAdapter extends RecyclerView.Adapter<SitioAdapter.Fila> {

    public interface Listener {
        void abrir(Sitio sitio);

        void opciones(Sitio sitio);
    }

    // Colores para la letra de los sitios sin icono.
    private static final int[] PALETA = {
            0xFF0C447C, 0xFF0F6E56, 0xFF993C1D, 0xFF534AB7,
            0xFF854F0B, 0xFF993556, 0xFF3B6D11, 0xFF185FA5
    };

    private final Listener listener;
    private final List<Sitio> sitios = new ArrayList<>();
    private boolean modoOrdenar = false;

    public SitioAdapter(Listener listener) {
        this.listener = listener;
    }

    // Reemplaza la lista que se muestra.
    // Con DiffUtil solo se actualizan los items que cambian: asi la seleccion del control remoto no se pierde.
    public void setDatos(List<Sitio> nuevos) {
        final List<Sitio> viejos = new ArrayList<>(sitios);
        final List<Sitio> actuales = new ArrayList<>(nuevos);
        DiffUtil.DiffResult cambios = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return viejos.size();
            }

            @Override
            public int getNewListSize() {
                return actuales.size();
            }

            @Override
            public boolean areItemsTheSame(int a, int b) {
                return viejos.get(a).id == actuales.get(b).id;
            }

            @Override
            public boolean areContentsTheSame(int a, int b) {
                Sitio x = viejos.get(a);
                Sitio y = actuales.get(b);
                return java.util.Objects.equals(x.nombre, y.nombre)
                        && java.util.Objects.equals(x.iconoPropio, y.iconoPropio)
                        && java.util.Objects.equals(x.iconoWeb, y.iconoWeb)
                        && x.favorito == y.favorito;
            }
        });
        sitios.clear();
        sitios.addAll(actuales);
        cambios.dispatchUpdatesTo(this);
    }

    // Lista que se esta mostrando, en su orden actual.
    public List<Sitio> getLista() {
        return sitios;
    }

    // Activa o desactiva el modo de arrastrar para ordenar.
    public void setModoOrdenar(boolean activo) {
        modoOrdenar = activo;
        notifyDataSetChanged();
    }

    // Cambia de lugar un sitio mientras se arrastra.
    public void mover(int de, int a) {
        Sitio s = sitios.remove(de);
        sitios.add(a, s);
        notifyItemMoved(de, a);
    }

    @NonNull
    @Override
    public Fila onCreateViewHolder(@NonNull ViewGroup padre, int tipo) {
        View v = LayoutInflater.from(padre.getContext()).inflate(R.layout.item_sitio, padre, false);
        return new Fila(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Fila fila, int posicion) {
        Sitio s = sitios.get(posicion);
        String nombre = s.nombre == null ? "" : s.nombre.trim();
        fila.nombre.setText(nombre);
        fila.estrella.setVisibility(s.favorito ? View.VISIBLE : View.GONE);

        String ruta = s.iconoPropio != null ? s.iconoPropio : s.iconoWeb;
        if (ruta != null && new File(ruta).exists()) {
            fila.letra.setVisibility(View.GONE);
            fila.icono.setVisibility(View.VISIBLE);
            Glide.with(fila.icono).load(new File(ruta)).into(fila.icono);
        } else {
            fila.icono.setVisibility(View.GONE);
            fila.letra.setVisibility(View.VISIBLE);
            fila.letra.setText(nombre.isEmpty() ? "?" : nombre.substring(0, 1).toUpperCase());
            float d = fila.itemView.getResources().getDisplayMetrics().density;
            GradientDrawable fondo = new GradientDrawable();
            fondo.setShape(GradientDrawable.RECTANGLE);
            fondo.setCornerRadius(16 * d);
            fondo.setColor(PALETA[Math.abs(nombre.hashCode() % PALETA.length)]);
            fila.letra.setBackground(fondo);
        }

        fila.itemView.setAlpha(modoOrdenar ? 0.85f : 1f);
        fila.itemView.setOnClickListener(v -> {
            if (modoOrdenar) return;
            int p = fila.getBindingAdapterPosition();
            if (p != RecyclerView.NO_POSITION) listener.abrir(sitios.get(p));
        });
        fila.itemView.setOnLongClickListener(v -> {
            if (modoOrdenar) return false;
            int p = fila.getBindingAdapterPosition();
            if (p != RecyclerView.NO_POSITION) listener.opciones(sitios.get(p));
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return sitios.size();
    }

    static class Fila extends RecyclerView.ViewHolder {
        final ImageView icono;
        final TextView letra;
        final TextView nombre;
        final TextView estrella;

        Fila(View v) {
            super(v);
            icono = v.findViewById(R.id.icono);
            letra = v.findViewById(R.id.letra);
            nombre = v.findViewById(R.id.nombre);
            estrella = v.findViewById(R.id.estrella);

            // Con el control remoto: borde azul y un poco mas grande al seleccionar.
            final MaterialCardView tarjeta = (MaterialCardView) v;
            final float d = v.getResources().getDisplayMetrics().density;
            final int colorFoco = ContextCompat.getColor(v.getContext(), R.color.foco);
            v.setOnFocusChangeListener((vista, tieneFoco) -> {
                tarjeta.setStrokeColor(colorFoco);
                tarjeta.setStrokeWidth(tieneFoco ? Math.round(3 * d) : 0);
                float escala = tieneFoco ? 1.07f : 1f;
                vista.animate().scaleX(escala).scaleY(escala).setDuration(120).start();
            });
        }
    }
}
