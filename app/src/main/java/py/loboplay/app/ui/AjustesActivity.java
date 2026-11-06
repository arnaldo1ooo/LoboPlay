package py.loboplay.app.ui;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import py.loboplay.app.R;
import py.loboplay.app.util.Bordes;

// Pantalla de configuracion de la app.
public class AjustesActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ajustes);
        Bordes.aplicar(this, false);
        findViewById(R.id.btnVolver).setOnClickListener(v -> finish());
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.contenedor, new AjustesFragment())
                    .commit();
        }
    }
}
