package edu.ap.citytripapplication.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import edu.ap.citytripapplication.database.dao.CityDao
import edu.ap.citytripapplication.database.dao.LocationDao
import edu.ap.citytripapplication.database.dao.ReviewDao
import edu.ap.citytripapplication.database.entity.CachedCity
import edu.ap.citytripapplication.database.entity.CachedLocation
import edu.ap.citytripapplication.database.entity.CachedReview
import edu.ap.citytripapplication.model.City
import edu.ap.citytripapplication.model.Location
import edu.ap.citytripapplication.model.LocationCategory
import edu.ap.citytripapplication.model.Review
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class CityTripRepository(
    private val cityDao: CityDao,
    private val locationDao: LocationDao,
    private val reviewDao: ReviewDao,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    // ==================== CITIES ====================

    /**
     * Get all cities from local cache
     */
    fun getAllCitiesFlow(): Flow<List<City>> {
        return cityDao.getAllCities().map { cachedCities ->
            cachedCities.map { it.toCity() }
        }
    }

    /**
     * Sync cities from Firebase to local cache
     */
    suspend fun syncCitiesFromFirebase(): Result<Int> {
        return try {
            println("SYNC: Starting cities sync from Firebase...")
            
            val citiesSnapshot = firestore.collection("cities")
                .get()
                .await()

            val cachedCities = citiesSnapshot.documents.map { doc ->
                CachedCity(
                    id = doc.id,
                    name = doc.getString("name") ?: "",
                    country = doc.getString("country") ?: "",
                    description = doc.getString("description") ?: "",
                    latitude = doc.getDouble("latitude") ?: 0.0,
                    longitude = doc.getDouble("longitude") ?: 0.0,
                    imageUrl = doc.getString("imageUrl") ?: "",
                    createdBy = doc.getString("createdBy") ?: "",
                    createdAt = (doc.getTimestamp("createdAt") ?: Timestamp.now()).seconds * 1000,
                    lastSyncedAt = System.currentTimeMillis()
                )
            }

            cityDao.insertCities(cachedCities)
            println("SYNC: Successfully synced ${cachedCities.size} cities")
            
            Result.success(cachedCities.size)
        } catch (e: Exception) {
            println("SYNC: Error syncing cities: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun getCachedCitiesCount(): Int {
        return cityDao.getCitiesCount()
    }

    // ==================== LOCATIONS ====================

    /**
     * Get all locations from local cache
     */
    fun getAllLocationsFlow(): Flow<List<Location>> {
        return locationDao.getAllLocations().map { cachedLocations ->
            cachedLocations.map { it.toLocation() }
        }
    }

    /**
     * Get locations for a specific city from local cache
     */
    fun getLocationsByCityIdFlow(cityId: String): Flow<List<Location>> {
        return locationDao.getLocationsByCityId(cityId).map { cachedLocations ->
            cachedLocations.map { it.toLocation() }
        }
    }

    /**
     * Sync all locations from Firebase to local cache
     */
    suspend fun syncLocationsFromFirebase(): Result<Int> {
        return try {
            println("SYNC: Starting locations sync from Firebase...")
            
            val locationsSnapshot = firestore.collection("locations")
                .get()
                .await()

            val cachedLocations = locationsSnapshot.documents.map { doc ->
                CachedLocation(
                    id = doc.id,
                    name = doc.getString("name") ?: "",
                    description = doc.getString("description") ?: "",
                    latitude = doc.getDouble("latitude") ?: 0.0,
                    longitude = doc.getDouble("longitude") ?: 0.0,
                    category = doc.getString("category") ?: "OTHER",
                    cityId = doc.getString("cityId") ?: "",
                    userId = doc.getString("userId") ?: "",
                    createdAt = (doc.getTimestamp("createdAt") ?: Timestamp.now()).seconds * 1000,
                    averageRating = doc.getDouble("averageRating") ?: 0.0,
                    totalRatings = (doc.getLong("totalRatings") ?: 0).toInt(),
                    imageUrl = doc.getString("imageUrl") ?: "",
                    lastSyncedAt = System.currentTimeMillis()
                )
            }

            locationDao.insertLocations(cachedLocations)
            println("SYNC: Successfully synced ${cachedLocations.size} locations")
            
            Result.success(cachedLocations.size)
        } catch (e: Exception) {
            println("SYNC: Error syncing locations: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Sync locations for a specific city
     */
    suspend fun syncLocationsByCityIdFromFirebase(cityId: String): Result<Int> {
        return try {
            println("SYNC: Starting locations sync for city $cityId from Firebase...")
            
            val locationsSnapshot = firestore.collection("locations")
                .whereEqualTo("cityId", cityId)
                .get()
                .await()

            val cachedLocations = locationsSnapshot.documents.map { doc ->
                CachedLocation(
                    id = doc.id,
                    name = doc.getString("name") ?: "",
                    description = doc.getString("description") ?: "",
                    latitude = doc.getDouble("latitude") ?: 0.0,
                    longitude = doc.getDouble("longitude") ?: 0.0,
                    category = doc.getString("category") ?: "OTHER",
                    cityId = doc.getString("cityId") ?: "",
                    userId = doc.getString("userId") ?: "",
                    createdAt = (doc.getTimestamp("createdAt") ?: Timestamp.now()).seconds * 1000,
                    averageRating = doc.getDouble("averageRating") ?: 0.0,
                    totalRatings = (doc.getLong("totalRatings") ?: 0).toInt(),
                    imageUrl = doc.getString("imageUrl") ?: "",
                    lastSyncedAt = System.currentTimeMillis()
                )
            }

            locationDao.insertLocations(cachedLocations)
            println("SYNC: Successfully synced ${cachedLocations.size} locations for city $cityId")
            
            Result.success(cachedLocations.size)
        } catch (e: Exception) {
            println("SYNC: Error syncing locations for city: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun getCachedLocationsCount(): Int {
        return locationDao.getLocationsCount()
    }

    // ==================== REVIEWS ====================

    /**
     * Get reviews for a location from local cache
     */
    fun getReviewsByLocationIdFlow(locationId: String): Flow<List<Review>> {
        return reviewDao.getReviewsByLocationId(locationId).map { cachedReviews ->
            cachedReviews.map { it.toReview() }
        }
    }

    /**
     * Sync all reviews from Firebase to local cache
     */
    suspend fun syncReviewsFromFirebase(): Result<Int> {
        return try {
            println("SYNC: Starting reviews sync from Firebase...")
            
            val reviewsSnapshot = firestore.collection("reviews")
                .get()
                .await()

            val cachedReviews = reviewsSnapshot.documents.map { doc ->
                CachedReview(
                    id = doc.id,
                    locationId = doc.getString("locationId") ?: "",
                    userId = doc.getString("userId") ?: "",
                    userName = doc.getString("userName") ?: "Anoniem",
                    userEmail = doc.getString("userEmail") ?: "",
                    rating = (doc.getLong("rating") ?: 0).toInt(),
                    comment = doc.getString("comment") ?: "",
                    createdAt = (doc.getTimestamp("createdAt") ?: Timestamp.now()).seconds * 1000,
                    lastSyncedAt = System.currentTimeMillis()
                )
            }

            reviewDao.insertReviews(cachedReviews)
            println("SYNC: Successfully synced ${cachedReviews.size} reviews")
            
            Result.success(cachedReviews.size)
        } catch (e: Exception) {
            println("SYNC: Error syncing reviews: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Sync reviews for a specific location
     */
    suspend fun syncReviewsByLocationIdFromFirebase(locationId: String): Result<Int> {
        return try {
            println("SYNC: Starting reviews sync for location $locationId from Firebase...")
            
            val reviewsSnapshot = firestore.collection("reviews")
                .whereEqualTo("locationId", locationId)
                .get()
                .await()

            val cachedReviews = reviewsSnapshot.documents.map { doc ->
                CachedReview(
                    id = doc.id,
                    locationId = doc.getString("locationId") ?: "",
                    userId = doc.getString("userId") ?: "",
                    userName = doc.getString("userName") ?: "Anoniem",
                    userEmail = doc.getString("userEmail") ?: "",
                    rating = (doc.getLong("rating") ?: 0).toInt(),
                    comment = doc.getString("comment") ?: "",
                    createdAt = (doc.getTimestamp("createdAt") ?: Timestamp.now()).seconds * 1000,
                    lastSyncedAt = System.currentTimeMillis()
                )
            }

            reviewDao.insertReviews(cachedReviews)
            println("SYNC: Successfully synced ${cachedReviews.size} reviews for location $locationId")
            
            Result.success(cachedReviews.size)
        } catch (e: Exception) {
            println("SYNC: Error syncing reviews for location: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun getCachedReviewsCount(): Int {
        return reviewDao.getReviewsCount()
    }

    // ==================== FULL SYNC ====================

    /**
     * Sync all data from Firebase to local cache
     */
    suspend fun syncAllDataFromFirebase(): Result<SyncStats> {
        return try {
            println("SYNC: Starting full data sync from Firebase...")
            
            val citiesResult = syncCitiesFromFirebase()
            val locationsResult = syncLocationsFromFirebase()
            val reviewsResult = syncReviewsFromFirebase()

            val stats = SyncStats(
                citiesSynced = citiesResult.getOrDefault(0),
                locationsSynced = locationsResult.getOrDefault(0),
                reviewsSynced = reviewsResult.getOrDefault(0),
                timestamp = System.currentTimeMillis()
            )

            println("SYNC: Full sync completed - Cities: ${stats.citiesSynced}, Locations: ${stats.locationsSynced}, Reviews: ${stats.reviewsSynced}")
            
            Result.success(stats)
        } catch (e: Exception) {
            println("SYNC: Error in full sync: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    // ==================== CACHE STATUS ====================

    suspend fun getCacheStats(): CacheStats {
        return CacheStats(
            citiesCount = cityDao.getCitiesCount(),
            locationsCount = locationDao.getLocationsCount(),
            reviewsCount = reviewDao.getReviewsCount()
        )
    }

    suspend fun clearAllCache() {
        cityDao.deleteAllCities()
        locationDao.deleteAllLocations()
        reviewDao.deleteAllReviews()
    }
}

data class SyncStats(
    val citiesSynced: Int,
    val locationsSynced: Int,
    val reviewsSynced: Int,
    val timestamp: Long
)

data class CacheStats(
    val citiesCount: Int,
    val locationsCount: Int,
    val reviewsCount: Int
) {
    val totalItems: Int
        get() = citiesCount + locationsCount + reviewsCount

    val isEmpty: Boolean
        get() = totalItems == 0
}






