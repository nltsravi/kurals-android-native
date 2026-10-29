package com.nltsravi.kurals.ui.components

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nltsravi.kurals.constants.AppConstants
import com.nltsravi.kurals.model.AppLanguage
import com.nltsravi.kurals.model.CardAspectRatio
import com.nltsravi.kurals.model.CardImageTheme
import com.nltsravi.kurals.model.Kural
import com.nltsravi.kurals.model.MeaningType
import com.nltsravi.kurals.model.SocialPlatform
import com.nltsravi.kurals.model.SocialShareOptions
import com.nltsravi.kurals.service.ShareService
import com.nltsravi.kurals.ui.strings.LocalAppLanguage
import com.nltsravi.kurals.ui.strings.LocalAppTranslations
import com.nltsravi.kurals.ui.theme.LocalAppPalette
import com.nltsravi.kurals.util.TextUtils
import com.nltsravi.kurals.util.rememberAssetImageBitmap
import com.nltsravi.kurals.util.rememberUriImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ShareModal(
    visible: Boolean,
    kural: Kural?,
    allowModifyCardImage: Boolean,
    selectedCardImageId: String,
    onSelectCardImageId: (String) -> Unit,
    cardImageTheme: CardImageTheme,
    onSelectCardImageTheme: (CardImageTheme) -> Unit,
    customImageUri: String?,
    onSelectCustomImageUri: (String?) -> Unit,
    onClose: () -> Unit
) {
    if (!visible || kural == null) return

    val context = LocalContext.current
    val palette = LocalAppPalette.current
    val t = LocalAppTranslations.current
    val language = LocalAppLanguage.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var activeTab by remember { mutableStateOf("image") }
    var meaningType by remember { mutableStateOf(MeaningType.MV) }
    var aspectRatio by remember { mutableStateOf(CardAspectRatio.RATIO_9_16) }
    var includeTransliteration by remember { mutableStateOf(false) }
    var includeHashtags by remember { mutableStateOf(true) }
    var copiedFeedback by remember { mutableStateOf(false) }
    var isSharingImage by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}
            onSelectCustomImageUri(uri.toString())
        }
    }

    val shareOptions = remember(meaningType, includeTransliteration, includeHashtags) {
        SocialShareOptions(
            meaningType = meaningType,
            includeTransliteration = includeTransliteration,
            includeHashtags = includeHashtags
        )
    }

    val previewText = remember(kural, shareOptions) {
        TextUtils.formatKuralForSocialShare(kural, shareOptions)
    }

    fun triggerImageShare(platform: SocialPlatform) {
        if (isSharingImage) return
        isSharingImage = true
        scope.launch {
            try {
                val bmp = withContext(Dispatchers.Default) {
                    KuralCardBitmapRenderer.renderBitmap(
                        context = context,
                        kural = kural,
                        meaningType = meaningType,
                        aspectRatio = aspectRatio,
                        allowModifyCardImage = allowModifyCardImage,
                        selectedCardImageId = selectedCardImageId,
                        imageTheme = cardImageTheme,
                        customImageUri = customImageUri
                    )
                }
                ShareService.shareImageToSocialPlatform(
                    context = context,
                    bitmap = bmp,
                    kural = kural,
                    platform = platform,
                    options = shareOptions
                )
            } catch (e: Exception) {
                Toast.makeText(context, e.message ?: "Share failed", Toast.LENGTH_SHORT).show()
            } finally {
                isSharingImage = false
            }
        }
    }

    fun triggerTextShare(platform: SocialPlatform) {
        ShareService.shareTextToSocialPlatform(
            context = context,
            kural = kural,
            platform = platform,
            options = shareOptions
        )
    }

    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
        containerColor = palette.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = t.shareModalHeaderTitle(kural.number),
                        color = palette.text,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${kural.chapterNameTamil} • ${kural.chapterNameEnglish}",
                        color = palette.textSecondary,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = palette.textMuted,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { onClose() }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Mode Switcher (Image Card vs Text Post)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = palette.cardElevated,
                border = BorderStroke(1.dp, palette.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val isImg = activeTab == "image"
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (isImg) palette.primary else Color.Transparent)
                            .clickable { activeTab = "image" }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            tint = if (isImg) Color.White else palette.textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = t.shareTabImageCard,
                            color = if (isImg) Color.White else palette.textSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    val isTxt = activeTab == "text"
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (isTxt) palette.primary else Color.Transparent)
                            .clickable { activeTab = "text" }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = if (isTxt) Color.White else palette.textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = t.shareTabTextPost,
                            color = if (isTxt) Color.White else palette.textSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
            ) {
                if (activeTab == "image") {
                    // Live Preview
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        KuralImageCard(
                            kural = kural,
                            meaningType = meaningType,
                            aspectRatio = aspectRatio,
                            allowModifyCardImage = allowModifyCardImage,
                            selectedCardImageId = selectedCardImageId,
                            imageTheme = cardImageTheme,
                            customImageUri = customImageUri
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = t.shareLivePreviewLabel,
                            color = palette.textMuted,
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Meaning selector pills
                    Text(
                        text = t.shareMeaningSectionLabel,
                        color = palette.textSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AppConstants.MEANING_OPTIONS.forEach { opt ->
                            val selected = meaningType == opt.key
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selected) palette.primarySoft else palette.cardElevated)
                                    .border(
                                        1.dp,
                                        if (selected) palette.primary else palette.border,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { meaningType = opt.key }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = opt.tamilLabel,
                                    color = if (selected) palette.primary else palette.textSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Aspect Ratio selector
                    Text(
                        text = t.shareCardSizeSectionLabel,
                        color = palette.textSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppConstants.ASPECT_RATIO_OPTIONS.forEach { opt ->
                            val selected = aspectRatio == opt.key
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selected) palette.primarySoft else palette.cardElevated)
                                    .border(
                                        1.dp,
                                        if (selected) palette.primary else palette.border,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { aspectRatio = opt.key }
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${opt.label} ",
                                    color = if (selected) palette.primary else palette.text,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = opt.subtitle,
                                    color = if (selected) palette.primary else palette.textMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    // Card Image selector (only when allowModifyCardImage is enabled in Settings)
                    if (allowModifyCardImage) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = t.shareImageStyleSectionLabel,
                            color = palette.textSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AppConstants.THIRUVALLUVAR_IMAGES.forEach { imgOpt ->
                                val selected = selectedCardImageId == imgOpt.id
                                val thumbBmp = rememberAssetImageBitmap(imgOpt.assetPath)
                                Column(
                                    modifier = Modifier
                                        .width(92.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (selected) palette.primarySoft else palette.cardElevated)
                                        .border(
                                            if (selected) 2.dp else 1.dp,
                                            if (selected) palette.primary else palette.border,
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            onSelectCardImageId(imgOpt.id)
                                            onSelectCardImageTheme(CardImageTheme.PORTRAIT)
                                        }
                                        .padding(6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    if (thumbBmp != null) {
                                        Image(
                                            bitmap = thumbBmp,
                                            contentDescription = imgOpt.labelEn,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(56.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (language == AppLanguage.TA) imgOpt.labelTa else imgOpt.labelEn,
                                        color = if (selected) palette.primary else palette.text,
                                        fontSize = 10.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 2,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 12.sp
                                    )
                                }
                            }

                            // Custom Photo option
                            val isCustomSelected = selectedCardImageId == "custom"
                            val customThumb = rememberUriImageBitmap(customImageUri)
                            Column(
                                modifier = Modifier
                                    .width(92.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isCustomSelected) palette.primarySoft else palette.cardElevated)
                                    .border(
                                        if (isCustomSelected) 2.dp else 1.dp,
                                        if (isCustomSelected) palette.primary else palette.border,
                                        RoundedCornerShape(12.dp)
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
                                    .padding(6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (customThumb != null) {
                                    Image(
                                        bitmap = customThumb,
                                        contentDescription = "Custom",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(palette.surface),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PhotoLibrary,
                                            contentDescription = null,
                                            tint = palette.primary
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (language == AppLanguage.TA) "சொந்தப் படம்" else "Custom Photo",
                                    color = if (isCustomSelected) palette.primary else palette.text,
                                    fontSize = 10.sp,
                                    fontWeight = if (isCustomSelected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 2,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        if (selectedCardImageId == "custom" || customImageUri != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                shape = RoundedCornerShape(8.dp),
                                color = palette.primarySoft,
                                border = BorderStroke(1.dp, palette.primary)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PhotoLibrary,
                                        contentDescription = null,
                                        tint = palette.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = t.sharePickChangeCustomPhoto,
                                        color = palette.primary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Text Format Options
                    Text(
                        text = t.shareMeaningSectionLabel,
                        color = palette.textSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AppConstants.MEANING_OPTIONS.forEach { opt ->
                            val selected = meaningType == opt.key
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selected) palette.primarySoft else palette.cardElevated)
                                    .border(
                                        1.dp,
                                        if (selected) palette.primary else palette.border,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { meaningType = opt.key }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = opt.tamilLabel,
                                    color = if (selected) palette.primary else palette.textSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.clickable { includeTransliteration = !includeTransliteration },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (includeTransliteration) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                contentDescription = null,
                                tint = if (includeTransliteration) palette.primary else palette.textMuted,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = t.shareIncludeTranslit,
                                color = palette.text,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Row(
                            modifier = Modifier.clickable { includeHashtags = !includeHashtags },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (includeHashtags) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                contentDescription = null,
                                tint = if (includeHashtags) palette.primary else palette.textMuted,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = t.shareIncludeHashtags,
                                color = palette.text,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Text Post Preview Box
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = palette.cardElevated,
                        border = BorderStroke(1.dp, palette.border)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = t.sharePostPreviewLabel.uppercase(),
                                    color = palette.textMuted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${t.shareCharactersCount(previewText.length)} ${if (previewText.length <= 280) "• X ✓" else ""}",
                                    color = if (previewText.length <= 280) palette.success else palette.textMuted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = previewText,
                                color = palette.text,
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Social Platform Action Grid
                Text(
                    text = if (activeTab == "image") t.shareImageSectionTitle else t.shareTextSectionTitle,
                    color = palette.text,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                data class PlatformBtn(
                    val platform: SocialPlatform,
                    val label: String,
                    val sub: String,
                    val color: Color,
                    val badge: String
                )

                val platforms = listOf(
                    PlatformBtn(SocialPlatform.WHATSAPP_STATUS, "WhatsApp", "Status / Chat", Color(0xFF25D366), "WA"),
                    PlatformBtn(SocialPlatform.INSTAGRAM, "Instagram", "Story / Post", Color(0xFFE1306C), "IG"),
                    PlatformBtn(SocialPlatform.TWITTER, "X (Twitter)", "Post", Color(0xFF1DA1F2), "X"),
                    PlatformBtn(SocialPlatform.THREADS, "Threads", "Post", Color(0xFF6B7280), "@"),
                    PlatformBtn(SocialPlatform.FACEBOOK, "Facebook", "Story / Feed", Color(0xFF1877F2), "f"),
                    PlatformBtn(SocialPlatform.SYSTEM, "More Options", "Save / Share", palette.primary, "⋯")
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    maxItemsInEachRow = 3
                ) {
                    platforms.forEach { item ->
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    if (activeTab == "image") {
                                        triggerImageShare(item.platform)
                                    } else {
                                        triggerTextShare(item.platform)
                                    }
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = palette.cardElevated,
                            border = BorderStroke(1.dp, palette.border)
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(item.color.copy(alpha = 0.16f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = item.badge,
                                        color = item.color,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = item.label,
                                    color = palette.text,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                Text(
                                    text = item.sub,
                                    color = palette.textMuted,
                                    fontSize = 9.5.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Quick Copy Button
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            ShareService.copyToClipboard(context, previewText)
                            copiedFeedback = true
                            scope.launch {
                                delay(2000)
                                copiedFeedback = false
                            }
                        },
                    shape = RoundedCornerShape(12.dp),
                    color = if (copiedFeedback) palette.success else palette.cardElevated,
                    border = BorderStroke(1.dp, if (copiedFeedback) palette.success else palette.primary)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (copiedFeedback) Icons.Default.Check else Icons.Default.ContentCopy,
                            contentDescription = null,
                            tint = if (copiedFeedback) Color.White else palette.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (copiedFeedback) t.shareCopiedBtn else t.shareQuickCopyBtn,
                            color = if (copiedFeedback) Color.White else palette.primary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

