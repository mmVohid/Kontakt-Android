package uz.seyf.kontakt.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/*
 * "Yumshoq feruza" palitra: sokin ko'k-yashil asosiy rang, lavanda aksent,
 * pastel konteynerlar va oq/yarim shaffof kartalar.
 * Dinamik rang (Material You) ataylab ishlatilmaydi: brend ko'rinishi barcha telefonlarda bir xil bo'lsin.
 */
private val LightScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFF2B7A86),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD2F0F4),
    onPrimaryContainer = Color(0xFF00363D),
    secondary = Color(0xFF5560A8),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE5E8FB),
    onSecondaryContainer = Color(0xFF1B2152),
    tertiary = Color(0xFF8A68B8),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF0E5FA),
    onTertiaryContainer = Color(0xFF2E1650),
    background = Color(0xFFF2F8F9),
    onBackground = Color(0xFF172326),
    surface = Color(0xFFF2F8F9),
    onSurface = Color(0xFF172326),
    surfaceVariant = Color(0xFFDDE8EA),
    onSurfaceVariant = Color(0xFF47585B),
    outline = Color(0xFF77898C),
    outlineVariant = Color(0xFFD0DDDF),
    error = Color(0xFFB3261E),
    errorContainer = Color(0xFFF9DEDC),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFAFDFD),
    surfaceContainer = Color(0xFFEDF4F5),
    surfaceContainerHigh = Color(0xFFE6EEEF),
    surfaceContainerHighest = Color(0xFFDFE8E9),
)

private val DarkScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFF86D2DD),
    onPrimary = Color(0xFF00363D),
    primaryContainer = Color(0xFF1C5A63),
    onPrimaryContainer = Color(0xFFD2F0F4),
    secondary = Color(0xFFB8BEF2),
    onSecondary = Color(0xFF232A6B),
    secondaryContainer = Color(0xFF3A4180),
    onSecondaryContainer = Color(0xFFE5E8FB),
    tertiary = Color(0xFFD7BCF5),
    onTertiary = Color(0xFF3F2763),
    tertiaryContainer = Color(0xFF58407F),
    onTertiaryContainer = Color(0xFFF0E5FA),
    background = Color(0xFF0F1819),
    onBackground = Color(0xFFE0EBED),
    surface = Color(0xFF0F1819),
    onSurface = Color(0xFFE0EBED),
    surfaceVariant = Color(0xFF33474A),
    onSurfaceVariant = Color(0xFFB5C7CA),
    outline = Color(0xFF819396),
    outlineVariant = Color(0xFF33474A),
    error = Color(0xFFF2B8B5),
    errorContainer = Color(0xFF8C1D18),
    surfaceContainerLowest = Color(0xFF0B1314),
    surfaceContainerLow = Color(0xFF152022),
    surfaceContainer = Color(0xFF19272A),
    surfaceContainerHigh = Color(0xFF223235),
    surfaceContainerHighest = Color(0xFF2C3D40),
)

private val SoftShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

private val SoftTypography: Typography = Typography().let { t ->
    t.copy(
        headlineMedium = t.headlineMedium.copy(fontWeight = FontWeight.Bold),
        headlineSmall = t.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
        titleLarge = t.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = t.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        titleSmall = t.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        labelMedium = t.labelMedium.copy(fontWeight = FontWeight.Medium),
    )
}

@Composable
fun SeyfTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (dark) DarkScheme else LightScheme,
        shapes = SoftShapes,
        typography = SoftTypography,
        content = content,
    )
}

/** Ekranning yuqori qismidagi yumshoq gradient fon. */
@Composable
fun softBackdrop(): Brush = Brush.verticalGradient(
    colors = listOf(
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f),
        MaterialTheme.colorScheme.background,
    ),
)

/** Asosiy "brend" gradienti: ikonka doiralari va tugmalar uchun. */
@Composable
fun brandBrush(): Brush = Brush.linearGradient(
    listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary),
)
