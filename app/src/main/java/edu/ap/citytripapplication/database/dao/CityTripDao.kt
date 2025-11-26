package edu.ap.citytripapplication.database.dao

import androidx.room.*
import edu.ap.citytripapplication.database.entity.CachedCity
import edu.ap.citytripapplication.database.entity.CachedLocation
import edu.ap.citytripapplication.database.entity.CachedReview
import kotlinx.coroutines.flow.Flow

@Dao
interface CityDao {
    @Query("SELECT * FROM cached_cities ORDER BY name ASC")
    fun getAllCities(): Flow<List<CachedCity>>

    @Query("SELECT * FROM cached_cities WHERE id = :cityId")
    suspend fun getCityById(cityId: String): CachedCity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCity(city: CachedCity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCities(cities: List<CachedCity>)

    @Delete
    suspend fun deleteCity(city: CachedCity)

    @Query("DELETE FROM cached_cities")
    suspend fun deleteAllCities()

    @Query("SELECT COUNT(*) FROM cached_cities")
    suspend fun getCitiesCount(): Int
}

@Dao
interface LocationDao {
    @Query("SELECT * FROM cached_locations ORDER BY name ASC")
    fun getAllLocations(): Flow<List<CachedLocation>>

    @Query("SELECT * FROM cached_locations WHERE cityId = :cityId ORDER BY name ASC")
    fun getLocationsByCityId(cityId: String): Flow<List<CachedLocation>>

    @Query("SELECT * FROM cached_locations WHERE id = :locationId")
    suspend fun getLocationById(locationId: String): CachedLocation?

    @Query("SELECT * FROM cached_locations WHERE category = :category")
    fun getLocationsByCategory(category: String): Flow<List<CachedLocation>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocation(location: CachedLocation)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocations(locations: List<CachedLocation>)

    @Delete
    suspend fun deleteLocation(location: CachedLocation)

    @Query("DELETE FROM cached_locations WHERE cityId = :cityId")
    suspend fun deleteLocationsByCityId(cityId: String)

    @Query("DELETE FROM cached_locations")
    suspend fun deleteAllLocations()

    @Query("SELECT COUNT(*) FROM cached_locations")
    suspend fun getLocationsCount(): Int

    @Query("SELECT COUNT(*) FROM cached_locations WHERE cityId = :cityId")
    suspend fun getLocationCountByCityId(cityId: String): Int
}

@Dao
interface ReviewDao {
    @Query("SELECT * FROM cached_reviews WHERE locationId = :locationId ORDER BY createdAt DESC")
    fun getReviewsByLocationId(locationId: String): Flow<List<CachedReview>>

    @Query("SELECT * FROM cached_reviews WHERE id = :reviewId")
    suspend fun getReviewById(reviewId: String): CachedReview?

    @Query("SELECT * FROM cached_reviews WHERE userId = :userId")
    fun getReviewsByUserId(userId: String): Flow<List<CachedReview>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReview(review: CachedReview)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReviews(reviews: List<CachedReview>)

    @Delete
    suspend fun deleteReview(review: CachedReview)

    @Query("DELETE FROM cached_reviews WHERE locationId = :locationId")
    suspend fun deleteReviewsByLocationId(locationId: String)

    @Query("DELETE FROM cached_reviews")
    suspend fun deleteAllReviews()

    @Query("SELECT COUNT(*) FROM cached_reviews")
    suspend fun getReviewsCount(): Int

    @Query("SELECT COUNT(*) FROM cached_reviews WHERE locationId = :locationId")
    suspend fun getReviewCountByLocationId(locationId: String): Int
}






