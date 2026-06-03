package com.accounting.newapp.data

import com.accounting.newapp.capture.Categorizer
import com.accounting.newapp.capture.PaymentCapture
import kotlinx.coroutines.flow.Flow

class AccountingRepository(private val database: AccountingDatabase) {
    private val dao = database.dao()

    fun observeTransactions(): Flow<List<TransactionEntity>> = dao.observeTransactions()
    fun observeTransactionsBetween(startMillis: Long, endMillis: Long): Flow<List<TransactionEntity>> =
        dao.observeTransactionsBetween(startMillis, endMillis)
    fun observePendingTransactions(): Flow<List<TransactionEntity>> = dao.observePendingTransactions()
    fun observeExpenseTotal(startMillis: Long, endMillis: Long): Flow<Long> =
        dao.observeExpenseTotal(startMillis, endMillis)
    fun observeCategoryTotals(startMillis: Long, endMillis: Long): Flow<List<CategoryTotal>> =
        dao.observeCategoryTotals(startMillis, endMillis)
    fun observeMerchantTotals(startMillis: Long, endMillis: Long): Flow<List<MerchantTotal>> =
        dao.observeMerchantTotals(startMillis, endMillis)
    fun observeCategories(): Flow<List<CategoryEntity>> = dao.observeCategories()
    fun observeTrashTransactions(): Flow<List<TransactionEntity>> = dao.observeTrashTransactions()

    suspend fun seedDefaultsIfNeeded() {
        if (dao.categoryCount() == 0) {
            dao.insertCategories(AccountingDatabase.defaultCategories)
        }
    }

    suspend fun addCapture(capture: PaymentCapture): Long {
        val rules = dao.getRules()
        val category = Categorizer.categoryFor(capture.merchant, capture.rawText, rules)
        val status = if (capture.confidence >= 0.78f) ConfirmationStatus.Confirmed else ConfirmationStatus.Pending
        return dao.insertTransaction(
            TransactionEntity(
                amountCents = capture.amountCents,
                merchant = capture.merchant.ifBlank { "未知商户" },
                category = category,
                sourceApp = capture.sourceApp,
                paymentMethod = capture.paymentMethod,
                occurredAtMillis = capture.occurredAtMillis,
                note = capture.rawText.take(500),
                confidence = capture.confidence,
                status = status,
            )
        )
    }

    suspend fun updateTransaction(transaction: TransactionEntity, learnRule: Boolean = true) {
        dao.updateTransaction(transaction)
        if (learnRule && transaction.merchant.isNotBlank()) {
            dao.insertRule(
                MerchantRuleEntity(
                    pattern = transaction.merchant,
                    category = transaction.category,
                    sourceApp = transaction.sourceApp,
                )
            )
        }
    }

    suspend fun softDeleteTransaction(transaction: TransactionEntity) {
        dao.updateTransaction(transaction.copy(deletedAtMillis = System.currentTimeMillis()))
    }

    suspend fun restoreTransaction(transaction: TransactionEntity) {
        dao.updateTransaction(transaction.copy(deletedAtMillis = 0))
    }

    suspend fun permanentlyDeleteTransaction(transaction: TransactionEntity) {
        dao.permanentlyDeleteTransaction(transaction.id)
    }

    suspend fun purgeExpiredTrash() {
        val cutoff = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
        dao.purgeExpiredTrash(cutoff)
    }
}