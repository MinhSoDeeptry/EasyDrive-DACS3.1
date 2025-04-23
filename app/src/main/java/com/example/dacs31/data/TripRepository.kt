package com.example.dacs31.data

import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.mapbox.geojson.Point
import kotlinx.coroutines.tasks.await
import android.util.Log

class TripRepository {
    private val db = Firebase.firestore
    private val requestsCollection = db.collection("requests")

    suspend fun getTripsByDriver(driverId: String): List<Trip> {
        Log.d("TripRepository", "Fetching trips for driverId: $driverId")
        return try {
            val snapshot = requestsCollection
                .whereEqualTo("driverId", driverId)
                .get()
                .await()

            Log.d("TripRepository", "Found ${snapshot.documents.size} trips")

            val trips = mutableListOf<Trip>()
            for (doc in snapshot.documents) {
                val customerId = doc.getString("customerId") ?: continue
                val status = doc.getString("status") ?: continue
                val pickupData = doc.get("pickupLocation") as? Map<String, Double>
                val destData = doc.get("destination") as? Map<String, Double>

                val pickupLocation = pickupData?.let {
                    Point.fromLngLat(it["longitude"] ?: 0.0, it["latitude"] ?: 0.0)
                }

                val destination = destData?.let {
                    Point.fromLngLat(it["longitude"] ?: 0.0, it["latitude"] ?: 0.0)
                }

                trips.add(
                    Trip(
                        id = doc.id,
                        customerId = customerId,
                        pickupLocation = pickupLocation,
                        destination = destination,
                        time = doc.getTimestamp("createdAt"),
                        status = status
                    )
                )
            }
            Log.d("TripRepository", "Trips: $trips")
            trips
        } catch (e: Exception) {
            Log.e("TripRepository", "Error fetching trips: ${e.message}")
            emptyList()
        }
    }
}