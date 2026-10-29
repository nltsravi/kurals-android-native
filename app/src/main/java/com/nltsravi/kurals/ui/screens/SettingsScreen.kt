package com.nltsravi.kurals.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.nltsravi.kurals.constants.AppConstants
import com.nltsravi.kurals.model.AppLanguage
import com.nltsravi.kurals.model.CardImageTheme
import com.nltsravi.kurals.model.NotificationSettings
import com.nltsravi.kurals.model.OlineriCachedState
import com.nltsravi.kurals.model.OlineriDownloadProgress
import com.nltsravi.kurals.model.ThemeMode
import com.nltsravi.kurals.service.NotificationService
import com.nltsravi.kurals.service.ShareService
import com.nltsravi.kurals.ui.strings.LocalAppTranslations
import com.nltsravi.kurals.ui.theme.LocalAppPalette
import com.nltsravi.kurals.util.rememberAssetImageBitmap
import com.nltsravi.kurals.util.rememberUriImageBitmap

private data class TimePreset(
    val labelTa: String,
    val labelEn: String,
    val hour: Int,
    val minute: Int
)

private val TIME_PRESETS = listOf(
    TimePreset("காலை 6:00", "6:00 AM", 6, 0),
    TimePreset("காலை 7:00", "7:00 AM", 7, 0),
    TimePreset("காலை 8:00", "8:00 AM", 8, 0),
    TimePreset("காலை 9:00", "9:00 AM", 9, 0),
    TimePreset("மாலை 6:00", "6:00 PM", 18, 0),
    TimePreset("இரவு 8:00", "8:00 PM", 20, 0),
    TimePreset("இரவு 9:00", "9:00 PM", 21, 0)
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    language: AppLanguage,
    onSelectLanguage: (AppLanguage) -> Unit,
    themeMode: ThemeMode,
    onSelectThemeMode: (ThemeMode) -> Unit,
    allowModifyCardImage: Boolean,
    onToggleAllowModifyCardImage: (Boolean) -> Unit,
    selectedCardImageId: String,
    onSelectCardImageId: (String) -> Unit,
    onSelectCardImageTheme: (CardImageTheme) -> Unit,
    customImageUri: String?,
    onSelectCustomImageUri: (String?) -> Unit,
    notificationSettings: NotificationSettings,
    onUpdateNotificationSettings: (NotificationSettings) -> Unit,
    isLearningModeEnabled: Boolean,
    isDownloadingArtifacts: Boolean,
    downloadProgress: OlineriDownloadProgress?,
    downloadError: String?,
    cachedOlineriState: OlineriCachedState?,
    onEnableLearningMode: () -> Unit,
    onOpenKuralPractice: (Int) -> Unit
) {
    val context = LocalContext.current
    val palette = LocalAppPalette.current
    val t = LocalAppTranslations.current

    var showTimePickerModal by remember { mutableStateOf(false) }
    var showPrivacyModal by remember { mutableStateOf(false) }
    var pickerHour by remember(notificationSettings.hour) { mutableIntStateOf(notificationSettings.hour) }
    var pickerMinute by remember(notificationSettings.minute) { mutableIntStateOf(notificationSettings.minute) }

    val buildVersionString = "v${AppConstants.APP_VERSION} (2)"

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}
            onSelectCustomImageUri(uri.toString())
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onUpdateNotificationSettings(notificationSettings.copy(enabled = true))
        } else {
            Toast.makeText(
                context,
                t.settingsNotificationPermissionDeniedMsg,
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun handleToggleNotifications(enabled: Boolean) {
        if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPerm = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPerm) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }
        onUpdateNotificationSettings(notificationSettings.copy(enabled = enabled))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Header
        Text(
            text = t.settingsHeaderTitle,
            color = palette.text,
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = t.settingsHeaderSubtitle,
            color = palette.textSecondary,
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(18.dp))

        // 1. Language Selector
        Text(
            text = t.settingsLanguageSectionTitle,
            color = palette.text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = t.settingsLanguageSectionDesc,
            color = palette.textSecondary,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        listOf(
            Triple(AppLanguage.TA, "அ", t.settingsLangTamilTitle to t.settingsLangTamilSub),
            Triple(AppLanguage.EN, "A", t.settingsLangEnglishTitle to t.settingsLangEnglishSub)
        ).forEach { (langKey, badgeChar, titleSub) ->
            val isSelected = language == langKey
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onSelectLanguage(langKey) },
                shape = RoundedCornerShape(14.dp),
                color = palette.card,
                border = BorderStroke(
                    if (isSelected) 1.5.dp else 1.dp,
                    if (isSelected) palette.primary else palette.border
                )
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) palette.primarySoft else palette.cardElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = badgeChar,
                            color = if (isSelected) palette.primary else palette.textSecondary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = titleSub.first,
                            color = palette.text,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = titleSub.second,
                            color = palette.textSecondary,
                            fontSize = 12.sp
                        )
                    }
                    Icon(
                        imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (isSelected) palette.primary else palette.textMuted,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2. Kural Learning Mode (Olineri Tamil Pronunciation SDK)
        Text(
            text = t.learningSettingsSectionTitle,
            color = palette.text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = t.learningSettingsSectionDesc,
            color = palette.textSecondary,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        if (isLearningModeEnabled) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = palette.card,
                border = BorderStroke(1.5.dp, palette.success)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .background(palette.success.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = null,
                                tint = palette.success,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = t.learningSettingsActiveTitle,
                                    color = palette.text,
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = palette.success,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = t.learningSettingsActiveSub,
                                color = palette.textSecondary,
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = palette.divider)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Olineri Tamil SDK (ta-IN v${cachedOlineriState?.version ?: "0.1.0"}) • ${cachedOlineriState?.phonemeCount ?: 34} Phonemes",
                            color = palette.textMuted,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(palette.primarySoft)
                                .clickable { onOpenKuralPractice(1) }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = null,
                                tint = palette.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (language == AppLanguage.TA) "இப்போதே பயிற்சி செய்" else "Practice Now",
                                color = palette.primary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        } else {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = palette.card,
                border = BorderStroke(1.dp, palette.border)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .background(palette.primarySoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = palette.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = t.learningPromptModalSubtitle,
                                color = palette.text,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = t.learningPromptModalDescription,
                                color = palette.textSecondary,
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            )
                        }
                    }

                    if (isDownloadingArtifacts) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = palette.cardElevated,
                            border = BorderStroke(1.dp, palette.primary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(
                                        color = palette.primary,
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Text(
                                        text = downloadProgress?.currentArtifact ?: t.learningSettingsDownloadingLabel,
                                        color = palette.text,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { (downloadProgress?.percentage ?: 15) / 100f },
                                    color = palette.primary,
                                    trackColor = palette.border,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                )
                            }
                        }
                    }

                    if (!downloadError.isNullOrBlank() && !isDownloadingArtifacts) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = palette.danger.copy(alpha = 0.1f),
                            border = BorderStroke(1.dp, palette.danger),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = palette.danger,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = downloadError,
                                    color = palette.danger,
                                    fontSize = 11.5.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(enabled = !isDownloadingArtifacts) { onEnableLearningMode() },
                        shape = RoundedCornerShape(12.dp),
                        color = palette.primary
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isDownloadingArtifacts) {
                                    t.learningSettingsDownloadingLabel
                                } else {
                                    t.learningSettingsEnableBtn
                                },
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        // 3. Appearance (Theme Selector)
        Text(
            text = t.settingsAppearanceTitle,
            color = palette.text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = t.settingsAppearanceDesc,
            color = palette.textSecondary,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        data class ThemeOpt(
            val mode: ThemeMode,
            val title: String,
            val sub: String,
            val icon: ImageVector
        )

        listOf(
            ThemeOpt(ThemeMode.SYSTEM, t.settingsThemeSystemTitle, t.settingsThemeSystemSub, Icons.Default.PhoneAndroid),
            ThemeOpt(ThemeMode.LIGHT, t.settingsThemeLightTitle, t.settingsThemeLightSub, Icons.Default.LightMode),
            ThemeOpt(ThemeMode.DARK, t.settingsThemeDarkTitle, t.settingsThemeDarkSub, Icons.Default.DarkMode)
        ).forEach { opt ->
            val isSelected = themeMode == opt.mode
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onSelectThemeMode(opt.mode) },
                shape = RoundedCornerShape(14.dp),
                color = palette.card,
                border = BorderStroke(
                    if (isSelected) 1.5.dp else 1.dp,
                    if (isSelected) palette.primary else palette.border
                )
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) palette.primarySoft else palette.cardElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = opt.icon,
                            contentDescription = null,
                            tint = if (isSelected) palette.primary else palette.textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = opt.title,
                            color = palette.text,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = opt.sub,
                            color = palette.textSecondary,
                            fontSize = 12.sp
                        )
                    }
                    Icon(
                        imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (isSelected) palette.primary else palette.textMuted,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 4. Social Share & Status Card Image Customization
        Text(
            text = t.settingsCardImageSectionTitle,
            color = palette.text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = t.settingsCardImageSectionDesc,
            color = palette.textSecondary,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = palette.card,
            border = BorderStroke(1.dp, palette.border)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (allowModifyCardImage) palette.primarySoft else palette.cardElevated),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        tint = if (allowModifyCardImage) palette.primary else palette.textMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = t.settingsAllowModifyCardImageTitle,
                        color = palette.text,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = t.settingsAllowModifyCardImageSub,
                        color = palette.textSecondary,
                        fontSize = 12.sp
                    )
                }
                Switch(
                    checked = allowModifyCardImage,
                    onCheckedChange = onToggleAllowModifyCardImage,
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = palette.primary)
                )
            }
        }

        if (allowModifyCardImage) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AppConstants.THIRUVALLUVAR_IMAGES.forEach { imgOpt ->
                    val selected = selectedCardImageId == imgOpt.id
                    val bmp = rememberAssetImageBitmap(imgOpt.assetPath)
                    Column(
                        modifier = Modifier
                            .width(105.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (selected) palette.primarySoft else palette.card)
                            .border(
                                if (selected) 2.dp else 1.dp,
                                if (selected) palette.primary else palette.border,
                                RoundedCornerShape(14.dp)
                            )
                            .clickable {
                                onSelectCardImageId(imgOpt.id)
                                onSelectCardImageTheme(CardImageTheme.PORTRAIT)
                            }
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (bmp != null) {
                            Image(
                                bitmap = bmp,
                                contentDescription = imgOpt.labelEn,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (language == AppLanguage.TA) imgOpt.labelTa else imgOpt.labelEn,
                            color = if (selected) palette.primary else palette.text,
                            fontSize = 11.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            maxLines = 2
                        )
                    }
                }

                // Custom Photo Card
                val isCustomSelected = selectedCardImageId == "custom"
                val customBmp = rememberUriImageBitmap(customImageUri)
                Column(
                    modifier = Modifier
                        .width(105.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isCustomSelected) palette.primarySoft else palette.card)
                        .border(
                            if (isCustomSelected) 2.dp else 1.dp,
                            if (isCustomSelected) palette.primary else palette.border,
                            RoundedCornerShape(14.dp)
                        )
                        .clickable {
                            if (customImageUri != null) {
                                onSelectCardImageId("custom")
                                onSelectCardImageTheme(CardImageTheme.CUSTOM)
                            } else {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        }
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (customBmp != null) {
                        Image(
                            bitmap = customBmp,
                            contentDescription = "Custom",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(palette.cardElevated),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = null,
                                tint = palette.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (language == AppLanguage.TA) "சொந்தப் படம்" else "Custom Photo",
                        color = if (isCustomSelected) palette.primary else palette.text,
                        fontSize = 11.sp,
                        fontWeight = if (isCustomSelected) FontWeight.Bold else FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                shape = RoundedCornerShape(12.dp),
                color = palette.primary
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (customImageUri != null) {
                            t.settingsChangeCustomPhotoBtn
                        } else {
                            t.settingsPickCustomPhotoBtn
                        },
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (customImageUri != null) {
                val customPreview = rememberUriImageBitmap(customImageUri)
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = palette.card,
                    border = BorderStroke(1.dp, palette.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (customPreview != null) {
                            Image(
                                bitmap = customPreview,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            )
                        }
                        Text(
                            text = t.settingsCustomPhotoSavedHint,
                            color = palette.textSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(palette.danger.copy(alpha = 0.12f))
                                .clickable { onSelectCustomImageUri(null) }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = null,
                                tint = palette.danger,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = t.settingsRemovePhotoBtn,
                                color = palette.danger,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        // 5. Daily Kural Notification
        Text(
            text = t.settingsNotificationsSectionTitle,
            color = palette.text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = t.settingsNotificationsSectionDesc,
            color = palette.textSecondary,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = palette.card,
            border = BorderStroke(1.dp, palette.border)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (notificationSettings.enabled) palette.primarySoft else palette.cardElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (notificationSettings.enabled) Icons.Default.NotificationsActive else Icons.Default.Notifications,
                            contentDescription = null,
                            tint = if (notificationSettings.enabled) palette.primary else palette.textMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = t.settingsDailyNotificationToggleTitle,
                            color = palette.text,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = t.settingsDailyNotificationToggleSub,
                            color = palette.textSecondary,
                            fontSize = 12.sp
                        )
                    }
                    Switch(
                        checked = notificationSettings.enabled,
                        onCheckedChange = { handleToggleNotifications(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = palette.primary)
                    )
                }

                if (notificationSettings.enabled) {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = palette.divider)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = palette.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "${t.settingsNotificationTimeLabel}:",
                                color = palette.textSecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = NotificationService.formatNotificationTime(
                                    notificationSettings.hour,
                                    notificationSettings.minute,
                                    language == AppLanguage.TA
                                ),
                                color = palette.primary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(palette.primarySoft)
                                .clickable {
                                    pickerHour = notificationSettings.hour
                                    pickerMinute = notificationSettings.minute
                                    showTimePickerModal = true
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = t.settingsNotificationChangeTimeBtn,
                                color = palette.primary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                val ok = NotificationService.postKuralNotification(context)
                                if (ok) {
                                    Toast.makeText(
                                        context,
                                        t.settingsNotificationTestSentToast,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    Toast.makeText(
                                        context,
                                        t.settingsNotificationPermissionDeniedMsg,
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            },
                        shape = RoundedCornerShape(10.dp),
                        color = palette.cardElevated,
                        border = BorderStroke(1.dp, palette.border)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = null,
                                tint = palette.primary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = t.settingsNotificationTestBtn,
                                color = palette.primary,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        // 6. Support & Share
        Text(
            text = t.settingsSupportSectionTitle,
            color = palette.text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = palette.card,
            border = BorderStroke(1.dp, palette.border)
        ) {
            Column {
                // Share with Friends
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, AppConstants.APP_NAME)
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "📖 ${AppConstants.APP_NAME}\nRead all 1,330 Thirukkural couplets with Tamil commentaries (Mu.Va, Solomon Pappaiah, Kalaignar, Parimelazhagar, Manakkudavar), English translations, Olineri pronunciation trainer & Status card sharing!\n\nDownload: ${AppConstants.PLAY_STORE_URL}"
                                )
                            }
                            context.startActivity(Intent.createChooser(shareIntent, AppConstants.APP_NAME))
                        }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFFEF3C7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = t.settingsShareFriendsTitle,
                            color = palette.text,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = t.settingsShareFriendsSub,
                            color = palette.textSecondary,
                            fontSize = 12.sp
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = palette.textMuted
                    )
                }

                HorizontalDivider(color = palette.divider)

                // Rate 5 Stars
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            try {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(AppConstants.PLAY_STORE_URL))
                                )
                            } catch (_: Exception) {}
                        }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFDBEAFE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = t.settingsRateAppTitle,
                            color = palette.text,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = t.settingsRateAppSub,
                            color = palette.textSecondary,
                            fontSize = 12.sp
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = palette.textMuted
                    )
                }

                HorizontalDivider(color = palette.divider)

                // Send Feedback
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            try {
                                val subject = Uri.encode("Thirukkural App Feedback ($buildVersionString)")
                                context.startActivity(
                                    Intent(
                                        Intent.ACTION_SENDTO,
                                        Uri.parse("mailto:${AppConstants.DEVELOPER_EMAIL}?subject=$subject")
                                    )
                                )
                            } catch (_: Exception) {}
                        }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFFCE7F3)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null,
                            tint = Color(0xFFDB2777),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = t.settingsFeedbackTitle,
                            color = palette.text,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = t.settingsFeedbackSub,
                            color = palette.textSecondary,
                            fontSize = 12.sp
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = palette.textMuted
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        // 7. Privacy & Security
        Text(
            text = t.settingsPrivacySectionTitle,
            color = palette.text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = palette.card,
            border = BorderStroke(1.dp, palette.border)
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showPrivacyModal = true }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFD1FAE5)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PrivacyTip,
                            contentDescription = null,
                            tint = Color(0xFF059669),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = t.settingsPrivacyPolicyTitle,
                            color = palette.text,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = t.settingsPrivacyPolicySub,
                            color = palette.textSecondary,
                            fontSize = 12.sp
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = palette.textMuted
                    )
                }

                HorizontalDivider(color = palette.divider)

                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = palette.success,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = t.settingsDataSafetyTitle,
                            color = palette.success,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    t.settingsDataSafetyBullets.forEach { bullet ->
                        Text(
                            text = bullet,
                            color = palette.textSecondary,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        // 8. Data & Offline Capability
        Text(
            text = t.settingsOfflineDataSectionTitle,
            color = palette.text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = palette.card,
            border = BorderStroke(1.dp, palette.border)
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                listOf(
                    Triple(t.settingsLabelKurals, t.settingsValueKurals, false),
                    Triple(t.settingsLabelChapters, t.settingsValueChapters, false),
                    Triple(t.settingsLabelSections, t.settingsValueSections, false),
                    Triple(t.settingsLabelCommentaries, t.settingsValueCommentaries, false),
                    Triple(t.settingsLabelOfflineMode, t.settingsValueOfflineMode, true)
                ).forEachIndexed { idx, (label, value, isSuccess) ->
                    if (idx > 0) {
                        HorizontalDivider(color = palette.divider)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 11.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = label,
                            color = palette.textSecondary,
                            fontSize = 13.5.sp
                        )
                        Text(
                            text = value,
                            color = if (isSuccess) palette.success else palette.text,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.End,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        // 9. About App
        Text(
            text = t.settingsAboutSectionTitle,
            color = palette.text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            shape = RoundedCornerShape(14.dp),
            color = palette.card,
            border = BorderStroke(1.dp, palette.border)
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                // App Name
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 11.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(t.settingsLabelAppName, color = palette.textSecondary, fontSize = 13.5.sp)
                    Text(AppConstants.APP_NAME, color = palette.text, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                }
                HorizontalDivider(color = palette.divider)

                // Version
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 11.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(t.settingsLabelVersion, color = palette.textSecondary, fontSize = 13.5.sp)
                    Text(AppConstants.APP_VERSION, color = palette.text, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                }
                HorizontalDivider(color = palette.divider)

                // Build Number (Copyable)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            ShareService.copyToClipboard(context, buildVersionString)
                            Toast.makeText(context, t.settingsBuildNumberCopiedToast, Toast.LENGTH_SHORT).show()
                        }
                        .padding(vertical = 11.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(t.settingsLabelBuildNumber, color = palette.textSecondary, fontSize = 13.5.sp)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = buildVersionString,
                            color = palette.primary,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy",
                            tint = palette.textMuted,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                HorizontalDivider(color = palette.divider)

                // Package ID
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 11.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(t.settingsLabelPackageId, color = palette.textSecondary, fontSize = 13.5.sp)
                    Text(AppConstants.PACKAGE_ID, color = palette.text, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                }
                HorizontalDivider(color = palette.divider)

                // Copyright
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 11.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(t.settingsLabelCopyright, color = palette.textSecondary, fontSize = 13.5.sp)
                    Text(AppConstants.COPYRIGHT_TEXT, color = palette.text, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    // Time Picker Modal
    if (showTimePickerModal) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showTimePickerModal = false },
            sheetState = sheetState,
            containerColor = palette.card
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = t.settingsSelectTimeModalTitle,
                        color = palette.text,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = palette.textMuted,
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { showTimePickerModal = false }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = if (language == AppLanguage.TA) "விரைவு நேரங்கள் (Quick Presets)" else "Quick Presets",
                    color = palette.textSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TIME_PRESETS.forEach { preset ->
                        val active = pickerHour == preset.hour && pickerMinute == preset.minute
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (active) palette.primarySoft else palette.cardElevated)
                                .border(
                                    1.dp,
                                    if (active) palette.primary else palette.border,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    pickerHour = preset.hour
                                    pickerMinute = preset.minute
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = if (language == AppLanguage.TA) preset.labelTa else preset.labelEn,
                                color = if (active) palette.primary else palette.text,
                                fontSize = 13.sp,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (language == AppLanguage.TA) "துல்லியமான நேரம் (Custom Time)" else "Custom Time",
                    color = palette.textSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = palette.cardElevated,
                    border = BorderStroke(1.dp, palette.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Hour Stepper
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(palette.surface)
                                    .clickable { pickerHour = (pickerHour + 1) % 24 },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = null, tint = palette.primary)
                            }
                            Text(
                                text = (if (pickerHour % 12 == 0) 12 else pickerHour % 12).toString().padStart(2, '0'),
                                color = palette.text,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(palette.surface)
                                    .clickable { pickerHour = (pickerHour + 23) % 24 },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = palette.primary)
                            }
                        }

                        Text(
                            text = ":",
                            color = palette.text,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )

                        // Minute Stepper (15-min increments)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(palette.surface)
                                    .clickable { pickerMinute = (pickerMinute + 15) % 60 },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = null, tint = palette.primary)
                            }
                            Text(
                                text = pickerMinute.toString().padStart(2, '0'),
                                color = palette.text,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(palette.surface)
                                    .clickable { pickerMinute = (pickerMinute + 45) % 60 },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = palette.primary)
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // AM / PM Toggle
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(palette.primarySoft)
                                .border(1.dp, palette.primary, RoundedCornerShape(12.dp))
                                .clickable { pickerHour = (pickerHour + 12) % 24 }
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = if (pickerHour >= 12) "PM" else "AM",
                                color = palette.primary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            onUpdateNotificationSettings(
                                notificationSettings.copy(
                                    enabled = true,
                                    hour = pickerHour,
                                    minute = pickerMinute
                                )
                            )
                            showTimePickerModal = false
                        },
                    shape = RoundedCornerShape(12.dp),
                    color = palette.primary
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 13.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${t.settingsDoneBtn} (${NotificationService.formatNotificationTime(pickerHour, pickerMinute, language == AppLanguage.TA)})",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    // Privacy Policy Modal
    if (showPrivacyModal) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showPrivacyModal = false },
            sheetState = sheetState,
            containerColor = palette.card
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = t.settingsPrivacyPolicyTitle,
                        color = palette.text,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = palette.textMuted,
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { showPrivacyModal = false }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Last Updated: ${AppConstants.PRIVACY_POLICY_LAST_UPDATED}",
                        color = palette.textMuted,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "1. Offline-First & Zero Data Collection",
                        color = palette.text,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${AppConstants.APP_NAME} is designed as a 100% offline-first application. We do not collect, transmit, store, or share any personal information, device identifiers, location data, or usage analytics on external servers.",
                        color = palette.textSecondary,
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )
                    Text(
                        text = "2. Local Device Storage",
                        color = palette.text,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Your custom Kural collections, theme preferences, language choices, and notification schedules are stored exclusively on your device using local application storage.",
                        color = palette.textSecondary,
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )
                    Text(
                        text = "3. Photos, Media & Microphone Permissions",
                        color = palette.text,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "If you choose to select a custom background photo for status cards, that image is processed locally on your device solely to render the shareable image card. If you enable Kural Learning Mode, microphone audio is analyzed on-device for Tamil phoneme pronunciation feedback and is never uploaded to any external server.",
                        color = palette.textSecondary,
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )
                    Text(
                        text = "4. Contact Us",
                        color = palette.text,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "If you have any questions about this Privacy Policy, please contact us at ${AppConstants.DEVELOPER_EMAIL}.",
                        color = palette.textSecondary,
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )
                }
            }
        }
    }
}

