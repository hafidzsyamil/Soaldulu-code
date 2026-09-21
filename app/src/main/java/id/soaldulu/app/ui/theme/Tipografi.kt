package id.soaldulu.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ─────────────────────────────────────────────────────────────────────────────
// Judul memakai serif, isi memakai sans — mengikuti berkas desain, di mana
// "Welcome", "Dashboard", "Permission", dan "Settings" jelas berserif
// sementara isi kartu dan paragraf tidak.
//
// Berkas font sungguhan belum ada di proyek. Untuk menggantinya nanti:
//   1. Taruh .ttf di app/src/main/res/font/ (huruf kecil, garis bawah)
//   2. Ganti dua baris di bawah, misalnya:
//        val Judul = FontFamily(Font(R.font.playfair_display_regular))
// ─────────────────────────────────────────────────────────────────────────────

/** Judul layar dan angka besar. */
val Judul = FontFamily.Serif

/** Isi, label, tombol. */
val Isi = FontFamily.SansSerif

/**
 * Skala Material 3 dengan keluarga huruf Soaldulu.
 *
 * Ukuran dibiarkan seperti baku M3 — sudah teruji untuk keterbacaan — kecuali
 * displayLarge yang dipakai angka kredit di Dashboard.
 */
val TipografiSoaldulu = Typography().run {
    copy(
        displayLarge = displayLarge.copy(fontFamily = Judul, fontSize = 44.sp),
        displayMedium = displayMedium.copy(fontFamily = Judul),
        displaySmall = displaySmall.copy(fontFamily = Judul),
        headlineLarge = headlineLarge.copy(fontFamily = Judul),
        headlineMedium = headlineMedium.copy(fontFamily = Judul),
        headlineSmall = headlineSmall.copy(fontFamily = Judul),
        titleLarge = titleLarge.copy(fontFamily = Judul),
        titleMedium = titleMedium.copy(fontFamily = Isi, fontWeight = FontWeight.Medium),
        titleSmall = titleSmall.copy(fontFamily = Isi, fontWeight = FontWeight.Medium),
        bodyLarge = bodyLarge.copy(fontFamily = Isi),
        bodyMedium = bodyMedium.copy(fontFamily = Isi),
        bodySmall = bodySmall.copy(fontFamily = Isi),
        labelLarge = labelLarge.copy(fontFamily = Isi),
        labelMedium = labelMedium.copy(fontFamily = Isi),
        labelSmall = labelSmall.copy(fontFamily = Isi),
    )
}

/** Batang soal. Sengaja lebih besar dari bodyLarge M3 karena dibaca lama. */
val GayaBatangSoal = TextStyle(
    fontFamily = Isi,
    fontSize = 19.sp,
    lineHeight = 28.sp,
)
