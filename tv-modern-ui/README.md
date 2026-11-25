# Xuper TV — interfaz moderna (Material 3 oscuro)

Modificación visual y de arranque para el APK descompilado de
[andymatt1/tv](https://github.com/andymatt1/tv) (`com.lite.fczx`, app **Xuper**).

El repositorio original no contiene código fuente Kotlin/Java: es un APK
descompilado con **apktool** (smali + recursos binarios). Por eso los cambios se
hacen en **recursos y manifiesto**, sin tocar la lógica smali.

## Qué cambia

### Interfaz
- Tema base `Theme.AppCompat.Light.NoActionBar` → `Theme.MaterialComponents.NoActionBar`
  (variante oscura de Material Components) con paleta M3:
  - fondo `#0A0C12`, superficies `#151927` / `#1D2234`
  - primario lavanda `#A78BFA`, secundario cian `#66E3FF`, terciario melocotón
  - texto principal `#F7F5FF`, texto secundario `#C9C7D6`
- Colores antiguos muy usados por la app (`color_main`, `color_6442EC`,
  `colorAccent`, `color_font_*`, etc.) redirigidos a la nueva paleta.
- Tarjetas y estados de foco redondeados (14 dp) con borde y degradado M3:
  navegación lateral, lista de canales, menú personal, diálogos, toasts y partidos.
- Scrims superiores/inferiores para que el texto se lea sobre el vídeo/pósters.
- selectores de texto corregidos para que el texto no quede oscuro sobre fondo oscuro.
- Imagen de splash de 1920×1080 (453 KB) sustituida por un degradado ligero de
  640×360: menos memoria decoded al arrancar.
- Botones y fondos de diálogo (PNG / 9-patch) teñidos con la nueva paleta,
  conservando las guías de estiramiento de los 9-patch.

### Rapidez
- 62 animaciones con duración > 180 ms limitadas a 180 ms (transiciones más secas).
- 22 servicios/receptores de push y analítica que no intervienen en la reproducción
  desactivados en el manifiesto (`android:enabled="false"`): Umeng/Taobao ACCS,
  Qiniu NetworkReceiver, AppMeasurement y DataTransport.
- Splash mucho más pequeño (menos decode de bitmap en el arranque).

## Archivos incluidos

| Archivo | Descripción |
| --- | --- |
| `xuper-modern-signed.apk` | Variante completa (reconstruida, alineada y firmada v1/v2/v3). |
| `xuper-modern-safe.apk` | **Recomendada**: misma interfaz, pero conserva el manifiesto y el tema AppCompat originales. |
| `xuper-modern-minimal.apk` | Variante de máxima compatibilidad: solo colores/drawables; no toca animaciones ni PNG. |
| `xuper-purple.apk` | Variante **morada elegante**: manifiesto/permisos intactos, iconos redondos y categorías en la parte superior. |
| `scripts/transform.py` | Transformador reproducible: recibe una carpeta ya decodificada con apktool. |
| `modern-ui.patch` | Diff textual de todos los cambios (útil para revisarlos o aplicarlos a mano). |
| `SHA256SUMS.txt` | Sumas de verificación de los archivos entregados. |

## Reproducir / reconstruir

Necesitas `apktool`, Java 17+, Pillow y numpy:

```bash
git clone https://github.com/andymatt1/tv.git tv
cd tv
# El repo ya viene descompilado; primero reconstruimos y redecodificamos
# para obtener res/values y layouts en XML legible:
apktool b . -o base.apk
apktool d -f base.apk -o decoded
python3 /ruta/a/tv-modern-ui/scripts/transform.py decoded --safe --no-anim --no-png
# Para la variante morada (categorías arriba + iconos redondos):
python3 /ruta/a/tv-modern-ui/scripts/purple_elegant.py decoded
apktool b decoded -o modern-unsigned.apk
zipalign -p -f 4 modern-unsigned.apk modern-aligned.apk
keytool -genkeypair -v -keystore modern.jks -alias modern \
  -keyalg RSA -keysize 2048 -validity 10000 -storepass cambio -keypass cambio \
  -dname "CN=Xuper Modern"
apksigner sign --ks modern.jks --ks-pass pass:cambio modern-aligned.apk
apksigner verify --verbose modern-aligned.apk
```

Si ya tienes el APK original (`2.apk`), puedes saltarte el primer `apktool b` y
hacer `apktool d 2.apk -o decoded`.

## Instalación

1. Haz copia de seguridad del APK original y de tus datos.
2. Desinstala la app instalada (la firma ya no coincide con la original).
3. Instala `xuper-modern-signed.apk` en el móvil/TV Android y prueba reproducción,
   búsqueda, diálogos de pago/login y control por mando.

## Avisos y rollback

- Es una app de terceros. Modifícala solo para uso personal y respeta sus términos.
- Al desactivar push/analítica, **no llegarán notificaciones** de esos servicios.
  Para recuperarlas, elimina `android:enabled="false"` de esos componentes en
  `AndroidManifest.xml` y reconstruye.
- Si la app se queda en la pantalla de carga, instala **`xuper-modern-safe.apk`**:
  conserva la paleta y los drawables M3 pero deja el manifiesto y el theme padre
  originales, que son los cambios que pueden interferir con el arranque.
  Si todavía falla, prueba **`xuper-modern-minimal.apk`**, que además mantiene
  las animaciones y las imágenes originales. También puedes generarlos con los
  flags `--safe`, `--no-anim` y `--no-png`.
- Para volver al estado original, reinstala el APK original.
