package com.example.prayasnow.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.prayasnow.ui.screens.HomeScreen
import com.example.prayasnow.ui.screens.LoginScreen
import com.example.prayasnow.ui.screens.UserProfileScreen
import com.example.prayasnow.ui.screens.QuizScreen
import com.example.prayasnow.ui.screens.SubjectQuizListScreen
import com.example.prayasnow.ui.screens.QuizTakingScreen
import com.example.prayasnow.viewmodel.AuthViewModel
import com.example.prayasnow.repository.QuizRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.example.prayasnow.repository.ProgressRepository

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Home : Screen("home")
    object Profile : Screen("profile")
    object Quiz : Screen("quiz")
    object SubjectQuizList : Screen("subject_quiz_list/{subject}")
    object QuizTaking : Screen("quiz_taking/{subject}/{quizTitle}")
}

@Composable
fun AppNavigation(
    authViewModel: AuthViewModel,
    quizRepository: QuizRepository,
    progressRepository: ProgressRepository,
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
                quizRepository = quizRepository,
                progressRepository = progressRepository,
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Profile.route) { inclusive = true }
                    }
                },
                onNavigateToQuiz = {
                    navController.navigate(Screen.Quiz.route)
                }
            )
        }
        
        composable(Screen.Quiz.route) {
            QuizScreen(
                authViewModel = authViewModel,
                quizRepository = quizRepository,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToSubject = { subject ->
                    navController.navigate("subject_quiz_list/$subject")
                }
            )
        }
        
        composable(
            route = Screen.SubjectQuizList.route,
            arguments = listOf(
                navArgument("subject") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val subject = backStackEntry.arguments?.getString("subject") ?: "Science"
            SubjectQuizListScreen(
                subject = subject,
                authViewModel = authViewModel,
                quizRepository = quizRepository,
                progressRepository = progressRepository,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToQuizTaking = { subjectName, quizTitle ->
                    navController.navigate("quiz_taking/$subjectName/$quizTitle")
                }
            )
        }
        
        composable(
            route = Screen.QuizTaking.route,
            arguments = listOf(
                navArgument("subject") {
                    type = NavType.StringType
                },
                navArgument("quizTitle") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val subject = backStackEntry.arguments?.getString("subject") ?: "Science"
            val quizTitle = backStackEntry.arguments?.getString("quizTitle") ?: "Physics Basics"
            QuizTakingScreen(
                subject = subject,
                quizTitle = quizTitle,
                authViewModel = authViewModel,
                quizRepository = quizRepository,
                progressRepository = progressRepository,
                onNavigateBack = {
                    navController.popBackStack()
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