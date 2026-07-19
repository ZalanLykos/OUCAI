package com.oucai.llama

import android.content.Context
import android.content.Intent
import android.net.Uri

data class CuratedModel(
    val name: String,
    val author: String,
    val parameters: String,
    val displaySize: String,
    val totalBytes: Long,
    val url: String,
    val filename: String,
    val huggingFaceUrl: String,
    val isVision: Boolean = false
) {
    companion object {
        val catalog = listOf(
            CuratedModel(
                name = "Qwen2.5-0.5B-Instruct (Q4_K_M)",
                author = "Qwen",
                parameters = "0.49B",
                displaySize = "351.45 MB",
                totalBytes = 368512204L,
                url = "https://huggingface.co/bartowski/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/Qwen2.5-0.5B-Instruct-Q4_K_M.gguf",
                filename = "Qwen2.5-0.5B-Instruct-Q4_K_M.gguf",
                huggingFaceUrl = "https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct",
                isVision = false
            ),
            CuratedModel(
                name = "Llama-3.2-1B-Instruct (Q8_0)",
                author = "bartowski",
                parameters = "1.24B",
                displaySize = "1.31 GB",
                totalBytes = 1409280000L,
                url = "https://huggingface.co/bartowski/Llama-3.2-1B-Instruct-GGUF/resolve/main/Llama-3.2-1B-Instruct-Q8_0.gguf",
                filename = "Llama-3.2-1B-Instruct-Q8_0.gguf",
                huggingFaceUrl = "https://huggingface.co/bartowski/Llama-3.2-1B-Instruct-GGUF",
                isVision = false
            ),
            CuratedModel(
                name = "Llama-3.2-3B-Instruct (Q8_0)",
                author = "bartowski",
                parameters = "3.21B",
                displaySize = "3.31 GB",
                totalBytes = 3550000000L,
                url = "https://huggingface.co/bartowski/Llama-3.2-3B-Instruct-GGUF/resolve/main/Llama-3.2-3B-Instruct-Q8_0.gguf",
                filename = "Llama-3.2-3B-Instruct-Q8_0.gguf",
                huggingFaceUrl = "https://huggingface.co/bartowski/Llama-3.2-3B-Instruct-GGUF",
                isVision = false
            ),
            CuratedModel(
                name = "Qwen2.5-1.5B-Instruct (Q8_0)",
                author = "bartowski",
                parameters = "1.54B",
                displaySize = "1.89 GB",
                totalBytes = 2029371392L,
                url = "https://huggingface.co/bartowski/Qwen2.5-1.5B-Instruct-GGUF/resolve/main/Qwen2.5-1.5B-Instruct-Q8_0.gguf",
                filename = "Qwen2.5-1.5B-Instruct-Q8_0.gguf",
                huggingFaceUrl = "https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct",
                isVision = false
            ),
            CuratedModel(
                name = "Qwen2.5-3B-Instruct (Q5_K_M)",
                author = "bartowski",
                parameters = "3.09B",
                displaySize = "2.44 GB",
                totalBytes = 2619932672L,
                url = "https://huggingface.co/bartowski/Qwen2.5-3B-Instruct-GGUF/resolve/main/Qwen2.5-3B-Instruct-Q5_K_M.gguf",
                filename = "Qwen2.5-3B-Instruct-Q5_K_M.gguf",
                huggingFaceUrl = "https://huggingface.co/Qwen/Qwen2.5-3B-Instruct",
                isVision = false
            ),
            CuratedModel(
                name = "SmolVLM2-500M-Instruct (Q8_0)",
                author = "bartowski",
                parameters = "2.26B",
                displaySize = "436.81 MB",
                totalBytes = 458227712L,
                url = "https://huggingface.co/bartowski/SmolVLM2-500M-Instruct-GGUF/resolve/main/SmolVLM2-500M-Instruct-Q8_0.gguf",
                filename = "SmolVLM2-500M-Instruct-Q8_0.gguf",
                huggingFaceUrl = "https://huggingface.co/bartowski/SmolVLM2-500M-Instruct-GGUF",
                isVision = true
            ),
            CuratedModel(
                name = "TinyLlama-1.1B-Chat (Q8_0)",
                author = "bartowski",
                parameters = "1.1B",
                displaySize = "1.21 GB",
                totalBytes = 1298000000L,
                url = "https://huggingface.co/bartowski/TinyLlama-1.1B-Chat-v1.0-GGUF/resolve/main/TinyLlama-1.1B-Chat-v1.0-Q8_0.gguf",
                filename = "TinyLlama-1.1B-Chat-v1.0-Q8_0.gguf",
                huggingFaceUrl = "https://huggingface.co/bartowski/TinyLlama-1.1B-Chat-v1.0-GGUF",
                isVision = false
            ),
            CuratedModel(
                name = "Mistral-7B-Instruct (Q4_K_M)",
                author = "bartowski",
                parameters = "7.24B",
                displaySize = "4.37 GB",
                totalBytes = 4690000000L,
                url = "https://huggingface.co/bartowski/Mistral-7B-Instruct-v0.3-GGUF/resolve/main/Mistral-7B-Instruct-v0.3-Q4_K_M.gguf",
                filename = "Mistral-7B-Instruct-v0.3-Q4_K_M.gguf",
                huggingFaceUrl = "https://huggingface.co/bartowski/Mistral-7B-Instruct-v0.3-GGUF",
                isVision = false
            ),
            CuratedModel(
                name = "Zephyr-7B-Beta (Q4_K_M)",
                author = "bartowski",
                parameters = "7.24B",
                displaySize = "4.37 GB",
                totalBytes = 4690000000L,
                url = "https://huggingface.co/bartowski/zephyr-7b-beta-GGUF/resolve/main/zephyr-7b-beta-Q4_K_M.gguf",
                filename = "zephyr-7b-beta-Q4_K_M.gguf",
                huggingFaceUrl = "https://huggingface.co/bartowski/zephyr-7b-beta-GGUF",
                isVision = false
            )
        )

        fun getLeadingIconResId(isVision: Boolean): Int {
            return if (isVision) R.drawable.ic_eye_24 else R.drawable.ic_chat_24
        }
    }

    fun openHuggingFacePage(context: Context) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(huggingFaceUrl))
            context.startActivity(intent)
        } catch (e: Exception) {
            // silently fail
        }
    }
}