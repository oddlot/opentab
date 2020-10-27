package io.oddlot.opentab.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Entity(
    tableName = "GroupTab",
    indices = [
        Index(value = ["name"], unique = true)
    ]
)
data class GroupTab (
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
interface GroupTabDao {
    @Query("SELECT * FROM GroupTab ORDER BY name ASC")
    fun allTabs(): List<GroupTab>

    @Query("SELECT * FROM GroupTab ORDER BY name ASC")
    fun allTabsAsLiveData(): LiveData<List<GroupTab>>

    @Query("SELECT * FROM GroupTab WHERE id = :tabId")
    fun tabById(tabId: Int): GroupTab

    @Query("SELECT * FROM GroupTab WHERE id = :tabId")
    fun getLiveTabById(tabId: Int): LiveData<GroupTab>

    @Query("SELECT * FROM GroupTab WHERE name = :tabName")
    fun getTabByName(tabName: String): GroupTab

    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insertTab(tab: GroupTab): Long

    @Query("DELETE FROM GroupTab WHERE id = :tabId")
    fun deleteTabById(tabId: Int): Int

    @Query("UPDATE GroupTab SET currency = :currency WHERE id = :tabId")
    fun setCurrency(tabId: Int, currency: String)

    @Update
    fun updateTab(tab: Tab)

    @Query("UPDATE GroupTab SET balance = :balance WHERE id = :tabId")
    fun updateTabBalance(tabId: Int, balance: Double)
}