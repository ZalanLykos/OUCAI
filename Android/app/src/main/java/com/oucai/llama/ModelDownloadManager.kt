package com.oucai.llama

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class ModelDownloadManager(private val context: Context) {

    companion object {
        private const val TAG = "ModelDownloadManager"
        private const val TIMEOUT_SECONDS = 60L
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    /**
     * Downloads a model from the given [url] to the app's internal models directory.
     * Reports [onProgress] as a Float 0.0..1.0.
     * Calls [onComplete] with the destination File on success, or [onError] with an exception message.
     * All callbacks are invoked on the calling coroutine's context (should be Main for UI updates).
     * On any failure (including cancellation), the partial file is deleted to prevent ghost entries.
     */
    suspend fun downloadModel(
        curatedModel: CuratedModel,
        onProgress: (Float) -> Unit,
        onComplete: (File) -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            val modelsDir = File(context.filesDir, "models")
            if (!modelsDir.exists()) modelsDir.mkdirs()

            val destination = File(modelsDir, curatedModel.filename)

            // If file already exists, call onComplete immediately
            if (destination.exists()) {
                withContext(Dispatchers.Main) {
                    onComplete(destination)
                }
                return@withContext
            }

            val request = Request.Builder()
                .url(curatedModel.url)
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                withContext(Dispatchers.Main) {
                    onError("Server returned ${response.code}: ${response.message}")
                }
                response.close()
                return@withContext
            }

            val body = response.body ?: run {
                withContext(Dispatchers.Main) {
                    onError("Empty response body")
                }
                response.close()
                return@withContext
            }

            val contentLength = body.contentLength()
            val inputStream = body.byteStream()
            val outputStream = FileOutputStream(destination)

            val buffer = ByteArray(8192)
            var bytesWritten: Long = 0
            var lastProgress = -1f

            try {
                inputStream.use { input ->
                    outputStream.use { output ->
                        while (true) {
                            // Check for cancellation before each read
                            if (!isActive) {
                                throw kotlinx.coroutines.CancellationException("Download cancelled")
                            }
                            val bytesRead = input.read(buffer)
                            if (bytesRead == -1) break

                            output.write(buffer, 0, bytesRead)
                            bytesWritten += bytesRead

                            if (contentLength > 0) {
                                val progress = bytesWritten.toFloat() / contentLength.toFloat()
                                // Throttle UI updates to avoid excessive calls
                                if (progress - lastProgress >= 0.01f || progress >= 1.0f) {
                                    lastProgress = progress
                                    withContext(Dispatchers.Main) {
                                        onProgress(progress)
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Clean up partial file on any failure (cancel, network error, etc.)
                deletePartialFile(destination)
                throw e
            }

            response.close()

            withContext(Dispatchers.Main) {
                onComplete(destination)
            }

            Log.i(TAG, "Download complete: ${destination.absolutePath}")

        } catch (e: Exception) {
            Log.e(TAG, "Download failed: ${e.message}", e)
            withContext(Dispatchers.Main) {
                onError(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Deletes a partial/incomplete download file if it exists.
     * This prevents ghost entries in the available models list.
     */
    fun deletePartialFile(curatedModel: CuratedModel) {
        val destination = File(File(context.filesDir, "models"), curatedModel.filename)
        deletePartialFile(destination)
    }

    private fun deletePartialFile(file: File) {
        if (file.exists()) {
            file.delete()
            Log.i(TAG, "Deleted partial file: ${file.absolutePath}")
        }
    }
}