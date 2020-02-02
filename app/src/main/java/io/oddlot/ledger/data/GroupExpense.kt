package io.oddlot.ledger.data

import androidx.lifecycle.LiveData
import androidx.room.*
import io.oddlot.ledger.activities.db

@Entity
data class GroupExpense(
    @PrimaryKey(autoGenerate = true)
    var id: Int?,
    @ForeignKey(entity = Tab::class, parentColumns = ["id"], childColumns = ["tabId"])
    var tabId: Int,
    @ForeignKey(entity = Member::class, parentColumns = ["id"], childColumns = ["payerId"])
    var payerId: Int,
    var amount: Double,
    var date: Long = System.currentTimeMillis(),
    var description: String? = null,
    var allocation: String? = null
) : Comparable<GroupExpense> {
    override fun compareTo(other: GroupExpense): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

@Dao
interface GroupExpenseDao {
    @Query("SELECT * FROM GroupExpense WHERE id = :groupId")
    fun getGroupExpenseById(groupId: Int): GroupExpense

    @Query("SELECT * FROM GroupExpense WHERE tabId = :tabId ORDER BY date DESC, id DESC")
    fun getGroupExpenseByTabId(tabId: Int): List<GroupExpense>

    @Query("SELECT * FROM GroupExpense WHERE tabId = :tabId ORDER BY date DESC, id DESC")
    fun getLiveGroupItemsByTabId(tabId: Int): LiveData<List<GroupExpense>>

//    @Query("SELECT SUM(item.amount) FROM Item WHERE groupId = :groupId")
//    fun getTotal(groupId: Int): Double

    @Query("DELETE FROM GroupExpense WHERE id = :groupId")
    fun deleteGroupItemById(groupId: Int): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(groupExpense: GroupExpense): Long

    @Update
    fun updateGroupItem(groupExpense: GroupExpense)
}

fun <T : GroupExpense> T.submit(): Long {
    return db.groupExpenseDao().insert(this)
}