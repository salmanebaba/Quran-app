package com.salmane.quran

import android.content.Context
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStreamReader


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

                        // INJECT CLEAN TEXT — normalize at index time, not at search time
                        val rawClean = cleanSurah?.ayahs?.getOrNull(aIndex)?.text ?: ""
                        ayah.normalizedText = normalizeArabic(rawClean)
                    }
                    tempMap[surah.number] = surah.ayahs
                    tempMetadata.add(SurahMetadata(surah.number, surah.name, surah.englishName, surah.ayahs.size))
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

    fun getEnglishSurahName(surahNumber: Int): String {
        return surahMetadataList.find { it.number == surahNumber }?.englishName ?: ""
    }

    fun searchAyahs(query: String): List<Ayah> {
        val normalizedQuery = normalizeArabic(query)
        if (normalizedQuery.length < 2) return emptyList()

        val results = mutableListOf<Ayah>()
        quranBySurah.values.forEach { ayahs ->
            ayahs.forEach { ayah ->
                if (ayah.normalizedText.contains(normalizedQuery)) {
                    results.add(ayah)
                }
            }
        }
        return results
    }

    /**
     * Normalizes Arabic text for search comparison.
     *
     * Handles:
     * 1. BOM / zero-width no-break space (U+FEFF)
     * 2. Tashkeel — full harakat (U+064B–U+065F) and superscript alef (U+0670)
     * 3. Tajweed / pause marks (U+06D6–U+06ED) that appear even in "clean" files
     * 4. Alef variants (آ أ إ ٱ) → plain alef (ا)
     * 5. Alef maqsura (ى) → yeh (ي)
     * 6. Teh marbuta (ة) → heh (ه)
     * 7. Waw with hamza (ؤ) → waw (و)
     * 8. Yeh with hamza (ئ) → yeh (ي)
     * 9. Standalone hamza (ء) → removed (users rarely type it)
     * 10. Tatweel / kashida (U+0640) → removed
     * 11. Collapse multiple spaces left by removed characters
     */
    private fun normalizeArabic(text: String): String {
        var t = text

        // 1. Remove BOM
        t = t.replace("\uFEFF", "")

        // 2. Remove tashkeel (harakat): U+064B–U+065F and superscript alef U+0670
        t = t.replace(Regex("[\u064B-\u065F\u0670]"), "")

        // 3. Remove tajweed / Quranic annotation marks: U+06D6–U+06ED
        //    These include pause marks (ۖ ۗ ۘ ۙ ۚ ۛ) that exist even in "simple" files
        //    and break phrase searches like "لا ريب فيه"
        t = t.replace(Regex("[\u06D6-\u06ED]"), "")

        // 4. Remove tatweel (kashida) U+0640
        t = t.replace("\u0640", "")

        // 5. Normalize alef variants → plain alef
        //    آ (U+0622), أ (U+0623), إ (U+0625), ٱ (U+0671)
        t = t.replace(Regex("[\u0622\u0623\u0625\u0671]"), "\u0627")

        // 6. Alef maqsura (ى U+0649) → yeh (ي U+064A)
        t = t.replace('\u0649', '\u064A')

        // 7. Teh marbuta (ة U+0629) → heh (ه U+0647)
        t = t.replace('\u0629', '\u0647')

        // 8. Waw with hamza (ؤ U+0624) → waw (و U+0648)
        t = t.replace('\u0624', '\u0648')

        // 9. Yeh with hamza above (ئ U+0626) → yeh (ي U+064A)
        t = t.replace('\u0626', '\u064A')

        // 10. Standalone hamza (ء U+0621) → remove
        //     Users almost never type a standalone hamza when searching
        t = t.replace("\u0621", "")

        // 11. Collapse multiple consecutive spaces into one
        t = t.replace(Regex("  +"), " ")

        return t.trim()
    }
}