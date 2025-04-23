package com.example.dacs31.map

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.dacs31.R
import com.example.dacs31.utils.getBitmapFromVectorDrawable
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.plugin.locationcomponent.createDefault2DPuck
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.lineLayer
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.sources.getSourceAs
import com.mapbox.turf.TurfMeasurement

@Composable
fun MapComponent(
    modifier: Modifier = Modifier,
    routePoints: List<Point> = emptyList(),
    fromPoint: Point? = null,
    toPoint: Point? = null,
    driverLocation: Point? = null,
    userLocation: Point? = null,
    nearbyDrivers: List<Point> = emptyList(), // Thêm tham số để hiển thị tài xế lân cận
    onUserLocationUpdated: (Point) -> Unit = {},
    onMapReady: (MapView, PointAnnotationManager) -> Unit = { _, _ -> },
    onMapViewReady: (MapView) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var pointAnnotationManager by remember { mutableStateOf<PointAnnotationManager?>(null) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView?.onStart()
                Lifecycle.Event.ON_STOP -> mapView?.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView?.onDestroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    AndroidView(
        factory = { ctx ->
            MapView(ctx).apply {
                setupMap(context, this) { annotationManager ->
                    pointAnnotationManager = annotationManager
                    onMapReady(this, annotationManager)
                    onMapViewReady(this)
                }
                location.addOnIndicatorPositionChangedListener { point ->
                    onUserLocationUpdated(point)
                }
                mapView = this
            }
        },
        update = { mv ->
            mv.getMapboxMap().getStyle { style ->
                updateMap(
                    style = style,
                    routePoints = routePoints,
                    fromPoint = fromPoint,
                    toPoint = toPoint,
                    driverLocation = driverLocation,
                    userLocation = userLocation,
                    nearbyDrivers = nearbyDrivers, // Truyền danh sách tài xế lân cận
                    pointAnnotationManager = pointAnnotationManager,
                    mapView = mv
                )
            }
        },
        modifier = modifier
    )
}

private fun setupMap(
    context: Context,
    mapView: MapView,
    onMapReady: (PointAnnotationManager) -> Unit
) {
    val mapboxMap = mapView.getMapboxMap()
    mapboxMap.loadStyleUri(Style.MAPBOX_STREETS) { style ->
        try {
            val userBitmap = context.getBitmapFromVectorDrawable(R.drawable.baseline_location_on_24)
            val startBitmap = context.getBitmapFromVectorDrawable(R.drawable.baseline_flag_24)
            val endBitmap = context.getBitmapFromVectorDrawable(R.drawable.baseline_destination_24)
            val driverBitmap = context.getBitmapFromVectorDrawable(R.drawable.baseline_driver_24)

            style.addImage("user-location-marker", userBitmap)
            style.addImage("start-marker", startBitmap)
            style.addImage("end-marker", endBitmap)
            style.addImage("driver-marker", driverBitmap)

            mapView.location.updateSettings {
                enabled = true
                locationPuck = createDefault2DPuck()
                pulsingEnabled = true
            }

            val annotationApi = mapView.annotations
            val annotationManager = annotationApi.createPointAnnotationManager()

            style.addSource(
                geoJsonSource("route-source") {
                    featureCollection(FeatureCollection.fromFeatures(emptyList()))
                }
            )

            style.addLayer(
                lineLayer("route-layer", "route-source") {
                    lineColor("#FF0000")
                    lineWidth(5.0)
                }
            )

            onMapReady(annotationManager)
        } catch (e: Exception) {
            Log.e("Mapbox", "Lỗi khi tải style hoặc thêm marker: ${e.message}")
        }
    }
}

private fun updateMap(
    style: Style,
    routePoints: List<Point>,
    fromPoint: Point?,
    toPoint: Point?,
    driverLocation: Point?,
    userLocation: Point?,
    nearbyDrivers: List<Point>,
    pointAnnotationManager: PointAnnotationManager?,
    mapView: MapView
) {
    Log.d("MapComponent", "Updating map with routePoints: $routePoints, driverLocation: $driverLocation, nearbyDrivers: $nearbyDrivers")
    val source = style.getSourceAs<com.mapbox.maps.extension.style.sources.generated.GeoJsonSource>("route-source")

    // Xóa tất cả các marker cũ
    pointAnnotationManager?.deleteAll()

    if (routePoints.isNotEmpty()) {
        val lineString = LineString.fromLngLats(routePoints)
        val feature = Feature.fromGeometry(lineString)
        source?.featureCollection(FeatureCollection.fromFeature(feature))
        Log.d("MapComponent", "Route drawn on map")

        fromPoint?.let { from ->
            val startMarker = PointAnnotationOptions()
                .withPoint(from)
                .withIconImage("start-marker")
            pointAnnotationManager?.create(startMarker)
        }
        toPoint?.let { to ->
            val endMarker = PointAnnotationOptions()
                .withPoint(to)
                .withIconImage("end-marker")
            pointAnnotationManager?.create(endMarker)
        }

        if (fromPoint != null && toPoint != null) {
            val bounds = TurfMeasurement.bbox(LineString.fromLngLats(routePoints))
            mapView.getMapboxMap().setCamera(
                CameraOptions.Builder()
                    .center(Point.fromLngLat((bounds[0] + bounds[2]) / 2, (bounds[1] + bounds[3]) / 2))
                    .zoom(12.0)
                    .build()
            )
            Log.d("MapComponent", "Camera adjusted to show route")
        }
    } else {
        source?.featureCollection(FeatureCollection.fromFeatures(emptyList()))
        Log.w("MapComponent", "No route points to draw")
    }

    // Hiển thị vị trí người dùng
    userLocation?.let { user ->
        pointAnnotationManager?.create(
            PointAnnotationOptions()
                .withPoint(user)
                .withIconImage("user-location-marker")
        )
        Log.d("MapComponent", "User marker added at: $user")
    }

    // Hiển thị vị trí tài xế chính (nếu có)
    driverLocation?.let { driver ->
        pointAnnotationManager?.create(
            PointAnnotationOptions()
                .withPoint(driver)
                .withIconImage("driver-marker")
        )
        Log.d("MapComponent", "Driver marker added at: $driver")
    }

    // Hiển thị các tài xế lân cận
    nearbyDrivers.forEach { driver ->
        pointAnnotationManager?.create(
            PointAnnotationOptions()
                .withPoint(driver)
                .withIconImage("driver-marker")
        )
    }
    Log.d("MapComponent", "Nearby drivers added: $nearbyDrivers")
}