package io.oddlot.opentab.parcelables

import android.os.Parcel
import android.os.Parcelable


class GroupTabParcelable(
    val tabId: Int,
    val name: String,
    val currency: String) : Parcelable {

    companion object {
        @JvmField
        val CREATOR = object : Parcelable.Creator<GroupTabParcelable> {
            override fun createFromParcel(parcel: Parcel): GroupTabParcelable {
                return GroupTabParcelable(parcel)
            }
            override fun newArray(p0: Int): Array<GroupTabParcelable> {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }
        }
    }

    private constructor(parcel: Parcel) : this (
        tabId = parcel.readInt(),
        name = parcel.readString()!!,
        currency = parcel.readString()!!
    )

    // 1. Invoked when calling Intent.putExtra()
    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(tabId)
        dest.writeString(name)
        dest.writeString(currency)
    }
    override fun describeContents() = 0
}