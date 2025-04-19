package com.example.dacs31

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
    val startDestination = if (authRepository.getCurrentUser() != null) {
        LaunchedEffect(Unit) {
            val role = authRepository.getUserRole()
            if (role == "Driver") "driver_home" else "customer_home"
        }
        "customer_home" // Mặc định, sẽ được thay thế sau khi LaunchedEffect chạy
    } else {
        "signin"
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
            CustomerHomeScreen(navController = navController,
                authRepository = authRepository)
        }
        composable("driver_home") {
            DriverHomeScreen(
                navController = navController,
                authRepository = authRepository
            )
        }
    }
}