package py.loboplay.app.data;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

// Un sitio guardado por el usuario.
@Entity(tableName = "sitio")
public class Sitio {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public String nombre;
    public String url;
    public String iconoPropio; // ruta del icono elegido por el usuario
    public String iconoWeb;    // ruta del icono bajado de la pagina
    public boolean favorito;

    public String videoUrl;    // video detectado en la pagina (opcional): si existe se abre directo, si no, la pagina
    public String pais;        // codigo de pais (PY, AR...) para organizar el menu

    @ColumnInfo(defaultValue = "0")
    public boolean escritorio; // abrir como pagina de escritorio

    @ColumnInfo(defaultValue = "1")
    public boolean bloquearAnuncios = true; // bloquear anuncios en este sitio (algunos canales no reproducen si lo detectan)

    @ColumnInfo(defaultValue = "0")
    public long orden;         // posicion elegida por el usuario

    @ColumnInfo(defaultValue = "0")
    public long ultimoAcceso;  // cuando se abrio por ultima vez
}
