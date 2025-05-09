package com.example.dacs31.ui.screen.driver

import android.Manifest
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import com.example.dacs31.R
import com.example.dacs31.data.AuthRepository
import com.example.dacs31.map.MapComponent
import com.example.dacs31.ui.screen.componentsUI.BottomControlBar
import com.example.dacs31.ui.screen.componentsUI.SideMenuDrawer
import com.example.dacs31.ui.screen.componentsUI.TopControlBar
import com.example.dacs31.ui.screen.location.SelectAddressDialog
import com.example.dacs31.ui.screen.location.getRoute
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
    var driverId by remember { mutableStateOf<String?>(null) }
    var user by remember { mutableStateOf<com.example.dacs31.data.User?>(null) }
    var userLocation by remember { mutableStateOf<Point?>(null) }
    var showPermissionDeniedDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isConnected by remember { mutableStateOf(false) }
    var incomingRequest by remember { mutableStateOf<RideRequest?>(null) }
    var selectedRequest by remember { mutableStateOf<RideRequest?>(null) }
    var fromPoint by remember { mutableStateOf<Point?>(null) }
    var toPoint by remember { mutableStateOf<Point?>(null) }
    var routePoints by remember { mutableStateOf<List<Point>>(emptyList()) }
    var showSelectAddressDialog by remember { mutableStateOf(false) }
    var isDrawerOpen by remember { mutableStateOf(false) }
    var selectedMode by remember { mutableStateOf("Transport") }
    val mapboxAccessToken = context.getString(R.string.mapbox_access_token)
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    // Firestore
    val db = Firebase.firestore

    Log.d("DriverHomeScreen", "Màn hình DriverHomeScreen được tải")

    // Lấy driverId và kiểm tra vai trò
    LaunchedEffect(Unit) {
        val currentUser = authRepository.getCurrentUser()
        user = currentUser
        if (currentUser == null || currentUser.role != "Driver") {
            Log.e("DriverHomeScreen", "Người dùng không phải tài xế, vai trò: ${currentUser?.role}")
            errorMessage = "Vui lòng đăng nhập với tài khoản tài xế."
            navController.navigate("signin") {
                popUpTo(navController.graph.startDestinationId) { inclusive = true }
            }
            return@LaunchedEffect
        }
        driverId = currentUser.uid
        Log.d("DriverHomeScreen", "Driver ID: $driverId")
    }

    if (driverId == null) {
        Log.d("DriverHomeScreen", "Đang chờ driver [<xaiArtifact/>](https://xaiartifact.com/)Id, hiển thị loading")
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val driverRef = db.collection("drivers").document(driverId!!)
    val requestsCollection = db.collection("requests")

    // Đồng bộ trạng thái isConnected và tạo tài liệu tài xế nếu chưa tồn tại
    LaunchedEffect(driverId) {
        driverRef.get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    driverRef.set(
                        mapOf(
                            "location" to mapOf("latitude" to 0.0, "longitude" to 0.0),
                            "isConnected" to false,
                            "createdAt" to com.google.firebase.Timestamp.now()
                        )
                    )
                        .addOnSuccessListener {
                            Log.d("Firestore", "Tạo tài liệu tài xế mới thành công")
                        }
                        .addOnFailureListener { e ->
                            Log.e("Firestore", "Tạo tài liệu tài xế thất bại: ${e.message}")
                            errorMessage = "Tạo tài liệu tài xế thất bại: ${e.message}"
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Lỗi khi kiểm tra tài liệu: ${e.message}")
                errorMessage = "Lỗi khi kiểm tra tài liệu: ${e.message}"
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
            Log.w("DriverHomeScreen", "Quyền vị trí bị từ chối")
        } else {
            Log.d("DriverHomeScreen", "Quyền vị trí được cấp")
        }
    }

    LaunchedEffect(Unit) {
        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

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
        if (userLocation != null) {
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
        } else {
            Log.w("DriverHomeScreen", "userLocation là null, không thể cập nhật vị trí")
        }
    }

    // Lắng nghe yêu cầu đặt xe từ khách hàng
    var requestListener by remember { mutableStateOf<ListenerRegistration?>(null) }

    LaunchedEffect(driverId, isConnected) {
        requestListener?.remove()
        requestListener = null

        if (!isConnected) {
            incomingRequest = null
            selectedRequest = null
            fromPoint = null
            toPoint = null
            routePoints = emptyList()
            Log.d("DriverHomeScreen", "Tài xế không kết nối, không lắng nghe yêu cầu")
            return@LaunchedEffect
        }

        Log.d("DriverHomeScreen", "Tài xế kết nối, bắt đầu lắng nghe yêu cầu")
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

                    if (selectedRequest == null) {
                        incomingRequest = requestList.firstOrNull()
                        Log.d("DriverHomeScreen", "Yêu cầu mới: $incomingRequest")
                    }
                }
            }
    }

    // Hiển thị dialog khi có yêu cầu đặt xe
    if (incomingRequest != null && selectedRequest == null) {
        val currentRequest = incomingRequest
        AlertDialog(
            onDismissRequest = { incomingRequest = null },
            title = { Text("Yêu cầu đặt xe mới") },
            text = { Text("Bạn có một yêu cầu đặt xe mới từ khách hàng ${currentRequest!!.customerId}. Chấp nhận không?") },
            confirmButton = {
                TextButton(onClick = {
                    requestListener?.remove()
                    requestsCollection.document(currentRequest!!.id).get()
                        .addOnSuccessListener { document ->
                            if (document.exists()) {
                                val status = document.getString("status")
                                val currentDriverId = document.getString("driverId")
                                if (status == "pending" && currentDriverId == null) {
                                    requestsCollection.document(currentRequest.id)
                                        .update(
                                            mapOf(
                                                "status" to "accepted",
                                                "driverId" to driverId
                                            )
                                        )
                                        .addOnSuccessListener {
                                            selectedRequest = currentRequest
                                            incomingRequest = null
                                            Log.d("Firestore", "Chấp nhận yêu cầu thành công: ${selectedRequest!!.id}")
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

                                                        if (selectedRequest == null) {
                                                            incomingRequest = requestList.firstOrNull()
                                                            Log.d("DriverHomeScreen", "Yêu cầu mới: $incomingRequest")
                                                        }
                                                    }
                                                }
                                        }
                                        .addOnFailureListener { e ->
                                            Log.e("Firestore", "Chấp nhận yêu cầu thất bại: ${e.message}")
                                            errorMessage = "Không thể chấp nhận yêu cầu: ${e.message}"
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

                                                        if (selectedRequest == null) {
                                                            incomingRequest = requestList.firstOrNull()
                                                            Log.d("DriverHomeScreen", "Yêu cầu mới: $incomingRequest")
                                                        }
                                                    }
                                                }
                                        }
                                } else {
                                    Log.w("Firestore", "Yêu cầu không còn ở trạng thái pending hoặc đã có tài xế")
                                    errorMessage = "Yêu cầu đã được xử lý bởi tài xế khác."
                                    incomingRequest = null
                                }
                            } else {
                                Log.w("Firestore", "Yêu cầu không tồn tại")
                                errorMessage = "Yêu cầu không tồn tại."
                                incomingRequest = null
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("Firestore", "Lỗi khi kiểm tra yêu cầu: ${e.message}")
                            errorMessage = "Lỗi khi kiểm tra yêu cầu: ${e.message}"
                            incomingRequest = null
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

                                        if (selectedRequest == null) {
                                            incomingRequest = requestList.firstOrNull()
                                            Log.d("DriverHomeScreen", "Yêu cầu mới: $incomingRequest")
                                        }
                                    }
                                }
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
                    val (points, _) = getRoute(
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

    // Xử lý dialog chọn địa chỉ
    if (showSelectAddressDialog) {
        SelectAddressDialog(
            onDismiss = { showSelectAddressDialog = false },
            onConfirm = { from, fromAddr, to, toAddr ->
                Log.d("DriverHomeScreen", "Confirm clicked: from=$from, fromAddr=$fromAddr, to=$to, toAddr=$toAddr")
                if (from != null && to != null) {
                    fromPoint = from
                    toPoint = to
                    coroutineScope.launch {
                        try {
                            val (points, _) = getRoute(from, to, mapboxAccessToken)
                            routePoints = points
                            Log.d("DriverHomeScreen", "Route points received: $routePoints")
                        } catch (e: Exception) {
                            Log.e("MapboxDirections", "Error fetching route: ${e.message}")
                            routePoints = emptyList()
                        }
                    }
                } else {
                    Log.w("DriverHomeScreen", "From or To point is null")
                }
            },
            userLocation = userLocation,
            mapboxAccessToken = mapboxAccessToken
        )
    }

    // Giao diện chính với Drawer
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            SideMenuDrawer(
                user = user,
                navController = navController,
                authRepository = authRepository,
                onDrawerClose = {
                    coroutineScope.launch { drawerState.close() }
                    isDrawerOpen = false
                }
            )
        },
        modifier = Modifier.fillMaxSize(),
        gesturesEnabled = false // Tắt vuốt để mở drawer, chỉ mở bằng nút
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            var mapViewInstance by remember { mutableStateOf<MapView?>(null) }

            // Đặt MapComponent ở lớp thấp nhất để không che khuất giao diện
            MapComponent(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(0f), // Lớp thấp nhất để không che khuất giao diện
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
                    Log.d("DriverHomeScreen", "MapView sẵn sàng")
                }
            )

            LaunchedEffect(userLocation) {
                if (userLocation != null) {
                    mapViewInstance?.getMapboxMap()?.setCamera(
                        CameraOptions.Builder()
                            .center(userLocation)
                            .zoom(15.0)
                            .build()
                    )
                    Log.d("DriverHomeScreen", "Camera moved to user location: $userLocation")
                } else {
                    Log.w("DriverHomeScreen", "userLocation là null, không thể di chuyển camera")
                }
            }

            // TopControlBar
            TopControlBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .align(Alignment.TopCenter)
                    .zIndex(2f), // Hiển thị lên trên bản đồ
                onMenuClick = {
                    coroutineScope.launch {
                        if (drawerState.isOpen) {
                            drawerState.close()
                            isDrawerOpen = false
                        } else {
                            drawerState.open()
                            isDrawerOpen = true
                        }
                    }
                }
            )

            // Nút My Location
            FloatingActionButton(
                onClick = {
                    userLocation?.let { point ->
                        mapViewInstance?.getMapboxMap()?.setCamera(
                            CameraOptions.Builder()
                                .center(point)
                                .zoom(15.0)
                                .build()
                        )
                        Log.d("DriverHomeScreen", "Moved camera to user location: $point")
                    } ?: run {
                        Log.w("DriverHomeScreen", "User location is null")
                        showPermissionDeniedDialog = true
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 16.dp, end = 16.dp)
                    .zIndex(2f), // Hiển thị lên trên bản đồ
                shape = CircleShape,
                containerColor = Color.White,
                contentColor = Color.Black
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "My Location"
                )
            }

            // Thêm thông báo trạng thái khi không có yêu cầu
            if (!isDrawerOpen) {
                if (!isConnected && selectedRequest == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .padding(16.dp)
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                            .align(Alignment.Center)
                            .zIndex(2f) // Hiển thị lên trên bản đồ
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { /* Không xử lý sự kiện chạm, để sự kiện xuyên qua bản đồ */ }
                            )
                    ) {
                        Text(
                            text = "Bạn hiện đang offline. Kết nối để nhận yêu cầu đặt xe.",
                            style = TextStyle(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else if (isConnected && incomingRequest == null && selectedRequest == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .padding(16.dp)
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                            .align(Alignment.Center)
                            .zIndex(2f) // Hiển thị lên trên bản đồ
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { /* Không xử lý sự kiện chạm, để sự kiện xuyên qua bản đồ */ }
                            )
                    ) {
                        Text(
                            text = "Đang chờ yêu cầu đặt xe...",
                            style = TextStyle(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            // Khu vực Bottom (bao gồm các nút và BottomControlBar)
            if (!isDrawerOpen) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .align(Alignment.BottomCenter)
                        .zIndex(2f), // Hiển thị lên trên bản đồ
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (selectedRequest != null) {
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
                            val driverDocRef = db.collection("drivers").document(driverId!!)
                            driverDocRef.get()
                                .addOnSuccessListener { document ->
                                    if (document.exists()) {
                                        driverDocRef.update("isConnected", isConnected)
                                            .addOnSuccessListener {
                                                Log.d("Firestore", "Cập nhật trạng thái isConnected thành công: $isConnected")
                                            }
                                            .addOnFailureListener { e ->
                                                Log.e("Firestore", "Cập nhật trạng thái thất bại: ${e.message}")
                                                errorMessage = "Cập nhật trạng thái thất bại: ${e.message}"
                                            }
                                    } else {
                                        driverDocRef.set(
                                            mapOf(
                                                "location" to mapOf("latitude" to 0.0, "longitude" to 0.0),
                                                "isConnected" to isConnected,
                                                "createdAt" to com.google.firebase.Timestamp.now()
                                            )
                                        )
                                            .addOnSuccessListener {
                                                Log.d("Firestore", "Tạo tài liệu tài xế thành công")
                                            }
                                            .addOnFailureListener { e ->
                                                Log.e("Firestore", "Tạo tài liệu tài xế thất bại: ${e.message}")
                                                errorMessage = "Tạo tài liệu tài xế thất bại: ${e.message}"
                                            }
                                    }
                                }
                                .addOnFailureListener { e ->
                                    Log.e("Firestore", "Lỗi khi kiểm tra tài liệu: ${e.message}")
                                    errorMessage = "Lỗi khi kiểm tra tài liệu: ${e.message}"
                                }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                    )
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            requestListener?.remove()
        }
    }
}

data class RideRequest(
    val id: String,
    val customerId: String,
    val pickupLocation: Point,
    val destination: Point,
    val status: String
)