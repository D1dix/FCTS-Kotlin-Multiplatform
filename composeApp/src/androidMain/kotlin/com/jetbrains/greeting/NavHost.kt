package com.jetbrains.greeting

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.jetbrains.greeting.viewmodels.UserViewModel


@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController(),
    startDestination: String = "home",
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("home") {
            HomeScreen(
                onNavigateToMenu = { navController.navigate("menu") },
                onNavigateToLogin = { navController.navigate("login") },
                onNavigateToProfile = { navController.navigate("profile") }
            )
        }

        composable(
            route = "menu",
            deepLinks = listOf(
                navDeepLink {
                    uriPattern = "app://menu"
                },
                navDeepLink {
                    uriPattern = "https://restaurant.com/menu"
                }
            )
        ) {
            MenuScreen(
                categoryId = "all",
                onNavigateToDetail = { itemId ->
                    navController.navigate("detail/$itemId")
                },
                onNavigateToCart = {
                    navController.navigate("cart")
                },
                onNavigateBack = {
                    navController.navigate("home")
                }
            )
        }


        composable(
            route = "detail/{itemId}",
            arguments = listOf(
                navArgument("itemId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val itemIdString = backStackEntry.arguments?.getString("itemId")
            val itemId = itemIdString?.toLongOrNull() ?: -1L
            DetailScreen(
                itemId = itemId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToCart = {
                    navController.navigate("cart")
                }
            )
        }

        composable(
            route = "cart",
            deepLinks = listOf(
                navDeepLink {
                    uriPattern = "app://cart"
                },
                navDeepLink {
                    uriPattern = "https://restaurant.com/cart"
                }
            )
        ) {
            CartScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = "login",
            deepLinks = listOf(
                navDeepLink {
                    uriPattern = "app://login"
                },
                navDeepLink {
                    uriPattern = "https://restaurant.com/login"
                }
            )
        ) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("home")
                },
                onNavigateToRegister = { navController.navigate("register") }
            )
        }

        composable("register") {
            RegisterScreen(
                onRegisterSuccess = { 
                    navController.navigate("home")
                },
                onNavigateToLogin = { navController.navigate("login") }
            )
        }

        composable("profile") {
            ProfileScreen(
                onNavigateToLogin = { navController.navigate("login") },
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}




