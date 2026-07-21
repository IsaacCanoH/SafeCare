package mx.utng.ich.safecare.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.infowindow.MarkerInfoWindow

@Composable
fun OsmMapView(
    modifier: Modifier = Modifier,
    center: GeoPoint = GeoPoint(21.1526, -100.9312),
    zoomLevel: Double = 15.0,
    onMapReady: (MapView) -> Unit = {}
) {
    val context = LocalContext.current
    val mapView = remember { MapView(context) }

    // Efecto para actualizar el centro cuando cambia externamente (ej. buscador)
    LaunchedEffect(center) {
        mapView.controller.animateTo(center)
    }

    DisposableEffect(Unit) {
        Configuration.getInstance().userAgentValue = context.packageName
        onDispose {
            mapView.onDetach()
        }
    }

    AndroidView(
        factory = {
            mapView.apply {
                setTileSource(TileSourceFactory.MAPNIK)
                controller.setZoom(zoomLevel)
                controller.setCenter(center)
                setMultiTouchControls(true)
                // Permitir que el mapa maneje sus propios eventos táctiles
                isClickable = true
                onMapReady(this)
            }
        },
        modifier = modifier,
        update = {
            // Se puede usar para actualizaciones de vista si es necesario
        }
    )
}

fun MapView.addSimpleMarker(point: GeoPoint, title: String) {
    val marker = Marker(this)
    marker.position = point
    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
    marker.title = title
    this.overlays.add(marker)
    this.invalidate()
}

fun MapView.addSafeZoneCircle(center: GeoPoint, radiusInMeters: Double, color: Int) {
    val circle = Polygon(this)
    circle.points = Polygon.pointsAsCircle(center, radiusInMeters)
    circle.fillPaint.color = color
    circle.outlinePaint.color = color
    circle.outlinePaint.strokeWidth = 2f
    this.overlays.add(circle)
    this.invalidate()
}
