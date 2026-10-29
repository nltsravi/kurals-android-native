package com.nltsravi.kurals.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nltsravi.kurals.constants.AppConstants
import com.nltsravi.kurals.model.AppLanguage
import com.nltsravi.kurals.model.CardImageTheme
import com.nltsravi.kurals.model.KuralCollection
import com.nltsravi.kurals.model.NotificationSettings
import com.nltsravi.kurals.model.OlineriCachedState
import com.nltsravi.kurals.model.ThemeMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class PreferencesAndCollectionStorage(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("thirukkural_app_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        fun nowIsoString(): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            return sdf.format(Date())
        }

        fun defaultCollections(): List<KuralCollection> {
            val now = nowIsoString()
            return listOf(
                KuralCollection(
                    id = "favorites",
                    name = "❤️ My Favorite Kurals",
                    description = "Personal favorite couplets for quick reading",
                    kuralNumbers = listOf(1, 2, 3),
                    createdAt = now,
                    updatedAt = now,
                    color = "#FF7C0A"
                ),
                KuralCollection(
                    id = "morning-reading",
                    name = "🌅 Morning Reading",
                    description = "Inspiring Kurals to start the day",
                    kuralNumbers = listOf(31, 32, 33),
                    createdAt = now,
                    updatedAt = now,
                    color = "#D97706"
                )
            )
        }

        fun addKuralToCollectionPure(
            collections: List<KuralCollection>,
            collectionId: String,
            kuralNumber: Int
        ): List<KuralCollection> {
            return collections.map { col ->
                if (col.id != collectionId) return@map col
                if (col.kuralNumbers.contains(kuralNumber)) return@map col
                col.copy(
                    kuralNumbers = col.kuralNumbers + kuralNumber,
                    updatedAt = nowIsoString()
                )
            }
        }

        fun removeKuralFromCollectionPure(
            collections: List<KuralCollection>,
            collectionId: String,
            kuralNumber: Int
        ): List<KuralCollection> {
            return collections.map { col ->
                if (col.id != collectionId) return@map col
                col.copy(
                    kuralNumbers = col.kuralNumbers.filter { it != kuralNumber },
                    updatedAt = nowIsoString()
                )
            }
        }

        fun reorderKuralInCollectionPure(
            collections: List<KuralCollection>,
            collectionId: String,
            fromIndex: Int,
            toIndex: Int
        ): List<KuralCollection> {
            return collections.map { col ->
                if (col.id != collectionId) return@map col
                val list = col.kuralNumbers.toMutableList()
                if (fromIndex < 0 || fromIndex >= list.size || toIndex < 0 || toIndex >= list.size) {
                    return@map col
                }
                val item = list.removeAt(fromIndex)
                list.add(toIndex, item)
                col.copy(
                    kuralNumbers = list,
                    updatedAt = nowIsoString()
                )
            }
        }
    }

    // Collections
    fun loadCollections(): List<KuralCollection> {
        val raw = prefs.getString(AppConstants.STORAGE_KEY_COLLECTIONS, null)
        if (raw.isNullOrBlank()) {
            val defaults = defaultCollections()
            saveCollections(defaults)
            return defaults
        }
        return try {
            val type = object : TypeToken<List<KuralCollection>>() {}.type
            val parsed: List<KuralCollection>? = gson.fromJson(raw, type)
            parsed ?: defaultCollections()
        } catch (_: Exception) {
            defaultCollections()
        }
    }

    fun saveCollections(collections: List<KuralCollection>) {
        prefs.edit().putString(AppConstants.STORAGE_KEY_COLLECTIONS, gson.toJson(collections)).apply()
    }

    // Theme Mode
    fun loadThemeMode(): ThemeMode {
        val key = prefs.getString(AppConstants.STORAGE_KEY_THEME, ThemeMode.SYSTEM.key)
        return ThemeMode.fromKey(key)
    }

    fun saveThemeMode(mode: ThemeMode) {
        prefs.edit().putString(AppConstants.STORAGE_KEY_THEME, mode.key).apply()
    }

    // Language
    fun loadLanguage(): AppLanguage {
        val key = prefs.getString(AppConstants.STORAGE_KEY_LANGUAGE, AppLanguage.TA.key)
        return AppLanguage.fromKey(key)
    }

    fun saveLanguage(lang: AppLanguage) {
        prefs.edit().putString(AppConstants.STORAGE_KEY_LANGUAGE, lang.key).apply()
    }

    // Card Image Customization
    fun loadAllowModifyCardImage(): Boolean {
        return prefs.getBoolean(AppConstants.STORAGE_KEY_ALLOW_MODIFY_CARD_IMAGE, false)
    }

    fun saveAllowModifyCardImage(allow: Boolean) {
        prefs.edit().putBoolean(AppConstants.STORAGE_KEY_ALLOW_MODIFY_CARD_IMAGE, allow).apply()
    }

    fun loadSelectedCardImageId(): String {
        val saved = prefs.getString(AppConstants.STORAGE_KEY_CARD_IMAGE_ID, null)
        if (saved != null && (saved == "custom" || AppConstants.THIRUVALLUVAR_IMAGES.any { it.id == saved })) {
            return saved
        }
        return "thiruvalluvar-color"
    }

    fun saveSelectedCardImageId(id: String) {
        prefs.edit().putString(AppConstants.STORAGE_KEY_CARD_IMAGE_ID, id).apply()
    }

    fun loadCardImageTheme(): CardImageTheme {
        val raw = prefs.getString(AppConstants.STORAGE_KEY_CARD_IMAGE_THEME, null)
        return try {
            if (raw != null) CardImageTheme.valueOf(raw) else CardImageTheme.PORTRAIT
        } catch (_: Exception) {
            CardImageTheme.PORTRAIT
        }
    }

    fun saveCardImageTheme(theme: CardImageTheme) {
        prefs.edit().putString(AppConstants.STORAGE_KEY_CARD_IMAGE_THEME, theme.name).apply()
    }

    fun loadCustomCardImageUri(): String? {
        return prefs.getString(AppConstants.STORAGE_KEY_CARD_CUSTOM_IMAGE_URI, null)
    }

    fun saveCustomCardImageUri(uri: String?) {
        if (uri == null) {
            prefs.edit().remove(AppConstants.STORAGE_KEY_CARD_CUSTOM_IMAGE_URI).apply()
        } else {
            prefs.edit().putString(AppConstants.STORAGE_KEY_CARD_CUSTOM_IMAGE_URI, uri).apply()
        }
    }

    // Notification Settings
    fun loadNotificationSettings(): NotificationSettings {
        val raw = prefs.getString(AppConstants.STORAGE_KEY_NOTIFICATIONS, null)
        if (raw.isNullOrBlank()) return NotificationSettings()
        return try {
            gson.fromJson(raw, NotificationSettings::class.java) ?: NotificationSettings()
        } catch (_: Exception) {
            NotificationSettings()
        }
    }

    fun saveNotificationSettings(settings: NotificationSettings) {
        prefs.edit().putString(AppConstants.STORAGE_KEY_NOTIFICATIONS, gson.toJson(settings)).apply()
    }

    // Learning Mode & Olineri Tamil SDK Artifacts
    fun loadLearningModeEnabled(): Boolean {
        return prefs.getBoolean(AppConstants.STORAGE_KEY_LEARNING_MODE_ENABLED, false)
    }

    fun saveLearningModeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(AppConstants.STORAGE_KEY_LEARNING_MODE_ENABLED, enabled).apply()
    }

    fun loadLearningPromptShown(): Boolean {
        return prefs.getBoolean(AppConstants.STORAGE_KEY_LEARNING_PROMPT_SHOWN, false)
    }

    fun saveLearningPromptShown(shown: Boolean) {
        prefs.edit().putBoolean(AppConstants.STORAGE_KEY_LEARNING_PROMPT_SHOWN, shown).apply()
    }

    fun loadOlineriCachedState(): OlineriCachedState? {
        val raw = prefs.getString(AppConstants.STORAGE_KEY_OLINERI_TAMIL_ARTIFACTS, null) ?: return null
        return try {
            val state = gson.fromJson(raw, OlineriCachedState::class.java)
            if (state != null && state.languageCode == "ta-IN" && state.artifacts.isNotEmpty()) {
                state
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    fun saveOlineriCachedState(state: OlineriCachedState) {
        prefs.edit().putString(AppConstants.STORAGE_KEY_OLINERI_TAMIL_ARTIFACTS, gson.toJson(state)).apply()
    }
}

