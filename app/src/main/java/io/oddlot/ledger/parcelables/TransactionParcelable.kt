package io.oddlot.ledger.parcelables

import android.os.Parcel
import android.os.Parcelable


class TransactionParcelable(
    val tabId: Int,
    val itemId: Int,
    var amount: Double,
    var description: String,
    var date: Long) : Parcelable {

    companion object {
        @JvmField
        val CREATOR = object : Parcelable.Creator<TransactionParcelable> {
            override fun createFromParcel(parcel: Parcel): TransactionParcelable {
                return TransactionParcelable(parcel)
            }
            override fun newArray(p0: Int): Array<TransactionParcelable> {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }
        }
    }

    private constructor(parcel: Parcel) : this(
        tabId = parcel.readInt(),
        itemId = parcel.readInt(),
        amount = parcel.readDouble(),
        description = parcel.readString()!!,
        date = parcel.readLong()
    )

    // Invoked when calling Intent.putExtra()
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(tabId)
        parcel.writeInt(itemId)
        parcel.writeDouble(amount)
        parcel.writeString(description)
        parcel.writeLong(date)
    }

    override fun describeContents() = 0
}