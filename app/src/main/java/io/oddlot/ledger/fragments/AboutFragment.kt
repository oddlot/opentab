package io.oddlot.ledger.fragments

import android.app.ActionBar
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import com.google.android.material.navigation.NavigationView
import io.oddlot.ledger.R
import io.oddlot.ledger.db
import io.oddlot.ledger.activities.prefs
import io.oddlot.ledger.utils.UsernameFilter
import io.oddlot.ledger.utils.basicEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class AboutFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_about, container, false)

        return view
    }
}