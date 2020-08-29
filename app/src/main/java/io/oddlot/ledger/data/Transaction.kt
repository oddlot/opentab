package io.oddlot.ledger.data

import androidx.lifecycle.LiveData
import androidx.room.*
import kotlin.math.round


@Entity(
        tableName = "item",
        foreignKeys = [
            ForeignKey(entity = Tab::class, parentColumns = ["id"], childColumns = ["tabId"], onDelete = ForeignKey.CASCADE)
        ]
)
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Int?,
    val tabId: Int,
    var amount: Double,
    var description: String? = null,
    var date: Long = System.currentTimeMillis()) : Comparable<Transaction> {

    override fun compareTo(other: Transaction): Int {
        val delta = round(
            (this.date - other.date) / 10000.0
        )
        return when (delta) {
            in 1 .. Int.MAX_VALUE -> 1
            in 0 .. 0 -> if (this.id!! > other.id!!) 1 else -1
            else -> -1
        }
    }
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM item")
    fun getAll(): List<Transaction>

    @Query("SELECT * FROM item")
    fun getAllAsLiveData(): LiveData<List<Transaction>>

    @Query("SELECT * FROM item WHERE id = :itemId")
    fun get(itemId: Int): Transaction

//    @Query("SELECT * FROM item WHERE groupId = :groupId")
//    fun getItemsByGroupId(groupId: Int): List<Item>

    @Query("SELECT * FROM item WHERE tabId = :tabId")
    fun getTransactionsByTabId(tabId: Int): List<Transaction>

    @Query("DELETE FROM item WHERE id = :itemId")
    fun deleteItemById(itemId: Int): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(transaction: Transaction): Long
}