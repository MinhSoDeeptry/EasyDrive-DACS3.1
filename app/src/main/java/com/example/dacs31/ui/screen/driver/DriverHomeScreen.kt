package com.example.dacs31.ui.screen.driver

import android.Manifest
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.dacs31.R
import com.example.dacs31.data.AuthRepository
import com.example.dacs31.map.MapComponent
import com.example.dacs31.ui.screen.componentsUI.BottomControlBar
import com.example.dacs31.ui.screen.componentsUI.TopControlBar
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.ListenerRegistration
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import kotlinx.coroutines.launch

@Composable
fun DriverHomeScreen(
    navController: NavController,
    authRepository: AuthRepository
) {
    val context = LocalContext.current
    var userLocation by remember { mutableStateOf<Point?>(null) } // Vị trí tài xế
    var showPermissionDeniedDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isConnected by remember { mutableStateOf(false) }
    var incomingRequest by remember { mutableStateOf<RideRequest?>(null) } // Lưu yêu cầu
    var selectedRequest by remember { mutableStateOf<RideRequest?>(null) } // Yêu cầu đã chấp nhận
    var fromPoint by remember { mutableStateOf<Point?>(null) } // Vị trí khách hàng
    var toPoint by remember { mutableStateOf<Point?>(null) } // Đích đến
    var routePoints by remember { mutableStateOf<List<Point>>(emptyList()) }

    // Lấy UID của tài xế
    val driverId = authRepository.getCurrentUser()?.uid
    val mapboxAccessToken = context.getString(R.string.mapbox_access_token)
    val coroutineScope = rememberCoroutineScope()

    // Firestore
    val db = Firebase.firestore
    val driverRef = db.collection("drivers").document(driverId ?: "unknown_driver")
    val requestsCollection = db.collection("requests")

    // Đồng bộ trạng thái isConnected
    LaunchedEffect(driverId) {
        if (driverId == null) {
            errorMessage = "Vui lòng đăng nhập để tiếp tục."
            navController.navigate("signin")
            return@LaunchedEffect
        }

        driverRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("Firestore", "Lỗi khi đọc trạng thái isConnected: ${error.message}")
                return@addSnapshotListener
            }
            val value = snapshot?.getBoolean("isConnected") ?: false
            isConnected = value
            Log.d("Firestore", "Trạng thái isConnected cập nhật: $value")
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

    // Hiển thị dialog lỗi
    if (errorMessage != null) {
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            title = { Text("Lỗi") },
            text = { Text(errorMessage!!) },
            confirmButton = {
                TextButton(onClick = { errorMessage = null }) {
                    Text("OK")
                }
            }
        )
    }

    // Cập nhật vị trí tài xế vào Firestore
    LaunchedEffect(userLocation) {
        if (driverId != null && userLocation != null) {
            val locationData = mapOf(
                "location" to mapOf(
                    "latitude" to userLocation!!.latitude(),
                    "longitude" to userLocation!!.longitude()
                ),
                "isConnected" to isConnected
            )
            driverRef.set(locationData, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener {
                    Log.d("Firestore", "Cập nhật vị trí tài xế thành công: $userLocation")
                }
                .addOnFailureListener { e ->
                    Log.e("Firestore", "Cập nhật vị trí thất bại: ${e.message}")
                }
        }
    }

    // Lắng nghe yêu cầu đặt xe từ khách hàng
    var requestListener by remember { mutableStateOf<ListenerRegistration?>(null) }

    LaunchedEffect(driverId, isConnected) {
        requestListener?.remove()
        requestListener = null

        if (driverId == null || !isConnected) {
            incomingRequest = null
            selectedRequest = null
            fromPoint = null
            toPoint = null
            routePoints = emptyList()
            return@LaunchedEffect
        }

        requestListener = requestsCollection
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("Firestore", "Lỗi khi lắng nghe yêu cầu: ${error.message}")
                    errorMessage = "Không thể tải yêu cầu: ${error.message}"
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val requestList = snapshot.documents.mapNotNull { doc ->
                        val pickupData = doc.get("pickupLocation") as? Map<String, Double>
                        val destData = doc.get("destination") as? Map<String, Double>
                        val customerId = doc.getString("customerId") ?: return@mapNotNull null
                        val status = doc.getString("status") ?: return@mapNotNull null

                        val pickupLocation = pickupData?.let {
                            Point.fromLngLat(it["longitude"] ?: 0.0, it["latitude"] ?: 0.0)
                        } ?: return@mapNotNull null

                        val destination = destData?.let {
                            Point.fromLngLat(it["longitude"] ?: 0.0, it["latitude"] ?: 0.0)
                        } ?: return@mapNotNull null

                        RideRequest(
                            id = doc.id,
                            customerId = customerId,
                            pickupLocation = pickupLocation,
                            destination = destination,
                            status = status
                        )
                    }

                    // Chỉ hiển thị dialog nếu chưa có yêu cầu nào được chấp nhận
                    if (selectedRequest == null) {
                        incomingRequest = requestList.firstOrNull()
                    }
                }
            }
    }

    // Hiển thị dialog khi có yêu cầu đặt xe
    if (incomingRequest != null && selectedRequest == null) {
        AlertDialog(
            onDismissRequest = { incomingRequest = null },
            title = { Text("Yêu cầu đặt xe mới") },
            text = { Text("Bạn có một yêu cầu đặt xe mới từ khách hàng ${incomingRequest!!.customerId}. Chấp nhận không?") },
            confirmButton = {
                TextButton(onClick = {
                    if (driverId == null) {
                        errorMessage = "Vui lòng đăng nhập để tiếp tục."
                        navController.navigate("signin")
                        return@TextButton
                    }

                    requestsCollection.document(incomingRequest!!.id)
                        .update(
                            mapOf(
                                "status" to "accepted",
                                "driverId" to driverId
                            )
                        )
                        .addOnSuccessListener {
                            selectedRequest = incomingRequest
                            incomingRequest = null
                            Log.d("Firestore", "Chấp nhận yêu cầu thành công: ${selectedRequest!!.id}")
                        }
                        .addOnFailureListener { e ->
                            Log.e("Firestore", "Chấp nhận yêu cầu thất bại: ${e.message}")
                            errorMessage = "Không thể chấp nhận yêu cầu: ${e.message}"
                        }
                }) {
                    Text("Chấp nhận")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    incomingRequest = null
                }) {
                    Text("Từ chối")
                }
            }
        )
    }

    // Lấy route sau khi chấp nhận yêu cầu
    LaunchedEffect(selectedRequest) {
        selectedRequest?.let { request ->
            fromPoint = request.pickupLocation
            toPoint = request.destination
            coroutineScope.launch {
                try {
                    val (points, _) = com.example.dacs31.ui.screen.location.getRoute(
                        request.pickupLocation,
                        request.destination,
                        mapboxAccessToken
                    )
                    routePoints = points
                    Log.d("DriverHomeScreen", "Route points received: $routePoints")
                } catch (e: Exception) {
                    Log.e("MapboxDirections", "Error fetching route: ${e.message}")
                    routePoints = emptyList()
                }
            }
        }
    }

    // Giao diện chính
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Lưu instance của MapView để điều chỉnh camera
        var mapViewInstance by remember { mutableStateOf<MapView?>(null) }

        // Bản đồ Mapbox làm nền
        MapComponent(
            modifier = Modifier.fillMaxSize(),
            routePoints = routePoints,
            fromPoint = fromPoint,
            toPoint = toPoint,
            userLocation = userLocation,
            onUserLocationUpdated = { point ->
                userLocation = point
                Log.d("DriverHomeScreen", "Driver location updated: $userLocation")
            },
            onMapReady = { mapView, pointAnnotationManager ->
                Log.d("DriverHomeScreen", "Map is ready")
            },
            onMapViewReady = { mapView ->
                mapViewInstance = mapView
            }
        )

        // Tự động di chuyển camera đến vị trí người dùng khi có userLocation
        LaunchedEffect(userLocation) {
            userLocation?.let { point ->
                mapViewInstance?.getMapboxMap()?.setCamera(
                    CameraOptions.Builder()
                        .center(point)
                        .zoom(15.0)
                        .build()
                )
                Log.d("DriverHomeScreen", "Camera moved to user location: $point")
            }
        }

        // Thanh điều khiển phía trên
        TopControlBar(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .align(Alignment.TopCenter),
            navController = navController,
            authRepository = authRepository
        )

        // Thanh điều khiển phía dưới
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (selectedRequest != null) {
                // Hiển thị thông tin chuyến đi đã chấp nhận
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp)),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Chuyến đi đang thực hiện",
                        style = TextStyle(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(8.dp)
                    )
                    Text(
                        text = "Điểm đón: (${selectedRequest!!.pickupLocation.latitude()}, ${selectedRequest!!.pickupLocation.longitude()})",
                        modifier = Modifier.padding(4.dp)
                    )
                    Text(
                        text = "Điểm đến: (${selectedRequest!!.destination.latitude()}, ${selectedRequest!!.destination.longitude()})",
                        modifier = Modifier.padding(4.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = {
                                requestsCollection.document(selectedRequest!!.id)
                                    .update("status", "completed")
                                    .addOnSuccessListener {
                                        selectedRequest = null
                                        fromPoint = null
                                        toPoint = null
                                        routePoints = emptyList()
                                        Log.d("Firestore", "Hoàn thành chuyến đi")
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e("Firestore", "Hoàn thành chuyến đi thất bại: ${e.message}")
                                        errorMessage = "Không thể hoàn thành chuyến đi: ${e.message}"
                                    }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Hoàn thành chuyến đi")
                        }
                        Button(
                            onClick = {
                                requestsCollection.document(selectedRequest!!.id)
                                    .update("status", "canceled")
                                    .addOnSuccessListener {
                                        selectedRequest = null
                                        fromPoint = null
                                        toPoint = null
                                        routePoints = emptyList()
                                        Log.d("Firestore", "Hủy chuyến đi")
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e("Firestore", "Hủy chuyến đi thất bại: ${e.message}")
                                        errorMessage = "Không thể hủy chuyến đi: ${e.message}"
                                    }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Hủy chuyến")
                        }
                    }
                }
            }

            BottomControlBar(
                navController = navController,
                showConnectButton = true,
                isConnected = isConnected,
                onConnectClick = {
                    isConnected = !isConnected
                    driverRef.update("isConnected", isConnected)
                        .addOnSuccessListener {
                            Log.d("Firestore", "Cập nhật trạng thái isConnected thành công: $isConnected")
                        }
                        .addOnFailureListener { e ->
                            Log.e("Firestore", "Cập nhật trạng thái thất bại: ${e.message}")
                            errorMessage = "Cập nhật trạng thái thất bại: ${e.message}"
                        }
                }
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            requestListener?.remove()
        }
    }
}

// Data class để lưu thông tin yêu cầu
data class RideRequest(
    val id: String,
    val customerId: String,
    val pickupLocation: Point,
    val destination: Point,
    val status: String
)