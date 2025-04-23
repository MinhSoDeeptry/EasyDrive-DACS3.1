package com.example.dacs31.ui.screen

import android.util.Log
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.dacs31.data.AuthRepository
import com.example.dacs31.data.User
import com.example.dacs31.ui.screen.componentsUI.BottomControlBar
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    navController: NavController,
    authRepository: AuthRepository
) {
    Box(modifier = Modifier.fillMaxSize()) {
        val coroutineScope = rememberCoroutineScope()
        var user by remember { mutableStateOf<User?>(null) }
        var fullName by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        var mobileNumber by remember { mutableStateOf("") }
        var gender by remember { mutableStateOf("") }
        var address by remember { mutableStateOf("") }
        var errorMessage by remember { mutableStateOf<String?>(null) }
        var isLoading by remember { mutableStateOf(true) }
        var isUpdating by remember { mutableStateOf(false) }

        val db = Firebase.firestore
        val usersCollection = db.collection("users")

        // Lấy thông tin người dùng
        LaunchedEffect(Unit) {
            val currentUser = authRepository.getCurrentUser()
            if (currentUser == null) {
                Log.e("ProfileScreen", "Người dùng chưa đăng nhập")
                errorMessage = "Vui lòng đăng nhập để tiếp tục."
                navController.navigate("signin") {
                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                }
                return@LaunchedEffect
            }

            val userFromFirestore = authRepository.getUserFromFirestore(currentUser.uid)
            if (userFromFirestore != null) {
                user = userFromFirestore
                fullName = userFromFirestore.fullName
                email = userFromFirestore.email
                mobileNumber = userFromFirestore.phoneNumber
                gender = userFromFirestore.gender
                address = userFromFirestore.address
                isLoading = false
            } else {
                val userData = User(
                    uid = currentUser.uid,
                    email = currentUser.email,
                    fullName = currentUser.fullName,
                    role = currentUser.role
                )
                usersCollection.document(currentUser.uid).set(userData)
                    .addOnSuccessListener {
                        user = userData
                        fullName = userData.fullName
                        email = userData.email
                        isLoading = false
                    }
                    .addOnFailureListener { e ->
                        errorMessage = "Không thể tạo hồ sơ: ${e.message}"
                        isLoading = false
                    }
            }
        }

        if (isLoading || user == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back"
                    )
                }
                Text(
                    text = "Edit Profile",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .weight(1f)
                        .wrapContentWidth(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.width(48.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Avatar
            Box(
                modifier = Modifier.size(100.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(Color.LightGray),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profile Picture",
                        modifier = Modifier.size(64.dp)
                    )
                }
                IconButton(
                    onClick = { /* TODO */ },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 8.dp, y = 8.dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFB800))
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Edit",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = fullName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = email,
                onValueChange = {},
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                enabled = false,
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = Color.Black,
                    disabledBorderColor = Color.Gray,
                    disabledLabelColor = Color.Gray
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(Color.Green)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+880", style = MaterialTheme.typography.bodyMedium)
                }
                OutlinedTextField(
                    value = mobileNumber,
                    onValueChange = { mobileNumber = it },
                    label = { Text("Your mobile number") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    enabled = !isUpdating
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            var expanded by remember { mutableStateOf(false) }
            val genderOptions = listOf("Male", "Female", "Other")

            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = gender,
                    onValueChange = {},
                    label = { Text("Gender") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false,
                    shape = RoundedCornerShape(8.dp),
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Dropdown",
                            modifier = Modifier.clickable { expanded = true }
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = Color.Black,
                        disabledBorderColor = Color.Gray,
                        disabledLabelColor = Color.Gray
                    )
                )
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    genderOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                gender = option
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Address") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                enabled = !isUpdating
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    coroutineScope.launch {
                        isUpdating = true
                        errorMessage = null
                        val updatedData = mapOf(
                            "fullName" to fullName,
                            "phoneNumber" to mobileNumber,
                            "gender" to gender,
                            "address" to address
                        )
                        usersCollection.document(user!!.uid).update(updatedData)
                            .addOnSuccessListener {
                                isUpdating = false
                                navController.popBackStack()
                            }
                            .addOnFailureListener { e ->
                                errorMessage = "Cập nhật thất bại: ${e.message}"
                                isUpdating = false
                            }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB800)),
                shape = RoundedCornerShape(8.dp),
                enabled = !isUpdating
            ) {
                if (isUpdating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("UPDATE", fontSize = 16.sp, fontWeight = FontWeight.Bold)
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

            Spacer(modifier = Modifier.height(64.dp)) // <--- Fix scroll by spacing for BottomControlBar
        }


    }
}

@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    MaterialTheme {
        ProfileScreen(
            navController = rememberNavController(),
            authRepository = AuthRepository()
        )
    }
}
