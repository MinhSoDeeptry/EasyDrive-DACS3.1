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
import androidx.navigation.NavController
import com.example.dacs31.R
import com.example.dacs31.data.AuthRepository
import com.example.dacs31.map.MapComponent
import com.example.dacs31.ui.screen.WaitingScreen
import com.example.dacs31.ui.screen.componentsUI.BottomControlBar
import com.example.dacs31.ui.screen.componentsUI.TopControlBar
import com.example.dacs31.ui.screen.location.RouteInfoDialog
import com.example.dacs31.ui.screen.location.SelectAddressDialog
import com.example.dacs31.ui.screen.location.getRoute
import com.example.dacs31.ui.screen.payment.PaymentScreen
import com.example.dacs31.ui.screen.transport.SelectTransportScreen
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.turf.TurfMeasurement
import kotlinx.coroutines.launch

@Composable
fun CustomerHomeScreen(
    navController: NavController,
    authRepository: AuthRepository
) {
    val context = LocalContext.current
    var customerId by remember { mutableStateOf<String?>(null) }
    var userLocation by remember { mutableStateOf<Point?>(null) }
    var showPermissionDeniedDialog by remember { mutableStateOf(false) }
    var showSelectAddressDialog by remember { mutableStateOf(false) }
    var showRouteInfoDialog by remember { mutableStateOf(false) }
    var showSelectTransportDialog by remember { mutableStateOf(false) }
    var showPaymentScreen by remember { mutableStateOf(false) }
    var showDriverAcceptedDialog by remember { mutableStateOf(false) }
    var showRequestCanceledDialog by remember { mutableStateOf(false) }
    var showTripCompletedDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSendingRequest by remember { mutableStateOf(false) }

    var routePoints by remember { mutableStateOf<List<Point>>(emptyList()) }
    var fromPoint by remember { mutableStateOf<Point?>(null) }
    var toPoint by remember { mutableStateOf<Point?>(null) }
    var routeDistance by remember { mutableStateOf(0.0) }
    var fromAddress by remember { mutableStateOf("Current location") }
    var toAddress by remember { mutableStateOf("") }
    var selectedTransport by remember { mutableStateOf<String?>(null) }
    var currentRequestId by remember { mutableStateOf<String?>(null) }
    var driverId by remember { mutableStateOf<String?>(null) }
    var driverLocation by remember { mutableStateOf<Point?>(null) }
    var mapViewInstance by remember { mutableStateOf<MapView?>(null) }
    var isPending by remember { mutableStateOf(false) }
    var isDrawerOpen by remember { mutableStateOf(false) } // Thêm trạng thái drawer

    var selectedMode by remember { mutableStateOf("Transport") }

    val mapboxAccessToken = context.getString(R.string.mapbox_access_token)
    val coroutineScope = rememberCoroutineScope()

    val db = Firebase.firestore
    val requestsCollection = db.collection("requests")

    // Lấy customerId và kiểm tra vai trò
    LaunchedEffect(Unit) {
        val user = authRepository.getCurrentUser()
        if (user == null) {
            Log.d("CustomerHomeScreen", "Người dùng chưa đăng nhập, điều hướng đến màn hình đăng nhập")
            errorMessage = "Vui lòng đăng nhập để tiếp tục."
            navController.navigate("signin") {
                popUpTo(navController.graph.startDestinationId) { inclusive = true }
            }
            return@LaunchedEffect
        }

        if (user.role != "Customer") {
            Log.d("CustomerHomeScreen", "Người dùng không phải khách hàng, vai trò: ${user.role}")
            errorMessage = "Vui lòng đăng nhập với tài khoản khách hàng."
            if (user.role == "driver") {
                navController.navigate("driver_home") {
                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                }
            } else {
                navController.navigate("signin") {
                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                }
            }
            return@LaunchedEffect
        }

        customerId = user.uid
        Log.d("CustomerHomeScreen", "Customer ID: $customerId")
    }

    if (customerId == null) {
        Log.d("CustomerHomeScreen", "Đang chờ customerId, hiển thị loading")
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    LaunchedEffect(Unit) {
        val testDocRef = db.collection("test").document("test_message")
        testDocRef.set(mapOf("message" to "Hello, Firestore!"))
            .addOnSuccessListener {
                Log.d("FirestoreTest", "Ghi dữ liệu thành công!")
            }
            .addOnFailureListener { e ->
                Log.e("FirestoreTest", "Ghi dữ liệu thất bại: ${e.message}")
            }
    }

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

    if (isPending && currentRequestId != null) {
        WaitingScreen(
            userLocation = userLocation,
            mapboxAccessToken = mapboxAccessToken,
            currentRequestId = currentRequestId!!,
            selectedTransport = selectedTransport,
            onRequestAccepted = {
                isPending = false
                showDriverAcceptedDialog = true
            },
            onRequestCanceled = {
                isPending = false
                showRequestCanceledDialog = true
                currentRequestId = null
                driverId = null
                driverLocation = null
            }
        )
        return
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

    if (showDriverAcceptedDialog) {
        AlertDialog(
            onDismissRequest = { showDriverAcceptedDialog = false },
            title = { Text("Yêu cầu được chấp nhận") },
            text = { Text("Tài xế đã chấp nhận yêu cầu của bạn. Hãy chuẩn bị cho chuyến đi!") },
            confirmButton = {
                TextButton(onClick = { showDriverAcceptedDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    if (showRequestCanceledDialog) {
        AlertDialog(
            onDismissRequest = { showRequestCanceledDialog = false },
            title = { Text("Yêu cầu bị hủy") },
            text = { Text("Yêu cầu của bạn đã bị hủy. Vui lòng đặt lại chuyến đi.") },
            confirmButton = {
                TextButton(onClick = { showRequestCanceledDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    if (showTripCompletedDialog) {
        AlertDialog(
            onDismissRequest = { showTripCompletedDialog = false },
            title = { Text("Chuyến đi hoàn thành") },
            text = { Text("Chuyến đi của bạn đã hoàn thành thành công!") },
            confirmButton = {
                TextButton(onClick = { showTripCompletedDialog = false }) {
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

    if (showSelectAddressDialog) {
        SelectAddressDialog(
            onDismiss = { showSelectAddressDialog = false },
            onConfirm = { from, fromAddr, to, toAddr ->
                Log.d("CustomerHomeScreen", "Confirm clicked: from=$from, fromAddr=$fromAddr, to=$to, toAddr=$toAddr")
                if (from != null && to != null) {
                    fromPoint = from
                    toPoint = to
                    fromAddress = fromAddr
                    toAddress = toAddr
                    coroutineScope.launch {
                        try {
                            val (points, distance) = getRoute(from, to, mapboxAccessToken)
                            routePoints = points
                            routeDistance = distance
                            Log.d("CustomerHomeScreen", "Route points received: $routePoints, Distance: $routeDistance m")
                            showRouteInfoDialog = true
                        } catch (e: Exception) {
                            Log.e("MapboxDirections", "Error fetching route: ${e.message}")
                            routePoints = emptyList()
                            routeDistance = 0.0
                        }
                    }
                } else {
                    Log.w("CustomerHomeScreen", "From or To point is null")
                }
            },
            userLocation = userLocation,
            mapboxAccessToken = mapboxAccessToken
        )
    }

    if (showSelectTransportDialog) {
        Log.d("CustomerHomeScreen", "Showing SelectTransportScreen")
        SelectTransportScreen(
            onDismiss = {
                showSelectTransportDialog = false
                Log.d("CustomerHomeScreen", "SelectTransportScreen dismissed")
            },
            onTransportSelected = { transport ->
                selectedTransport = transport
                showSelectTransportDialog = false
                showPaymentScreen = true
                Log.d("CustomerHomeScreen", "Transport selected: $transport")
            }
        )
    }

    if (showPaymentScreen) {
        if (isSendingRequest) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        PaymentScreen(
            selectedTransport = selectedTransport ?: "Car",
            routeDistance = routeDistance,
            onDismiss = {
                showPaymentScreen = false
                Log.d("CustomerHomeScreen", "PaymentScreen dismissed")
            },
            onConfirmRide = {
                if (fromPoint == null || toPoint == null) {
                    errorMessage = "Điểm đi hoặc điểm đến không hợp lệ. Vui lòng thử lại."
                    showPaymentScreen = false
                    return@PaymentScreen
                }

                isSendingRequest = true

                val requestData = mapOf(
                    "customerId" to customerId,
                    "pickupLocation" to mapOf(
                        "latitude" to fromPoint!!.latitude(),
                        "longitude" to fromPoint!!.longitude()
                    ),
                    "destination" to mapOf(
                        "latitude" to toPoint!!.latitude(),
                        "longitude" to toPoint!!.longitude()
                    ),
                    "status" to "pending",
                    "driverId" to null,
                    "createdAt" to Timestamp.now()
                )
                Log.d("Firestore", "Chuẩn bị lưu yêu cầu: $requestData")

                requestsCollection.add(requestData)
                    .addOnSuccessListener { documentReference ->
                        val requestId = documentReference.id
                        Log.d("Firestore", "Gửi yêu cầu đặt xe thành công, requestId: $requestId")
                        currentRequestId = requestId
                        isPending = true
                        isSendingRequest = false
                    }
                    .addOnFailureListener { e ->
                        Log.e("Firestore", "Gửi yêu cầu thất bại: ${e.message}")
                        errorMessage = "Không thể gửi yêu cầu: ${e.message}. Vui lòng thử lại."
                        isSendingRequest = false
                    }

                showPaymentScreen = false
                Log.d("CustomerHomeScreen", "Ride confirmed with transport: $selectedTransport")
            }
        )
    }

    var requestListener by remember { mutableStateOf<ListenerRegistration?>(null) }
    var driverListener by remember { mutableStateOf<ListenerRegistration?>(null) }

    LaunchedEffect(currentRequestId) {
        requestListener?.remove()
        requestListener = null

        currentRequestId?.let { requestId ->
            requestListener = requestsCollection.document(requestId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("Firestore", "Lỗi khi lắng nghe trạng thái yêu cầu: ${error.message}")
                        return@addSnapshotListener
                    }

                    if (snapshot != null && snapshot.exists()) {
                        val status = snapshot.getString("status")
                        val newDriverId = snapshot.getString("driverId")
                        when (status) {
                            "accepted" -> {
                                if (!isPending) {
                                    showDriverAcceptedDialog = true
                                }
                                driverId = newDriverId
                            }
                            "canceled" -> {
                                if (!isPending) {
                                    showRequestCanceledDialog = true
                                }
                                requestsCollection.document(requestId).delete()
                                currentRequestId = null
                                driverId = null
                                driverLocation = null
                            }
                            "completed" -> {
                                requestsCollection.document(requestId).get()
                                    .addOnSuccessListener { document ->
                                        if (document.exists()) {
                                            requestsCollection.document(requestId).delete()
                                        }
                                        currentRequestId = null
                                        driverId = null
                                        driverLocation = null
                                        showTripCompletedDialog = true
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e("Firestore", "Lỗi khi kiểm tra tài liệu: ${e.message}")
                                        currentRequestId = null
                                        driverId = null
                                        driverLocation = null
                                    }
                            }
                        }
                    }
                }
        }
    }

    LaunchedEffect(driverId) {
        driverListener?.remove()
        driverListener = null

        driverId?.let { id ->
            driverListener = db.collection("drivers").document(id)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("Firestore", "Lỗi khi lắng nghe vị trí tài xế: ${error.message}")
                        return@addSnapshotListener
                    }

                    if (snapshot != null && snapshot.exists()) {
                        val locationData = snapshot.get("location") as? Map<String, Double>
                        locationData?.let {
                            val latitude = it["latitude"] ?: return@let
                            val longitude = it["longitude"] ?: return@let
                            driverLocation = Point.fromLngLat(longitude, latitude)
                            Log.d("CustomerHomeScreen", "Vị trí tài xế cập nhật: $driverLocation")
                        }
                    }
                }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            requestListener?.remove()
            driverListener?.remove()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        MapComponent(
            modifier = Modifier.fillMaxSize(),
            routePoints = routePoints,
            fromPoint = fromPoint,
            toPoint = toPoint,
            driverLocation = driverLocation,
            userLocation = userLocation,
            onUserLocationUpdated = { point ->
                userLocation = point
                Log.d("CustomerHomeScreen", "User location updated: $userLocation")
            },
            onMapReady = { mapView, pointAnnotationManager ->
                Log.d("CustomerHomeScreen", "Map is ready")
            },
            onMapViewReady = { mapView ->
                mapViewInstance = mapView
            }
        )

        LaunchedEffect(userLocation) {
            userLocation?.let { point ->
                mapViewInstance?.getMapboxMap()?.setCamera(
                    CameraOptions.Builder()
                        .center(point)
                        .zoom(15.0)
                        .build()
                )
                Log.d("CustomerHomeScreen", "Camera moved to user location: $point")
            }
        }

        TopControlBar(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .align(Alignment.TopCenter),
            navController = navController,
            authRepository = authRepository,
            onDrawerStateChange = { isOpen ->
                isDrawerOpen = isOpen
            }
        )

        FloatingActionButton(
            onClick = {
                userLocation?.let { point ->
                    driverLocation?.let { driver ->
                        val points = listOf(point, driver)
                        val bounds = TurfMeasurement.bbox(LineString.fromLngLats(points))
                        mapViewInstance?.getMapboxMap()?.setCamera(
                            CameraOptions.Builder()
                                .center(Point.fromLngLat((bounds[0] + bounds[2]) / 2, (bounds[1] + bounds[3]) / 2))
                                .zoom(12.0)
                                .build()
                        )
                        Log.d("CustomerHomeScreen", "Moved camera to show user and driver")
                    } ?: run {
                        mapViewInstance?.getMapboxMap()?.setCamera(
                            CameraOptions.Builder()
                                .center(point)
                                .zoom(15.0)
                                .build()
                        )
                        Log.d("CustomerHomeScreen", "Moved camera to user location: $point")
                    }
                } ?: run {
                    Log.w("CustomerHomeScreen", "User location is null")
                    showPermissionDeniedDialog = true
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = if (showRouteInfoDialog) 150.dp else 16.dp, end = 16.dp),
            shape = CircleShape,
            containerColor = Color.White,
            contentColor = Color.Black
        ) {
            Icon(
                imageVector = Icons.Default.MyLocation,
                contentDescription = "My Location"
            )
        }

        if (!isDrawerOpen) { // Ẩn BottomControlBar khi drawer mở
            BottomControlBar(
                navController = navController,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
            )
        }

        if (showRouteInfoDialog) {
            RouteInfoDialog(
                fromAddress = fromAddress,
                toAddress = toAddress,
                distance = routeDistance,
                onDismiss = {
                    showRouteInfoDialog = false
                    showSelectTransportDialog = true
                    Log.d("CustomerHomeScreen", "RouteInfoDialog dismissed, opening SelectTransportScreen")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .align(Alignment.BottomCenter)
            )
        }

        if (!showRouteInfoDialog && !isDrawerOpen) { // Ẩn Column khi drawer mở hoặc RouteInfoDialog hiển thị
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 100.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { /* TODO: Xử lý khi nhấn Rental */ },
                    modifier = Modifier
                        .width(172.dp)
                        .height(54.dp)
                        .padding(start = 15.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEDAE10)
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

                Box(
                    modifier = Modifier
                        .width(336.dp)
                        .height(48.dp)
                        .padding(horizontal = 28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFFFBE7))
                        .border(
                            BorderStroke(2.dp, Color(0xFFF3BD06)),
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { showSelectAddressDialog = true }
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

                Row(
                    modifier = Modifier
                        .width(336.dp)
                        .height(48.dp)
                        .padding(horizontal = 28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFFFBE7))
                        .border(
                            BorderStroke(2.dp, Color(0xFFF3BD06)),
                            RoundedCornerShape(8.dp)
                        )
                ) {
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
}