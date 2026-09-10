package id.soaldulu.app.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ─────────────────────────────────────────────────────────────────────────────
// BELUM FINAL — berkas font belum ada di proyek.
//
// Handoff Bagian 7.3 meminta tiga serif dari Google Fonts. Karena aplikasi
// wajib berjalan penuh tanpa internet, font HARUS dibundel sebagai .ttf,
// bukan diunduh saat dipakai.
//
// Cara menggantinya nanti:
//   1. Unduh dari fonts.google.com, taruh .ttf di app/src/main/res/font/
//      (nama berkas huruf kecil, pakai garis bawah, tanpa angka di depan)
//   2. Ganti ketiga baris di bawah, misalnya:
//
//        val Cormorant = FontFamily(
//            Font(R.font.cormorant_garamond_regular),
//            Font(R.font.cormorant_garamond_semibold, FontWeight.SemiBold),
//        )
//
// Sampai itu terjadi, ketiganya memakai serif bawaan sistem. Proporsi dan
// tata letak sudah benar; hanya bentuk hurufnya yang belum sesuai.
// ─────────────────────────────────────────────────────────────────────────────

/** Judul layar dan angka besar. Rapuh di ukuran kecil — pakai hanya >= 24sp. */
val Cormorant = FontFamily.Serif

/** Batang soal, opsi, teks umum. */
val CrimsonPro = FontFamily.Serif

/** Label kapital dan teks tombol. */
val Cinzel = FontFamily.Serif

/**
 * Skala teks Soaldulu (handoff Bagian 7.3).
 *
 * Pakai nilai ini, bukan angka px dari mockup.
 */
object Teks {

    /** 40sp Cormorant — sisa kredit di Home. */
    val angkaKredit = TextStyle(
        fontFamily = Cormorant,
        fontSize = 40.sp,
        fontWeight = FontWeight.Normal,
    )

    /** 28sp Cormorant — judul layar. */
    val judulLayar = TextStyle(
        fontFamily = Cormorant,
        fontSize = 28.sp,
        fontWeight = FontWeight.Normal,
    )

    /** 32sp Cormorant — "Benar" / "Belum tepat". */
    val verdict = TextStyle(
        fontFamily = Cormorant,
        fontSize = 32.sp,
        fontWeight = FontWeight.Normal,
    )

    /** 20sp Crimson Pro — batang soal. */
    val batangSoal = TextStyle(
        fontFamily = CrimsonPro,
        fontSize = 20.sp,
        lineHeight = 28.sp,
    )

    /** 17sp Crimson Pro — opsi jawaban. */
    val opsi = TextStyle(
        fontFamily = CrimsonPro,
        fontSize = 17.sp,
        lineHeight = 24.sp,
    )

    /** 16sp Crimson Pro — teks isi umum. */
    val isi = TextStyle(
        fontFamily = CrimsonPro,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    )

    /** 15sp Cinzel kapital — teks tombol. */
    val tombol = TextStyle(
        fontFamily = Cinzel,
        fontSize = 15.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 1.sp,
    )

    /** 12sp Cinzel kapital — label bagian dan overline. */
    val label = TextStyle(
        fontFamily = Cinzel,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 1.5.sp,
    )

    /** 13sp Crimson Pro miring — caption. */
    val caption = TextStyle(
        fontFamily = CrimsonPro,
        fontSize = 13.sp,
        fontStyle = FontStyle.Italic,
        lineHeight = 18.sp,
    )
}
