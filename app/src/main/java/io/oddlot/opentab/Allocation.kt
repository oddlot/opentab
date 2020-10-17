package io.oddlot.opentab

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.oddlot.opentab.data.Member

class Allocation(var payees: MutableSet<Member> = mutableSetOf()) : HashMap<Int, Double?>() {
//    companion object {
//        private val type = object: TypeToken<HashMap<Int, Double>>() {}.type
//
//        @JvmStatic
//        fun deserialize(jsonMap: String?): Allocation {
//            return Gson().fromJson(jsonMap, type)
//        }
//    }

    init {
        // Initialize with payees if passed
        payees.forEach { payee ->
            this[payee.id!!] = 0.0
        }
    }

    fun getTotal(): Double {
        var total = 0.0
        this.values.let {
            it.forEach { amount ->
                total += amount!!
            }
        }

        return total
    }

    fun splitEqual(amount: Double = this.getTotal()) {
        val split = amount / if (payees.size == 0) 1 else payees.size

        payees.forEach { payee ->
            this[payee.id!!] = split
        }
    }

    /* Convert Allocation object to JSON string */
    fun serialize(): String = Gson().toJson(this)
}

/* Convert JSON string to Allocation object */
fun <T: String?> T.deserialize(): Allocation {
    val type = object: TypeToken<Allocation>() {}.type

    return Gson().fromJson(this, type)
}

// Alternatively
//inline fun <reified T> String.deserialize(): T {
//    val type = object: TypeToken<T>() {}.type
//
//    return Gson().fromJson<T>(this, type)
//}