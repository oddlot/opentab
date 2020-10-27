package io.oddlot.opentab.ui.misc

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import io.oddlot.opentab.R

class HelpFragment : Fragment() {

    companion object {
        fun newInstance(): HelpFragment {
            return HelpFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_help, container, false)

        return view
    }
}