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
    var text: String,        // The Arabic text
    val numberInSurah: Int,
    val page: Int,           // Page 1 to 604
    val hizbQuarter: Int,    // For your 1/4 Hizb marks
    val juz: Int,
    var surahName: String = "",
    var surahNumber: Int = 0,
)
