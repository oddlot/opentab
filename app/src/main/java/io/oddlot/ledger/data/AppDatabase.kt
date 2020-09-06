package io.oddlot.ledger.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase


@Database(entities = [Tab::class, Transaction::class, Member::class, Membership::class, /* GroupTab::class, */ GroupExpense::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tabDao(): TabDao
    abstract fun transactionDao(): TransactionDao
    abstract fun memberDao(): MemberDao
    abstract fun membershipDao(): MembershipDao
    abstract fun groupExpenseDao(): GroupExpenseDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE Tab ADD isFavorited INTEGER DEFAULT 0 NOT NULL")
            }
        }
    }
}