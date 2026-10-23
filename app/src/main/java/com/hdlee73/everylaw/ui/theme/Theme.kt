package com.hdlee73.everylaw.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hdlee73.everylaw.core.model.LawLevel

// 시안 C · 머티리얼 유 (Tonal): M3 baseline 보라 + 계층별 톤 컨테이너
private val Light = lightColorScheme(
    primary = Color(0xFF6750A4), onPrimary = Color.White,
    primaryContainer = Color(0xFFE8DEF8), onPrimaryContainer = Color(0xFF4F378B),
    secondaryContainer = Color(0xFFE8DEF8), onSecondaryContainer = Color(0xFF4F378B),
    background = Color(0xFFFEF7FF), onBackground = Color(0xFF1D1B20),
    surface = Color(0xFFFEF7FF), onSurface = Color(0xFF1D1B20),
    surfaceVariant = Color(0xFFE7E0EC), onSurfaceVariant = Color(0xFF49454F),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF7F2FA),
    surfaceContainer = Color(0xFFF3EDF7),
    surfaceContainerHigh = Color(0xFFECE6F0),
    surfaceContainerHighest = Color(0xFFE6E0E9),
    outline = Color(0xFF79747E), outlineVariant = Color(0xFFCAC4D0),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFD0BCFF), onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B), onPrimaryContainer = Color(0xFFEADDFF),
    secondaryContainer = Color(0xFF4A4458), onSecondaryContainer = Color(0xFFE8DEF8),
    background = Color(0xFF141218), onBackground = Color(0xFFE6E0E9),
    surface = Color(0xFF141218), onSurface = Color(0xFFE6E0E9),
    surfaceVariant = Color(0xFF49454F), onSurfaceVariant = Color(0xFFCAC4D0),
    surfaceContainerLowest = Color(0xFF0F0D13),
    surfaceContainerLow = Color(0xFF1D1B20),
    surfaceContainer = Color(0xFF211F26),
    surfaceContainerHigh = Color(0xFF2B2930),
    surfaceContainerHighest = Color(0xFF36343B),
    outline = Color(0xFF938F99), outlineVariant = Color(0xFF49454F),
)

/** 계층 하나의 톤: 카드 배경(container), 카드 글자(onContainer), 배지(accent/onAccent) */
@Immutable
data class LevelTone(val container: Color, val onContainer: Color, val accent: Color, val onAccent: Color)

@Immutable
data class LevelPalette(private val tones: Map<LawLevel, LevelTone>) {
    operator fun get(level: LawLevel): LevelTone = tones.getValue(level)
}

private val LightLevels = LevelPalette(
    mapOf(
        LawLevel.LAW to LevelTone(Color(0xFFE8DEF8), Color(0xFF381E72), Color(0xFF6750A4), Color.White),
        LawLevel.DECREE to LevelTone(Color(0xFFD3E4FF), Color(0xFF0A3A6B), Color(0xFF0B5CAD), Color.White),
        LawLevel.RULE to LevelTone(Color(0xFFC9F0E0), Color(0xFF07402F), Color(0xFF0F6B50), Color.White),
        LawLevel.SUPERVISORY_REGULATION to LevelTone(Color(0xFFFFE0CC), Color(0xFF5C2600), Color(0xFF9A4400), Color.White),
        LawLevel.ENFORCEMENT_DETAIL to LevelTone(Color(0xFFFFD8E4), Color(0xFF5E0A2B), Color(0xFF9C2452), Color.White),
    )
)

private val DarkLevels = LevelPalette(
    mapOf(
        LawLevel.LAW to LevelTone(Color(0xFF3B2E5A), Color(0xFFEADDFF), Color(0xFFD0BCFF), Color(0xFF381E72)),
        LawLevel.DECREE to LevelTone(Color(0xFF173A5E), Color(0xFFD3E4FF), Color(0xFFA4C9FF), Color(0xFF00315D)),
        LawLevel.RULE to LevelTone(Color(0xFF113E31), Color(0xFFC9F0E0), Color(0xFF8DD7B8), Color(0xFF003828)),
        LawLevel.SUPERVISORY_REGULATION to LevelTone(Color(0xFF4A2A12), Color(0xFFFFE0CC), Color(0xFFFFB68A), Color(0xFF532200)),
        LawLevel.ENFORCEMENT_DETAIL to LevelTone(Color(0xFF4D1F30), Color(0xFFFFD8E4), Color(0xFFFFB0C8), Color(0xFF5E1133)),
    )
)

val LocalLevelPalette = staticCompositionLocalOf { LightLevels }

private val AppTypography = Typography().run {
    copy(
        headlineMedium = headlineMedium.copy(fontWeight = FontWeight.Black),
        headlineSmall = headlineSmall.copy(fontWeight = FontWeight.Black),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.Bold),
        bodyMedium = bodyMedium.copy(lineHeight = 22.sp),
        bodyLarge = bodyLarge.copy(lineHeight = 26.sp),
    )
}

private val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

val LawBodyStyle = TextStyle(fontSize = 14.sp, lineHeight = 24.sp)

@Composable
fun EverylawTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    androidx.compose.runtime.CompositionLocalProvider(LocalLevelPalette provides if (darkTheme) DarkLevels else LightLevels) {
        MaterialTheme(
            colorScheme = if (darkTheme) Dark else Light,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}

object LevelColors {
    val current: LevelPalette @Composable get() = LocalLevelPalette.current
}
