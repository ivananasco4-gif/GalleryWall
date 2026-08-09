# Corvina Gallery

App de galería Android (Kotlin + Jetpack Compose) que replica el efecto del video:
una malla de miniaturas cubre toda la pantalla y, alrededor de donde tocas o
arrastras el dedo, las fotos cercanas se agrandan y giran suavemente formando
un "lente" tipo ojo, con el título de la foto enfocada arriba. Cuando no tocas
la pantalla, el lente se mueve solo con una leve deriva automática.

## Cómo abrirlo

1. Abre Android Studio (Koala o más reciente recomendado).
2. **File > Open** y selecciona esta carpeta (`GalleryWall`).
3. Si Android Studio pregunta por el *Gradle Wrapper*, acepta que lo genere
   automáticamente (no incluí el binario `gradle-wrapper.jar` porque este
   entorno no tiene acceso a internet para descargarlo). Alternativamente,
   ejecuta una vez `gradle wrapper` con tu Gradle local.
4. Sincroniza el proyecto y ejecuta en un dispositivo real (el emulador no
   sirve para probar 120 fps ni fotos reales de galería).

## Cómo funciona el efecto

- `GridWallScreen.kt` dibuja una rejilla de celdas sobre un `Canvas`. Para
  cada celda calcula su distancia al punto de "foco" (donde tocas) y aplica
  una escala + rotación que decae suavemente (`falloff`) hasta el borde del
  radio del lente (`lensRadius`). Así las celdas cercanas se ven grandes y
  giradas, y las lejanas quedan intactas, sin costuras.
- Arrastrar el dedo mueve el foco en tiempo real (`detectDragGestures`).
- Doble tap sobre la pantalla abre la foto enfocada a pantalla completa.
- Cuando no hay contacto, un `LaunchedEffect` con `withFrameNanos` mueve el
  foco en una órbita lenta (efecto "demo" automático, como en tu video).
- `MainActivity.kt` busca el modo de pantalla con mayor frecuencia de
  refresco (`Display.supportedModes`) y lo solicita vía
  `window.attributes.preferredDisplayModeId`, para que el dispositivo use
  120 Hz si el hardware lo soporta. Compose anima siguiendo el reloj de
  vsync real, así que aprovecha esa frecuencia automáticamente.

## Tamaño de miniatura

Abajo, centrado en pantalla, hay un selector con 5 tamaños (definidos en
`ThumbnailSize.kt`):

- **Micro** (10dp): densidad tipo el video de referencia.
- **Nano** (16dp)
- **Pequeño** (24dp)
- **Mediano** (34dp): el mismo tamaño que la demo web de muestra.
- **Grande** (50dp)

La elección se guarda en `SharedPreferences` y se recuerda entre sesiones.
Cambiar el tamaño recalcula cuántas celdas caben en pantalla y vuelve a
pedir miniaturas a la resolución adecuada (más chicas para Micro/Nano, más
nítidas para Grande).

## Ocultar los ajustes

Abajo a la derecha hay un botón ("Ocultar" / "Ajustes") que oculta con un
fundido el segmentado de modo, el selector de tamaño y el interruptor
"Auto", dejando la pantalla completamente limpia — solo la malla y la
marca de agua. Tócalo de nuevo para que vuelvan a aparecer. Es un estado
de la sesión (no se guarda entre aperturas de la app; siempre arranca con
los ajustes visibles).

## Modo de interacción

Arriba del selector de tamaño hay un segmentado con dos modos
(`InteractionMode.kt`, `ModeSelector.kt`):

- **Burbuja**: el efecto de lupa/ojo original — las celdas cercanas al
  punto de contacto se agrandan y giran.
- **Elevación**: una "sábana" plana; donde tocas, algo del tamaño de una
  foto se levanta como un domo 3D (con sombra debajo y un sombreado tipo
  esfera — luz arriba-izquierda, penumbra en el borde), y la malla de
  alrededor se comba levemente hacia afuera cerca de la base, sin girar.
  Las fotos dentro del domo quedan derechas.

La elección se guarda y se recuerda entre sesiones.

## Movimiento del lente

El lente **no se mueve solo por defecto**: arranca centrado y solo cambia
de posición mientras tocas o arrastras. Si quieres el modo "demo" donde
deriva solo cuando no lo tocas, actívalo con el interruptor **"Auto"**
abajo a la izquierda (`AutoMoveToggle.kt`). Se apaga automáticamente en
cuanto vuelves a tocar la pantalla, y tu elección se recuerda entre
sesiones (`AutoMovePrefs`).

## Ajustes rápidos (en `GridWallScreen.kt`)

- `cellPx` (línea ~75): tamaño de cada miniatura de fondo → más chico = malla
  más densa (más parecida al video, pero más costo de dibujo).
- `maxScale`: cuánto se agranda la foto en el centro del lente.
- `baseLensRadius`: qué tan grande es la zona de "lupa".
- `swirlMax`: intensidad del giro/espiral alrededor del lente.

## Nombre e identidad visual

- Nombre de la app: **Corvina Gallery** (`strings.xml` → `app_name`).
- Ícono adaptativo (Android 8+) armado a partir del logo que enviaste: la
  esfera va como `foreground` (`res/mipmap-*/ic_launcher_foreground.png`)
  sobre fondo negro (`res/mipmap-anydpi-v26/ic_launcher.xml` +
  `colors.xml` → `ic_launcher_background`). También quedaron los íconos
  legacy cuadrados para versiones anteriores a Android 8.
- El logo completo (esfera + texto) se muestra en las pantallas de permiso
  y de carga (`res/drawable/corvina_logo_full.png`).
- Una marca de agua chica ("CORVINA" + esferita) queda fija en la esquina
  superior derecha de la malla, igual que el watermark del video de
  referencia (`res/drawable/corvina_mark.png`).
- El acento de color del selector de tamaños se ajustó a celeste
  (`#3FC7F4`) para combinar con el logo.

Si más adelante quieres publicarla en Play Store, lo único pendiente de
identidad sería cambiar el `applicationId`/paquete de
`com.example.gallerywall` a algo propio (p. ej. `com.corvina.gallery`) —
dímelo cuando quieras hacerlo y lo dejamos listo.

## Nota de rendimiento

Con **Micro** en una pantalla grande, la malla puede superar las 3000-4000
celdas. Para que siga siendo fluido, el dibujo está optimizado: la
distorsión (escala + giro) solo se calcula y ordena para las celdas que
están *dentro* del radio del lente (unas pocas cientos, sin importar cuán
densa sea la malla); el resto se dibuja directo, sin trigonometría ni
orden-Z, porque fuera del lente no hay solapamiento. Si aun así notas caída
de fps en Micro en tu dispositivo, prueba con Nano/Pequeño.

## Permisos

Pide permiso de lectura de fotos (`READ_MEDIA_IMAGES` en Android 13+,
`READ_EXTERNAL_STORAGE` en versiones anteriores) la primera vez que abres
la app.
