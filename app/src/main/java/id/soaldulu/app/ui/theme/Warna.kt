package id.soaldulu.app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Palet Soaldulu, revisi 22 September 2026.
 *
 * Empat warna dasar dari berkas desain:
 *   1B262C  navy tua      0F4C75  biru dalam
 *   3282B8  biru sedang   BBE1FA  biru pucat
 *
 * Dipetakan ke peran Material 3, bukan dipakai mentah. Kartu biru terisi
 * pada mockup adalah primaryContainer: di tema gelap ia jadi 0F4C75 dengan
 * teks pucat, di tema terang ia jadi BBE1FA dengan teks gelap. Identitas
 * kartu biru terjaga di kedua tema tanpa melawan aturan kontras M3.
 */

private val BiruDalam = Color(0xFF0F4C75)
private val BiruSedang = Color(0xFF3282B8)
private val BiruPucat = Color(0xFFBBE1FA)
private val NavyTua = Color(0xFF1B262C)

val SkemaGelapM3 = darkColorScheme(
    primary = BiruSedang,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = BiruDalam,
    onPrimaryContainer = BiruPucat,
    secondary = BiruPucat,
    onSecondary = NavyTua,
    secondaryContainer = Color(0xFF14364E),
    onSecondaryContainer = BiruPucat,
    tertiary = Color(0xFFD0BCFF),
    onTertiary = Color(0xFF381E72),
    background = NavyTua,
    onBackground = Color(0xFFE7F2FA),
    surface = NavyTua,
    onSurface = Color(0xFFE7F2FA),
    surfaceVariant = Color(0xFF14364E),
    onSurfaceVariant = Color(0xFF9DBBCE),
    surfaceContainer = Color(0xFF16303E),
    surfaceContainerHigh = Color(0xFF1B3A4B),
    surfaceContainerHighest = Color(0xFF204558),
    outline = Color(0xFF2A5E85),
    outlineVariant = Color(0xFF24455C),
    error = Color(0xFFFFA2A2),
    onError = NavyTua,
    errorContainer = Color(0xFF6B2020),
    onErrorContainer = Color(0xFFFFDAD6),
)

val SkemaTerangM3 = lightColorScheme(
    primary = BiruDalam,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = BiruPucat,
    onPrimaryContainer = Color(0xFF0A344F),
    secondary = BiruSedang,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD4EAF8),
    onSecondaryContainer = Color(0xFF0A344F),
    tertiary = Color(0xFF6750A4),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFF7FAFD),
    onBackground = Color(0xFF16222A),
    surface = Color(0xFFF7FAFD),
    onSurface = Color(0xFF16222A),
    surfaceVariant = Color(0xFFDDE8F0),
    onSurfaceVariant = Color(0xFF41535F),
    surfaceContainer = Color(0xFFEBF1F7),
    surfaceContainerHigh = Color(0xFFE5EDF4),
    surfaceContainerHighest = Color(0xFFDFE8F1),
    outline = Color(0xFF71838F),
    outlineVariant = Color(0xFFC1CFDA),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
)

/**
 * Warna yang tidak punya peran di Material 3 tapi dibutuhkan penelitian ini.
 *
 * `positif` dan `negatif` adalah penanda status jawaban, bukan elemen yang
 * bisa disentuh. Nilai terang dan gelapnya berbeda karena 9BFA01 dan FFA2A2
 * dari mockup tidak terbaca di atas latar pucat.
 */
@Immutable
data class WarnaTambahan(
    val positif: Color,
    val negatif: Color,
    /** Tautan di dalam kalimat, seperti "terms and condition". */
    val tautan: Color,
)

val TambahanGelap = WarnaTambahan(
    positif = Color(0xFF9BFA01),
    negatif = Color(0xFFFFA2A2),
    tautan = Color(0xFFD4BD3B),
)

val TambahanTerang = WarnaTambahan(
    positif = Color(0xFF2E7D00),
    negatif = Color(0xFFB3261E),
    tautan = Color(0xFF8A6D00),
)

val LocalWarnaTambahan = staticCompositionLocalOf { TambahanGelap }

val WarnaTambah: WarnaTambahan
    @Composable
    @ReadOnlyComposable
    get() = LocalWarnaTambahan.current
