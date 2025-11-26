package edu.ap.citytripapplication.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import edu.ap.citytripapplication.database.dao.CityDao
import edu.ap.citytripapplication.database.dao.LocationDao
import edu.ap.citytripapplication.database.dao.ReviewDao
import edu.ap.citytripapplication.database.entity.CachedCity
import edu.ap.citytripapplication.database.entity.CachedLocation
import edu.ap.citytripapplication.database.entity.CachedReview

@Database(
    entities = [
        CachedCity::class,
        CachedLocation::class,
        CachedReview::class
    ],
    version = 1,
    exportSchema = false
)
abstract class CityTripDatabase : RoomDatabase() {
    
    abstract fun cityDao(): CityDao
    abstract fun locationDao(): LocationDao
    abstract fun reviewDao(): ReviewDao

    companion object {
        @Volatile
        private var INSTANCE: CityTripDatabase? = null

        fun getDatabase(context: Context): CityTripDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CityTripDatabase::class.java,
                    "city_trip_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        fun clearInstance() {
            INSTANCE = null
        }
    }
}