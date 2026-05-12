package com.salmane.quran

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import androidx.core.content.edit

class QuranViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("SimpleQuranData", Context.MODE_PRIVATE)

    var currentSurah by mutableIntStateOf(0)
    var currentAyahs by mutableStateOf(emptyList<Ayah>())
    var targetScrollIndex by mutableIntStateOf(0)
    var isDataLoaded by mutableStateOf(false)
    var isDndPref by mutableStateOf(prefs.getBoolean("dnd_pref", false))
    var isDarkMode by mutableStateOf(prefs.getBoolean("dark_mode", false))
    var isEnglish by mutableStateOf(prefs.getBoolean("is_english", false))
    var fontSize by mutableStateOf(prefs.getFloat("font_size", 26f))
    var showBookmarksDialog by mutableStateOf(false)
    var showSettingsDialog by mutableStateOf(false)
    var showGoToDialog by mutableStateOf(false)
    var bookmarksList by mutableStateOf(emptyList<Bookmark>())

    // Auto Scroll Speed state
    var scrollSpeed by mutableStateOf(prefs.getFloat("scroll_speed", 5f))

    // Tracking general app entry time
    var lastEntryTimestamp by mutableStateOf(prefs.getLong("last_entry_timestamp_display", 0L))
    // Search State
    var searchQuery by mutableStateOf("")
    var searchResults by mutableStateOf(emptyList<Ayah>())

    // Tracks if we opened a specific bookmark so we can update it (session mode)
    var activeBookmarkTimestamp by mutableStateOf<Long?>(null)

    init {
        loadData()
        updateSessionTimestamp()
    }

    private fun loadData() {
        viewModelScope.launch {
            QuranRepository.loadQuran(getApplication())
            isDataLoaded = true
        }
    }

    private fun updateSessionTimestamp() {
        // The display value is what was saved in the previous session
        val lastSaved = prefs.getLong("last_entry_timestamp_internal", 0L)
        lastEntryTimestamp = lastSaved

        // Update the display persistent storage so it stays valid for the whole current session
        prefs.edit { putLong("last_entry_timestamp_display", lastSaved) }

        // Save current time as the "internal" one for the NEXT app launch
        prefs.edit { putLong("last_entry_timestamp_internal", System.currentTimeMillis()) }
    }


    fun startNewKhatma() {
        currentSurah = 1
        loadSurahContent(1)
        targetScrollIndex = 0
        activeBookmarkTimestamp = null
        saveLastReadPosition(1, 0)
        clearSearch()
    }

    fun resumeLastPlace() {
        val surah = prefs.getInt("last_visit_surah", 1)
        val index = prefs.getInt("last_visit_index", 0)
        currentSurah = surah
        loadSurahContent(surah)
        targetScrollIndex = index
        activeBookmarkTimestamp = null
        clearSearch()
    }

    private fun loadSurahContent(surahNumber: Int) {
        currentAyahs = QuranRepository.getSurah(surahNumber)
    }

    fun onSearchQueryChange(query: String) {
        searchQuery = query
        searchResults = QuranRepository.searchAyahs(query)
    }

    fun clearSearch() {
        searchQuery = ""
        searchResults = emptyList()
    }

    fun onSearchResultClick(ayah: Ayah) {
        currentSurah = ayah.surahNumber
        loadSurahContent(ayah.surahNumber)
        // The LazyColumn renders ayahs directly from the list (no separate Bismillah item),
        // so list index is always numberInSurah - 1 for every surah.
        targetScrollIndex = ayah.numberInSurah - 1
        activeBookmarkTimestamp = null
        saveLastReadPosition(ayah.surahNumber, targetScrollIndex)
    }

    fun openBookmark(bookmark: Bookmark) {
        currentSurah = bookmark.surahNumber
        loadSurahContent(bookmark.surahNumber)
        targetScrollIndex = bookmark.ayahIndex
        activeBookmarkTimestamp = bookmark.timestamp
        showBookmarksDialog = false
        saveLastReadPosition(bookmark.surahNumber, bookmark.ayahIndex)
        clearSearch()
    }

    fun openBookmarks() {
        bookmarksList = BookmarkHelper.getBookmarks(getApplication())
        showBookmarksDialog = true
    }

    fun toggleDnd(shouldEnable: Boolean) {
        if (shouldEnable) {
            if (DndHelper.isPermissionGranted(getApplication())) {
                isDndPref = true
                prefs.edit { putBoolean("dnd_pref", true) }
            } else {
                DndHelper.requestPermission(getApplication())
            }
        } else {
            isDndPref = false
            prefs.edit { putBoolean("dnd_pref", false) }
        }
    }

    fun onSurahChange(newSurah: Int) {
        currentSurah = newSurah
        loadSurahContent(newSurah)
        targetScrollIndex = 0
        saveLastReadPosition(newSurah, 0)
    }

    fun saveBookmark(name: String, index: Int) {
        val oldTimestamp = activeBookmarkTimestamp
        val newTimestamp = System.currentTimeMillis()

        // Preserve custom name if updating an existing session bookmark
        var finalName = name
        if (oldTimestamp != null) {
            val existing = BookmarkHelper.getBookmarks(getApplication()).find { it.timestamp == oldTimestamp }
            if (existing != null) {
                finalName = existing.surahName
            }
        }

        val bm = Bookmark(finalName,currentSurah, index, timestamp = newTimestamp)

        BookmarkHelper.saveOrUpdateBookmark(getApplication(), bm, oldTimestamp)

        activeBookmarkTimestamp = newTimestamp
        bookmarksList = BookmarkHelper.getBookmarks(getApplication())

        // Also update last read position whenever a bookmark is saved
        saveLastReadPosition(currentSurah, index)
    }

    fun updateLastReadIndex(index: Int) {
        if (currentSurah != 0) {
            targetScrollIndex = index
            saveLastReadPosition(currentSurah, index)
        }
    }

    fun saveLastReadPosition(surah: Int, index: Int) {
        prefs.edit {
            putInt("last_visit_surah", surah)
                .putInt("last_visit_index", index)
        }
    }

    fun renameBookmark(bookmark: Bookmark, newName: String) {
        BookmarkHelper.renameBookmark(getApplication(), bookmark, newName)
        bookmarksList = BookmarkHelper.getBookmarks(getApplication())
    }

    fun exitReading() {
        currentSurah = 0
        activeBookmarkTimestamp = null
    }

    fun goHome() {
        exitReading()
        clearSearch()
    }

    fun jumpToAyah(surah: Int, ayah: Int) {
        currentSurah = surah
        loadSurahContent(surah)
        // Adjust for Bismillah (index 0 is Bismillah if not 1 or 9)
        // Ayah 1 -> index 1 if Bismillah, index 0 if not.
        targetScrollIndex = ayah - 1
        saveLastReadPosition(surah, targetScrollIndex)
        showGoToDialog = false
    }

    fun updateScrollSpeed(newSpeed: Float) {
        scrollSpeed = newSpeed
        prefs.edit { putFloat("scroll_speed", newSpeed) }
    }

    fun toggleDarkMode(enabled: Boolean) {
        isDarkMode = enabled
        prefs.edit { putBoolean("dark_mode", enabled) }
    }

    fun toggleLanguage(english: Boolean) {
        isEnglish = english
        prefs.edit { putBoolean("is_english", english) }
    }

    fun updateFontSize(newSize: Float) {
        fontSize = newSize
        prefs.edit { putFloat("font_size", newSize) }
    }
}