package io.oddlot.ledger.data

import androidx.room.Database
import androidx.room.RoomDatabase


@Database(entities = [Tab::class, Transaction::class, Member::class, Membership::class, /* GroupTab::class, */ GroupExpense::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tabDao(): TabDao
    abstract fun transactionDao(): TransactionDao
    abstract fun memberDao(): MemberDao
    abstract fun membershipDao(): MembershipDao
    abstract fun groupExpenseDao(): GroupExpenseDao
}