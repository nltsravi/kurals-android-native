package com.nltsravi.kurals.data

import android.content.Context
import com.nltsravi.kurals.constants.AppConstants
import com.nltsravi.kurals.model.Category
import com.nltsravi.kurals.model.Chapter
import com.nltsravi.kurals.model.ChapterGroup
import com.nltsravi.kurals.model.Kural
import com.nltsravi.kurals.util.TextUtils
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.ceil
import kotlin.random.Random

object KuralRepository {

    private var kuralsCache: List<Kural>? = null
    private var kuralByNumberMap: Map<Int, Kural>? = null
    private var chaptersCache: List<Chapter>? = null
    private var chapterByNumberMap: Map<Int, Chapter>? = null
    private var categoriesCache: List<Category>? = null

    @Synchronized
    fun initialize(context: Context) {
        if (kuralsCache != null && chaptersCache != null && categoriesCache != null) {
            return
        }
        val detailJson = context.assets.open("data/detail.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
        val kuralsJson = context.assets.open("data/thirukkural.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
        initializeFromJsonStrings(detailJson, kuralsJson)
    }

    @Synchronized
    fun initializeFromJsonStrings(detailJsonString: String, kuralsJsonString: String) {
        if (kuralsCache != null && chaptersCache != null && categoriesCache != null) {
            return
        }

        // 1. Parse detail.json hierarchy
        val detailRoot = JSONArray(detailJsonString).getJSONObject(0)
        val sectionsArray = detailRoot.getJSONObject("section").getJSONArray("detail")

        val categories = mutableListOf<Category>()
        val chapters = mutableListOf<Chapter>()
        val chapterMap = mutableMapOf<Int, Chapter>()

        for (i in 0 until sectionsArray.length()) {
            val sec = sectionsArray.getJSONObject(i)
            val categoryId = sec.getInt("number")
            val categoryTamil = sec.getString("name")
            val categoryEnglish = sec.getString("translation")
            val categoryTranslit = sec.getString("transliteration")

            val chapterGroups = mutableListOf<ChapterGroup>()
            val categoryChapters = mutableListOf<Chapter>()
            val cgArray = sec.getJSONObject("chapterGroup").getJSONArray("detail")

            for (j in 0 until cgArray.length()) {
                val cg = cgArray.getJSONObject(j)
                val cgNumber = cg.getInt("number")
                val cgTamil = cg.getString("name")
                val cgEnglish = cg.getString("translation")
                val cgTranslit = cg.getString("transliteration")

                val groupChapters = mutableListOf<Chapter>()
                val chArray = cg.getJSONObject("chapters").getJSONArray("detail")

                for (k in 0 until chArray.length()) {
                    val ch = chArray.getJSONObject(k)
                    val chNumber = ch.getInt("number")
                    val chapter = Chapter(
                        number = chNumber,
                        nameTamil = ch.getString("name"),
                        nameEnglish = ch.getString("translation"),
                        transliteration = ch.getString("transliteration"),
                        start = ch.getInt("start"),
                        end = ch.getInt("end"),
                        categoryId = categoryId,
                        categoryTamil = categoryTamil,
                        categoryEnglish = categoryEnglish,
                        chapterGroupId = cgNumber,
                        chapterGroupTamil = cgTamil,
                        chapterGroupEnglish = cgEnglish
                    )
                    groupChapters.add(chapter)
                    categoryChapters.add(chapter)
                    chapters.add(chapter)
                    chapterMap[chNumber] = chapter
                }

                chapterGroups.add(
                    ChapterGroup(
                        number = cgNumber,
                        nameTamil = cgTamil,
                        nameEnglish = cgEnglish,
                        transliteration = cgTranslit,
                        categoryId = categoryId,
                        chapters = groupChapters
                    )
                )
            }

            val startKural = categoryChapters.firstOrNull()?.start ?: 1
            val endKural = categoryChapters.lastOrNull()?.end ?: 1330

            categories.add(
                Category(
                    number = categoryId,
                    nameTamil = categoryTamil,
                    nameEnglish = categoryEnglish,
                    transliteration = categoryTranslit,
                    chapterGroups = chapterGroups,
                    chapters = categoryChapters,
                    startKural = startKural,
                    endKural = endKural
                )
            )
        }

        categoriesCache = categories
        chaptersCache = chapters
        chapterByNumberMap = chapterMap

        // 2. Parse thirukkural.json
        val kuralsRoot = JSONObject(kuralsJsonString)
        val rawKuralsArray = kuralsRoot.getJSONArray("kural")
        val enrichedKurals = ArrayList<Kural>(rawKuralsArray.length())
        val kuralMap = HashMap<Int, Kural>(rawKuralsArray.length())

        for (i in 0 until rawKuralsArray.length()) {
            val item = rawKuralsArray.getJSONObject(i)
            val number = item.getInt("Number")
            val chapterNumber = ceil(number.toDouble() / AppConstants.KURALS_PER_CHAPTER.toDouble()).toInt()
            val chapter = chapterMap[chapterNumber]

            val line1 = item.optString("Line1", "")
            val line2 = item.optString("Line2", "")
            val translation = item.optString("Translation", "")
            val mv = item.optString("mv", "")
            val sp = item.optString("sp", "")
            val mk = item.optString("mk", "")
            val prm = item.optString("prm", "").ifBlank { mv }
            val mnk = item.optString("mnk", "").ifBlank { sp }
            val explanation = item.optString("explanation", "")
            val couplet = item.optString("couplet", "")
            val translit1 = item.optString("transliteration1", "")
            val translit2 = item.optString("transliteration2", "")

            val chNameTa = chapter?.nameTamil ?: ""
            val chNameEn = chapter?.nameEnglish ?: ""
            val chTranslit = chapter?.transliteration ?: ""
            val cgId = chapter?.chapterGroupId ?: 1
            val cgTa = chapter?.chapterGroupTamil ?: ""
            val cgEn = chapter?.chapterGroupEnglish ?: ""
            val catId = chapter?.categoryId ?: 1
            val catTa = chapter?.categoryTamil ?: ""
            val catEn = chapter?.categoryEnglish ?: ""

            val kural = Kural(
                number = number,
                line1 = line1,
                line2 = line2,
                translation = translation,
                mv = mv,
                sp = sp,
                mk = mk,
                prm = prm,
                mnk = mnk,
                explanation = explanation,
                couplet = couplet,
                transliteration1 = translit1,
                transliteration2 = translit2,
                chapterNumber = chapterNumber,
                chapterNameTamil = chNameTa,
                chapterNameEnglish = chNameEn,
                chapterTransliteration = chTranslit,
                chapterGroupId = cgId,
                chapterGroupTamil = cgTa,
                chapterGroupEnglish = cgEn,
                categoryId = catId,
                categoryTamil = catTa,
                categoryEnglish = catEn,
                searchTamilNormalized = TextUtils.normalizeSearchText(
                    "$line1 $line2 $mv $sp $mk $prm $mnk"
                ),
                searchTranslitNormalized = TextUtils.normalizeSearchText(
                    "$translit1 $translit2 $chTranslit"
                ),
                searchEnglishNormalized = TextUtils.normalizeSearchText(
                    "$translation $couplet $explanation"
                ),
                searchChapterNormalized = TextUtils.normalizeSearchText(
                    "$chNameTa $chNameEn $chTranslit"
                ),
                searchCategoryNormalized = TextUtils.normalizeSearchText(
                    "$catTa $catEn $cgTa $cgEn"
                )
            )

            enrichedKurals.add(kural)
            kuralMap[number] = kural
        }

        kuralsCache = enrichedKurals
        kuralByNumberMap = kuralMap
    }

    fun getAllKurals(): List<Kural> = kuralsCache ?: emptyList()

    fun getKuralByNumber(number: Int): Kural? = kuralByNumberMap?.get(number)

    fun getKuralsByNumbers(numbers: List<Int>): List<Kural> {
        val map = kuralByNumberMap ?: return emptyList()
        return numbers.mapNotNull { map[it] }
    }

    fun getKuralsByChapter(chapterNumber: Int): List<Kural> {
        val chapter = getChapterByNumber(chapterNumber) ?: return emptyList()
        val result = mutableListOf<Kural>()
        for (i in chapter.start..chapter.end) {
            val k = getKuralByNumber(i)
            if (k != null) result.add(k)
        }
        return result
    }

    fun getAllChapters(): List<Chapter> = chaptersCache ?: emptyList()

    fun getChapterByNumber(chapterNumber: Int): Chapter? = chapterByNumberMap?.get(chapterNumber)

    fun getAllCategories(): List<Category> = categoriesCache ?: emptyList()

    fun getRandomKural(): Kural {
        val all = getAllKurals()
        if (all.isEmpty()) throw IllegalStateException("KuralRepository is not initialized")
        val randomIndex = Random.nextInt(all.size)
        return all[randomIndex]
    }
}

