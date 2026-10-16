package com.mototrip.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mototrip.MotoTripApp
import com.mototrip.ui.active_trip.ActiveTripScreen
import com.mototrip.ui.active_trip.ActiveTripViewModel
import com.mototrip.ui.history.HistoryScreen
import com.mototrip.ui.history.HistoryViewModel
import com.mototrip.ui.home.HomeScreen
import com.mototrip.ui.home.HomeViewModel
import com.mototrip.ui.trip_detail.TripDetailScreen
import com.mototrip.ui.trip_detail.TripDetailViewModel

object Destinations {
    const val HOME_ROUTE = "home"
    const val ACTIVE_TRIP_ROUTE = "active_trip/{tripId}"
    const val HISTORY_ROUTE = "history"
    const val TRIP_DETAIL_ROUTE = "trip_detail/{tripId}"

    fun createActiveTripRoute(tripId: String) = "active_trip/$tripId"
    fun createTripDetailRoute(tripId: String) = "trip_detail/$tripId"
}

@Composable
fun MotoTripNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = Destinations.HOME_ROUTE
) {
    val context = LocalContext.current
    val appContainer = (context.applicationContext as MotoTripApp).appContainer

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Destinations.HOME_ROUTE) {
            val homeViewModel: HomeViewModel = viewModel(
                factory = HomeViewModel.provideFactory(
                    appContainer.getActiveTripUseCase,
                    appContainer.startTripUseCase
                )
            )
            HomeScreen(
                viewModel = homeViewModel,
                onNavigateToActiveTrip = { tripId ->
                    navController.navigate(Destinations.createActiveTripRoute(tripId))
                },
                onNavigateToHistory = {
                    navController.navigate(Destinations.HISTORY_ROUTE)
                }
            )
        }

        composable(
            route = Destinations.ACTIVE_TRIP_ROUTE,
            arguments = listOf(navArgument("tripId") { type = NavType.StringType })
        ) { backStackEntry ->
            val tripId = backStackEntry.arguments?.getString("tripId").orEmpty()
            val activeViewModel: ActiveTripViewModel = viewModel(
                factory = ActiveTripViewModel.provideFactory(
                    tripId = tripId,
                    getTripDetailUseCase = appContainer.getTripDetailUseCase,
                    pauseTripUseCase = appContainer.pauseTripUseCase,
                    resumeTripUseCase = appContainer.resumeTripUseCase,
                    finishTripUseCase = appContainer.finishTripUseCase,
                    addStopUseCase = appContainer.addStopUseCase,
                    addFuelEntryUseCase = appContainer.addFuelEntryUseCase,
                    addExpenseUseCase = appContainer.addExpenseUseCase,
                    tripRepository = appContainer.tripRepository
                )
            )
            ActiveTripScreen(
                viewModel = activeViewModel,
                onNavigateBack = { navController.popBackStack() },
                onTripFinished = {
                    navController.navigate(Destinations.createTripDetailRoute(tripId)) {
                        popUpTo(Destinations.HOME_ROUTE)
                    }
                }
            )
        }

        composable(Destinations.HISTORY_ROUTE) {
            val historyViewModel: HistoryViewModel = viewModel(
                factory = HistoryViewModel.provideFactory(
                    appContainer.getAllTripsUseCase,
                    appContainer.deleteTripUseCase
                )
            )
            HistoryScreen(
                viewModel = historyViewModel,
                onNavigateBack = { navController.popBackStack() },
                onTripSelected = { tripId ->
                    navController.navigate(Destinations.createTripDetailRoute(tripId))
                }
            )
        }

        composable(
            route = Destinations.TRIP_DETAIL_ROUTE,
            arguments = listOf(navArgument("tripId") { type = NavType.StringType })
        ) { backStackEntry ->
            val tripId = backStackEntry.arguments?.getString("tripId").orEmpty()
            val detailViewModel: TripDetailViewModel = viewModel(
                factory = TripDetailViewModel.provideFactory(
                    tripId = tripId,
                    tripRepository = appContainer.tripRepository
                )
            )
            TripDetailScreen(
                viewModel = detailViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
