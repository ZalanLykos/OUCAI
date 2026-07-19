package com.oucai.llama

import android.app.AlertDialog
import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.arm.aichat.AiChat
import com.arm.aichat.InferenceEngine
import com.arm.aichat.gguf.GgufMetadata
import com.arm.aichat.gguf.GgufMetadataReader
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private lateinit var messagesRv: RecyclerView
    private lateinit var userInputEt: EditText
    private lateinit var btnSend: ImageButton
    private lateinit var btnAttachment: ImageButton
    private lateinit var btnExpandSettings: ImageButton
    private lateinit var btnNewChat: ImageButton
    private lateinit var btnMoreOptions: ImageButton
    private lateinit var tvModelSubtitle: TextView
    private lateinit var tvAppTitle: TextView
    private lateinit var chatPanel: LinearLayout
    private lateinit var logcatPanel: LinearLayout
    private lateinit var settingsPanel: ScrollView
    private lateinit var modelsPanel: LinearLayout
    private lateinit var appInfoPanel: ScrollView
    private lateinit var modelListLayout: LinearLayout
    private lateinit var logcatOutputTv: TextView
    private lateinit var modelLogOutputTv: TextView
    private lateinit var primarySwatch: View
    private lateinit var secondarySwatch: View
    private lateinit var toolbar: com.google.android.material.appbar.MaterialToolbar
    private lateinit var noModelPlaceholder: LinearLayout
    private lateinit var inputBar: LinearLayout
    private lateinit var navView: NavigationView
    private lateinit var navMenuView: NavigationView
    private lateinit var rvRecentChats: RecyclerView
    private lateinit var btnDeleteAllChats: ImageButton
    private lateinit var recentChatsAdapter: RecentChatsAdapter

    // New layout references
    private lateinit var curatedModelListLayout: RecyclerView
    private lateinit var curatedModelAdapter: CuratedModelAdapter
    private lateinit var btnOpenHfDownloader: com.google.android.material.button.MaterialButton

    private lateinit var engine: InferenceEngine
    private var generationJob: Job? = null
    private var logcatJob: Job? = null
    private var currentLogLevel = "I"

    private var isModelReady = false
    private var isGenerating = false
    private var loadedModelName: String? = null
    private val messages = mutableListOf<Message>()
    private val lastAssistantMsg = StringBuilder()
    private val messageAdapter = MessageAdapter(messages)
    private var lastMetadataString = "No model loaded."

    private lateinit var prefs: SharedPreferences
    private lateinit var historyManager: ChatHistoryManager
    private var currentConversation: ChatConversation? = null
    private val historyItemIdMap = mutableMapOf<Int, String>()

    // Download manager
    private lateinit var downloadManager: ModelDownloadManager
    
    /** Tracks active download coroutine jobs per model filename for cancellation */
    private val downloadJobs = mutableMapOf<String, kotlinx.coroutines.Job>()
    
    /** SharedPreferences key for tracking completed curated model downloads */
    private val completedDownloadsKey = "completed_curated_downloads"

    companion object {
        private val TAG = MainActivity::class.java.simpleName
        private const val PREFS_NAME = "oucai_settings"
        private const val KEY_THEME = "theme_mode"
        private const val KEY_PRIMARY_COLOR = "primary_color"
        private const val KEY_SECONDARY_COLOR = "secondary_color"
        private const val KEY_SELECTED_THEME_PRESET = "selected_theme_preset"
        private const val KEY_LOADED_MODEL = "loaded_model"
        private const val KEY_LAST_CONVERSATION = "last_conversation_id"
        private const val KEY_LOAD_PREVIOUS_MODEL = "load_previous_model"
        private const val KEY_SYSTEM_PROMPT = "system_prompt"
        private const val KEY_AUTO_SAVE = "auto_save_conversations"
        private const val KEY_STREAM_RESPONSES = "stream_responses"

        private const val DIRECTORY_MODELS = "models"
        private const val FILE_EXTENSION_GGUF = ".gguf"
        private const val BENCH_PROMPT_PROCESSING_TOKENS = 512
        private const val BENCH_TOKEN_GENERATION_TOKENS = 128
        private const val BENCH_SEQUENCE = 1
        private const val BENCH_REPETITION = 3
    }

    private val THEME_PRESETS = listOf(
        ThemePreset("Midnight Black", "#000000", "#161616"),
        ThemePreset("Dark Gray", "#212121", "#2D2D2D"),
        ThemePreset("Charcoal", "#1A1A1A", "#242424"),
        ThemePreset("Deep Purple", "#1A1A2E", "#16213E"),
        ThemePreset("Forest Night", "#0D1B0D", "#1A2E1A"),
        ThemePreset("Ocean Depth", "#0A192F", "#172A45"),
        ThemePreset("Custom", "#1565C0", "#F5F5F5")
    )

    private fun verifyCoreSignature() {
        val verificationBlock = """
            ====================================================
            [OUCAI Engine Core] Natively Deployed Secure Architecture
            Authorization: Open Source Distribution
            Signature Verified: Made by Zalan Lykos
            Website: [https://zalanlykos.github.io](https://zalanlykos.github.io)
            ====================================================
        """.trimIndent()
        android.util.Log.i("OUCAI_Core", verificationBlock)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedTheme = prefs.getInt(KEY_THEME, AppCompatDelegate.MODE_NIGHT_YES)
        AppCompatDelegate.setDefaultNightMode(savedTheme)
        loadedModelName = prefs.getString(KEY_LOADED_MODEL, null)

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        onBackPressedDispatcher.addCallback { Log.w(TAG, "Ignore back press for simplicity") }

        historyManager = ChatHistoryManager(this)
        downloadManager = ModelDownloadManager(this)

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)
        val btnHamburger = findViewById<ImageButton>(R.id.btn_hamburger)
        btnHamburger.setOnClickListener {
            drawerLayout.open()
        }
        navView = findViewById<NavigationView>(R.id.nav_view)
        val toggle = androidx.appcompat.app.ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.nav_open, R.string.nav_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        chatPanel = findViewById(R.id.chat_panel)
        logcatPanel = findViewById(R.id.logcat_panel)
        settingsPanel = findViewById(R.id.settings_panel)
        modelsPanel = findViewById(R.id.models_panel)
        appInfoPanel = findViewById(R.id.app_info_panel)
        modelListLayout = findViewById(R.id.model_list)
        messagesRv = findViewById(R.id.messages)
        messagesRv.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        messagesRv.adapter = messageAdapter
        userInputEt = findViewById(R.id.user_input)
        btnSend = findViewById(R.id.btn_send)
        btnAttachment = findViewById(R.id.btn_attachment)
        btnExpandSettings = findViewById(R.id.btn_expand_settings)
        btnNewChat = findViewById(R.id.btn_new_chat)
        btnMoreOptions = findViewById(R.id.btn_more_options)
        tvModelSubtitle = findViewById(R.id.tv_model_subtitle)
        tvAppTitle = findViewById(R.id.tv_app_title)
        inputBar = findViewById(R.id.input_bar)
        logcatOutputTv = findViewById(R.id.logcat_output)
        modelLogOutputTv = findViewById(R.id.model_log_output)
        primarySwatch = findViewById(R.id.primary_color_swatch)
        secondarySwatch = findViewById(R.id.secondary_color_swatch)
        noModelPlaceholder = findViewById(R.id.no_model_placeholder)
        
        // Wire up the Models button on the no-model placeholder
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_open_models_from_placeholder).setOnClickListener {
            showModelsPanel()
        }

        // New layout references
        curatedModelListLayout = findViewById(R.id.curated_model_list)
        btnOpenHfDownloader = findViewById(R.id.btn_open_hf_downloader)
        
        // Set up curated model RecyclerView
        val curatedModels = CuratedModel.catalog
        curatedModelAdapter = CuratedModelAdapter(
            context = this,
            models = curatedModels,
            onDownloadClick = { model -> handleCuratedModelDownload(model) },
            onCancelClick = { model -> handleCuratedModelCancel(model) },
            onSettingsClick = { model -> 
                Toast.makeText(this, "Settings for ${model.name} coming soon", Toast.LENGTH_SHORT).show()
            },
            onHuggingFaceClick = { model -> model.openHuggingFacePage(this) }
        )
        curatedModelListLayout.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        curatedModelListLayout.adapter = curatedModelAdapter
        
        // Restore completed download states from SharedPreferences
        restoreCompletedDownloadStates()
        
        // Wire up Hugging Face downloader button
        btnOpenHfDownloader.setOnClickListener {
            showHfSearchBottomSheet()
        }

        verifyCoreSignature()

        applyUserColors()
        applyFirstLaunchTheme()

        // Update model subtitle if model is already loaded
        loadedModelName?.let { name ->
            tvModelSubtitle.text = name.removeSuffix(FILE_EXTENSION_GGUF)
            tvModelSubtitle.visibility = View.VISIBLE
        }

        // --- Wire up new toolbar buttons ---
        btnNewChat.setOnClickListener {
            if (prefs.getBoolean(KEY_AUTO_SAVE, true)) {
                saveCurrentConversation()
            }
            startNewConversation()
        }

        btnMoreOptions.setOnClickListener {
            val items = arrayOf("Settings", "Clear Conversation")
            AlertDialog.Builder(this)
                .setTitle("Options")
                .setItems(items) { _, which ->
                    when (which) {
                        0 -> showSettingsPanel()
                        1 -> {
                            currentConversation?.let { conv ->
                                historyManager.deleteConversation(conv.id)
                            }
                            currentConversation = null
                            messages.clear()
                            messageAdapter.notifyDataSetChanged()
                            updateNavDrawerHistory()
                            Toast.makeText(this, "Conversation cleared", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .show()
        }

        // --- Wire up input bar buttons ---
        btnAttachment.setOnClickListener {
            getContent.launch(arrayOf("*/*"))
        }

        btnExpandSettings.setOnClickListener {
            showModelsPopup()
        }

        // Send button toggles between send and stop
        btnSend.setOnClickListener {
            if (isGenerating) {
                generationJob?.cancel()
                generationJob = null
                isGenerating = false
                userInputEt.isEnabled = true
                btnSend.setImageResource(R.drawable.ic_send_24)
                btnSend.isEnabled = true
                btnSend.alpha = 1.0f
            } else if (isModelReady) {
                handleUserInput()
            } else {
                getContent.launch(arrayOf("*/*"))
            }
        }

        // TextWatcher to enable/disable send button
        userInputEt.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!isGenerating) {
                    val hasText = !s.isNullOrBlank()
                    btnSend.isEnabled = hasText && isModelReady
                    btnSend.alpha = if (hasText && isModelReady) 1.0f else 0.38f
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Wire up log panel toolbar buttons
        findViewById<ImageButton>(R.id.btn_copy_logcat).setOnClickListener {
            copyToClipboard("Logcat", logcatOutputTv.text.toString())
        }
        findViewById<ImageButton>(R.id.btn_copy_model_log).setOnClickListener {
            copyToClipboard("Model Log", modelLogOutputTv.text.toString())
        }

        val btnLogcatStart = findViewById<ImageButton>(R.id.btn_logcat_start)
        val btnModelLogStart = findViewById<ImageButton>(R.id.btn_model_log_start)

        btnLogcatStart.setOnClickListener { toggleLogcat(btnLogcatStart) }
        findViewById<ImageButton>(R.id.btn_logcat_clear).setOnClickListener {
            logcatOutputTv.text = ""
        }
        findViewById<ImageButton>(R.id.btn_logcat_scroll_top).setOnClickListener {
            val scrollView = logcatOutputTv.parent as? ScrollView
            scrollView?.fullScroll(View.FOCUS_UP)
        }
        findViewById<ImageButton>(R.id.btn_logcat_scroll_end).setOnClickListener {
            val scrollView = logcatOutputTv.parent as? ScrollView
            scrollView?.fullScroll(View.FOCUS_DOWN)
        }

        btnModelLogStart.setOnClickListener { toggleLogcat(btnModelLogStart) }
        findViewById<ImageButton>(R.id.btn_model_log_clear).setOnClickListener {
            modelLogOutputTv.text = ""
        }
        findViewById<ImageButton>(R.id.btn_model_log_scroll_top).setOnClickListener {
            val scrollView = modelLogOutputTv.parent as? ScrollView
            scrollView?.fullScroll(View.FOCUS_UP)
        }
        findViewById<ImageButton>(R.id.btn_model_log_scroll_end).setOnClickListener {
            val scrollView = modelLogOutputTv.parent as? ScrollView
            scrollView?.fullScroll(View.FOCUS_DOWN)
        }

        findViewById<View>(R.id.btn_primary_color).setOnClickListener {
            showColorPickerDialog("Primary Color", KEY_PRIMARY_COLOR, primarySwatch)
        }
        findViewById<View>(R.id.btn_secondary_color).setOnClickListener {
            showColorPickerDialog("Background Color", KEY_SECONDARY_COLOR, secondarySwatch)
        }

        findViewById<Button>(R.id.btn_add_model).setOnClickListener {
            getContent.launch(arrayOf("*/*"))
        }

        // Wire up collapsible sections
        val chevronReady = findViewById<ImageView>(R.id.chevron_ready)
        
        var isReadyExpanded = false
        chevronReady.setOnClickListener {
            isReadyExpanded = !isReadyExpanded
            curatedModelListLayout.visibility = if (isReadyExpanded) View.VISIBLE else View.GONE
            chevronReady.setImageResource(if (isReadyExpanded) R.drawable.ic_chevron_up_24 else R.drawable.ic_chevron_down_24)
        }
        
        // Make chevron green and start infinite bounce animation to grab attention
        chevronReady.setColorFilter(Color.parseColor("#4CAF50"))
        chevronReady.postDelayed(object : Runnable {
            override fun run() {
                chevronReady.animate()
                    .translationY(-6f)
                    .setDuration(300)
                    .withEndAction {
                        chevronReady.animate()
                            .translationY(0f)
                            .setDuration(300)
                            .withEndAction {
                                chevronReady.postDelayed(this, 800)
                            }
                            .start()
                    }
                    .start()
            }
        }, 1000)
        
        val sectionHfHeader = findViewById<TextView>(R.id.section_hf_header)
        val chevronHf = findViewById<ImageView>(R.id.chevron_hf)
        
        var isHfExpanded = false
        sectionHfHeader.setOnClickListener {
            isHfExpanded = !isHfExpanded
            chevronHf.setImageResource(if (isHfExpanded) R.drawable.ic_chevron_up_24 else R.drawable.ic_chevron_down_24)
        }

        // Wire up app info panel buttons
        findViewById<MaterialButton>(R.id.btn_visit_website).setOnClickListener {
            openUrl("https://zalanlykos.github.io")
        }

        findViewById<MaterialButton>(R.id.btn_github_star).setOnClickListener {
            openUrl("https://zalanlykos.github.io")
        }

        findViewById<LinearLayout>(R.id.version_badge).setOnClickListener {
            copyToClipboard("Version", "v1.0.0 (1)")
        }

        lifecycleScope.launch(Dispatchers.Default) {
            engine = AiChat.getInferenceEngine(applicationContext)
            loadedModelName?.let { name ->
                val modelFile = File(ensureModelsDirectory(), name)
                if (modelFile.exists()) {
                    try { engine.cleanUp() } catch (_: Exception) {}
                    engine.loadModel(modelFile.path)
                    withContext(Dispatchers.Main) {
                        isModelReady = true
                        updateNoModelPlaceholder()
                        userInputEt.hint = "Type a message..."
                        userInputEt.isEnabled = true
                        tvModelSubtitle.text = name.removeSuffix(FILE_EXTENSION_GGUF)
                        tvModelSubtitle.visibility = View.VISIBLE
                        Toast.makeText(this@MainActivity, "Loaded: $name", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        // Get the navigation menu view from the header
        navMenuView = navView.getHeaderView(0).findViewById<NavigationView>(R.id.nav_menu_view)
        
        // Set up navigation item selection listener on the menu view
        navMenuView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_chat -> showChatPanel()
                R.id.nav_models -> showModelsPanel()
                R.id.nav_settings -> showSettingsPanel()
                R.id.nav_logcat -> showLogcatPanel()
                R.id.nav_app_info -> showAppInfoPanel()
                else -> {
                    val convId = historyItemIdMap[item.itemId]
                    if (convId != null) {
                        if (prefs.getBoolean(KEY_AUTO_SAVE, true)) {
                            saveCurrentConversation()
                        }
                        loadConversation(convId)
                        showChatPanel()
                    }
                }
            }
            drawerLayout.closeDrawers()
            true
        }
        
        // Initialize recent chats RecyclerView from the header
        rvRecentChats = navView.getHeaderView(0).findViewById(R.id.rv_recent_chats)
        rvRecentChats.layoutManager = LinearLayoutManager(this)
        recentChatsAdapter = RecentChatsAdapter(
            conversations = mutableListOf(),
            onConversationClick = { conv ->
                if (prefs.getBoolean(KEY_AUTO_SAVE, true)) {
                    saveCurrentConversation()
                }
                loadConversation(conv.id)
                showChatPanel()
                drawerLayout.closeDrawers()
            },
            onDeleteClick = { conv ->
                showDeleteConversationDialog(conv)
            }
        )
        rvRecentChats.adapter = recentChatsAdapter

        // Wire up delete all button
        btnDeleteAllChats = navView.getHeaderView(0).findViewById(R.id.btn_delete_all_chats)
        btnDeleteAllChats.setOnClickListener {
            showDeleteAllConversationsDialog()
        }

        // Update nav drawer with saved conversations
        updateNavDrawerHistory()
    }

    private fun startNewConversation() {
        currentConversation = null
        messages.clear()
        messageAdapter.notifyDataSetChanged()
        updateNavDrawerHistory()
        Toast.makeText(this, "New conversation started", Toast.LENGTH_SHORT).show()
    }

    private fun saveCurrentConversation() {
        val conv = currentConversation ?: return
        if (messages.isEmpty()) return
        conv.messages.clear()
        conv.messages.addAll(messages)
        conv.timestamp = System.currentTimeMillis()
        historyManager.saveConversation(conv)
        updateNavDrawerHistory()
    }

    private fun loadConversation(convId: String) {
        val conv = historyManager.getConversation(convId) ?: return
        currentConversation = conv
        messages.clear()
        messages.addAll(conv.messages)
        messageAdapter.notifyDataSetChanged()
        messagesRv.post {
            if (messages.isNotEmpty()) {
                messagesRv.smoothScrollToPosition(messages.size - 1)
            }
        }
        prefs.edit().putString(KEY_LAST_CONVERSATION, conv.id).apply()
    }

    private fun updateNavDrawerHistory() {
        val conversations = historyManager.loadConversations()
        recentChatsAdapter.updateConversations(conversations)
    }

    private fun showDeleteConversationDialog(conversation: ChatConversation) {
        AlertDialog.Builder(this)
            .setTitle("Delete Conversation")
            .setMessage("Delete \"${conversation.title}\"?")
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton("Delete") { _, _ ->
                historyManager.deleteConversation(conversation.id)
                
                // If the deleted conversation was currently loaded, clear the chat
                if (currentConversation?.id == conversation.id) {
                    currentConversation = null
                    messages.clear()
                    messageAdapter.notifyDataSetChanged()
                }
                
                updateNavDrawerHistory()
                Toast.makeText(this, "Conversation deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteAllConversationsDialog() {
        val conversations = historyManager.loadConversations()
        if (conversations.isEmpty()) {
            Toast.makeText(this, "No conversations to delete", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Delete All Conversations")
            .setMessage("Are you sure you want to delete all ${conversations.size} conversation(s)? This action cannot be undone.")
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton("Delete All") { _, _ ->
                historyManager.deleteAllConversations()
                currentConversation = null
                messages.clear()
                messageAdapter.notifyDataSetChanged()
                updateNavDrawerHistory()
                Toast.makeText(this, "All conversations deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showColorPickerDialog(title: String, prefKey: String, swatch: View) {
        val dialog = Dialog(this, android.R.style.Theme_Material_Light_Dialog_NoActionBar)
        dialog.setContentView(R.layout.dialog_color_picker)
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.85).toInt(),
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        val canvas = dialog.findViewById<ColorPickerView>(R.id.color_canvas)
        val brightnessSlider = dialog.findViewById<SeekBar>(R.id.brightness_slider)
        val preview = dialog.findViewById<View>(R.id.color_preview)
        val hexText = dialog.findViewById<TextView>(R.id.color_hex)
        val dialogTitle = dialog.findViewById<TextView>(R.id.dialog_title)

        dialogTitle.text = "Select $title"

        val currentHex = prefs.getString(prefKey, if (prefKey == KEY_PRIMARY_COLOR) "#1565C0" else "#000000") ?: "#1565C0"
        val currentColor = Color.parseColor(currentHex)
        canvas.setInitialColor(currentColor)
        preview.setBackgroundColor(currentColor)
        hexText.text = currentHex
        brightnessSlider.progress = 255

        canvas.onColorChanged = { color ->
            val hex = String.format("#%06X", 0xFFFFFF and color)
            preview.setBackgroundColor(color)
            hexText.text = hex
        }

        brightnessSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, value: Int, fromUser: Boolean) {
                canvas.setBrightness(value)
                val color = canvas.getColor()
                val hex = String.format("#%06X", 0xFFFFFF and color)
                preview.setBackgroundColor(color)
                hexText.text = hex
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        dialog.findViewById<MaterialButton>(R.id.btn_apply).setOnClickListener {
            val color = canvas.getColor()
            val hex = String.format("#%06X", 0xFFFFFF and color)
            prefs.edit().putString(prefKey, hex).apply()
            dialog.dismiss()
            applyUserColors()
        }

        dialog.findViewById<MaterialButton>(R.id.btn_cancel).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun applyUserColors() {
        try {
            val primaryHex = prefs.getString(KEY_PRIMARY_COLOR, "#1565C0") ?: "#1565C0"
            val secondaryHex = prefs.getString(KEY_SECONDARY_COLOR, "#F5F5F5") ?: "#F5F5F5"
            val primaryColor = Color.parseColor(primaryHex)
            val secondaryColor = Color.parseColor(secondaryHex)

            toolbar.setBackgroundColor(primaryColor)
            primarySwatch.setBackgroundColor(primaryColor)
            secondarySwatch.setBackgroundColor(secondaryColor)
            chatPanel.setBackgroundColor(secondaryColor)
            settingsPanel.setBackgroundColor(secondaryColor)
            modelsPanel.setBackgroundColor(secondaryColor)
            inputBar.setBackgroundColor(secondaryColor)
            window.statusBarColor = primaryColor
            window.navigationBarColor = primaryColor

            userInputEt.background?.mutate()?.setTint(primaryColor)
            messageAdapter.setPrimaryColor(primaryColor)

            val settingsContent = settingsPanel.getChildAt(0) as? ViewGroup
            if (settingsContent != null) colorAllCards(settingsContent, secondaryColor)

            if (navView.headerCount > 0) navView.getHeaderView(0)?.setBackgroundColor(primaryColor)
        } catch (e: Exception) {
            Log.w(TAG, "Could not apply custom colors: ${e.message}")
        }
    }

    private fun colorAllCards(parent: ViewGroup, color: Int) {
        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)
            if (child is MaterialCardView) child.setCardBackgroundColor(color)
            if (child is ViewGroup) colorAllCards(child, color)
        }
    }

    private fun updateNoModelPlaceholder() {
        noModelPlaceholder.visibility = if (isModelReady) View.GONE else View.VISIBLE
    }

    private fun showChatPanel() {
        chatPanel.visibility = View.VISIBLE
        logcatPanel.visibility = View.GONE
        settingsPanel.visibility = View.GONE
        modelsPanel.visibility = View.GONE
        appInfoPanel.visibility = View.GONE
        stopLogcat()
        updateNoModelPlaceholder()
        tvAppTitle.text = "Chat"
        btnNewChat.visibility = View.VISIBLE
        btnMoreOptions.visibility = View.VISIBLE
        updateNavigationSelection(R.id.nav_chat)
    }

    private fun showLogcatPanel() {
        chatPanel.visibility = View.GONE
        logcatPanel.visibility = View.VISIBLE
        settingsPanel.visibility = View.GONE
        modelsPanel.visibility = View.GONE
        appInfoPanel.visibility = View.GONE
        modelLogOutputTv.text = lastMetadataString
        stopLogcat()
        findViewById<ImageButton>(R.id.btn_logcat_start).setImageResource(R.drawable.ic_play_24)
        findViewById<ImageButton>(R.id.btn_logcat_start).setColorFilter(Color.parseColor("#4CAF50"))
        findViewById<ImageButton>(R.id.btn_model_log_start).setImageResource(R.drawable.ic_play_24)
        findViewById<ImageButton>(R.id.btn_model_log_start).setColorFilter(Color.parseColor("#4CAF50"))
        tvAppTitle.text = "Logcat"
        btnNewChat.visibility = View.GONE
        btnMoreOptions.visibility = View.GONE
        updateNavigationSelection(R.id.nav_logcat)
    }

    private fun showSettingsPanel() {
        chatPanel.visibility = View.GONE
        logcatPanel.visibility = View.GONE
        settingsPanel.visibility = View.VISIBLE
        modelsPanel.visibility = View.GONE
        appInfoPanel.visibility = View.GONE
        stopLogcat()
        tvAppTitle.text = "Settings"
        btnNewChat.visibility = View.GONE
        btnMoreOptions.visibility = View.GONE
        updateNavigationSelection(R.id.nav_settings)

        // Wire up the load previous model switch
        val switchLoadPreviousModel = findViewById<Switch>(R.id.switch_load_previous_model)
        switchLoadPreviousModel.isChecked = prefs.getBoolean(KEY_LOAD_PREVIOUS_MODEL, true)
        switchLoadPreviousModel.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(KEY_LOAD_PREVIOUS_MODEL, isChecked).apply()
        }

        // Set up theme presets RecyclerView
        setupThemePresetsList()
        
        // Set up theme presets expand/collapse toggle
        val themePresetsContainer = findViewById<LinearLayout>(R.id.theme_presets_container)
        val btnToggleThemePresets = findViewById<LinearLayout>(R.id.btn_toggle_theme_presets)
        val themePresetsArrow = findViewById<ImageView>(R.id.theme_presets_arrow)
        
        // Set arrow to green and point down initially (collapsed state)
        themePresetsArrow.setColorFilter(Color.parseColor("#4CAF50"))
        themePresetsArrow.setImageResource(R.drawable.ic_chevron_down_24)
        
        btnToggleThemePresets.setOnClickListener {
            val isExpanded = themePresetsContainer.visibility == View.VISIBLE
            themePresetsContainer.visibility = if (isExpanded) View.GONE else View.VISIBLE
            themePresetsArrow.setImageResource(if (isExpanded) R.drawable.ic_chevron_down_24 else R.drawable.ic_chevron_up_24)
        }

        // Wire up System Prompt
        val editSystemPrompt = findViewById<EditText>(R.id.edit_system_prompt)
        val savedSystemPrompt = prefs.getString(KEY_SYSTEM_PROMPT, "") ?: ""
        editSystemPrompt.setText(savedSystemPrompt)
        editSystemPrompt.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                prefs.edit().putString(KEY_SYSTEM_PROMPT, s.toString()).apply()
            }
        })

        // Wire up Auto-save Conversations switch
        val switchAutoSave = findViewById<Switch>(R.id.switch_auto_save)
        switchAutoSave.isChecked = prefs.getBoolean(KEY_AUTO_SAVE, true)
        switchAutoSave.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(KEY_AUTO_SAVE, isChecked).apply()
        }

        // Wire up Stream Responses switch
        val switchStreamResponses = findViewById<Switch>(R.id.switch_stream_responses)
        switchStreamResponses.isChecked = prefs.getBoolean(KEY_STREAM_RESPONSES, true)
        switchStreamResponses.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(KEY_STREAM_RESPONSES, isChecked).apply()
        }
    }

    private fun setupThemePresetsList() {
        val recyclerView = findViewById<RecyclerView>(R.id.theme_presets_list)
        recyclerView.layoutManager = LinearLayoutManager(this)
        
        var selectedPosition = prefs.getInt(KEY_SELECTED_THEME_PRESET, -1)
        
        val adapter = object : RecyclerView.Adapter<ThemePresetViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThemePresetViewHolder {
                val view = layoutInflater.inflate(R.layout.item_theme_preset, parent, false)
                return ThemePresetViewHolder(view)
            }

            override fun onBindViewHolder(holder: ThemePresetViewHolder, position: Int) {
                val theme = THEME_PRESETS[position]
                val isSelected = position == selectedPosition
                holder.bind(theme, isSelected)
                
                // Remove any existing listeners before setting new ones
                holder.itemView.setOnClickListener(null)
                
                // Handle entire item click
                holder.itemView.setOnClickListener {
                    if (selectedPosition != position) {
                        val previousPosition = selectedPosition
                        selectedPosition = position
                        applyThemePreset(position)
                        // Notify only the changed items
                        if (previousPosition >= 0) notifyItemChanged(previousPosition)
                        notifyItemChanged(position)
                    }
                }
            }

            override fun getItemCount() = THEME_PRESETS.size
        }
        
        recyclerView.adapter = adapter
    }

    private fun applyThemePreset(index: Int) {
        if (index !in THEME_PRESETS.indices) return
        
        val theme = THEME_PRESETS[index]
        prefs.edit()
            .putInt(KEY_SELECTED_THEME_PRESET, index)
            .putString(KEY_PRIMARY_COLOR, theme.primaryColor)
            .putString(KEY_SECONDARY_COLOR, theme.secondaryColor)
            .apply()
        
        applyUserColors()
        Toast.makeText(this, "Applied: ${theme.name}", Toast.LENGTH_SHORT).show()
    }

    private fun applyFirstLaunchTheme() {
        val hasLaunchedBefore = prefs.contains(KEY_SELECTED_THEME_PRESET)
        if (!hasLaunchedBefore) {
            // Apply Theme 1 (Midnight Black) on first launch
            applyThemePreset(0)
        }
    }

    private class ThemePresetViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val themeName: TextView = itemView.findViewById(R.id.theme_name)
        private val primarySwatch: View = itemView.findViewById(R.id.primary_swatch)
        private val secondarySwatch: View = itemView.findViewById(R.id.secondary_swatch)
        private val card: com.google.android.material.card.MaterialCardView = itemView as com.google.android.material.card.MaterialCardView

        fun bind(theme: ThemePreset, isSelected: Boolean) {
            themeName.text = theme.name
            primarySwatch.setBackgroundColor(Color.parseColor(theme.primaryColor))
            secondarySwatch.setBackgroundColor(Color.parseColor(theme.secondaryColor))
            
            // Highlight selected theme with a different stroke
            if (isSelected) {
                card.strokeWidth = 3
                card.strokeColor = Color.parseColor("#4CAF50")
            } else {
                card.strokeWidth = 1
                card.strokeColor = Color.parseColor("#333333")
            }
        }
    }

    private fun showAppInfoPanel() {
        chatPanel.visibility = View.GONE
        logcatPanel.visibility = View.GONE
        settingsPanel.visibility = View.GONE
        modelsPanel.visibility = View.GONE
        appInfoPanel.visibility = View.VISIBLE
        stopLogcat()
        tvAppTitle.text = "App Info"
        btnNewChat.visibility = View.GONE
        btnMoreOptions.visibility = View.GONE
        updateNavigationSelection(R.id.nav_app_info)
    }

    private fun showModelsPanel() {
        chatPanel.visibility = View.GONE
        logcatPanel.visibility = View.GONE
        settingsPanel.visibility = View.GONE
        modelsPanel.visibility = View.VISIBLE
        appInfoPanel.visibility = View.GONE
        stopLogcat()
        refreshModelList()
        populateCuratedModels()
        tvAppTitle.text = "Models"
        btnNewChat.visibility = View.GONE
        btnMoreOptions.visibility = View.GONE
        updateNavigationSelection(R.id.nav_models)
    }

    private fun updateNavigationSelection(selectedItemId: Int) {
        val menu = navMenuView.menu
        for (i in 0 until menu.size()) {
            val item = menu.getItem(i)
            item.isChecked = item.itemId == selectedItemId
        }
    }

    private fun safeCleanup() {
        try { engine.cleanUp() } catch (_: Exception) {}
    }

    private fun refreshModelList() {
        modelListLayout.removeAllViews()
        val modelsDir = ensureModelsDirectory()
        val modelFiles = modelsDir.listFiles { f -> f.name.endsWith(FILE_EXTENSION_GGUF) }
            ?: emptyArray()

        if (modelFiles.isEmpty()) {
            val emptyTv = TextView(this)
            emptyTv.text = "No models found. Tap '+ Add Model' to import a GGUF file."
            emptyTv.setPadding(16, 32, 16, 32)
            emptyTv.setTextColor(Color.GRAY)
            emptyTv.gravity = android.view.Gravity.CENTER
            modelListLayout.addView(emptyTv)
            return
        }

        val primaryHex = prefs.getString(KEY_PRIMARY_COLOR, "#1565C0") ?: "#1565C0"
        val primaryColor = Color.parseColor(primaryHex)

        for (file in modelFiles.sortedByDescending { it.lastModified() }) {
            val card = layoutInflater.inflate(R.layout.item_available_model, modelListLayout, false) as MaterialCardView

            val isThisLoaded = file.name == loadedModelName && isModelReady
            val isOtherLoaded = loadedModelName != null && !isThisLoaded

            val modelName = file.name.removeSuffix(FILE_EXTENSION_GGUF)
            card.findViewById<TextView>(R.id.model_name).text = modelName
            card.findViewById<TextView>(R.id.full_model_name).text = file.name
            
            val ramText = card.findViewById<TextView>(R.id.model_ram)
            if (isThisLoaded) {
                ramText.text = formatFileSize(file.length())
            } else {
                ramText.text = "N/A"
            }
            
            val statusDot = card.findViewById<View>(R.id.status_dot)
            if (isThisLoaded) {
                statusDot.background = android.graphics.drawable.ColorDrawable(Color.parseColor("#66BB6A"))
            } else {
                statusDot.background = android.graphics.drawable.ColorDrawable(Color.parseColor("#888888"))
            }
            
            val btnMainAction = card.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_main_action)
            val btnSettings = card.findViewById<ImageButton>(R.id.btn_settings)
            val btnDelete = card.findViewById<ImageButton>(R.id.btn_delete)
            val btnExpand = card.findViewById<ImageButton>(R.id.btn_expand)
            val detailsSection = card.findViewById<LinearLayout>(R.id.details_section)

            if (isThisLoaded) {
                btnMainAction.text = "Offload"
                btnMainAction.setIconResource(R.drawable.ic_eject_24)
                btnMainAction.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#1A4CAF50")))
                btnMainAction.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#4CAF50")))
                btnMainAction.setTextColor(Color.parseColor("#4CAF50"))
                
                btnMainAction.setOnClickListener {
                    lifecycleScope.launch(Dispatchers.Default) { safeCleanup() }
                    isModelReady = false
                    loadedModelName = null
                    prefs.edit().remove(KEY_LOADED_MODEL).apply()
                    userInputEt.isEnabled = false
                    btnSend.isEnabled = false
                    btnSend.alpha = 0.38f
                    tvModelSubtitle.visibility = View.GONE
                    refreshModelList()
                    Toast.makeText(this, "Model unloaded", Toast.LENGTH_SHORT).show()
                }
            } else {
                btnMainAction.text = "Load"
                btnMainAction.setIconResource(R.drawable.ic_play_outline_24)
                btnMainAction.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#1A90CAF9")))
                btnMainAction.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#90CAF9")))
                btnMainAction.setTextColor(Color.parseColor("#90CAF9"))
                
                btnMainAction.setOnClickListener {
                    lifecycleScope.launch(Dispatchers.Default) {
                        safeCleanup()
                        engine.loadModel(file.path)
                    }
                    isModelReady = true
                    loadedModelName = file.name
                    prefs.edit().putString(KEY_LOADED_MODEL, file.name).apply()
                    userInputEt.hint = "Type a message..."
                    userInputEt.isEnabled = true
                    tvModelSubtitle.text = modelName
                    tvModelSubtitle.visibility = View.VISIBLE
                    refreshModelList()
                    Toast.makeText(this, "Loaded: ${file.name}", Toast.LENGTH_SHORT).show()
                }
            }
            
            btnSettings.setOnClickListener {
                Toast.makeText(this, "Model settings coming soon", Toast.LENGTH_SHORT).show()
            }
            
            btnDelete.setOnClickListener {
                AlertDialog.Builder(this)
                    .setTitle("Delete Model")
                    .setMessage("Delete ${file.name}?")
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton("Delete") { _, _ ->
                        file.delete()
                        if (loadedModelName == file.name) {
                            isModelReady = false
                            loadedModelName = null
                            prefs.edit().remove(KEY_LOADED_MODEL).apply()
                            tvModelSubtitle.visibility = View.GONE
                        }
                        refreshModelList()
                        Toast.makeText(this, "Deleted: ${file.name}", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
            
            var isExpanded = false
            btnExpand.setOnClickListener {
                isExpanded = !isExpanded
                detailsSection.visibility = if (isExpanded) View.VISIBLE else View.GONE
                btnExpand.setImageResource(if (isExpanded) R.drawable.ic_chevron_up_24 else R.drawable.ic_chevron_down_24)
            }
            
            modelListLayout.addView(card)
        }
    }

    /**
     * Tracks download speed by storing bytes and timestamps.
     */
    private val downloadSpeedTracker = mutableMapOf<String, MutableList<Pair<Long, Long>>>()

    /**
     * Handles the download click for a curated model.
     * Updates the adapter state to show Architecture B (downloading view)
     * and starts the actual download via ModelDownloadManager.
     */
    private fun handleCuratedModelDownload(model: CuratedModel) {
        // Cancel any existing download for this model
        downloadJobs[model.filename]?.cancel()
        downloadJobs.remove(model.filename)

        // Set initial downloading state
        curatedModelAdapter.updateDownloadState(
            model,
            DownloadState.Downloading(
                progress = 0f,
                speedBytesPerSec = 0,
                bytesDownloaded = 0,
                totalBytes = model.totalBytes
            )
        )

        // Initialize speed tracking
        downloadSpeedTracker[model.filename] = mutableListOf()

        val job = lifecycleScope.launch {
            downloadManager.downloadModel(
                curatedModel = model,
                onProgress = { fraction ->
                    val bytesDownloaded = (fraction * model.totalBytes).toLong()
                    
                    // Track speed: record timestamp and bytes
                    val tracker = downloadSpeedTracker.getOrPut(model.filename) { mutableListOf() }
                    val now = System.currentTimeMillis()
                    tracker.add(Pair(now, bytesDownloaded))
                    
                    // Keep only last 3 seconds of data for speed calculation
                    val cutoff = now - 3000
                    while (tracker.isNotEmpty() && tracker.first().first < cutoff) {
                        tracker.removeAt(0)
                    }
                    
                    // Calculate speed from tracked data
                    val speed = if (tracker.size >= 2) {
                        val oldest = tracker.first()
                        val newest = tracker.last()
                        val timeDelta = (newest.first - oldest.first) / 1000f // seconds
                        val bytesDelta = newest.second - oldest.second
                        if (timeDelta > 0) (bytesDelta / timeDelta).toLong() else 0L
                    } else {
                        0L
                    }

                    curatedModelAdapter.updateDownloadState(
                        model,
                        DownloadState.Downloading(
                            progress = fraction,
                            speedBytesPerSec = speed,
                            bytesDownloaded = bytesDownloaded,
                            totalBytes = model.totalBytes
                        )
                    )
                },
                onComplete = { file ->
                    downloadJobs.remove(model.filename)
                    downloadSpeedTracker.remove(model.filename)
                    curatedModelAdapter.updateDownloadState(model, DownloadState.Completed)
                    markDownloadCompleted(model)
                    Toast.makeText(this@MainActivity, "Downloaded: ${file.name}", Toast.LENGTH_SHORT).show()
                    refreshModelList()
                },
                onError = { errorMsg ->
                    downloadJobs.remove(model.filename)
                    downloadSpeedTracker.remove(model.filename)
                    curatedModelAdapter.updateDownloadState(model, DownloadState.Idle)
                    Toast.makeText(this@MainActivity, "Download failed: $errorMsg", Toast.LENGTH_LONG).show()
                }
            )
        }
        downloadJobs[model.filename] = job
    }

    /**
     * Restores completed download states from SharedPreferences.
     * Also cleans up partial curated model files from interrupted downloads.
     */
    private fun restoreCompletedDownloadStates() {
        val completedSet = prefs.getStringSet(completedDownloadsKey, emptySet()) ?: emptySet()
        val modelsDir = File(filesDir, "models")
        
        // Clean up any partial curated model files from interrupted downloads
        if (modelsDir.exists()) {
            for (model in CuratedModel.catalog) {
                val modelFile = File(modelsDir, model.filename)
                if (modelFile.exists() && model.filename !in completedSet) {
                    // Partial file from an interrupted download - delete it
                    modelFile.delete()
                    Log.d(TAG, "Deleted partial curated file on startup: ${model.filename}")
                }
            }
        }
        
        // Restore completed states
        for (model in CuratedModel.catalog) {
            if (model.filename in completedSet) {
                val modelFile = File(modelsDir, model.filename)
                if (modelFile.exists()) {
                    curatedModelAdapter.updateDownloadState(model, DownloadState.Completed)
                } else {
                    val updatedSet = completedSet.toMutableSet()
                    updatedSet.remove(model.filename)
                    prefs.edit().putStringSet(completedDownloadsKey, updatedSet).apply()
                }
            }
        }
    }

    /**
     * Marks a model as completed in SharedPreferences so the state persists across restarts.
     */
    private fun markDownloadCompleted(model: CuratedModel) {
        val completedSet = prefs.getStringSet(completedDownloadsKey, emptySet())?.toMutableSet() ?: mutableSetOf()
        completedSet.add(model.filename)
        prefs.edit().putStringSet(completedDownloadsKey, completedSet).apply()
    }

    /**
     * Handles the cancel download click for a curated model.
     * Cancels the active download job, deletes the partial file, and resets the state to Idle.
     */
    private fun handleCuratedModelCancel(model: CuratedModel) {
        downloadJobs[model.filename]?.cancel()
        downloadJobs.remove(model.filename)
        downloadSpeedTracker.remove(model.filename)
        downloadManager.deletePartialFile(model)
        curatedModelAdapter.updateDownloadState(model, DownloadState.Idle)
        Toast.makeText(this, "Download cancelled: ${model.name}", Toast.LENGTH_SHORT).show()
    }

    private fun populateCuratedModels() {
        // No-op: The adapter is already set up in onCreate
        // and will display the catalog automatically.
        // This method is kept for compatibility with showModelsPanel().
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun lightenColor(color: Int, factor: Float): Int {
        val r = (Color.red(color) + (255 - Color.red(color)) * (1 - factor)).toInt()
        val g = (Color.green(color) + (255 - Color.green(color)) * (1 - factor)).toInt()
        val b = (Color.blue(color) + (255 - Color.blue(color)) * (1 - factor)).toInt()
        return Color.rgb(r, g, b)
    }

    private fun darkenColor(color: Int, factor: Float): Int {
        val r = (Color.red(color) * factor).toInt()
        val g = (Color.green(color) * factor).toInt()
        val b = (Color.blue(color) * factor).toInt()
        return Color.rgb(r, g, b)
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
            else -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        }
    }

    private fun setLogLevel(level: String) {
        currentLogLevel = level
        stopLogcat()
        clearLogcat()
        startLogcat()
    }

    private fun clearLogcat() {
        logcatOutputTv.text = ""
        modelLogOutputTv.text = ""
    }

    private fun copyToClipboard(label: String, text: String) {
        if (text.isBlank()) {
            Toast.makeText(this, "$label is empty", Toast.LENGTH_SHORT).show()
            return
        }
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
        Toast.makeText(this, "$label copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    private fun appendModelLog(text: String) {
        modelLogOutputTv.append(text)
        val scrollParent = modelLogOutputTv.parent as? ScrollView
        scrollParent?.fullScroll(View.FOCUS_DOWN)
    }

    private fun toggleLogcat(btn: ImageButton) {
        if (logcatJob != null) {
            stopLogcat()
            btn.setImageResource(R.drawable.ic_play_24)
            btn.setColorFilter(Color.parseColor("#4CAF50"))
        } else {
            startLogcat()
            btn.setImageResource(R.drawable.ic_pause_24)
            btn.setColorFilter(Color.parseColor("#FF5252"))
        }
    }

    private fun startLogcat() {
        stopLogcat()
        logcatJob = lifecycleScope.launch(Dispatchers.IO) {
            try {
                val process = Runtime.getRuntime().exec(
                    arrayOf("logcat", "-v", "brief", "-t", "200", "*:$currentLogLevel")
                )
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                while (isActive) {
                    val line = reader.readLine() ?: break
                    if (line.contains("ai-chat") || line.contains(TAG) || line.contains("OUCAI")) {
                        withContext(Dispatchers.Main) {
                            logcatOutputTv.append(line + "\n")
                            val scrollParent = logcatOutputTv.parent as? ScrollView
                            scrollParent?.fullScroll(View.FOCUS_DOWN)
                        }
                    }
                }
                process.destroy()
            } catch (e: Exception) {
                Log.w(TAG, "Logcat error: ${e.message}")
            }
        }
    }

    private fun stopLogcat() {
        logcatJob?.cancel()
        logcatJob = null
    }

    private val getContent = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        Log.i(TAG, "Selected file uri:\n $uri")
        uri?.let { handleSelectedModel(it) }
    }

    private fun handleSelectedModel(uri: Uri) {
        btnSend.isEnabled = false
        userInputEt.hint = "Parsing GGUF..."

        lifecycleScope.launch(Dispatchers.IO) {
            Log.i(TAG, "Parsing GGUF metadata...")
            contentResolver.openInputStream(uri)?.use {
                GgufMetadataReader.create().readStructuredMetadata(it)
            }?.let { metadata ->
                lastMetadataString = metadata.toString()
                Log.i(TAG, "GGUF parsed: \n$metadata")
                withContext(Dispatchers.Main) {
                    logcatOutputTv.append("=== Model Metadata ===\n$lastMetadataString\n\n")
                    appendModelLog("=== Model Metadata ===\n$lastMetadataString\n\n")
                }

                val modelName = metadata.filename() + FILE_EXTENSION_GGUF
                contentResolver.openInputStream(uri)?.use { input ->
                    ensureModelFile(modelName, input)
                }?.let { modelFile ->
                    loadModel(modelName, modelFile)
                    withContext(Dispatchers.Main) {
                        isModelReady = true
                        loadedModelName = modelName
                        updateNoModelPlaceholder()
                        prefs.edit().putString(KEY_LOADED_MODEL, modelName).apply()
                        userInputEt.hint = "Type a message..."
                        userInputEt.isEnabled = true
                        tvModelSubtitle.text = modelName.removeSuffix(FILE_EXTENSION_GGUF)
                        tvModelSubtitle.visibility = View.VISIBLE
                        refreshModelList()
                        appendModelLog("Model loaded successfully: $modelName\n")
                    }
                }
            }
        }
    }

    private suspend fun ensureModelFile(modelName: String, input: InputStream) =
        withContext(Dispatchers.IO) {
            File(ensureModelsDirectory(), modelName).also { file ->
                if (!file.exists()) {
                    Log.i(TAG, "Start copying file to $modelName")
                    withContext(Dispatchers.Main) { userInputEt.hint = "Copying file..." }
                    FileOutputStream(file).use { input.copyTo(it) }
                    Log.i(TAG, "Finished copying file to $modelName")
                } else {
                    Log.i(TAG, "File already exists $modelName")
                }
            }
        }

    private suspend fun loadModel(modelName: String, modelFile: File) =
        withContext(Dispatchers.IO) {
            Log.i(TAG, "Loading model $modelName")
            withContext(Dispatchers.Main) { userInputEt.hint = "Loading model..." }
            engine.loadModel(modelFile.path)
        }

    private fun showModelsPopup() {
        val modelsDir = ensureModelsDirectory()
        val modelFiles = modelsDir.listFiles { f -> f.name.endsWith(FILE_EXTENSION_GGUF) }
            ?: emptyArray()

        if (modelFiles.isEmpty()) {
            Toast.makeText(this, "No models found. Tap '+' to import a GGUF file.", Toast.LENGTH_SHORT).show()
            return
        }

        val modelNames = modelFiles.map { it.name.removeSuffix(FILE_EXTENSION_GGUF) }.toTypedArray()
        val currentName = loadedModelName?.removeSuffix(FILE_EXTENSION_GGUF)

        AlertDialog.Builder(this)
            .setTitle("Select Model")
            .setItems(modelNames) { _, which ->
                val selectedFile = modelFiles[which]
                if (selectedFile.name != loadedModelName) {
                    lifecycleScope.launch(Dispatchers.Default) {
                        safeCleanup()
                        engine.loadModel(selectedFile.path)
                    }
                    isModelReady = true
                    loadedModelName = selectedFile.name
                    prefs.edit().putString(KEY_LOADED_MODEL, selectedFile.name).apply()
                    userInputEt.hint = "Type a message..."
                    userInputEt.isEnabled = true
                    tvModelSubtitle.text = selectedFile.name.removeSuffix(FILE_EXTENSION_GGUF)
                    tvModelSubtitle.visibility = View.VISIBLE
                    Toast.makeText(this, "Loaded: ${selectedFile.name}", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    private fun handleUserInput() {
        userInputEt.text.toString().also { userMsg ->
            if (userMsg.isEmpty()) {
                Toast.makeText(this, "Input message is empty!", Toast.LENGTH_SHORT).show()
            } else {
                userInputEt.text = null
                userInputEt.isEnabled = false
                isGenerating = true
                btnSend.setImageResource(R.drawable.ic_stop_24)
                btnSend.isEnabled = true
                btnSend.alpha = 1.0f

                if (currentConversation == null) {
                    currentConversation = historyManager.createNewConversation()
                    currentConversation!!.title = historyManager.generateTitle(userMsg)
                }

                // Get system prompt if configured
                val systemPrompt = prefs.getString(KEY_SYSTEM_PROMPT, "") ?: ""
                
                messages.add(Message(UUID.randomUUID().toString(), userMsg, true))
                lastAssistantMsg.clear()
                messages.add(Message(UUID.randomUUID().toString(), lastAssistantMsg.toString(), false))

                // Auto-save conversation if enabled
                if (prefs.getBoolean(KEY_AUTO_SAVE, true)) {
                    saveCurrentConversation()
                }
                currentConversation?.let { prefs.edit().putString(KEY_LAST_CONVERSATION, it.id).apply() }

                generationJob = lifecycleScope.launch(Dispatchers.Default) {
                    // Check if streaming is enabled
                    val streamEnabled = prefs.getBoolean(KEY_STREAM_RESPONSES, true)
                    
                    // Prepare message to send - prepend system prompt on first message
                    val messageToSend = if (systemPrompt.isNotBlank() && messages.size == 2) {
                        // First message: include system prompt as context
                        "$systemPrompt\n\nUser: $userMsg"
                    } else {
                        userMsg
                    }
                    
                    if (streamEnabled) {
                        // Stream responses token by token
                        engine.sendUserPrompt(messageToSend)
                            .onCompletion {
                                withContext(Dispatchers.Main) {
                                    isGenerating = false
                                    userInputEt.isEnabled = true
                                    btnSend.setImageResource(R.drawable.ic_send_24)
                                    btnSend.isEnabled = true
                                    btnSend.alpha = 1.0f
                                    if (prefs.getBoolean(KEY_AUTO_SAVE, true)) {
                                        saveCurrentConversation()
                                    }
                                }
                            }.collect { token ->
                                withContext(Dispatchers.Main) {
                                    val messageCount = messages.size
                                    check(messageCount > 0 && !messages[messageCount - 1].isUser)
                                    messages.removeAt(messageCount - 1).copy(
                                        content = lastAssistantMsg.append(token).toString()
                                    ).let { messages.add(it) }
                                    messageAdapter.notifyItemChanged(messages.size - 1)
                                }
                            }
                    } else {
                        // Non-streaming: wait for complete response
                        val fullResponse = StringBuilder()
                        engine.sendUserPrompt(messageToSend)
                            .onCompletion {
                                withContext(Dispatchers.Main) {
                                    isGenerating = false
                                    userInputEt.isEnabled = true
                                    btnSend.setImageResource(R.drawable.ic_send_24)
                                    btnSend.isEnabled = true
                                    btnSend.alpha = 1.0f
                                    if (prefs.getBoolean(KEY_AUTO_SAVE, true)) {
                                        saveCurrentConversation()
                                    }
                                }
                            }
                            .collect { token ->
                                fullResponse.append(token)
                            }
                        
                        withContext(Dispatchers.Main) {
                            val messageCount = messages.size
                            check(messageCount > 0 && !messages[messageCount - 1].isUser)
                            messages.removeAt(messageCount - 1).copy(
                                content = fullResponse.toString()
                            ).let { messages.add(it) }
                            messageAdapter.notifyItemChanged(messages.size - 1)
                        }
                    }
                }
            }
        }
    }

    @Deprecated("This benchmark doesn't accurately indicate GUI performance expected by app developers")
    private suspend fun runBenchmark(modelName: String, modelFile: File) =
        withContext(Dispatchers.Default) {
            Log.i(TAG, "Starts benchmarking $modelName")
            withContext(Dispatchers.Main) { userInputEt.hint = "Running benchmark..." }
            engine.bench(
                pp = BENCH_PROMPT_PROCESSING_TOKENS,
                tg = BENCH_TOKEN_GENERATION_TOKENS,
                pl = BENCH_SEQUENCE,
                nr = BENCH_REPETITION
            ).let { result ->
                messages.add(Message(UUID.randomUUID().toString(), result, false))
                withContext(Dispatchers.Main) { messageAdapter.notifyItemChanged(messages.size - 1) }
            }
        }

    private fun ensureModelsDirectory() =
        File(filesDir, DIRECTORY_MODELS).also {
            if (it.exists() && !it.isDirectory) it.delete()
            if (!it.exists()) it.mkdir()
        }

    override fun onStop() {
        generationJob?.cancel()
        super.onStop()
    }

    override fun onDestroy() {
        // Cancel all active download jobs so partial file cleanup runs
        downloadJobs.values.forEach { it.cancel() }
        downloadJobs.clear()
        if (prefs.getBoolean(KEY_AUTO_SAVE, true)) {
            saveCurrentConversation()
        }
        stopLogcat()
        try {
            engine.destroy()
        } catch (e: Exception) {
            Log.w(TAG, "Error during engine destroy: ${e.message}")
        }
        super.onDestroy()
    }

    private fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Log.w(TAG, "Could not open URL: ${e.message}")
            Toast.makeText(this, "Could not open link", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun showHfSearchBottomSheet() {
        val bottomSheet = HuggingFaceSearchBottomSheet.newInstance(object : HuggingFaceSearchBottomSheet.OnRepoSelectedListener {
            override fun onRepoSelected(repoId: String) {
                Toast.makeText(this@MainActivity, "Selected: $repoId", Toast.LENGTH_SHORT).show()
                // TODO: Feed the selected repo path to the background download engine
                lifecycleScope.launch(Dispatchers.IO) {
                    // Background download task will be implemented here
                    Log.i(TAG, "Download requested for: $repoId")
                }
            }
        })
        bottomSheet.show(supportFragmentManager, "HuggingFaceSearch")
    }
}

fun GgufMetadata.filename() = when {
    basic.name != null -> {
        basic.name?.let { name ->
            basic.sizeLabel?.let { size -> "$name-$size" } ?: name
        }
    }
    architecture?.architecture != null -> {
        architecture?.architecture?.let { arch ->
            basic.uuid?.let { uuid -> "$arch-$uuid" } ?: "$arch-${System.currentTimeMillis()}"
        }
    }
    else -> "model-${System.currentTimeMillis().toHexString()}"
}