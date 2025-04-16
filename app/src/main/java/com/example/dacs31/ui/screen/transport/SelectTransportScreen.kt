package com.example.dacs31.ui.screen.transport

import android.util.Log
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.dacs31.R

@Composable
fun SelectTransportScreen(
    onDismiss: () -> Unit,
    onTransportSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTransport by remember { mutableStateOf<String?>(null) }

    Log.d("SelectTransportScreen", "Dialog opened")

    Dialog(
        onDismissRequest = {
            Log.d("SelectTransportScreen", "Dialog dismissed via onDismissRequest")
            onDismiss()
        },
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
                        .clickable {
                            Log.d("SelectTransportScreen", "Back button clicked")
                            onDismiss()
                        },
                    tint = Color.Black
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Select your transport",
                    style = TextStyle(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF2A2A2A),
                        lineHeight = 30.sp
                    ),
                    modifier = Modifier.weight(1f)
                )
            }

            // Hàng 1: Car và Bike
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TransportOption(
                    iconResId = R.drawable.ic_car,
                    label = "Car",
                    isSelected = selectedTransport == "Car",
                    onClick = {
                        Log.d("SelectTransportScreen", "Car selected")
                        selectedTransport = "Car"
                        onTransportSelected("Car")
                        onDismiss()
                    },
                    modifier = Modifier
                        .size(160.dp) // Đảm bảo kích thước đồng nhất
                )

                TransportOption(
                    iconResId = R.drawable.ic_bike,
                    label = "Bike",
                    isSelected = selectedTransport == "Bike",
                    onClick = {
                        Log.d("SelectTransportScreen", "Bike selected")
                        selectedTransport = "Bike"
                        onTransportSelected("Bike")
                        onDismiss()
                    },
                    modifier = Modifier
                        .size(160.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Hàng 2: Cycle và Taxi
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TransportOption(
                    iconResId = R.drawable.ic_cycle,
                    label = "Cycle",
                    isSelected = selectedTransport == "Cycle",
                    onClick = {
                        Log.d("SelectTransportScreen", "Cycle selected")
                        selectedTransport = "Cycle"
                        onTransportSelected("Cycle")
                        onDismiss()
                    },
                    modifier = Modifier
                        .size(160.dp)
                )

                TransportOption(
                    iconResId = R.drawable.ic_taxi,
                    label = "Taxi",
                    isSelected = selectedTransport == "Taxi",
                    onClick = {
                        Log.d("SelectTransportScreen", "Taxi selected")
                        selectedTransport = "Taxi"
                        onTransportSelected("Taxi")
                        onDismiss()
                    },
                    modifier = Modifier
                        .size(160.dp)
                )
            }
        }
    }
}

@Composable
fun TransportOption(
    iconResId: Int,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) Color(0xFFFFFBE7) else Color(0xFFF0F0F0))
            .clickable { onClick() }
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(id = iconResId),
            contentDescription = label,
            tint = Color.Black,
            modifier = Modifier
                .size(48.dp) // Đảm bảo icon đồng nhất
                .aspectRatio(1f) // Giữ tỷ lệ 1:1
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = TextStyle(
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black,
                textAlign = TextAlign.Center
            ),
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        )
    }
}