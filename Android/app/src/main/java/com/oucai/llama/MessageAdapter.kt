package com.oucai.llama

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class Message(
    val id: String,
    val content: String,
    val isUser: Boolean,
    val metrics: String? = null
)

class MessageAdapter(
    private val messages: MutableList<Message>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_USER = 1
        private const val VIEW_TYPE_ASSISTANT = 2
    }

    private var primaryColor: Int? = null

    fun setPrimaryColor(color: Int) {
        primaryColor = color
        notifyDataSetChanged()
    }

    fun updateMessages(newMessages: MutableList<Message>) {
        messages.clear()
        messages.addAll(newMessages)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].isUser) VIEW_TYPE_USER else VIEW_TYPE_ASSISTANT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_USER) {
            val view = layoutInflater.inflate(R.layout.item_message_user, parent, false)
            UserMessageViewHolder(view)
        } else {
            val view = layoutInflater.inflate(R.layout.item_message_assistant, parent, false)
            AssistantMessageViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        when (holder) {
            is UserMessageViewHolder -> bindUserMessage(holder, message)
            is AssistantMessageViewHolder -> bindAssistantMessage(holder, message)
        }
    }

    private fun bindUserMessage(holder: UserMessageViewHolder, message: Message) {
        holder.contentView.text = message.content
        // Tint user message bubble with primary color
        primaryColor?.let { color ->
            holder.contentView.background?.mutate()?.setTint(color)
        }
        bindMetrics(holder.metricsRow, holder.metricsText, holder.copyButton, message)
    }

    private fun bindAssistantMessage(holder: AssistantMessageViewHolder, message: Message) {
        holder.contentView.text = message.content
        bindMetrics(holder.metricsRow, holder.metricsText, holder.copyButton, message)
    }

    private fun bindMetrics(
        metricsRow: LinearLayout,
        metricsText: TextView,
        copyButton: ImageButton,
        message: Message
    ) {
        if (message.metrics != null) {
            metricsRow.visibility = View.VISIBLE
            metricsText.text = message.metrics
            copyButton.setOnClickListener { v ->
                val clipboard = v.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("AI Response", message.content)
                clipboard.setPrimaryClip(clip)
            }
        } else {
            metricsRow.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = messages.size

    class UserMessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val contentView: TextView = view.findViewById(R.id.msg_content)
        val metricsRow: LinearLayout = view.findViewById(R.id.metrics_row)
        val metricsText: TextView = view.findViewById(R.id.metrics_text)
        val copyButton: ImageButton = view.findViewById(R.id.btn_copy)
    }

    class AssistantMessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val contentView: TextView = view.findViewById(R.id.msg_content)
        val metricsRow: LinearLayout = view.findViewById(R.id.metrics_row)
        val metricsText: TextView = view.findViewById(R.id.metrics_text)
        val copyButton: ImageButton = view.findViewById(R.id.btn_copy)
    }
}