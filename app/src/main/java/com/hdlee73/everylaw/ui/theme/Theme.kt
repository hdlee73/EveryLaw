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
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hdlee73.everylaw.R
import com.hdlee73.everylaw.core.model.LawLevel

// 시안 2 · 네이비 프로: 남색(주색) + 금색(포인트) + 링크 블루. 카드는 흰색, 바탕은 옅은 청회색.
val Navy = Color(0xFF0B1B3A)
val Gold = Color(0xFFC8A24A)
val GoldTint = Color(0xFFF6EEDB)
val LinkBlue = Color(0xFF2F5BD3)

private val Light = lightColorScheme(
    primary = Navy, onPrimary = Color.White,
    primaryContainer = GoldTint, onPrimaryContainer = Navy,
    secondary = Gold, onSecondary = Navy,
    secondaryContainer = GoldTint, onSecondaryContainer = Color(0xFF3F3010),
    tertiary = LinkBlue, onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE7ECF8), onTertiaryContainer = Navy,
    background = Color(0xFFF4F6FA), onBackground = Navy,
    surface = Color(0xFFF4F6FA), onSurface = Navy,
    surfaceVariant = Color(0xFFE7ECF8), onSurfaceVariant = Color(0xFF56627A),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color(0xFFEDF0F6),
    surfaceContainerHighest = Color(0xFFE7ECF8),
    outline = Color(0xFF8994AA), outlineVariant = Color(0xFFD6DDEA),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFE9CF8E), onPrimary = Navy,
    primaryContainer = Color(0xFF3A3220), onPrimaryContainer = Color(0xFFF6EEDB),
    secondary = Gold, onSecondary = Navy,
    secondaryContainer = Color(0xFF3A3220), onSecondaryContainer = Color(0xFFF6EEDB),
    tertiary = Color(0xFF9DB6FF), onTertiary = Color(0xFF0A1F5C),
    tertiaryContainer = Color(0xFF1C2A4B), onTertiaryContainer = Color(0xFFDCE4FF),
    background = Color(0xFF0A1226), onBackground = Color(0xFFE3E8F5),
    surface = Color(0xFF0A1226), onSurface = Color(0xFFE3E8F5),
    surfaceVariant = Color(0xFF1C2A4B), onSurfaceVariant = Color(0xFFB9C3D9),
    surfaceContainerLowest = Color(0xFF070D1C),
    surfaceContainerLow = Color(0xFF0F1A33),
    surfaceContainer = Color(0xFF121F3B),
    surfaceContainerHigh = Color(0xFF182848),
    surfaceContainerHighest = Color(0xFF1F3158),
    outline = Color(0xFF7C88A3), outlineVariant = Color(0xFF2A3A5E),
)

/** 계층 하나의 톤: 카드 배경(container), 카드 글자(onContainer), 배지·단계 번호(accent/onAccent) */
@Immutable
data class LevelTone(val container: Color, val onContainer: Color, val accent: Color, val onAccent: Color)

@Immutable
data class LevelPalette(private val tones: Map<LawLevel, LevelTone>) {
    operator fun get(level: LawLevel): LevelTone = tones.getValue(level)
}

private val LightLevels = LevelPalette(
    mapOf(
        LawLevel.LAW to LevelTone(Color(0xFFE7ECF8), Navy, Navy, Color.White),
        LawLevel.DECREE to LevelTone(Color(0xFFE7ECF8), Navy, LinkBlue, Color.White),
        LawLevel.RULE to LevelTone(Color(0xFFE3F4F0), Color(0xFF0B3D35), Color(0xFF0F7B6C), Color.White),
        LawLevel.SUPERVISORY_REGULATION to LevelTone(GoldTint, Color(0xFF3F3010), Gold, Navy),
        LawLevel.ENFORCEMENT_DETAIL to LevelTone(Color(0xFFE3E8F1), Navy, Color(0xFF56627A), Color.White),
    )
)

private val DarkLevels = LevelPalette(
    mapOf(
        LawLevel.LAW to LevelTone(Color(0xFF1C2A4B), Color(0xFFE3E8F5), Color(0xFFE3E8F5), Navy),
        LawLevel.DECREE to LevelTone(Color(0xFF1C2A4B), Color(0xFFE3E8F5), Color(0xFF9DB6FF), Color(0xFF0A1F5C)),
        LawLevel.RULE to LevelTone(Color(0xFF123A33), Color(0xFFD3F1EA), Color(0xFF6FD0BF), Color(0xFF00382F)),
        LawLevel.SUPERVISORY_REGULATION to LevelTone(Color(0xFF3A3220), Color(0xFFF6EEDB), Gold, Navy),
        LawLevel.ENFORCEMENT_DETAIL to LevelTone(Color(0xFF243350), Color(0xFFD5DCEC), Color(0xFFA8B3CC), Navy),
    )
)

val LocalLevelPalette = staticCompositionLocalOf { LightLevels }

/** IBM Plex Sans KR (SIL OFL). 보통·굵게만 넣고 중간 굵기는 가장 가까운 쪽을 쓴다. */
val PlexKr = FontFamily(
    Font(R.font.ibm_plex_sans_kr_regular, FontWeight.Normal),
    Font(R.font.ibm_plex_sans_kr_regular, FontWeight.Medium),
    Font(R.font.ibm_plex_sans_kr_bold, FontWeight.SemiBold),
    Font(R.font.ibm_plex_sans_kr_bold, FontWeight.Bold),
)

private val AppTypography = Typography().run {
    fun TextStyle.plex(weight: FontWeight? = null) = copy(fontFamily = PlexKr, fontWeight = weight ?: fontWeight)
    copy(
        displayLarge = displayLarge.plex(), displayMedium = displayMedium.plex(), displaySmall = displaySmall.plex(),
        headlineLarge = headlineLarge.plex(FontWeight.Bold),
        headlineMedium = headlineMedium.plex(FontWeight.Bold),
        headlineSmall = headlineSmall.plex(FontWeight.Bold),
        titleLarge = titleLarge.plex(FontWeight.Bold),
        titleMedium = titleMedium.plex(FontWeight.Bold),
        titleSmall = titleSmall.plex(FontWeight.SemiBold),
        bodyLarge = bodyLarge.plex().copy(lineHeight = 26.sp),
        bodyMedium = bodyMedium.plex().copy(lineHeight = 22.sp),
        bodySmall = bodySmall.plex(),
        labelLarge = labelLarge.plex(FontWeight.SemiBold),
        labelMedium = labelMedium.plex(FontWeight.SemiBold),
        labelSmall = labelSmall.plex(),
    )
}

// 시안: 카드 16dp, 칩·태그 8dp 안팎의 단정한 모서리
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

val LawBodyStyle = TextStyle(fontFamily = PlexKr, fontSize = 14.sp, lineHeight = 24.sp)

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
