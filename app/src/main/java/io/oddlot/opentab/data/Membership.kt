package io.oddlot.opentab.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Entity(
    tableName = "membership",
    foreignKeys = [
        ForeignKey(entity = Tab::class, parentColumns = ["id"], childColumns = ["tabId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Member::class, parentColumns = ["id"], childColumns = ["memberId"], onDelete = ForeignKey.CASCADE)
    ]
)
data class Membership(
    @PrimaryKey(autoGenerate = true)
    val id: Int?,
    var tabId: Int,
    var memberId: Int
)

@Dao
interface MembershipDao {
    @Query("DELETE FROM membership WHERE tabId = :tabId AND memberId = :memberId")
    fun delete(tabId: Int, memberId: Int): Int

    @Query("SELECT * FROM membership")
    fun getAll(): List<Membership>

    @Query("SELECT * FROM membership WHERE tabId = :tabId AND memberId = :memberId")
    fun getMembership(tabId: Int, memberId: Int): Membership?

    @Query("SELECT * FROM membership WHERE id = :id")
    fun getMembershipById(id: Int): Membership

    @Query("SELECT * FROM membership WHERE memberId = :memberId")
    fun getMembershipsByMemberId(memberId: Int): List<Membership>

    @Query("SELECT * FROM membership WHERE tabId = :tabId")
    fun getLiveMembershipsByTabId(tabId: Int): LiveData<List<Membership>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(membership: Membership): Long
}