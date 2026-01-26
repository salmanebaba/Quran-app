package com.example.quran

import com.google.gson.annotations.SerializedName

// Matches the "surahs" list in your file
data class QuranResponse(
    val data: QuranDataWrapper
)

data class QuranDataWrapper(
    val surahs: List<Surah>
)

data class Surah(
    val number: Int,
    val name: String, // Arabic name like "سورة الفاتحة"
    val ayahs: List<Ayah>
)

data class Ayah(
    val number: Int,
    var text: String,        // The Uthmani Text (Beautiful, for Display)
    val numberInSurah: Int,
    val page: Int,
    val hizbQuarter: Int,
    val juz: Int,

    // Helper fields
    var surahName: String = "",
    var surahNumber: Int = 0,
    var normalizedText: String = "" // This will now hold the "Clean" text from the new file
)