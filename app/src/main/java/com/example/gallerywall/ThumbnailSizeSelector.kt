package com.example.gallerywall

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Fila de chips para elegir la densidad de la malla. Cada chip muestra un
 * punto cuyo tamaño representa proporcionalmente el tamaño real de celda,
 * para que sea obvio cuál es "nano" y cuál es "grande" de un vistazo.
 */
@Composable
fun ThumbnailSizeSelector(
    selected: ThumbnailSize,
    onSelect: (ThumbnailSize) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        ThumbnailSize.entries.forEachIndexed { index, size ->
            val isSelected = size == selected
            Box(
                modifier = Modifier
                    .padding(horizontal = 6.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) Color.White.copy(alpha = 0.18f) else Color.Transparent
                    )
                    .clickable { onSelect(size) }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Punto de vista previa: crece de nano a grande.
                    val dotSize = 6.dp + (size.cellDp.value / 50f * 14).dp
                    Box(
                        modifier = Modifier
                            .size(dotSize)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) Color(0xFF3FC7F4) else Color.White.copy(alpha = 0.6f)
                            )
                    )
                    Text(
                        text = "  ${size.label}",
                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.65f),
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}
