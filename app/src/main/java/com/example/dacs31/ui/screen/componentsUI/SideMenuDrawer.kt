package com.example.dacs31.ui.screen.componentsUI

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.dacs31.data.AuthRepository
import com.example.dacs31.data.User
@Composable
fun SideMenuDrawer(
    user: User?,
    navController: NavController,
    authRepository: AuthRepository,
    onDrawerClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(300.dp)
            .background(Color.White)
            .padding(16.dp)
    ) {
        // Thêm nút Back ở góc trên bên trái
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.Start
        ) {
            IconButton(
                onClick = { onDrawerClose() },
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.Black,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Tiêu đề và thông tin người dùng
        Text(
            text = "Menu",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        user?.let {
            Text(
                text = it.fullName ?: "User",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = it.email ?: "No email",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        } ?: Text(
            text = "Loading user...",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Divider()

        Spacer(modifier = Modifier.height(16.dp))

        // Các mục menu
        DrawerItem(
            icon = Icons.Default.Home,
            label = "Home",
            onClick = {
                navController.navigate("home") {
                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                }
                onDrawerClose()
            }
        )

        DrawerItem(
            icon = Icons.Default.FavoriteBorder,
            label = "Favourite",
            onClick = {
                navController.navigate("favourite") {
                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                }
                onDrawerClose()
            }
        )

        DrawerItem(
            icon = Icons.Default.AccountBalanceWallet,
            label = "Wallet",
            onClick = {
                navController.navigate("wallet") {
                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                }
                onDrawerClose()
            }
        )

        DrawerItem(
            icon = Icons.Default.AccessTime,
            label = "History",
            onClick = {
                navController.navigate("history") {
                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                }
                onDrawerClose()
            }
        )

        DrawerItem(
            icon = Icons.Default.Person,
            label = "Profile",
            onClick = {
                navController.navigate("profile") {
                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                }
                onDrawerClose()
            }
        )

        Spacer(modifier = Modifier.weight(1f))

        // Nút đăng xuất
        DrawerItem(
            icon = Icons.Default.ExitToApp,
            label = "Logout",
            onClick = {
                authRepository.signOut()
                navController.navigate("signin") {
                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                }
                onDrawerClose()
            }
        )
    }
}

@Composable
fun DrawerItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(24.dp),
            tint = Color.Black
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Black
        )
    }
}