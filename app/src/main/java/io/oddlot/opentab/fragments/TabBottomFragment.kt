package io.oddlot.opentab.fragments

import android.os.Bundle
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class TabBottomFragment(val tabId: Int) : BottomSheetDialogFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tabId
    }

//    override fun setupDialog(dialog: Dialog, style: Int) {
//        super.setupDialog(dialog, style)
//    }
}