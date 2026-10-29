package com.nltsravi.kurals.util

import com.nltsravi.kurals.model.Kural
import com.nltsravi.kurals.model.MeaningType
import com.nltsravi.kurals.model.SocialShareOptions
import java.text.Normalizer
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

object TextUtils {

    private val PUNCTUATION_REGEX = Regex("[.,/#!\$%^&*;:{}=\\-_`~()\"'’?।|\\\\<>\\[\\]+]")
    private val MULTISPACE_REGEX = Regex("\\s+")
    private val TRAILING_DOTS_REGEX = Regex("\\.{2,}|…", RegexOption.IGNORE_CASE)

    /**
     * Normalize text for search:
     * - Unicode NFC normalization
     * - Lowercase for English/Transliteration
     * - Strip punctuation & extra spaces
     */
    fun normalizeSearchText(input: String?): String {
        if (input.isNullOrBlank()) return ""
        val nfc = try {
            Normalizer.normalize(input, Normalizer.Form.NFC)
        } catch (_: Exception) {
            input
        }
        return nfc
            .lowercase()
            .replace(PUNCTUATION_REGEX, " ")
            .replace(MULTISPACE_REGEX, " ")
            .trim()
    }

    /**
     * Clean and parse numeric input if query is a pure number
     */
    fun parseQueryNumber(query: String): Int? {
        val trimmed = query.trim()
        if (!Regex("^\\d+$").matches(trimmed)) {
            return null
        }
        return trimmed.toIntOrNull()
    }

    /**
     * Strictly format a Thirukkural couplet according to the classic Venpa structure:
     * - First line (அடி 1): 4 words (சீர்கள்)
     * - Second line (அடி 2): 3 words (சீர்கள்)
     * - Never contains ellipses ("..." or "…")
     */
    fun formatKuralCouplet(line1: String, line2: String): Pair<String, String> {
        val cleanLine1 = line1
            .replace(TRAILING_DOTS_REGEX, "")
            .replace(MULTISPACE_REGEX, " ")
            .trim()
        val cleanLine2 = line2
            .replace(TRAILING_DOTS_REGEX, "")
            .replace(MULTISPACE_REGEX, " ")
            .trim()

        val words1 = if (cleanLine1.isEmpty()) emptyList() else cleanLine1.split(" ")
        val words2 = if (cleanLine2.isEmpty()) emptyList() else cleanLine2.split(" ")

        if (words1.size == 4 && words2.size == 3) {
            return Pair(cleanLine1, cleanLine2)
        }

        val allWords = words1 + words2
        if (allWords.size >= 7) {
            return Pair(
                allWords.subList(0, 4).joinToString(" "),
                allWords.subList(4, 7).joinToString(" ")
            )
        }

        return Pair(cleanLine1, cleanLine2)
    }

    /**
     * Format Kural text for copying or sharing
     */
    fun formatKuralText(
        kural: Kural,
        mode: String = "both"
    ): String {
        val (line1, line2) = formatKuralCouplet(kural.line1, kural.line2)
        val header = "திருக்குறள் #${kural.number} (${kural.chapterNameTamil} - ${kural.chapterNameEnglish})"
        val tamilLines = "$line1\n$line2"
        val translitLines = "${kural.transliteration1}\n${kural.transliteration2}"

        return when (mode) {
            "tamil" -> "$header\n\n$tamilLines"
            "translit" -> "$header\n\n$translitLines\n\nMeaning: ${kural.translation}"
            else -> "$header\n\n$tamilLines\n\n$translitLines\n\nமு.வ உரை: ${kural.mv}\n\nTranslation: ${kural.translation}"
        }
    }

    /**
     * Format Kural text specifically optimized for Social Media posts (WhatsApp, X/Twitter, Instagram, Threads, Facebook)
     */
    fun formatKuralForSocialShare(
        kural: Kural,
        options: SocialShareOptions = SocialShareOptions()
    ): String {
        val (line1, line2) = formatKuralCouplet(kural.line1, kural.line2)
        val parts = mutableListOf<String>()

        parts.add("📜 திருக்குறள் #${kural.number} | ${kural.chapterNameTamil}")
        parts.add("$line1\n$line2")

        if (options.includeTransliteration) {
            parts.add("🗣️ ${kural.transliteration1}\n${kural.transliteration2}")
        }

        when (options.meaningType) {
            MeaningType.MV -> parts.add("💡 பொருள் (மு.வ):\n${kural.mv}")
            MeaningType.SP -> parts.add("💡 பொருள் (சாலமன் பாப்பையா):\n${kural.sp}")
            MeaningType.MK -> parts.add("💡 பொருள் (மு.கருணாநிதி):\n${kural.mk}")
            MeaningType.PRM -> parts.add("💡 பொருள் (பரிமேலழகர்):\n${kural.prm}")
            MeaningType.MNK -> parts.add("💡 பொருள் (மணக்குடவர்):\n${kural.mnk}")
            MeaningType.TRANSLATION -> parts.add("🌐 Meaning:\n${kural.translation}")
            MeaningType.BOTH -> {
                parts.add("💡 பொருள்:\n${kural.mv}")
                parts.add("🌐 Meaning:\n${kural.translation}")
            }
        }

        if (options.includeHashtags) {
            val cleanChapterTag = kural.chapterNameTamil.replace(Regex("[\\s\\-]+"), "_")
            parts.add("#திருக்குறள் #Thirukkural #Kural${kural.number} #$cleanChapterTag #TamilWisdom")
        }

        return parts.joinToString("\n\n")
    }

    /**
     * Sanitize filename for export
     */
    fun sanitizeFilename(name: String): String {
        val cleaned = name
            .replace(Regex("[^a-zA-Z0-9\\u0B80-\\u0BFF_-]"), "_")
            .replace(Regex("_+"), "_")
            .replace(Regex("^_|_\$"), "")
        return cleaned.take(50).ifEmpty { "thirukkural_export" }
    }

    /**
     * Estimate the visual typographic width of a Tamil string in "base character units".
     * Matches `getTamilTypographicWidth` in `KuralCoupletText.tsx`.
     */
    fun getTamilTypographicWidth(text: String): Double {
        var units = 0.0
        for (ch in text) {
            val code = ch.code
            when {
                // Zero-width / above-below combining marks: Pulli (்), U (ு), UU (ூ)
                code == 0x0BCD || code == 0x0BC1 || code == 0x0BC2 -> {
                    units += 0.08
                }
                // Narrow spacing vowel signs: AA (ா), I (ி), II (ீ)
                code == 0x0BBE || code == 0x0BBF || code == 0x0BC0 -> {
                    units += 0.45
                }
                // Pre-base vowel signs: E (ெ), EE (ே), AI (ை)
                code == 0x0BC6 || code == 0x0BC7 || code == 0x0BC8 -> {
                    units += 0.72
                }
                // Two-part vowel signs: O (ொ), OO (ோ), AU (ௌ), AU length mark (ௗ)
                code == 0x0BCA || code == 0x0BCB || code == 0x0BCC || code == 0x0BD7 -> {
                    units += 1.15
                }
                // Spaces between சீர்கள்
                ch == ' ' -> {
                    units += 0.42
                }
                // Base Tamil consonants & independent vowels
                else -> {
                    units += 0.92
                }
            }
        }
        return max(units, 1.0)
    }

    /**
     * Calculate a single uniform font size (in sp/px) that guarantees BOTH lines fit
     * on a single line each within `availableWidth`, clamped between `minFontSize` and `maxFontSize`.
     */
    fun calculateCoupletFontSize(
        line1: String,
        line2: String,
        availableWidth: Float,
        maxFontSize: Float = 17f,
        minFontSize: Float = 10.5f
    ): Float {
        if (availableWidth <= 0f) return min(maxFontSize, 13.5f)
        val widthUnits1 = getTamilTypographicWidth(line1)
        val widthUnits2 = getTamilTypographicWidth(line2)
        val dominantUnits = max(widthUnits1, widthUnits2)

        val safeWidth = max(availableWidth - 10f, 120f)
        val fittedSize = floor((safeWidth / (dominantUnits * 0.76)) * 2.0).toFloat() / 2f

        return max(minFontSize, min(maxFontSize, fittedSize))
    }
}

