package com.example

import android.app.Application
import com.example.data.database.AppDatabase
import com.example.data.repository.PaymentVerificationRepository
import com.example.data.repository.StudyNotesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class StudyNotesApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { StudyNotesRepository(database.appDao()) }
    val paymentVerificationRepository by lazy { PaymentVerificationRepository(database.appDao()) }

    override fun onCreate() {
        super.onCreate()
        // Pre-populate sample notes, categories, subjects, etc. on startup
        applicationScope.launch {
            repository.populateSampleDataIfNeeded()
        }
    }
}
