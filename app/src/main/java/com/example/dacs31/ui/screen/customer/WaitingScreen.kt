package com.example.dacs31.ui.screen

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dacs31.map.MapComponent
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView

@Composable
fun WaitingScreen(
    userLocation: Point?,
    mapboxAccessToken: String,
    currentRequestId: String,
    selectedTransport: String?,
    onRequestAccepted: () -> Unit,
    onRequestCanceled: () -> Unit,
    modifier: Modifier = Modifier
) {
    var mapViewInstance by remember { mutableStateOf<MapView?>(null) }
    var showDriverAcceptedDialog by remember { mutableStateOf(false) }
    var showRequestCanceledDialog by remember { mutableStateOf(false) }
    var nearbyDrivers by remember { mutableStateOf<List<Point>>(emptyList()) }

    val db = Firebase.firestore
    val requestsCollection = db.collection("requests")
    val driversCollection = db.collection("drivers")

    // Listener để theo dõi trạng thái yêu cầu
    var requestListener by remember { mutableStateOf<ListenerRegistration?>(null) }

    // Listener để lấy vị trí các tài xế lân cận
    var driversListener by remember { mutableStateOf<ListenerRegistration?>(null) }

    LaunchedEffect(Unit) {
        driversListener = driversCollection
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("Firestore", "Lỗi khi lấy danh sách tài xế: ${error.message}")
                    return@addSnapshotListener
                }

                val driverLocations = mutableListOf<Point>()
                snapshot?.documents?.forEach { document ->
                    val locationData = document.get("location") as? Map<String, Double>
                    locationData?.let {
                        val latitude = it["latitude"] ?: return@let
                        val longitude = it["longitude"] ?: return@let
                        driverLocations.add(Point.fromLngLat(longitude, latitude))
                    }
                }
                nearbyDrivers = driverLocations
                Log.d("WaitingScreen", "Cập nhật vị trí tài xế: $nearbyDrivers")
            }
    }

    LaunchedEffect(currentRequestId) {
        requestListener?.remove()
        requestListener = null

        requestListener = requestsCollection.document(currentRequestId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("Firestore", "Lỗi khi lắng nghe trạng thái yêu cầu: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val status = snapshot.getString("status")
                    when (status) {
                        "accepted" -> {
                            showDriverAcceptedDialog = true
                        }
                        "canceled" -> {
                            showRequestCanceledDialog = true
                            requestsCollection.document(currentRequestId).delete()
                        }
                    }
                }
            }
    }

    // Hiển thị dialog khi tài xế chấp nhận
    if (showDriverAcceptedDialog) {
        AlertDialog(
            onDismissRequest = {
                showDriverAcceptedDialog = false
                onRequestAccepted()
            },
            title = { Text("Yêu cầu được chấp nhận") },
            text = { Text("Tài xế đã chấp nhận yêu cầu của bạn. Hãy chuẩn bị cho chuyến đi!") },
            confirmButton = {
                TextButton(onClick = {
                    showDriverAcceptedDialog = false
                    onRequestAccepted()
                }) {
                    Text("OK")
                }
            }
        )
    }

    // Hiển thị dialog khi yêu cầu bị hủy
    if (showRequestCanceledDialog) {
        AlertDialog(
            onDismissRequest = {
                showRequestCanceledDialog = false
                onRequestCanceled()
            },
            title = { Text("Yêu cầu bị hủy") },
            text = { Text("Yêu cầu của bạn đã bị hủy. Vui lòng đặt lại chuyến đi.") },
            confirmButton = {
                TextButton(onClick = {
                    showRequestCanceledDialog = false
                    onRequestCanceled()
                }) {
                    Text("OK")
                }
            }
        )
    }

    // Hàm xử lý khi nhấn nút "Hủy"
    fun handleCancelRequest() {
        requestsCollection.document(currentRequestId).delete()
            .addOnSuccessListener {
                Log.d("WaitingScreen", "Yêu cầu đã được hủy thành công: $currentRequestId")
                onRequestCanceled()
            }
            .addOnFailureListener { e ->
                Log.e("WaitingScreen", "Lỗi khi hủy yêu cầu: ${e.message}")
            }
    }

    // Update camera when user location changes
    LaunchedEffect(userLocation, mapViewInstance) {
        if (mapViewInstance != null && userLocation != null) {
            mapViewInstance?.getMapboxMap()?.setCamera(
                CameraOptions.Builder()
                    .center(userLocation)
                    .zoom(15.0)
                    .build()
            )
        }
    }
    var showCancelConfirmationDialog by remember { mutableStateOf(false) }

    if (showCancelConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showCancelConfirmationDialog = false },
            title = { Text("Xác nhận hủy") },
            text = { Text("Bạn có chắc chắn muốn hủy yêu cầu không?") },
            confirmButton = {
                TextButton(onClick = {
                    showCancelConfirmationDialog = false
                    handleCancelRequest()
                }) {
                    Text("Có")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelConfirmationDialog = false }) {
                    Text("Không")
                }
            }
        )
    }
    Box(modifier = modifier.fillMaxSize()) {
        // Display the map with user location and nearby drivers
        MapComponent(
            modifier = Modifier.fillMaxSize(),
            routePoints = emptyList(),
            fromPoint = null,
            toPoint = null,
            driverLocation = null,
            userLocation = userLocation,
            nearbyDrivers = nearbyDrivers,
            onUserLocationUpdated = { /* Not needed for waiting screen */ },
            onMapReady = { _, _ -> },
            onMapViewReady = { mapView ->
                mapViewInstance = mapView
            }
        )

        // "Vẫn đang tìm tài xế..." overlay
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Color(0xFFFFD700)) // Yellow color
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DirectionsBike,
                    contentDescription = "Transport Icon",
                    tint = Color.Black,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Vẫn đang tìm tài xế...",
                    color = Color.Black,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Cần thêm chút thời gian để tìm, bạn thông cảm chút nhé.",
                color = Color.Black,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { showCancelConfirmationDialog = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF44336)
                )
            ) {
                Text(
                    text = "Hủy yêu cầu",
                    color = Color.White,
                    fontSize = 14.sp
                )
            }
        }
    }

    // Dọn dẹp listener khi Composable bị hủy
    DisposableEffect(Unit) {
        onDispose {
            requestListener?.remove()
            driversListener?.remove()
        }
    }


}
