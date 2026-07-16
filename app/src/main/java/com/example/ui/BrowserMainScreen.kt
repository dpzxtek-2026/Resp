package com.example.ui

import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Bookmark
import com.example.data.HistoryItem

@Composable
fun BrowserMainScreen(
    viewModel: BrowserViewModel,
    isDarkTheme: Boolean,
    onToggleDarkTheme: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val bookmarks by viewModel.bookmarks.collectAsState()
    val history by viewModel.history.collectAsState()

    // Map to persist WebView instances keyed by Tab State IDs
    val webViewsCache = remember { mutableMapOf<String, WebView>() }

    val activeTab = remember(uiState.tabs, uiState.activeTabId) {
        uiState.tabs.find { it.id == uiState.activeTabId }
    }

    var isSearchEditing by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }

    // Intercept hardware back button
    BackHandler(enabled = true) {
        when {
            isSearchEditing -> {
                isSearchEditing = false
            }
            uiState.isAiOpen -> {
                viewModel.setAiOpen(false)
            }
            uiState.isTabManagerOpen -> {
                viewModel.setTabManagerOpen(false)
            }
            uiState.isHistoryOpen -> {
                viewModel.setHistoryOpen(false)
            }
            uiState.isBookmarksOpen -> {
                viewModel.setBookmarksOpen(false)
            }
            activeTab != null && activeTab!!.canGoBack -> {
                // Trigger back on webview
                webViewsCache[activeTab!!.id]?.goBack()
            }
            else -> {
                // If on homepage or no back stack, we can't do anything, but let standard Android handle exit
            }
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        topBar = {
            if (!uiState.isTabManagerOpen && !uiState.isHistoryOpen && !uiState.isBookmarksOpen && !isSearchEditing) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Chrome Top Navigation and Search Row
                    ChromeTopBar(
                        activeTab = activeTab,
                        tabsCount = uiState.tabs.size,
                        searchInput = uiState.searchInput,
                        isBookmarked = bookmarks.any { it.url == activeTab?.url },
                        onBackClick = {
                            if (activeTab?.canGoBack == true) {
                                webViewsCache[activeTab.id]?.goBack()
                            }
                        },
                        onForwardClick = {
                            if (activeTab?.canGoForward == true) {
                                webViewsCache[activeTab.id]?.goForward()
                            }
                        },
                        onHomeClick = {
                            activeTab?.id?.let { tabId ->
                                viewModel.updateTabUrl(tabId, "about:newtab")
                                viewModel.updateTabTitle(tabId, "New Tab")
                            }
                        },
                        onRefreshClick = {
                            activeTab?.id?.let { tabId ->
                                webViewsCache[tabId]?.reload()
                            }
                        },
                        onOmniboxClick = {
                            isSearchEditing = true
                        },
                        onTabBadgeClick = {
                            viewModel.setTabManagerOpen(true)
                        },
                        onMenuClick = {
                            menuExpanded = true
                        },
                        onAiClick = {
                            activeTab?.let { tab ->
                                viewModel.updateTabUrl(tab.id, "https://google.ai")
                                viewModel.updateTabTitle(tab.id, "Google ai")
                            }
                        }
                    )

                    // Drops Menu
                    Box(modifier = Modifier.align(Alignment.End)) {
                        ChromeDropdownMenu(
                            expanded = menuExpanded,
                            isBookmarked = bookmarks.any { it.url == activeTab?.url },
                            isDesktopMode = uiState.isDesktopMode,
                            isDarkTheme = isDarkTheme,
                            onDismiss = { menuExpanded = false },
                            onNewTab = {
                                viewModel.createNewTab()
                                menuExpanded = false
                            },
                            onToggleBookmark = {
                                activeTab?.let {
                                    viewModel.toggleBookmark(it.title, it.url)
                                }
                                menuExpanded = false
                            },
                            onBookmarksClick = {
                                viewModel.setBookmarksOpen(true)
                                menuExpanded = false
                            },
                            onHistoryClick = {
                                viewModel.setHistoryOpen(true)
                                menuExpanded = false
                            },
                            onToggleDesktop = {
                                viewModel.toggleDesktopMode()
                                menuExpanded = false
                            },
                            onToggleDark = {
                                onToggleDarkTheme()
                                menuExpanded = false
                            },
                            onClearHistory = {
                                viewModel.clearAllHistory()
                                menuExpanded = false
                            },
                            onDpzxProjectClick = {
                                activeTab?.let { tab ->
                                    viewModel.updateTabUrl(tab.id, "https://dpzxproject.pages.dev")
                                    viewModel.updateTabTitle(tab.id, "DPZXPROJECT")
                                }
                                menuExpanded = false
                            }
                        )
                    }

                    // Progress Bar
                    if (activeTab?.isLoading == true && activeTab?.progress ?: 0 < 100) {
                        LinearProgressIndicator(
                            progress = { (activeTab?.progress ?: 0) / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp),
                            color = com.example.ui.theme.NeonPink,
                            trackColor = Color.Transparent
                        )
                    } else {
                        Spacer(modifier = Modifier.height(3.dp))
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main Webpage or Native Home screen
            if (activeTab != null) {
                if (activeTab!!.isNewTab) {
                    HomeScreen(
                        onSearchFocused = { isSearchEditing = true },
                        onNavigateToUrl = { url ->
                            viewModel.updateTabUrl(activeTab!!.id, url)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    BrowserScreen(
                        tabId = activeTab!!.id,
                        url = activeTab!!.url,
                        isDesktopMode = uiState.isDesktopMode,
                        onUrlChanged = { newUrl ->
                            viewModel.updateTabUrl(activeTab!!.id, newUrl)
                        },
                        onTitleChanged = { newTitle ->
                            viewModel.updateTabTitle(activeTab!!.id, newTitle)
                        },
                        onLoadingChanged = { loading ->
                            viewModel.updateTabLoading(activeTab!!.id, loading)
                        },
                        onProgressChanged = { progress ->
                            viewModel.updateTabProgress(activeTab!!.id, progress)
                        },
                        onNavigationStateChanged = { canBack, canForward ->
                            viewModel.updateTabNavigationState(activeTab!!.id, canBack, canForward)
                        },
                        onPageVisited = { title, url ->
                            viewModel.visitPage(title, url)
                        },
                        webViewsCache = webViewsCache,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Tab Switcher Page Overlay
            AnimatedVisibility(
                visible = uiState.isTabManagerOpen,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
            ) {
                TabManager(
                    tabs = uiState.tabs,
                    activeTabId = uiState.activeTabId,
                    onTabSelected = { tabId -> viewModel.selectTab(tabId) },
                    onTabClosed = { tabId ->
                        // Remove WebView from cache to avoid leak
                        webViewsCache.remove(tabId)?.destroy()
                        viewModel.closeTab(tabId)
                    },
                    onNewTab = { viewModel.createNewTab() },
                    onCloseAll = {
                        webViewsCache.values.forEach { it.destroy() }
                        webViewsCache.clear()
                        viewModel.closeAllTabs()
                    },
                    onBack = { viewModel.setTabManagerOpen(false) }
                )
            }

            // History Overlay
            AnimatedVisibility(
                visible = uiState.isHistoryOpen,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                HistoryScreen(
                    historyItems = history,
                    onItemClicked = { url ->
                        activeTab?.let { viewModel.updateTabUrl(it.id, url) }
                        viewModel.setHistoryOpen(false)
                    },
                    onDeleteItem = { id -> viewModel.deleteHistory(id) },
                    onClearAll = { viewModel.clearAllHistory() },
                    onBack = { viewModel.setHistoryOpen(false) }
                )
            }

            // Bookmarks Overlay
            AnimatedVisibility(
                visible = uiState.isBookmarksOpen,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                BookmarksScreen(
                    bookmarks = bookmarks,
                    onItemClicked = { url ->
                        activeTab?.let { viewModel.updateTabUrl(it.id, url) }
                        viewModel.setBookmarksOpen(false)
                    },
                    onDeleteBookmark = { bookmark ->
                        viewModel.toggleBookmark(bookmark.title, bookmark.url)
                    },
                    onBack = { viewModel.setBookmarksOpen(false) }
                )
            }

            // Focused Omnibox Fullscreen Editing Overlay
            AnimatedVisibility(
                visible = isSearchEditing,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                OmniboxEditingOverlay(
                    initialText = if (activeTab?.isNewTab == true) "" else (activeTab?.url ?: ""),
                    bookmarks = bookmarks,
                    history = history,
                    onSubmitted = { input ->
                        viewModel.onAddressSubmitted(input)
                        isSearchEditing = false
                    },
                    onDismiss = { isSearchEditing = false }
                )
            }
        }
    }
}

// Compact Neon-styled Cyber Browser Top Navigation Bar
@Composable
fun ChromeTopBar(
    activeTab: TabState?,
    tabsCount: Int,
    searchInput: String,
    isBookmarked: Boolean,
    onBackClick: () -> Unit,
    onForwardClick: () -> Unit,
    onHomeClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onOmniboxClick: () -> Unit,
    onTabBadgeClick: () -> Unit,
    onMenuClick: () -> Unit,
    onAiClick: () -> Unit
) {
    Surface(
        tonalElevation = 8.dp,
        color = com.example.ui.theme.CyberSurface,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                    colors = listOf(
                        com.example.ui.theme.NeonPink,
                        com.example.ui.theme.NeonCyan
                    )
                ),
                shape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Navigation Back (Neon Pink Highlight when active)
            IconButton(
                onClick = onBackClick,
                enabled = activeTab?.canGoBack == true,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    modifier = Modifier.size(20.dp),
                    tint = if (activeTab?.canGoBack == true) com.example.ui.theme.NeonPink else com.example.ui.theme.CyberMuted
                )
            }

            // Navigation Forward
            IconButton(
                onClick = onForwardClick,
                enabled = activeTab?.canGoForward == true,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Forward",
                    modifier = Modifier.size(20.dp),
                    tint = if (activeTab?.canGoForward == true) com.example.ui.theme.NeonCyan else com.example.ui.theme.CyberMuted
                )
            }

            // Home (Neon Lime)
            IconButton(
                onClick = onHomeClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Home",
                    modifier = Modifier.size(20.dp),
                    tint = com.example.ui.theme.NeonLime
                )
            }

            // High-Performance Glowing Address Box
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .padding(horizontal = 4.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(com.example.ui.theme.CyberSurfaceVariant)
                    .border(1.5.dp, com.example.ui.theme.NeonCyan, RoundedCornerShape(20.dp))
                    .clickable { onOmniboxClick() }
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (activeTab?.isNewTab == true || activeTab?.url?.startsWith("https") == false) Icons.Default.Info else Icons.Default.Lock,
                    contentDescription = "Security Status",
                    tint = if (activeTab?.url?.startsWith("https") == true) com.example.ui.theme.NeonLime else com.example.ui.theme.NeonPink,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (activeTab?.isNewTab == true) "Search or type URL" else (activeTab?.url ?: ""),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    ),
                    color = if (activeTab?.isNewTab == true) {
                        com.example.ui.theme.CyberMuted.copy(alpha = 0.6f)
                    } else {
                        com.example.ui.theme.GlowingWhite
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (activeTab?.isNewTab == false) {
                    IconButton(
                        onClick = onRefreshClick,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh page",
                            modifier = Modifier.size(16.dp),
                            tint = com.example.ui.theme.NeonCyan
                        )
                    }
                }
            }

            // Floating AI Assistant Button (Pulsing style)
            IconButton(
                onClick = onAiClick,
                modifier = Modifier
                    .size(36.dp)
                    .padding(horizontal = 2.dp)
                    .border(1.2.dp, com.example.ui.theme.NeonLime, CircleShape)
                    .background(com.example.ui.theme.NeonLime.copy(alpha = 0.15f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "AI Companion",
                    modifier = Modifier.size(16.dp),
                    tint = com.example.ui.theme.NeonLime
                )
            }

            // Tab Switcher Badge with glowing outline
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clickable { onTabBadgeClick() },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .border(1.5.dp, com.example.ui.theme.NeonPink, RoundedCornerShape(6.dp))
                        .background(com.example.ui.theme.NeonPink.copy(alpha = 0.1f), RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tabsCount.toString(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = com.example.ui.theme.NeonPink
                        )
                    )
                }
            }

            // Morevert action
            IconButton(
                onClick = onMenuClick,
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Browser Menu",
                    modifier = Modifier.size(20.dp),
                    tint = com.example.ui.theme.NeonPink
                )
            }
        }
    }
}

// Custom Cyberpunk Actions Menu
@Composable
fun ChromeDropdownMenu(
    expanded: Boolean,
    isBookmarked: Boolean,
    isDesktopMode: Boolean,
    isDarkTheme: Boolean,
    onDismiss: () -> Unit,
    onNewTab: () -> Unit,
    onToggleBookmark: () -> Unit,
    onBookmarksClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onToggleDesktop: () -> Unit,
    onToggleDark: () -> Unit,
    onClearHistory: () -> Unit,
    onDpzxProjectClick: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier
            .background(com.example.ui.theme.CyberSurface)
            .border(1.dp, com.example.ui.theme.CyberOutline, RoundedCornerShape(8.dp))
    ) {
        DropdownMenuItem(
            text = { Text("New Cyber Tab", color = com.example.ui.theme.GlowingWhite) },
            onClick = onNewTab
        )
        DropdownMenuItem(
            text = { Text(if (isBookmarked) "Remove Bookmark" else "Bookmark Neural Link", color = com.example.ui.theme.GlowingWhite) },
            leadingIcon = {
                Icon(
                    imageVector = if (isBookmarked) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = null,
                    tint = if (isBookmarked) com.example.ui.theme.NeonLime else com.example.ui.theme.CyberMuted
                )
            },
            onClick = onToggleBookmark
        )
        HorizontalDivider(color = com.example.ui.theme.CyberOutline)
        
        // Brand New Portal to DPZXPROJECT (Glowing pink option)
        DropdownMenuItem(
            text = { 
                Text(
                    text = "DPZXPROJECT PORTAL 🌌", 
                    color = com.example.ui.theme.NeonPink,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                ) 
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    tint = com.example.ui.theme.NeonPink
                )
            },
            onClick = onDpzxProjectClick
        )
        
        HorizontalDivider(color = com.example.ui.theme.CyberOutline)
        DropdownMenuItem(
            text = { Text("Bookmarks Log", color = com.example.ui.theme.GlowingWhite) },
            onClick = onBookmarksClick
        )
        DropdownMenuItem(
            text = { Text("History Node", color = com.example.ui.theme.GlowingWhite) },
            onClick = onHistoryClick
        )
        HorizontalDivider(color = com.example.ui.theme.CyberOutline)
        DropdownMenuItem(
            text = { Text(if (isDesktopMode) "Mobile Mode" else "Cyber Desktop Mode", color = com.example.ui.theme.GlowingWhite) },
            onClick = onToggleDesktop
        )
        DropdownMenuItem(
            text = { Text(if (isDarkTheme) "Light Terminal" else "Dark Cyber Terminal", color = com.example.ui.theme.GlowingWhite) },
            onClick = onToggleDark
        )
        DropdownMenuItem(
            text = {
                Text(
                    text = "Purge History Node",
                    color = com.example.ui.theme.NeonPink,
                    fontWeight = FontWeight.Bold
                )
            },
            onClick = onClearHistory
        )
    }
}

// Fullscreen Focused Search Overlay with suggestions
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OmniboxEditingOverlay(
    initialText: String,
    bookmarks: List<Bookmark>,
    history: List<HistoryItem>,
    onSubmitted: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(initialText) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    val filteredSuggestions = remember(text, bookmarks, history) {
        if (text.isBlank()) {
            emptyList()
        } else {
            val matchedBookmarks = bookmarks.filter {
                it.title.contains(text, ignoreCase = true) || it.url.contains(text, ignoreCase = true)
            }.map { it.url to it.title }
            
            val matchedHistory = history.filter {
                it.title.contains(text, ignoreCase = true) || it.url.contains(text, ignoreCase = true)
            }.map { it.url to it.title }

            (matchedBookmarks + matchedHistory)
                .distinctBy { it.first }
                .take(6)
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Search toolbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                }
                
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester)
                        .testTag("omnibox_input_field"),
                    placeholder = { Text("Search or type URL") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            if (text.isNotBlank()) {
                                onSubmitted(text)
                            }
                        }
                    ),
                    trailingIcon = {
                        if (text.isNotEmpty()) {
                            IconButton(onClick = { text = "" }) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    }
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Suggestions List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                // If query is empty, let's show visual instruction
                if (text.isBlank()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Type to search Google",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "or enter a complete web address URL.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                } else {
                    // Search item for raw typed text
                    item {
                        SuggestionRow(
                            title = "Search Google for \"$text\"",
                            url = "Google Search",
                            isSearchType = true,
                            onClick = { onSubmitted(text) }
                        )
                    }

                    // Matching Bookmark / History items
                    items(filteredSuggestions) { suggestion ->
                        SuggestionRow(
                            title = suggestion.second,
                            url = suggestion.first,
                            isSearchType = false,
                            onClick = { onSubmitted(suggestion.first) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SuggestionRow(
    title: String,
    url: String,
    isSearchType: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isSearchType) Icons.Default.Search else Icons.Default.Language,
            contentDescription = null,
            tint = if (isSearchType) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSearchType) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (!isSearchType) {
                Text(
                    text = url,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}
