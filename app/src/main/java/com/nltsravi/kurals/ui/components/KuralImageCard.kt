package com.nltsravi.kurals.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nltsravi.kurals.constants.AppConstants
import com.nltsravi.kurals.model.CardAspectRatio
import com.nltsravi.kurals.model.CardImageTheme
import com.nltsravi.kurals.model.Kural
import com.nltsravi.kurals.model.MeaningType
import com.nltsravi.kurals.util.AssetImageLoader
import com.nltsravi.kurals.util.TextUtils
import com.nltsravi.kurals.util.rememberAssetImageBitmap
import com.nltsravi.kurals.util.rememberUriImageBitmap

data class MeaningBlock(
    val title: String,
    val primaryText: String,
    val secondaryText: String? = null
)

fun resolveMeaningBlock(kural: Kural, meaningType: MeaningType): MeaningBlock {
    return when (meaningType) {
        MeaningType.MV -> MeaningBlock("மு.வரதராசனார் உரை :", kural.mv)
        MeaningType.SP -> MeaningBlock("சாலமன் பாப்பையா உரை :", kural.sp)
        MeaningType.MK -> MeaningBlock("மு.கருணாநிதி உரை :", kural.mk)
        MeaningType.PRM -> MeaningBlock("பரிமேலழகர் உரை :", kural.prm)
        MeaningType.MNK -> MeaningBlock("மணக்குடவர் உரை :", kural.mnk)
        MeaningType.TRANSLATION -> MeaningBlock("English Meaning :", kural.translation)
        MeaningType.BOTH -> MeaningBlock("பொருள் / Meaning :", kural.mv, kural.translation)
    }
}

/**
 * Live Compose preview of the Thirukkural Status / Social Card
 * matching `KuralImageCard.tsx` (both arch portrait template & no-image velvet plaque template).
 */
@Composable
fun KuralImageCard(
    kural: Kural,
    meaningType: MeaningType,
    aspectRatio: CardAspectRatio,
    allowModifyCardImage: Boolean,
    selectedCardImageId: String,
    imageTheme: CardImageTheme = CardImageTheme.PORTRAIT,
    customImageUri: String? = null,
    modifier: Modifier = Modifier
) {
    val (line1, line2) = remember(kural.line1, kural.line2) {
        TextUtils.formatKuralCouplet(kural.line1, kural.line2)
    }
    val meaning = remember(kural, meaningType) {
        resolveMeaningBlock(kural, meaningType)
    }

    val isNoImageMode = !allowModifyCardImage ||
        imageTheme == CardImageTheme.NONE ||
        (!allowModifyCardImage && selectedCardImageId == "hide")

    val cardWidthDp = 320.dp
    val cardHeightDp = when (aspectRatio) {
        CardAspectRatio.RATIO_9_16 -> 568.dp
        CardAspectRatio.RATIO_4_5 -> 400.dp
        CardAspectRatio.RATIO_1_1 -> 320.dp
    }

    val templateAsset = if (isNoImageMode) {
        "images/templates/traditional_template_no_image_bg.png"
    } else {
        "images/templates/traditional_template_bg.png"
    }
    val templateBitmap = rememberAssetImageBitmap(templateAsset)
    val brandIconBitmap = rememberAssetImageBitmap("images/thalir_kalvikkoodam_icon.png")

    val selectedPortraitOption = remember(selectedCardImageId) {
        AppConstants.getThiruvalluvarImageById(selectedCardImageId)
    }
    val portraitBitmap = rememberAssetImageBitmap(selectedPortraitOption.assetPath)
    val customBitmap = rememberUriImageBitmap(customImageUri)

    val archSizeDp = when (aspectRatio) {
        CardAspectRatio.RATIO_9_16 -> 118.dp
        CardAspectRatio.RATIO_4_5 -> 92.dp
        CardAspectRatio.RATIO_1_1 -> 74.dp
    }
    val archTopOffsetDp = when (aspectRatio) {
        CardAspectRatio.RATIO_9_16 -> 84.dp
        CardAspectRatio.RATIO_4_5 -> 58.dp
        CardAspectRatio.RATIO_1_1 -> 44.dp
    }

    Box(
        modifier = modifier
            .width(cardWidthDp)
            .height(cardHeightDp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0A3828))
    ) {
        if (templateBitmap != null) {
            Image(
                bitmap = templateBitmap,
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Portrait inside Golden Arch (when image mode is active)
        if (!isNoImageMode) {
            val activePortrait = if (selectedCardImageId == "custom" && customBitmap != null) {
                customBitmap
            } else {
                portraitBitmap
            }
            if (activePortrait != null) {
                Box(
                    modifier = Modifier
                        .size(archSizeDp)
                        .align(Alignment.TopCenter)
                        .offset(y = archTopOffsetDp)
                        .clip(
                            RoundedCornerShape(
                                topStart = archSizeDp / 2,
                                topEnd = archSizeDp / 2,
                                bottomStart = 8.dp,
                                bottomEnd = 8.dp
                            )
                        )
                ) {
                    Image(
                        bitmap = activePortrait,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // Foreground Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = when (aspectRatio) {
                        CardAspectRatio.RATIO_9_16 -> 26.dp
                        CardAspectRatio.RATIO_4_5 -> 20.dp
                        CardAspectRatio.RATIO_1_1 -> 16.dp
                    },
                    bottom = when (aspectRatio) {
                        CardAspectRatio.RATIO_9_16 -> 34.dp
                        CardAspectRatio.RATIO_4_5 -> 26.dp
                        CardAspectRatio.RATIO_1_1 -> 22.dp
                    },
                    start = 24.dp,
                    end = 24.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top bar: Thalir Kalvikkoodam icon + Kural Number badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (brandIconBitmap != null) {
                    Image(
                        bitmap = brandIconBitmap,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(if (aspectRatio == CardAspectRatio.RATIO_1_1) 34.dp else 42.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                } else {
                    Spacer(modifier = Modifier.size(34.dp))
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xB80A2E1F))
                        .border(1.dp, Color(0x99D4AF37), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "குறள் ${kural.number}",
                        color = Color(0xFFFFE89E),
                        fontSize = if (aspectRatio == CardAspectRatio.RATIO_1_1) 11.sp else 13.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            if (!isNoImageMode) {
                Spacer(
                    modifier = Modifier.height(
                        when (aspectRatio) {
                            CardAspectRatio.RATIO_9_16 -> 150.dp
                            CardAspectRatio.RATIO_4_5 -> 110.dp
                            CardAspectRatio.RATIO_1_1 -> 82.dp
                        }
                    )
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    KuralCoupletText(
                        line1 = line1,
                        line2 = line2,
                        color = Color(0xFF1F1206),
                        maxFontSize = if (aspectRatio == CardAspectRatio.RATIO_9_16) 13.5f else 11.5f,
                        minFontSize = 8.5f,
                        lineGapDp = 4,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            } else {
                Spacer(
                    modifier = Modifier.height(
                        when (aspectRatio) {
                            CardAspectRatio.RATIO_9_16 -> 26.dp
                            CardAspectRatio.RATIO_4_5 -> 16.dp
                            CardAspectRatio.RATIO_1_1 -> 10.dp
                        }
                    )
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF0F2D1F))
                        .border(1.5.dp, Color(0xFFD4AF37), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 12.dp)
                ) {
                    KuralCoupletText(
                        line1 = line1,
                        line2 = line2,
                        color = Color(0xFFFFF8E7),
                        maxFontSize = if (aspectRatio == CardAspectRatio.RATIO_9_16) 14f else 12f,
                        minFontSize = 9f,
                        lineGapDp = 6,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Meaning section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                Text(
                    text = meaning.title,
                    color = Color(0xFF8B1E0F),
                    fontSize = if (aspectRatio == CardAspectRatio.RATIO_1_1) 10.sp else 11.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = meaning.primaryText,
                    color = Color(0xFF1A1108),
                    fontSize = if (aspectRatio == CardAspectRatio.RATIO_9_16) 11.sp else 9.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = if (aspectRatio == CardAspectRatio.RATIO_9_16) 16.sp else 13.5.sp,
                    textAlign = TextAlign.Center,
                    maxLines = if (aspectRatio == CardAspectRatio.RATIO_9_16) 7 else 4,
                    overflow = TextOverflow.Ellipsis
                )
                if (!meaning.secondaryText.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = meaning.secondaryText,
                        color = Color(0xFF3A2410),
                        fontSize = if (aspectRatio == CardAspectRatio.RATIO_9_16) 9.5.sp else 8.5.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 13.sp,
                        textAlign = TextAlign.Center,
                        maxLines = if (aspectRatio == CardAspectRatio.RATIO_9_16) 4 else 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * Renders the high-resolution (1080px wide) PNG Bitmap for sharing to WhatsApp Status, Instagram, X, Threads, Facebook, etc.
 */
object KuralCardBitmapRenderer {

    fun renderBitmap(
        context: Context,
        kural: Kural,
        meaningType: MeaningType,
        aspectRatio: CardAspectRatio,
        allowModifyCardImage: Boolean,
        selectedCardImageId: String,
        imageTheme: CardImageTheme = CardImageTheme.PORTRAIT,
        customImageUri: String? = null
    ): Bitmap {
        val width = 1080
        val height = when (aspectRatio) {
            CardAspectRatio.RATIO_9_16 -> 1920
            CardAspectRatio.RATIO_4_5 -> 1350
            CardAspectRatio.RATIO_1_1 -> 1080
        }

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(AndroidColor.parseColor("#0A3828"))

        val isNoImageMode = !allowModifyCardImage ||
            imageTheme == CardImageTheme.NONE ||
            (!allowModifyCardImage && selectedCardImageId == "hide")

        val templateAsset = if (isNoImageMode) {
            "images/templates/traditional_template_no_image_bg.png"
        } else {
            "images/templates/traditional_template_bg.png"
        }
        AssetImageLoader.loadAssetBitmap(context, templateAsset)?.let { bg ->
            canvas.drawBitmap(bg, null, Rect(0, 0, width, height), null)
        }

        // Portrait inside Golden Arch
        if (!isNoImageMode) {
            val portraitBmp = if (selectedCardImageId == "custom" && !customImageUri.isNullOrBlank()) {
                AssetImageLoader.loadUriBitmap(context, customImageUri)
            } else {
                val option = AppConstants.getThiruvalluvarImageById(selectedCardImageId)
                AssetImageLoader.loadAssetBitmap(context, option.assetPath)
            }
            if (portraitBmp != null) {
                val archSize = when (aspectRatio) {
                    CardAspectRatio.RATIO_9_16 -> 400f
                    CardAspectRatio.RATIO_4_5 -> 310f
                    CardAspectRatio.RATIO_1_1 -> 250f
                }
                val archLeft = (width - archSize) / 2f
                val archTop = when (aspectRatio) {
                    CardAspectRatio.RATIO_9_16 -> 284f
                    CardAspectRatio.RATIO_4_5 -> 196f
                    CardAspectRatio.RATIO_1_1 -> 148f
                }
                val archRect = RectF(archLeft, archTop, archLeft + archSize, archTop + archSize)

                val saveCount = canvas.save()
                val clipPath = Path().apply {
                    val radii = floatArrayOf(
                        archSize / 2f, archSize / 2f,
                        archSize / 2f, archSize / 2f,
                        28f, 28f,
                        28f, 28f
                    )
                    addRoundRect(archRect, radii, Path.Direction.CW)
                }
                canvas.clipPath(clipPath)
                canvas.drawBitmap(portraitBmp, null, archRect, null)
                canvas.restoreToCount(saveCount)
            }
        }

        // Top Bar: Brand Icon + Kural Badge
        val padH = 82f
        val topY = when (aspectRatio) {
            CardAspectRatio.RATIO_9_16 -> 92f
            CardAspectRatio.RATIO_4_5 -> 72f
            CardAspectRatio.RATIO_1_1 -> 58f
        }
        AssetImageLoader.loadAssetBitmap(context, "images/thalir_kalvikkoodam_icon.png")?.let { icon ->
            val iconSize = if (aspectRatio == CardAspectRatio.RATIO_1_1) 115f else 140f
            canvas.drawBitmap(icon, null, RectF(padH, topY, padH + iconSize, topY + iconSize), null)
        }

        val badgeText = "குறள் ${kural.number}"
        val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#FFE89E")
            textSize = if (aspectRatio == CardAspectRatio.RATIO_1_1) 36f else 42f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val badgeW = badgeTextPaint.measureText(badgeText) + 56f
        val badgeH = 74f
        val badgeLeft = width - padH - badgeW
        val badgeRect = RectF(badgeLeft, topY + 18f, width - padH, topY + 18f + badgeH)
        val badgeFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#B80A2E1F")
            style = Paint.Style.FILL
        }
        val badgeStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#99D4AF37")
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawRoundRect(badgeRect, 22f, 22f, badgeFill)
        canvas.drawRoundRect(badgeRect, 22f, 22f, badgeStroke)
        canvas.drawText(badgeText, badgeLeft + 28f, badgeRect.centerY() + 14f, badgeTextPaint)

        // Couplet
        val (line1, line2) = TextUtils.formatKuralCouplet(kural.line1, kural.line2)
        val coupletPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val maxCoupletWidth = width - padH * 2 - 48f
        var coupletFontSize = if (aspectRatio == CardAspectRatio.RATIO_9_16) 44f else 38f
        coupletPaint.textSize = coupletFontSize
        while (coupletFontSize > 24f &&
            (coupletPaint.measureText(line1) > maxCoupletWidth || coupletPaint.measureText(line2) > maxCoupletWidth)
        ) {
            coupletFontSize -= 1.5f
            coupletPaint.textSize = coupletFontSize
        }

        var currentY: Float
        if (isNoImageMode) {
            val boxTop = when (aspectRatio) {
                CardAspectRatio.RATIO_9_16 -> 320f
                CardAspectRatio.RATIO_4_5 -> 240f
                CardAspectRatio.RATIO_1_1 -> 195f
            }
            val boxHeight = coupletFontSize * 3.8f
            val boxRect = RectF(padH, boxTop, width - padH, boxTop + boxHeight)
            val boxFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = AndroidColor.parseColor("#0F2D1F")
                style = Paint.Style.FILL
            }
            val boxStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = AndroidColor.parseColor("#D4AF37")
                style = Paint.Style.STROKE
                strokeWidth = 5f
            }
            canvas.drawRoundRect(boxRect, 30f, 30f, boxFill)
            canvas.drawRoundRect(boxRect, 30f, 30f, boxStroke)

            coupletPaint.color = AndroidColor.parseColor("#FFF8E7")
            canvas.drawText(line1, padH + 28f, boxTop + coupletFontSize * 1.45f, coupletPaint)
            canvas.drawText(line2, padH + 28f, boxTop + coupletFontSize * 2.85f, coupletPaint)
            currentY = boxRect.bottom + 64f
        } else {
            val coupletTop = when (aspectRatio) {
                CardAspectRatio.RATIO_9_16 -> 745f
                CardAspectRatio.RATIO_4_5 -> 560f
                CardAspectRatio.RATIO_1_1 -> 445f
            }
            coupletPaint.color = AndroidColor.parseColor("#1F1206")
            canvas.drawText(line1, padH + 16f, coupletTop, coupletPaint)
            canvas.drawText(line2, padH + 16f, coupletTop + coupletFontSize * 1.45f, coupletPaint)
            currentY = coupletTop + coupletFontSize * 2.5f
        }

        // Meaning section
        val meaning = resolveMeaningBlock(kural, meaningType)
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#8B1E0F")
            textSize = if (aspectRatio == CardAspectRatio.RATIO_1_1) 34f else 38f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(meaning.title, width / 2f, currentY, titlePaint)
        currentY += 32f

        val bodyTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#1A1108")
            textSize = if (aspectRatio == CardAspectRatio.RATIO_9_16) 36f else 31f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val textBlockWidth = (width - padH * 2 - 24f).toInt()
        val primaryLayout = StaticLayout.Builder
            .obtain(meaning.primaryText, 0, meaning.primaryText.length, bodyTextPaint, textBlockWidth)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(8f, 1.15f)
            .setMaxLines(if (aspectRatio == CardAspectRatio.RATIO_9_16) 8 else 5)
            .build()

        canvas.save()
        canvas.translate((width - textBlockWidth) / 2f, currentY)
        primaryLayout.draw(canvas)
        canvas.restore()
        currentY += primaryLayout.height + 24f

        if (!meaning.secondaryText.isNullOrBlank()) {
            val secTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = AndroidColor.parseColor("#3A2410")
                textSize = if (aspectRatio == CardAspectRatio.RATIO_9_16) 31f else 27f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }
            val secLayout = StaticLayout.Builder
                .obtain(meaning.secondaryText, 0, meaning.secondaryText.length, secTextPaint, textBlockWidth)
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setLineSpacing(6f, 1.12f)
                .setMaxLines(if (aspectRatio == CardAspectRatio.RATIO_9_16) 5 else 3)
                .build()

            canvas.save()
            canvas.translate((width - textBlockWidth) / 2f, currentY)
            secLayout.draw(canvas)
            canvas.restore()
        }

        return bitmap
    }
}

