package com.example.prayasnow

import android.app.Application
import com.google.firebase.FirebaseApp

class PrayasNowApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
} 