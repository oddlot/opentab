package io.oddlot.ledger.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import io.oddlot.ledger.R
import io.oddlot.ledger.data.Member

class MembersAdapter(var members: List<Member>) : RecyclerView.Adapter<MembersAdapter.MemberViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MemberViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.layout_member_row, parent, false)

        return MemberViewHolder(view)
    }

    override fun getItemCount(): Int = members.size

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        val nv = holder.view.findViewById<TextView>(R.id.memberName)
        nv.text = members[position].name
    }

    class MemberViewHolder(val view: View): RecyclerView.ViewHolder(view)
}