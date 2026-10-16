package com.mototrip.ui.maps

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.mototrip.domain.model.Stop
import com.mototrip.domain.model.TrackPoint
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.annotations.PolylineOptions

@Composable
fun TripMapView(
    trackPoints: List<TrackPoint>,
    stops: List<Stop>,
    modifier: Modifier = Modifier
) {
    var forceCanvasView by remember { mutableStateOf(false) }

    if (trackPoints.isEmpty() || forceCanvasView) {
        Box(modifier = modifier) {
            TrackPolylineCanvas(trackPoints = trackPoints, stops = stops, modifier = Modifier.fillMaxSize())
            if (trackPoints.isNotEmpty()) {
                FilterChip(
                    selected = true,
                    onClick = { forceCanvasView = false },
                    label = { Text("Trazado Offline") },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }
        return
    }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember { MapView(context) }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDestroy()
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                view.getMapAsync { map ->
                    val styleUri = "https://demotiles.maplibre.org/style.json"
                    map.setStyle(Style.Builder().fromUri(styleUri)) {
                        map.clear()

                        val latLngList = trackPoints.map { LatLng(it.latitude, it.longitude) }

                        if (latLngList.isNotEmpty()) {
                            // Draw high-visibility safety amber polyline
                            map.addPolyline(
                                PolylineOptions()
                                    .addAll(latLngList)
                                    .color(android.graphics.Color.parseColor("#FF9100"))
                                    .width(6f)
                            )

                            // Add start marker (Green)
                            map.addMarker(
                                MarkerOptions()
                                    .position(latLngList.first())
                                    .title("Inicio de viaje")
                            )

                            // Add end marker (Blue/Red)
                            if (latLngList.size > 1) {
                                map.addMarker(
                                    MarkerOptions()
                                        .position(latLngList.last())
                                        .title("Posición actual / Fin")
                                )
                            }

                            // Add stops markers
                            stops.forEach { stop ->
                                val lat = stop.latitude
                                val lon = stop.longitude
                                if (lat != null && lon != null) {
                                    map.addMarker(
                                        MarkerOptions()
                                            .position(LatLng(lat, lon))
                                            .title("${stop.type.name}: ${stop.name ?: "Parada"}")
                                    )
                                }
                            }

                            // Center and fit camera protecting against single or identical coordinates
                            if (latLngList.size == 1) {
                                map.easeCamera(CameraUpdateFactory.newLatLngZoom(latLngList.first(), 15.0), 800)
                            } else {
                                val first = latLngList.first()
                                val hasVariation = latLngList.any { it.latitude != first.latitude || it.longitude != first.longitude }
                                if (hasVariation) {
                                    val boundsBuilder = LatLngBounds.Builder()
                                    latLngList.forEach { boundsBuilder.include(it) }
                                    val bounds = boundsBuilder.build()
                                    map.easeCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100), 800)
                                } else {
                                    map.easeCamera(CameraUpdateFactory.newLatLngZoom(first, 15.0), 800)
                                }
                            }
                        }
                    }
                }
            }
        )

        FilterChip(
            selected = false,
            onClick = { forceCanvasView = true },
            label = { Text("Ver Trazado") },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp),
            colors = FilterChipDefaults.filterChipColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                labelColor = MaterialTheme.colorScheme.onSurface
            )
        )
    }
}
