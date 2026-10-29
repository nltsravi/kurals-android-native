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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nltsravi.kurals.data.KuralRepository
import com.nltsravi.kurals.model.Kural
import com.nltsravi.kurals.model.KuralCollection
import com.nltsravi.kurals.ui.components.EmptyState
import com.nltsravi.kurals.ui.components.ExportModal
import com.nltsravi.kurals.ui.components.KuralCoupletText
import com.nltsravi.kurals.ui.strings.LocalAppTranslations
import com.nltsravi.kurals.ui.theme.LocalAppPalette

@Composable
fun CollectionDetailScreen(
    collection: KuralCollection?,
    isLearningModeEnabled: Boolean,
    onBack: () -> Unit,
    onRenameCollection: (String, String) -> Unit,
    onDeleteCollection: (String) -> Unit,
    onMoveKural: (String, Int, Int) -> Unit,
    onRemoveKuralFromCollection: (String, Int) -> Unit,
    onOpenKuralDetail: (Int, Boolean) -> Unit,
    onShareKural: (Kural) -> Unit
) {
    val palette = LocalAppPalette.current
    val t = LocalAppTranslations.current

    if (collection == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.background),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = t.collectionNotFound, color = palette.text, fontSize = 16.sp)
        }
        return
    }

    val kurals = remember(collection.kuralNumbers) {
        KuralRepository.getKuralsByNumbers(collection.kuralNumbers)
    }

    var isEditingTitle by remember { mutableStateOf(false) }
    var editedTitle by remember(collection.name) { mutableStateOf(collection.name) }
    var showExportModal by remember { mutableStateOf(false) }
    var showDeleteCollectionDialog by remember { mutableStateOf(false) }
    var kuralToRemove by remember { mutableStateOf<Int?>(null) }

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
                    text = collection.name,
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
                    contentDescription = "Export",
                    tint = palette.primary,
                    modifier = Modifier
                        .size(22.dp)
                        .clickable { showExportModal = true }
                )
            }
        }

        // Collection Header Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            shape = RoundedCornerShape(16.dp),
            color = palette.card,
            border = BorderStroke(1.dp, palette.border)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (isEditingTitle) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = editedTitle,
                            onValueChange = { editedTitle = it },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(palette.primary)
                                .clickable {
                                    if (editedTitle.isNotBlank()) {
                                        onRenameCollection(collection.id, editedTitle.trim())
                                    }
                                    isEditingTitle = false
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Save",
                                tint = Color.White
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = collection.name,
                            color = palette.text,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Rename",
                            tint = palette.textSecondary,
                            modifier = Modifier
                                .size(20.dp)
                                .clickable {
                                    editedTitle = collection.name
                                    isEditingTitle = true
                                }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = t.collectionDetailCountSubtitle(kurals.size, collection.description),
                    color = palette.textSecondary,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(palette.primary)
                            .clickable { showExportModal = true }
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = t.collectionDetailExportBtn,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(palette.cardElevated)
                            .border(1.dp, palette.border, RoundedCornerShape(10.dp))
                            .clickable { showDeleteCollectionDialog = true }
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = null,
                            tint = palette.danger,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = t.collectionDetailDeleteBtn,
                            color = palette.danger,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        if (kurals.isEmpty()) {
            EmptyState(
                icon = Icons.Default.Bookmarks,
                title = t.collectionDetailEmptyTitle,
                description = t.collectionDetailEmptyDesc
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
            ) {
                itemsIndexed(kurals, key = { _, k -> k.number }) { index, kural ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = palette.card,
                        border = BorderStroke(1.dp, palette.border)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(palette.badgeBg)
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = "#${kural.number}",
                                            color = palette.badgeText,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        text = "${kural.chapterNameTamil} (${kural.chapterNameEnglish})",
                                        color = palette.textSecondary,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (isLearningModeEnabled) {
                                        Icon(
                                            imageVector = Icons.Default.Mic,
                                            contentDescription = "Practice",
                                            tint = palette.primary,
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clickable { onOpenKuralDetail(kural.number, true) }
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "Share",
                                        tint = palette.primary,
                                        modifier = Modifier
                                            .size(19.dp)
                                            .clickable { onShareKural(kural) }
                                    )
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowUp,
                                        contentDescription = "Move Up",
                                        tint = if (index == 0) palette.textMuted else palette.text,
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clickable(enabled = index > 0) {
                                                onMoveKural(collection.id, index, index - 1)
                                            }
                                    )
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Move Down",
                                        tint = if (index == kurals.lastIndex) palette.textMuted else palette.text,
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clickable(enabled = index < kurals.lastIndex) {
                                                onMoveKural(collection.id, index, index + 1)
                                            }
                                    )
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Remove",
                                        tint = palette.danger,
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clickable { kuralToRemove = kural.number }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenKuralDetail(kural.number, false) }
                            ) {
                                KuralCoupletText(
                                    line1 = kural.line1,
                                    line2 = kural.line2,
                                    color = palette.text,
                                    maxFontSize = 16.5f,
                                    minFontSize = 10.5f,
                                    lineGapDp = 5
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "${kural.transliteration1} / ${kural.transliteration2}",
                                    color = palette.textSecondary,
                                    fontSize = 12.sp,
                                    fontStyle = FontStyle.Italic
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete Collection Confirmation
    if (showDeleteCollectionDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteCollectionDialog = false },
            title = { Text(t.collectionsDeleteTitle, fontWeight = FontWeight.Bold) },
            text = { Text(t.collectionsDeleteConfirmMsg(collection.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteCollectionDialog = false
                        onDeleteCollection(collection.id)
                        onBack()
                    }
                ) {
                    Text(t.collectionsDeleteBtn, color = palette.danger, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteCollectionDialog = false }) {
                    Text(t.collectionsCancelBtn, color = palette.textSecondary)
                }
            }
        )
    }

    // Remove Kural Confirmation
    kuralToRemove?.let { num ->
        AlertDialog(
            onDismissRequest = { kuralToRemove = null },
            title = { Text(t.collectionRemoveKuralTitle, fontWeight = FontWeight.Bold) },
            text = { Text(t.collectionRemoveKuralMsg(num, collection.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRemoveKuralFromCollection(collection.id, num)
                        kuralToRemove = null
                    }
                ) {
                    Text(t.collectionRemoveBtn, color = palette.danger, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { kuralToRemove = null }) {
                    Text(t.collectionsCancelBtn, color = palette.textSecondary)
                }
            }
        )
    }

    ExportModal(
        visible = showExportModal,
        title = collection.name,
        subtitle = collection.description.ifBlank { "Custom Thirukkural Collection" },
        kurals = kurals,
        onClose = { showExportModal = false }
    )
}

