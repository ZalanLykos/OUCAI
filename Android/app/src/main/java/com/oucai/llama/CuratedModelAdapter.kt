package com.oucai.llama

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

/**
 * Sealed class representing the download state of a curated model.
 */
sealed class DownloadState {
    data object Idle : DownloadState()
    data class Downloading(
        val progress: Float,
        val speedBytesPerSec: Long,
        val bytesDownloaded: Long,
        val totalBytes: Long
    ) : DownloadState()
    data object Completed : DownloadState()
}

/**
 * Adapter for the curated model list in the "Available to Download" section.
 * Manages download states, expand/collapse toggling, and click listeners.
 */
class CuratedModelAdapter(
    private val context: Context,
    private val models: List<CuratedModel>,
    private val onDownloadClick: (CuratedModel) -> Unit,
    private val onCancelClick: (CuratedModel) -> Unit,
    private val onSettingsClick: (CuratedModel) -> Unit,
    private val onHuggingFaceClick: (CuratedModel) -> Unit
) : RecyclerView.Adapter<CuratedModelAdapter.ModelViewHolder>() {

    /** Tracks download state per model filename */
    private val downloadStates = mutableMapOf<String, DownloadState>()

    /** Tracks expanded state per model filename */
    private val expandedStates = mutableMapOf<String, Boolean>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ModelViewHolder {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.item_curated_download, parent, false)
        return ModelViewHolder(view)
    }

    override fun getItemCount(): Int = models.size

    override fun onBindViewHolder(holder: ModelViewHolder, position: Int) {
        val model = models[position]
        holder.bind(model)
    }

    /**
     * Updates the download state for a given model and refreshes the corresponding row.
     */
    fun updateDownloadState(model: CuratedModel, state: DownloadState) {
        downloadStates[model.filename] = state
        val index = models.indexOf(model)
        if (index >= 0) {
            notifyItemChanged(index)
        }
    }

    /**
     * Returns the current download state for a model, defaulting to Idle.
     */
    fun getDownloadState(model: CuratedModel): DownloadState {
        return downloadStates[model.filename] ?: DownloadState.Idle
    }

    /**
     * Toggles the expanded state for a model and refreshes the row.
     */
    fun toggleExpanded(model: CuratedModel) {
        val current = expandedStates[model.filename] ?: false
        expandedStates[model.filename] = !current
        val index = models.indexOf(model)
        if (index >= 0) {
            notifyItemChanged(index)
        }
    }

    /**
     * Formats bytes into a human-readable string (e.g., "285 MB").
     */
    private fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
            else -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        }
    }

    /**
     * Formats speed in bytes/sec to a readable string (e.g., "4.2 MB/s").
     */
    private fun formatSpeed(bytesPerSec: Long): String {
        return when {
            bytesPerSec < 1024 -> "$bytesPerSec B/s"
            bytesPerSec < 1024 * 1024 -> "${bytesPerSec / 1024} KB/s"
            bytesPerSec < 1024 * 1024 * 1024 -> String.format("%.1f MB/s", bytesPerSec / (1024.0 * 1024.0))
            else -> String.format("%.2f GB/s", bytesPerSec / (1024.0 * 1024.0 * 1024.0))
        }
    }

    inner class ModelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val curatedIcon: ImageView = itemView.findViewById(R.id.curated_icon)
        private val curatedName: TextView = itemView.findViewById(R.id.curated_name)
        private val curatedSize: TextView = itemView.findViewById(R.id.curated_size)

        // Architecture A: Idle controls
        private val idleControlRow: LinearLayout = itemView.findViewById(R.id.idle_control_row)
        private val btnDownload: MaterialButton = itemView.findViewById(R.id.btn_download)
        private val btnSettings: ImageButton = itemView.findViewById(R.id.btn_settings)
        private val btnExpand: ImageButton = itemView.findViewById(R.id.btn_expand)

        // Architecture B: Downloading controls
        private val downloadingControlRow: LinearLayout = itemView.findViewById(R.id.downloading_control_row)
        private val btnCancelDownload: ImageView = itemView.findViewById(R.id.btn_cancel_download)
        private val downloadingText: TextView = itemView.findViewById(R.id.downloading_text)
        private val downloadSpeed: TextView = itemView.findViewById(R.id.download_speed)
        private val downloadProgress: ProgressBar = itemView.findViewById(R.id.download_progress)
        private val downloadBytesText: TextView = itemView.findViewById(R.id.download_bytes_text)

        // Expandable details
        private val detailsSection: LinearLayout = itemView.findViewById(R.id.details_section)
        private val fullModelName: TextView = itemView.findViewById(R.id.full_model_name)
        private val paramValue: TextView = itemView.findViewById(R.id.param_value)
        private val authorValue: TextView = itemView.findViewById(R.id.author_value)
        private val btnHuggingFace: MaterialButton = itemView.findViewById(R.id.btn_huggingface)

        fun bind(model: CuratedModel) {
            // --- Header Row ---
            curatedIcon.setImageResource(CuratedModel.getLeadingIconResId(model.isVision))
            curatedName.text = model.name
            curatedSize.text = model.displaySize

            // --- Download State ---
            val state = downloadStates[model.filename] ?: DownloadState.Idle

            when (state) {
                is DownloadState.Idle -> {
                    idleControlRow.visibility = View.VISIBLE
                    downloadingControlRow.visibility = View.GONE
                }
                is DownloadState.Downloading -> {
                    idleControlRow.visibility = View.GONE
                    downloadingControlRow.visibility = View.VISIBLE

                    val percent = (state.progress * 100).toInt()
                    downloadingText.text = "Downloading... $percent%"
                    downloadSpeed.text = formatSpeed(state.speedBytesPerSec)
                    downloadProgress.progress = percent
                    downloadBytesText.text = "${formatBytes(state.bytesDownloaded)} / ${formatBytes(state.totalBytes)}"
                }
                is DownloadState.Completed -> {
                    idleControlRow.visibility = View.VISIBLE
                    downloadingControlRow.visibility = View.GONE
                    btnDownload.text = "Downloaded"
                    btnDownload.isEnabled = false
                }
            }

            // --- Download Button ---
            btnDownload.setOnClickListener {
                onDownloadClick(model)
            }

            // --- Cancel Download Button ---
            btnCancelDownload.setOnClickListener {
                onCancelClick(model)
            }

            // --- Settings Button ---
            btnSettings.setOnClickListener {
                onSettingsClick(model)
            }

            // --- Expand/Collapse ---
            val isExpanded = expandedStates[model.filename] ?: false
            detailsSection.visibility = if (isExpanded) View.VISIBLE else View.GONE
            btnExpand.setImageResource(
                if (isExpanded) R.drawable.ic_chevron_up_24
                else R.drawable.ic_chevron_down_24
            )
            btnExpand.setOnClickListener {
                toggleExpanded(model)
            }

            // --- Expandable Details ---
            fullModelName.text = model.name
            paramValue.text = model.parameters
            authorValue.text = model.author

            // --- Hugging Face Button ---
            btnHuggingFace.setOnClickListener {
                onHuggingFaceClick(model)
            }
        }
    }
}