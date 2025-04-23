package com.example.dacs31

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.dacs31.data.AuthRepository
import com.example.dacs31.ui.screen.SignInScreen
import com.example.dacs31.ui.screen.SignUpScreen
import com.example.dacs31.ui.screen.customer.CustomerHomeScreen
import com.example.dacs31.ui.screen.driver.DriverHomeScreen
import com.example.dacs31.ui.theme.DACS31Theme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val authRepository = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DACS31Theme {
                AppNavigation(authRepository)
            }
        }
    }
}

@Composable
fun AppNavigation(authRepository: AuthRepository) {
    val navController = rememberNavController()
    val coroutineScope = rememberCoroutineScope()
    var startDestination by remember { mutableStateOf("signin") } // Khởi tạo mặc định

    // Lấy trạng thái đăng nhập và vai trò khi ứng dụng khởi động
    LaunchedEffect(Unit) {
        // Làm mới token nếu có người dùng hiện tại
        val currentUser = authRepository.getCurrentUser()
        if (currentUser != null) {
            try {
                currentUser.uid?.let { uid ->
                    val role = authRepository.getUserRole()
                    startDestination = when (role) {
                        "driver" -> "driver_home"
                        "customer" -> "customer_home"
                        else -> "signin" // Nếu vai trò không hợp lệ, chuyển về đăng nhập
                    }
                }
            } catch (e: Exception) {
                // Nếu có lỗi khi lấy vai trò, chuyển về màn hình đăng nhập
                startDestination = "signin"
            }
        } else {
            startDestination = "signin"
        }

        // Điều hướng đến startDestination
        navController.navigate(startDestination) {
            popUpTo(navController.graph.startDestinationId) { inclusive = true }
        }
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable("signin") {
            SignInScreen(
                navController = navController,
                authRepository = authRepository
            )
        }
        composable("signup") {
            SignUpScreen(
                navController = navController,
                authRepository = authRepository
            )
        }
        composable("customer_home") {
            CustomerHomeScreen(
                navController = navController,
                authRepository = authRepository
            )
        }
        composable("driver_home") {
            DriverHomeScreen(
                navController = navController,
                authRepository = authRepository
            )
        }
    }
}