package io.oddlot.opentab.data

import android.net.Uri
import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun toUri(uriString: String?): Uri? {
        return uriString?.let {
            Uri.parse(uriString)
        }
    }

    @TypeConverter
    fun fromUri(uri: Uri?): String? {
        return uri?.toString()
    }

    @TypeConverter
    fun stringFromUris(uris: MutableList<Uri>?): String? {
        return uris?.map { it.toString() }?.joinToString(separator = ",")
    }

    @TypeConverter
    fun urisFromString(uriString: String?): MutableList<Uri>? {
        var uris = mutableListOf<Uri>()
        val stringList = uriString?.split(",")
        stringList?.forEach { uriString ->
            uris.add(Uri.parse(uriString))
        }
        return uris
    }
}