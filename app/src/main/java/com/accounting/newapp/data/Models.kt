package com.accounting.newapp.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class TransactionType {
    Expense,
    Income,
    Refund
}

enum class ConfirmationStatus {
    Confirmed,
    Pending
}

enum class ThemeMode {
    System,
    Light,
    Dark
}

@Entity(tableName = "transactions", indices = [Index(value = ["sourceApp", "amountCents", "occurredAtMillis"], unique = true)])
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amountCents: Long,
    val currency: String = "CNY",
    val merchant: String,
    val category: String,
    val sourceApp: String,
    val paymentMethod: String,
    val occurredAtMillis: Long,
    val note: String = "",
    val confidence: Float,
    val status: ConfirmationStatus,
    val type: TransactionType = TransactionType.Expense,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val deletedAtMillis: Long = 0,
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val name: String,
    val colorHex: String,
    val sortOrder: Int,
)

@Entity(tableName = "merchant_rules")
data class MerchantRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pattern: String,
    val category: String,
    val sourceApp: String? = null,
    val updatedAtMillis: Long = System.currentTimeMillis(),
)

data class CategoryTotal(
    val category: String,
    val amountCents: Long,
)

data class MerchantTotal(
    val merchant: String,
    val amountCents: Long,
)

data class DayTotal(
    val dayStartMillis: Long,
    val amountCents: Long,
)

