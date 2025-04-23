package com.example.dacs31.data

import com.google.firebase.Timestamp
import com.mapbox.geojson.Point

data class Trip(
    val id: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val pickupLocation: Point? = null,
    val destination: Point? = null,
    val time: Timestamp? = null,
    val status: String = "", // "pending", "accepted", "completed", "canceled"

)