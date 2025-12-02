package edu.ap.citytripapplication.navigation

import LocationProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import edu.ap.citytripapplication.MapScreen
import edu.ap.citytripapplication.ui.screens.AddCityScreen
import edu.ap.citytripapplication.ui.screens.AddLocationScreen
import edu.ap.citytripapplication.ui.screens.ChatListScreen
import edu.ap.citytripapplication.ui.screens.ChatScreen
import edu.ap.citytripapplication.ui.screens.CitiesListScreen
import edu.ap.citytripapplication.ui.screens.CityDetailsScreen
import edu.ap.citytripapplication.ui.screens.LoginScreen
import edu.ap.citytripapplication.ui.screens.NewChatScreen
import edu.ap.citytripapplication.ui.screens.RegisterScreen
import edu.ap.citytripapplication.ui.screens.EditProfileScreen
import edu.ap.citytripapplication.ui.screens.UserListScreen
import edu.ap.citytripapplication.viewmodel.AuthViewModel
import edu.ap.citytripapplication.viewmodel.LocationViewModel

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Register : Screen("register")
    data object CitiesList : Screen("cities")
    data object AddCity : Screen("addCity")
    data object CityDetails : Screen("city/{cityId}") {
        fun createRoute(cityId: String) = "city/$cityId"
    }
    data object AddLocation : Screen("addLocation/{cityId}/{latitude}/{longitude}") {
        fun createRoute(cityId: String, latitude: Double, longitude: Double) =
            "addLocation/$cityId/$latitude/$longitude"
    }
    data object Map : Screen("map")

    object EditProfile : Screen("edit_profile")

    object ChatList : Screen("chat_list")
    object UserList : Screen("user_list")
    object Chat : Screen("chat/{conversationId}/{receiverId}") {
        fun createRoute(conversationId: String, receiverId: String) =
            "chat/$conversationId/$receiverId" }
    object NewChat : Screen("new_chat/{receiverId}") {
        fun createRoute(receiverId: String) = "new_chat/$receiverId"
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()
    val authState by authViewModel.authState.collectAsState()

    val startDestination = if (authState.isAuthenticated) {
        Screen.CitiesList.route
    } else {
        Screen.Login.route
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                viewModel = authViewModel,
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                },
                onLoginSuccess = {
                    navController.navigate(Screen.CitiesList.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                viewModel = authViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onRegisterSuccess = {
                    navController.navigate(Screen.CitiesList.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.CitiesList.route) {
            CitiesListScreen(
                onNavigateBack = {
                    authViewModel.signOut()
                },
                onNavigateToCityDetails = { cityId ->
                    navController.navigate(Screen.CityDetails.createRoute(cityId))
                },
                onAddCity = {
                    navController.navigate(Screen.AddCity.route)
                },
                onNavigateToMap = {
                    navController.navigate(Screen.Map.route)
                },
                onNavigateToChats = {
                    navController.navigate(Screen.ChatList.route)
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.EditProfile.route)
                }
            )
        }

        composable(Screen.AddCity.route) {
            AddCityScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.CityDetails.route) { backStackEntry ->
            val cityId = backStackEntry.arguments?.getString("cityId") ?: ""
            CityDetailsScreen(
                cityId = cityId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onAddLocation = { cityIdForLocation ->
                    // Get current location from service
                    val locationService = LocationProvider.getService()
                    val currentLocation = locationService.getCurrentLocation()

                    navController.navigate(
                        Screen.AddLocation.createRoute(
                            cityId = cityIdForLocation,
                            latitude = currentLocation?.latitude ?: 51.2194,
                            longitude = currentLocation?.longitude ?: 4.4025
                        )
                    )
                }
            )
        }

        composable(
            route = Screen.AddLocation.route,
            arguments = listOf(
                navArgument("cityId") { type = NavType.StringType },
                navArgument("latitude") { type = NavType.StringType },
                navArgument("longitude") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val cityId = backStackEntry.arguments?.getString("cityId") ?: ""
            val latitude = backStackEntry.arguments?.getString("latitude")?.toDoubleOrNull() ?: 0.0
            val longitude = backStackEntry.arguments?.getString("longitude")?.toDoubleOrNull() ?: 0.0

            val locationViewModel: LocationViewModel = viewModel()

            AddLocationScreen(
                viewModel = locationViewModel,
                cityId = cityId,
                currentLatitude = latitude,
                currentLongitude = longitude,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Map.route) {
            MapScreen(
                navController = navController,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.EditProfile.route) {
            EditProfileScreen(navController = navController)
        }

        composable(Screen.ChatList.route) {
            ChatListScreen(navController)
        }

        composable(Screen.UserList.route) {
            UserListScreen(navController)
        }

        composable(
            route = Screen.Chat.route,
            arguments = listOf(
                navArgument("conversationId") { type = NavType.StringType },
                navArgument("receiverId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val conversationId = backStackEntry.arguments?.getString("conversationId") ?: ""
            val receiverId = backStackEntry.arguments?.getString("receiverId") ?: ""
            ChatScreen(navController, conversationId, receiverId)
        }

        composable(
            route = Screen.NewChat.route,
            arguments = listOf(
                navArgument("receiverId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val receiverId = backStackEntry.arguments?.getString("receiverId") ?: ""
            NewChatScreen(navController, receiverId)
        }
    }
}