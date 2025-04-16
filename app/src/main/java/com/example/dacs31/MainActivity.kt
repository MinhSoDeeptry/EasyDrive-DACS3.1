package com.example.dacs31

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.dacs31.ui.screen.customer.CustomerHomeScreen
import com.example.dacs31.ui.screen.driver.DriverHomeScreen
import com.example.dacs31.ui.screen.ProfileScreen
import com.example.dacs31.ui.screen.SignInScreen
import com.example.dacs31.ui.screen.SignUpScreen
import com.example.dacs31.ui.screen.WelcomeScreen
import com.example.dacs31.ui.theme.DACS31Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DACS31Theme {
                MainApp()
            }
        }
    }
}

@Composable
fun MainApp() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = "welcome"
    ) {
        composable("welcome") {
            WelcomeScreen(navController = navController)
        }
        composable("signup") {
            SignUpScreen(navController = navController)
        }
        composable("signin") {
            SignInScreen(navController = navController)
        }
        composable("profile") {
            ProfileScreen(navController = navController)
        }
        composable("driver_home") {
            DriverHomeScreen(navController = navController)
        }
        composable("customer_home") {
            CustomerHomeScreen(navController = navController)
        }
        // Các màn hình khác trong BottomNavigation
        composable("favourite") {
            // TODO: Thêm FavouriteScreen
        }
        composable("wallet") {
            // TODO: Thêm WalletScreen
        }
        composable("offer") {
            // TODO: Thêm OfferScreen
        }
    }
}