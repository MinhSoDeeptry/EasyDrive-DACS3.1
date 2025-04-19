package com.example.dacs31.data

data class User(
    val uid: String = "",
    val email: String = "",
    val fullName: String = "",
    val role: String = "Customer", // Thêm role, mặc định là Customer
    val createdAt: Long = System.currentTimeMillis()
)