package edu.ap.citytripapplication.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import edu.ap.citytripapplication.model.City
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class CitiesViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _cities = MutableStateFlow<List<City>>(emptyList())
    val cities: StateFlow<List<City>> = _cities.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadCities() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val citiesList = db.collection("cities")
                    .get()
                    .await()
                    .documents
                    .map { doc ->
                        City(
                            id = doc.id,
                            name = doc.getString("name") ?: "",
                            country = doc.getString("country") ?: "",
                            description = doc.getString("description") ?: "",
                            latitude = doc.getDouble("latitude") ?: 0.0,
                            longitude = doc.getDouble("longitude") ?: 0.0,
                            imageUrl = doc.getString("imageUrl") ?: "",
                            createdBy = doc.getString("createdBy") ?: "",
                            createdAt = doc.getTimestamp("createdAt") ?: Timestamp.now()
                        )
                    }
                _cities.value = citiesList
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addCity(
        name: String,
        country: String,
        description: String,
        latitude: Double,
        longitude: Double,
        onSuccess: () -> Unit
    ) {
        val currentUser = auth.currentUser
        if (currentUser == null) return

        viewModelScope.launch {
            try {
                val cityId = UUID.randomUUID().toString()
                val city = City(
                    id = cityId,
                    name = name,
                    country = country,
                    description = description,
                    latitude = latitude,
                    longitude = longitude,
                    createdBy = currentUser.uid,
                    createdAt = Timestamp.now()
                )

                db.collection("cities")
                    .document(cityId)
                    .set(city)
                    .await()

                loadCities() // Refresh the list
                onSuccess()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}