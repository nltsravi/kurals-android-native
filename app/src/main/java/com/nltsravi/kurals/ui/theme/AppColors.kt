package com.nltsravi.kurals.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.nltsravi.kurals.model.ThemeMode

data class AppPalette(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val card: Color,
    val cardElevated: Color,
    val text: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val primary: Color,
    val primarySoft: Color,
    val accent: Color,
    val border: Color,
    val divider: Color,
    val badgeBg: Color,
    val badgeText: Color,
    val tabIconDefault: Color,
    val tabIconSelected: Color,
    val danger: Color,
    val success: Color
)

data class CategoryColorScheme(
    val bg: Color,
    val text: Color,
    val border: Color
)

object KuralsPalettes {
    val Light = AppPalette(
        isDark = false,
        background = Color(0xFFFFF2E5),
        surface = Color(0xFFFFFFFF),
        card = Color(0xFFFFFFFF),
        cardElevated = Color(0xFFFFE5CC),
        text = Color(0xFF2B1605),
        textSecondary = Color(0xFF5C3412),
        textMuted = Color(0xFF8C5322),
        primary = Color(0xFFFF7C0A),
        primarySoft = Color(0xFFFFD8B3),
        accent = Color(0xFFB45309),
        border = Color(0xFFFFCFA3),
        divider = Color(0xFFFFDEC2),
        badgeBg = Color(0xFFFFDFC2),
        badgeText = Color(0xFF9A3412),
        tabIconDefault = Color(0xFF8C5322),
        tabIconSelected = Color(0xFFFF7C0A),
        danger = Color(0xFFDC2626),
        success = Color(0xFF059669)
    )

    val Dark = AppPalette(
        isDark = true,
        background = Color(0xFF261405),
        surface = Color(0xFF381F0A),
        card = Color(0xFF381F0A),
        cardElevated = Color(0xFF4A2A0F),
        text = Color(0xFFFFF5EB),
        textSecondary = Color(0xFFFFCFA3),
        textMuted = Color(0xFFC28246),
        primary = Color(0xFFFF8D29),
        primarySoft = Color(0xFF5C320E),
        accent = Color(0xFFF59E0B),
        border = Color(0xFF5C3412),
        divider = Color(0xFF4A2A0F),
        badgeBg = Color(0xFF5C320E),
        badgeText = Color(0xFFFFB870),
        tabIconDefault = Color(0xFFA86A30),
        tabIconSelected = Color(0xFFFF8D29),
        danger = Color(0xFFEF4444),
        success = Color(0xFF10B981)
    )

    fun categoryColors(categoryId: Int, isDark: Boolean): CategoryColorScheme {
        return if (!isDark) {
            when (categoryId) {
                1 -> CategoryColorScheme(
                    bg = Color(0xFFFEF3C7),
                    text = Color(0xFFB45309),
                    border = Color(0xFFFCD34D)
                )
                2 -> CategoryColorScheme(
                    bg = Color(0xFFDBEAFE),
                    text = Color(0xFF1D4ED8),
                    border = Color(0xFF93C5FD)
                )
                else -> CategoryColorScheme(
                    bg = Color(0xFFFCE7F3),
                    text = Color(0xFFBE185D),
                    border = Color(0xFFF9A8D4)
                )
            }
        } else {
            when (categoryId) {
                1 -> CategoryColorScheme(
                    bg = Color(0xFF3D2607),
                    text = Color(0xFFFBBF24),
                    border = Color(0xFF78350F)
                )
                2 -> CategoryColorScheme(
                    bg = Color(0xFF172554),
                    text = Color(0xFF60A5FA),
                    border = Color(0xFF1E3A8A)
                )
                else -> CategoryColorScheme(
                    bg = Color(0xFF3B0764),
                    text = Color(0xFFF472B6),
                    border = Color(0xFF831843)
                )
            }
        }
    }

    fun parseHexColor(hex: String, fallback: Color = Color(0xFFFF7C0A)): Color {
        return try {
            Color(android.graphics.Color.parseColor(hex))
        } catch (_: Exception) {
            fallback
        }
    }
}

val LocalAppPalette = staticCompositionLocalOf { KuralsPalettes.Light }

@Composable
fun KuralsTheme(
    themeMode: ThemeMode,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val useDark = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }
    val palette = if (useDark) KuralsPalettes.Dark else KuralsPalettes.Light

    val colorScheme = if (useDark) {
        darkColorScheme(
            primary = palette.primary,
            secondary = palette.accent,
            background = palette.background,
            surface = palette.surface,
            onPrimary = Color.White,
            onBackground = palette.text,
            onSurface = palette.text,
            error = palette.danger
        )
    } else {
        lightColorScheme(
            primary = palette.primary,
            secondary = palette.accent,
            background = palette.background,
            surface = palette.surface,
            onPrimary = Color.White,
            onBackground = palette.text,
            onSurface = palette.text,
            error = palette.danger
        )
    }

    CompositionLocalProvider(LocalAppPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}

