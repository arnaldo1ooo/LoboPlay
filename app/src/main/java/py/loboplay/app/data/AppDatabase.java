package py.loboplay.app.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

// Base de datos local de la app.
@Database(entities = {Sitio.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public abstract SitioDao sitioDao();

    private static volatile AppDatabase instancia;

    // Devuelve la unica base de datos de la app.
    public static AppDatabase get(Context ctx) {
        if (instancia == null) {
            synchronized (AppDatabase.class) {
                if (instancia == null) {
                    instancia = Room.databaseBuilder(
                                    ctx.getApplicationContext(), AppDatabase.class, "loboplay.db")
                            .build();
                }
            }
        }
        return instancia;
    }
}
