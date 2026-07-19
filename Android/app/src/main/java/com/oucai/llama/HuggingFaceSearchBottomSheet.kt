package com.oucai.llama

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HuggingFaceSearchBottomSheet : BottomSheetDialogFragment() {
    
    private lateinit var rvResults: RecyclerView
    private lateinit var etSearch: EditText
    private lateinit var btnClose: ImageButton
    private val adapter = HfSearchAdapter { repoId ->
        onRepoSelected(repoId)
    }
    
    private var searchJob: Job? = null
    private var listener: OnRepoSelectedListener? = null
    
    interface OnRepoSelectedListener {
        fun onRepoSelected(repoId: String)
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.layout_hf_search_sheet, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        rvResults = view.findViewById(R.id.rv_hf_search_results)
        etSearch = view.findViewById(R.id.et_search)
        btnClose = view.findViewById(R.id.btn_close)
        
        rvResults.layoutManager = LinearLayoutManager(context)
        rvResults.adapter = adapter
        
        btnClose.setOnClickListener {
            dismiss()
        }
        
        etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                performSearch(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }
    
    private fun performSearch(query: String) {
        searchJob?.cancel()
        
        if (query.isBlank()) {
            adapter.submitList(emptyList())
            return
        }
        
        searchJob = CoroutineScope(Dispatchers.Main).launch {
            try {
                val results = withContext(Dispatchers.IO) {
                    searchHuggingFaceModels(query)
                }
                adapter.submitList(results)
            } catch (e: Exception) {
                Toast.makeText(context, "Search failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private suspend fun searchHuggingFaceModels(query: String): List<HfSearchResult> {
        // TODO: Implement actual Hugging Face API search
        // For now, return mock data
        return withContext(Dispatchers.IO) {
            listOf(
                HfSearchResult(
                    repoId = "prism-ml/Ternary-Bonsai-27B",
                    author = "prism-ml",
                    title = "Ternary-Bonsai-27B",
                    tag = "Vision",
                    timeAgo = "12 hours ago",
                    downloads = "301.89k",
                    likes = "717"
                )
            )
        }
    }
    
    private fun onRepoSelected(repoId: String) {
        listener?.onRepoSelected(repoId)
        dismiss()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        searchJob?.cancel()
    }
    
    companion object {
        fun newInstance(listener: OnRepoSelectedListener): HuggingFaceSearchBottomSheet {
            val fragment = HuggingFaceSearchBottomSheet()
            fragment.listener = listener
            return fragment
        }
    }
}

data class HfSearchResult(
    val repoId: String,
    val author: String,
    val title: String,
    val tag: String,
    val timeAgo: String,
    val downloads: String,
    val likes: String
)