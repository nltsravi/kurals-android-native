package com.nltsravi.kurals.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nltsravi.kurals.data.KuralRepository
import com.nltsravi.kurals.model.KuralCollection
import com.nltsravi.kurals.ui.components.CollectionCard
import com.nltsravi.kurals.ui.components.EmptyState
import com.nltsravi.kurals.ui.components.ExportModal
import com.nltsravi.kurals.ui.strings.LocalAppTranslations
import com.nltsravi.kurals.ui.theme.LocalAppPalette

@Composable
fun CollectionsScreen(
    collections: List<KuralCollection>,
    onOpenCollectionDetail: (String) -> Unit,
    onOpenCreateCollection: () -> Unit,
    onDeleteCollection: (String) -> Unit
) {
    val palette = LocalAppPalette.current
    val t = LocalAppTranslations.current

    var exportCollection by remember { mutableStateOf<KuralCollection?>(null) }
    var deleteCandidate by remember { mutableStateOf<KuralCollection?>(null) }

    val exportKurals = remember(exportCollection) {
        exportCollection?.let { KuralRepository.getKuralsByNumbers(it.kuralNumbers) } ?: emptyList()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = t.collectionsHeaderTitle,
                    color = palette.text,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = t.collectionsHeaderSubtitle(collections.size),
                    color = palette.textSecondary,
                    fontSize = 12.sp
                )
            }

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(palette.primary)
                    .clickable(onClick = onOpenCreateCollection)
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = t.collectionsNewBtn,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (collections.isEmpty()) {
            EmptyState(
                icon = Icons.Default.Bookmarks,
                title = t.collectionsEmptyTitle,
                description = t.collectionsEmptyDesc
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
            ) {
                items(collections, key = { it.id }) { col ->
                    CollectionCard(
                        collection = col,
                        onClick = { onOpenCollectionDetail(col.id) },
                        onExportClick = { exportCollection = col },
                        onDeleteClick = { deleteCandidate = col }
                    )
                }
            }
        }
    }

    // Delete Confirmation Dialog
    deleteCandidate?.let { candidate ->
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text(t.collectionsDeleteTitle, fontWeight = FontWeight.Bold) },
            text = { Text(t.collectionsDeleteConfirmMsg(candidate.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteCollection(candidate.id)
                        deleteCandidate = null
                    }
                ) {
                    Text(t.collectionsDeleteBtn, color = palette.danger, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteCandidate = null }) {
                    Text(t.collectionsCancelBtn, color = palette.textSecondary)
                }
            }
        )
    }

    // Export Modal
    ExportModal(
        visible = exportCollection != null,
        title = exportCollection?.name ?: "Collection",
        subtitle = exportCollection?.description?.ifBlank { "Custom Thirukkural Collection" } ?: "Custom Thirukkural Collection",
        kurals = exportKurals,
        onClose = { exportCollection = null }
    )
}

