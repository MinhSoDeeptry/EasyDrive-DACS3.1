package com.example.dacs31.ui.screen.componentsUI

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
            .fillMaxWidth(0.75f)
            .background(Color.White)
            .systemBarsPadding()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile Picture",
                    modifier = Modifier.size(40.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = user?.fullName ?: "Guest",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = user?.email ?: "email@example.com",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
        }

        Divider(color = Color.LightGray, thickness = 1.dp)

        Spacer(modifier = Modifier.height(16.dp))

        MenuItem(
            icon = Icons.Default.Person,
            title = "Profile",
            onClick = {
                navController.navigate("profile")
                onDrawerClose()
            }
        )

        MenuItem(
            icon = Icons.Default.History,
            title = "History",
            onClick = {
                navController.navigate("history")
                onDrawerClose()
            }
        )

        MenuItem(
            icon = Icons.Default.ThumbUp,
            title = "Compliment",
            onClick = {
                navController.navigate("compliment")
                onDrawerClose()
            }
        )

        MenuItem(
            icon = Icons.Default.AccountBalance,
            title = "Balance",
            onClick = {
                navController.navigate("balance")
                onDrawerClose()
            }
        )

        MenuItem(
            icon = Icons.Default.Info,
            title = "About Us",
            onClick = {
                navController.navigate("about_us")
                onDrawerClose()
            }
        )

        MenuItem(
            icon = Icons.Default.Settings,
            title = "Settings",
            onClick = {
                navController.navigate("settings")
                onDrawerClose()
            }
        )

        MenuItem(
            icon = Icons.Default.Help,
            title = "Help & Support",
            onClick = {
                navController.navigate("help_support")
                onDrawerClose()
            }
        )

        Spacer(modifier = Modifier.weight(1f))

        MenuItem(
            icon = Icons.Default.ExitToApp,
            title = "Logout",
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
fun MenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
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
            contentDescription = title,
            tint = Color.Black,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            fontSize = 16.sp,
            color = Color.Black
        )
    }
}