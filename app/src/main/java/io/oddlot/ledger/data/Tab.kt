package io.oddlot.ledger.data

import androidx.lifecycle.LiveData
import androidx.room.*
import java.util.*

@Entity
data class Tab(
    @PrimaryKey(autoGenerate = true)
    val id: Int?,
    var name: String,
    var balance: Double = 0.0,
    var currency: String = "USD",
    var isGroup: Boolean = false
)

@Dao
interface TabDao {
    @Query("SELECT * FROM Tab ORDER BY name ASC")
    fun getAll(): List<Tab>

    @Query("SELECT * FROM Tab ORDER BY name ASC")
    fun getAllAsLiveData(): LiveData<List<Tab>>

    @Query("SELECT * FROM Tab WHERE id = :tabId")
    fun get(tabId: Int): Tab

    @Query("SELECT * FROM Tab WHERE id = :tabId")
    fun getLiveTabById(tabId: Int): LiveData<Tab>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insert(tab: Tab): Long

    @Query("DELETE FROM Tab WHERE id = :tabId")
    fun deleteTabById(tabId: Int): Int

    @Query("UPDATE Tab SET currency = :currency WHERE id = :tabId")
    fun setCurrency(tabId: Int, currency: String)

    @Update
    fun updateTab(tab: Tab)

    @Query("UPDATE Tab SET balance = :balance WHERE id = :tabId")
    fun updateTabBalance(tabId: Int, balance: Double)
}