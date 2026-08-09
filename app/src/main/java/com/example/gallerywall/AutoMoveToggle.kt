package com.example.gallerywall

import androidx.compose.animation.core.animateDpAsState
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
 * Interruptor tipo switch para que el lente derive solo cuando no lo tocas.
 * Apagado por defecto: el usuario lo prende si quiere ese modo "demo".
 */
@Composable
fun AutoMoveToggle(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val trackColor = if (enabled) Color(0xFF3FC7F4) else Color.White.copy(alpha = 0.25f)
    val knobOffset by animateDpAsState(if (enabled) 18.dp else 2.dp, label = "knob")

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable { onToggle(!enabled) }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Auto",
            color = if (enabled) Color.White else Color.White.copy(alpha = 0.6f),
            fontSize = 12.sp,
            fontWeight = if (enabled) FontWeight.Bold else FontWeight.Normal
        )
        Box(
            modifier = Modifier
                .padding(start = 8.dp)
                .size(width = 36.dp, height = 20.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(trackColor)
        ) {
            Box(
                modifier = Modifier
                    .padding(start = knobOffset, top = 2.dp)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }
    }
}
