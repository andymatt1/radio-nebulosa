# Radio Nebulosa

Aplicación Android nativa (Kotlin + Jetpack Compose) dedicada **exclusivamente a las emisoras de radio de Colombia**. Arranca directamente en un catálogo cacheado con estética de plataforma de TV, fondo e icono nebulosos, reproductor HLS en segundo plano y auto-actualización del APK.

## Características

- **Arranque instantáneo:** el catálogo se guarda en el dispositivo y se muestra de inmediato; solo se consulta Radio Browser cuando tiene más de 10 minutos. Indicador de actualización en segundo plano, sin pantallas de espera.
- **Catálogo 100 % Colombia:** consulta `countrycode=CO` a Radio Browser API con `limit=2000` y respaldo entre 4 espejos. La URL se construye con `HttpUrl.Builder` (sin espacios ni saltos) y se filtra de nuevo por `countrycode` en el cliente; actualmente devuelve 675 emisoras. Trae nombre, logo (`favicon`), ciudad, géneros (`tags`) y URL de stream (`url_resolved`).
- **Interfaz estilo plataforma de TV:** cabecera con pestañas *Inicio / Mi lista*, banner protagonista, carrusel de canales en vivo, píldoras de géneros, ciudades y guía completa de emisoras. Icono cósmico clásico restaurado, fondo nebuloso místico y paleta violeta/cian/magenta.
- **Logo siempre impecable:** si Radio Browser deja el `favicon` vacío o con `"null"`, se busca el icono del dominio de la emisora con el servicio sin rastreo de DuckDuckGo; si falla, se muestra una tarjeta cósmica con iniciales y color determinista por emisora.
- **Reproductor:** mini-reproductor flotante con ondas "EN VIVO" y vista completa con fondo nebuloso, halo animado, favoritos, volumen, play/pausa y reconectar. La emisora se conserva al pausar y se reanuda sin volver a buscarla.
- **Favoritos:** botón de corazón en cada emisora, en el mini-reproductor y en el reproductor completo. Sección **Mis favoritos**, filtro **♥ Favoritos** y guardado persistente de la emisora completa.
- **Búsqueda:** tolerante a acentos ("bogota" encuentra "Bogotá"), por nombre, ciudad, género o frecuencia, con botón de limpiar.
- **Segundo plano:** `Media3 / ExoPlayer` + módulo **HLS** + `MediaSessionService`; notificación multimedia, pantalla de bloqueo y controles externos. Si falla la URL resuelta, reintenta con la URL original y aplica backoff 1/3/8/15 s.
- **Auto-actualización:** Ajustes → *Buscar actualizaciones* lee `version.json`, descarga el APK con progreso, verifica su SHA-256, lanza el instalador vía `FileProvider` y se reabre con `MY_PACKAGE_REPLACED`.

## Requisitos

- Android Studio **Ladybug (2024.2)** o superior (o Gradle 8.7 desde terminal).
- **JDK 17** (el Gradle wrapper se ejecuta con la JDK que tengas configurada).
- Android SDK **Platform 36** y **Build-Tools 36.0.0**.

- Dispositivo/emulador con **API 23+**.

## Compilar el APK

### Desde Android Studio

1. Clona el repositorio:
   ```bash
   git clone https://github.com/andymatt1/radio-nebulosa.git
   cd radio-nebulosa
   ```
2. Abre la carpeta como proyecto (`File → Open`).
3. Espera a que Gradle sincronice.
4. `Build → Build Bundle(s) / APK(s) → Build APK(s)`.

El APK queda en:

```
app/build/outputs/apk/debug/app-debug.apk
```

### Desde terminal

```bash
# 1) Crea local.properties apuntando a tu SDK (NO se versiona):
echo "sdk.dir=/ruta/a/tu/Android/Sdk" > local.properties

# 2) Compila
./gradlew assembleDebug       # APK de depuración
./gradlew assembleRelease     # APK de release sin firmar
```

Para distribuir, firma el release con tu propia keystore:

```bash
./gradlew assembleRelease
apksigner sign --ks mi-keystore.jks \
  app/build/outputs/apk/release/app-release-unsigned.apk
```

## Publicar una actualización

1. Compila el APK y súbelo a un **Release** de GitHub, por ejemplo:
   `v1.1.0/radio-nebulosa-v1.1.0.apk`.
2. Edita [`version.json`](version.json) en la rama `main`:
   ```json
   {
     "latestVersion": "1.1.0",
     "apkUrl": "https://github.com/andymatt1/radio-nebulosa/releases/download/v1.1.0/radio-nebulosa-v1.1.0.apk",
     "changelog": "Novedades de la versión…"
   }
   ```
3. Sube también `versionCode` y `versionName` en `app/build.gradle.kts`.
4. La app consulta siempre:
   `https://raw.githubusercontent.com/andymatt1/radio-nebulosa/main/version.json`.

> En Android 8+ el sistema pedirá habilitar *"Instalar aplicaciones desconocidas"* la primera vez. La app abre ese ajuste automáticamente.

## Permisos

| Permiso | Uso |
| --- | --- |
| `INTERNET`, `ACCESS_NETWORK_STATE` | Catálogo y streams |
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Radio en segundo plano |
| `POST_NOTIFICATIONS` | Controles multimedia (se solicita en Android 13+) |
| `WAKE_LOCK` | Evita que el stream se corte con la pantalla apagada |
| `REQUEST_INSTALL_PACKAGES` | Instalar el APK de actualización |

## Estructura

```
app/src/main/java/com/andymatt1/radionebulosa/
├── data/            # Modelo, API, categorías, favoritos
├── player/          # Servicio Media3 + ExoPlayer
├── updater/         # version.json, descarga e instalación, receiver
├── ui/home/         # Catálogo, búsqueda, destacadas, categorías
├── ui/player/       # Reproductor completo
├── ui/settings/     # Ajustes y actualizaciones
├── ui/splash/       # Robot cósmico animado
├── MainViewModel.kt
└── MainActivity.kt
```

## API

[Radio Browser API](https://de1.api.radio-browser.info/) — se filtra estrictamente por `countrycode=CO`, `hidebroken=true`, ordenado por votos, con reintento automático entre espejos.

---

¿Problemas para compilar? Abre un *issue* indicando versión de Android Studio, JDK y el error de Gradle.
