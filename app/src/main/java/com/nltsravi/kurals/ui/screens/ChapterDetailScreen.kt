package com.nltsravi.kurals.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Download
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nltsravi.kurals.constants.AppConstants
import com.nltsravi.kurals.data.KuralRepository
import com.nltsravi.kurals.model.Kural
import com.nltsravi.kurals.ui.components.ExportModal
import com.nltsravi.kurals.ui.components.KuralCard
import com.nltsravi.kurals.ui.strings.LocalAppTranslations
import com.nltsravi.kurals.ui.theme.LocalAppPalette

@Composable
fun ChapterDetailScreen(
    chapterNumber: Int,
    isKuralSaved: (Int) -> Boolean,
    isLearningModeEnabled: Boolean,
    onBack: () -> Unit,
    onNavigateToChapter: (Int) -> Unit,
    onOpenKuralDetail: (Int, Boolean) -> Unit,
    onShareKural: (Kural) -> Unit,
    onBookmarkKural: (Int) -> Unit
) {
    val palette = LocalAppPalette.current
    val t = LocalAppTranslations.current

    val chapter = remember(chapterNumber) {
        KuralRepository.getChapterByNumber(chapterNumber)
    }
    val kurals = remember(chapterNumber) {
        KuralRepository.getKuralsByChapter(chapterNumber)
    }

    var showExportModal by remember { mutableStateOf(false) }

    if (chapter == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.background),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = t.chapterNotFound, color = palette.text, fontSize = 16.sp)
        }
        return
    }

    val prevChapter = if (chapter.number > 1) chapter.number - 1 else null
    val nextChapter = if (chapter.number < AppConstants.TOTAL_CHAPTERS) chapter.number + 1 else null

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
                    text = "${chapter.number}. ${chapter.nameTamil}",
                    color = palette.text,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                )

                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = "Export Chapter",
                    tint = palette.primary,
                    modifier = Modifier
                        .size(22.dp)
                        .clickable { showExportModal = true }
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp)
        ) {
            item(key = "chapter-header") {
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
                            text = t.chapterBreadcrumb(
                                chapter.categoryTamil,
                                chapter.chapterGroupTamil,
                                chapter.number
                            ),
                            color = palette.primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = chapter.nameTamil,
                            color = palette.text,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "${chapter.nameEnglish} • ${chapter.transliteration}",
                            color = palette.textSecondary,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = palette.divider)
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = t.chapterKuralsRangeLabel(chapter.start, chapter.end),
                                color = palette.textMuted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(palette.primarySoft)
                                    .clickable { showExportModal = true }
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = null,
                                    tint = palette.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = t.chapterExportPdfDocx,
                                    color = palette.primary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            items(kurals, key = { it.number }) { kural ->
                KuralCard(
                    kural = kural,
                    isBookmarked = isKuralSaved(kural.number),
                    showChapterBadge = false,
                    onClick = { onOpenKuralDetail(kural.number, false) },
                    onBookmarkClick = { onBookmarkKural(kural.number) },
                    onShareClick = { onShareKural(kural) },
                    onPracticeClick = if (isLearningModeEnabled) {
                        { onOpenKuralDetail(kural.number, true) }
                    } else null
                )
            }

            item(key = "chapter-nav-footer") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp, bottom = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(enabled = prevChapter != null) {
                                if (prevChapter != null) onNavigateToChapter(prevChapter)
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
                                tint = if (prevChapter != null) palette.text else palette.textMuted,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = t.chapterPrev(prevChapter ?: 1),
                                color = if (prevChapter != null) palette.text else palette.textMuted,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(enabled = nextChapter != null) {
                                if (nextChapter != null) onNavigateToChapter(nextChapter)
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
                                text = t.chapterNext(nextChapter ?: AppConstants.TOTAL_CHAPTERS),
                                color = if (nextChapter != null) palette.text else palette.textMuted,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = if (nextChapter != null) palette.text else palette.textMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    ExportModal(
        visible = showExportModal,
        title = "அதிகாரம் ${chapter.number} - ${chapter.nameTamil} (${chapter.nameEnglish})",
        subtitle = "${chapter.categoryTamil} • ${chapter.chapterGroupTamil} • Kurals ${chapter.start}-${chapter.end}",
        kurals = kurals,
        onClose = { showExportModal = false }
    )
}

