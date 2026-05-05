package com.travellapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.travellapp.ui.screen.*
import com.travellapp.ui.viewmodel.AuthViewModel
import com.travellapp.ui.viewmodel.TripViewModel

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object TripList : Screen("trip_list")
    object TripSetup : Screen("trip_setup?tripId={tripId}") {
        fun createRoute(tripId: Int? = null) =
            if (tripId != null) "trip_setup?tripId=$tripId" else "trip_setup"
    }
    object Attractions : Screen("attractions/{tripId}") {
        fun createRoute(tripId: Int) = "attractions/$tripId"
    }
    object AddAttraction : Screen("add_attraction/{tripId}?attractionId={attractionId}") {
        fun createRoute(tripId: Int, attractionId: Int? = null) =
            if (attractionId != null) "add_attraction/$tripId?attractionId=$attractionId"
            else "add_attraction/$tripId"
    }
    object Itinerary : Screen("itinerary/{tripId}") {
        fun createRoute(tripId: Int) = "itinerary/$tripId"
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()
    val tripViewModel: TripViewModel = hiltViewModel()
    val isSignedIn by authViewModel.isSignedIn.collectAsState()

    NavHost(
        navController = navController,
        startDestination = if (isSignedIn) Screen.TripList.route else Screen.Login.route
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                authViewModel = authViewModel,
                onLoginSuccess = {
                    navController.navigate(Screen.TripList.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.TripList.route) {
            TripListScreen(
                tripViewModel = tripViewModel,
                authViewModel = authViewModel,
                onNewTrip = { navController.navigate(Screen.TripSetup.createRoute()) },
                onEditTrip = { tripId -> navController.navigate(Screen.TripSetup.createRoute(tripId)) },
                onOpenAttractions = { tripId -> navController.navigate(Screen.Attractions.createRoute(tripId)) },
                onSignOut = {
                    authViewModel.signOut()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.TripSetup.route,
            arguments = listOf(navArgument("tripId") {
                type = NavType.StringType; nullable = true; defaultValue = null
            })
        ) { backStack ->
            val tripIdStr = backStack.arguments?.getString("tripId")
            TripSetupScreen(
                tripViewModel = tripViewModel,
                authViewModel = authViewModel,
                editTripId = tripIdStr?.toIntOrNull(),
                onSaved = { tripId ->
                    navController.navigate(Screen.Attractions.createRoute(tripId)) {
                        popUpTo(Screen.TripList.route)
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Attractions.route,
            arguments = listOf(navArgument("tripId") { type = NavType.IntType })
        ) { backStack ->
            val tripId = backStack.arguments!!.getInt("tripId")
            AttractionsScreen(
                tripId = tripId,
                tripViewModel = tripViewModel,
                onAddAttraction = { navController.navigate(Screen.AddAttraction.createRoute(tripId)) },
                onEditAttraction = { attractionId ->
                    navController.navigate(Screen.AddAttraction.createRoute(tripId, attractionId))
                },
                onGenerateItinerary = {
                    tripViewModel.generateItinerary()
                    navController.navigate(Screen.Itinerary.createRoute(tripId))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.AddAttraction.route,
            arguments = listOf(
                navArgument("tripId") { type = NavType.IntType },
                navArgument("attractionId") {
                    type = NavType.StringType; nullable = true; defaultValue = null
                }
            )
        ) { backStack ->
            val tripId = backStack.arguments!!.getInt("tripId")
            val attractionIdStr = backStack.arguments?.getString("attractionId")
            AddAttractionScreen(
                tripId = tripId,
                editAttractionId = attractionIdStr?.toIntOrNull(),
                tripViewModel = tripViewModel,
                onSaved = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Itinerary.route,
            arguments = listOf(navArgument("tripId") { type = NavType.IntType })
        ) {
            ItineraryScreen(
                tripViewModel = tripViewModel,
                onBack = { navController.popBackStack() },
                onEditAttractions = { tripId ->
                    navController.navigate(Screen.Attractions.createRoute(tripId)) {
                        popUpTo(Screen.Attractions.createRoute(tripId)) { inclusive = true }
                    }
                }
            )
        }
    }
}
