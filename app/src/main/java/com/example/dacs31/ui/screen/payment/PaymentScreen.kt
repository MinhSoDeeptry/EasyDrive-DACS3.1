package com.example.dacs31.ui.screen.payment

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.dacs31.R
import kotlin.math.roundToInt

@Composable
fun PaymentScreen(
    selectedTransport: String,
    routeDistance: Double, // Quãng đường (mét)
    onDismiss: () -> Unit,
    onConfirmRide: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedPaymentMethod by remember { mutableStateOf("Visa") }

    // Tính giá dựa trên quãng đường và phương tiện
    val distanceInKm = routeDistance / 1000 // Chuyển từ mét sang km
    val basePrice = 17000 // Giá 2km đầu (VND)
    val additionalRate = 3000 // Giá mỗi km sau 2km (VND)
    var totalPrice = 0

    when {
        distanceInKm <= 2 -> totalPrice = basePrice
        distanceInKm > 2 -> {
            val additionalKm = distanceInKm - 2
            totalPrice = basePrice + (additionalKm * additionalRate).roundToInt()
        }
    }

    // Nhân đôi giá nếu là xe ô tô
    if (selectedTransport == "Car") {
        totalPrice *= 2
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnClickOutside = true,
            dismissOnBackPress = true
        )
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .background(Color.White, RoundedCornerShape(16.dp))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: Nút Back và Tiêu đề
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { onDismiss() },
                    tint = Color.Black
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Payment",
                    style = TextStyle(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    ),
                    modifier = Modifier.weight(1f)
                )
            }

            // Thông tin xe
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFFFBE7), RoundedCornerShape(8.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Mustang Shelby GT",
                        style = TextStyle(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_star),
                            contentDescription = "Star",
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "4.9 (531 reviews)",
                            style = TextStyle(
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                        )
                    }
                }
                // Hình ảnh xe (giả lập bằng placeholder)
                Spacer(modifier = Modifier
                    .size(80.dp)
                    .background(Color.Gray, RoundedCornerShape(8.dp)))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Chi tiết giá
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Distance",
                        style = TextStyle(
                            fontSize = 16.sp,
                            color = Color.Black
                        )
                    )
                    Text(
                        text = String.format("%.2f km", distanceInKm),
                        style = TextStyle(
                            fontSize = 16.sp,
                            color = Color.Black
                        )
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Price",
                        style = TextStyle(
                            fontSize = 16.sp,
                            color = Color.Black
                        )
                    )
                    Text(
                        text = "₫${totalPrice}",
                        style = TextStyle(
                            fontSize = 16.sp,
                            color = Color.Black
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Phương thức thanh toán
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Select payment method",
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                )
                Text(
                    text = "View All",
                    style = TextStyle(
                        fontSize = 14.sp,
                        color = Color(0xFFEDAE10),
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = Modifier.clickable { /* TODO: Xử lý View All */ }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Danh sách phương thức thanh toán
            PaymentMethodOption(
                iconResId = R.drawable.ic_visa,
                label = "Visa",
                cardNumber = "**** **** **** 8970",
                expiry = "12/26",
                isSelected = selectedPaymentMethod == "Visa",
                onClick = { selectedPaymentMethod = "Visa" }
            )
            PaymentMethodOption(
                iconResId = R.drawable.ic_mastercard,
                label = "Mastercard",
                cardNumber = "**** **** **** 8970",
                expiry = "12/26",
                isSelected = selectedPaymentMethod == "Mastercard",
                onClick = { selectedPaymentMethod = "Mastercard" }
            )
            PaymentMethodOption(
                iconResId = R.drawable.ic_paypal,
                label = "PayPal",
                email = "mailaddress@mail.com",
                expiry = "12/26",
                isSelected = selectedPaymentMethod == "PayPal",
                onClick = { selectedPaymentMethod = "PayPal" }
            )
            PaymentMethodOption(
                iconResId = R.drawable.ic_cash,
                label = "Cash",
                isSelected = selectedPaymentMethod == "Cash",
                onClick = { selectedPaymentMethod = "Cash" }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Nút Confirm Ride
            Button(
                onClick = onConfirmRide,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(8.dp)),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFEDAE10)
                )
            ) {
                Text(
                    text = "Confirm Ride",
                    color = Color.Black,
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }
}

@Composable
fun PaymentMethodOption(
    iconResId: Int,
    label: String,
    cardNumber: String? = null,
    email: String? = null,
    expiry: String? = null,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(if (isSelected) Color(0xFFFFFBE7) else Color.White, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = iconResId),
            contentDescription = label,
            tint = if (label == "Cash") Color.Gray else Color.Unspecified,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            when {
                cardNumber != null -> Text(
                    text = "$label $cardNumber",
                    style = TextStyle(
                        fontSize = 16.sp,
                        color = Color.Black
                    )
                )
                email != null -> Text(
                    text = "$label $email",
                    style = TextStyle(
                        fontSize = 16.sp,
                        color = Color.Black
                    )
                )
                else -> Text(
                    text = label,
                    style = TextStyle(
                        fontSize = 16.sp,
                        color = if (label == "Cash") Color.Gray else Color.Black
                    )
                )
            }
            if (expiry != null) {
                Text(
                    text = "Expires: $expiry",
                    style = TextStyle(
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                )
            }
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
}