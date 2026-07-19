package com.oucai.llama

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class HfSearchAdapter(
    private val onItemClick: (String) -> Unit
) : ListAdapter<HfSearchResult, HfSearchAdapter.ViewHolder>(DiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_hf_search_result, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvAuthor: TextView = itemView.findViewById(R.id.tv_author)
        private val tvTitle: TextView = itemView.findViewById(R.id.tv_title)
        private val tvTag: TextView = itemView.findViewById(R.id.tv_tag)
        private val tvTime: TextView = itemView.findViewById(R.id.tv_time)
        private val tvDownloads: TextView = itemView.findViewById(R.id.tv_downloads)
        private val tvLikes: TextView = itemView.findViewById(R.id.tv_likes)
        
        fun bind(result: HfSearchResult) {
            tvAuthor.text = result.author
            tvTitle.text = result.title
            tvTag.text = result.tag
            tvTime.text = result.timeAgo
            tvDownloads.text = result.downloads
            tvLikes.text = result.likes
            
            itemView.setOnClickListener {
                onItemClick(result.repoId)
            }
        }
    }
    
    class DiffCallback : DiffUtil.ItemCallback<HfSearchResult>() {
        override fun areItemsTheSame(oldItem: HfSearchResult, newItem: HfSearchResult): Boolean {
            return oldItem.repoId == newItem.repoId
        }
        
        override fun areContentsTheSame(oldItem: HfSearchResult, newItem: HfSearchResult): Boolean {
            return oldItem == newItem
        }
    }
}