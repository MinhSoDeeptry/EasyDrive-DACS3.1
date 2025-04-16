package com.example.dacs31.ui.screen.location

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
    onConfirm: (Point?, Point?) -> Unit, // Callback để trả về tọa độ From và To
    userLocation: Point?, // Vị trí hiện tại từ Mapbox
    mapboxAccessToken: String, // Mapbox Access Token
    modifier: Modifier = Modifier
) {
    // Dữ liệu mẫu cho danh sách "Recent Places" (có thể bỏ nếu chỉ dùng API)
    val recentPlaces = listOf(
        RecentPlace("Office", "2972 Westheimer Rd. Santa Ana, Illinois 85486", "2.7km"),
        RecentPlace("Coffee shop", "1901 Thornridge Cir. Shiloh, Hawaii 81063", "1.1km"),
        RecentPlace("Shopping center", "4140 Parker Rd. Allentown, New Mexico 31134", "4.9km"),
        RecentPlace("Shopping mall", "4140 Parker Rd. Allentown, New Mexico 31134", "4.0km")
    ).map { it.toPlace() }

    // Trạng thái cho ô nhập "From" và "To"
    var fromText by remember { mutableStateOf(pointToAddress(userLocation)) }
    var toText by remember { mutableStateOf("") }

    // Trạng thái để lưu tọa độ của địa điểm được chọn
    var fromPoint by remember { mutableStateOf(userLocation) } // Mặc định là vị trí hiện tại
    var toPoint by remember { mutableStateOf<Point?>(null) }

    // Trạng thái cho danh sách đề xuất
    var fromSuggestions by remember { mutableStateOf<List<Place>>(emptyList()) }
    var toSuggestions by remember { mutableStateOf<List<Place>>(emptyList()) }
    var showFromSuggestions by remember { mutableStateOf(false) }
    var showToSuggestions by remember { mutableStateOf(false) }

    // Coroutine scope để gọi API
    val coroutineScope = rememberCoroutineScope()

    // Hàm tìm kiếm địa điểm cho ô "From"
    fun searchFrom(query: String) {
        if (query.isNotBlank()) {
            coroutineScope.launch {
                try {
                    fromSuggestions = searchPlaces(query, mapboxAccessToken)
                    showFromSuggestions = fromSuggestions.isNotEmpty()
                } catch (e: Exception) {
                    // Xử lý lỗi (có thể hiển thị thông báo lỗi)
                    showFromSuggestions = false
                }
            }
        } else {
            showFromSuggestions = false
        }
    }

    // Hàm tìm kiếm địa điểm cho ô "To"
    fun searchTo(query: String) {
        if (query.isNotBlank()) {
            coroutineScope.launch {
                try {
                    toSuggestions = searchPlaces(query, mapboxAccessToken)
                    showToSuggestions = toSuggestions.isNotEmpty()
                } catch (e: Exception) {
                    // Xử lý lỗi (có thể hiển thị thông báo lỗi)
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
            usePlatformDefaultWidth = false // Cho phép tùy chỉnh chiều rộng
        )
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .fillMaxHeight() // Tràn toàn bộ chiều cao màn hình
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
                    .weight(1f) // Chiếm toàn bộ không gian còn lại, trừ nút Confirm
            ) {
                // Ô nhập "From"
                item {
                    OutlinedTextField(
                        value = fromText,
                        onValueChange = {
                            fromText = it
                            searchFrom(it) // Tìm kiếm địa điểm khi nhập
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        placeholder = { Text("From", color = Color.Gray) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Location",
                                tint = Color.Gray
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                // Danh sách đề xuất cho "From"
                if (showFromSuggestions && fromSuggestions.isNotEmpty()) {
                    item {
                        Text(
                            text = "Suggestions for From",
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
                    items(fromSuggestions) { place ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    fromText = place.address
                                    fromPoint = place.coordinates
                                    showFromSuggestions = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Location",
                                tint = Color.Gray,
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
                        }
                    }
                }

                // Ô nhập "To"
                item {
                    OutlinedTextField(
                        value = toText,
                        onValueChange = {
                            toText = it
                            searchTo(it) // Tìm kiếm địa điểm khi nhập
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        placeholder = { Text("To", color = Color.Gray) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Location",
                                tint = Color.Gray
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
                                    showToSuggestions = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Location",
                                tint = Color.Gray,
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
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Location",
                            tint = Color.Gray,
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
                    onConfirm(fromPoint, toPoint) // Trả về tọa độ From và To
                    onDismiss() // Đóng dialog
                },
                modifier = Modifier
                    .width(336.dp)
                    .height(48.dp)
                    .padding(top = 16.dp)
                    .clip(RoundedCornerShape(8.dp)),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFEDAE10) // Màu vàng giống nút Rental
                ),
                contentPadding = PaddingValues(0.dp)
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