package com.example.cleanlake.Adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.cleanlake.Model.UserModel
import com.example.cleanlake.R

class UserAdapter(
    private var userList: List<UserModel>,
    private val onEdit: (UserModel) -> Unit,
    private val onDelete: (UserModel) -> Unit
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNama: TextView = itemView.findViewById(R.id.tvNama)
        val tvUsername: TextView = itemView.findViewById(R.id.tvUsername)
        val tvEmail: TextView = itemView.findViewById(R.id.tvEmail)
        val tvRole: TextView = itemView.findViewById(R.id.tvRole)
        val imbEdit: ImageView = itemView.findViewById(R.id.imbEdit)
        val imbDelete: ImageView = itemView.findViewById(R.id.imbDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = userList[position]
        holder.tvNama.text = user.namaLengkap
        holder.tvUsername.text = user.username
        holder.tvEmail.text = user.email
        holder.tvRole.text = user.role

        holder.imbEdit.setOnClickListener { onEdit(user) }
        holder.imbDelete.setOnClickListener { onDelete(user) }
    }

    override fun getItemCount(): Int = userList.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newList: List<UserModel>) {
        userList = newList
        notifyDataSetChanged()
    }
}
