package com.nltsravi.kurals.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nltsravi.kurals.constants.AppConstants
import com.nltsravi.kurals.data.KuralRepository
import com.nltsravi.kurals.model.Kural
import com.nltsravi.kurals.service.OlineriService
import com.nltsravi.kurals.service.ShareService
import com.nltsravi.kurals.ui.components.ExportModal
import com.nltsravi.kurals.ui.components.KuralCoupletText
import com.nltsravi.kurals.ui.components.KuralPronunciationTrainer
import com.nltsravi.kurals.ui.strings.LocalAppTranslations
import com.nltsravi.kurals.ui.theme.KuralsPalettes
import com.nltsravi.kurals.ui.theme.LocalAppPalette
import com.nltsravi.kurals.util.TextUtils

@Composable
fun KuralDetailScreen(
    kuralNumber: Int,
    isBookmarked: Boolean,
    isLearningModeEnabled: Boolean,
    pronunciationSDK: OlineriService.PronunciationSDK,
    onBack: () -> Unit,
    onNavigateToKural: (Int) -> Unit,
    onNavigateToChapter: (Int) -> Unit,
    onBookmarkClick: (Int) -> Unit,
    onShareClick: (Kural) -> Unit
) {
    val context = LocalContext.current
    val palette = LocalAppPalette.current
    val t = LocalAppTranslations.current

    val kural = remember(kuralNumber) {
        KuralRepository.getKuralByNumber(kuralNumber)
    }

    var showExportModal by remember { mutableStateOf(false) }

    if (kural == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.background),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = t.kuralNotFound, color = palette.text, fontSize = 16.sp)
        }
        return
    }

    val catColors = KuralsPalettes.categoryColors(kural.categoryId, palette.isDark)
    val prevNum = if (kural.number > 1) kural.number - 1 else null
    val nextNum = if (kural.number < AppConstants.TOTAL_KURALS) kural.number + 1 else null

    fun handleCopy(mode: String) {
        val text = TextUtils.formatKuralText(kural, mode)
        ShareService.copyToClipboard(context, text)
        Toast.makeText(context, t.kuralCopiedToast, Toast.LENGTH_SHORT).show()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
    ) {
        // Top Bar
        Surface(
            color = palette.background,
            border = BorderStroke(0.5.dp, palette.divider),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = palette.text,
                    modifier = Modifier
                        .size(26.dp)
                        .clickable { onBack() }
                )

                Text(
                    text = "${t.kuralHeaderLabel} #${kural.number}",
                    color = palette.text,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Bookmark",
                        tint = if (isBookmarked) palette.primary else palette.text,
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { onBookmarkClick(kural.number) }
                    )
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = palette.text,
                        modifier = Modifier
                            .size(22.dp)
                            .clickable { onShareClick(kural) }
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Main Couplet Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                shape = RoundedCornerShape(18.dp),
                color = palette.card,
                border = BorderStroke(1.5.dp, palette.primary)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(palette.badgeBg)
                                .padding(horizontal = 12.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "குறள் #${kural.number}",
                                color = palette.badgeText,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(catColors.bg)
                                .border(1.dp, catColors.border, RoundedCornerShape(10.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${kural.categoryTamil} • ${kural.categoryEnglish}",
                                color = catColors.text,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Chapter Link Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToChapter(kural.chapterNumber) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "அதிகாரம் ${kural.chapterNumber}: ${kural.chapterNameTamil} (${kural.chapterNameEnglish})",
                            color = palette.primary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = palette.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 2-Line Tamil Couplet
                    KuralCoupletText(
                        line1 = kural.line1,
                        line2 = kural.line2,
                        color = palette.text,
                        maxFontSize = 17.5f,
                        minFontSize = 11f,
                        lineGapDp = 7
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Transliteration Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(palette.cardElevated)
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = kural.transliteration1,
                                color = palette.textSecondary,
                                fontSize = 14.sp,
                                fontStyle = FontStyle.Italic,
                                lineHeight = 21.sp
                            )
                            Text(
                                text = kural.transliteration2,
                                color = palette.textSecondary,
                                fontSize = 14.sp,
                                fontStyle = FontStyle.Italic,
                                lineHeight = 21.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = palette.divider)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Share Status Card + Export PDF/DOCX Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(palette.primary)
                                .clickable { onShareClick(kural) }
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = t.kuralShareStatusCardBtn,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(palette.cardElevated)
                                .border(1.dp, palette.border, RoundedCornerShape(10.dp))
                                .clickable { showExportModal = true }
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                tint = palette.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = t.kuralExportPdfDocxBtn,
                                color = palette.primary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Copy Action Bar (Tamil / Translit / Both)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "tamil" to t.kuralCopyTamil,
                            "translit" to t.kuralCopyTranslit,
                            "both" to t.kuralCopyBoth
                        ).forEach { (mode, label) ->
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(palette.cardElevated)
                                    .border(1.dp, palette.border, RoundedCornerShape(10.dp))
                                    .clickable { handleCopy(mode) }
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = null,
                                    tint = palette.textSecondary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = label,
                                    color = palette.textSecondary,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // Olineri Tamil Pronunciation Trainer (when Learning Mode is enabled)
            if (isLearningModeEnabled) {
                KuralPronunciationTrainer(
                    kural = kural,
                    sdk = pronunciationSDK
                )
            }

            // English Translation & G.U. Pope Couplet
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                shape = RoundedCornerShape(16.dp),
                color = palette.card,
                border = BorderStroke(1.dp, palette.border)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = t.kuralEnglishTranslationTitle,
                        color = palette.text,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = kural.translation,
                        color = palette.textSecondary,
                        fontSize = 14.sp,
                        lineHeight = 22.sp
                    )
                    if (kural.couplet.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(palette.cardElevated)
                                .padding(12.dp)
                        ) {
                            Column {
                                Text(
                                    text = t.kuralGuPopeCoupletLabel,
                                    color = palette.textMuted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "\"${kural.couplet}\"",
                                    color = palette.textSecondary,
                                    fontSize = 13.sp,
                                    fontStyle = FontStyle.Italic,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }
                }
            }

            // 5 Tamil Commentaries (உரைகள்)
            Text(
                text = t.kuralCommentariesTitle,
                color = palette.text,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 10.dp)
            )

            data class ScholarCommentary(
                val badge: String,
                val badgeBg: Color,
                val badgeText: Color,
                val name: String,
                val text: String
            )

            val commentaries = listOf(
                ScholarCommentary("மு.வ", Color(0xFFFEF3C7), Color(0xFFB45309), t.kuralScholarMv, kural.mv),
                ScholarCommentary("சா.பா", Color(0xFFDBEAFE), Color(0xFF1D4ED8), t.kuralScholarSp, kural.sp),
                ScholarCommentary("மு.க", Color(0xFFFCE7F3), Color(0xFFBE185D), t.kuralScholarMk, kural.mk),
                ScholarCommentary("பரி", Color(0xFFD1FAE5), Color(0xFF065F46), t.kuralScholarPrm, kural.prm),
                ScholarCommentary("மண", Color(0xFFEDE9FE), Color(0xFF5B21B6), t.kuralScholarMnk, kural.mnk)
            )

            commentaries.forEach { item ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = palette.card,
                    border = BorderStroke(1.dp, palette.border)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(item.badgeBg)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = item.badge,
                                    color = item.badgeText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            Text(
                                text = item.name,
                                color = palette.text,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = item.text,
                            color = palette.textSecondary,
                            fontSize = 14.sp,
                            lineHeight = 22.sp
                        )
                    }
                }
            }

            // Structure & Metadata
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
                color = palette.card,
                border = BorderStroke(1.dp, palette.border)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = t.kuralStructureMetadataTitle,
                        color = palette.text,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    listOf(
                        t.kuralMetaPaal to "${kural.categoryTamil} (${kural.categoryEnglish})",
                        t.kuralMetaIyal to "${kural.chapterGroupTamil} (${kural.chapterGroupEnglish})",
                        t.kuralMetaAdhigaram to "#${kural.chapterNumber} ${kural.chapterNameTamil} (${kural.chapterNameEnglish})"
                    ).forEach { (label, value) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = label,
                                color = palette.textMuted,
                                fontSize = 13.sp
                            )
                            Text(
                                text = value,
                                color = palette.text,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Previous / Next Kural Navigation
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(enabled = prevNum != null) {
                            if (prevNum != null) onNavigateToKural(prevNum)
                        },
                    shape = RoundedCornerShape(12.dp),
                    color = palette.card,
                    border = BorderStroke(1.dp, palette.border)
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = null,
                            tint = if (prevNum != null) palette.text else palette.textMuted,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = t.kuralPrevBtn(prevNum ?: 1),
                            color = if (prevNum != null) palette.text else palette.textMuted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(enabled = nextNum != null) {
                            if (nextNum != null) onNavigateToKural(nextNum)
                        },
                    shape = RoundedCornerShape(12.dp),
                    color = palette.card,
                    border = BorderStroke(1.dp, palette.border)
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = t.kuralNextBtn(nextNum ?: AppConstants.TOTAL_KURALS),
                            color = if (nextNum != null) palette.text else palette.textMuted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = if (nextNum != null) palette.text else palette.textMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }

    ExportModal(
        visible = showExportModal,
        title = "Thirukkural #${kural.number} - ${kural.chapterNameTamil}",
        subtitle = "${kural.categoryTamil} • ${kural.chapterNameEnglish}",
        kurals = listOf(kural),
        onClose = { showExportModal = false }
    )
}

