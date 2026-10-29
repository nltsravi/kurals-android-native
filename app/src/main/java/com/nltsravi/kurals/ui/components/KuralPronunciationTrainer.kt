package com.nltsravi.kurals.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.nltsravi.kurals.model.AppLanguage
import com.nltsravi.kurals.model.Kural
import com.nltsravi.kurals.model.PhonemeFeedback
import com.nltsravi.kurals.model.PhonemeStatus
import com.nltsravi.kurals.model.PronunciationResult
import com.nltsravi.kurals.service.OlineriService
import com.nltsravi.kurals.service.TamilVoicePracticeController
import com.nltsravi.kurals.ui.strings.LocalAppLanguage
import com.nltsravi.kurals.ui.strings.LocalAppTranslations
import com.nltsravi.kurals.ui.theme.LocalAppPalette
import com.nltsravi.kurals.util.TextUtils

private enum class PracticeScope {
    FULL, LINE1, LINE2, WORD
}

private data class AksharaClusterGroup(
    val clusterIndex: Int,
    val grapheme: String,
    val dominantStatus: PhonemeStatus,
    val phonemes: List<PhonemeFeedback>
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KuralPronunciationTrainer(
    kural: Kural,
    sdk: OlineriService.PronunciationSDK,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val palette = LocalAppPalette.current
    val t = LocalAppTranslations.current
    val language = LocalAppLanguage.current

    val (line1, line2) = remember(kural.line1, kural.line2) {
        TextUtils.formatKuralCouplet(kural.line1, kural.line2)
    }
    val allWords = remember(line1, line2) {
        "$line1 $line2".split(Regex("\\s+")).filter { it.isNotBlank() }
    }

    var scope by remember(kural.number) { mutableStateOf(PracticeScope.FULL) }
    var selectedWordIndex by remember(kural.number) { mutableIntStateOf(0) }
    var isRecording by remember(kural.number) { mutableStateOf(false) }
    var liveTranscript by remember(kural.number) { mutableStateOf("") }
    var manualSpokenInput by remember(kural.number) { mutableStateOf("") }
    var showManualInput by remember(kural.number) { mutableStateOf(false) }
    var result by remember(kural.number) { mutableStateOf<PronunciationResult?>(null) }
    var selectedClusterIndex by remember(kural.number) { mutableStateOf<Int?>(null) }
    var recordingLevel by remember { mutableFloatStateOf(0.15f) }

    val targetText = remember(scope, selectedWordIndex, line1, line2, allWords) {
        when (scope) {
            PracticeScope.LINE1 -> line1
            PracticeScope.LINE2 -> line2
            PracticeScope.WORD -> allWords.getOrElse(selectedWordIndex) { line1 }
            PracticeScope.FULL -> "$line1 $line2"
        }
    }

    val specialPhonetics = remember(targetText) {
        OlineriService.extractSpecialTamilPhonetics(targetText)
    }

    // Reset result when targetText changes
    LaunchedEffect(targetText) {
        result = null
        selectedClusterIndex = null
        liveTranscript = ""
    }

    val voiceController = remember(context, targetText) {
        TamilVoicePracticeController(
            context = context,
            onLevelChanged = { level -> recordingLevel = level },
            onLiveTranscriptChanged = { text -> liveTranscript = text },
            onFinished = { pcmSamples, recognizedText ->
                isRecording = false
                val spokenToEvaluate = recognizedText.ifBlank { manualSpokenInput.trim() }
                val evaluation = sdk.assessPronunciation(targetText, pcmSamples, spokenToEvaluate)
                result = evaluation
                val firstIssue = evaluation.phonemes.firstOrNull { it.status != PhonemeStatus.CORRECT }
                selectedClusterIndex = firstIssue?.clusterIndex ?: evaluation.phonemes.firstOrNull()?.clusterIndex
            },
            onError = {
                isRecording = false
            }
        )
    }

    DisposableEffect(voiceController) {
        onDispose {
            voiceController.release()
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            result = null
            selectedClusterIndex = null
            isRecording = true
            voiceController.startListening()
        }
    }

    fun startRecordingWithPermissionCheck() {
        val hasPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPerm) {
            result = null
            selectedClusterIndex = null
            isRecording = true
            voiceController.startListening()
        } else {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val clusterGroups = remember(result) {
        val res = result ?: return@remember emptyList()
        val map = LinkedHashMap<Int, MutableList<PhonemeFeedback>>()
        for (p in res.phonemes) {
            map.getOrPut(p.clusterIndex) { mutableListOf() }.add(p)
        }
        map.entries.map { (clusterIdx, items) ->
            var dominant = PhonemeStatus.CORRECT
            for (item in items) {
                if (item.status == PhonemeStatus.SUBSTITUTION) {
                    dominant = PhonemeStatus.SUBSTITUTION
                    break
                } else if (item.status == PhonemeStatus.OMISSION) {
                    dominant = PhonemeStatus.OMISSION
                } else if (item.status == PhonemeStatus.UNCERTAIN && dominant == PhonemeStatus.CORRECT) {
                    dominant = PhonemeStatus.UNCERTAIN
                }
            }
            AksharaClusterGroup(
                clusterIndex = clusterIdx,
                grapheme = items.firstOrNull()?.displayGrapheme ?: "",
                dominantStatus = dominant,
                phonemes = items
            )
        }
    }

    val selectedCluster = remember(clusterGroups, selectedClusterIndex) {
        clusterGroups.find { it.clusterIndex == selectedClusterIndex } ?: clusterGroups.firstOrNull()
    }

    fun statusColor(status: PhonemeStatus): Color {
        return when (status) {
            PhonemeStatus.CORRECT -> palette.success
            PhonemeStatus.SUBSTITUTION -> palette.danger
            PhonemeStatus.OMISSION -> Color(0xFFD97706)
            PhonemeStatus.UNCERTAIN -> Color(0xFFF59E0B)
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        shape = RoundedCornerShape(18.dp),
        color = palette.card,
        border = BorderStroke(1.5.dp, palette.primary)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(palette.primarySoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = palette.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = t.learningTrainerTitle,
                            color = palette.text,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = t.learningTrainerSubtitle,
                            color = palette.textSecondary,
                            fontSize = 11.5.sp
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(palette.badgeBg)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "ta-IN • G2P",
                        color = palette.badgeText,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Scope Tabs (Full Kural / Line 1 / Line 2)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    PracticeScope.FULL to t.learningPracticeFullKural,
                    PracticeScope.LINE1 to t.learningPracticeLine1,
                    PracticeScope.LINE2 to t.learningPracticeLine2
                ).forEach { (sc, label) ->
                    val active = scope == sc
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (active) palette.primary else palette.cardElevated)
                            .border(
                                1.dp,
                                if (active) palette.primary else palette.border,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { scope = sc }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (active) Color.White else palette.textSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 7 Word (சீர்) Selector Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                allWords.forEachIndexed { idx, word ->
                    val isSelected = scope == PracticeScope.WORD && selectedWordIndex == idx
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (isSelected) palette.primarySoft else palette.cardElevated)
                            .border(
                                1.dp,
                                if (isSelected) palette.primary else palette.border,
                                RoundedCornerShape(9.dp)
                            )
                            .clickable {
                                scope = PracticeScope.WORD
                                selectedWordIndex = idx
                            }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "${idx + 1}. $word",
                            color = if (isSelected) palette.primary else palette.textSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Active Target Text Box
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = palette.cardElevated,
                border = BorderStroke(1.dp, palette.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (language == AppLanguage.TA) "பயிற்சிக்குரிய தமிழ் வரி:" else "Target Tamil Text:",
                            color = palette.textMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(palette.surface)
                                .border(1.dp, palette.border, RoundedCornerShape(8.dp))
                                .clickable { showManualInput = !showManualInput }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = palette.primary,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = if (language == AppLanguage.TA) "எழுத்து சோதனை" else "Type Test",
                                color = palette.primary,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = targetText,
                        color = palette.text,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 24.sp
                    )
                }
            }

            // Optional Manual Spoken Text Input Box
            if (showManualInput) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = palette.cardElevated,
                    border = BorderStroke(1.dp, palette.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = if (language == AppLanguage.TA) {
                                "உங்கள் உச்சரிப்பைத் தட்டச்சு செய்து ஒலிநெறி (Olineri) ஆய்வைச் சோதிக்கவும்:"
                            } else {
                                "Type spoken Tamil text to test Olineri phoneme alignment (e.g. try replacing ழ with ல):"
                            },
                            color = palette.textSecondary,
                            fontSize = 11.5.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = manualSpokenInput,
                                onValueChange = { manualSpokenInput = it },
                                placeholder = { Text(targetText, fontSize = 12.sp) },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(9.dp))
                                    .clickable {
                                        val txt = manualSpokenInput.trim().ifEmpty { targetText }
                                        val syntheticPcm = FloatArray(16000) { idx ->
                                            (kotlin.math.sin(idx * 0.14) * 0.25).toFloat()
                                        }
                                        val evaluation = sdk.assessPronunciation(targetText, syntheticPcm, txt)
                                        result = evaluation
                                        val firstIssue = evaluation.phonemes.firstOrNull { it.status != PhonemeStatus.CORRECT }
                                        selectedClusterIndex = firstIssue?.clusterIndex ?: evaluation.phonemes.firstOrNull()?.clusterIndex
                                    },
                                shape = RoundedCornerShape(9.dp),
                                color = palette.primary
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = if (language == AppLanguage.TA) "மதிப்பிடு" else "Check",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Special Tamil Phonetics Spotlight
            if (specialPhonetics.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = t.learningSpecialPhoneticsTitle,
                    color = palette.textSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    specialPhonetics.forEach { item ->
                        Surface(
                            modifier = Modifier.width(215.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = palette.cardElevated,
                            border = BorderStroke(1.dp, palette.border)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(palette.primarySoft),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = item.grapheme,
                                            color = palette.primary,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = if (language == AppLanguage.TA) item.nameTa else item.nameEn,
                                            color = palette.text,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "IPA ${item.ipa}",
                                            color = palette.primary,
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (language == AppLanguage.TA) item.tipTa else item.tipEn,
                                    color = palette.textSecondary,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Primary Microphone Record / Stop Controls + Contrast Simulation Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            if (isRecording) {
                                voiceController.stopAndEvaluate()
                            } else {
                                startRecordingWithPermissionCheck()
                            }
                        },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isRecording) palette.danger else palette.primary
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp, horizontal = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isRecording) t.learningRecordStopBtn else t.learningRecordStartBtn,
                            color = Color.White,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (!isRecording) {
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                val sim = sdk.simulateLearnerChallenge(targetText)
                                result = sim
                                val firstIssue = sim.phonemes.firstOrNull { it.status != PhonemeStatus.CORRECT }
                                selectedClusterIndex = firstIssue?.clusterIndex ?: sim.phonemes.firstOrNull()?.clusterIndex
                            },
                        shape = RoundedCornerShape(12.dp),
                        color = palette.cardElevated,
                        border = BorderStroke(1.dp, palette.primary)
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Science,
                                contentDescription = null,
                                tint = palette.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (language == AppLanguage.TA) "ழ/ல சோதனை" else "Test ழ/ல",
                                color = palette.primary,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Live Recording Meter & Transcript
            if (isRecording) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = palette.cardElevated,
                    border = BorderStroke(1.dp, palette.danger),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(palette.danger)
                            )
                            Text(
                                text = if (language == AppLanguage.TA) {
                                    "🎙️ கவனித்துக் கொண்டிருக்கிறது... குறளைத் தெளிவாக வாசிக்கவும்"
                                } else {
                                    "🎙️ Listening (ta-IN)... Speak the Kural clearly"
                                },
                                color = palette.text,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { recordingLevel.coerceIn(0.1f, 1f) },
                            color = palette.primary,
                            trackColor = palette.border,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                        if (liveTranscript.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "\"$liveTranscript\"",
                                color = palette.textSecondary,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Pronunciation Assessment Result
            result?.let { res ->
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = palette.cardElevated,
                    border = BorderStroke(1.dp, palette.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val scoreColor = if (res.passed) palette.success else palette.primary
                            Box(
                                modifier = Modifier
                                    .size(58.dp)
                                    .clip(CircleShape)
                                    .background(scoreColor.copy(alpha = 0.14f))
                                    .border(2.5.dp, scoreColor, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${res.overallScore}%",
                                    color = scoreColor,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${t.learningScoreLabel}: ${if (res.passed) "✓ PASS" else "↻ PRACTICE"}",
                                    color = palette.text,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (language == AppLanguage.TA) res.summaryMessageTamil else res.summaryMessageEnglish,
                                    color = palette.textSecondary,
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = t.learningTapLetterHint,
                            color = palette.textMuted,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Interactive Akshara Chips
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            clusterGroups.forEach { group ->
                                val stColor = statusColor(group.dominantStatus)
                                val isSelected = selectedCluster?.clusterIndex == group.clusterIndex
                                Column(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(stColor.copy(alpha = if (isSelected) 0.25f else 0.12f))
                                        .border(
                                            if (isSelected) 2.dp else 1.dp,
                                            stColor,
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable { selectedClusterIndex = group.clusterIndex }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = group.grapheme,
                                        color = palette.text,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        text = "/${group.phonemes.joinToString("") { it.expectedIpa }}/",
                                        color = stColor,
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Detailed Phoneme & Articulatory Tongue-Position Card
                        selectedCluster?.let { cluster ->
                            val stColor = statusColor(cluster.dominantStatus)
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = palette.surface,
                                border = BorderStroke(1.5.dp, stColor),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${if (language == AppLanguage.TA) "எழுத்து" else "Letter"}: \"${cluster.grapheme}\"",
                                            color = palette.text,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(stColor.copy(alpha = 0.18f))
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            val badgeLabel = when (cluster.dominantStatus) {
                                                PhonemeStatus.CORRECT -> if (language == AppLanguage.TA) "துல்லியம் ✓" else "Correct ✓"
                                                PhonemeStatus.SUBSTITUTION -> if (language == AppLanguage.TA) "ஒலி மாற்றம்" else "Substituted"
                                                PhonemeStatus.OMISSION -> if (language == AppLanguage.TA) "விடுபட்டது" else "Omitted"
                                                PhonemeStatus.UNCERTAIN -> if (language == AppLanguage.TA) "தெளிவில்லை" else "Uncertain"
                                            }
                                            Text(
                                                text = badgeLabel,
                                                color = stColor,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    HorizontalDivider(color = palette.divider)
                                    Spacer(modifier = Modifier.height(8.dp))

                                    cluster.phonemes.forEachIndexed { pIdx, p ->
                                        if (pIdx > 0) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                        }
                                        Column {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = "${if (language == AppLanguage.TA) "எதிர்பார்த்த ஒலி" else "Expected IPA"}: /${p.expectedIpa}/${if (p.actualIpa != null && p.actualIpa != p.expectedIpa) " → /${p.actualIpa}/" else ""}",
                                                    color = palette.primary,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "GOP ${(p.gopScore * 100).toInt()}%",
                                                    color = palette.textMuted,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = if (language == AppLanguage.TA) p.messageTamil else p.messageEnglish,
                                                color = palette.textSecondary,
                                                fontSize = 12.sp,
                                                lineHeight = 17.sp
                                            )
                                            val tip = if (language == AppLanguage.TA) p.articulatoryTipTamil else p.articulatoryTipEnglish
                                            val mainMsg = if (language == AppLanguage.TA) p.messageTamil else p.messageEnglish
                                            if (!tip.isNullOrBlank() && tip != mainMsg) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(palette.primarySoft)
                                                        .padding(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Info,
                                                        contentDescription = null,
                                                        tint = palette.primary,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Text(
                                                        text = tip,
                                                        color = palette.primary,
                                                        fontSize = 11.5.sp,
                                                        lineHeight = 16.sp,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

