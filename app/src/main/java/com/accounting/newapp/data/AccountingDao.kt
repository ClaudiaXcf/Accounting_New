package com.accounting.newapp.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountingDao {
    @Query("SELECT * FROM transactions ORDER BY occurredAtMillis DESC")
    fun observeTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE occurredAtMillis BETWEEN :startMillis AND :endMillis ORDER BY occurredAtMillis DESC")
    fun observeTransactionsBetween(startMillis: Long, endMillis: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE status = 'Pending' ORDER BY occurredAtMillis DESC")
    fun observePendingTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT COALESCE(SUM(amountCents), 0) FROM transactions WHERE type = 'Expense' AND occurredAtMillis BETWEEN :startMillis AND :endMillis")
    fun observeExpenseTotal(startMillis: Long, endMillis: Long): Flow<Long>

    @Query("SELECT category, SUM(amountCents) AS amountCents FROM transactions WHERE type = 'Expense' AND occurredAtMillis BETWEEN :startMillis AND :endMillis GROUP BY category ORDER BY amountCents DESC")
    fun observeCategoryTotals(startMillis: Long, endMillis: Long): Flow<List<CategoryTotal>>

    @Query("SELECT merchant, SUM(amountCents) AS amountCents FROM transactions WHERE type = 'Expense' AND occurredAtMillis BETWEEN :startMillis AND :endMillis GROUP BY merchant ORDER BY amountCents DESC LIMIT 10")
    fun observeMerchantTotals(startMillis: Long, endMillis: Long): Flow<List<MerchantTotal>>

    @Query("SELECT * FROM categories ORDER BY sortOrder ASC")
    fun observeCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun categoryCount(): Int

    @Query("SELECT * FROM merchant_rules ORDER BY updatedAtMillis DESC")
    suspend fun getRules(): List<MerchantRuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: MerchantRuleEntity): Long
}
