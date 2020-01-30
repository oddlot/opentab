package io.oddlot.ledger.parcelables

import android.os.Parcel
import android.os.Parcelable


class GroupItemParcelable(
    val id: Int,
    var payerId: Int,
    var amountPaid: Double,
    var description: String = "",
    var date: Long) : Parcelable {

    companion object {
        @JvmField
        val CREATOR = object : Parcelable.Creator<GroupItemParcelable> {
            override fun createFromParcel(parcel: Parcel): GroupItemParcelable {
                return GroupItemParcelable(parcel)
            }
            override fun newArray(p0: Int): Array<GroupItemParcelable> {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }
        }
    }

    // Invoked when calling Intent.putExtra()
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeInt(payerId)
        parcel.writeDouble(amountPaid)
        parcel.writeString(description)
        parcel.writeLong(date)
    }

    private constructor(parcel: Parcel) : this(
        id = parcel.readInt(),
        payerId = parcel.readInt(),
        amountPaid = parcel.readDouble(),
        description = parcel.readString()!!,
        date = parcel.readLong()
    )

    override fun describeContents() = 0
}