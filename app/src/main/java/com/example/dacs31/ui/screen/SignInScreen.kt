package com.example.dacs31.ui.screen

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.dacs31.R
import com.example.dacs31.data.AuthRepository
import kotlinx.coroutines.launch

@Composable
fun SignInScreen(
    navController: NavController,
    authRepository: AuthRepository
) {
    val coroutineScope = rememberCoroutineScope()
    var emailOrPhone by remember { mutableStateOf(TextFieldValue()) }
    var password by remember { mutableStateOf(TextFieldValue()) }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = "Sign in",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = emailOrPhone,
            onValueChange = { emailOrPhone = it },
            label = { Text("Email or Phone Number") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            enabled = !isLoading
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Enter Your Password") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            enabled = !isLoading,
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password"
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = { /* TODO: Xử lý quên mật khẩu */ }) {
                Text(
                    text = "Forget password?",
                    color = Color.Red,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                coroutineScope.launch {
                    isLoading = true
                    errorMessage = null
                    val result = authRepository.login(emailOrPhone.text, password.text)
                    if (result.isSuccess) {
                        val user = authRepository.getCurrentUser()
                        if (user != null) {
                            val role = user.role
                            Log.d("SignInScreen", "Đăng nhập thành công, vai trò: $role")
                            when (role) {
                                "Driver" -> {
                                    Log.d("SignInScreen", "Điều hướng đến driver_home")
                                    navController.navigate("driver_home") {
                                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                                    }
                                }
                                "Customer" -> {
                                    Log.d("SignInScreen", "Điều hướng đến customer_home")
                                    navController.navigate("customer_home") {
                                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                                    }
                                }
                                else -> {
                                    Log.e("SignInScreen", "Vai trò không hợp lệ: $role")
                                    errorMessage = "Vai trò không hợp lệ. Vui lòng đăng nhập lại."
                                    authRepository.signOut()
                                }
                            }
                        } else {
                            Log.e("SignInScreen", "Không thể lấy thông tin người dùng")
                            errorMessage = "Không thể lấy thông tin người dùng. Vui lòng thử lại."
                        }
                    } else {
                        Log.e("SignInScreen", "Đăng nhập thất bại: ${result.exceptionOrNull()?.message}")
                        errorMessage = result.exceptionOrNull()?.message ?: "Đăng nhập thất bại"
                    }
                    isLoading = false
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB800)),
            shape = RoundedCornerShape(8.dp),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Sign In", modifier = Modifier.padding(vertical = 8.dp))
            }
        }

        errorMessage?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = it,
                color = Color.Red,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            IconButton(onClick = { /* TODO: Google login */ }) {
                Icon(
                    painter = painterResource(R.drawable.ic_google),
                    contentDescription = "Sign in with Google",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(32.dp)
                )
            }
            IconButton(onClick = { /* TODO: Facebook login */ }) {
                Icon(
                    painter = painterResource(R.drawable.ic_facebook),
                    contentDescription = "Sign in with Facebook",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(32.dp)
                )
            }
            IconButton(onClick = { /* TODO: Apple login */ }) {
                Icon(
                    painter = painterResource(R.drawable.ic_apple),
                    contentDescription = "Sign in with Apple",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Don't have an account?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(onClick = { navController.navigate("signup") }) {
                Text(
                    text = "Sign up",
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SignInScreenPreview() {
    MaterialTheme {
        SignInScreen(
            navController = rememberNavController(),
            authRepository = AuthRepository()
        )
    }
}