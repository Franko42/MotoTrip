package com.mototrip.ui.maps

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.mototrip.domain.model.Stop
import com.mototrip.domain.model.TrackPoint

@Composable
fun TrackPolylineCanvas(
    trackPoints: List<TrackPoint>,
    stops: List<Stop>,
    modifier: Modifier = Modifier
) {
    if (trackPoints.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Sin puntos de recorrido GPS registrados.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val minLat = trackPoints.minOf { it.latitude }
    val maxLat = trackPoints.maxOf { it.latitude }
    val minLon = trackPoints.minOf { it.longitude }
    val maxLon = trackPoints.maxOf { it.longitude }

    val latSpan = (maxLat - minLat).coerceAtLeast(0.0001)
    val lonSpan = (maxLon - minLon).coerceAtLeast(0.0001)

    val routeColor = MaterialTheme.colorScheme.primary
    val startColor = Color(0xFF2E7D32) // Green
    val finishColor = Color(0xFFC62828) // Red
    val stopColor = Color(0xFFF57C00) // Amber

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val paddingPx = 32.dp.toPx()
            val canvasW = size.width - 2 * paddingPx
            val canvasH = size.height - 2 * paddingPx

            fun project(lat: Double, lon: Double): Offset {
                // Invert latitude so North is at the top
                val x = paddingPx + ((lon - minLon) / lonSpan * canvasW).toFloat()
                val y = paddingPx + ((maxLat - lat) / latSpan * canvasH).toFloat()
                return Offset(x, y)
            }

            // Draw route path
            val path = Path()
            val first = trackPoints.first()
            val firstOffset = project(first.latitude, first.longitude)
            path.moveTo(firstOffset.x, firstOffset.y)

            for (i in 1 until trackPoints.size) {
                val pt = trackPoints[i]
                val offset = project(pt.latitude, pt.longitude)
                path.lineTo(offset.x, offset.y)
            }

            drawPath(
                path = path,
                color = routeColor,
                style = Stroke(width = 6.dp.toPx())
            )

            // Draw Start marker
            drawCircle(color = startColor, radius = 10.dp.toPx(), center = firstOffset)

            // Draw Finish marker
            val last = trackPoints.last()
            val lastOffset = project(last.latitude, last.longitude)
            drawCircle(color = finishColor, radius = 10.dp.toPx(), center = lastOffset)

            // Draw Stop markers
            stops.forEach { stop ->
                val sLat = stop.latitude
                val sLon = stop.longitude
                if (sLat != null && sLon != null) {
                    val stopOffset = project(sLat, sLon)
                    drawCircle(color = stopColor, radius = 8.dp.toPx(), center = stopOffset)
                }
            }
        }

        Text(
            text = "Recorrido vectorial offline (GPS local)",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.align(Alignment.BottomStart)
        )
    }
}
