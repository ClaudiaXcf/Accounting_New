package com.accounting.newapp.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun transactionTypeToString(value: TransactionType): String = value.name

    @TypeConverter
    fun stringToTransactionType(value: String): TransactionType =
        try { TransactionType.valueOf(value) } catch (_: Exception) { TransactionType.Expense }

    @TypeConverter
    fun statusToString(value: ConfirmationStatus): String = value.name

    @TypeConverter
    fun stringToStatus(value: String): ConfirmationStatus =
        try { ConfirmationStatus.valueOf(value) } catch (_: Exception) { ConfirmationStatus.Pending }
}

