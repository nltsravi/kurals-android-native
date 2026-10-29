package com.nltsravi.kurals.constants

import com.nltsravi.kurals.model.CardAspectRatio
import com.nltsravi.kurals.model.CardImageTheme
import com.nltsravi.kurals.model.MeaningType
import com.nltsravi.kurals.model.ThiruvalluvarImageOption

object AppConstants {
    const val APP_NAME = "திருக்குறள் - Thirukkural"
    const val APP_SHORT_NAME = "திருக்குறள்"
    const val APP_VERSION = "1.1.0"
    const val PACKAGE_ID = "com.nltsravi.kurals"
    const val PLAY_STORE_URL = "https://play.google.com/store/apps/details?id=$PACKAGE_ID"
    const val APP_STORE_URL = "https://apps.apple.com/app/thirukkural-tamil/id0000000000"
    const val COPYRIGHT_HOLDER = "Neolumina Technology Solutions"
    const val COPYRIGHT_TEXT = "© $COPYRIGHT_HOLDER"
    const val DEVELOPER_EMAIL = "support@neoluminatechnologies.com"
    const val PRIVACY_POLICY_LAST_UPDATED = "April 2026"

    const val TOTAL_KURALS = 1330
    const val TOTAL_CHAPTERS = 133
    const val TOTAL_CATEGORIES = 3
    const val KURALS_PER_CHAPTER = 10

    const val STORAGE_KEY_COLLECTIONS = "@thirukkural:collections:v1"
    const val STORAGE_KEY_THEME = "@thirukkural:theme:v1"
    const val STORAGE_KEY_LANGUAGE = "@thirukkural:language:v1"
    const val STORAGE_KEY_CARD_IMAGE_THEME = "@thirukkural:card_image_theme:v1"
    const val STORAGE_KEY_CARD_CUSTOM_IMAGE_URI = "@thirukkural:card_custom_image_uri:v1"
    const val STORAGE_KEY_CARD_IMAGE_ID = "@thirukkural:card_image_id:v1"
    const val STORAGE_KEY_ALLOW_MODIFY_CARD_IMAGE = "@thirukkural:allow_modify_card_image:v1"
    const val STORAGE_KEY_NOTIFICATIONS = "@thirukkural:notifications:v1"
    const val STORAGE_KEY_LEARNING_MODE_ENABLED = "@thirukkural:learning_mode_enabled:v1"
    const val STORAGE_KEY_LEARNING_PROMPT_SHOWN = "@thirukkural:learning_prompt_shown:v1"
    const val STORAGE_KEY_OLINERI_TAMIL_ARTIFACTS = "@thirukkural:olineri_tamil_artifacts:v1"

    const val OLINERI_REPO_BASE_URL = "https://raw.githubusercontent.com/nltsravi/olineri-core/main"
    const val OLINERI_REPO_WEB_URL = "https://github.com/nltsravi/olineri-core"
    const val OLINERI_TAMIL_PACKAGE_PATH = "packages/lang-ta/src"
    const val OLINERI_CORE_PACKAGE_PATH = "packages/core/src"

    val SEARCH_SUGGESTIONS = listOf(
        "1",
        "வான்சிறப்பு",
        "அறத்துப்பால்",
        "அகர முதல",
        "காமத்துப்பால்",
        "நட்பு",
        "அன்புடைமை",
        "agaram",
        "1330"
    )

    val COLLECTION_COLORS = listOf(
        "#FF7C0A",
        "#D97706",
        "#E11D48",
        "#10B981",
        "#7C3AED",
        "#2563EB"
    )

    data class MeaningOption(
        val key: MeaningType,
        val label: String,
        val tamilLabel: String
    )

    val MEANING_OPTIONS = listOf(
        MeaningOption(MeaningType.MV, "Mu. Varadarajan", "மு.வரதராசனார் உரை"),
        MeaningOption(MeaningType.SP, "Solomon Pappaiah", "சாலமன் பாப்பையா உரை"),
        MeaningOption(MeaningType.MK, "M. Karunanidhi", "மு.கருணாநிதி உரை"),
        MeaningOption(MeaningType.PRM, "Parimelazhagar", "பரிமேலழகர் உரை"),
        MeaningOption(MeaningType.MNK, "Manakkudavar", "மணக்குடவர் உரை"),
        MeaningOption(MeaningType.TRANSLATION, "English Meaning", "ஆங்கிலப் பொருள்"),
        MeaningOption(MeaningType.BOTH, "Mu.Va + English", "மு.வ + ஆங்கிலம்")
    )

    data class AspectRatioOption(
        val key: CardAspectRatio,
        val label: String,
        val subtitle: String,
        val width: Int,
        val height: Int
    )

    val ASPECT_RATIO_OPTIONS = listOf(
        AspectRatioOption(CardAspectRatio.RATIO_9_16, "9:16", "Status", 1080, 1920),
        AspectRatioOption(CardAspectRatio.RATIO_4_5, "4:5", "Feed", 1080, 1350),
        AspectRatioOption(CardAspectRatio.RATIO_1_1, "1:1", "Square", 1080, 1080)
    )

    data class ImageThemeOption(
        val key: CardImageTheme,
        val label: String,
        val tamilLabel: String,
        val icon: String
    )

    val CARD_IMAGE_THEME_OPTIONS = listOf(
        ImageThemeOption(CardImageTheme.PORTRAIT, "Thiruvalluvar", "திருவள்ளுவர்", "🧘"),
        ImageThemeOption(CardImageTheme.PALM_LEAF, "Palm Leaf", "ஓலைச்சுவடி", "📜"),
        ImageThemeOption(CardImageTheme.TEMPLE_LAMP, "Deepam Lamp", "தீபம்", "🪔"),
        ImageThemeOption(CardImageTheme.MINIMAL_SEAL, "Classic Seal", "முத்திரை", "🏵️"),
        ImageThemeOption(CardImageTheme.NONE, "No Image", "படம் இல்லை", "🚫"),
        ImageThemeOption(CardImageTheme.CUSTOM, "Custom Photo", "சொந்தப் படம்", "🖼️")
    )

    val THIRUVALLUVAR_IMAGES = listOf(
        ThiruvalluvarImageOption(
            id = "thiruvalluvar-color",
            labelTa = "திருவள்ளுவர் (வண்ணம்)",
            labelEn = "Thiruvalluvar (Color)",
            assetPath = "images/thiruvalluvar/thiruvalluvar-color.png"
        ),
        ThiruvalluvarImageOption(
            id = "thiruvalluvar-bw",
            labelTa = "திருவள்ளுவர் (கருப்பு வெள்ளை)",
            labelEn = "Thiruvalluvar (B&W)",
            assetPath = "images/thiruvalluvar/thiruvalluvar-bw.jpg"
        ),
        ThiruvalluvarImageOption(
            id = "beach-statue",
            labelTa = "கன்னியாகுமரி சிலை",
            labelEn = "Kanyakumari Statue",
            assetPath = "images/thiruvalluvar/beach-statue.png"
        ),
        ThiruvalluvarImageOption(
            id = "statue",
            labelTa = "வள்ளுவர் சிலை",
            labelEn = "Valluvar Statue",
            assetPath = "images/thiruvalluvar/statue.png"
        ),
        ThiruvalluvarImageOption(
            id = "hide",
            labelTa = "செயலி சின்னம்",
            labelEn = "App Icon",
            assetPath = "images/icon.png"
        )
    )

    fun getThiruvalluvarImageById(id: String): ThiruvalluvarImageOption {
        return THIRUVALLUVAR_IMAGES.find { it.id == id } ?: THIRUVALLUVAR_IMAGES.first()
    }
}

