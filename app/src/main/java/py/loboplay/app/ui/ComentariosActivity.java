package py.loboplay.app.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import py.loboplay.app.R;
import py.loboplay.app.data.AppDatabase;
import py.loboplay.app.data.Sitio;
import py.loboplay.app.util.Bordes;
import py.loboplay.app.util.Comentarios;
import py.loboplay.app.util.Hilos;

// Formulario de comentarios: se envia solo a un formulario de Google.
public class ComentariosActivity extends AppCompatActivity {
    public static final String EXTRA_SITIO_ID = "sitioId";

    private MaterialAutoCompleteTextView campoTipo;
    private MaterialAutoCompleteTextView campoCanal;
    private TextInputEditText campoMensaje;
    private TextInputEditText campoContacto;
    private MaterialButton btnEnviar;
    private List<Sitio> sitios = new ArrayList<>();
    private Sitio elegido;
    private String ninguno;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comentarios);
        Bordes.aplicar(this, false);
        findViewById(R.id.btnVolver).setOnClickListener(v -> finish());

        campoTipo = findViewById(R.id.campoTipo);
        campoCanal = findViewById(R.id.campoCanal);
        campoMensaje = findViewById(R.id.campoMensaje);
        campoContacto = findViewById(R.id.campoContacto);
        btnEnviar = findViewById(R.id.btnEnviar);
        ninguno = getString(R.string.comentario_ninguno);

        String[] tipos = getResources().getStringArray(R.array.comentario_tipos);
        campoTipo.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, tipos));
        campoTipo.setText(tipos[0], false);

        final long idInicial = getIntent().getLongExtra(EXTRA_SITIO_ID, -1);
        Hilos.IO.execute(() -> {
            List<Sitio> lista = AppDatabase.get(getApplicationContext()).sitioDao().lista();
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                sitios = lista;
                List<String> nombres = new ArrayList<>();
                nombres.add(ninguno);
                for (Sitio s : lista) {
                    nombres.add(s.nombre);
                    if (s.id == idInicial) elegido = s;
                }
                campoCanal.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, nombres));
                campoCanal.setText(elegido != null ? elegido.nombre : ninguno, false);
            });
        });
        campoCanal.setOnItemClickListener((padre, v, pos, id) -> elegido = pos == 0 ? null : sitios.get(pos - 1));

        btnEnviar.setOnClickListener(v -> enviar());
    }

    private void enviar() {
        String mensaje = campoMensaje.getText() == null ? "" : campoMensaje.getText().toString().trim();
        // Con un canal elegido el mensaje es opcional (escribir con el control remoto es incomodo).
        if (mensaje.length() < 3 && elegido == null) {
            campoMensaje.setError(getString(R.string.comentario_falta_mensaje));
            campoMensaje.requestFocus();
            return;
        }
        if (mensaje.contains("http://") || mensaje.contains("https://")) {
            campoMensaje.setError(getString(R.string.comentario_sin_enlaces));
            campoMensaje.requestFocus();
            return;
        }
        if (!Comentarios.puedeEnviarAhora()) {
            Toast.makeText(this, R.string.comentario_espera, Toast.LENGTH_SHORT).show();
            return;
        }

        final Map<String, String> campos = new LinkedHashMap<>();
        campos.put(Comentarios.TIPO, campoTipo.getText().toString());
        campos.put(Comentarios.CANAL, elegido != null ? elegido.nombre : "");
        campos.put(Comentarios.MENSAJE, mensaje);
        campos.put(Comentarios.CONTACTO, campoContacto.getText() == null ? "" : campoContacto.getText().toString().trim());
        boolean tv = (getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_TYPE_MASK)
                == android.content.res.Configuration.UI_MODE_TYPE_TELEVISION;
        Comentarios.agregarDatosTecnicos(this, campos, tv);
        if (elegido != null) {
            boolean conVideo = elegido.videoUrl != null && !elegido.videoUrl.trim().isEmpty();
            campos.put(Comentarios.DATOS_CANAL, elegido.url + " | video detectado: " + (conVideo ? "sí" : "no")
                    + (conVideo ? " | " + elegido.videoUrl : "") + " | país: " + elegido.pais);
        }

        btnEnviar.setEnabled(false);
        btnEnviar.setText(R.string.comentario_enviando);
        final android.content.Context app = getApplicationContext();
        Hilos.RED.execute(() -> {
            boolean llego = Comentarios.enviarOGuardar(app, campos);
            runOnUiThread(() -> {
                Toast.makeText(app, llego ? R.string.comentario_gracias : R.string.comentario_pendiente,
                        Toast.LENGTH_LONG).show();
                if (!isFinishing() && !isDestroyed()) finish();
            });
        });
    }
}
