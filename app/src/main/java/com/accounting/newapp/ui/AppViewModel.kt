@file:OptIn(kotlinx.coroutines.FlowPreview::class)

package com.accounting.newapp.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.accounting.newapp.AccountingApplication
import com.accounting.newapp.data.AccountingRepository
import com.accounting.newapp.data.CategoryTotal
import com.accounting.newapp.data.MerchantTotal
import com.accounting.newapp.data.ThemeMode
import com.accounting.newapp.data.TransactionEntity
import com.accounting.newapp.report.ReportPeriod
import com.accounting.newapp.report.ReportRanges
import com.accounting.newapp.settings.UserPreferences
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val todayTotalCents: Long = 0,
    val recentTransactions: List<TransactionEntity> = emptyList(),
    val pendingTransactions: List<TransactionEntity> = emptyList(),
)

data class ReportUiState(
    val period: ReportPeriod = ReportPeriod.Month,
    val totalCents: Long = 0,
    val categories: List<CategoryTotal> = emptyList(),
    val merchants: List<MerchantTotal> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: AccountingRepository = (application as AccountingApplication).repository
    private val preferences = UserPreferences(application)
    private val selectedPeriod = MutableStateFlow(ReportPeriod.Month)

    val themeMode: StateFlow<ThemeMode> = preferences.themeMode.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        ThemeMode.System,
    )

    val transactions: StateFlow<List<TransactionEntity>> = repository.observeTransactions().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )

    val trashTransactions: StateFlow<List<TransactionEntity>> = repository.observeTrashTransactions().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )

    private val todayRange = ReportRanges.current(ReportPeriod.Day)

    val homeUiState: StateFlow<HomeUiState> = combine(
        repository.observeExpenseTotal(todayRange.startMillis, todayRange.endMillis)
            .catch { emit(0L) },
        transactions,
        repository.observePendingTransactions()
            .catch { emit(emptyList()) },
    ) { todayTotal, allTransactions, pending ->
        HomeUiState(
            todayTotalCents = todayTotal,
            recentTransactions = allTransactions.take(5),
            pendingTransactions = pending,
        )
    }.debounce(150).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    val reportUiState: StateFlow<ReportUiState> = selectedPeriod.flatMapLatest { period ->
        val range = ReportRanges.current(period)
        combine(
            repository.observeExpenseTotal(range.startMillis, range.endMillis)
                .catch { emit(0L) },
            repository.observeCategoryTotals(range.startMillis, range.endMillis)
                .catch { emit(emptyList()) },
            repository.observeMerchantTotals(range.startMillis, range.endMillis)
                .catch { emit(emptyList()) },
        ) { total, categories, merchants ->
            ReportUiState(period, total, categories, merchants)
        }
    }.catch { emit(ReportUiState()) }
        .debounce(150)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReportUiState())

    init {
        viewModelScope.launch { repository.purgeExpiredTrash() }
    }

    fun setPeriod(period: ReportPeriod) {
        selectedPeriod.value = period
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { preferences.setThemeMode(mode) }
    }

    fun updateTransaction(transaction: TransactionEntity) {
        viewModelScope.launch { repository.updateTransaction(transaction) }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch { repository.softDeleteTransaction(transaction) }
    }

    fun restoreTransaction(transaction: TransactionEntity) {
        viewModelScope.launch { repository.restoreTransaction(transaction) }
    }

    fun permanentlyDeleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch { repository.permanentlyDeleteTransaction(transaction) }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = AppViewModel(application) as T
    }
}