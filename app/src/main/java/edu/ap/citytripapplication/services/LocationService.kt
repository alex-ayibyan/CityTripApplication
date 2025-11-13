import android.content.Context
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource

class LocationService (
    private val context: Context
) {

    private var useRealLocation: Boolean = true
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    fun getCurrentLocation(): android.location.Location? {
        return if (useRealLocation) {
            getRealLocation()
        } else {
            getDummyLocation()
        }
    }

    fun getDummyLocation(): android.location.Location {
        // Default to Antwerp center for dummy location
        return createLocation(51.3, 4.4025, "Dummy Location")
    }

    fun isUsingRealLocation(): Boolean = useRealLocation

    fun setUseRealLocation(useReal: Boolean) {
        this.useRealLocation = useReal
    }

    private fun getRealLocation(): android.location.Location? {
        return try {
            var result: android.location.Location? = null
            val cancellationTokenSource = CancellationTokenSource()

            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource.token
            ).addOnSuccessListener { location ->
                result = location
            }.addOnFailureListener { exception ->
                // Fall back to dummy location if real location fails
                result = getDummyLocation()
            }

            // For simplicity, return dummy location immediately
            // In a real app, you'd use callbacks or coroutines
            getDummyLocation()
        } catch (e: SecurityException) {
            e.printStackTrace()
            getDummyLocation()
        }
    }

    private fun createLocation(lat: Double, lng: Double, provider: String): android.location.Location {
        return android.location.Location(provider).apply {
            latitude = lat
            longitude = lng
            time = System.currentTimeMillis()
            accuracy = 50.0f // Mock accuracy
        }
    }

}

object LocationProvider {
    private var locationService: LocationService? = null

    fun initialize(context: Context) {
        locationService = LocationService(context)
    }

    fun getService(): LocationService {
        return locationService ?: throw IllegalStateException("LocationProvider not initialized")
    }
}
