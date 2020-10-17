package io.oddlot.opentab.parcelables

import android.os.Parcel
import android.os.Parcelable


class TabParcelable(
    val id: Int,
    val name: String,
    val currency: String) : Parcelable {

    companion object {
        @JvmField
        val CREATOR = object : Parcelable.Creator<TabParcelable> {
            override fun createFromParcel(parcel: Parcel): TabParcelable {
                return TabParcelable(parcel)
            }
            override fun newArray(p0: Int): Array<TabParcelable> {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }
        }
    }

    private constructor(parcel: Parcel) : this (
        id = parcel.readInt(), // Id
        name = parcel.readString()!!, // Name
        currency = parcel.readString()!! // Currency
    )

    // 1. Invoked when calling Intent.putExtra()
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeString(name)
        parcel.writeString(currency)
    }
    override fun describeContents() = 0
}