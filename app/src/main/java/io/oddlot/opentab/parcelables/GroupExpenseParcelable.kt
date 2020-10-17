package io.oddlot.opentab.parcelables

import android.os.Parcel
import android.os.Parcelable


class GroupExpenseParcelable(
    val id: Int,
    var payerId: Int,
    var amountPaid: Double,
    var date: Long,
    var description: String? = "",
    var tabId: Int = -1) : Parcelable {

    companion object {
        @JvmField
        val CREATOR = object : Parcelable.Creator<GroupExpenseParcelable> {
            override fun createFromParcel(parcel: Parcel): GroupExpenseParcelable {
                return GroupExpenseParcelable(parcel)
            }
            override fun newArray(p0: Int): Array<GroupExpenseParcelable> {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }
        }
    }

    // Invoked when calling Intent.putExtra()
    override fun writeToParcel(out: Parcel, flags: Int) {
        out.writeInt(id)
        out.writeInt(payerId)
        out.writeDouble(amountPaid)
        out.writeLong(date)
        out.writeString(description)
        out.writeInt(tabId)
    }

    private constructor(parcel: Parcel) : this(
        id = parcel.readInt(),
        payerId = parcel.readInt(),
        amountPaid = parcel.readDouble(),
        date = parcel.readLong(),
        description = parcel.readString(),
        tabId = parcel.readInt()
    )

    override fun describeContents() = 0
}