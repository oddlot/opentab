package io.oddlot.opentab.data

import androidx.lifecycle.LiveData
import androidx.room.*
import io.oddlot.opentab.db
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

@Entity(
    tableName = "Tab",
    indices = [
        Index(value = ["name"], unique = true)
    ]
)
data class Tab (
    @PrimaryKey(autoGenerate = true)
    val id: Int?,
    var name: String,
    var balance: Double = 0.0,
    var currency: String = "USD",
    var pinned: Boolean = false,
    var locked: Boolean = false
) : Comparable<Tab> {
    override fun compareTo(other: Tab): Int {
        return if (this.pinned && !other.pinned) { // only this pinned
            1
        } else if (!this.pinned && other.pinned) { // only other pinned
            -1
        } else {
            0
        }
    }
}

@Dao
interface TabDao {
    @Query("SELECT * FROM Tab ORDER BY name ASC")
    fun getAll(): List<Tab>

    @Query("SELECT * FROM Tab ORDER BY name ASC")
    fun getAllAsLiveData(): LiveData<List<Tab>>

    @Query("SELECT * FROM Tab WHERE id = :tabId")
    fun getTabById(tabId: Int): Tab

    @Query("SELECT * FROM Tab WHERE id = :tabId")
    fun getLiveTabById(tabId: Int): LiveData<Tab>

    @Query("SELECT * FROM Tab WHERE name = :tabName")
    fun getTabByName(tabName: String): Tab

    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insertTab(tab: Tab): Long

    @Query("DELETE FROM Tab WHERE id = :tabId")
    fun deleteTabById(tabId: Int): Int

    @Query("UPDATE Tab SET currency = :currency WHERE id = :tabId")
    fun setCurrency(tabId: Int, currency: String)

    @Update
    fun updateTab(tab: Tab)

    @Query("UPDATE Tab SET balance = :balance WHERE id = :tabId")
    fun updateTabBalance(tabId: Int, balance: Double)
}

fun getTabBalance(tabId: Int) = runBlocking {
    var balance = 0.toDouble()

    CoroutineScope(IO).launch {
        val transactions = db.transactionDao().getTransactionsByTabId(tabId).forEach { txn ->
            balance += txn.amount
        }
    }

    balance
}