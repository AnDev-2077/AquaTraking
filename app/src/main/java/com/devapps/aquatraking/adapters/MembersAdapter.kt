package com.devapps.aquatraking.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.devapps.aquatraking.R
import com.devapps.aquatraking.databinding.ItemAddMemberBinding
import com.devapps.aquatraking.objets.Member
import com.squareup.picasso.Picasso

class MembersAdapter(
    val membersList: MutableList<Member>
) : RecyclerView.Adapter<MembersAdapter.MemberViewHolder>() {

    class MemberViewHolder(val binding: ItemAddMemberBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        val binding = ItemAddMemberBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MemberViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        val member = membersList[position]
        val email = member.email
        val profileImageUrl = member.profileImageUrl
        holder.binding.tvMemberEmail.text = email
        Picasso.get().load(profileImageUrl).placeholder(R.drawable.ic_person_circle).into(holder.binding.ivMember)
        holder.binding.ivDelete.setOnClickListener {
            membersList.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, membersList.size)
        }

    }

    override fun getItemCount(): Int {
        return membersList.size
    }

    fun containsMember(email: String): Boolean {
        return membersList.any {it.email == email}
    }

    fun addMember(email: String, imageUrl: String) {
        val newMember = Member(email, imageUrl)
        membersList.add(newMember)
        notifyItemInserted(membersList.size - 1)
    }
}