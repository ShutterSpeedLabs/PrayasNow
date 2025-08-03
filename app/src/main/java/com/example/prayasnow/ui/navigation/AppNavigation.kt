package com.example.prayasnow.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.prayasnow.ui.screens.HomeScreen
import com.example.prayasnow.ui.screens.LoginScreen
import com.example.prayasnow.ui.screens.UserProfileScreen
import com.example.prayasnow.viewmodel.AuthViewModel
import com.example.prayasnow.repository.QuizRepository
import com.google.firebase.firestore.FirebaseFirestore

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Home : Screen("home")
    object Profile : Screen("profile")
}

@Composable
fun AppNavigation(
    authViewModel: AuthViewModel,
    quizRepository: QuizRepository,
    firestore: FirebaseFirestore
) {
    val navController = rememberNavController()
    val authState by authViewModel.authState.collectAsState()
    
    NavHost(
        navController = navController,
        startDestination = if (authState.isLoggedIn) Screen.Profile.route else Screen.Login.route
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                authViewModel = authViewModel,
                onNavigateToHome = {
                    navController.navigate(Screen.Profile.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                quizRepository = quizRepository,
                firestore = firestore
            )
        }
        
        composable(Screen.Profile.route) {
            UserProfileScreen(
                authViewModel = authViewModel,
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Profile.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.Home.route) {
            HomeScreen(
                authViewModel = authViewModel,
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }
    }
} 