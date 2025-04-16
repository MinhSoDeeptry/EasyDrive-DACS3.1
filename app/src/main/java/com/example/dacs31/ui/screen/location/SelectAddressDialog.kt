package com.example.dacs31.ui.screen.location

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mapbox.geojson.Point
import kotlinx.coroutines.launch

@Composable
fun SelectAddressDialog(
    onDismiss: () -> Unit,
    onConfirm: (Point?, String, Point?, String) -> Unit,
    userLocation: Point?,
    mapboxAccessToken: String,
    modifier: Modifier = Modifier
) {
    // Dữ liệu mẫu cho danh sách "Recent Places"
    val recentPlaces = listOf(
        RecentPlace("Office", "2972 Westheimer Rd. Santa Ana, Illinois 85486", "2.7km"),
        RecentPlace("Coffee shop", "1901 Thornridge Cir. Shiloh, Hawaii 81063", "1.1km"),
        RecentPlace("Shopping center", "4140 Parker Rd. Allentown, New Mexico 31134", "4.9km"),
        RecentPlace("Shopping mall", "4140 Parker Rd. Allentown, New Mexico 31134", "4.0km")
    ).map { it.toPlace() }

    // Trạng thái cho ô nhập "From" và "To"
    var fromText by remember { mutableStateOf("Current location") }
    var toText by remember { mutableStateOf("") }

    // Trạng thái để lưu tọa độ và địa chỉ của địa điểm được chọn
    var fromPoint by remember { mutableStateOf(userLocation) }
    var toPoint by remember { mutableStateOf<Point?>(null) }
    var fromAddress by remember { mutableStateOf("Current location") }
    var toAddress by remember { mutableStateOf("") }

    // Trạng thái cho danh sách đề xuất
    var toSuggestions by remember { mutableStateOf<List<Place>>(emptyList()) }
    var showToSuggestions by remember { mutableStateOf(false) }

    // Coroutine scope để gọi API
    val coroutineScope = rememberCoroutineScope()

    // Hàm tìm kiếm địa điểm cho ô "To"
    fun searchTo(query: String) {
        if (query.isNotBlank()) {
            coroutineScope.launch {
                try {
                    toSuggestions = searchPlaces(query, mapboxAccessToken)
                    showToSuggestions = toSuggestions.isNotEmpty()
                } catch (e: Exception) {
                    showToSuggestions = false
                }
            }
        } else {
            showToSuggestions = false
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .background(Color.White, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Tiêu đề và nút đóng
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Select address",
                    style = TextStyle(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                )
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { onDismiss() },
                    tint = Color.Black
                )
            }

            // Nội dung cuộn được
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                // Ô nhập "From" (Current location, không cho phép chỉnh sửa)
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Location",
                            tint = Color.Red,
                            modifier = Modifier.size(24.dp)
                        )
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 8.dp)
                        ) {
                            Text(
                                text = "Current location",
                                style = TextStyle(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            )
                            Text(
                                text = fromText,
                                style = TextStyle(
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            )
                        }
                    }
                }

                // Ô nhập "To"
                item {
                    OutlinedTextField(
                        value = toText,
                        onValueChange = {
                            toText = it
                            searchTo(it)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        placeholder = { Text("To", color = Color.Gray) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Location",
                                tint = Color.Yellow,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                // Danh sách đề xuất cho "To"
                if (showToSuggestions && toSuggestions.isNotEmpty()) {
                    item {
                        Text(
                            text = "Suggestions for To",
                            style = TextStyle(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        )
                    }
                    items(toSuggestions) { place ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    toText = place.address
                                    toPoint = place.coordinates
                                    toAddress = place.address
                                    showToSuggestions = false
                                    Log.d("SelectAddressDialog", "Selected To: ${place.address}, Coordinates: $toPoint")
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Location",
                                tint = Color.Yellow,
                                modifier = Modifier.size(24.dp)
                            )
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 8.dp)
                            ) {
                                Text(
                                    text = "Destination",
                                    style = TextStyle(
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                )
                                Text(
                                    text = place.address,
                                    style = TextStyle(
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                )
                            }
                        }
                    }
                }

                // Tiêu đề "Recent places"
                item {
                    Text(
                        text = "Recent places",
                        style = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    )
                }

                // Danh sách "Recent Places"
                items(recentPlaces) { place ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                toText = place.address
                                toAddress = place.address
                                // Gọi Mapbox Geocoding API để lấy tọa độ từ địa chỉ
                                coroutineScope.launch {
                                    try {
                                        val places = searchPlaces(place.address, mapboxAccessToken)
                                        if (places.isNotEmpty()) {
                                            toPoint = places[0].coordinates
                                            toAddress = places[0].address
                                            Log.d("SelectAddressDialog", "Selected Recent Place: ${place.address}, Coordinates: $toPoint")
                                        }
                                    } catch (e: Exception) {
                                        Log.e("SelectAddressDialog", "Error fetching coordinates for recent place: ${e.message}")
                                    }
                                }
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Location",
                            tint = Color.Yellow,
                            modifier = Modifier.size(24.dp)
                        )
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 8.dp)
                        ) {
                            Text(
                                text = place.name,
                                style = TextStyle(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            )
                            Text(
                                text = place.address,
                                style = TextStyle(
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            )
                        }
                        place.distance?.let {
                            Text(
                                text = it,
                                style = TextStyle(
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                ),
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            }

            // Nút Confirm
            Button(
                onClick = {
                    onConfirm(fromPoint, fromAddress, toPoint, toAddress)
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(top = 16.dp)
                    .clip(RoundedCornerShape(8.dp)),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFEDAE10)
                ),
                contentPadding = PaddingValues(0.dp),
                enabled = toPoint != null && toAddress.isNotBlank() // Chỉ cho phép nhấn khi đã chọn điểm đến
            ) {
                Text(
                    text = "Confirm",
                    color = Color.Black,
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