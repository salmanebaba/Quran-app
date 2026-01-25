package com.example.quran

import android.content.Context
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStreamReader
import java.text.Normalizer

object QuranRepository {
    // Key = Surah Number (1-114), Value = List of Ayahs in that Surah
    private var quranBySurah: Map<Int, List<Ayah>> = emptyMap()

    // NEW: A search index to store the "clean" version of every Ayah for fast searching
    private var searchIndex: List<Pair<String, Ayah>> = emptyList()

    var isReady: Boolean = false

    suspend fun loadQuran(context: Context) {
        if (isReady) return

        withContext(Dispatchers.IO) {
            try {
                context.assets.open("quran-uthmani-quran-academy").use { inputStream ->
                    InputStreamReader(inputStream).use { reader ->
                        val response = Gson().fromJson(reader, QuranResponse::class.java)

                        val tempMap = mutableMapOf<Int, List<Ayah>>()
                        val tempSearchIndex = mutableListOf<Pair<String, Ayah>>()

                        response.data.surahs.forEach { surah ->
                            surah.ayahs.forEach { ayah ->
                                ayah.surahName = surah.name
                                ayah.surahNumber = surah.number

                                // 1. Fix the "return line" issue
                                ayah.text = ayah.text.trim()

                                // 2. Create a "clean" version for search (Remove Tachkil & Tajwid)
                                val cleanText = normalizeArabic(ayah.text)
                                tempSearchIndex.add(Pair(cleanText, ayah))
                            }
                            tempMap[surah.number] = surah.ayahs
                        }

                        quranBySurah = tempMap
                        searchIndex = tempSearchIndex // Save the index
                        isReady = true
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getSurah(surahNumber: Int): List<Ayah> {
        return quranBySurah[surahNumber] ?: emptyList()
    }

    fun searchAyahs(query: String): List<Ayah> {
        if (query.length < 2) return emptyList() // Lowered limit slightly for Arabic roots

        // 1. Normalize the USER'S query (remove tachkil if they typed it)
        val normalizedQuery = normalizeArabic(query)

        val results = mutableListOf<Ayah>()

        // 2. Search in our pre-calculated index
        searchIndex.forEach { (cleanText, ayah) ->
            if (cleanText.contains(normalizedQuery)) {
                results.add(ayah)
            }
        }
        return results
    }

    // HELPER: Removes Tashkeel, Tajwid marks, and Quranic symbols
    private fun normalizeArabic(text: String): String {
        var t = text

        // 1. Remove Tashkeel (Diacritics: Fatha, Damma, Kasra, Shadda, Sukun, etc.)
        // Range \u064B-\u065F covers standard Tashkeel
        // Range \u0670 is the small superscript Aleph (used in Rahman)
        // Range \u06D6-\u06ED covers Quranic stop marks
        t = t.replace(Regex("[\u064B-\u065F\u0670\u06D6-\u06ED]"), "")

        // 2. Remove Tatweel (Kashida) _
        t = t.replace(Regex("\u0640"), "")

        // 3. Normalize Aleph Forms
        // Replaces:
        // ٱ (Aleph Wasla - standard in Uthmani)
        // أ (Aleph with Hamza Above)
        // إ (Aleph with Hamza Below)
        // آ (Aleph with Madda)
        // WITH plain ا (Aleph)
        t = t.replace(Regex("[\u0622\u0623\u0625\u0671]"), "\u0627")

        // 4. Normalize Ya / Alif Maqsura
        // Users often type ي for ى or vice versa. We normalize both to ي (\u064A)
        // ى (\u0649) -> ي (\u064A)
        t = t.replace('\u0649', '\u064A')

        // 5. Normalize Ta Marbuta
        // ة (\u0629) -> ه (\u0647)
        t = t.replace('\u0629', '\u0647')

        // 6. Remove any remaining non-letter characters (like BOM)
        t = t.replace("\ufeff", "").trim()

        return t
    }
}