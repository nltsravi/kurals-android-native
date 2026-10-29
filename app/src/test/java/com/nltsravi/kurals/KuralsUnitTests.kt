package com.nltsravi.kurals

import com.nltsravi.kurals.constants.AppConstants
import com.nltsravi.kurals.data.KuralRepository
import com.nltsravi.kurals.data.PreferencesAndCollectionStorage
import com.nltsravi.kurals.model.AppLanguage
import com.nltsravi.kurals.model.MeaningType
import com.nltsravi.kurals.model.PhonemeStatus
import com.nltsravi.kurals.model.SearchMatchType
import com.nltsravi.kurals.model.SocialShareOptions
import com.nltsravi.kurals.service.ExportService
import com.nltsravi.kurals.service.NotificationService
import com.nltsravi.kurals.service.OlineriService
import com.nltsravi.kurals.service.SearchService
import com.nltsravi.kurals.ui.strings.Translations
import com.nltsravi.kurals.util.TextUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.File
import java.util.zip.ZipInputStream
import kotlin.math.sin

class KuralsUnitTests {

    companion object {
        @BeforeClass
        @JvmStatic
        fun setUpClass() {
            val assetsDir = listOf(
                File("src/main/assets/data"),
                File("app/src/main/assets/data")
            ).firstOrNull { it.exists() }
                ?: throw IllegalStateException("Cannot locate assets/data directory for unit tests")

            val detailJson = File(assetsDir, "detail.json").readText(Charsets.UTF_8)
            val kuralsJson = File(assetsDir, "thirukkural.json").readText(Charsets.UTF_8)
            KuralRepository.initializeFromJsonStrings(detailJson, kuralsJson)
        }
    }

    // 1. Data Loading & Integrity Tests
    @Test
    fun `loads all 1330 Kurals, 133 Chapters, and 3 Categories`() {
        val kurals = KuralRepository.getAllKurals()
        val chapters = KuralRepository.getAllChapters()
        val categories = KuralRepository.getAllCategories()

        assertEquals(1330, kurals.size)
        assertEquals(133, chapters.size)
        assertEquals(3, categories.size)

        val kural1 = KuralRepository.getKuralByNumber(1)
        assertNotNull(kural1)
        assertTrue(kural1!!.line1.contains("அகர முதல"))
        assertEquals("கடவுள் வாழ்த்து", kural1.chapterNameTamil)
        assertEquals("அறத்துப்பால்", kural1.categoryTamil)
        assertTrue(kural1.prm.isNotBlank())
        assertTrue(kural1.mnk.isNotBlank())

        val kural1330 = KuralRepository.getKuralByNumber(1330)
        assertNotNull(kural1330)
        assertEquals(133, kural1330!!.chapterNumber)
        assertEquals("காமத்துப்பால்", kural1330.categoryTamil)
    }

    // 2. Strict 4+3 Word Couplet Formatting & Uniform Font Size Tests
    @Test
    fun `all 1330 Kurals strictly follow 4 words on line 1 and 3 words on line 2 without ellipses`() {
        val allKurals = KuralRepository.getAllKurals()
        for (k in allKurals) {
            val (line1, line2) = TextUtils.formatKuralCouplet(k.line1, k.line2)
            assertFalse("Line 1 has ellipsis in Kural #${k.number}", line1.contains("..."))
            assertFalse("Line 2 has ellipsis in Kural #${k.number}", line2.contains("..."))
            val words1 = line1.split(" ").filter { it.isNotEmpty() }
            val words2 = line2.split(" ").filter { it.isNotEmpty() }
            assertEquals("Kural #${k.number} line 1 should have 4 words", 4, words1.size)
            assertEquals("Kural #${k.number} line 2 should have 3 words", 3, words2.size)

            val fontSize = TextUtils.calculateCoupletFontSize(line1, line2, 320f, 17f, 10.5f)
            assertTrue(fontSize in 10.5f..17f)
        }
    }

    // 3. Multi-Tiered Search Engine Tests
    @Test
    fun `searchKurals ranks exact Kural number, Chapter name, Tamil phrase, Transliteration, and English`() {
        // Exact Kural number
        val numRes = SearchService.searchKurals("1")
        assertTrue(numRes.isNotEmpty())
        assertEquals(1, numRes.first().kural.number)
        assertEquals(SearchMatchType.KURAL_NUMBER, numRes.first().matchType)

        // Exact Kural 1330
        val k1330Res = SearchService.searchKurals("1330")
        assertEquals(1330, k1330Res.first().kural.number)

        // Chapter name in Tamil
        val chRes = SearchService.searchKurals("வான்சிறப்பு")
        assertTrue(chRes.isNotEmpty())
        assertEquals(2, chRes.first().kural.chapterNumber)
        assertEquals(SearchMatchType.CHAPTER_NAME, chRes.first().matchType)

        // Tamil phrase
        val taRes = SearchService.searchKurals("அகர முதல")
        assertTrue(taRes.isNotEmpty())
        assertEquals(1, taRes.first().kural.number)

        // Transliteration ("Akara Mudhala" and phonetic "agaram")
        val akaraRes = SearchService.searchKurals("Akara Mudhala")
        assertTrue(akaraRes.isNotEmpty())
        assertEquals(1, akaraRes.first().kural.number)

        val trRes = SearchService.searchKurals("agaram")
        assertTrue(trRes.isNotEmpty())
        assertEquals(1, trRes.first().kural.number)

        // Category search
        val catRes = SearchService.searchKurals("அறத்துப்பால்")
        assertTrue(catRes.isNotEmpty())
        assertEquals(1, catRes.first().kural.categoryId)
    }

    // 4. Collections Pure Operations Tests
    @Test
    fun `collection add, remove, and reorder pure functions work accurately`() {
        val initial = PreferencesAndCollectionStorage.defaultCollections()
        assertEquals(2, initial.size)
        assertEquals("favorites", initial[0].id)

        val afterAdd = PreferencesAndCollectionStorage.addKuralToCollectionPure(initial, "favorites", 10)
        assertTrue(afterAdd[0].kuralNumbers.contains(10))

        // Duplicate add should be idempotent
        val afterDup = PreferencesAndCollectionStorage.addKuralToCollectionPure(afterAdd, "favorites", 10)
        assertEquals(afterAdd[0].kuralNumbers.size, afterDup[0].kuralNumbers.size)

        // Reorder
        val afterMove = PreferencesAndCollectionStorage.reorderKuralInCollectionPure(afterAdd, "favorites", 0, 2)
        assertEquals(listOf(2, 3, 1, 10), afterMove[0].kuralNumbers)

        // Remove
        val afterRemove = PreferencesAndCollectionStorage.removeKuralFromCollectionPure(afterMove, "favorites", 10)
        assertFalse(afterRemove[0].kuralNumbers.contains(10))
    }

    // 5. Social Share Formatting & Card Meaning Resolution Tests
    @Test
    fun `formatKuralForSocialShare formats all 7 scholar commentaries, transliteration, and hashtags`() {
        val kural1 = KuralRepository.getKuralByNumber(1)!!
        for (meaning in MeaningType.entries) {
            val text = TextUtils.formatKuralForSocialShare(
                kural1,
                SocialShareOptions(
                    meaningType = meaning,
                    includeTransliteration = true,
                    includeHashtags = true
                )
            )
            assertTrue(text.contains("திருக்குறள் #1"))
            assertTrue(text.contains(kural1.transliteration1))
            assertTrue(text.contains("#திருக்குறள்"))
        }
    }

    // 6. Export Printable HTML & OpenXML DOCX ZIP Package Tests
    @Test
    fun `exportService generates printable HTML and valid OpenXML DOCX ZIP archive`() {
        val kurals = KuralRepository.getKuralsByChapter(1)
        val html = ExportService.generatePrintableHtml(
            title = "அதிகாரம் 1 - கடவுள் வாழ்த்து",
            subtitle = "அறத்துப்பால்",
            kurals = kurals
        )
        assertTrue(html.contains("திருக்குறள்கள்"))
        assertTrue(html.contains("குறள் #1"))
        assertTrue(html.contains(AppConstants.COPYRIGHT_TEXT))

        val docxBytes = ExportService.generateDocxBytes(
            title = "அதிகாரம் 1 - கடவுள் வாழ்த்து",
            subtitle = "அறத்துப்பால்",
            kurals = kurals
        )
        assertTrue(docxBytes.isNotEmpty())

        val entries = mutableSetOf<String>()
        var documentXmlContent = ""
        ZipInputStream(ByteArrayInputStream(docxBytes)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                entries.add(entry.name)
                if (entry.name == "word/document.xml") {
                    documentXmlContent = zis.readBytes().toString(Charsets.UTF_8)
                }
                entry = zis.nextEntry
            }
        }
        assertTrue(entries.contains("[Content_Types].xml"))
        assertTrue(entries.contains("word/document.xml"))
        assertTrue(entries.contains("word/header1.xml"))
        assertTrue(entries.contains("word/footer1.xml"))
        assertTrue(documentXmlContent.contains("குறள் #1"))
    }

    // 7. Olineri Tamil Pronunciation Learning Mode SDK Tests
    @Test
    fun `olineriService downloads only Tamil artifacts and performs Tamil G2P, alignment, and acoustic scoring`() {
        // Verify strictly Tamil-only artifacts
        assertEquals(6, OlineriService.REQUIRED_TAMIL_ARTIFACTS.size)
        for (artifact in OlineriService.REQUIRED_TAMIL_ARTIFACTS) {
            assertTrue(
                artifact.path.startsWith("packages/lang-ta/") || artifact.path.startsWith("packages/core/")
            )
            assertFalse(artifact.path.contains("lang-hi"))
            assertFalse(artifact.path.contains("lang-te"))
            assertFalse(artifact.path.contains("lang-kn"))
        }

        // Verify Tamil Brahmic G2P conversion on "அகர முதல எழுத்தெல்லாம்" and "வாழ்க"
        val vazhgaTargets = OlineriService.convertTamilG2P("வாழ்க")
        assertTrue(vazhgaTargets.any { it.expectedIpa == "ɻ" && it.displayGrapheme == "ழ்" })
        for (target in vazhgaTargets) {
            assertTrue(target.charEndUtf8 > target.charStartUtf8)
        }

        // Verify special Tamil phonetics extraction
        val kural1 = KuralRepository.getKuralByNumber(1)!!
        val specials = OlineriService.extractSpecialTamilPhonetics("${kural1.line1} ${kural1.line2}")
        assertTrue(specials.any { it.grapheme == "ழ" && it.ipa == "/ɻ/" })

        // Verify PronunciationSDK assessment (exact match -> 100% pass, 'ழ' -> 'ல' substitution -> articulatory tip)
        val sdk = OlineriService.PronunciationSDK(null, AppLanguage.TA)
        val voicedPcm = FloatArray(16000) { idx -> (sin(idx * 0.15) * 0.25).toFloat() }

        val exactResult = sdk.assessPronunciation("அகர முதல எழுத்தெல்லாம்", voicedPcm, "அகர முதல எழுத்தெல்லாம்")
        assertTrue(exactResult.overallScore >= 90)
        assertTrue(exactResult.passed)
        assertTrue(exactResult.phonemes.all { it.status == PhonemeStatus.CORRECT })

        val englishSpokenResult = sdk.assessPronunciation("அகர முதல எழுத்தெல்லாம்", voicedPcm, "hello world")
        assertEquals(0, englishSpokenResult.overallScore)
        assertFalse(englishSpokenResult.passed)

        val subResult = sdk.assessPronunciation("எழுத்தெல்லாம்", voicedPcm, "எலுத்தெல்லாம்")
        val zhhaFeedback = subResult.phonemes.find { it.expectedIpa == "ɻ" }
        assertNotNull(zhhaFeedback)
        assertEquals(PhonemeStatus.SUBSTITUTION, zhhaFeedback!!.status)
        assertEquals("l", zhhaFeedback.actualIpa)
        assertTrue(zhhaFeedback.messageTamil.contains("ழ"))

        // Verify silent audio returns 0 score
        val silentResult = sdk.assessPronunciation("அகர முதல", FloatArray(8000) { 0f }, "")
        assertEquals(0, silentResult.overallScore)
        assertFalse(silentResult.passed)
    }

    // 8. Bilingual Translations & Notification Time Formatting Tests
    @Test
    fun `translations and notification time formatting support Tamil and English`() {
        assertEquals("முகப்பு", Translations.forLanguage(AppLanguage.TA).tabHome)
        assertEquals("Home", Translations.forLanguage(AppLanguage.EN).tabHome)

        assertEquals("8:00 AM", NotificationService.formatNotificationTime(8, 0, isTamil = false))
        assertEquals("காலை 8:00", NotificationService.formatNotificationTime(8, 0, isTamil = true))
        assertEquals("6:00 PM", NotificationService.formatNotificationTime(18, 0, isTamil = false))
        assertEquals("மாலை 6:00", NotificationService.formatNotificationTime(18, 0, isTamil = true))
        assertEquals("9:00 PM", NotificationService.formatNotificationTime(21, 0, isTamil = false))
        assertEquals("இரவு 9:00", NotificationService.formatNotificationTime(21, 0, isTamil = true))
    }
}

