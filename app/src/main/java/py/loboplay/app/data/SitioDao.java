package py.loboplay.app.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

// Acceso a la lista de sitios guardados.
@Dao
public interface SitioDao {

    // Todos los sitios: favoritos primero, luego en el orden elegido.
    @Query("SELECT * FROM sitio ORDER BY favorito DESC, orden ASC, nombre COLLATE NOCASE ASC")
    LiveData<List<Sitio>> todos();

    // Lista simple en el mismo orden (para el respaldo).
    @Query("SELECT * FROM sitio ORDER BY favorito DESC, orden ASC, nombre COLLATE NOCASE ASC")
    List<Sitio> lista();

    @Query("SELECT * FROM sitio WHERE id = :id")
    Sitio buscar(long id);

    // Ultimo numero de orden usado.
    @Query("SELECT COALESCE(MAX(orden), 0) FROM sitio")
    long maxOrden();

    // Guarda la posicion de un sitio.
    @Query("UPDATE sitio SET orden = :orden WHERE id = :id")
    void ponerOrden(long id, long orden);

    // Anota cuando se abrio un sitio.
    @Query("UPDATE sitio SET ultimoAcceso = :cuando WHERE id = :id")
    void marcarAbierto(long id, long cuando);

    @Insert
    long insertar(Sitio sitio);

    @Update
    void actualizar(Sitio sitio);

    @Delete
    void eliminar(Sitio sitio);
}
