package com.nltsravi.kurals.service

import com.nltsravi.kurals.constants.AppConstants
import com.nltsravi.kurals.data.KuralRepository
import com.nltsravi.kurals.model.SearchMatchType
import com.nltsravi.kurals.model.SearchResult
import com.nltsravi.kurals.util.TextUtils

object SearchService {

    /**
     * Multi-tiered search across all 1330 Kurals, 133 Chapters, and 3 Categories.
     * Supports:
     * 1. Exact Kural number (1-1330) -> Score 1000
     * 2. Exact Chapter number (1-133) -> Score 800
     * 3. Exact Chapter name (Tamil / English / Transliteration) -> Score 700, startsWith 600, contains 500
     * 4. Category name (அறத்துப்பால் / பொருட்பால் / காமத்துப்பால் / Virtue / Wealth / Love) -> Score 550 / 450
     * 5. Exact phrase match in Tamil couplet or Transliteration -> Score 400
     * 6. Starts with query in Tamil couplet -> Score 350
     * 7. Starts with query in Transliteration -> Score 300
     * 8. Contains in Tamil couplet or commentary -> Score 250
     * 9. Contains in Transliteration -> Score 200
     * 10. Contains in English translation/explanation -> Score 150
     */
    fun searchKurals(rawQuery: String, limit: Int = 50): List<SearchResult> {
        val trimmed = rawQuery.trim()
        if (trimmed.isEmpty()) return emptyList()

        val allKurals = KuralRepository.getAllKurals()
        val results = mutableListOf<SearchResult>()
        val seenKuralNumbers = HashSet<Int>()

        // 1. Check if query is a pure number
        val queryNum = TextUtils.parseQueryNumber(trimmed)
        if (queryNum != null) {
            if (queryNum in 1..AppConstants.TOTAL_KURALS) {
                val exactKural = KuralRepository.getKuralByNumber(queryNum)
                if (exactKural != null) {
                    results.add(
                        SearchResult(
                            kural = exactKural,
                            score = 1000,
                            matchType = SearchMatchType.KURAL_NUMBER
                        )
                    )
                    seenKuralNumbers.add(exactKural.number)
                }
            }

            if (queryNum in 1..AppConstants.TOTAL_CHAPTERS) {
                val chapterKurals = KuralRepository.getKuralsByChapter(queryNum)
                for (k in chapterKurals) {
                    if (!seenKuralNumbers.contains(k.number)) {
                        results.add(
                            SearchResult(
                                kural = k,
                                score = 800,
                                matchType = SearchMatchType.CHAPTER_NUMBER
                            )
                        )
                        seenKuralNumbers.add(k.number)
                    }
                }
            }
        }

        // 2. Textual search
        val rawNormalized = TextUtils.normalizeSearchText(trimmed)
        val normalizedQuery = if (rawNormalized == "agaram" || rawNormalized == "agara") {
            "akara"
        } else {
            rawNormalized.replace("agaram", "akara").replace("agara ", "akara ")
        }
        if (normalizedQuery.isEmpty()) {
            return results.take(limit)
        }

        val queryTokens = normalizedQuery.split(" ").filter { it.isNotEmpty() }

        for (kural in allKurals) {
            if (seenKuralNumbers.contains(kural.number)) continue

            var bestScore = 0
            var bestMatchType: SearchMatchType = SearchMatchType.CONTAINS_TAMIL

            val line1Norm = TextUtils.normalizeSearchText(kural.line1)
            val line2Norm = TextUtils.normalizeSearchText(kural.line2)
            val coupletTamilNorm = "$line1Norm $line2Norm"
            val translitNorm = TextUtils.normalizeSearchText("${kural.transliteration1} ${kural.transliteration2}")
            val chapterTamilNorm = TextUtils.normalizeSearchText(kural.chapterNameTamil)
            val chapterEngNorm = TextUtils.normalizeSearchText(kural.chapterNameEnglish)
            val chapterTranslitNorm = TextUtils.normalizeSearchText(kural.chapterTransliteration)

            // Tier A: Chapter Name Match
            if (chapterTamilNorm == normalizedQuery ||
                chapterEngNorm == normalizedQuery ||
                chapterTranslitNorm == normalizedQuery
            ) {
                bestScore = 700
                bestMatchType = SearchMatchType.CHAPTER_NAME
            } else if (chapterTamilNorm.startsWith(normalizedQuery) ||
                chapterEngNorm.startsWith(normalizedQuery) ||
                chapterTranslitNorm.startsWith(normalizedQuery)
            ) {
                bestScore = 600
                bestMatchType = SearchMatchType.CHAPTER_NAME
            } else if (kural.searchChapterNormalized.contains(normalizedQuery)) {
                bestScore = 500
                bestMatchType = SearchMatchType.CHAPTER_NAME
            }

            // Tier B: Category / Iyal Match
            if (bestScore < 550) {
                val catTamilNorm = TextUtils.normalizeSearchText(kural.categoryTamil)
                val catEngNorm = TextUtils.normalizeSearchText(kural.categoryEnglish)
                if (catTamilNorm == normalizedQuery || catEngNorm == normalizedQuery) {
                    bestScore = 550
                    bestMatchType = SearchMatchType.CATEGORY_NAME
                } else if (kural.searchCategoryNormalized.contains(normalizedQuery)) {
                    bestScore = maxOf(bestScore, 450)
                    if (bestScore == 450) bestMatchType = SearchMatchType.CATEGORY_NAME
                }
            }

            // Tier C: Exact Phrase in Tamil Couplet or Transliteration
            if (bestScore < 400) {
                if (coupletTamilNorm == normalizedQuery || translitNorm == normalizedQuery) {
                    bestScore = 400
                    bestMatchType = SearchMatchType.EXACT_PHRASE
                }
            }

            // Tier D: Starts with in Tamil Line 1 or Line 2
            if (bestScore < 350) {
                if (line1Norm.startsWith(normalizedQuery) || line2Norm.startsWith(normalizedQuery)) {
                    bestScore = 350
                    bestMatchType = SearchMatchType.STARTS_WITH_TAMIL
                }
            }

            // Tier E: Starts with in Transliteration
            if (bestScore < 300) {
                if (translitNorm.startsWith(normalizedQuery)) {
                    bestScore = 300
                    bestMatchType = SearchMatchType.STARTS_WITH_TRANSLIT
                }
            }

            // Tier F: Contains in Tamil (Couplet or Commentary)
            if (bestScore < 250) {
                val allTokensInCouplet = queryTokens.all { token -> coupletTamilNorm.contains(token) }
                if (coupletTamilNorm.contains(normalizedQuery)) {
                    bestScore = 260
                    bestMatchType = SearchMatchType.CONTAINS_TAMIL
                } else if (allTokensInCouplet) {
                    bestScore = 250
                    bestMatchType = SearchMatchType.CONTAINS_TAMIL
                } else if (kural.searchTamilNormalized.contains(normalizedQuery)) {
                    bestScore = 220
                    bestMatchType = SearchMatchType.CONTAINS_TAMIL
                }
            }

            // Tier G: Contains in Transliteration
            if (bestScore < 200) {
                val allTokensInTranslit = queryTokens.all { token -> kural.searchTranslitNormalized.contains(token) }
                if (kural.searchTranslitNormalized.contains(normalizedQuery)) {
                    bestScore = 200
                    bestMatchType = SearchMatchType.CONTAINS_TRANSLIT
                } else if (allTokensInTranslit) {
                    bestScore = 190
                    bestMatchType = SearchMatchType.CONTAINS_TRANSLIT
                }
            }

            // Tier H: Contains in English Translation / Explanation
            if (bestScore < 150) {
                val allTokensInEnglish = queryTokens.all { token -> kural.searchEnglishNormalized.contains(token) }
                if (kural.searchEnglishNormalized.contains(normalizedQuery)) {
                    bestScore = 150
                    bestMatchType = SearchMatchType.CONTAINS_ENGLISH
                } else if (allTokensInEnglish) {
                    bestScore = 140
                    bestMatchType = SearchMatchType.CONTAINS_ENGLISH
                }
            }

            if (bestScore > 0) {
                results.add(
                    SearchResult(
                        kural = kural,
                        score = bestScore,
                        matchType = bestMatchType
                    )
                )
            }
        }

        return results
            .sortedWith(compareByDescending<SearchResult> { it.score }.thenBy { it.kural.number })
            .take(limit)
    }
}

