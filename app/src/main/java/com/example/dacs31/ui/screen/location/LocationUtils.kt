package com.example.dacs31.ui.screen.location

import android.util.Log
import com.mapbox.geojson.Point
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

// Data class cho địa điểm
data class Place(
    val name: String,
    val address: String,
    val distance: String? = null,
    val coordinates: Point? = null
)

// Giả lập RecentPlace thành Place
data class RecentPlace(
    val name: String,
    val address: String,
    val distance: String
) {
    fun toPlace(): Place = Place(name, address, distance)
}

// Giả lập chuyển đổi Point thành địa chỉ
fun pointToAddress(point: Point?): String {
    if (point == null) return "Current Location (Unknown)"
    return "123 Current St, Your City, Country"
}

// Lọc danh sách địa điểm để đề xuất dựa trên từ khóa (dùng cho danh sách tĩnh)
fun suggestPlaces(query: String, places: List<Place>): List<Place> {
    if (query.isBlank()) return places
    return places.filter {
        it.name.contains(query, ignoreCase = true) || it.address.contains(query, ignoreCase = true)
    }
}

// Gọi Mapbox Geocoding API để tìm kiếm địa điểm thực tế qua HTTP
suspend fun searchPlaces(query: String, accessToken: String): List<Place> = suspendCancellableCoroutine { continuation ->
    val client = OkHttpClient()
    val url = "https://api.mapbox.com/geocoding/v5/mapbox.places/${query}.json?access_token=$accessToken&country=VN&types=address&language=vi"

    val request = Request.Builder()
        .url(url)
        .build()

    client.newCall(request).enqueue(object : okhttp3.Callback {
        override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
            Log.e("MapboxGeocoding", "Error fetching geocoding data: ${e.message}")
            continuation.resumeWithException(e)
        }

        override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
            response.body?.string()?.let { jsonString ->
                Log.d("MapboxGeocoding", "Geocoding response: $jsonString")
                val json = JSONObject(jsonString)
                val features = json.getJSONArray("features")
                val places = mutableListOf<Place>()
                for (i in 0 until features.length()) {
                    val feature = features.getJSONObject(i)
                    val placeName = feature.getString("place_name")
                    val address = if (feature.has("properties") && feature.getJSONObject("properties").has("address")) {
                        feature.getJSONObject("properties").getString("address")
                    } else {
                        placeName
                    }
                    val coordinatesJson = feature.getJSONObject("geometry").getJSONArray("coordinates")
                    val lng = coordinatesJson.getDouble(0)
                    val lat = coordinatesJson.getDouble(1)
                    val coordinates = Point.fromLngLat(lng, lat)
                    places.add(Place(placeName, address, coordinates = coordinates))
                }
                continuation.resume(places)
            } ?: continuation.resume(emptyList())
        }
    })
}

// Gọi Mapbox Directions API để lấy tuyến đường và khoảng cách
suspend fun getRoute(from: Point, to: Point, accessToken: String): Pair<List<Point>, Double> = suspendCancellableCoroutine { continuation ->
    val client = OkHttpClient()
    val url = "https://api.mapbox.com/directions/v5/mapbox/driving/${from.longitude()},${from.latitude()};${to.longitude()},${to.latitude()}?geometries=geojson&access_token=$accessToken"

    Log.d("MapboxDirections", "Requesting route with URL: $url")

    val request = Request.Builder()
        .url(url)
        .build()

    client.newCall(request).enqueue(object : okhttp3.Callback {
        override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
            Log.e("MapboxDirections", "Error fetching route: ${e.message}")
            continuation.resumeWithException(e)
        }

        override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
            response.body?.string()?.let { jsonString ->
                Log.d("MapboxDirections", "Directions response: $jsonString")
                val json = JSONObject(jsonString)
                val routes = json.getJSONArray("routes")
                if (routes.length() > 0) {
                    val route = routes.getJSONObject(0)
                    val distance = route.getDouble("distance") // Khoảng cách (mét)
                    val geometry = route.getJSONObject("geometry")
                    val coordinates = geometry.getJSONArray("coordinates")
                    val points = mutableListOf<Point>()
                    for (i in 0 until coordinates.length()) {
                        val coord = coordinates.getJSONArray(i)
                        val lng = coord.getDouble(0)
                        val lat = coord.getDouble(1)
                        points.add(Point.fromLngLat(lng, lat))
                    }
                    Log.d("MapboxDirections", "Route points: $points, Distance: $distance m")
                    continuation.resume(Pair(points, distance))
                } else {
                    Log.w("MapboxDirections", "No routes found in response")
                    continuation.resume(Pair(emptyList(), 0.0))
                }
            } ?: run {
                Log.w("MapboxDirections", "Empty response body")
                continuation.resume(Pair(emptyList(), 0.0))
            }
        }
    })
}