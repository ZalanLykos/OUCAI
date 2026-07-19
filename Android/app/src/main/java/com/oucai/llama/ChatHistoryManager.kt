package com.oucai.llama

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class ChatConversation(
    val id: String,
    var title: String,
    var timestamp: Long,
    val messages: MutableList<Message> = mutableListOf()
)

class ChatHistoryManager(private val context: Context) {

    companion object {
        private const val FILE_NAME = "chat_history.json"
        private const val MAX_TITLE_LENGTH = 40
    }

    private val file: File get() = File(context.filesDir, FILE_NAME)

    fun loadConversations(): MutableList<ChatConversation> {
        val list = mutableListOf<ChatConversation>()
        try {
            if (!file.exists()) return list
            val json = file.readText()
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val id = obj.getString("id")
                val title = obj.getString("title")
                val timestamp = obj.getLong("timestamp")
                val messagesArr = obj.getJSONArray("messages")
                val messages = mutableListOf<Message>()
                for (j in 0 until messagesArr.length()) {
                    val msgObj = messagesArr.getJSONObject(j)
                    messages.add(
                        Message(
                            id = msgObj.getString("id"),
                            content = msgObj.getString("content"),
                            isUser = msgObj.getBoolean("isUser")
                        )
                    )
                }
                list.add(ChatConversation(id, title, timestamp, messages))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        // Sort by timestamp descending (most recent first)
        list.sortByDescending { it.timestamp }
        return list
    }

    fun saveConversation(conversation: ChatConversation) {
        try {
            val conversations = loadConversations()
            // Replace existing or add new
            val existingIndex = conversations.indexOfFirst { it.id == conversation.id }
            if (existingIndex >= 0) {
                conversations[existingIndex] = conversation
            } else {
                conversations.add(conversation)
            }
            writeToFile(conversations)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun deleteConversation(id: String) {
        try {
            val conversations = loadConversations()
            conversations.removeAll { it.id == id }
            writeToFile(conversations)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun deleteAllConversations() {
        try {
            writeToFile(emptyList())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getConversation(id: String): ChatConversation? {
        val conversations = loadConversations()
        return conversations.find { it.id == id }
    }

    private fun writeToFile(conversations: List<ChatConversation>) {
        val arr = JSONArray()
        for (conv in conversations) {
            val obj = JSONObject()
            obj.put("id", conv.id)
            obj.put("title", conv.title)
            obj.put("timestamp", conv.timestamp)
            val messagesArr = JSONArray()
            for (msg in conv.messages) {
                val msgObj = JSONObject()
                msgObj.put("id", msg.id)
                msgObj.put("content", msg.content)
                msgObj.put("isUser", msg.isUser)
                messagesArr.put(msgObj)
            }
            obj.put("messages", messagesArr)
            arr.put(obj)
        }
        file.writeText(arr.toString(2))
    }

    fun generateTitle(firstMessage: String): String {
        val cleaned = firstMessage.trim().replace("\n", " ")
        return if (cleaned.length > MAX_TITLE_LENGTH) {
            cleaned.take(MAX_TITLE_LENGTH) + "..."
        } else {
            cleaned
        }
    }

    fun createNewConversation(): ChatConversation {
        return ChatConversation(
            id = UUID.randomUUID().toString(),
            title = "New Conversation",
            timestamp = System.currentTimeMillis()
        )
    }

    fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}