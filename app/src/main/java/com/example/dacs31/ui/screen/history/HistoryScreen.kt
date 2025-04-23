package com.example.dacs31.ui.screen.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.dacs31.data.AuthRepository
import com.google.firebase.Timestamp
import android.util.Log
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import com.example.dacs31.data.Trip
import com.example.dacs31.data.TripRepository
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryScreen(
    navController: NavController,
    authRepository: AuthRepository
) {
    var userId by remember { mutableStateOf<String?>(null) }
    var role by remember { mutableStateOf<String?>(null) }
    var trips by remember { mutableStateOf<List<Trip>>(emptyList()) }
    var selectedTab by remember { mutableStateOf("Upcoming") }
    val tripRepository = remember { TripRepository() }

    // Lấy userId và role
    LaunchedEffect(Unit) {
        val user = authRepository.getCurrentUser()
        Log.d("HistoryScreen", "Current user: $user")
        if (user == null) {
            navController.navigate("signin") {
                popUpTo(navController.graph.startDestinationId) { inclusive = true }
            }
            return@LaunchedEffect
        }
        userId = user.uid
        role = user.role
        Log.d("HistoryScreen", "User ID: $userId, Role: $role")
    }

    // Lấy dữ liệu chuyến đi
    LaunchedEffect(userId, role, selectedTab) {
        if (userId != null && role != null) {
            val allTrips = tripRepository.getTripsByUser(userId!!, role!!)
            Log.d("HistoryScreen", "All trips: $allTrips")
            trips = when (selectedTab) {
                "Upcoming" -> allTrips.filter { it.status in listOf("pending", "accepted") }
                "Completed" -> allTrips.filter { it.status == "completed" }
                "Canceled" -> allTrips.filter { it.status == "canceled" }
                else -> emptyList()
            }
            Log.d("HistoryScreen", "Filtered trips for $selectedTab: $trips")
        }
    }

    // Đợi userId và role
    if (userId == null || role == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 16.dp)
    ) {
        // Thanh điều hướng với nút Back
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Back",
                modifier = Modifier.clickable { navController.popBackStack() },
                style = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "History",
                style = TextStyle(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = Modifier.weight(1f))
        }

        // Tab Row
        TabRow(
            selectedTabIndex = when (selectedTab) {
                "Upcoming" -> 0
                "Completed" -> 1
                "Canceled" -> 2
                else -> 0
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            containerColor = Color.White,
            contentColor = Color.Black,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[when (selectedTab) {
                        "Upcoming" -> 0
                        "Completed" -> 1
                        "Canceled" -> 2
                        else -> 0
                    }]),
                    color = Color(0xFFEDAE10) // Màu vàng giống trong hình
                )
            }
        ) {
            Tab(
                selected = selectedTab == "Upcoming",
                onClick = { selectedTab = "Upcoming" },
                text = { Text("Upcoming") }
            )
            Tab(
                selected = selectedTab == "Completed",
                onClick = { selectedTab = "Completed" },
                text = { Text("Completed") }
            )
            Tab(
                selected = selectedTab == "Canceled",
                onClick = { selectedTab = "Canceled" },
                text = { Text("Canceled") }
            )
        }

        // Danh sách chuyến đi
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (trips.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Không có dữ liệu cho tab ${selectedTab.lowercase()}",
                            style = TextStyle(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.Gray
                            )
                        )
                    }
                }
            } else {
                items(trips) { trip ->
                    TripItem(trip)
                }
            }
        }
    }
}

@Composable
fun TripItem(trip: Trip) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFFBE7) // Màu nền giống trong hình
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Khách hàng: ${trip.customerName}",
                style = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Điểm đón: (${trip.pickupLocation?.latitude() ?: "N/A"}, ${trip.pickupLocation?.longitude() ?: "N/A"})",
                style = TextStyle(
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Điểm đến: (${trip.destination?.latitude() ?: "N/A"}, ${trip.destination?.longitude() ?: "N/A"})",
                style = TextStyle(
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formatTimestamp(trip.time),
                style = TextStyle(
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            )
        }
    }
}

fun formatTimestamp(timestamp: Timestamp?): String {
    if (timestamp == null) return "Unknown time"
    val date = timestamp.toDate()
    val today = Calendar.getInstance().apply { time = Date() }
    val tripDate = Calendar.getInstance().apply { time = date }
    val isToday = today.get(Calendar.YEAR) == tripDate.get(Calendar.YEAR) &&
            today.get(Calendar.DAY_OF_YEAR) == tripDate.get(Calendar.DAY_OF_YEAR)

    val formatter = SimpleDateFormat("h:mm a", Locale.getDefault())
    val timeString = formatter.format(date)
    return if (isToday) {
        "Today at $timeString"
    } else {
        val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
        val isTomorrow = tomorrow.get(Calendar.YEAR) == tripDate.get(Calendar.YEAR) &&
                tomorrow.get(Calendar.DAY_OF_YEAR) == tripDate.get(Calendar.DAY_OF_YEAR)
        if (isTomorrow) {
            "Tomorrow at $timeString"
        } else {
            SimpleDateFormat("MMM d 'at' h:mm a", Locale.getDefault()).format(date)
        }
    }
}