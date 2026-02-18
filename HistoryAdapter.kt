package com.example.firebase

import com.example.firebase.ChatEntity

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

import com.example.firebase.databinding.ItemHistoryBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryAdapter(
    private val onDeleteClick: (ChatEntity) -> Unit
) : ListAdapter<ChatEntity, HistoryAdapter.HistoryViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemHistoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return HistoryViewHolder(binding, onDeleteClick)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class HistoryViewHolder(
        private val binding: ItemHistoryBinding,
        private val onDeleteClick: (ChatEntity) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(chatEntity: ChatEntity) {
            binding.apply {
                tvMessage.text = chatEntity.message
                tvType.text = if (chatEntity.isUser) "You" else "AI"
                tvTimestamp.text = formatTimestamp(chatEntity.timestamp)

                // Set background color based on sender
                root.setCardBackgroundColor(
                    if (chatEntity.isUser) {
                        root.context.getColor(android.R.color.holo_blue_light)
                    } else {
                        root.context.getColor(android.R.color.holo_green_light)
                    }
                )

                // Delete button
                btnDelete.setOnClickListener {
                    onDeleteClick(chatEntity)
                }
            }
        }

        private fun formatTimestamp(timestamp: Long): String {
            val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<ChatEntity>() {
        override fun areItemsTheSame(oldItem: ChatEntity, newItem: ChatEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ChatEntity, newItem: ChatEntity): Boolean {
            return oldItem == newItem
        }
    }
}