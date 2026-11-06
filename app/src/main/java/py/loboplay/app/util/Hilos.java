package py.loboplay.app.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Hilos de fondo para no trabar la pantalla.
public final class Hilos {
    public static final ExecutorService IO = Executors.newFixedThreadPool(3);

    // Hilos solo para consultar paginas de internet (asi no frenan lo demas).
    public static final ExecutorService RED = Executors.newFixedThreadPool(2);

    private Hilos() {
    }
}
