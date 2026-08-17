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

/**
 * Composable que muestra un mapa interactivo de OpenStreetMap mediante OsmDroid.
 *
 * Inicializa el mapa con el centro y nivel de zoom indicados, habilita controles
 * multitáctiles y entrega la vista configurada a través del callback [onMapReady]
 * para agregar marcadores o zonas.
 *
 * @param modifier Modificador de Compose para ajustar tamaño y diseño del mapa.
 * @param center Punto geográfico central del mapa al iniciar.
 * @param zoomLevel Nivel de zoom inicial del mapa.
 * @param onMapReady Callback invocado cuando el mapa está listo para recibir overlays.
 */
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

/**
 * Agrega un marcador simple en las coordenadas indicadas sobre el mapa.
 *
 * Los marcadores deben agregarse después de los perímetros para quedar
 * visualmente por encima de ellos en la pila de overlays.
 *
 * @param point Coordenadas geográficas donde se coloca el marcador.
 * @param title Título descriptivo del marcador.
 */
fun MapView.addSimpleMarker(point: GeoPoint, title: String) {
    val marker = Marker(this)
    marker.position = point
    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
    marker.title = title
    this.overlays.add(marker)
    this.invalidate()
}

/**
 * Dibuja el perímetro circular de una zona segura sobre el mapa.
 *
 * @param center Centro geográfico de la zona segura.
 * @param radiusInMeters Radio de la zona en metros.
 * @param color Color ARGB para el relleno y borde del círculo.
 * @param title Título descriptivo de la zona segura.
 */
fun MapView.addSafeZoneCircle(
    center: GeoPoint,
    radiusInMeters: Double,
    color: Int,
    title: String
) {
    val circle = Polygon(this)
    circle.points = Polygon.pointsAsCircle(center, radiusInMeters)
    circle.fillPaint.color = color
    circle.outlinePaint.color = color
    circle.outlinePaint.strokeWidth = 2f
    circle.title = title
    this.overlays.add(circle)
    this.invalidate()
}
