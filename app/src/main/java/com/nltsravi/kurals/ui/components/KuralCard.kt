package com.nltsravi.kurals.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nltsravi.kurals.model.Kural
import com.nltsravi.kurals.ui.theme.KuralsPalettes
import com.nltsravi.kurals.ui.theme.LocalAppPalette

@Composable
fun KuralCard(
    kural: Kural,
    isBookmarked: Boolean,
    onClick: () -> Unit,
    onBookmarkClick: (() -> Unit)? = null,
    onShareClick: (() -> Unit)? = null,
    onPracticeClick: (() -> Unit)? = null,
    showChapterBadge: Boolean = true,
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    val palette = LocalAppPalette.current
    val catColors = KuralsPalettes.categoryColors(kural.categoryId, palette.isDark)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = palette.card,
        border = BorderStroke(1.dp, palette.border)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top bar: Number badge + Category pill + Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(palette.badgeBg)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "#${kural.number}",
                            color = palette.badgeText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (showChapterBadge) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(catColors.bg)
                                .border(1.dp, catColors.border, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = kural.categoryTamil,
                                color = catColors.text,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onPracticeClick != null) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(palette.primarySoft)
                                .border(1.dp, palette.primary, CircleShape)
                                .clickable(onClick = onPracticeClick),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Practice Pronunciation",
                                tint = palette.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    if (onShareClick != null) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(palette.cardElevated)
                                .border(1.dp, palette.border, CircleShape)
                                .clickable(onClick = onShareClick),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share Kural",
                                tint = palette.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    if (onBookmarkClick != null) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .clickable(onClick = onBookmarkClick),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = "Bookmark Kural",
                                tint = if (isBookmarked) palette.primary else palette.textMuted,
                                modifier = Modifier.size(21.dp)
                            )
                        }
                    }
                }
            }

            if (showChapterBadge) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${kural.chapterNumber}. ${kural.chapterNameTamil} (${kural.chapterNameEnglish})",
                    color = palette.textSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 2-Line Tamil Couplet
            KuralCoupletText(
                line1 = kural.line1,
                line2 = kural.line2,
                color = palette.text,
                maxFontSize = 16.5f,
                minFontSize = 10.5f,
                lineGapDp = 5
            )

            // Transliteration
            if (!compact) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(palette.cardElevated)
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Column {
                        Text(
                            text = kural.transliteration1,
                            color = palette.textSecondary,
                            fontSize = 13.sp,
                            fontStyle = FontStyle.Italic,
                            lineHeight = 19.sp
                        )
                        Text(
                            text = kural.transliteration2,
                            color = palette.textSecondary,
                            fontSize = 13.sp,
                            fontStyle = FontStyle.Italic,
                            lineHeight = 19.sp
                        )
                    }
                }
            }

            // Meaning preview
            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = palette.divider, thickness = 1.dp)
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = kural.mv,
                color = palette.textSecondary,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                maxLines = if (compact) 1 else 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = kural.translation,
                color = palette.textMuted,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                maxLines = if (compact) 1 else 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

