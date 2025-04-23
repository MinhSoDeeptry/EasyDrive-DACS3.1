package com.example.dacs31.map

import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.dacs31.R
import com.example.dacs31.utils.getBitmapFromVectorDrawable
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.geojson.*
import com.mapbox.maps.*
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.lineLayer
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.sources.getSourceAs
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.*
import com.mapbox.maps.plugin.gestures.*
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.plugin.locationcomponent.createDefault2DPuck
import com.mapbox.turf.TurfMeasurement

@Composable
fun MapComponent(
    modifier: Modifier = Modifier,
    routePoints: List<Point> = emptyList(),
    fromPoint: Point? = null,
    toPoint: Point? = null,
    driverLocation: Point? = null,
    userLocation: Point? = null,
    nearbyDrivers: List<Point> = emptyList(),
    onUserLocationUpdated: (Point) -> Unit = {},
    onMapReady: (MapView, PointAnnotationManager) -> Unit = { _, _ -> },
    onMapViewReady: (MapView) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var pointAnnotationManager by remember { mutableStateOf<PointAnnotationManager?>(null) }
    var clickListener by remember { mutableStateOf<OnMapClickListener?>(null) }
    var moveListener by remember { mutableStateOf<OnMoveListener?>(null) }

    // Quản lý lifecycle của MapView và cleanup listeners
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView?.onStart()
                Lifecycle.Event.ON_STOP -> mapView?.onStop()
                Lifecycle.Event.ON_DESTROY -> {
                    clickListener?.let { listener ->
                        mapView?.gestures?.removeOnMapClickListener(listener)
                    }
                    moveListener?.let { listener ->
                        mapView?.gestures?.removeOnMoveListener(listener)
                    }
                    mapView?.onDestroy()
                    mapView = null
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            clickListener?.let { listener ->
                mapView?.gestures?.removeOnMapClickListener(listener)
            }
            moveListener?.let { listener ->
                mapView?.gestures?.removeOnMoveListener(listener)
            }
            mapView?.onDestroy()
            mapView = null
        }
    }

    AndroidView(
        modifier = modifier.fillMaxSize(), // Đảm bảo MapComponent chiếm toàn bộ không gian
        factory = { ctx ->
            MapView(ctx).apply {
                mapView = this
                setupMap(context, this) { annotationManager ->
                    pointAnnotationManager = annotationManager
                    onMapReady(this, annotationManager)
                    onMapViewReady(this)
                }

                // Kích hoạt location component
                location.updateSettings {
                    enabled = true
                    locationPuck = createDefault2DPuck(withBearing = false)
                    pulsingEnabled = true
                }
                location.addOnIndicatorPositionChangedListener { point ->
                    onUserLocationUpdated(point)
                }

                // Kích hoạt các gesture để di chuyển bản đồ
                gestures.updateSettings {
                    scrollEnabled = true
                    pinchToZoomEnabled = true
                    doubleTapToZoomInEnabled = true
                    doubleTouchToZoomOutEnabled = true
                    rotateEnabled = true
                }

                // Log trạng thái gesture để kiểm tra
                Log.d("MapComponent", "Gestures enabled - Scroll: ${gestures.scrollEnabled}, PinchToZoom: ${gestures.pinchToZoomEnabled}")

                // Thêm listener để kiểm tra sự kiện chạm và di chuyển
                clickListener = OnMapClickListener { point ->
                    Log.d("MapComponent", "Map clicked at: $point")
                    false
                }

                moveListener = object : OnMoveListener {
                    override fun onMoveBegin(detector: MoveGestureDetector) {
                        Log.d("MapComponent", "Map move started")
                    }

                    override fun onMove(detector: MoveGestureDetector): Boolean {
                        Log.d("MapComponent", "Map moving")
                        return false
                    }

                    override fun onMoveEnd(detector: MoveGestureDetector) {
                        Log.d("MapComponent", "Map move ended")
                    }
                }

                clickListener?.let { gestures.addOnMapClickListener(it) }
                moveListener?.let { gestures.addOnMoveListener(it) }
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
                    nearbyDrivers = nearbyDrivers,
                    pointAnnotationManager = pointAnnotationManager,
                    mapView = mv
                )
            }
        }
    )
}

private fun setupMap(
    context: Context,
    mapView: MapView,
    onMapReady: (PointAnnotationManager) -> Unit
) {
    val mapboxMap = mapView.getMapboxMap()
    mapboxMap.loadStyle(Style.MAPBOX_STREETS) { style ->
        try {
            // Thêm hình ảnh cho các marker
            val userBitmap = context.getBitmapFromVectorDrawable(R.drawable.baseline_location_on_24)
            val startBitmap = context.getBitmapFromVectorDrawable(R.drawable.baseline_flag_24)
            val endBitmap = context.getBitmapFromVectorDrawable(R.drawable.baseline_destination_24)
            val driverBitmap = context.getBitmapFromVectorDrawable(R.drawable.baseline_driver_24)

            style.addImage("user-location-marker", userBitmap)
            style.addImage("start-marker", startBitmap)
            style.addImage("end-marker", endBitmap)
            style.addImage("driver-marker", driverBitmap)

            // Tạo source và layer cho route
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

            // Tạo annotation manager để thêm marker
            val annotationApi = mapView.annotations
            val annotationManager = annotationApi.createPointAnnotationManager()

            onMapReady(annotationManager)
        } catch (e: Exception) {
            Log.e("Mapbox", "Lỗi setup map: ${e.message}")
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
    val source = style.getSourceAs<GeoJsonSource>("route-source")
    pointAnnotationManager?.deleteAll()

    // Cập nhật route trên bản đồ
    if (routePoints.isNotEmpty()) {
        val lineString = LineString.fromLngLats(routePoints)
        val feature = Feature.fromGeometry(lineString)
        source?.featureCollection(FeatureCollection.fromFeature(feature))

        // Thêm marker cho điểm bắt đầu và kết thúc
        fromPoint?.let {
            pointAnnotationManager?.create(
                PointAnnotationOptions()
                    .withPoint(it)
                    .withIconImage("start-marker")
            )
        }
        toPoint?.let {
            pointAnnotationManager?.create(
                PointAnnotationOptions()
                    .withPoint(it)
                    .withIconImage("end-marker")
            )
        }

        // Cập nhật camera để hiển thị toàn bộ route
        if (fromPoint != null && toPoint != null) {
            val bounds = TurfMeasurement.bbox(LineString.fromLngLats(routePoints))
            val camera = CameraOptions.Builder()
                .center(Point.fromLngLat((bounds[0] + bounds[2]) / 2, (bounds[1] + bounds[3]) / 2))
                .zoom(12.0)
                .build()
            mapView.getMapboxMap().setCamera(camera)
        }
    } else {
        source?.featureCollection(FeatureCollection.fromFeatures(emptyList()))
    }

    // Thêm marker cho vị trí người dùng
    userLocation?.let {
        pointAnnotationManager?.create(
            PointAnnotationOptions()
                .withPoint(it)
                .withIconImage("user-location-marker")
        )
    }

    // Thêm marker cho vị trí tài xế
    driverLocation?.let {
        pointAnnotationManager?.create(
            PointAnnotationOptions()
                .withPoint(it)
                .withIconImage("driver-marker")
        )
    }

    // Thêm marker cho các tài xế gần đó
    nearbyDrivers.forEach {
        pointAnnotationManager?.create(
            PointAnnotationOptions()
                .withPoint(it)
                .withIconImage("driver-marker")
        )
    }
}