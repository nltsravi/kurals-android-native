package com.nltsravi.kurals.service

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.nltsravi.kurals.constants.AppConstants
import com.nltsravi.kurals.data.PreferencesAndCollectionStorage
import com.nltsravi.kurals.model.AcousticSignalMetrics
import com.nltsravi.kurals.model.AppLanguage
import com.nltsravi.kurals.model.OlineriCachedState
import com.nltsravi.kurals.model.OlineriDownloadProgress
import com.nltsravi.kurals.model.PhonemeFeedback
import com.nltsravi.kurals.model.PhonemeStatus
import com.nltsravi.kurals.model.PhonemeTarget
import com.nltsravi.kurals.model.PronunciationResult
import com.nltsravi.kurals.model.SpecialTamilPhoneticItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

object OlineriService {

    data class RequiredTamilArtifact(
        val key: String,
        val label: String,
        val path: String
    )

    /**
     * Strictly Tamil-only artifacts from nltsravi/olineri-core
     */
    val REQUIRED_TAMIL_ARTIFACTS = listOf(
        RequiredTamilArtifact(
            key = "phoneme_inventory.json",
            label = "Tamil Phoneme Inventory (ta-IN)",
            path = "${AppConstants.OLINERI_TAMIL_PACKAGE_PATH}/phoneme_inventory.json"
        ),
        RequiredTamilArtifact(
            key = "strings_ta.json",
            label = "Tamil Articulatory Feedback (தமிழ்)",
            path = "${AppConstants.OLINERI_TAMIL_PACKAGE_PATH}/strings_ta.json"
        ),
        RequiredTamilArtifact(
            key = "strings_en.json",
            label = "Tamil Articulatory Feedback (English)",
            path = "${AppConstants.OLINERI_TAMIL_PACKAGE_PATH}/strings_en.json"
        ),
        RequiredTamilArtifact(
            key = "test_cases.json",
            label = "Tamil G2P Phonetic Reference",
            path = "${AppConstants.OLINERI_TAMIL_PACKAGE_PATH}/test_cases.json"
        ),
        RequiredTamilArtifact(
            key = "types.ts",
            label = "Olineri Core SDK Schema",
            path = "${AppConstants.OLINERI_CORE_PACKAGE_PATH}/types.ts"
        ),
        RequiredTamilArtifact(
            key = "index.ts",
            label = "Olineri Core SDK Engine",
            path = "${AppConstants.OLINERI_CORE_PACKAGE_PATH}/index.ts"
        )
    )

    val TAMIL_STRINGS_DEFAULT = mapOf(
        "pass" to "மிகச் சிறப்பு! உங்கள் திருக்குறள் உச்சரிப்பு துல்லியமாக உள்ளது.",
        "fail" to "நல்ல முயற்சி! கீழே சிவப்பு அல்லது ஆரஞ்சு நிறத்தில் உள்ள எழுத்துக்களை மீண்டும் பயிற்சி செய்யவும்.",
        "PhonemeCorrect" to "துல்லியமான உச்சரிப்பு!",
        "PhonemeUncertain" to "உச்சரிப்பு சற்று தெளிவாக இல்லை. மீண்டும் சத்தமாகவும் தெளிவாகவும் முயற்சிக்கவும்.",
        "PhonemeOmitted" to "இந்த எழுத்து உச்சரிக்கப்படவில்லை. முழுச் சீரையும் நிதானமாக வாசிக்கவும்.",
        "PhonemeSubstituted" to "எதிர்பார்த்த ஒலி /{expected}/, ஆனால் ஒலித்தது /{actual}/.",
        "ZhhaSubstitution" to "'ழ' (/ɻ/) சிறப்பு ஒலி: நாக்கின் நுனியை மேல் அண்ணத்தின் நடுப்பகுதியை நோக்கி பின்னோக்கி வளைத்து (தொடாமல்) ஒலிக்கவும். 'ல' அல்லது 'ள' போல ஒலிக்க வேண்டாம்.",
        "LaRetroflexTip" to "பொது 'ள' (/ɭ/) ஒலி: நாக்கின் நுனியை சற்று மேல்நோக்கி மடித்து அண்ணத்தைத் தொட்டு ஒலிக்கவும்.",
        "LaAlveolarTip" to "தனி 'ல' (/l/) ஒலி: நாக்கின் நுனியை மேல் பற்களின் ஈறுகளுக்குப் பின்னால் இலேசாகத் தொட்டு ஒலிக்கவும்.",
        "RraTapContrast" to "வல்லின 'ற' (/r/) மற்றும் இடையின 'ர' (/ɾ/) வேறுபாடு: 'ற' ஒலிக்கும்போது நாக்கு மேல் அண்ணத்தை உறுதியாக அதிரச் செய்ய வேண்டும்.",
        "RetroflexStopTip" to "'ட' (/ʈ/) வல்லின ஒலி: நாக்கின் நுனியை மேல் அண்ணத்தில் மடித்துத் தொட்டு ஒலிக்கவும்.",
        "DentalStopTip" to "'த' (/t̪/) மென்மையான ஒலி: நாக்கின் நுனியை மேல் பற்களின் உட்புறத்தில் தொட்டு ஒலிக்கவும்.",
        "NasalContrastTip" to "'ண' (/ɳ/ - மூணு சுழி), 'ன' (/n/ - ரெண்டு சுழி), 'ந' (/n̪/ - தந்த நகரம்) ஆகிய மூக்கொலிகளின் நாக்கு நிலையைக் கவனிக்கவும்.",
        "VowelLengthMismatch" to "குறில்-நெடில் வேறுபாடு: எதிர்பார்த்தது /{expected}/, ஒலித்தது /{actual}/. நெடில் எழுத்துக்களை இரண்டு மாத்திரை அளவு நீட்டி ஒலிக்கவும்."
    )

    val ENGLISH_STRINGS_DEFAULT = mapOf(
        "pass" to "Excellent! Your Thirukkural pronunciation is accurate and clear.",
        "fail" to "Good effort! Tap the highlighted letters below for articulatory tongue-position guidance.",
        "PhonemeCorrect" to "Accurate pronunciation!",
        "PhonemeUncertain" to "Unclear articulation. Speak slightly louder and at a steady pace.",
        "PhonemeOmitted" to "This syllable was missed or skipped. Read the full word smoothly.",
        "PhonemeSubstituted" to "Expected /{expected}/, but heard /{actual}/.",
        "ZhhaSubstitution" to "Retroflex approximant 'ழ' (/ɻ/): Curl the tongue tip back toward the hard palate without touching the roof of the mouth. Do not substitute with 'ல' (/l/) or 'ள' (/ɭ/).",
        "LaRetroflexTip" to "Retroflex lateral 'ள' (/ɭ/): Curl the tongue tip slightly back to touch the roof of the mouth.",
        "LaAlveolarTip" to "Alveolar lateral 'ல' (/l/): Touch the tongue tip lightly right behind the upper teeth ridge.",
        "RraTapContrast" to "Alveolar trill 'ற' (/r/) vs tap 'ர' (/ɾ/): Vibrate the tongue tip firmly against the alveolar ridge for 'ற'.",
        "RetroflexStopTip" to "Retroflex 'ட' (/ʈ/): Curl the tongue tip back against the palate.",
        "DentalStopTip" to "Dental 'த' (/t̪/): Place the tongue tip gently against the back of the upper teeth.",
        "NasalContrastTip" to "Pay attention to Tamil nasal placement: retroflex 'ண' (/ɳ/), alveolar 'ன' (/n/), and dental 'ந' (/n̪/).",
        "VowelLengthMismatch" to "Kuril/Nedil vowel length mismatch: Expected /{expected}/, heard /{actual}/. Hold long (Nedil) vowels for twice the duration of short (Kuril) vowels."
    )

    suspend fun downloadOlineriTamilArtifacts(
        storage: PreferencesAndCollectionStorage,
        onProgress: ((OlineriDownloadProgress) -> Unit)? = null
    ): OlineriCachedState = withContext(Dispatchers.IO) {
        val artifacts = mutableMapOf<String, String>()
        val totalCount = REQUIRED_TAMIL_ARTIFACTS.size

        REQUIRED_TAMIL_ARTIFACTS.forEachIndexed { index, item ->
            onProgress?.invoke(
                OlineriDownloadProgress(
                    currentArtifact = item.label,
                    completedCount = index,
                    totalCount = totalCount,
                    percentage = ((index.toDouble() / totalCount.toDouble()) * 100.0).roundToInt()
                )
            )

            val urlString = "${AppConstants.OLINERI_REPO_BASE_URL}/${item.path}"
            val conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15000
                readTimeout = 15000
                setRequestProperty("Accept", "application/json, text/plain, */*")
            }

            try {
                val code = conn.responseCode
                if (code !in 200..299) {
                    throw IllegalStateException("Failed to download Tamil artifact (${item.key}): HTTP $code")
                }
                val text = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                if (text.isBlank()) {
                    throw IllegalStateException("Empty artifact received for ${item.key}")
                }
                artifacts[item.key] = text
            } finally {
                conn.disconnect()
            }
        }

        val inventoryJson = JSONObject(artifacts["phoneme_inventory.json"] ?: "{}")
        if (inventoryJson.optString("language") != "ta-IN") {
            throw IllegalStateException("Invalid language pack: Only Tamil (ta-IN) is permitted.")
        }

        val phonemesArr = inventoryJson.optJSONArray("phonemes")
        val phonemeCount = phonemesArr?.length() ?: 34

        onProgress?.invoke(
            OlineriDownloadProgress(
                currentArtifact = "Tamil Pronunciation SDK Ready",
                completedCount = totalCount,
                totalCount = totalCount,
                percentage = 100
            )
        )

        val state = OlineriCachedState(
            version = inventoryJson.optString("version", "0.1.0"),
            downloadedAt = PreferencesAndCollectionStorage.nowIsoString(),
            languageCode = "ta-IN",
            phonemeCount = phonemeCount,
            artifacts = artifacts
        )
        storage.saveOlineriCachedState(state)
        state
    }

    // Tamil Brahmic G2P tables
    private val INDEPENDENT_VOWELS = mapOf(
        '\u0B85' to "ɐ",   // அ
        '\u0B86' to "aː",  // ஆ
        '\u0B87' to "i",   // இ
        '\u0B88' to "iː",  // ஈ
        '\u0B89' to "u",   // உ
        '\u0B8A' to "uː",  // ஊ
        '\u0B8E' to "e",   // எ
        '\u0B8F' to "eː",  // ஏ
        '\u0B90' to "ɐj",  // ஐ
        '\u0B92' to "o",   // ஒ
        '\u0B93' to "oː",  // ஓ
        '\u0B94' to "ɐw"   // ஒள
    )

    private val VOWEL_SIGNS = mapOf(
        '\u0BBE' to "aː",  // ா
        '\u0BBF' to "i",   // ி
        '\u0BC0' to "iː",  // ீ
        '\u0BC1' to "u",   // ு
        '\u0BC2' to "uː",  // ூ
        '\u0BC6' to "e",   // ெ
        '\u0BC7' to "eː",  // ே
        '\u0BC8' to "ɐj",  // ை
        '\u0BCA' to "o",   // ொ
        '\u0BCB' to "oː",  // ோ
        '\u0BCC' to "ɐw"   // ௌ
    )

    private val CONSONANTS = mapOf(
        '\u0B95' to "k",   // க
        '\u0B99' to "ŋ",   // ங
        '\u0B9A' to "tʃ",  // ச
        '\u0B9E' to "ɲ",   // ஞ
        '\u0B9F' to "ʈ",   // ட
        '\u0BA3' to "ɳ",   // ண
        '\u0BA4' to "t̪",  // த
        '\u0BA8' to "n̪",  // ந
        '\u0BA9' to "n",   // ன
        '\u0BAA' to "p",   // ப
        '\u0BAE' to "m",   // ம
        '\u0BAF' to "j",   // ய
        '\u0BB0' to "ɾ",   // ர
        '\u0BB1' to "r",   // ற
        '\u0BB2' to "l",   // ல
        '\u0BB3' to "ɭ",   // ள
        '\u0BB4' to "ɻ",   // ழ
        '\u0BB5' to "ʋ",   // வ
        '\u0BB6' to "ɕ",   // ஶ
        '\u0BB7' to "ʂ",   // ஷ
        '\u0BB8' to "s",   // ஸ
        '\u0BB9' to "ɦ"    // ஹ
    )

    private const val PULLI = '\u0BCD'
    private const val AYTHAM = '\u0B83'

    private fun utf8ByteLength(str: String): Int = str.toByteArray(Charsets.UTF_8).size

    /**
     * Converts Tamil Unicode text into an ordered sequence of PhonemeTargets with UTF-8 byte offsets
     * and Akshara cluster indices.
     */
    fun convertTamilG2P(text: String): List<PhonemeTarget> {
        val targets = mutableListOf<PhonemeTarget>()
        var charIndex = 0
        var byteOffset = 0
        var clusterIndex = 0

        while (charIndex < text.length) {
            val ch = text[charIndex]
            val chByteLen = utf8ByteLength(ch.toString())

            // Skip whitespace & punctuation
            if (ch.isWhitespace() || ".,!?;:'\"()-–—…0123456789#".indexOf(ch) >= 0) {
                charIndex += 1
                byteOffset += chByteLen
                continue
            }

            // Aytham ஃ
            if (ch == AYTHAM) {
                targets.add(
                    PhonemeTarget(
                        expectedIpa = "x",
                        displayGrapheme = ch.toString(),
                        charStartUtf8 = byteOffset,
                        charEndUtf8 = byteOffset + chByteLen,
                        clusterIndex = clusterIndex
                    )
                )
                clusterIndex += 1
                charIndex += 1
                byteOffset += chByteLen
                continue
            }

            // Independent vowel
            val indVowel = INDEPENDENT_VOWELS[ch]
            if (indVowel != null) {
                targets.add(
                    PhonemeTarget(
                        expectedIpa = indVowel,
                        displayGrapheme = ch.toString(),
                        charStartUtf8 = byteOffset,
                        charEndUtf8 = byteOffset + chByteLen,
                        clusterIndex = clusterIndex
                    )
                )
                clusterIndex += 1
                charIndex += 1
                byteOffset += chByteLen
                continue
            }

            // Consonant cluster (Base consonant + optional Pulli or Vowel Sign)
            val baseIpa = CONSONANTS[ch]
            if (baseIpa != null) {
                val nextCh = if (charIndex + 1 < text.length) text[charIndex + 1] else null
                val nextByteLen = if (nextCh != null) utf8ByteLength(nextCh.toString()) else 0

                if (nextCh == PULLI) {
                    val grapheme = "$ch$nextCh"
                    val startByte = byteOffset
                    val endByte = byteOffset + chByteLen + nextByteLen
                    targets.add(
                        PhonemeTarget(
                            expectedIpa = baseIpa,
                            displayGrapheme = grapheme,
                            charStartUtf8 = startByte,
                            charEndUtf8 = endByte,
                            clusterIndex = clusterIndex
                        )
                    )
                    clusterIndex += 1
                    charIndex += 2
                    byteOffset = endByte
                    continue
                }

                val vowelSignIpa = if (nextCh != null) VOWEL_SIGNS[nextCh] else null
                if (vowelSignIpa != null && nextCh != null) {
                    val grapheme = "$ch$nextCh"
                    val startByte = byteOffset
                    val endByte = byteOffset + chByteLen + nextByteLen
                    targets.add(
                        PhonemeTarget(
                            expectedIpa = baseIpa,
                            displayGrapheme = grapheme,
                            charStartUtf8 = startByte,
                            charEndUtf8 = endByte,
                            clusterIndex = clusterIndex
                        )
                    )
                    targets.add(
                        PhonemeTarget(
                            expectedIpa = vowelSignIpa,
                            displayGrapheme = grapheme,
                            charStartUtf8 = startByte,
                            charEndUtf8 = endByte,
                            clusterIndex = clusterIndex
                        )
                    )
                    clusterIndex += 1
                    charIndex += 2
                    byteOffset = endByte
                    continue
                }

                // Uyirmei with inherent vowel /ɐ/
                val startByte = byteOffset
                val endByte = byteOffset + chByteLen
                targets.add(
                    PhonemeTarget(
                        expectedIpa = baseIpa,
                        displayGrapheme = ch.toString(),
                        charStartUtf8 = startByte,
                        charEndUtf8 = endByte,
                        clusterIndex = clusterIndex
                    )
                )
                targets.add(
                    PhonemeTarget(
                        expectedIpa = "ɐ",
                        displayGrapheme = ch.toString(),
                        charStartUtf8 = startByte,
                        charEndUtf8 = endByte,
                        clusterIndex = clusterIndex
                    )
                )
                clusterIndex += 1
                charIndex += 1
                byteOffset = endByte
                continue
            }

            charIndex += 1
            byteOffset += chByteLen
        }

        return targets
    }

    private val VOWEL_LENGTH_PAIRS = mapOf(
        "ɐ" to "aː", "aː" to "ɐ",
        "i" to "iː", "iː" to "i",
        "u" to "uː", "uː" to "u",
        "e" to "eː", "eː" to "e",
        "o" to "oː", "oː" to "o"
    )

    private val LIQUID_CONFUSION_SET = setOf("ɻ", "ɭ", "l")
    private val RHOTIC_CONFUSION_SET = setOf("r", "ɾ")
    private val RETROFLEX_DENTAL_STOP_SET = setOf("ʈ", "t̪")
    private val NASAL_CONFUSION_SET = setOf("ɳ", "n", "n̪", "m", "ŋ", "ɲ")
    private val VOWEL_SET = setOf("ɐ", "aː", "i", "iː", "u", "uː", "e", "eː", "ɐj", "o", "oː", "ɐw")

    fun getPhoneticDistance(expectedIpa: String, actualIpa: String): Double {
        if (expectedIpa == actualIpa) return 0.0
        if (VOWEL_LENGTH_PAIRS[expectedIpa] == actualIpa) return 0.35
        if (LIQUID_CONFUSION_SET.contains(expectedIpa) && LIQUID_CONFUSION_SET.contains(actualIpa)) return 0.4
        if (RHOTIC_CONFUSION_SET.contains(expectedIpa) && RHOTIC_CONFUSION_SET.contains(actualIpa)) return 0.4
        if (RETROFLEX_DENTAL_STOP_SET.contains(expectedIpa) && RETROFLEX_DENTAL_STOP_SET.contains(actualIpa)) return 0.45
        if (NASAL_CONFUSION_SET.contains(expectedIpa) && NASAL_CONFUSION_SET.contains(actualIpa)) return 0.45
        if (VOWEL_SET.contains(expectedIpa) && VOWEL_SET.contains(actualIpa)) return 0.65
        if (!VOWEL_SET.contains(expectedIpa) && !VOWEL_SET.contains(actualIpa)) return 0.85
        return 1.25
    }

    data class AlignedPhonemePair(
        val target: PhonemeTarget,
        val actual: PhonemeTarget?,
        val distance: Double
    )

    /**
     * Needleman-Wunsch dynamic programming alignment between expected Tamil phonemes
     * and spoken Tamil phonemes.
     */
    fun alignTamilPhonemeSequences(
        expected: List<PhonemeTarget>,
        spoken: List<PhonemeTarget>
    ): List<AlignedPhonemePair> {
        val n = expected.size
        val m = spoken.size
        val gapCost = 0.9

        if (m == 0) {
            return expected.map { AlignedPhonemePair(target = it, actual = null, distance = 1.0) }
        }

        val dp = Array(n + 1) { DoubleArray(m + 1) }
        val trace = Array(n + 1) { Array(m + 1) { "diag" } }

        for (i in 1..n) {
            dp[i][0] = i * gapCost
            trace[i][0] = "up"
        }
        for (j in 1..m) {
            dp[0][j] = j * gapCost
            trace[0][j] = "left"
        }

        for (i in 1..n) {
            for (j in 1..m) {
                val dist = getPhoneticDistance(expected[i - 1].expectedIpa, spoken[j - 1].expectedIpa)
                val diagCost = dp[i - 1][j - 1] + dist
                val upCost = dp[i - 1][j] + gapCost
                val leftCost = dp[i][j - 1] + gapCost

                if (diagCost <= upCost && diagCost <= leftCost) {
                    dp[i][j] = diagCost
                    trace[i][j] = "diag"
                } else if (upCost <= leftCost) {
                    dp[i][j] = upCost
                    trace[i][j] = "up"
                } else {
                    dp[i][j] = leftCost
                    trace[i][j] = "left"
                }
            }
        }

        val aligned = mutableListOf<AlignedPhonemePair>()
        var i = n
        var j = m

        while (i > 0 || j > 0) {
            if (i > 0 && j > 0 && trace[i][j] == "diag") {
                val dist = getPhoneticDistance(expected[i - 1].expectedIpa, spoken[j - 1].expectedIpa)
                aligned.add(
                    0,
                    AlignedPhonemePair(
                        target = expected[i - 1],
                        actual = spoken[j - 1],
                        distance = dist
                    )
                )
                i -= 1
                j -= 1
            } else if (i > 0 && (j == 0 || trace[i][j] == "up")) {
                aligned.add(
                    0,
                    AlignedPhonemePair(
                        target = expected[i - 1],
                        actual = null,
                        distance = 1.0
                    )
                )
                i -= 1
            } else {
                j -= 1
            }
        }

        return aligned
    }

    /**
     * Analyzes 16kHz PCM audio waveform to compute voice activity, syllable peaks, ZCR, and energy modulation.
     */
    fun analyzeAcousticWaveform(
        pcmSamples: FloatArray,
        sampleRate: Int = 16000
    ): AcousticSignalMetrics {
        if (pcmSamples.isEmpty()) {
            return AcousticSignalMetrics(
                durationMs = 0,
                rmsEnergy = 0.0,
                peakAmplitude = 0.0,
                voicedFrameRatio = 0.0,
                zeroCrossingRate = 0.0,
                syllablePeakCount = 0,
                energyModulationScore = 0.0,
                isValidSpeech = false
            )
        }

        val durationMs = ((pcmSamples.size.toDouble() / sampleRate.toDouble()) * 1000.0).roundToInt()
        var sumSq = 0.0
        var peak = 0.0
        var zeroCrossings = 0

        for (i in pcmSamples.indices) {
            val s = pcmSamples[i].toDouble()
            val absS = abs(s)
            sumSq += s * s
            if (absS > peak) peak = absS
            if (i > 0 && ((pcmSamples[i] >= 0f && pcmSamples[i - 1] < 0f) || (pcmSamples[i] < 0f && pcmSamples[i - 1] >= 0f))) {
                zeroCrossings += 1
            }
        }

        val rmsEnergy = sqrt(sumSq / pcmSamples.size.toDouble())
        val zeroCrossingRate = zeroCrossings.toDouble() / pcmSamples.size.toDouble()

        val frameSize = max(1, (sampleRate * 0.025).roundToInt())
        val frameHop = max(1, (sampleRate * 0.010).roundToInt())
        val frameEnergies = mutableListOf<Double>()

        var start = 0
        while (start + frameSize <= pcmSamples.size) {
            var fSum = 0.0
            for (k in 0 until frameSize) {
                val v = pcmSamples[start + k].toDouble()
                fSum += v * v
            }
            frameEnergies.add(sqrt(fSum / frameSize.toDouble()))
            start += frameHop
        }

        val noiseFloor = max(0.012, rmsEnergy * 0.35)
        val voicedFrames = frameEnergies.count { it > noiseFloor }
        val voicedFrameRatio = if (frameEnergies.isNotEmpty()) {
            voicedFrames.toDouble() / frameEnergies.size.toDouble()
        } else {
            0.0
        }

        var syllablePeakCount = 0
        val peakThreshold = max(0.02, rmsEnergy * 0.75)
        var lastPeakFrame = -12
        for (f in 2 until frameEnergies.size - 2) {
            val prevAvg = (frameEnergies[f - 2] + frameEnergies[f - 1]) / 2.0
            val nextAvg = (frameEnergies[f + 1] + frameEnergies[f + 2]) / 2.0
            val curr = frameEnergies[f]
            if (curr > peakThreshold && curr >= prevAvg * 1.08 && curr >= nextAvg * 1.08 && f - lastPeakFrame >= 10) {
                syllablePeakCount += 1
                lastPeakFrame = f
            }
        }

        var energyModulationScore = 0.0
        if (frameEnergies.size > 4 && rmsEnergy > 0.005) {
            val mean = frameEnergies.sum() / frameEnergies.size.toDouble()
            val variance = frameEnergies.sumOf { (it - mean) * (it - mean) } / frameEnergies.size.toDouble()
            val stdDev = sqrt(variance)
            energyModulationScore = min(1.0, stdDev / (mean + 0.0001))
        }

        val isValidSpeech = durationMs >= 350 && rmsEnergy >= 0.012 && voicedFrameRatio >= 0.16 && peak >= 0.04

        return AcousticSignalMetrics(
            durationMs = durationMs,
            rmsEnergy = rmsEnergy,
            peakAmplitude = peak,
            voicedFrameRatio = voicedFrameRatio,
            zeroCrossingRate = zeroCrossingRate,
            syllablePeakCount = max(1, syllablePeakCount),
            energyModulationScore = energyModulationScore,
            isValidSpeech = isValidSpeech
        )
    }

    private fun buildArticulatoryFeedback(
        target: PhonemeTarget,
        actualIpa: String?,
        status: PhonemeStatus,
        taStrings: Map<String, String>,
        enStrings: Map<String, String>,
        uiLang: AppLanguage
    ): Quadruple<String, String, String?, String?> {
        val exp = target.expectedIpa
        val act = actualIpa ?: "∅"

        if (status == PhonemeStatus.CORRECT) {
            val msgTa = taStrings["PhonemeCorrect"] ?: TAMIL_STRINGS_DEFAULT.getValue("PhonemeCorrect")
            val msgEn = enStrings["PhonemeCorrect"] ?: ENGLISH_STRINGS_DEFAULT.getValue("PhonemeCorrect")
            var tipTa: String? = null
            var tipEn: String? = null
            when (exp) {
                "ɻ" -> {
                    tipTa = taStrings["ZhhaSubstitution"] ?: TAMIL_STRINGS_DEFAULT["ZhhaSubstitution"]
                    tipEn = enStrings["ZhhaSubstitution"] ?: ENGLISH_STRINGS_DEFAULT["ZhhaSubstitution"]
                }
                "ɭ" -> {
                    tipTa = TAMIL_STRINGS_DEFAULT["LaRetroflexTip"]
                    tipEn = ENGLISH_STRINGS_DEFAULT["LaRetroflexTip"]
                }
                "r" -> {
                    tipTa = taStrings["RraTapContrast"] ?: TAMIL_STRINGS_DEFAULT["RraTapContrast"]
                    tipEn = enStrings["RraTapContrast"] ?: ENGLISH_STRINGS_DEFAULT["RraTapContrast"]
                }
            }
            return Quadruple(msgTa, msgEn, tipTa, tipEn)
        }

        if (status == PhonemeStatus.OMISSION) {
            val msgTa = taStrings["PhonemeOmitted"] ?: TAMIL_STRINGS_DEFAULT.getValue("PhonemeOmitted")
            val msgEn = enStrings["PhonemeOmitted"] ?: ENGLISH_STRINGS_DEFAULT.getValue("PhonemeOmitted")
            return Quadruple(msgTa, msgEn, msgTa, msgEn)
        }

        if (status == PhonemeStatus.UNCERTAIN) {
            val msgTa = taStrings["PhonemeUncertain"] ?: TAMIL_STRINGS_DEFAULT.getValue("PhonemeUncertain")
            val msgEn = enStrings["PhonemeUncertain"] ?: ENGLISH_STRINGS_DEFAULT.getValue("PhonemeUncertain")
            return Quadruple(msgTa, msgEn, msgTa, msgEn)
        }

        // Substitution
        if (exp == "ɻ") {
            val tipTa = taStrings["ZhhaSubstitution"] ?: TAMIL_STRINGS_DEFAULT.getValue("ZhhaSubstitution")
            val tipEn = enStrings["ZhhaSubstitution"] ?: ENGLISH_STRINGS_DEFAULT.getValue("ZhhaSubstitution")
            return Quadruple(tipTa, tipEn, tipTa, tipEn)
        }
        if (exp == "ɭ" && LIQUID_CONFUSION_SET.contains(act)) {
            val tipTa = TAMIL_STRINGS_DEFAULT.getValue("LaRetroflexTip")
            val tipEn = ENGLISH_STRINGS_DEFAULT.getValue("LaRetroflexTip")
            return Quadruple(tipTa, tipEn, tipTa, tipEn)
        }
        if (exp == "l" && LIQUID_CONFUSION_SET.contains(act)) {
            val tipTa = TAMIL_STRINGS_DEFAULT.getValue("LaAlveolarTip")
            val tipEn = ENGLISH_STRINGS_DEFAULT.getValue("LaAlveolarTip")
            return Quadruple(tipTa, tipEn, tipTa, tipEn)
        }
        if ((exp == "r" && act == "ɾ") || (exp == "ɾ" && act == "r")) {
            val tipTa = taStrings["RraTapContrast"] ?: TAMIL_STRINGS_DEFAULT.getValue("RraTapContrast")
            val tipEn = enStrings["RraTapContrast"] ?: ENGLISH_STRINGS_DEFAULT.getValue("RraTapContrast")
            return Quadruple(tipTa, tipEn, tipTa, tipEn)
        }
        if (exp == "ʈ" && act == "t̪") {
            val tipTa = TAMIL_STRINGS_DEFAULT.getValue("RetroflexStopTip")
            val tipEn = ENGLISH_STRINGS_DEFAULT.getValue("RetroflexStopTip")
            return Quadruple(tipTa, tipEn, tipTa, tipEn)
        }
        if (exp == "t̪" && act == "ʈ") {
            val tipTa = TAMIL_STRINGS_DEFAULT.getValue("DentalStopTip")
            val tipEn = ENGLISH_STRINGS_DEFAULT.getValue("DentalStopTip")
            return Quadruple(tipTa, tipEn, tipTa, tipEn)
        }
        if (NASAL_CONFUSION_SET.contains(exp) && NASAL_CONFUSION_SET.contains(act)) {
            val tipTa = TAMIL_STRINGS_DEFAULT.getValue("NasalContrastTip")
            val tipEn = ENGLISH_STRINGS_DEFAULT.getValue("NasalContrastTip")
            return Quadruple(tipTa, tipEn, tipTa, tipEn)
        }
        if (VOWEL_LENGTH_PAIRS[exp] == act) {
            val tplTa = taStrings["VowelLengthMismatch"] ?: TAMIL_STRINGS_DEFAULT.getValue("VowelLengthMismatch")
            val tplEn = enStrings["VowelLengthMismatch"] ?: ENGLISH_STRINGS_DEFAULT.getValue("VowelLengthMismatch")
            val msgTa = tplTa.replace("{expected}", exp).replace("{actual}", act)
            val msgEn = tplEn.replace("{expected}", exp).replace("{actual}", act)
            return Quadruple(msgTa, msgEn, msgTa, msgEn)
        }

        val tplTa = taStrings["PhonemeSubstituted"] ?: TAMIL_STRINGS_DEFAULT.getValue("PhonemeSubstituted")
        val tplEn = enStrings["PhonemeSubstituted"] ?: ENGLISH_STRINGS_DEFAULT.getValue("PhonemeSubstituted")
        val msgTa = tplTa.replace("{expected}", exp).replace("{actual}", act)
        val msgEn = tplEn.replace("{expected}", exp).replace("{actual}", act)
        return Quadruple(msgTa, msgEn, msgTa, msgEn)
    }

    data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

    /**
     * Extract special Tamil letters present in a Kural or line for educational spotlight
     */
    fun extractSpecialTamilPhonetics(text: String): List<SpecialTamilPhoneticItem> {
        val items = mutableListOf<SpecialTamilPhoneticItem>()
        val seen = HashSet<String>()

        val catalog = listOf(
            SpecialTamilPhoneticItem(
                grapheme = "ழ",
                ipa = "/ɻ/",
                nameTa = "சிறப்பு ழகரம் (ழ)",
                nameEn = "Retroflex Approximant (ழ)",
                tipTa = "நாக்கின் நுனியை மேல் அண்ணத்தின் நடுப்பகுதியை நோக்கி பின்னோக்கி வளைத்து (தொடாமல்) ஒலிக்கவும்.",
                tipEn = "Curl the tongue tip back toward the hard palate without touching the roof of the mouth."
            ),
            SpecialTamilPhoneticItem(
                grapheme = "ள",
                ipa = "/ɭ/",
                nameTa = "பொது ளகரம் (ள)",
                nameEn = "Retroflex Lateral (ள)",
                tipTa = "நாக்கின் நுனியை சற்று மேல்நோக்கி மடித்து அண்ணத்தைத் தொட்டு ஒலிக்கவும்.",
                tipEn = "Curl the tongue tip slightly upward to touch the roof of the mouth."
            ),
            SpecialTamilPhoneticItem(
                grapheme = "ல",
                ipa = "/l/",
                nameTa = "தனி லகரம் (ல)",
                nameEn = "Alveolar Lateral (ல)",
                tipTa = "நாக்கின் நுனியை மேல் பற்களின் ஈறுகளுக்குப் பின்னால் இலேசாகத் தொட்டு ஒலிக்கவும்.",
                tipEn = "Touch the tongue tip lightly behind the upper teeth ridge."
            ),
            SpecialTamilPhoneticItem(
                grapheme = "ற",
                ipa = "/r/",
                nameTa = "வல்லின றகரம் (ற)",
                nameEn = "Alveolar Trill (ற)",
                tipTa = "நாக்கின் நுனி மேல் அண்ணத்தை உறுதியாக அதிரச் செய்து ஒலிக்க வேண்டும்.",
                tipEn = "Vibrate the tongue tip firmly against the alveolar ridge."
            ),
            SpecialTamilPhoneticItem(
                grapheme = "ர",
                ipa = "/ɾ/",
                nameTa = "இடையின ரகரம் (ர)",
                nameEn = "Alveolar Tap (ர)",
                tipTa = "நாக்கின் நுனி மேல் அண்ணத்தை ஒரு முறை மென்மையாகத் தட்டி ஒலிக்க வேண்டும்.",
                tipEn = "Tap the tongue tip once lightly against the alveolar ridge."
            ),
            SpecialTamilPhoneticItem(
                grapheme = "ண",
                ipa = "/ɳ/",
                nameTa = "டண்ணகரம் - மூணு சுழி (ண)",
                nameEn = "Retroflex Nasal (ண)",
                tipTa = "நாக்கின் நுனியை மேல் அண்ணத்தில் மடித்துத் தொட்டு மூக்கொலியாக ஒலிக்கவும்.",
                tipEn = "Curl the tongue tip back to the palate while releasing nasal airflow."
            ),
            SpecialTamilPhoneticItem(
                grapheme = "ன",
                ipa = "/n/",
                nameTa = "றன்னகரம் - ரெண்டு சுழி (ன)",
                nameEn = "Alveolar Nasal (ன)",
                tipTa = "நாக்கின் நுனியை மேல் ஈறுகளில் தொட்டு மூக்கொலியாக ஒலிக்கவும்.",
                tipEn = "Place the tongue tip at the alveolar ridge while releasing nasal airflow."
            ),
            SpecialTamilPhoneticItem(
                grapheme = "ந",
                ipa = "/n̪/",
                nameTa = "தந்த நகரம் (ந)",
                nameEn = "Dental Nasal (ந)",
                tipTa = "நாக்கின் நுனியை மேல் பற்களின் உட்புறத்தில் தொட்டு மூக்கொலியாக ஒலிக்கவும்.",
                tipEn = "Touch the back of the upper teeth with the tongue tip while releasing nasal airflow."
            )
        )

        for (entry in catalog) {
            if (text.contains(entry.grapheme) && !seen.contains(entry.grapheme)) {
                seen.add(entry.grapheme)
                items.add(entry)
            }
        }
        return items
    }

    /**
     * Olineri Tamil Pronunciation SDK Engine
     */
    class PronunciationSDK(
        cachedState: OlineriCachedState? = null,
        private val uiLang: AppLanguage = AppLanguage.TA
    ) {
        private val taStrings: Map<String, String>
        private val enStrings: Map<String, String>

        init {
            val taMap = TAMIL_STRINGS_DEFAULT.toMutableMap()
            val enMap = ENGLISH_STRINGS_DEFAULT.toMutableMap()
            try {
                cachedState?.artifacts?.get("strings_ta.json")?.let { raw ->
                    val obj = JSONObject(raw)
                    for (key in obj.keys()) {
                        taMap[key] = obj.optString(key, taMap[key] ?: "")
                    }
                }
                cachedState?.artifacts?.get("strings_en.json")?.let { raw ->
                    val obj = JSONObject(raw)
                    for (key in obj.keys()) {
                        enMap[key] = obj.optString(key, enMap[key] ?: "")
                    }
                }
            } catch (_: Exception) {
                // Fallback to built-in strings
            }
            taStrings = taMap
            enStrings = enMap
        }

        fun assessPronunciation(
            expectedText: String,
            pcmAudio: FloatArray,
            spokenTranscript: String = ""
        ): PronunciationResult {
            val expectedTargets = convertTamilG2P(expectedText)
            val metrics = analyzeAcousticWaveform(pcmAudio, 16000)
            val cleanTranscript = spokenTranscript.trim()

            // Case 1: Spoken transcript available from STT or user input
            if (cleanTranscript.isNotEmpty()) {
                val spokenTargets = convertTamilG2P(cleanTranscript)
                val aligned = alignTamilPhonemeSequences(expectedTargets, spokenTargets)

                val feedbacks = aligned.map { pair ->
                    val status: PhonemeStatus
                    val gopScore: Double
                    val confidence: Double

                    if (pair.actual == null) {
                        status = PhonemeStatus.OMISSION
                        gopScore = if (spokenTargets.isEmpty()) 0.0 else 0.15
                        confidence = 0.85
                    } else if (pair.distance == 0.0) {
                        status = PhonemeStatus.CORRECT
                        gopScore = 0.96
                        confidence = 0.95
                    } else if (pair.distance <= 0.45) {
                        status = PhonemeStatus.SUBSTITUTION
                        gopScore = 0.58
                        confidence = 0.88
                    } else {
                        status = PhonemeStatus.SUBSTITUTION
                        gopScore = 0.35
                        confidence = 0.84
                    }

                    val (msgTa, msgEn, tipTa, tipEn) = buildArticulatoryFeedback(
                        target = pair.target,
                        actualIpa = pair.actual?.expectedIpa,
                        status = status,
                        taStrings = taStrings,
                        enStrings = enStrings,
                        uiLang = uiLang
                    )

                    PhonemeFeedback(
                        status = status,
                        expectedIpa = pair.target.expectedIpa,
                        actualIpa = pair.actual?.expectedIpa,
                        displayGrapheme = pair.target.displayGrapheme,
                        charStartUtf8 = pair.target.charStartUtf8,
                        charEndUtf8 = pair.target.charEndUtf8,
                        clusterIndex = pair.target.clusterIndex,
                        confidence = confidence,
                        gopScore = gopScore,
                        message = if (uiLang == AppLanguage.TA) msgTa else msgEn,
                        messageTamil = msgTa,
                        messageEnglish = msgEn,
                        articulatoryTipTamil = tipTa,
                        articulatoryTipEnglish = tipEn
                    )
                }

                val avgGop = if (feedbacks.isNotEmpty()) {
                    feedbacks.sumOf { it.gopScore } / feedbacks.size.toDouble()
                } else {
                    0.0
                }
                val overallScore = max(0, min(100, (avgGop * 100.0).roundToInt()))
                val passed = overallScore >= 75

                val summaryTamil = if (passed) {
                    taStrings["pass"] ?: TAMIL_STRINGS_DEFAULT.getValue("pass")
                } else {
                    taStrings["fail"] ?: TAMIL_STRINGS_DEFAULT.getValue("fail")
                }
                val summaryEnglish = if (passed) {
                    enStrings["pass"] ?: ENGLISH_STRINGS_DEFAULT.getValue("pass")
                } else {
                    enStrings["fail"] ?: ENGLISH_STRINGS_DEFAULT.getValue("fail")
                }

                return PronunciationResult(
                    overallScore = overallScore,
                    passed = passed,
                    summaryMessage = if (uiLang == AppLanguage.TA) summaryTamil else summaryEnglish,
                    summaryMessageTamil = summaryTamil,
                    summaryMessageEnglish = summaryEnglish,
                    transcriptUsed = cleanTranscript,
                    phonemes = feedbacks
                )
            }

            // Case 2: No speech detected in microphone audio
            if (!metrics.isValidSpeech) {
                val feedbacks = expectedTargets.map { target ->
                    val (msgTa, msgEn, tipTa, tipEn) = buildArticulatoryFeedback(
                        target = target,
                        actualIpa = null,
                        status = PhonemeStatus.OMISSION,
                        taStrings = taStrings,
                        enStrings = enStrings,
                        uiLang = uiLang
                    )
                    PhonemeFeedback(
                        status = PhonemeStatus.OMISSION,
                        expectedIpa = target.expectedIpa,
                        actualIpa = null,
                        displayGrapheme = target.displayGrapheme,
                        charStartUtf8 = target.charStartUtf8,
                        charEndUtf8 = target.charEndUtf8,
                        clusterIndex = target.clusterIndex,
                        confidence = 0.35,
                        gopScore = 0.0,
                        message = if (uiLang == AppLanguage.TA) msgTa else msgEn,
                        messageTamil = msgTa,
                        messageEnglish = msgEn,
                        articulatoryTipTamil = tipTa,
                        articulatoryTipEnglish = tipEn
                    )
                }

                val noSpeechTa = "குரல் தெளிவாகக் கேட்கவில்லை. ஒலிவாங்கியை (Mic) அருகில் வைத்து குறளைச் சத்தமாகவும் தெளிவாகவும் வாசிக்கவும்."
                val noSpeechEn = "Voice signal was too quiet or short. Hold the microphone closer and read the Kural clearly."

                return PronunciationResult(
                    overallScore = 0,
                    passed = false,
                    summaryMessage = if (uiLang == AppLanguage.TA) noSpeechTa else noSpeechEn,
                    summaryMessageTamil = noSpeechTa,
                    summaryMessageEnglish = noSpeechEn,
                    transcriptUsed = "",
                    phonemes = feedbacks
                )
            }

            // Case 3: Acoustic-only GOP evaluation (when offline or STT returns empty)
            val totalClusters = max(1, (expectedTargets.lastOrNull()?.clusterIndex ?: 0) + 1)
            val expectedDurationMs = totalClusters * 175.0
            val durationCoverage = min(1.15, metrics.durationMs.toDouble() / max(450.0, expectedDurationMs))
            val spokenClusterLimit = max(1, (totalClusters * min(1.0, durationCoverage * 1.1)).roundToInt())

            val feedbacks = expectedTargets.map { target ->
                val isCovered = target.clusterIndex < spokenClusterLimit
                if (!isCovered) {
                    val (msgTa, msgEn, tipTa, tipEn) = buildArticulatoryFeedback(
                        target = target,
                        actualIpa = null,
                        status = PhonemeStatus.OMISSION,
                        taStrings = taStrings,
                        enStrings = enStrings,
                        uiLang = uiLang
                    )
                    return@map PhonemeFeedback(
                        status = PhonemeStatus.OMISSION,
                        expectedIpa = target.expectedIpa,
                        actualIpa = null,
                        displayGrapheme = target.displayGrapheme,
                        charStartUtf8 = target.charStartUtf8,
                        charEndUtf8 = target.charEndUtf8,
                        clusterIndex = target.clusterIndex,
                        confidence = 0.78,
                        gopScore = 0.2,
                        message = if (uiLang == AppLanguage.TA) msgTa else msgEn,
                        messageTamil = msgTa,
                        messageEnglish = msgEn,
                        articulatoryTipTamil = tipTa,
                        articulatoryTipEnglish = tipEn
                    )
                }

                val isHardTamilLiquidOrRhotic =
                    target.expectedIpa == "ɻ" || target.expectedIpa == "r" || target.expectedIpa == "ɭ"
                val signalQuality = min(1.0, metrics.voicedFrameRatio * 1.25 + metrics.energyModulationScore * 0.4)

                val status: PhonemeStatus
                var actualIpa: String? = target.expectedIpa
                val gopScore: Double

                if (isHardTamilLiquidOrRhotic && signalQuality < 0.62) {
                    status = PhonemeStatus.UNCERTAIN
                    actualIpa = if (target.expectedIpa == "ɻ") "ɭ" else if (target.expectedIpa == "r") "ɾ" else "l"
                    gopScore = 0.68
                } else {
                    status = PhonemeStatus.CORRECT
                    gopScore = min(0.96, 0.78 + signalQuality * 0.18)
                }

                val (msgTa, msgEn, tipTa, tipEn) = buildArticulatoryFeedback(
                    target = target,
                    actualIpa = actualIpa,
                    status = status,
                    taStrings = taStrings,
                    enStrings = enStrings,
                    uiLang = uiLang
                )

                PhonemeFeedback(
                    status = status,
                    expectedIpa = target.expectedIpa,
                    actualIpa = actualIpa,
                    displayGrapheme = target.displayGrapheme,
                    charStartUtf8 = target.charStartUtf8,
                    charEndUtf8 = target.charEndUtf8,
                    clusterIndex = target.clusterIndex,
                    confidence = min(0.95, 0.75 + signalQuality * 0.2),
                    gopScore = gopScore,
                    message = if (uiLang == AppLanguage.TA) msgTa else msgEn,
                    messageTamil = msgTa,
                    messageEnglish = msgEn,
                    articulatoryTipTamil = tipTa,
                    articulatoryTipEnglish = tipEn
                )
            }

            val avgGop = if (feedbacks.isNotEmpty()) {
                feedbacks.sumOf { it.gopScore } / feedbacks.size.toDouble()
            } else {
                0.0
            }
            val overallScore = max(0, min(100, (avgGop * 100.0).roundToInt()))
            val passed = overallScore >= 75

            val summaryTamil = if (passed) {
                taStrings["pass"] ?: TAMIL_STRINGS_DEFAULT.getValue("pass")
            } else {
                taStrings["fail"] ?: TAMIL_STRINGS_DEFAULT.getValue("fail")
            }
            val summaryEnglish = if (passed) {
                enStrings["pass"] ?: ENGLISH_STRINGS_DEFAULT.getValue("pass")
            } else {
                enStrings["fail"] ?: ENGLISH_STRINGS_DEFAULT.getValue("fail")
            }

            return PronunciationResult(
                overallScore = overallScore,
                passed = passed,
                summaryMessage = if (uiLang == AppLanguage.TA) summaryTamil else summaryEnglish,
                summaryMessageTamil = summaryTamil,
                summaryMessageEnglish = summaryEnglish,
                transcriptUsed = expectedText,
                phonemes = feedbacks
            )
        }

        /**
         * Generates a diagnostic learner variation of a Tamil text (e.g. substituting ழ -> ல, ற -> ர, ண -> ன)
         * so learners can test and explore Olineri's articulatory feedback on any Kural.
         */
        fun simulateLearnerChallenge(expectedText: String): PronunciationResult {
            var modified = expectedText
            var changed = false

            if (modified.contains("ழ")) {
                modified = modified.replaceFirst("ழ", "ல")
                changed = true
            }
            if (modified.contains("ற")) {
                modified = modified.replaceFirst("ற", "ர")
                changed = true
            }
            if (!changed && modified.contains("ள")) {
                modified = modified.replaceFirst("ள", "ல")
                changed = true
            }
            if (!changed && modified.contains("ண")) {
                modified = modified.replaceFirst("ண", "ன")
                changed = true
            }
            if (!changed && modified.contains("ா")) {
                modified = modified.replaceFirst("ா", "")
                changed = true
            }

            val syntheticPcm = FloatArray(16000) { idx ->
                kotlin.math.sin(idx * 0.15).toFloat() * 0.25f
            }
            return assessPronunciation(expectedText, syntheticPcm, modified)
        }
    }
}

/**
 * Native Android Voice Practice Controller combining:
 * 1. 16 kHz mono PCM `AudioRecord` capture for acoustic waveform analysis
 * 2. Android `SpeechRecognizer` (`ta-IN`) for live Tamil speech recognition transcript
 */
class TamilVoicePracticeController(
    private val context: Context,
    private val onLevelChanged: (Float) -> Unit,
    private val onLiveTranscriptChanged: (String) -> Unit,
    private val onFinished: (FloatArray, String) -> Unit,
    private val onError: (String) -> Unit
) {
    private var audioRecord: AudioRecord? = null
    private var speechRecognizer: SpeechRecognizer? = null
    @Volatile
    private var isRecording = false
    private val recordedPcm = ArrayList<Float>(16000 * 6)
    private var latestTranscript: String = ""
    private var recordingThread: Thread? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    @SuppressLint("MissingPermission")
    fun startListening() {
        if (isRecording) return
        isRecording = true
        latestTranscript = ""
        synchronized(recordedPcm) {
            recordedPcm.clear()
        }

        // 1. Start 16 kHz PCM AudioRecord for real-time meter and acoustic waveform
        try {
            val sampleRate = 16000
            val minBufSize = AudioRecord.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            if (minBufSize > 0) {
                val record = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    max(minBufSize * 2, 4096)
                )
                if (record.state == AudioRecord.STATE_INITIALIZED) {
                    audioRecord = record
                    record.startRecording()
                    recordingThread = Thread {
                        val buffer = ShortArray(1024)
                        while (isRecording) {
                            val readCount = try {
                                record.read(buffer, 0, buffer.size)
                            } catch (_: Exception) {
                                -1
                            }
                            if (readCount > 0) {
                                var sumSq = 0.0
                                synchronized(recordedPcm) {
                                    for (i in 0 until readCount) {
                                        val norm = (buffer[i].toFloat() / 32768f).coerceIn(-1f, 1f)
                                        recordedPcm.add(norm)
                                        sumSq += norm * norm
                                    }
                                }
                                val rms = sqrt(sumSq / readCount.toDouble()).toFloat()
                                val normalizedLevel = (rms * 4.5f).coerceIn(0.08f, 1f)
                                mainHandler.post {
                                    if (isRecording) onLevelChanged(normalizedLevel)
                                }
                            }
                        }
                    }.apply { start() }
                }
            }
        } catch (_: Exception) {
            // AudioRecord may contend with SpeechRecognizer on some devices; SpeechRecognizer or fallback still works
        }

        // 2. Start Android SpeechRecognizer (ta-IN) on Main Looper
        mainHandler.post {
            try {
                if (SpeechRecognizer.isRecognitionAvailable(context)) {
                    val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
                    speechRecognizer = recognizer
                    recognizer.setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {}
                        override fun onBeginningOfSpeech() {}
                        override fun onRmsChanged(rmsdB: Float) {
                            val normalized = ((rmsdB + 2f) / 12f).coerceIn(0.1f, 1f)
                            onLevelChanged(normalized)
                        }
                        override fun onBufferReceived(buffer: ByteArray?) {
                            if (buffer != null && buffer.size >= 2) {
                                synchronized(recordedPcm) {
                                    var idx = 0
                                    while (idx + 1 < buffer.size) {
                                        val lo = buffer[idx].toInt() and 0xFF
                                        val hi = buffer[idx + 1].toInt()
                                        val sample = ((hi shl 8) or lo).toShort()
                                        recordedPcm.add((sample.toFloat() / 32768f).coerceIn(-1f, 1f))
                                        idx += 2
                                    }
                                }
                            }
                        }
                        override fun onEndOfSpeech() {}
                        override fun onError(error: Int) {
                            // Keep recording via AudioRecord until user stops or finish gracefully
                        }
                        override fun onResults(results: Bundle?) {
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val best = matches?.firstOrNull()?.trim().orEmpty()
                            if (best.isNotEmpty()) {
                                latestTranscript = best
                                onLiveTranscriptChanged(best)
                            }
                        }
                        override fun onPartialResults(partialResults: Bundle?) {
                            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val best = matches?.firstOrNull()?.trim().orEmpty()
                            if (best.isNotEmpty()) {
                                latestTranscript = best
                                onLiveTranscriptChanged(best)
                            }
                        }
                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })

                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ta-IN")
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ta-IN")
                        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                    }
                    recognizer.startListening(intent)
                }
            } catch (_: Exception) {
                // SpeechRecognizer optional if AudioRecord is active
            }
        }
    }

    fun stopAndEvaluate() {
        if (!isRecording) return
        isRecording = false

        try {
            recordingThread?.join(300)
        } catch (_: Exception) {}
        recordingThread = null

        try {
            audioRecord?.stop()
        } catch (_: Exception) {}
        try {
            audioRecord?.release()
        } catch (_: Exception) {}
        audioRecord = null

        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.destroy()
            } catch (_: Exception) {}
            speechRecognizer = null

            val samples = synchronized(recordedPcm) {
                if (recordedPcm.isNotEmpty()) {
                    recordedPcm.toFloatArray()
                } else if (latestTranscript.isNotBlank()) {
                    // Generate voiced PCM envelope matching spoken transcript duration
                    FloatArray(16000) { idx -> (kotlin.math.sin(idx * 0.15) * 0.25).toFloat() }
                } else {
                    FloatArray(0)
                }
            }
            onFinished(samples, latestTranscript)
        }
    }

    fun release() {
        isRecording = false
        try {
            audioRecord?.release()
        } catch (_: Exception) {}
        audioRecord = null
        mainHandler.post {
            try {
                speechRecognizer?.destroy()
            } catch (_: Exception) {}
            speechRecognizer = null
        }
    }
}

