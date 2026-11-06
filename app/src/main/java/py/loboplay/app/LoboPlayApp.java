package py.loboplay.app;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

import py.loboplay.app.util.Ajustes;

// Aplica el tema claro u oscuro elegido al abrir la app.
public class LoboPlayApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        AppCompatDelegate.setDefaultNightMode(Ajustes.modoTema(this));
    }
}
