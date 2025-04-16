package com.example.dacs31.ui.screen.customer

import android.Manifest
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import com.example.dacs31.R
import com.example.dacs31.ui.screen.componentsUI.TopControlBar
import com.example.dacs31.ui.screen.componentsUI.BottomControlBar
import com.example.dacs31.ui.screen.location.SelectAddressDialog
import com.example.dacs31.ui.screen.location.getRoute
import com.example.dacs31.utils.getBitmapFromVectorDrawable
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
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
import kotlinx.coroutines.launch

@Composable
fun CustomerHomeScreen(navController: NavController) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var pointAnnotationManager by remember { mutableStateOf<PointAnnotationManager?>(null) }
    var userLocation by remember { mutableStateOf<Point?>(null) }
    var showPermissionDeniedDialog by remember { mutableStateOf(false) }
    var showSelectAddressDialog by remember { mutableStateOf(false) }

    // Trạng thái để lưu tuyến đường
    var routePoints by remember { mutableStateOf<List<Point>>(emptyList()) }

    // Trạng thái để theo dõi chế độ được chọn: "Transport" hoặc "Delivery"
    var selectedMode by remember { mutableStateOf("Transport") }

    // Mapbox Access Token
    val mapboxAccessToken = context.getString(R.string.mapbox_access_token)

    // Coroutine scope để gọi API
    val coroutineScope = rememberCoroutineScope()

    // Kiểm tra kết nối Firebase
    LaunchedEffect(Unit) {
        val database = Firebase.database
        val myRef = database.getReference("test_message")
        myRef.setValue("Hello, Firebase!")
            .addOnSuccessListener {
                Log.d("FirebaseTest", "Ghi dữ liệu thành công!")
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseTest", "Ghi dữ liệu thất bại: ${e.message}")
            }
    }

    // Xin quyền vị trí
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            showPermissionDeniedDialog = true
        }
    }

    LaunchedEffect(Unit) {
        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    // Hiển thị dialog nếu quyền vị trí bị từ chối
    if (showPermissionDeniedDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDeniedDialog = false },
            title = { Text("Quyền vị trí bị từ chối") },
            text = { Text("Ứng dụng cần quyền vị trí để hiển thị bản đồ và vị trí của bạn. Vui lòng cấp quyền trong cài đặt.") },
            confirmButton = {
                TextButton(onClick = { showPermissionDeniedDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    // Hiển thị dialog "Select Address"
    if (showSelectAddressDialog) {
        SelectAddressDialog(
            onDismiss = { showSelectAddressDialog = false },
            onConfirm = { fromPoint, toPoint ->
                if (fromPoint != null && toPoint != null) {
                    coroutineScope.launch {
                        try {
                            routePoints = getRoute(fromPoint, toPoint, mapboxAccessToken)
                        } catch (e: Exception) {
                            Log.e("MapboxDirections", "Error fetching route: ${e.message}")
                            routePoints = emptyList()
                        }
                    }
                }
            },
            userLocation = userLocation,
            mapboxAccessToken = mapboxAccessToken
        )
    }

    // Quản lý lifecycle cho MapView
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

    // Giao diện chính
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Bản đồ Mapbox làm nền
        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    val mapboxMap = getMapboxMap()
                    mapboxMap.loadStyleUri(Style.MAPBOX_STREETS) { style ->
                        try {
                            val bitmap = ctx.getBitmapFromVectorDrawable(R.drawable.baseline_location_on_24)
                            val imageId = "user-location-marker"
                            style.addImage(imageId, bitmap)

                            // Cấu hình hiển thị vị trí người dùng
                            location.updateSettings {
                                enabled = true
                                locationPuck = createDefault2DPuck()
                                pulsingEnabled = true
                            }

                            // Khởi tạo PointAnnotationManager
                            val annotationApi = annotations
                            pointAnnotationManager = annotationApi.createPointAnnotationManager()

                            // Thêm source và layer để vẽ tuyến đường
                            style.addSource(
                                geoJsonSource("route-source") {
                                    featureCollection(FeatureCollection.fromFeatures(emptyList()))
                                }
                            )

                            style.addLayer(
                                lineLayer("route-layer", "route-source") {
                                    lineColor("#FF0000") // Màu đỏ cho tuyến đường
                                    lineWidth(5.0)
                                }
                            )

                            // Lắng nghe vị trí người dùng
                            location.addOnIndicatorPositionChangedListener { point ->
                                userLocation = point

                                // Di chuyển camera đến vị trí người dùng
                                mapboxMap.setCamera(
                                    CameraOptions.Builder()
                                        .center(point)
                                        .zoom(15.0)
                                        .build()
                                )

                                // Cập nhật marker
                                pointAnnotationManager?.deleteAll()
                                val pointAnnotationOptions = PointAnnotationOptions()
                                    .withPoint(point)
                                    .withIconImage(imageId)
                                pointAnnotationManager?.create(pointAnnotationOptions)
                            }
                        } catch (e: Exception) {
                            Log.e("Mapbox", "Lỗi khi tải style hoặc thêm marker: ${e.message}")
                        }
                    }
                    mapView = this
                }
            },
            update = { mapView ->
                // Cập nhật tuyến đường trên bản đồ
                mapView.getMapboxMap().getStyle { style ->
                    val source = style.getSourceAs<com.mapbox.maps.extension.style.sources.generated.GeoJsonSource>("route-source")
                    if (routePoints.isNotEmpty()) {
                        val lineString = LineString.fromLngLats(routePoints)
                        val feature = Feature.fromGeometry(lineString)
                        source?.featureCollection(FeatureCollection.fromFeature(feature))
                    } else {
                        source?.featureCollection(FeatureCollection.fromFeatures(emptyList()))
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Thanh điều khiển phía trên
        TopControlBar(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .align(Alignment.TopCenter)
        )

        // Thanh điều khiển phía dưới
        BottomControlBar(
            navController = navController,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        )

        // Các nút ở giữa, đặt sát gần BottomControlBar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 100.dp), // Khoảng cách từ BottomControlBar
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Nút "Rental" (cta_primary)
            Button(
                onClick = { /* TODO: Xử lý khi nhấn Rental */ },
                modifier = Modifier
                    .width(172.dp)
                    .height(54.dp)
                    .padding(start = 15.dp)
                    .clip(RoundedCornerShape(8.dp)),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFEDAE10) // Màu vàng từ cta_primary
                ),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text = "Rental",
                    color = Color.Black,
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 23.sp
                    )
                )
            }

            // Ô tìm kiếm (search với rectangle_3)
            Box(
                modifier = Modifier
                    .width(336.dp)
                    .height(48.dp)
                    .padding(horizontal = 28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFFFFBE7)) // Màu nền từ rectangle_3
                    .border(
                        BorderStroke(2.dp, Color(0xFFF3BD06)), // Viền từ rectangle_3
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { showSelectAddressDialog = true } // Mở dialog khi nhấn
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Where would you go?",
                        color = Color.Gray,
                        style = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 23.sp
                        )
                    )
                    Icon(
                        imageVector = Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = Color.Gray
                    )
                }
            }

            // Tab/Switch cho "Transport" và "Delivery"
            Row(
                modifier = Modifier
                    .width(336.dp)
                    .height(48.dp)
                    .padding(horizontal = 28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFFFFBE7)) // Màu nền mặc định
                    .border(
                        BorderStroke(2.dp, Color(0xFFF3BD06)), // Viền bao quanh toàn bộ Row
                        RoundedCornerShape(8.dp)
                    )
            ) {
                // Tab "Transport"
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(
                            if (selectedMode == "Transport") Color(0xFFEDAE10) else Color(0xFFFFFBE7)
                        )
                        .clickable { selectedMode = "Transport" },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Transport",
                        color = if (selectedMode == "Transport") Color.White else Color(0xFF414141),
                        style = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 23.sp
                        )
                    )
                }

                // Tab "Delivery"
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(
                            if (selectedMode == "Delivery") Color(0xFFEDAE10) else Color(0xFFFFFBE7)
                        )
                        .clickable { selectedMode = "Delivery" },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Delivery",
                        color = if (selectedMode == "Delivery") Color.White else Color(0xFF414141),
                        style = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 23.sp
                        )
                    )
                }
            }
        }
    }
}