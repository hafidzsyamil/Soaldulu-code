package id.soaldulu.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/**
 * Tema Soaldulu. Gelap saja.
 *
 * Tidak ada tema terang, tidak ada dynamic color, tidak ada toggle tema
 * (handoff Bagian 0.3 dan 2.2). Tema gelap hangat dipilih karena aplikasi
 * paling sering muncul pada penggunaan malam hari.
 *
 * ColorScheme Material di sini hanya jaring pengaman supaya komponen
 * Material bawaan tidak tampil dengan warna ungu template. Warna yang
 * dipakai layar-layar Soaldulu diambil langsung dari Warna.kt, dan
 * teksnya dari Teks di Tipografi.kt.
 */
private val SkemaWarna = darkColorScheme(
    primary = Accent,
    onPrimary = OnAccent,
    secondary = Accent,
    onSecondary = OnAccent,
    background = Background,
    onBackground = OnBackground,
    surface = Surface,
    onSurface = OnBackground,
    surfaceVariant = Muted,
    onSurfaceVariant = OnBackgroundDim,
    outline = Border,
    error = Emphasis,
    onError = OnBackground,
)

private val TipografiMaterial = Typography(
    displayLarge = Teks.angkaKredit,
    headlineLarge = Teks.judulLayar,
    headlineMedium = Teks.verdict,
    titleLarge = Teks.batangSoal,
    bodyLarge = Teks.isi,
    bodyMedium = Teks.opsi,
    bodySmall = Teks.caption,
    labelLarge = Teks.tombol,
    labelSmall = Teks.label,
)

@Composable
fun SoalduluTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SkemaWarna,
        typography = TipografiMaterial,
        content = content,
    )
}
