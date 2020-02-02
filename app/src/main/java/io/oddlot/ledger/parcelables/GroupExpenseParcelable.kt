package io.oddlot.ledger.parcelables

import android.os.Parcel
import android.os.Parcelable


class GroupExpenseParcelable(
    val id: Int,
    var payerId: Int,
    var amountPaid: Double,
    var date: Long,
    var description: String? = "") : Parcelable {

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
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeInt(payerId)
        parcel.writeDouble(amountPaid)
        parcel.writeLong(date)
        parcel.writeString(description)
    }

    private constructor(parcel: Parcel) : this(
        id = parcel.readInt(),
        payerId = parcel.readInt(),
        amountPaid = parcel.readDouble(),
        date = parcel.readLong(),
        description = parcel.readString()
    )

    override fun describeContents() = 0
}