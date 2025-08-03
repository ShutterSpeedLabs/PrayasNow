package com.example.prayasnow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.prayasnow.di.AppModule
import com.example.prayasnow.ui.navigation.AppNavigation
import com.example.prayasnow.ui.theme.PrayasNowTheme
import com.example.prayasnow.repository.QuizRepository
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Initialize dependencies
        val auth = AppModule.provideFirebaseAuth()
        val firestore = AppModule.provideFirebaseFirestore()
        val database = AppModule.provideAppDatabase(this)
        val repository = AppModule.provideAuthRepository(auth, firestore, database)
        val authViewModel = AppModule.provideAuthViewModel(repository)
        val quizRepository = QuizRepository(firestore, database)
        
        setContent {
            PrayasNowTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(
                        authViewModel = authViewModel,
                        quizRepository = quizRepository,
                        firestore = firestore
                    )
                }
            }
        }
    }
}