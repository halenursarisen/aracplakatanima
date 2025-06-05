package com.example.kullaniciapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChatAdapter(private val messages: List<ChatMessage>) :
    RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    companion object {
        private const val VIEW_TYPE_USER = 0
        private const val VIEW_TYPE_BOT = 1
        private const val VIEW_TYPE_OPTION = 2
    }

    var clickListener: ((String) -> Unit)? = null

    inner class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textMessage: TextView? = itemView.findViewById(R.id.text_message)
        val textOption: TextView? = itemView.findViewById(R.id.text_option)
    }

    override fun getItemViewType(position: Int): Int {
        return when {
            messages[position].isOption -> VIEW_TYPE_OPTION
            messages[position].isUser -> VIEW_TYPE_USER
            else -> VIEW_TYPE_BOT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val layout = when (viewType) {
            VIEW_TYPE_USER -> R.layout.item_user_message
            VIEW_TYPE_BOT -> R.layout.item_bot_message
            VIEW_TYPE_OPTION -> R.layout.item_option_message
            else -> R.layout.item_bot_message
        }
        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val message = messages[position]

        when (getItemViewType(position)) {
            VIEW_TYPE_USER, VIEW_TYPE_BOT -> {
                holder.textMessage?.text = message.message
            }
            VIEW_TYPE_OPTION -> {
                holder.textOption?.text = message.message
                holder.textOption?.setOnClickListener {
                    clickListener?.invoke(message.message)
                }
            }
        }
    }

    override fun getItemCount(): Int = messages.size
}
