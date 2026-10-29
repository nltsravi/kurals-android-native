package com.nltsravi.kurals.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nltsravi.kurals.constants.AppConstants
import com.nltsravi.kurals.data.KuralRepository
import com.nltsravi.kurals.data.PreferencesAndCollectionStorage
import com.nltsravi.kurals.model.AppLanguage
import com.nltsravi.kurals.model.CardImageTheme
import com.nltsravi.kurals.model.Kural
import com.nltsravi.kurals.model.KuralCollection
import com.nltsravi.kurals.model.NotificationSettings
import com.nltsravi.kurals.model.OlineriCachedState
import com.nltsravi.kurals.model.OlineriDownloadProgress
import com.nltsravi.kurals.model.ThemeMode
import com.nltsravi.kurals.service.NotificationService
import com.nltsravi.kurals.service.OlineriService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

class AppViewModel(application: Application) : AndroidViewModel(application) {

    val storage = PreferencesAndCollectionStorage(application)

    // Theme & Card Image state
    private val _themeMode = MutableStateFlow(storage.loadThemeMode())
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _allowModifyCardImage = MutableStateFlow(storage.loadAllowModifyCardImage())
    val allowModifyCardImage: StateFlow<Boolean> = _allowModifyCardImage.asStateFlow()

    private val _selectedCardImageId = MutableStateFlow(storage.loadSelectedCardImageId())
    val selectedCardImageId: StateFlow<String> = _selectedCardImageId.asStateFlow()

    private val _cardImageTheme = MutableStateFlow(storage.loadCardImageTheme())
    val cardImageTheme: StateFlow<CardImageTheme> = _cardImageTheme.asStateFlow()

    private val _customImageUri = MutableStateFlow(storage.loadCustomCardImageUri())
    val customImageUri: StateFlow<String?> = _customImageUri.asStateFlow()

    // Language state
    private val _language = MutableStateFlow(storage.loadLanguage())
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    // Collections state
    private val _collections = MutableStateFlow(storage.loadCollections())
    val collections: StateFlow<List<KuralCollection>> = _collections.asStateFlow()

    // Random Kural of the Day
    private val _randomKural = MutableStateFlow<Kural?>(null)
    val randomKural: StateFlow<Kural?> = _randomKural.asStateFlow()

    // Notification settings
    private val _notificationSettings = MutableStateFlow(storage.loadNotificationSettings())
    val notificationSettings: StateFlow<NotificationSettings> = _notificationSettings.asStateFlow()

    // Learning Mode (Olineri Tamil Pronunciation SDK)
    private val _isLearningModeEnabled = MutableStateFlow(storage.loadLearningModeEnabled())
    val isLearningModeEnabled: StateFlow<Boolean> = _isLearningModeEnabled.asStateFlow()

    private val _cachedOlineriState = MutableStateFlow(storage.loadOlineriCachedState())
    val cachedOlineriState: StateFlow<OlineriCachedState?> = _cachedOlineriState.asStateFlow()

    private val _showLaunchPrompt = MutableStateFlow(false)
    val showLaunchPrompt: StateFlow<Boolean> = _showLaunchPrompt.asStateFlow()

    private val _isDownloadingArtifacts = MutableStateFlow(false)
    val isDownloadingArtifacts: StateFlow<Boolean> = _isDownloadingArtifacts.asStateFlow()

    private val _downloadProgress = MutableStateFlow<OlineriDownloadProgress?>(null)
    val downloadProgress: StateFlow<OlineriDownloadProgress?> = _downloadProgress.asStateFlow()

    private val _downloadError = MutableStateFlow<String?>(null)
    val downloadError: StateFlow<String?> = _downloadError.asStateFlow()

    init {
        KuralRepository.initialize(application)
        _randomKural.value = KuralRepository.getRandomKural()

        val enabled = storage.loadLearningModeEnabled()
        val promptShown = storage.loadLearningPromptShown()
        val cached = storage.loadOlineriCachedState()
        if (enabled && cached != null) {
            _isLearningModeEnabled.value = true
            _cachedOlineriState.value = cached
        } else if (!promptShown) {
            _showLaunchPrompt.value = true
        }

        // Schedule daily notifications if enabled
        if (_notificationSettings.value.enabled) {
            NotificationService.scheduleDailyNotifications(application, _notificationSettings.value)
        }
    }

    fun shuffleRandomKural() {
        _randomKural.value = KuralRepository.getRandomKural()
    }

    // Theme & Card Image setters
    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        storage.saveThemeMode(mode)
    }

    fun setAllowModifyCardImage(allow: Boolean) {
        _allowModifyCardImage.value = allow
        storage.saveAllowModifyCardImage(allow)
    }

    fun setSelectedCardImageId(id: String) {
        _selectedCardImageId.value = id
        storage.saveSelectedCardImageId(id)
    }

    fun setCardImageTheme(theme: CardImageTheme) {
        _cardImageTheme.value = theme
        storage.saveCardImageTheme(theme)
    }

    fun setCustomImageUri(uri: String?) {
        _customImageUri.value = uri
        storage.saveCustomCardImageUri(uri)
        if (uri != null) {
            setSelectedCardImageId("custom")
            setCardImageTheme(CardImageTheme.CUSTOM)
        } else if (_selectedCardImageId.value == "custom") {
            setSelectedCardImageId("thiruvalluvar-color")
            setCardImageTheme(CardImageTheme.PORTRAIT)
        }
    }

    // Language setter
    fun setLanguage(lang: AppLanguage) {
        _language.value = lang
        storage.saveLanguage(lang)
    }

    // Collections operations
    private fun persistCollections(next: List<KuralCollection>) {
        _collections.value = next
        storage.saveCollections(next)
    }

    fun createCollection(name: String, description: String = "", color: String = AppConstants.COLLECTION_COLORS[0]): KuralCollection {
        val now = PreferencesAndCollectionStorage.nowIsoString()
        val newCol = KuralCollection(
            id = "col-${System.currentTimeMillis()}-${Random.nextInt(1000, 9999)}",
            name = name.trim(),
            description = description.trim(),
            kuralNumbers = emptyList(),
            createdAt = now,
            updatedAt = now,
            color = color
        )
        persistCollections(_collections.value + newCol)
        return newCol
    }

    fun deleteCollection(id: String) {
        persistCollections(_collections.value.filter { it.id != id })
    }

    fun renameCollection(id: String, name: String, description: String? = null) {
        val now = PreferencesAndCollectionStorage.nowIsoString()
        val next = _collections.value.map { c ->
            if (c.id == id) {
                c.copy(
                    name = name.trim(),
                    description = description?.trim() ?: c.description,
                    updatedAt = now
                )
            } else {
                c
            }
        }
        persistCollections(next)
    }

    fun addKuralToCollection(collectionId: String, kuralNumber: Int) {
        val next = PreferencesAndCollectionStorage.addKuralToCollectionPure(
            _collections.value,
            collectionId,
            kuralNumber
        )
        persistCollections(next)
    }

    fun removeKuralFromCollection(collectionId: String, kuralNumber: Int) {
        val next = PreferencesAndCollectionStorage.removeKuralFromCollectionPure(
            _collections.value,
            collectionId,
            kuralNumber
        )
        persistCollections(next)
    }

    fun toggleKuralInCollection(collectionId: String, kuralNumber: Int) {
        val col = _collections.value.find { it.id == collectionId } ?: return
        if (col.kuralNumbers.contains(kuralNumber)) {
            removeKuralFromCollection(collectionId, kuralNumber)
        } else {
            addKuralToCollection(collectionId, kuralNumber)
        }
    }

    fun isKuralSaved(kuralNumber: Int): Boolean {
        return _collections.value.any { it.kuralNumbers.contains(kuralNumber) }
    }

    fun moveKural(collectionId: String, fromIndex: Int, toIndex: Int) {
        val next = PreferencesAndCollectionStorage.reorderKuralInCollectionPure(
            _collections.value,
            collectionId,
            fromIndex,
            toIndex
        )
        persistCollections(next)
    }

    fun getCollectionById(id: String): KuralCollection? {
        return _collections.value.find { it.id == id }
    }

    // Notifications
    fun updateNotificationSettings(settings: NotificationSettings) {
        _notificationSettings.value = settings
        storage.saveNotificationSettings(settings)
        NotificationService.scheduleDailyNotifications(getApplication(), settings)
    }

    // Learning Mode (Enable-only policy matching LearningModeContext.tsx)
    fun enableLearningMode(onComplete: ((Boolean) -> Unit)? = null) {
        if (_isDownloadingArtifacts.value) return
        _isDownloadingArtifacts.value = true
        _downloadError.value = null

        viewModelScope.launch {
            try {
                val state = OlineriService.downloadOlineriTamilArtifacts(storage) { progress ->
                    _downloadProgress.value = progress
                }
                _cachedOlineriState.value = state
                _isLearningModeEnabled.value = true
                _showLaunchPrompt.value = false
                storage.saveLearningModeEnabled(true)
                storage.saveLearningPromptShown(true)
                _isDownloadingArtifacts.value = false
                onComplete?.invoke(true)
            } catch (e: Exception) {
                val msg = e.message ?: "Unable to download Tamil pronunciation artifacts. Please check your internet connection."
                _downloadError.value = msg
                _isDownloadingArtifacts.value = false
                onComplete?.invoke(false)
            }
        }
    }

    fun dismissLaunchPrompt() {
        _showLaunchPrompt.value = false
        _downloadError.value = null
        storage.saveLearningPromptShown(true)
    }

    fun openLaunchPrompt() {
        _downloadError.value = null
        _showLaunchPrompt.value = true
    }

    fun createPronunciationSDK(): OlineriService.PronunciationSDK {
        return OlineriService.PronunciationSDK(_cachedOlineriState.value, _language.value)
    }
}

