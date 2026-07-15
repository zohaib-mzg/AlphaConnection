package com.alphaconnection.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.alphaconnection.R
import com.alphaconnection.data.MessageModel

class MessageAdapter(
    private val messages: MutableList<MessageModel> = mutableListOf()
) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvSender: TextView = view.findViewById(R.id.tvSender)
        val tvMessageText: TextView = view.findViewById(R.id.tvMessageText)
        val tvMessageTime: TextView = view.findViewById(R.id.tvMessageTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]
        holder.tvSender.text = if (message.isSent) "You (${message.sender})" else message.sender
        holder.tvMessageText.text = message.text
        holder.tvMessageTime.text = message.timestamp
    }

    override fun getItemCount(): Int = messages.size

    fun addMessage(message: MessageModel) {
        messages.add(0, message)
        notifyItemInserted(0)
    }

    fun clear() {
        val size = messages.size
        messages.clear()
        notifyItemRangeRemoved(0, size)
    }
}
