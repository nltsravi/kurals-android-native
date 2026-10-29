package com.nltsravi.kurals.model

data class RawKuralItem(
    val Number: Int = 0,
    val Line1: String = "",
    val Line2: String = "",
    val Translation: String = "",
    val mv: String = "",
    val sp: String = "",
    val mk: String = "",
    val prm: String? = null,
    val mnk: String? = null,
    val explanation: String = "",
    val couplet: String = "",
    val transliteration1: String = "",
    val transliteration2: String = ""
)

data class Chapter(
    val number: Int,
    val nameTamil: String,
    val nameEnglish: String,
    val transliteration: String,
    val start: Int,
    val end: Int,
    val categoryId: Int,
    val categoryTamil: String,
    val categoryEnglish: String,
    val chapterGroupId: Int,
    val chapterGroupTamil: String,
    val chapterGroupEnglish: String
)

data class ChapterGroup(
    val number: Int,
    val nameTamil: String,
    val nameEnglish: String,
    val transliteration: String,
    val categoryId: Int,
    val chapters: List<Chapter>
)

data class Category(
    val number: Int,
    val nameTamil: String,
    val nameEnglish: String,
    val transliteration: String,
    val chapterGroups: List<ChapterGroup>,
    val chapters: List<Chapter>,
    val startKural: Int,
    val endKural: Int
)

data class Kural(
    val number: Int,
    val line1: String,
    val line2: String,
    val translation: String,
    val mv: String,
    val sp: String,
    val mk: String,
    val prm: String,
    val mnk: String,
    val explanation: String,
    val couplet: String,
    val transliteration1: String,
    val transliteration2: String,
    val chapterNumber: Int,
    val chapterNameTamil: String,
    val chapterNameEnglish: String,
    val chapterTransliteration: String,
    val chapterGroupId: Int,
    val chapterGroupTamil: String,
    val chapterGroupEnglish: String,
    val categoryId: Int,
    val categoryTamil: String,
    val categoryEnglish: String,
    val searchTamilNormalized: String,
    val searchTranslitNormalized: String,
    val searchEnglishNormalized: String,
    val searchChapterNormalized: String,
    val searchCategoryNormalized: String
)

data class KuralCollection(
    val id: String,
    val name: String,
    val description: String = "",
    val kuralNumbers: List<Int> = emptyList(),
    val createdAt: String,
    val updatedAt: String,
    val color: String = "#FF7C0A"
)

enum class SearchMatchType {
    KURAL_NUMBER,
    CHAPTER_NUMBER,
    CHAPTER_NAME,
    CATEGORY_NAME,
    EXACT_PHRASE,
    STARTS_WITH_TAMIL,
    STARTS_WITH_TRANSLIT,
    CONTAINS_TAMIL,
    CONTAINS_TRANSLIT,
    CONTAINS_ENGLISH
}

data class SearchResult(
    val kural: Kural,
    val score: Int,
    val matchType: SearchMatchType
)

enum class ExportFormat {
    PDF,
    DOCX
}

enum class MeaningType {
    MV,
    SP,
    MK,
    PRM,
    MNK,
    TRANSLATION,
    BOTH
}

enum class CardAspectRatio {
    RATIO_9_16,
    RATIO_4_5,
    RATIO_1_1
}

enum class CardImageTheme {
    PORTRAIT,
    PALM_LEAF,
    TEMPLE_LAMP,
    MINIMAL_SEAL,
    NONE,
    CUSTOM
}

enum class SocialPlatform {
    WHATSAPP_STATUS,
    WHATSAPP,
    INSTAGRAM,
    TWITTER,
    THREADS,
    FACEBOOK,
    SYSTEM
}

data class SocialShareOptions(
    val meaningType: MeaningType = MeaningType.MV,
    val includeTransliteration: Boolean = false,
    val includeHashtags: Boolean = true
)

data class ThiruvalluvarImageOption(
    val id: String,
    val labelTa: String,
    val labelEn: String,
    val assetPath: String
)

enum class ThemeMode(val key: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromKey(key: String?): ThemeMode {
            return entries.find { it.key == key } ?: SYSTEM
        }
    }
}

enum class AppLanguage(val key: String) {
    TA("ta"),
    EN("en");

    companion object {
        fun fromKey(key: String?): AppLanguage {
            return entries.find { it.key == key } ?: TA
        }
    }
}

data class NotificationSettings(
    val enabled: Boolean = true,
    val hour: Int = 8,
    val minute: Int = 0
)

// Olineri Tamil Pronunciation SDK Models
enum class PhonemeStatus(val key: String) {
    CORRECT("correct"),
    SUBSTITUTION("substitution"),
    OMISSION("omission"),
    UNCERTAIN("uncertain")
}

data class PhonemeTarget(
    val expectedIpa: String,
    val displayGrapheme: String,
    val charStartUtf8: Int,
    val charEndUtf8: Int,
    val clusterIndex: Int
)

data class PhonemeFeedback(
    val status: PhonemeStatus,
    val expectedIpa: String,
    val actualIpa: String?,
    val displayGrapheme: String,
    val charStartUtf8: Int,
    val charEndUtf8: Int,
    val clusterIndex: Int,
    val confidence: Double,
    val gopScore: Double,
    val message: String,
    val messageTamil: String,
    val messageEnglish: String,
    val articulatoryTipTamil: String? = null,
    val articulatoryTipEnglish: String? = null
)

data class PronunciationResult(
    val overallScore: Int,
    val passed: Boolean,
    val summaryMessage: String,
    val summaryMessageTamil: String,
    val summaryMessageEnglish: String,
    val transcriptUsed: String,
    val phonemes: List<PhonemeFeedback>
)

data class AcousticSignalMetrics(
    val durationMs: Int,
    val rmsEnergy: Double,
    val peakAmplitude: Double,
    val voicedFrameRatio: Double,
    val zeroCrossingRate: Double,
    val syllablePeakCount: Int,
    val energyModulationScore: Double,
    val isValidSpeech: Boolean
)

data class OlineriDownloadProgress(
    val currentArtifact: String,
    val completedCount: Int,
    val totalCount: Int,
    val percentage: Int
)

data class OlineriCachedState(
    val version: String,
    val downloadedAt: String,
    val languageCode: String = "ta-IN",
    val phonemeCount: Int,
    val artifacts: Map<String, String>
)

data class SpecialTamilPhoneticItem(
    val grapheme: String,
    val ipa: String,
    val nameTa: String,
    val nameEn: String,
    val tipTa: String,
    val tipEn: String
)

