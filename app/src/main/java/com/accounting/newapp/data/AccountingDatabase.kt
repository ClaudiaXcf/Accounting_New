package com.accounting.newapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [TransactionEntity::class, CategoryEntity::class, MerchantRuleEntity::class],
    version = 3,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AccountingDatabase : RoomDatabase() {
    abstract fun dao(): AccountingDao

    companion object {
        fun create(context: Context): AccountingDatabase {
            return Room.databaseBuilder(context, AccountingDatabase::class.java, "accounting.db")
                .fallbackToDestructiveMigration()
                .build()
        }

        val defaultCategories = listOf(
            CategoryEntity("餐饮", "#FF9F0A", 0),
            CategoryEntity("购物", "#34C759", 1),
            CategoryEntity("交通", "#007AFF", 2),
            CategoryEntity("娱乐", "#AF52DE", 3),
            CategoryEntity("居家", "#5856D6", 4),
            CategoryEntity("医疗", "#FF3B30", 5),
            CategoryEntity("其他", "#8E8E93", 99),
        )
    }
}
