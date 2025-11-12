package edu.ap.citytripapplication.navigation

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
import edu.ap.citytripapplication.ui.screens.AddLocationScreen
import edu.ap.citytripapplication.ui.screens.LoginScreen
import edu.ap.citytripapplication.ui.screens.RegisterScreen
import edu.ap.citytripapplication.viewmodel.AuthViewModel
import edu.ap.citytripapplication.viewmodel.LocationViewModel

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object Map : Screen("map")
    object AddLocation : Screen("add_location/{cityId}/{latitude}/{longitude}") {
        fun createRoute(cityId: String, latitude: Double, longitude: Double) = 
            "add_location/$cityId/$latitude/$longitude"
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()
    val locationViewModel: LocationViewModel = viewModel()
    val authState by authViewModel.authState.collectAsState()

    // Determine start destination based on auth state
    val startDestination = if (authState.isAuthenticated) {
        Screen.Map.route
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
                    navController.navigate(Screen.Map.route) {
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
                    navController.navigate(Screen.Map.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Map.route) {
            MapScreen(
                navController = navController,
                authViewModel = authViewModel
            )
        }

        composable(
            route = Screen.AddLocation.route,
            arguments = listOf(
                navArgument("cityId") { type = NavType.StringType },
                navArgument("latitude") { type = NavType.FloatType },
                navArgument("longitude") { type = NavType.FloatType }
            )
        ) { backStackEntry ->
            val cityId = backStackEntry.arguments?.getString("cityId") ?: ""
            val latitude = backStackEntry.arguments?.getFloat("latitude")?.toDouble() ?: 0.0
            val longitude = backStackEntry.arguments?.getFloat("longitude")?.toDouble() ?: 0.0

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
    }
}