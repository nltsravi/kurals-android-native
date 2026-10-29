package com.nltsravi.kurals.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nltsravi.kurals.constants.AppConstants
import com.nltsravi.kurals.model.Kural
import com.nltsravi.kurals.service.SearchService
import com.nltsravi.kurals.ui.components.EmptyState
import com.nltsravi.kurals.ui.components.KuralCard
import com.nltsravi.kurals.ui.components.SearchBar
import com.nltsravi.kurals.ui.strings.LocalAppTranslations
import com.nltsravi.kurals.ui.theme.LocalAppPalette
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    isKuralSaved: (Int) -> Boolean,
    isLearningModeEnabled: Boolean,
    onOpenKuralDetail: (Int, Boolean) -> Unit,
    onShareKural: (Kural) -> Unit,
    onBookmarkKural: (Int) -> Unit
) {
    val palette = LocalAppPalette.current
    val t = LocalAppTranslations.current

    var query by remember { mutableStateOf("") }
    var debouncedQuery by remember { mutableStateOf("") }

    LaunchedEffect(query) {
        delay(200)
        debouncedQuery = query
    }

    val results = remember(debouncedQuery) {
        if (debouncedQuery.isBlank()) emptyList()
        else SearchService.searchKurals(debouncedQuery, 60)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
    ) {
        // Header + Search Input
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp, bottom = 10.dp)
        ) {
            Text(
                text = t.searchHeaderTitle,
                color = palette.text,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = t.searchHeaderSubtitle,
                color = palette.textSecondary,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            SearchBar(
                value = query,
                onValueChange = { query = it },
                placeholder = t.searchInputPlaceholder,
                autoFocus = false
            )
        }

        if (debouncedQuery.isBlank()) {
            // Suggestions + Search Guide
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = t.searchTryQuickTitle,
                    color = palette.text,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppConstants.SEARCH_SUGGESTIONS.forEach { chip ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(palette.card)
                                .border(1.dp, palette.border, RoundedCornerShape(20.dp))
                                .clickable {
                                    query = chip
                                    debouncedQuery = chip
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = chip,
                                color = palette.text,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = palette.card,
                    border = BorderStroke(1.dp, palette.border)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = t.searchGuideTitle,
                            color = palette.text,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        t.searchGuideBullets.forEach { bullet ->
                            Text(
                                text = bullet,
                                color = palette.textSecondary,
                                fontSize = 13.sp,
                                lineHeight = 19.sp
                            )
                        }
                    }
                }
            }
        } else {
            // Results Header + List
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = t.searchResultsCount(results.size),
                    color = palette.textSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = t.searchSortRelevance,
                    color = palette.textMuted,
                    fontSize = 12.sp
                )
            }

            if (results.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.Search,
                    title = t.searchEmptyResultTitle,
                    description = t.searchEmptyResultDesc
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    items(results, key = { it.kural.number }) { item ->
                        KuralCard(
                            kural = item.kural,
                            isBookmarked = isKuralSaved(item.kural.number),
                            onClick = { onOpenKuralDetail(item.kural.number, false) },
                            onBookmarkClick = { onBookmarkKural(item.kural.number) },
                            onShareClick = { onShareKural(item.kural) },
                            onPracticeClick = if (isLearningModeEnabled) {
                                { onOpenKuralDetail(item.kural.number, true) }
                            } else null
                        )
                    }
                }
            }
        }
    }
}

