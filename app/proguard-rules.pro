# Reglas de R8 para LoboPlay (ofuscación y reducción de código en release).

# Para que los errores de la versión publicada muestren archivo y línea al desofuscarlos con mapping.txt.
-keepattributes SourceFile,LineNumberTable,*Annotation*
-renamesourcefileattribute SourceFile

# Métodos que se llaman desde JavaScript en el WebView (window.lp.ok / fallo, etc.).
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Room: la entidad y las consultas se acceden por código generado.
-keep class py.loboplay.app.data.Sitio { *; }

# jsoup nombra clases opcionales que no se incluyen.
-dontwarn com.google.re2j.**
-dontwarn org.jspecify.annotations.**
-dontwarn javax.annotation.**
