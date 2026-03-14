package com.example.spotcast.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.spotcast.ui.screen.CapsuleListScreen
import com.example.spotcast.ui.screen.FriendsScreen
import com.example.spotcast.ui.screen.LoginScreen
import com.example.spotcast.ui.screen.MapScreen
import com.example.spotcast.ui.screen.SettingsScreen

object Routes {
    const val LOGIN = "login"
    const val MAP = "map"
    const val SETTINGS = "settings"
    const val CAPSULE_LIST = "capsule_list"
    const val FRIENDS = "friends"
}

@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String,
) {
    NavHost(navController = navController, startDestination = startDestination) {

        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.MAP) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.MAP) {
            MapScreen(
                onLogout = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.MAP) { inclusive = true }
                    }
                },
                onSettings = {
                    navController.navigate(Routes.SETTINGS)
                },
                onCapsuleList = {
                    navController.navigate(Routes.CAPSULE_LIST)
                },
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onFriends = { navController.navigate(Routes.FRIENDS) },
                onLogout = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.CAPSULE_LIST) {
            CapsuleListScreen(
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.FRIENDS) {
            FriendsScreen(
                onBack = { navController.popBackStack() },
            )
        }
    }
}
