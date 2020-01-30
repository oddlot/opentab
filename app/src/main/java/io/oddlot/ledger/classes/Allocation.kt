package io.oddlot.ledger.classes

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.oddlot.ledger.data.Member

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

    fun addPayee(payee: Member) {
        this.payees!! += payee
        this.apply { splitEqual(getTotal())}
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

    /* Convert Allocation tp JSON string */
    fun serialize(): String = Gson().toJson(this)

}

/* Convert JSON map/string to Allocation */
fun <T: String?> T.deserialize(): Allocation {
    val type = object: TypeToken<Allocation>() {}.type

    return Gson().fromJson<Allocation>(this, type)
}