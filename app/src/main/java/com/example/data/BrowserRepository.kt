package com.example.data

import kotlinx.coroutines.flow.Flow

class BrowserRepository(private val database: BrowserDatabase) {

    private val historyDao = database.historyDao()
    private val bookmarkDao = database.bookmarkDao()
    private val tabDao = database.tabDao()

    // --- History ---
    val allHistory: Flow<List<HistoryItem>> = historyDao.getAllHistory()

    suspend fun addHistoryItem(title: String, url: String) {
        if (url.isBlank() || url.startsWith("about:blank")) return
        // To avoid duplicates, we could clean up existing history with the same URL (optional, but nice)
        historyDao.insertHistoryItem(
            HistoryItem(
                title = if (title.isBlank()) url else title,
                url = url,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteHistoryItem(id: Long) {
        historyDao.deleteHistoryItem(id)
    }

    suspend fun clearHistory() {
        historyDao.clearAllHistory()
    }

    // --- Bookmarks ---
    val allBookmarks: Flow<List<Bookmark>> = bookmarkDao.getAllBookmarks()

    suspend fun isBookmarked(url: String): Boolean {
        return bookmarkDao.isBookmarked(url)
    }

    suspend fun toggleBookmark(title: String, url: String) {
        if (url.isBlank()) return
        val existing = bookmarkDao.getBookmarkByUrl(url)
        if (existing != null) {
            bookmarkDao.deleteBookmark(existing)
        } else {
            bookmarkDao.insertBookmark(
                Bookmark(
                    title = if (title.isBlank()) url else title,
                    url = url,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun addBookmark(title: String, url: String) {
        bookmarkDao.insertBookmark(Bookmark(title = title, url = url))
    }

    suspend fun removeBookmarkByUrl(url: String) {
        bookmarkDao.deleteBookmarkByUrl(url)
    }

    // --- Tabs ---
    val allTabs: Flow<List<TabItem>> = tabDao.getAllTabs()

    suspend fun saveTab(tab: TabItem) {
        tabDao.insertTab(tab)
    }

    suspend fun saveTabs(tabs: List<TabItem>) {
        tabDao.insertTabs(tabs)
    }

    suspend fun deleteTab(id: String) {
        tabDao.deleteTab(id)
    }

    suspend fun clearTabs() {
        tabDao.clearAllTabs()
    }
}
