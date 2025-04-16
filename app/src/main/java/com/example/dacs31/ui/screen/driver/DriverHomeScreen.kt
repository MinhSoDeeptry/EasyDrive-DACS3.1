package com.example.dacs31.ui.screen.driver

import android.Manifest
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import com.example.dacs31.R
import com.example.dacs31.ui.screen.componentsUI.BottomControlBar
import com.example.dacs31.ui.screen.componentsUI.TopControlBar
import com.example.dacs31.utils.getBitmapFromVectorDrawable
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
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

@Composable
fun DriverHomeScreen(navController: NavController) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var pointAnnotationManager by remember { mutableStateOf<PointAnnotationManager?>(null) }
    var userLocation by remember { mutableStateOf<Point?>(null) }
    var isConnected by remember { mutableStateOf(false) }
    var showPermissionDeniedDialog by remember { mutableStateOf(false) }

    // Firebase Realtime Database
    val database = Firebase.database
    val connectedRef = database.getReference("driver_status/connected")

    // Kiểm tra kết nối Firebase và đồng bộ trạng thái isConnected
    LaunchedEffect(Unit) {
        // Lắng nghe thay đổi từ Firebase
        connectedRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val value = snapshot.getValue(Boolean::class.java)
                if (value != null) {
                    isConnected = value
                    Log.d("Firebase", "Trạng thái isConnected cập nhật từ Firebase: $value")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Lỗi khi đọc dữ liệu: ${error.message}")
            }
        })

        // Kiểm tra kết nối Firebase bằng cách ghi dữ liệu thử nghiệm
        val testRef = database.getReference("test_message")
        testRef.setValue("Hello, Firebase!")
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

                            // Khởi tạo PointAnnotationManager ngay sau khi style tải
                            val annotationApi = annotations
                            pointAnnotationManager = annotationApi.createPointAnnotationManager()

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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BottomControlBar(
                navController = navController,
                showConnectButton = true, // Hiển thị nút Connect/Disconnect
                isConnected = isConnected,
                onConnectClick = {
                    isConnected = !isConnected
                    connectedRef.setValue(isConnected)
                        .addOnSuccessListener {
                            Log.d("Firebase", "Cập nhật trạng thái isConnected thành công: $isConnected")
                        }
                        .addOnFailureListener { e ->
                            Log.e("Firebase", "Cập nhật trạng thái thất bại: ${e.message}")
                        }
                }
            )
        }
    }
}