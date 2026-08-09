package com.example.gallerywall

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestHighestRefreshRate()

        setContent {
            MaterialTheme {
                GalleryWallApp(this)
            }
        }
    }

    /** Busca el modo de pantalla con mayor frecuencia de refresco (idealmente 120Hz) y lo solicita. */
    private fun requestHighestRefreshRate() {
        val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) display else windowManager.defaultDisplay
        val bestMode = display?.supportedModes?.maxByOrNull { it.refreshRate }
        if (bestMode != null) {
            val params: WindowManager.LayoutParams = window.attributes
            params.preferredDisplayModeId = bestMode.modeId
            window.attributes = params
        }
    }
}

private val REQUIRED_PERMISSION = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    Manifest.permission.READ_MEDIA_IMAGES
} else {
    Manifest.permission.READ_EXTERNAL_STORAGE
}

@Composable
fun GalleryWallApp(activity: ComponentActivity) {
    var hasPermission by remember { mutableStateOf(false) }
    var photos by remember { mutableStateOf<List<Photo>>(emptyList()) }
    var openedPhoto by remember { mutableStateOf<Photo?>(null) }
    var thumbSize by remember { mutableStateOf(ThumbnailSize.DEFAULT) }
    var autoMoveEnabled by remember { mutableStateOf(false) }
    var mode by remember { mutableStateOf(InteractionMode.DEFAULT) }
    var controlsVisible by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        thumbSize = ThumbnailSizePrefs.load(activity)
        autoMoveEnabled = AutoMovePrefs.load(activity)
        mode = InteractionModePrefs.load(activity)
    }

    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    LaunchedEffect(Unit) {
        val already = androidx.core.content.ContextCompat.checkSelfPermission(
            activity, REQUIRED_PERMISSION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (already) {
            hasPermission = true
        } else {
            launcher.launch(REQUIRED_PERMISSION)
        }
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            photos = PhotoRepository.loadPhotos(activity)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when {
            !hasPermission -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.corvina_logo_full),
                        contentDescription = "Corvina Gallery",
                        modifier = Modifier
                            .size(180.dp)
                            .align(Alignment.CenterHorizontally)
                    )
                    Text(
                        "Corvina Gallery necesita permiso para acceder a tus fotos.",
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(top = 12.dp, bottom = 16.dp)
                    )
                    Button(
                        onClick = { launcher.launch(REQUIRED_PERMISSION) },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("Conceder permiso")
                    }
                }
            }
            photos.isEmpty() -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                ) {
                    Image(
                        painter = painterResource(R.drawable.corvina_logo_full),
                        contentDescription = "Corvina Gallery",
                        modifier = Modifier
                            .size(160.dp)
                            .align(Alignment.CenterHorizontally)
                    )
                    Text(
                        "Cargando fotos…",
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(top = 12.dp)
                    )
                }
            }
            openedPhoto != null -> {
                PhotoViewerScreen(
                    context = activity,
                    photo = openedPhoto!!,
                    onClose = { openedPhoto = null }
                )
            }
            else -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    GridWallScreen(
                        context = activity,
                        photos = photos,
                        cellSizeDp = thumbSize.cellDp,
                        autoMoveEnabled = autoMoveEnabled,
                        mode = mode,
                        onOpenPhoto = { openedPhoto = it }
                    )

                    // Marca de agua discreta, como en el video de referencia.
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .statusBarsPadding()
                            .padding(top = 16.dp, end = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(R.drawable.corvina_mark),
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "  CORVINA",
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    AnimatedVisibility(
                        visible = controlsVisible,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(bottom = 18.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            ModeSelector(
                                selected = mode,
                                onSelect = { newMode ->
                                    mode = newMode
                                    InteractionModePrefs.save(activity, newMode)
                                }
                            )
                            ThumbnailSizeSelector(
                                selected = thumbSize,
                                onSelect = { size ->
                                    thumbSize = size
                                    ThumbnailSizePrefs.save(activity, size)
                                },
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = controlsVisible,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .navigationBarsPadding()
                            .padding(start = 16.dp, bottom = 18.dp)
                    ) {
                        AutoMoveToggle(
                            enabled = autoMoveEnabled,
                            onToggle = { enabled ->
                                autoMoveEnabled = enabled
                                AutoMovePrefs.save(activity, enabled)
                            }
                        )
                    }

                    // Botón para mostrar u ocultar todos los ajustes y dejar la
                    // pantalla limpia. Siempre visible, sin importar el estado.
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .navigationBarsPadding()
                            .padding(end = 16.dp, bottom = 18.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.55f))
                            .clickable { controlsVisible = !controlsVisible }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = if (controlsVisible) "Ocultar" else "Ajustes",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
