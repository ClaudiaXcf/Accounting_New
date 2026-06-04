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
        val windowMs = 60_000L
        val windowStart = capture.occurredAtMillis - windowMs
        val windowEnd = capture.occurredAtMillis + windowMs

        // 1. 同源去重：同一 app 的同一笔交易
        val sameSourceCount = dao.countSimilarTransactions(
            capture.sourceApp,
            capture.amountCents,
            windowStart,
            windowEnd,
        )
        if (sameSourceCount > 0) return -1L

        // 2. 跨源去重：不同 app 捕获的同一笔交易（如微信通知 + 银行短信）
        //    找到后在同一条记录上合并来源（sourceApp 变为 "微信支付,招商银行"）
        val existing = dao.findSimilarTransaction(capture.amountCents, windowStart, windowEnd)
        if (existing != null) {
            if (!existing.sourceApp.contains(capture.sourceApp)) {
                val mergedSourceApp = "${existing.sourceApp},${capture.sourceApp}"
                val mergedNote = if (existing.note.isNotBlank() && capture.rawText.isNotBlank()) {
                    "${existing.note}\n[${capture.sourceApp}] ${capture.rawText.take(200)}"
                } else {
                    capture.rawText.take(500)
                }
                dao.updateTransaction(existing.copy(
                    sourceApp = mergedSourceApp,
                    note = mergedNote,
                ))
            }
            return -1L
        }

        // 3. 新交易，正常插入
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