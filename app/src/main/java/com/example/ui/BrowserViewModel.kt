package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.BrowserDatabase
import com.example.data.BrowserRepository
import com.example.data.HistoryItem
import com.example.data.Bookmark
import com.example.data.TabItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.UUID

// Chat Message model representing integrated Gemini AI chats
data class ChatMessage(
    val content: String,
    val isUser: Boolean,
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis()
)

// UI State representing the overall state of the browser
data class BrowserUiState(
    val tabs: List<TabState> = emptyList(),
    val activeTabId: String? = null,
    val isTabManagerOpen: Boolean = false,
    val isHistoryOpen: Boolean = false,
    val isBookmarksOpen: Boolean = false,
    val isAiOpen: Boolean = false,
    val searchInput: String = "",
    val isDesktopMode: Boolean = false
)

// In-memory tab representation containing WebView specific states
data class TabState(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "New Tab",
    val url: String = "about:newtab",
    val progress: Int = 0,
    val isLoading: Boolean = false,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false
) {
    val isNewTab: Boolean get() = url == "about:newtab" || url.isBlank()
}

class BrowserViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: BrowserRepository
    private val _uiState = MutableStateFlow(BrowserUiState())
    val uiState: StateFlow<BrowserUiState> = _uiState.asStateFlow()

    val bookmarks: StateFlow<List<Bookmark>>
    val history: StateFlow<List<HistoryItem>>

    init {
        val database = BrowserDatabase.getDatabase(application)
        repository = BrowserRepository(database)
        bookmarks = MutableStateFlow<List<Bookmark>>(emptyList())
        history = MutableStateFlow<List<HistoryItem>>(emptyList())

        // Load bookmarks and history
        viewModelScope.launch {
            repository.allBookmarks.collect {
                (bookmarks as MutableStateFlow).value = it
            }
        }
        viewModelScope.launch {
            repository.allHistory.collect {
                (history as MutableStateFlow).value = it
            }
        }

        // Restore tabs from DB if they exist, or create a default new tab
        viewModelScope.launch {
            val savedTabs = repository.allTabs.first()
            if (savedTabs.isNotEmpty()) {
                val tabStates = savedTabs.map {
                    TabState(
                        id = it.id,
                        title = it.title,
                        url = it.url
                    )
                }
                val activeTab = savedTabs.find { it.isSelected } ?: savedTabs.first()
                _uiState.value = BrowserUiState(
                    tabs = tabStates,
                    activeTabId = activeTab.id,
                    searchInput = if (activeTab.url == "about:newtab") "" else activeTab.url
                )
            } else {
                // Initial launch setup
                createNewTab()
            }
        }
    }

    // --- Tab Management ---

    fun createNewTab(url: String = "about:newtab") {
        val newTab = TabState(url = url, title = if (url == "about:newtab") "New Tab" else "Loading...")
        val currentTabs = _uiState.value.tabs.toMutableList()
        currentTabs.add(newTab)

        _uiState.value = _uiState.value.copy(
            tabs = currentTabs,
            activeTabId = newTab.id,
            isTabManagerOpen = false,
            isHistoryOpen = false,
            isBookmarksOpen = false,
            searchInput = if (url == "about:newtab") "" else url
        )

        saveTabsToDb()
    }

    fun selectTab(tabId: String) {
        val tab = _uiState.value.tabs.find { it.id == tabId } ?: return
        _uiState.value = _uiState.value.copy(
            activeTabId = tabId,
            isTabManagerOpen = false,
            searchInput = if (tab.isNewTab) "" else tab.url
        )
        saveTabsToDb()
    }

    fun closeTab(tabId: String) {
        val currentTabs = _uiState.value.tabs.toMutableList()
        val indexToClose = currentTabs.indexOfFirst { it.id == tabId }
        if (indexToClose == -1) return

        currentTabs.removeAt(indexToClose)
        viewModelScope.launch { repository.deleteTab(tabId) }

        if (currentTabs.isEmpty()) {
            _uiState.value = _uiState.value.copy(tabs = emptyList(), activeTabId = null)
            createNewTab()
        } else {
            val newActiveId = if (_uiState.value.activeTabId == tabId) {
                // If the closed tab was active, switch to another
                val nextActiveIndex = if (indexToClose < currentTabs.size) indexToClose else currentTabs.size - 1
                currentTabs[nextActiveIndex].id
            } else {
                _uiState.value.activeTabId
            }
            val activeTab = currentTabs.find { it.id == newActiveId }
            _uiState.value = _uiState.value.copy(
                tabs = currentTabs,
                activeTabId = newActiveId,
                searchInput = if (activeTab?.isNewTab == true) "" else (activeTab?.url ?: "")
            )
            saveTabsToDb()
        }
    }

    fun closeAllTabs() {
        _uiState.value = _uiState.value.copy(tabs = emptyList(), activeTabId = null)
        viewModelScope.launch { repository.clearTabs() }
        createNewTab()
    }

    private fun saveTabsToDb() {
        val activeId = _uiState.value.activeTabId
        val tabItems = _uiState.value.tabs.map {
            TabItem(
                id = it.id,
                title = it.title,
                url = it.url,
                isSelected = it.id == activeId
            )
        }
        viewModelScope.launch {
            repository.clearTabs()
            repository.saveTabs(tabItems)
        }
    }

    // --- Tab Navigation and Event Callbacks from WebView ---

    fun updateTabUrl(tabId: String, url: String) {
        val updatedTabs = _uiState.value.tabs.map {
            if (it.id == tabId) {
                it.copy(url = url, title = if (url == "about:newtab") "New Tab" else it.title)
            } else it
        }
        val isActive = _uiState.value.activeTabId == tabId
        _uiState.value = _uiState.value.copy(
            tabs = updatedTabs,
            searchInput = if (isActive) (if (url == "about:newtab") "" else url) else _uiState.value.searchInput
        )
        saveTabsToDb()
    }

    fun updateTabTitle(tabId: String, title: String) {
        val updatedTabs = _uiState.value.tabs.map {
            if (it.id == tabId) {
                it.copy(title = title)
            } else it
        }
        _uiState.value = _uiState.value.copy(tabs = updatedTabs)
        saveTabsToDb()
    }

    fun updateTabLoading(tabId: String, isLoading: Boolean) {
        val updatedTabs = _uiState.value.tabs.map {
            if (it.id == tabId) {
                it.copy(isLoading = isLoading)
            } else it
        }
        _uiState.value = _uiState.value.copy(tabs = updatedTabs)
    }

    fun updateTabProgress(tabId: String, progress: Int) {
        val updatedTabs = _uiState.value.tabs.map {
            if (it.id == tabId) {
                it.copy(progress = progress)
            } else it
        }
        _uiState.value = _uiState.value.copy(tabs = updatedTabs)
    }

    fun updateTabNavigationState(tabId: String, canGoBack: Boolean, canGoForward: Boolean) {
        val updatedTabs = _uiState.value.tabs.map {
            if (it.id == tabId) {
                it.copy(canGoBack = canGoBack, canGoForward = canGoForward)
            } else it
        }
        _uiState.value = _uiState.value.copy(tabs = updatedTabs)
    }

    fun onAddressSubmitted(input: String) {
        if (input.isBlank()) return
        val activeId = _uiState.value.activeTabId ?: return

        // Format search query or format URL
        val targetUrl = formatUrlOrSearch(input)

        updateTabUrl(activeId, targetUrl)
    }

    private fun formatUrlOrSearch(input: String): String {
        val trimmed = input.trim()
        // If it looks like a URL (contains . and no spaces, or starts with http:// / https:// / about: / file://)
        val isUrlPattern = (trimmed.startsWith("http://") || trimmed.startsWith("https://") || trimmed.startsWith("about:") || trimmed.startsWith("file://")) ||
                (trimmed.contains(".") && !trimmed.contains(" ") && trimmed.indexOf(".") > 0 && trimmed.indexOf(".") < trimmed.length - 1)

        return if (isUrlPattern) {
            if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://") && !trimmed.startsWith("about:") && !trimmed.startsWith("file://")) {
                "https://$trimmed"
            } else {
                trimmed
            }
        } else {
            // Treat as Google search
            "https://www.google.com/search?q=${java.net.URLEncoder.encode(trimmed, "UTF-8")}"
        }
    }

    // --- Search Input Typing ---

    fun updateSearchInput(input: String) {
        _uiState.value = _uiState.value.copy(searchInput = input)
    }

    // --- History ---

    fun visitPage(title: String, url: String) {
        viewModelScope.launch {
            repository.addHistoryItem(title, url)
        }
    }

    fun deleteHistory(id: Long) {
        viewModelScope.launch {
            repository.deleteHistoryItem(id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    // --- Bookmarks ---

    fun toggleBookmark(title: String, url: String) {
        viewModelScope.launch {
            repository.toggleBookmark(title, url)
        }
    }

    fun isBookmarked(url: String, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = repository.isBookmarked(url)
            callback(result)
        }
    }

    // --- Display Screens ---

    fun setTabManagerOpen(open: Boolean) {
        _uiState.value = _uiState.value.copy(isTabManagerOpen = open)
    }

    fun setHistoryOpen(open: Boolean) {
        _uiState.value = _uiState.value.copy(isHistoryOpen = open, isBookmarksOpen = false, isTabManagerOpen = false)
    }

    fun setBookmarksOpen(open: Boolean) {
        _uiState.value = _uiState.value.copy(isBookmarksOpen = open, isHistoryOpen = false, isTabManagerOpen = false)
    }

    fun toggleDesktopMode() {
        _uiState.value = _uiState.value.copy(isDesktopMode = !_uiState.value.isDesktopMode)
    }

    // --- Integrated Gemini AI Assistant State & Methods ---

    private val _aiMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                content = "Greetings! I am the Gemini Neural Assistant integrated into dpzxbrowse! 🚀\n\nAsk me anything, translate code, summarize topics, or explore neon-fast answers right inside your workspace.",
                isUser = false
            )
        )
    )
    val aiMessages: StateFlow<List<ChatMessage>> = _aiMessages.asStateFlow()

    private val _aiLoading = MutableStateFlow(false)
    val aiLoading: StateFlow<Boolean> = _aiLoading.asStateFlow()

    fun setAiOpen(open: Boolean) {
        _uiState.value = _uiState.value.copy(
            isAiOpen = open,
            isBookmarksOpen = false,
            isHistoryOpen = false,
            isTabManagerOpen = false
        )
    }

    fun clearAiChat() {
        _aiMessages.value = listOf(
            ChatMessage(
                content = "Neural connection re-established. How can I assist you, cyber-surfer?",
                isUser = false
            )
        )
    }

    fun sendAiMessage(prompt: String) {
        if (prompt.isBlank()) return
        
        val userMsg = ChatMessage(content = prompt, isUser = true)
        _aiMessages.value = _aiMessages.value + userMsg
        _aiLoading.value = true

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val client = okhttp3.OkHttpClient.Builder()
                    .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .build()

                val apiKey = com.example.BuildConfig.GEMINI_API_KEY
                
                // Build a structured conversation history payload for Gemini API
                val contentsArray = org.json.JSONArray()
                
                // Extract last 10 messages to keep the token limit optimized
                val recentMessages = _aiMessages.value.takeLast(10)
                for (msg in recentMessages) {
                    val role = if (msg.isUser) "user" else "model"
                    val contentObj = org.json.JSONObject()
                    contentObj.put("role", role)
                    val partsArray = org.json.JSONArray()
                    val partObj = org.json.JSONObject()
                    partObj.put("text", msg.content)
                    partsArray.put(partObj)
                    contentObj.put("parts", partsArray)
                    contentsArray.put(contentObj)
                }

                val jsonBody = org.json.JSONObject()
                jsonBody.put("contents", contentsArray)

                // High-performance System Instruction specifying our brand-new personality
                val sysInstruction = org.json.JSONObject()
                val sysParts = org.json.JSONArray()
                val sysPart = org.json.JSONObject()
                sysPart.put("text", "You are the advanced built-in AI companion of dpzxbrowse!, a high-octane neon black-themed web browser. Your responses should be helpful, clear, modern, and have a sleek, energetic cyberpunk tech-savvy tone. Always keep code formatting readable.")
                sysParts.put(sysPart)
                sysInstruction.put("parts", sysParts)
                jsonBody.put("systemInstruction", sysInstruction)

                val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                val body = jsonBody.toString().toRequestBody(mediaType)

                val request = okhttp3.Request.Builder()
                    .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
                    .post(body)
                    .build()

                val response = client.newCall(request).execute()
                val responseStr = response.body?.string() ?: ""
                
                if (response.isSuccessful && responseStr.isNotEmpty()) {
                    val resObj = org.json.JSONObject(responseStr)
                    val candidates = resObj.optJSONArray("candidates")
                    if (candidates != null && candidates.length() > 0) {
                        val firstCandidate = candidates.getJSONObject(0)
                        val content = firstCandidate.optJSONObject("content")
                        val parts = content?.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            val text = parts.getJSONObject(0).optString("text", "No connection received.")
                            _aiMessages.value = _aiMessages.value + ChatMessage(content = text, isUser = false)
                        } else {
                            _aiMessages.value = _aiMessages.value + ChatMessage(content = "Error parsing neural stream parts.", isUser = false)
                        }
                    } else {
                        _aiMessages.value = _aiMessages.value + ChatMessage(content = "Error parsing neural stream candidates.", isUser = false)
                    }
                } else {
                    _aiMessages.value = _aiMessages.value + ChatMessage(content = "Neural link failed. Server responded with status code: ${response.code}", isUser = false)
                }
            } catch (e: Exception) {
                _aiMessages.value = _aiMessages.value + ChatMessage(content = "Connection timed out. Cybernetic uplink failed: ${e.message}", isUser = false)
            } finally {
                _aiLoading.value = false
            }
        }
    }
}
