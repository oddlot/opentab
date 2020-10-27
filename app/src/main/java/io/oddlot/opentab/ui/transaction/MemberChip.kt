package io.oddlot.opentab.ui.transaction

import android.content.Context
import android.content.res.ColorStateList
import android.util.Log
import androidx.core.view.get
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import io.oddlot.opentab.R

class MemberChip(context: Context) : Chip(context) {
    init {
        chipBackgroundColor = ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf(-android.R.attr.state_checked)),
            intArrayOf(R.color.Watermelon, R.color.material_on_surface_disabled)
        )
        isCheckable = true
        checkedIcon = null
        chipIconTint = null
        isClickable = true
        setChipBackgroundColorResource(R.color.chip_allocation) // setBackgroundColor doesn't work
//        setTextColor(ResourcesCompat.getColor(resources, R.color.checkable_text_color, ContextThemeWrapper(context, R.style.BaseChipStyle).theme))

        setOnCheckedChangeListener { btn, isChecked ->
            if (isChecked) {
                val group = btn.parent as ChipGroup
                val allChip = group[0] as Chip

                allChip.isChecked = false
            }
        }
    }
}