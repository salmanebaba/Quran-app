package com.example.quran

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            QuranApp()
        }
    }
}

// ==========================================
// 1. MAIN NAVIGATION & APP STATE
// ==========================================

@Composable
fun QuranApp(viewModel: QuranViewModel = viewModel()) {
    val context = LocalContext.current

    if (!viewModel.isDataLoaded) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Loading Quran...")
            }
        }
    } else {
        // --- BOOKMARKS POPUP ---
        if (viewModel.showBookmarksDialog) {
            BookmarksDialog(
                bookmarks = viewModel.bookmarksList,
                onDismiss = { viewModel.showBookmarksDialog = false },
                onSelect = { bookmark ->
                    viewModel.openBookmark(bookmark)
                },
                onDelete = { bookmark ->
                    BookmarkHelper.deleteBookmark(context, bookmark)
                    viewModel.bookmarksList = BookmarkHelper.getBookmarks(context)
                },
                onRename = { bookmark, newName ->
                    viewModel.renameBookmark(bookmark, newName)
                }
            )
        }

        // --- SCREENS ---
        if (viewModel.currentSurah == 0) {
            HomeScreen(
                onNew = { viewModel.startNewKhatma() },
                onContinue = { viewModel.resumeLastPlace() },
                onOpenBookmarks = { viewModel.openBookmarks() },
                isDndEnabled = viewModel.isDndPref,
                onDndToggle = { viewModel.toggleDnd(it) },
                searchQuery = viewModel.searchQuery,
                onSearchQueryChange = { viewModel.onSearchQueryChange(it) },
                searchResults = viewModel.searchResults,
                onSearchResultClick = { viewModel.onSearchResultClick(it) },
                onClearSearch = { viewModel.clearSearch() },
                lastEntryTimestamp = viewModel.lastEntryTimestamp
            )
        } else {
            // key(currentSurah) forces a refresh when Surah changes, preventing scroll issues
            key(viewModel.currentSurah) {
                ReadingScreen(
                    surahNumber = viewModel.currentSurah,
                    ayahs = viewModel.currentAyahs,
                    initialAyahIndex = viewModel.targetScrollIndex,
                    isDndPreferenceOn = viewModel.isDndPref,
                    onSurahChange = { viewModel.onSurahChange(it) },
                    onSaveBookmark = { name, index ->
                        viewModel.saveBookmark(name, index)
                        android.widget.Toast.makeText(context, "Bookmark Updated", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    onBack = { viewModel.exitReading() },
                    onGoHome = { viewModel.goHome() }
                )
            }
        }
    }
}

// ==========================================
// 2. HOME SCREEN
// ==========================================

@Composable
fun HomeScreen(
    onNew: () -> Unit,
    onContinue: () -> Unit,
    onOpenBookmarks: () -> Unit,
    isDndEnabled: Boolean,
    onDndToggle: (Boolean) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    searchResults: List<Ayah>,
    onSearchResultClick: (Ayah) -> Unit,
    onClearSearch: () -> Unit,
    lastEntryTimestamp: Long
) {
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // General "Last App Entry" Info
        if (lastEntryTimestamp > 0) {
            Text(
                text = "Last Visit: ${StringUtils.formatRelativeTime(lastEntryTimestamp)}",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.align(Alignment.End)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
        Text("القرآن الكريم", fontSize = 40.sp, fontWeight = FontWeight.Bold)


        Spacer(modifier = Modifier.height(24.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("البحت عن آية") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = onClearSearch) {
                        Icon(Icons.Default.Close, contentDescription = "Clear search")
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        if (searchQuery.isNotEmpty()) {
            // Search Results List
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(top = 8.dp)
            ) {
                items(searchResults, key = { it.number }) { ayah ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onSearchResultClick(ayah) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = ayah.text,
                                style = TextStyle(textDirection = TextDirection.Rtl, fontSize = 18.sp),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${ayah.surahName} | Ayah ${ayah.numberInSurah}",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
                if (searchResults.isEmpty() && searchQuery.length >= 3) {
                    item {
                        Text("No results found", modifier = Modifier.padding(16.dp), color = Color.Gray)
                    }
                }
            }
        } else {
            // Main Buttons (only show when not searching)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(onClick = onNew, modifier = Modifier.width(220.dp).height(50.dp)) { Text("Start New Khatma") }
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onContinue, modifier = Modifier.width(220.dp).height(50.dp)) { Text("Resume Last Place") }
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onOpenBookmarks, modifier = Modifier.width(220.dp).height(50.dp)) { Text("Saved Bookmarks") }

                Spacer(modifier = Modifier.height(40.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Auto Do Not Disturb", fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.width(12.dp))
                    Switch(checked = isDndEnabled, onCheckedChange = onDndToggle)
                }
                if (isDndEnabled) {
                    Text("(Activates while reading)", fontSize = 12.sp, color = Color.Gray)
                }
            }
        }
    }
}

// ==========================================
// 3. READING SCREEN (The Core)
// ==========================================

@Composable
fun ReadingScreen(
    surahNumber: Int,
    ayahs: List<Ayah>,
    initialAyahIndex: Int,
    isDndPreferenceOn: Boolean,
    onSurahChange: (Int) -> Unit,
    onSaveBookmark: (String, Int) -> Unit,
    onBack: () -> Unit,
    onGoHome: () -> Unit
) {
    BackHandler {
        onBack()
    }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialAyahIndex)

    // --- Logic Helpers ---
    DndHandler(isDndPreferenceOn) // Handles Lifecycle DND logic

    // Header Info
    val firstVisibleIndex by remember { derivedStateOf { listState.firstVisibleItemIndex } }
    val layoutInfo by remember { derivedStateOf { listState.layoutInfo } }
    val totalItems by remember { derivedStateOf { layoutInfo.totalItemsCount } }
    val visibleItemsCount by remember { derivedStateOf { layoutInfo.visibleItemsInfo.size } }

    // Adjust index for Bismillah (if present, it shifts indices by 1)
    val adjustedIndex = if (surahNumber != 1 && surahNumber != 9)
        (firstVisibleIndex - 1).coerceAtLeast(0)
    else firstVisibleIndex

    val currentAyah = ayahs.getOrNull(adjustedIndex)
    val surahName = ayahs.firstOrNull()?.surahName ?: ""
    val currentHizbQuarter = currentAyah?.hizbQuarter ?: ayahs.firstOrNull()?.hizbQuarter ?: 0

    // UI States
    var showMenu by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var isAutoScrolling by remember { mutableStateOf(false) }
    var scrollSpeed by remember { mutableFloatStateOf(3f) }

    // Auto Scroll Logic
    LaunchedEffect(isAutoScrolling, scrollSpeed) {
        while (isAutoScrolling) {
            listState.scrollBy(scrollSpeed)
            delay(40)
        }
    }

    // Speed Dialog
    if (showSpeedDialog) {
        SpeedControlDialog(
            currentSpeed = scrollSpeed,
            onSpeedChange = { scrollSpeed = it },
            onDismiss = { showSpeedDialog = false }
        )
    }

    Scaffold(
        topBar = {
            ReadingTopBar(
                surahName = surahName,
                hizbQuarter = currentHizbQuarter,
                onBack = onBack,
                onPrevSurah = { if (surahNumber > 1) onSurahChange(surahNumber - 1) },
                onNextSurah = { if (surahNumber < 114) onSurahChange(surahNumber + 1) },
                hasPrev = surahNumber > 1,
                hasNext = surahNumber < 114,
                onMenuClick = { showMenu = true }
            )

            Box(modifier = Modifier.fillMaxWidth().wrapContentSize(Alignment.TopEnd)) {
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    DropdownMenuItem(
                        text = { Text("Save Bookmark") },
                        onClick = { onSaveBookmark(surahName, firstVisibleIndex); showMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text(if (isAutoScrolling) "Stop Scroll" else "Auto Scroll") },
                        onClick = { isAutoScrolling = !isAutoScrolling; showMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Adjust Speed") },
                        onClick = { showSpeedDialog = true; showMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Go Home") },
                        onClick = { showMenu = false; onGoHome() }
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                if (surahNumber != 1 && surahNumber != 9) {
                    item(key = "bismillah") {
                        Text(
                            text = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ",
                            style = TextStyle(fontSize = 30.sp, textAlign = TextAlign.Center),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                        )
                    }
                }

                items(ayahs, key = { it.number }) { ayah ->
                    val displayText = if (ayah.numberInSurah == 1 && surahNumber != 1) {
                        StringUtils.removeBismillahPrefix(ayah.text)
                    } else {
                        ayah.text
                    }

                    Text(
                        text = StringUtils.buildSingleAyahText(displayText, ayah.numberInSurah),
                        style = TextStyle(
                            fontSize = 26.sp,
                            lineHeight = 48.sp,
                            textAlign = TextAlign.Center,
                            textDirection = TextDirection.Rtl
                        ),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                    )
                }

                item(key = "footer") {
                    if (surahNumber < 114) {
                        Button(
                            onClick = { onSurahChange(surahNumber + 1) },
                            modifier = Modifier.fillMaxWidth().padding(top = 24.dp)
                        ) { Text("Next Surah") }
                    }
                }
            }

            // --- Scroll Indicator Line (Untouchable) ---
            if (totalItems > 0 && visibleItemsCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 4.dp, top = 16.dp, bottom = 16.dp)
                        .width(4.dp)
                        .background(Color.Gray.copy(alpha = 0.1f), RoundedCornerShape(2.dp))
                ) {
                    val topWeight = firstVisibleIndex.toFloat()
                    val thumbWeight = visibleItemsCount.toFloat()
                    val bottomWeight = (totalItems.toFloat() - topWeight - thumbWeight).coerceAtLeast(0.01f)

                    Column(modifier = Modifier.fillMaxSize()) {
                        if (topWeight > 0) Spacer(modifier = Modifier.weight(topWeight))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(thumbWeight)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(2.dp))
                        )
                        if (bottomWeight > 0) Spacer(modifier = Modifier.weight(bottomWeight))
                    }
                }
            }
        }
    }
}

@Composable
fun ReadingTopBar(
    surahName: String,
    hizbQuarter: Int,
    onBack: () -> Unit,
    onPrevSurah: () -> Unit,
    onNextSurah: () -> Unit,
    hasPrev: Boolean,
    hasNext: Boolean,
    onMenuClick: () -> Unit
) {
    Surface(tonalElevation = 2.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(surahName, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(
                    text = StringUtils.getHizbArabicText(hizbQuarter),
                    fontSize = 14.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPrevSurah, enabled = hasPrev) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Prev", tint = if (hasPrev) MaterialTheme.colorScheme.primary else Color.Gray)
                }
                IconButton(onClick = onNextSurah, enabled = hasNext) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next", tint = if (hasNext) MaterialTheme.colorScheme.primary else Color.Gray)
                }
                IconButton(onClick = onMenuClick) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                }
            }
        }
    }
}

@Composable
fun SpeedControlDialog(currentSpeed: Float, onSpeedChange: (Float) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Auto Scroll Speed") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "${currentSpeed.toInt()}", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("pixels / tick", fontSize = 12.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { onSpeedChange((currentSpeed - 1f).coerceAtLeast(1f)) },
                        shape = CircleShape,
                        modifier = Modifier.size(40.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) { Text("-", fontWeight = FontWeight.Bold) }

                    Slider(
                        value = currentSpeed,
                        onValueChange = onSpeedChange,
                        valueRange = 1f..15f,
                        steps = 14,
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                    )

                    OutlinedButton(
                        onClick = { onSpeedChange((currentSpeed + 1f).coerceAtMost(15f)) },
                        shape = CircleShape,
                        modifier = Modifier.size(40.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) { Text("+", fontWeight = FontWeight.Bold) }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )
}

@Composable
fun BookmarksDialog(
    bookmarks: List<Bookmark>,
    onDismiss: () -> Unit,
    onSelect: (Bookmark) -> Unit,
    onDelete: (Bookmark) -> Unit,
    onRename: (Bookmark, String) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }
    var bookmarkToRename by remember { mutableStateOf<Bookmark?>(null) }
    var newName by remember { mutableStateOf("") }

    if (bookmarkToRename != null) {
        AlertDialog(
            onDismissRequest = { bookmarkToRename = null },
            title = { Text("Rename Bookmark") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("New Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    bookmarkToRename?.let { onRename(it, newName) }
                    bookmarkToRename = null
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { bookmarkToRename = null }) { Text("Cancel") }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Saved Bookmarks") },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                items(bookmarks, key = { it.timestamp }) { bookmark ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(bookmark) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(bookmark.surahName, fontWeight = FontWeight.Bold)
                            Text(
                                "Ayah ${bookmark.ayahIndex + 1} • ${dateFormat.format(Date(bookmark.timestamp))}",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                        Row {
                            IconButton(onClick = {
                                bookmarkToRename = bookmark
                                newName = bookmark.surahName
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Rename", tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                            }
                            IconButton(onClick = { onDelete(bookmark) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.6f))
                            }
                        }
                    }
                    HorizontalDivider()
                }
                if (bookmarks.isEmpty()) item { Text("No bookmarks yet.") }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

// ==========================================
// 5. HELPER LOGIC (Clean Code)
// ==========================================

// --- DND LOGIC ---
@Composable
fun DndHandler(isDndOn: Boolean) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, isDndOn) {
        val observer = LifecycleEventObserver { _, event ->
            if (isDndOn) {
                when (event) {
                    Lifecycle.Event.ON_RESUME -> DndHelper.enableDnd(context)
                    Lifecycle.Event.ON_PAUSE -> DndHelper.disableDnd(context)
                    else -> {}
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            if (isDndOn) DndHelper.disableDnd(context)
        }
    }
}

object DndHelper {
    fun isPermissionGranted(context: Context): Boolean {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return nm.isNotificationPolicyAccessGranted
    }

    fun requestPermission(context: Context) {
        val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun enableDnd(context: Context) {
        if (isPermissionGranted(context)) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            // Use INTERRUPTION_FILTER_PRIORITY instead of NONE to avoid "Total Silence" which can sometimes
            // cause issues with system alarms or be perceived as "crashing" the audio/system behavior.
            nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
        }
    }

    fun disableDnd(context: Context) {
        if (isPermissionGranted(context)) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
        }
    }
}

// --- BOOKMARK LOGIC ---
data class Bookmark(val surahNumber: Int, var surahName: String, val ayahIndex: Int, val timestamp: Long)

object BookmarkHelper {
    fun saveOrUpdateBookmark(context: Context, bookmark: Bookmark, oldTimestamp: Long?) {
        val prefs = context.getSharedPreferences("SimpleQuranData", Context.MODE_PRIVATE)
        val gson = Gson()
        val list = getBookmarks(context).toMutableList()

        if (oldTimestamp != null) {
            list.removeAll { it.timestamp == oldTimestamp }
        }

        list.add(0, bookmark)
        prefs.edit().putString("bookmarks_list", gson.toJson(list)).apply()
    }

    fun deleteBookmark(context: Context, bookmark: Bookmark) {
        val prefs = context.getSharedPreferences("SimpleQuranData", Context.MODE_PRIVATE)
        val gson = Gson()
        val list = getBookmarks(context).toMutableList()
        list.removeAll { it.timestamp == bookmark.timestamp }
        prefs.edit().putString("bookmarks_list", gson.toJson(list)).apply()
    }

    fun renameBookmark(context: Context, bookmark: Bookmark, newName: String) {
        val prefs = context.getSharedPreferences("SimpleQuranData", Context.MODE_PRIVATE)
        val gson = Gson()
        val list = getBookmarks(context).toMutableList()
        val index = list.indexOfFirst { it.timestamp == bookmark.timestamp }
        if (index != -1) {
            list[index] = list[index].copy(surahName = newName)
            prefs.edit().putString("bookmarks_list", gson.toJson(list)).apply()
        }
    }

    fun getBookmarks(context: Context): List<Bookmark> {
        val prefs = context.getSharedPreferences("SimpleQuranData", Context.MODE_PRIVATE)
        val json = prefs.getString("bookmarks_list", "[]")
        return try {
            val type = object : TypeToken<List<Bookmark>>() {}.type
            Gson().fromJson(json, type) ?: emptyList()
        } catch (e: Exception) { emptyList() }
    }
}

// --- STRING & FORMATTING LOGIC ---
object StringUtils {
    fun formatRelativeTime(timestamp: Long): String {
        if (timestamp == 0L) return ""
        val diff = System.currentTimeMillis() - timestamp
        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            days > 0 -> "$days day${if (days > 1) "s" else ""} ago"
            hours > 0 -> "$hours hour${if (hours > 1) "s" else ""} ago"
            minutes > 0 -> "$minutes minute${if (minutes > 1) "s" else ""} ago"
            else -> "Just now"
        }
    }

    fun getHizbArabicText(quarterIndex: Int): String {
        if (quarterIndex == 0) return ""
        val hizb = ((quarterIndex - 1) / 4) + 1
        val remainder = (quarterIndex - 1) % 4
        val fraction = when (remainder) {
            1 -> "1/4"
            2 -> "1/2"
            3 -> "3/4"
            else -> ""
        }
        return if (fraction.isNotEmpty()) " ح $hizb | $fraction" else "$hizb ح"
    }

    fun removeBismillahPrefix(text: String): String {
        val bismillah = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ"
        val cleanText = text.replace("\ufeff", "").trim()
        return if (cleanText.startsWith(bismillah)) cleanText.removePrefix(bismillah).trim() else text
    }

    fun buildSingleAyahText(text: String, number: Int) = buildAnnotatedString {
        append(text)
        withStyle(style = SpanStyle(color = Color(0xFFB48E43))) {
            append(" \uFD3F$number\uFD3E")
        }
    }
}