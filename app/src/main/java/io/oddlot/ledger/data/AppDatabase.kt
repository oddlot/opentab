package io.oddlot.ledger.data

import androidx.room.Database
import androidx.room.RoomDatabase


@Database(entities = [Tab::class, Expense::class, Member::class, Membership::class, /* GroupTab::class, */ GroupExpense::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tabDao(): TabDao
    abstract fun itemDao(): ItemDao
    abstract fun memberDao(): MemberDao
    abstract fun membershipDao(): MembershipDao
    abstract fun groupExpenseDao(): GroupExpenseDao
}