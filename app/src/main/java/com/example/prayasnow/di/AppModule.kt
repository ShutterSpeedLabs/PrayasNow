package com.example.prayasnow.di

import android.content.Context
import com.example.prayasnow.data.AppDatabase
import com.example.prayasnow.repository.AuthRepository
import com.example.prayasnow.viewmodel.AuthViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object AppModule {
    
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }
    
    fun provideFirebaseFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }
    
    fun provideAppDatabase(context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }
    
    fun provideAuthRepository(
        auth: FirebaseAuth,
        firestore: FirebaseFirestore,
        database: AppDatabase
    ): AuthRepository {
        return AuthRepository(auth, firestore, database)
    }
    
    fun provideAuthViewModel(repository: AuthRepository): AuthViewModel {
        return AuthViewModel(repository)
    }
} 