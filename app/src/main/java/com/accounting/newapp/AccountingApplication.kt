package com.accounting.newapp

import android.app.Application
import com.accounting.newapp.data.AccountingDatabase
import com.accounting.newapp.data.AccountingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AccountingApplication : Application() {
    val database by lazy { AccountingDatabase.create(this) }
    val repository by lazy { AccountingRepository(database) }
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            repository.seedDefaultsIfNeeded()
        }
    }
}
