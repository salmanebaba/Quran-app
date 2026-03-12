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
    @SerializedName("number") val number: Int,
    @SerializedName("text") var text: String,
    @SerializedName("numberInSurah") val numberInSurah: Int,
    @SerializedName("page") val page: Int,
    @SerializedName("hizbQuarter") val hizbQuarter: Int,
    @SerializedName("juz") val juz: Int,

    // Add these so they don't get lost in Signed APK
    @SerializedName("surahName") var surahName: String = "",
    @SerializedName("surahNumber") var surahNumber: Int = 0,
    @SerializedName("normalizedText") var normalizedText: String = ""
)

data class SurahMetadata(val number: Int, val name: String, val ayahCount: Int)

data class Bookmark(
    @SerializedName("surahName") val surahName: String, @SerializedName("surahNumber") val surahNumber: Int,
    @SerializedName("ayahIndex") val ayahIndex: Int,
    @SerializedName("timestamp") val timestamp: Long
)