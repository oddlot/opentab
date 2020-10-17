package io.oddlot.opentab.data

import androidx.lifecycle.LiveData
import androidx.room.*


@Entity(
    tableName = "member"
    ,
    indices = [
        Index(value = ["name"], unique = true)
    ]
)
data class Member(
    @PrimaryKey(autoGenerate = true)
    val id: Int?,
    var name: String
)

@Dao
interface MemberDao {
    @Query("DELETE FROM member WHERE id = :memberId")
    fun deleteMemberById(memberId: Int): Int

    @Query("SELECT * FROM member ORDER BY name ASC")
    fun getAll(): List<Member>

    @Query("SELECT * FROM member WHERE id = :memberId")
    fun getMemberById(memberId: Int): Member

    @Query("SELECT * FROM member WHERE member.name = :name")
    fun getMemberByName(name: String): Member?

    @Query("SELECT member.* FROM member JOIN membership ON member.id = membership.memberId WHERE membership.tabId = :tabId ORDER BY name ASC")
    fun getMembersByTabId(tabId: Int): List<Member>

    @Query("SELECT member.* FROM member JOIN membership ON member.id = membership.memberId WHERE membership.tabId = :tabId ORDER BY name ASC")
    fun getLiveMembersByTabId(tabId: Int): LiveData<List<Member>>

    @Query("SELECT name FROM member WHERE id = :memberId")
    fun getMemberNameById(memberId: Int): String

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(member: Member): Long

    @Query("UPDATE member SET name = :name WHERE id = :memberId")
    fun updateMemberName(memberId: Int, name: String)
}