package com.example.prayasnow

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.example.prayasnow.di.AppModule
import com.example.prayasnow.repository.QuizRepository
import com.example.prayasnow.ui.navigation.AppNavigation
import com.example.prayasnow.ui.theme.PrayasNowTheme
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MainActivity", "onCreate started")
        
        try {
            enableEdgeToEdge()
            Log.d("MainActivity", "Edge to edge enabled")
            
            // Initialize all dependencies
            val auth = AppModule.provideFirebaseAuth()
            val firestore = AppModule.provideFirebaseFirestore()
            val database = AppModule.provideAppDatabase(this)
            val repository = AppModule.provideAuthRepository(auth, firestore, database)
            val authViewModel = AppModule.provideAuthViewModel(repository)
            val quizRepository = QuizRepository(firestore, database)
            val progressRepository = AppModule.provideProgressRepository(database, firestore, this)
            
            Log.d("MainActivity", "All dependencies initialized successfully")
            
            // Start background sync for unsynced progress
            lifecycleScope.launch {
                try {
                    progressRepository.syncAllUnsyncedProgress()
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error syncing unsynced progress: ${e.message}")
                }
            }
            
            setContent {
                PrayasNowTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        AppNavigation(
                            authViewModel = authViewModel,
                            quizRepository = quizRepository,
                            progressRepository = progressRepository,
                            firestore = firestore
                        )
                    }
                }
            }
            
        } catch (e: Exception) {
            Log.e("MainActivity", "Critical error in onCreate: ${e.message}", e)
            // Show a simple error screen
            setContent {
                PrayasNowTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        androidx.compose.material3.Text(
                            text = "App initialization failed: ${e.message}\n\nPlease check logcat for details.",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}