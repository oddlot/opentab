package io.oddlot.opentab.presentation.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import io.oddlot.opentab.R
import io.oddlot.opentab.data.Member

class MemberAdapter(val members: List<Member>) : RecyclerView.Adapter<MemberAdapter.MemberViewHolder>() {
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
        val nameView = holder.view.findViewById<TextView>(R.id.memberName)

        nameView.text = members[position].name
    }

    class MemberViewHolder(val view: View): RecyclerView.ViewHolder(view)
}