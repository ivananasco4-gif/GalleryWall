package com.example.gallerywall

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.BoxWithConstraints
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.max
import kotlin.math.min

@Composable
fun GridWallScreen(
    context: Context,
    photos: List<Photo>,
    cellSizeDp: Dp,
    autoMoveEnabled: Boolean,
    mode: InteractionMode,
    onOpenPhoto: (Photo) -> Unit
) {
    val thumbnails = remember(cellSizeDp) { mutableStateListOf<Bitmap?>() }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }

        val cellPx = with(density) { cellSizeDp.toPx() }
        val overscan = 1.4f
        val cols = max(1, ((widthPx * overscan) / cellPx).toInt())
        val rows = max(1, ((heightPx * overscan) / cellPx).toInt())
        val cellCount = cols * rows

        val thumbPx = with(density) { (cellSizeDp * 2.2f).toPx().toInt().coerceIn(48, 220) }

        LaunchedEffect(photos, cellCount, thumbPx) {
            if (photos.isEmpty()) return@LaunchedEffect
            val needed = min(photos.size, cellCount)
            if (thumbnails.size < needed) {
                repeat(needed - thumbnails.size) { thumbnails.add(null) }
            }
            val chunkSize = 12
            for (start in 0 until needed step chunkSize) {
                val end = min(start + chunkSize, needed)
                val jobs = (start until end).map { i ->
                    launch {
                        val bmp = PhotoRepository.loadThumbnail(context, photos[i], thumbPx)
                        if (i < thumbnails.size) thumbnails[i] = bmp
                    }
                }
                jobs.forEach { it.join() }
            }
        }

        var focus by remember { mutableStateOf(Offset(widthPx / 2f, heightPx / 2f)) }
        var isDragging by remember { mutableStateOf(false) }

        LaunchedEffect(isDragging, autoMoveEnabled, widthPx, heightPx) {
            if (isDragging || !autoMoveEnabled) return@LaunchedEffect
            val cx = widthPx / 2f
            val cy = heightPx / 2f
            val radius = min(widthPx, heightPx) * 0.18f
            val start = withFrameNanos { it }
            while (true) {
                val now = withFrameNanos { it }
                val t = (now - start) / 1_000_000_000f
                focus = Offset(
                    x = cx + radius * cos(t * 0.35f),
                    y = cy + radius * sin(t * 0.5f)
                )
            }
        }

        val lensRadius = min(widthPx, heightPx) * 0.30f
        val maxScale = 4.2f
        val swirlMax = 0.9f

        val gridOffX = (cols * cellPx - widthPx) / 2f
        val gridOffY = (rows * cellPx - heightPx) / 2f
        val selectedIndex = remember(focus, cols, rows, cellPx) {
            val col = ((focus.x + gridOffX) / cellPx).toInt().coerceIn(0, cols - 1)
            val row = ((focus.y + gridOffY) / cellPx).toInt().coerceIn(0, rows - 1)
            val idx = row * cols + col
            if (photos.isNotEmpty()) ((idx % photos.size) + photos.size) % photos.size else -1
        }
        val selectedPhoto = photos.getOrNull(selectedIndex)

        val drawBubble: DrawScope.() -> Unit = {
            val gridOffsetX = (cols * cellPx - widthPx) / 2f
            val gridOffsetY = (rows * cellPx - heightPx) / 2f
            val restSize = cellPx * 0.92f

            val near = ArrayList<Triple<Int, Int, Float>>(256)

            for (row in 0 until rows) {
                for (col in 0 until cols) {
                    val restX = col * cellPx + cellPx / 2f - gridOffsetX
                    val restY = row * cellPx + cellPx / 2f - gridOffsetY
                    val d = hypot((restX - focus.x).toDouble(), (restY - focus.y).toDouble()).toFloat()

                    if (d >= lensRadius) {
                        val bmpIndex = if (thumbnails.isNotEmpty()) (row * cols + col) % thumbnails.size else -1
                        val bmp = if (bmpIndex >= 0) thumbnails.getOrNull(bmpIndex) else null
                        if (bmp != null) {
                            withTransform({
                                translate(restX - restSize / 2f, restY - restSize / 2f)
                            }) {
                                drawImage(
                                    image = bmp.asImageBitmap(),
                                    dstOffset = IntOffset(0, 0),
                                    dstSize = IntSize(restSize.toInt().coerceAtLeast(1), restSize.toInt().coerceAtLeast(1))
                                )
                            }
                        }
                    } else {
                        near.add(Triple(row, col, d))
                    }
                }
            }

            near.sortByDescending { it.third }

            for ((row, col, dist) in near) {
                val bmpIndex = if (thumbnails.isNotEmpty()) (row * cols + col) % thumbnails.size else -1
                val bmp = if (bmpIndex >= 0) thumbnails.getOrNull(bmpIndex) else null

                val restX = col * cellPx + cellPx / 2f - gridOffsetX
                val restY = row * cellPx + cellPx / 2f - gridOffsetY

                val t = (dist / lensRadius).coerceIn(0f, 1f)
                val falloff = (1f - t) * (1f - t)
                val localScale = 1f + (maxScale - 1f) * falloff
                val swirl = swirlMax * falloff

                val dx = restX - focus.x
                val dy = restY - focus.y
                val angle = atan2(dy.toDouble(), dx.toDouble()).toFloat() + swirl
                val baseDist = hypot(dx.toDouble(), dy.toDouble()).toFloat()
                val scaledDist = baseDist * localScale

                val drawX = focus.x + cos(angle) * scaledDist
                val drawY = focus.y + sin(angle) * scaledDist
                val size = cellPx * localScale * 0.92f

                if (bmp != null) {
                    withTransform({
                        translate(drawX - size / 2f, drawY - size / 2f)
                    }) {
                        drawImage(
                            image = bmp.asImageBitmap(),
                            dstOffset = IntOffset(0, 0),
                            dstSize = IntSize(size.toInt().coerceAtLeast(1), size.toInt().coerceAtLeast(1))
                        )
                    }
                }
            }
        }

        val drawElevation: DrawScope.() -> Unit = {
            val gridOffsetX = (cols * cellPx - widthPx) / 2f
            val gridOffsetY = (rows * cellPx - heightPx) / 2f
            val bumpRadius = cellPx * 1.7f
            val skirtRadius = bumpRadius * 1.7f
            val maxLift = cellPx * 0.5f
            val maxElevScale = 1.5f
            val restSize = cellPx * 0.90f

            val near = ArrayList<Triple<Int, Int, Float>>(64)

            for (row in 0 until rows) {
                for (col in 0 until cols) {
                    val restX = col * cellPx + cellPx / 2f - gridOffsetX
                    val restY = row * cellPx + cellPx / 2f - gridOffsetY
                    val d = hypot((restX - focus.x).toDouble(), (restY - focus.y).toDouble()).toFloat()

                    if (d >= skirtRadius) {
                        val bmpIndex = if (thumbnails.isNotEmpty()) (row * cols + col) % thumbnails.size else -1
                        val bmp = if (bmpIndex >= 0) thumbnails.getOrNull(bmpIndex) else null
                        if (bmp != null) {
                            withTransform({
                                translate(restX - restSize / 2f, restY - restSize / 2f)
                            }) {
                                drawImage(
                                    image = bmp.asImageBitmap(),
                                    dstOffset = IntOffset(0, 0),
                                    dstSize = IntSize(restSize.toInt().coerceAtLeast(1), restSize.toInt().coerceAtLeast(1))
                                )
                            }
                        }
                    } else {
                        near.add(Triple(row, col, d))
                    }
                }
            }

            near.sortByDescending { it.third }

            for ((row, col, dist) in near) {
                val bmpIndex = if (thumbnails.isNotEmpty()) (row * cols + col) % thumbnails.size else -1
                val bmp = if (bmpIndex >= 0) thumbnails.getOrNull(bmpIndex) else null

                val restX = col * cellPx + cellPx / 2f - gridOffsetX
                val restY = row * cellPx + cellPx / 2f - gridOffsetY

                val influence = exp((-(dist * dist) / (2f * bumpRadius * bumpRadius)).toDouble()).toFloat()
                val scale = 1f + (maxElevScale - 1f) * influence
                val lift = maxLift * influence

                val dx = restX - focus.x
                val dy = restY - focus.y
                val dlen = hypot(dx.toDouble(), dy.toDouble()).toFloat().coerceAtLeast(1f)
                val skirtT = ((skirtRadius - dist) / (skirtRadius - bumpRadius)).coerceIn(0f, 1f)
                val drapePush = if (dist > bumpRadius) skirtT * skirtT * cellPx * 0.35f else 0f
                val drawXBase = if (dist > bumpRadius) restX + (dx / dlen) * drapePush else restX
                val baseY = if (dist > bumpRadius) restY + (dy / dlen) * drapePush else restY

                val w = cellPx * scale * 0.90f
                val drawX = drawXBase
                val drawY = baseY - lift

                if (influence > 0.04f) {
                    drawOval(
                        color = Color.Black.copy(alpha = influence * 0.4f),
                        topLeft = Offset(restX - w * 0.55f, restY + w * 0.42f - w * 0.16f),
                        size = Size(w * 1.1f, w * 0.32f)
                    )
                }

                if (bmp != null) {
                    withTransform({
                        translate(drawX - w / 2f, drawY - w / 2f)
                    }) {
                        drawImage(
                            image = bmp.asImageBitmap(),
                            dstOffset = IntOffset(0, 0),
                            dstSize = IntSize(w.toInt().coerceAtLeast(1), w.toInt().coerceAtLeast(1))
                        )
                    }
                }

                if (influence > 0.04f) {
                    val brush = Brush.radialGradient(
                        0f to Color.White.copy(alpha = 0.30f * influence),
                        0.55f to Color.White.copy(alpha = 0f),
                        1f to Color.Black.copy(alpha = 0.28f * influence),
                        center = Offset(drawX - w * 0.18f, drawY - w * 0.22f),
                        radius = (w * 0.75f).coerceAtLeast(1f)
                    )
                    drawRect(
                        brush = brush,
                        topLeft = Offset(drawX - w / 2f, drawY - w / 2f),
                        size = Size(w, w)
                    )
                }
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { isDragging = true },
                        onDragEnd = { isDragging = false },
                        onDragCancel = { isDragging = false }
                    ) { change, _ ->
                        change.consume()
                        focus = change.position
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { pos -> focus = pos },
                        onDoubleTap = { selectedPhoto?.let(onOpenPhoto) }
                    )
                }
        ) {
            when (mode) {
                InteractionMode.BURBUJA -> drawBubble()
                InteractionMode.ELEVACION -> drawElevation()
            }
        }

        if (selectedPhoto != null) {
            Box(
                modifier = Modifier
                    .padding(start = 20.dp, top = 48.dp, end = 20.dp)
            ) {
                Text(
                    text = selectedPhoto.displayName,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Start
                )
            }
        }
    }
}
