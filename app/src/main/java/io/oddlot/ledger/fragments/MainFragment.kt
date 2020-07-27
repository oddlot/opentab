package io.oddlot.ledger.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import io.oddlot.ledger.R
import io.oddlot.ledger.adapters.TabsAdapter
import io.oddlot.ledger.view_models.TabsViewModel
import io.oddlot.ledger.activities.db
import io.oddlot.ledger.activities.prefs
import io.oddlot.ledger.utils.basicEditText
import io.oddlot.ledger.data.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import java.util.*
import kotlin.concurrent.thread

val TAG = "MAIN_FRAGMENT"

class MainFragment : Fragment() {
    private lateinit var mTabsViewModel: TabsViewModel

    companion object {
        fun newInstance(): MainFragment {
            return MainFragment()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_tabs, container, false)
        val tabsHeader = view.findViewById<TextView>(R.id.tabsHeader).apply {
            val c = Calendar.getInstance()
            val name = prefs.getString("USERNAME", "Guest")

            when (c.get(Calendar.HOUR_OF_DAY)) {
                in 0 until 12 -> text = "Good morning, $name"
                in 12 until 18 -> text = "Good afternoon, $name"
                else -> text = "Good evening, $name"
            }
        }
        val tabsRecyclerView = view.findViewById<RecyclerView>(R.id.tabsRecyclerView).apply {
            //        val tabsRecyclerView = view.findViewById<RecyclerView>(R.id.tabsRecyclerViewFragment).apply {
            layoutManager = LinearLayoutManager(activity)
        }
        // Load ViewModel and LiveData objects
        mTabsViewModel = ViewModelProviders.of(this).get(TabsViewModel::class.java)
        mTabsViewModel.getAll().observe(this, Observer {
            tabsRecyclerView.adapter = TabsAdapter(it).apply {
                notifyDataSetChanged()
            }
        })


        /*
        Create Tab Fab
         */
        view.findViewById<FloatingActionButton>(R.id.createTabFab).apply {
            setOnClickListener {
                // Individual Tab
                val builder = AlertDialog.Builder(context!!).apply {
                    setTitle("Create a New Tab")

                    val tabNameInput = basicEditText(context).also {
                        it.requestFocus()
                    }
                    val container = FrameLayout(context).apply {
                        addView(tabNameInput)
                    }

                    setView(container)

                    setPositiveButton("OK") { dialog, which ->
                        try {
                            // Throw exception if no name is entered
                            var inputText = tabNameInput.text
                            if (tabNameInput.text.isBlank() or (inputText.length > 18))
                                throw IllegalArgumentException("Exception")
                            else {
                                // Local
                                thread {
                                    val newTab = Tab(null, tabNameInput.text.toString(), 0.0)
                                    db.tabDao().insert(newTab)
                                }
                            }

                        } catch (e: IllegalArgumentException) {
                            if (tabNameInput.text.isBlank())
                                Toast.makeText(context, "Name is required", Toast.LENGTH_LONG).show()
                            else
                                Toast.makeText(
                                    context,
                                    "Tab name must be 18 characters or less",
                                    Toast.LENGTH_LONG
                                ).show()
                        }
                    }
                    setNegativeButton("Cancel") { dialog, which ->
                        /**
                         * Hide soft input by adding below to Activity in Manifest
                         * android:windowSoftInputMode="stateAlwaysHidden"
                         */
                    }
                }
                builder.show()

//                var groupTabDialog = AlertDialog.Builder(context!!).apply {
//                    val tabNameInput = basicEditText(
//                        context
//                    ).also { it.requestFocus() }
//                    val container = FrameLayout(context).apply {
//                        addView(tabNameInput)
//                    }
//                    setView(container)
//                    setTitle("Create a Group Tab")
//                    setPositiveButton("OK") { dialog, which ->
//                        try {
//                            // Throw exception if no name is entered
//                            val inputText = tabNameInput.text
//                            if (tabNameInput.text.isBlank() or (inputText.length > 32))
//                                throw IllegalArgumentException("Exception")
//                            else {
//                                val tabName = tabNameInput.text.toString()
//
//                                CoroutineScope(IO).launch {
//                                    val groupTabId = db.tabDao().insert(
//                                        Tab(null, tabName, isGroup=true)
//                                    )
//
//                                    val newMS = Membership(null, groupTabId.toInt(), 1)
//                                    db.membershipDao().insert(newMS)
//                                }
//                            }
//
//                        } catch (e: IllegalArgumentException) {
//                            if (tabNameInput.text.isBlank())
//                                Toast.makeText(
//                                    context,
//                                    "Name is required",
//                                    Toast.LENGTH_LONG
//                                ).show()
//                            else
//                                Toast.makeText(
//                                    context,
//                                    "Name cannot be greater than 32 characters",
//                                    Toast.LENGTH_LONG
//                                ).show()
//                        }
//                    }
//                    setNegativeButton("Cancel") { dialog, which ->
//                        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
//                        imm.hideSoftInputFromWindow(tabNameInput.windowToken, 0)
//                        dialog.cancel()
//                    }
//                }
//                groupTabDialog.show()

                // Show soft keyboard
                val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY)
            }

            setOnLongClickListener {
                var groupTabDialog = AlertDialog.Builder(context!!).apply {
                    val tabNameInput =
                        basicEditText(context)
                    val container = FrameLayout(context).apply {
                        addView(tabNameInput)
                    }
                    setView(container)
                    setTitle("Create a Group Tab")
                    setPositiveButton("OK") { dialog, which ->
                        try {
                            // Throw exception if no name is entered
                            val inputText = tabNameInput.text
                            if (tabNameInput.text.isBlank() or (inputText.length > 15))
                                throw IllegalArgumentException("Exception")
                            else {
                                val tabName = tabNameInput.text.toString()

                                CoroutineScope(IO).launch {
                                    val groupTabId = db.tabDao().insert(
                                        Tab(null, tabName, isGroup=true)
                                    )

                                    val newMS = Membership(null, groupTabId.toInt(), 1)
                                    db.membershipDao().insert(newMS)
                                }
                            }

                        } catch (e: IllegalArgumentException) {
                            if (tabNameInput.text.isBlank())
                                Toast.makeText(
                                    context,
                                    "Name is required",
                                    Toast.LENGTH_LONG
                                ).show()
                            else
                                Toast.makeText(
                                    context,
                                    "Tab name must be 15 characters or less",
                                    Toast.LENGTH_LONG
                                ).show()
                        }
                    }
                    setNegativeButton("Cancel") { dialog, which ->
                        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.hideSoftInputFromWindow(tabNameInput.windowToken, 0)
                        dialog.cancel()
                    }
                }
                groupTabDialog.show()

                true
            }
        }
        return view
    }
}