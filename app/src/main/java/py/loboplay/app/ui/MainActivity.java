package py.loboplay.app.ui;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import py.loboplay.app.R;
import py.loboplay.app.data.AppDatabase;
import py.loboplay.app.data.IconoHelper;
import py.loboplay.app.data.Precarga;
import py.loboplay.app.data.Sitio;
import py.loboplay.app.data.SitioDao;
import py.loboplay.app.util.Ajustes;
import py.loboplay.app.util.Bordes;
import py.loboplay.app.util.DetectorVideo;
import py.loboplay.app.util.DialogoBusqueda;
import py.loboplay.app.util.Hilos;
import py.loboplay.app.util.Paises;
import py.loboplay.app.util.VerificadorCanales;

// Pantalla principal: menu con todos los sitios.
public class MainActivity extends AppCompatActivity implements SitioAdapter.Listener {

    private SitioAdapter adaptador;
    private List<Sitio> todos = new ArrayList<>();
    private EditText buscar;
    private Chip chipTodos;
    private Chip chipFavoritos;
    private Chip chipRecientes;
    private ChipGroup chipsPais;
    private View filaPais;
    private String paisFiltro; // null = todos los paises
    private java.util.List<String> codigosPais = new java.util.ArrayList<>();
    private View vacio;
    private TextView vacioTitulo;
    private TextView vacioAyuda;
    private RecyclerView lista;
    private ImageButton btnOrdenar;
    private boolean modoOrdenar = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Bordes.aplicar(this, false);

        Precarga.ejecutarSiHaceFalta(this, () -> {
            if (isFinishing() || isDestroyed()) return;
            VerificadorCanales.iniciar(this, informe -> new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                    .setTitle("Verificación de canales")
                    .setMessage(informe)
                    .setPositiveButton("Entendido", null)
                    .show());
        });

        buscar = findViewById(R.id.buscar);
        chipTodos = findViewById(R.id.chipTodos);
        chipFavoritos = findViewById(R.id.chipFavoritos);
        chipRecientes = findViewById(R.id.chipRecientes);
        chipsPais = findViewById(R.id.chipsPais);
        filaPais = findViewById(R.id.filaPais);
        chipsPais.setOnCheckedStateChangeListener((grupo, ids) -> {
            Chip marcado = ids.isEmpty() ? null : (Chip) grupo.findViewById(ids.get(0));
            paisFiltro = marcado == null ? null : (String) marcado.getTag();
            filtrar();
        });
        vacio = findViewById(R.id.vacio);
        vacioTitulo = findViewById(R.id.vacioTitulo);
        vacioAyuda = findViewById(R.id.vacioAyuda);
        lista = findViewById(R.id.lista);

        // Pantalla de bienvenida solo al abrir la app (no al girar la pantalla).
        final View bienvenida = findViewById(R.id.bienvenida);
        if (savedInstanceState == null) {
            bienvenida.setVisibility(View.VISIBLE);
            bienvenida.requestFocus();
            bienvenida.postDelayed(() -> bienvenida.animate().alpha(0f).setDuration(300).withEndAction(() -> {
                bienvenida.setVisibility(View.GONE);
                lista.requestFocus();
            }).start(), 1500);
        }
        btnOrdenar = findViewById(R.id.btnOrdenar);

        // Cuantas columnas entran segun el ancho de la pantalla.
        int columnas = Math.max(2, getResources().getConfiguration().screenWidthDp / 160);
        lista.setLayoutManager(new GridLayoutManager(this, columnas));
        adaptador = new SitioAdapter(this);
        lista.setAdapter(adaptador);
        prepararArrastre();

        AppDatabase.get(this).sitioDao().todos().observe(this, sitios -> {
            todos = sitios;
            armarPaises();
            filtrar();
        });

        buscar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void onTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                filtrar();
            }
        });

        ChipGroup chips = findViewById(R.id.chips);
        chips.setOnCheckedStateChangeListener((grupo, ids) -> filtrar());

        findViewById(R.id.btnAgregar).setOnClickListener(v ->
                startActivity(new Intent(this, AgregarActivity.class)));
        findViewById(R.id.btnAjustes).setOnClickListener(v ->
                startActivity(new Intent(this, AjustesActivity.class)));
        btnOrdenar.setOnClickListener(v -> alternarOrdenar());
    }

    // Permite arrastrar las tarjetas para cambiar su orden.
    private void prepararArrastre() {
        ItemTouchHelper ayudante = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT, 0) {
            @Override
            public boolean isLongPressDragEnabled() {
                return modoOrdenar;
            }

            @Override
            public boolean onMove(@NonNull RecyclerView r, @NonNull RecyclerView.ViewHolder desde,
                                  @NonNull RecyclerView.ViewHolder hasta) {
                int a = desde.getBindingAdapterPosition();
                int b = hasta.getBindingAdapterPosition();
                if (a < 0 || b < 0) return false;
                List<Sitio> actual = adaptador.getLista();
                // Los favoritos y los demas se ordenan por separado.
                if (actual.get(a).favorito != actual.get(b).favorito) return false;
                adaptador.mover(a, b);
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder v, int direccion) {
            }

            @Override
            public void clearView(@NonNull RecyclerView r, @NonNull RecyclerView.ViewHolder v) {
                super.clearView(r, v);
                if (modoOrdenar) guardarOrden(new ArrayList<>(adaptador.getLista()));
            }
        });
        ayudante.attachToRecyclerView(lista);
    }

    // Entra o sale del modo ordenar.
    private void alternarOrdenar() {
        if (modoOrdenar) {
            salirModoOrdenar();
            return;
        }
        boolean hayFiltro = !buscar.getText().toString().trim().isEmpty() || !chipTodos.isChecked() || paisFiltro != null;
        if (hayFiltro) {
            Toast.makeText(this, "Quita la búsqueda y los filtros para ordenar", Toast.LENGTH_LONG).show();
            return;
        }
        modoOrdenar = true;
        adaptador.setModoOrdenar(true);
        btnOrdenar.setSelected(true);
        Toast.makeText(this, "Mantén presionado un sitio y arrástralo. Toca el ícono otra vez para terminar.",
                Toast.LENGTH_LONG).show();
    }

    private void salirModoOrdenar() {
        modoOrdenar = false;
        adaptador.setModoOrdenar(false);
        btnOrdenar.setSelected(false);
    }

    // Guarda el orden actual de la lista completa.
    private void guardarOrden(List<Sitio> ordenada) {
        final AppDatabase db = AppDatabase.get(getApplicationContext());
        Hilos.IO.execute(() -> db.runInTransaction(() -> {
            SitioDao dao = db.sitioDao();
            for (int i = 0; i < ordenada.size(); i++) {
                dao.ponerOrden(ordenada.get(i).id, i + 1);
            }
        }));
    }

    // Arma la fila de paises con los que tienen los sitios guardados (solo si hay alguno).
    private void armarPaises() {
        // "Otro" va siempre al final.
        java.util.TreeSet<String> codigos = new java.util.TreeSet<>((a, b) -> {
            if (a.equals(b)) return 0;
            if (a.equals(Paises.OTRO)) return 1;
            if (b.equals(Paises.OTRO)) return -1;
            return a.compareTo(b);
        });
        for (Sitio s : todos) codigos.add(codigoPais(s));
        if (paisFiltro != null && !codigos.contains(paisFiltro)) paisFiltro = null;
        // Solo se rearma si cambiaron los paises: rehacer los chips en cada cambio de la base le quitaba
        // la seleccion al que tenia el foco con el control remoto.
        java.util.List<String> nuevos = new java.util.ArrayList<>(codigos);
        if (nuevos.equals(codigosPais)) return;
        codigosPais = nuevos;
        chipsPais.removeAllViews();
        filaPais.setVisibility(codigos.isEmpty() ? View.GONE : View.VISIBLE);
        if (codigos.isEmpty()) return;
        chipsPais.addView(crearChipPais(null, getString(R.string.pais_todos)));
        for (String c : codigos) chipsPais.addView(crearChipPais(c, Paises.etiqueta(c)));
    }

    // Pais del sitio; si no se pudo identificar cuenta como "Otro".
    private static String codigoPais(Sitio s) {
        return s.pais == null || s.pais.isEmpty() ? Paises.OTRO : s.pais;
    }

    private Chip crearChipPais(String codigo, String texto) {
        Chip chip = (Chip) getLayoutInflater().inflate(R.layout.chip_pais, chipsPais, false);
        chip.setId(View.generateViewId());
        chip.setTag(codigo);
        chip.setText(texto);
        chip.setCheckable(true);
        chip.setChecked(java.util.Objects.equals(codigo, paisFiltro));
        return chip;
    }

    // Muestra solo los sitios que coinciden con la busqueda y el filtro.
    private void filtrar() {
        String q = buscar.getText().toString().trim().toLowerCase(Locale.ROOT);
        boolean soloFavoritos = chipFavoritos.isChecked();
        boolean soloRecientes = chipRecientes.isChecked();

        List<Sitio> base = new ArrayList<>(todos);
        if (soloRecientes) {
            List<Sitio> abiertos = new ArrayList<>();
            for (Sitio s : base) {
                if (s.ultimoAcceso > 0) abiertos.add(s);
            }
            Collections.sort(abiertos, (a, b) -> Long.compare(b.ultimoAcceso, a.ultimoAcceso));
            base = abiertos.size() > 12 ? new ArrayList<>(abiertos.subList(0, 12)) : abiertos;
        }

        List<Sitio> visibles = new ArrayList<>();
        for (Sitio s : base) {
            if (soloFavoritos && !s.favorito) continue;
            if (paisFiltro != null && !paisFiltro.equals(codigoPais(s))) continue;
            boolean coincide = q.isEmpty()
                    || (s.nombre != null && s.nombre.toLowerCase(Locale.ROOT).contains(q))
                    || (s.url != null && s.url.toLowerCase(Locale.ROOT).contains(q));
            if (coincide) visibles.add(s);
        }

        boolean hayFiltro = !q.isEmpty() || soloFavoritos || soloRecientes || paisFiltro != null;
        if (modoOrdenar && hayFiltro) salirModoOrdenar();
        adaptador.setDatos(visibles);

        boolean sinResultados = visibles.isEmpty();
        vacio.setVisibility(sinResultados ? View.VISIBLE : View.GONE);
        lista.setVisibility(sinResultados ? View.GONE : View.VISIBLE);
        if (sinResultados && soloRecientes && !todos.isEmpty()) {
            vacioTitulo.setText(R.string.recientes);
            vacioAyuda.setText(R.string.sin_recientes);
        } else if (sinResultados && soloFavoritos && !todos.isEmpty()) {
            vacioTitulo.setText(R.string.favoritos);
            vacioAyuda.setText(R.string.sin_favoritos);
        } else {
            vacioTitulo.setText(R.string.sin_sitios);
            vacioAyuda.setText(R.string.sin_sitios_ayuda);
        }
    }

    // Abre el sitio en el navegador interno o en el externo.
    @Override
    public void abrir(Sitio sitio) {
        final long id = sitio.id;
        Hilos.IO.execute(() -> AppDatabase.get(getApplicationContext())
                .sitioDao().marcarAbierto(id, System.currentTimeMillis()));

        if (Ajustes.abrirExterno(this)) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(sitio.url)));
                return;
            } catch (ActivityNotFoundException ignorado) {
                // si no hay navegador externo se usa el interno
            }
        }
        boolean sinVideo = sitio.videoUrl == null || sitio.videoUrl.trim().isEmpty();
        if (sinVideo && Ajustes.puedeBuscarVideo(this, id)) {
            buscarVideoYAbrir(sitio);
            return;
        }
        abrirEnNavegador(sitio);
    }

    // Sitio sin video: antes de abrirlo intenta detectarlo (con Cancelar para abrir la pagina ya).
    private void buscarVideoYAbrir(final Sitio sitio) {
        Ajustes.marcarIntentoVideo(this, sitio.id);
        final androidx.appcompat.app.AlertDialog[] dialogo = new androidx.appcompat.app.AlertDialog[1];
        final DetectorVideo[] detector = new DetectorVideo[1];
        dialogo[0] = DialogoBusqueda.mostrar(this, R.string.video_buscando, () -> {
            if (detector[0] != null) detector[0].cancelar();
            abrirEnNavegador(sitio);
        });
        detector[0] = DetectorVideo.buscar(this, sitio.url, sitio.bloquearAnuncios, video -> {
            if (dialogo[0] != null) dialogo[0].dismiss();
            if (isFinishing() || isDestroyed()) return;
            if (video != null) {
                sitio.videoUrl = video;
                Hilos.IO.execute(() -> AppDatabase.get(getApplicationContext()).sitioDao().actualizar(sitio));
            }
            abrirEnNavegador(sitio);
        });
    }

    private void abrirEnNavegador(Sitio sitio) {
        Intent i = new Intent(this, NavegadorActivity.class);
        // Si se detecto un video se abre directo; si no, la pagina.
        boolean conVideo = sitio.videoUrl != null && !sitio.videoUrl.trim().isEmpty();
        i.putExtra(NavegadorActivity.EXTRA_URL, conVideo ? sitio.videoUrl : sitio.url);
        i.putExtra(NavegadorActivity.EXTRA_ESCRITORIO, sitio.escritorio);
        i.putExtra(NavegadorActivity.EXTRA_BLOQUEAR_ANUNCIOS, sitio.bloquearAnuncios);
        i.putExtra(NavegadorActivity.EXTRA_PAGINA, sitio.url);
        i.putExtra(NavegadorActivity.EXTRA_SITIO_ID, sitio.id);
        startActivity(i);
    }

    // Menu con las acciones de un sitio (pulsacion larga).
    @Override
    public void opciones(Sitio sitio) {
        mostrarOpciones(sitio);
    }

    private void mostrarOpciones(Sitio sitio) {
        String[] acciones = {
                "Editar",
                sitio.favorito ? "Quitar de favoritos" : "Marcar como favorito",
                "Mover antes",
                "Mover después",
                "Actualizar icono de la página",
                "Eliminar"
        };
        new MaterialAlertDialogBuilder(this)
                .setTitle(sitio.nombre)
                .setItems(acciones, (d, cual) -> {
                    switch (cual) {
                        case 0:
                            Intent i = new Intent(this, AgregarActivity.class);
                            i.putExtra(AgregarActivity.EXTRA_ID, sitio.id);
                            startActivity(i);
                            break;
                        case 1:
                            cambiarFavorito(sitio);
                            break;
                        case 2:
                            moverSitio(sitio, -1);
                            break;
                        case 3:
                            moverSitio(sitio, 1);
                            break;
                        case 4:
                            Hilos.IO.execute(() -> IconoHelper.refrescarIconoWeb(getApplicationContext(), sitio.id));
                            break;
                        default:
                            confirmarEliminar(sitio);
                    }
                })
                .show();
    }

    // Cambia un sitio de lugar con su vecino (sirve tambien con control remoto).
    private void moverSitio(Sitio sitio, int paso) {
        List<Sitio> completa = new ArrayList<>(todos);
        int i = -1;
        for (int k = 0; k < completa.size(); k++) {
            if (completa.get(k).id == sitio.id) {
                i = k;
                break;
            }
        }
        int j = i + paso;
        if (i < 0 || j < 0 || j >= completa.size()
                || completa.get(i).favorito != completa.get(j).favorito) {
            Toast.makeText(this, paso < 0 ? "Ya está al principio" : "Ya está al final", Toast.LENGTH_SHORT).show();
            return;
        }
        Collections.swap(completa, i, j);
        guardarOrden(completa);
    }

    // Marca o desmarca un sitio como favorito.
    private void cambiarFavorito(Sitio sitio) {
        Hilos.IO.execute(() -> {
            SitioDao dao = AppDatabase.get(getApplicationContext()).sitioDao();
            Sitio actual = dao.buscar(sitio.id);
            if (actual == null) return;
            actual.favorito = !actual.favorito;
            dao.actualizar(actual);
        });
    }

    // Pregunta antes de borrar un sitio.
    private void confirmarEliminar(Sitio sitio) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Eliminar " + sitio.nombre)
                .setMessage("Se quitará de tu lista.")
                .setNegativeButton(R.string.cancelar, null)
                .setPositiveButton("Eliminar", (d, w) -> Hilos.IO.execute(() -> {
                    SitioDao dao = AppDatabase.get(getApplicationContext()).sitioDao();
                    Sitio actual = dao.buscar(sitio.id);
                    if (actual == null) return;
                    IconoHelper.borrar(actual.iconoPropio);
                    IconoHelper.borrar(actual.iconoWeb);
                    dao.eliminar(actual);
                }))
                .show();
    }

    @Override
    protected void onDestroy() {
        VerificadorCanales.cancelar();
        super.onDestroy();
    }
}
