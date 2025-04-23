package com.example.dacs31.data

data class User(
    val uid: String = "",
    val email: String = "",
    val fullName: String = "",
    val phoneNumber: String = "",
    val gender: String = "",
    val address: String = "",
    val role: String = "Customer", // Mặc định là Customer
    val createdAt: Long = System.currentTimeMillis()
)