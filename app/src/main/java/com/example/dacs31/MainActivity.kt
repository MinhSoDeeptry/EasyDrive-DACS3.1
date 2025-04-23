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
import com.example.dacs31.ui.screen.*
import com.example.dacs31.ui.screen.customer.CustomerHomeScreen
import com.example.dacs31.ui.screen.driver.DriverHomeScreen
import com.example.dacs31.ui.screen.history.HistoryScreen
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
    var startDestination by remember { mutableStateOf("signin") }

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            val currentUser = authRepository.getCurrentUser()
            startDestination = if (currentUser != null) {
                try {
                    val role = authRepository.getUserRole() ?: "unknown"
                    when (role) {
                        "driver" -> "driver_home"
                        "customer" -> "customer_home"
                        else -> "signin"
                    }
                } catch (e: Exception) {
                    "signin"
                }
            } else {
                "signin"
            }

            navController.navigate(startDestination) {
                popUpTo(navController.graph.startDestinationId) { inclusive = true }
            }
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
        composable("profile") {
            ProfileScreen(
                navController = navController,
                authRepository = authRepository
            )
        }
        composable("history") {
            HistoryScreen(navController = navController, authRepository = authRepository)
        }
//        composable("history") {
//            HistoryScreen(
//                navController = navController,
//                authRepository = authRepository
//            )
//        }
//        composable("compliment") {
//            ComplimentScreen(
//                navController = navController,
//                authRepository = authRepository
//            )
//        }
//        composable("balance") {
//            BalanceScreen(
//                navController = navController,
//                authRepository = authRepository
//            )
//        }
//        composable("about_us") {
//            AboutUsScreen(
//                navController = navController,
//                authRepository = authRepository
//            )
//        }
//        composable("settings") {
//            SettingsScreen(
//                navController = navController,
//                authRepository = authRepository
//            )
//        }
//        composable("help_support") {
//            HelpSupportScreen(
//                navController = navController,
//                authRepository = authRepository
//            )
//        }
        composable("home") {
            CustomerHomeScreen(
                navController = navController,
                authRepository = authRepository
            )
        }
//        composable("favourite") {
//            FavouriteScreen(
//                navController = navController,
//                authRepository = authRepository
//            )
//        }
//        composable("wallet") {
//            WalletScreen(
//                navController = navController,
//                authRepository = authRepository
//            )
//        }
//        composable("offer") {
//            OfferScreen(
//                navController = navController,
//                authRepository = authRepository
//            )
//        }
    }
}