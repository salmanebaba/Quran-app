package com.example.quran

import android.content.Context
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStreamReader
import java.text.Normalizer

object QuranRepository {
    // Key = Surah Number (1-114), Value = List of Ayahs
    private var quranBySurah: Map<Int, List<Ayah>> = emptyMap()
    var isReady: Boolean = false

    suspend fun loadQuran(context: Context) {
        if (isReady) return

        withContext(Dispatchers.IO) {
            try {
                val gson = Gson()

                // 1. LOAD UTHMANI (For Display)
                val uthmaniStream = context.assets.open("quran-uthmani-quran-academy")
                val uthmaniResponse = gson.fromJson(InputStreamReader(uthmaniStream), QuranResponse::class.java)
                uthmaniStream.close()

                // 2. LOAD CLEAN SIMPLE TEXT (For Search)
                // Assumes file is named "quran-simple-clean" in assets
                val simpleStream = context.assets.open("quran-simple-clean")
                val simpleResponse = gson.fromJson(InputStreamReader(simpleStream), QuranResponse::class.java)
                simpleStream.close()

                // 3. MERGE THEM
                val tempMap = mutableMapOf<Int, List<Ayah>>()

                // Loop through Uthmani Surahs
                uthmaniResponse.data.surahs.forEachIndexed { sIndex, surah ->
                    // Get matching Clean Surah
                    val cleanSurah = simpleResponse.data.surahs.getOrNull(sIndex)

                    surah.ayahs.forEachIndexed { aIndex, ayah ->
                        // Basic Info
                        ayah.text = ayah.text.replace("\n", "").trim()
                        ayah.surahName = surah.name
                        ayah.surahNumber = surah.number

                        // INJECT CLEAN TEXT
                        // We grab the text from the simple file and put it into 'normalizedText'
                        val cleanText = cleanSurah?.ayahs?.getOrNull(aIndex)?.text ?: ""

                        // Small fix: Remove BOM (\ufeff) if present in clean text
                        ayah.normalizedText = cleanText.replace("\ufeff", "").trim()
                    }
                    tempMap[surah.number] = surah.ayahs
                }

                quranBySurah = tempMap
                isReady = true

            } catch (e: Exception) {
                e.printStackTrace()
                // Fallback: If clean file fails, app still loads but search might be weak
                isReady = true
            }
        }
    }

    fun getSurah(surahNumber: Int): List<Ayah> {
        return quranBySurah[surahNumber] ?: emptyList()
    }

    fun searchAyahs(query: String): List<Ayah> {
        // 1. Clean the user's query just in case (e.g. they typed a random Tashkeel)
        val cleanQuery = normalizeQuery(query)

        if (cleanQuery.length < 2) return emptyList()

        val results = mutableListOf<Ayah>()

        // 2. Search against the 'normalizedText' which now comes directly from the clean file
        quranBySurah.values.forEach { ayahs ->
            ayahs.forEach { ayah ->
                if (ayah.normalizedText.contains(cleanQuery)) {
                    results.add(ayah)
                }
            }
        }
        return results
    }

    // Helper: Just ensures the QUERY is clean enough to match the simple text
    private fun normalizeQuery(text: String): String {
        // Remove common diacritics just in case the user's keyboard added them
        val marks = Regex("[\u064B-\u065F\u0670\u06D6-\u06ED]")
        var t = text.replace(marks, "")

        // Normalize Alephs (أ -> ا) to match the simple text style
        t = t.replace(Regex("[\u0622\u0623\u0625\u0671]"), "\u0627") // Replace آ أ إ ٱ with ا
        t = t.replace('\u0649', '\u064A') // ى -> ي
        t = t.replace('\u0629', '\u0647') // ة -> ه

        return t.trim()
    }
}