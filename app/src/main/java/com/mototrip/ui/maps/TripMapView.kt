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
    if (trackPoints.isEmpty()) {
        TrackPolylineCanvas(trackPoints = trackPoints, stops = stops, modifier = modifier)
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
                            // Draw polyline
                            map.addPolyline(
                                PolylineOptions()
                                    .addAll(latLngList)
                                    .color(android.graphics.Color.parseColor("#1E88E5"))
                                    .width(5f)
                            )

                            // Add start and end markers
                            map.addMarker(
                                MarkerOptions()
                                    .position(latLngList.first())
                                    .title("Inicio")
                            )

                            if (latLngList.size > 1) {
                                map.addMarker(
                                    MarkerOptions()
                                        .position(latLngList.last())
                                        .title("Destino / Fin")
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

                            // Center and fit camera
                            val boundsBuilder = LatLngBounds.Builder()
                            latLngList.forEach { boundsBuilder.include(it) }
                            val bounds = boundsBuilder.build()
                            map.easeCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100), 1000)
                        }
                    }
                }
            }
        )
    }
}
