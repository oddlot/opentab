//package io.oddlot.ledger.fragments
//
//import android.os.Bundle
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import androidx.fragment.app.Fragment
//import androidx.lifecycle.Observer
//import androidx.lifecycle.ViewModelProviders
//import androidx.recyclerview.widget.LinearLayoutManager
//import androidx.recyclerview.widget.RecyclerView
//import io.oddlot.ledger.view_models.ItemsViewModel
//import io.oddlot.ledger.R
//import io.oddlot.ledger.adapters.ItemsAdapter
//
//class IndividualTabFragment : Fragment() {
//    private lateinit var viewModel: ItemsViewModel
//
//    companion object {
//        fun newInstance(): IndividualTabFragment {
//            return IndividualTabFragment()
//        }
//    }
//
//    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
//        val view = inflater.inflate(R.layout.fragment_individual_tab, container, false)
//        val itemsRecyclerView = view.findViewById<RecyclerView>(R.id.itemsRecyclerView).apply {
//            layoutManager  = LinearLayoutManager(activity)
//        }
//
//        // Load ViewModel and LiveData objects
//        viewModel = ViewModelProviders.of(this).get(ItemsViewModel::class.java)
//        viewModel.getItems().observe(this, Observer {
//            itemsRecyclerView.adapter = ItemsAdapter(it).apply {
//                notifyDataSetChanged() // Update RecyclerView on change
//            }
//        })
//
//        return view
//    }
//}