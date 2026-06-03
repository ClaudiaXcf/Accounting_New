package com.accounting.newapp.data

import com.accounting.newapp.capture.Categorizer
import com.accounting.newapp.capture.PaymentCapture
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged

class AccountingRepository(private val database: AccountingDatabase) {
    private val dao = database.dao()

    fun observeTransactions(): Flow<List<TransactionEntity>> = dao.observeTransactions().distinctUntilChanged()
    fun observeTransactionsBetween(startMillis: Long, endMillis: Long): Flow<List<TransactionEntity>> =
        dao.observeTransactionsBetween(startMillis, endMillis).distinctUntilChanged()
    fun observePendingTransactions(): Flow<List<TransactionEntity>> = dao.observePendingTransactions().distinctUntilChanged()
    fun observeExpenseTotal(startMillis: Long, endMillis: Long): Flow<Long> =
        dao.observeExpenseTotal(startMillis, endMillis).distinctUntilChanged()
    fun observeCategoryTotals(startMillis: Long, endMillis: Long): Flow<List<CategoryTotal>> =
        dao.observeCategoryTotals(startMillis, endMillis).distinctUntilChanged()
    fun observeMerchantTotals(startMillis: Long, endMillis: Long): Flow<List<MerchantTotal>> =
        dao.observeMerchantTotals(startMillis, endMillis).distinctUntilChanged()
    fun observeCategories(): Flow<List<CategoryEntity>> = dao.observeCategories().distinctUntilChanged()
    fun observeTrashTransactions(): Flow<List<TransactionEntity>> = dao.observeTrashTransactions().distinctUntilChanged()

    suspend fun seedDefaultsIfNeeded() {
        if (dao.categoryCount() == 0) {
            dao.insertCategories(AccountingDatabase.defaultCategories)
        }
    }

    suspend fun addCapture(capture: PaymentCapture): Long {
        // 60秒窗口内去重：防止同一笔支付被多个事件源重复插入
        val windowMs = 60_000L
        val count = dao.countSimilarTransactions(
            capture.sourceApp,
            capture.amountCents,
            capture.occurredAtMillis - windowMs,
            capture.occurredAtMillis + windowMs,
        )
        if (count > 0) return -1L

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