package com.example.quran

import android.content.Context
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStreamReader
import java.text.Normalizer

data class SurahMetadata(val number: Int, val name: String, val ayahCount: Int)

object QuranRepository {
    // Key = Surah Number (1-114), Value = List of Ayahs
    private var quranBySurah: Map<Int, List<Ayah>> = emptyMap()
    private var surahMetadataList: List<SurahMetadata> = emptyList()
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
                val simpleStream = context.assets.open("quran-simple-clean")
                val simpleResponse = gson.fromJson(InputStreamReader(simpleStream), QuranResponse::class.java)
                simpleStream.close()

                // 3. MERGE THEM
                val tempMap = mutableMapOf<Int, List<Ayah>>()
                val tempMetadata = mutableListOf<SurahMetadata>()

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
                        val cleanText = cleanSurah?.ayahs?.getOrNull(aIndex)?.text ?: ""
                        ayah.normalizedText = cleanText.replace("\ufeff", "").trim()
                    }
                    tempMap[surah.number] = surah.ayahs
                    tempMetadata.add(SurahMetadata(surah.number, surah.name, surah.ayahs.size))
                }

                quranBySurah = tempMap
                surahMetadataList = tempMetadata.sortedBy { it.number }
                isReady = true

            } catch (e: Exception) {
                e.printStackTrace()
                isReady = true
            }
        }
    }

    fun getSurah(surahNumber: Int): List<Ayah> {
        return quranBySurah[surahNumber] ?: emptyList()
    }

    fun getSurahList(): List<SurahMetadata> = surahMetadataList

    fun searchAyahs(query: String): List<Ayah> {
        val cleanQuery = normalizeQuery(query)
        if (cleanQuery.length < 2) return emptyList()
        val results = mutableListOf<Ayah>()
        quranBySurah.values.forEach { ayahs ->
            ayahs.forEach { ayah ->
                if (ayah.normalizedText.contains(cleanQuery)) {
                    results.add(ayah)
                }
            }
        }
        return results
    }

    private fun normalizeQuery(text: String): String {
        val marks = Regex("[\u064B-\u065F\u0670\u06D6-\u06ED]")
        var t = text.replace(marks, "")
        t = t.replace(Regex("[\u0622\u0623\u0625\u0671]"), "\u0627")
        t = t.replace('\u0649', '\u064A')
        t = t.replace('\u0629', '\u0647')
        return t.trim()
    }
}