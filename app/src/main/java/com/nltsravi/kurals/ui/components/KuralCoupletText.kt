package com.nltsravi.kurals.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nltsravi.kurals.util.TextUtils
import kotlin.math.roundToInt

/**
 * Renders a 2-line Thirukkural couplet with:
 * - Line 1: 4 words (சீர்கள்)
 * - Line 2: 3 words (சீர்கள்)
 * - Identical font size on both lines
 * - Never wraps onto a 3rd line and never truncates with "..."
 */
@Composable
fun KuralCoupletText(
    line1: String,
    line2: String,
    color: Color,
    modifier: Modifier = Modifier,
    maxFontSize: Float = 17f,
    minFontSize: Float = 10.5f,
    lineGapDp: Int = 6,
    fontWeight: FontWeight = FontWeight.Bold,
    centerAlign: Boolean = false
) {
    val (formattedLine1, formattedLine2) = remember(line1, line2) {
        TextUtils.formatKuralCouplet(line1, line2)
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val density = LocalDensity.current
        val availableWidthDp = with(density) { constraints.maxWidth.toDp().value }
        val uniformFontSize = remember(formattedLine1, formattedLine2, availableWidthDp, maxFontSize, minFontSize) {
            TextUtils.calculateCoupletFontSize(
                line1 = formattedLine1,
                line2 = formattedLine2,
                availableWidth = availableWidthDp,
                maxFontSize = maxFontSize,
                minFontSize = minFontSize
            )
        }
        val lineHeightSp = (uniformFontSize * 1.48f).roundToInt().sp

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(lineGapDp.dp),
            horizontalAlignment = if (centerAlign) Alignment.CenterHorizontally else Alignment.Start
        ) {
            Text(
                text = formattedLine1,
                color = color,
                fontSize = uniformFontSize.sp,
                lineHeight = lineHeightSp,
                fontWeight = fontWeight,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Visible,
                textAlign = if (centerAlign) TextAlign.Center else TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = formattedLine2,
                color = color,
                fontSize = uniformFontSize.sp,
                lineHeight = lineHeightSp,
                fontWeight = fontWeight,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Visible,
                textAlign = if (centerAlign) TextAlign.Center else TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

