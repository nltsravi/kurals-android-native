package com.nltsravi.kurals.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nltsravi.kurals.data.KuralRepository
import com.nltsravi.kurals.ui.components.ChapterCard
import com.nltsravi.kurals.ui.strings.LocalAppTranslations
import com.nltsravi.kurals.ui.theme.LocalAppPalette

@Composable
fun ChaptersScreen(
    initialCategoryId: Int? = null,
    onOpenChapterDetail: (Int) -> Unit
) {
    val palette = LocalAppPalette.current
    val t = LocalAppTranslations.current
    val categories = remember { KuralRepository.getAllCategories() }

    var activeCategoryId by remember {
        mutableIntStateOf(if (initialCategoryId in listOf(1, 2, 3)) initialCategoryId!! else 1)
    }

    LaunchedEffect(initialCategoryId) {
        if (initialCategoryId != null && initialCategoryId in listOf(1, 2, 3)) {
            activeCategoryId = initialCategoryId
        }
    }

    val activeCategory = remember(activeCategoryId, categories) {
        categories.find { it.number == activeCategoryId } ?: categories.first()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp, bottom = 10.dp)
        ) {
            Text(
                text = t.chaptersHeaderTitle,
                color = palette.text,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = t.chaptersHeaderSubtitle,
                color = palette.textSecondary,
                fontSize = 12.sp
            )
        }

        // Category Filter Pills
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { cat ->
                val isSelected = cat.number == activeCategoryId
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) palette.primary else palette.card)
                        .border(
                            1.dp,
                            if (isSelected) palette.primary else palette.border,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { activeCategoryId = cat.number }
                        .padding(vertical = 10.dp, horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = cat.nameTamil,
                        color = if (isSelected) Color.White else palette.text,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${cat.nameEnglish} (${cat.chapters.size})",
                        color = if (isSelected) Color.White.copy(alpha = 0.85f) else palette.textMuted,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Grouped Chapters by ChapterGroup (இயல்)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
        ) {
            activeCategory.chapterGroups.forEach { cg ->
                item(key = "group-${cg.number}") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = cg.nameTamil,
                                color = palette.text,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${cg.nameEnglish} • ${cg.transliteration}",
                                color = palette.textSecondary,
                                fontSize = 12.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(palette.badgeBg)
                                .padding(horizontal = 10.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "${cg.chapters.size} ${t.homeChaptersUnit}",
                                color = palette.badgeText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                items(cg.chapters, key = { it.number }) { chapter ->
                    ChapterCard(
                        chapter = chapter,
                        onClick = { onOpenChapterDetail(chapter.number) }
                    )
                }
            }
        }
    }
}

