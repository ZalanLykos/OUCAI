package com.oucai.llama

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class RecentChatsAdapter(
    private val conversations: MutableList<ChatConversation>,
    private val onConversationClick: (ChatConversation) -> Unit,
    private val onDeleteClick: (ChatConversation) -> Unit
) : RecyclerView.Adapter<RecentChatsAdapter.RecentChatViewHolder>() {

    inner class RecentChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextView = itemView.findViewById(R.id.tv_conversation_title)
        private val tvTimestamp: TextView = itemView.findViewById(R.id.tv_conversation_timestamp)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btn_delete_conversation)

        fun bind(conversation: ChatConversation) {
            tvTitle.text = conversation.title
            tvTimestamp.text = ChatHistoryManager(itemView.context).formatTimestamp(conversation.timestamp)

            // Click on the item loads the conversation
            itemView.setOnClickListener {
                onConversationClick(conversation)
            }

            // Click on delete button deletes the conversation
            btnDelete.setOnClickListener {
                onDeleteClick(conversation)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecentChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recent_chat, parent, false)
        return RecentChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecentChatViewHolder, position: Int) {
        holder.bind(conversations[position])
    }

    override fun getItemCount(): Int = conversations.size

    fun updateConversations(newConversations: List<ChatConversation>) {
        conversations.clear()
        conversations.addAll(newConversations)
        notifyDataSetChanged()
    }
}