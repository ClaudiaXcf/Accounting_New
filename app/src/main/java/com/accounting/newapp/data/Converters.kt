package com.accounting.newapp.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun transactionTypeToString(value: TransactionType): String = value.name

    @TypeConverter
    fun stringToTransactionType(value: String): TransactionType = TransactionType.valueOf(value)

    @TypeConverter
    fun statusToString(value: ConfirmationStatus): String = value.name

    @TypeConverter
    fun stringToStatus(value: String): ConfirmationStatus = ConfirmationStatus.valueOf(value)
}

