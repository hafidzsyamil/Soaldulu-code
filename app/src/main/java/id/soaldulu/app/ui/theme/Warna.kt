package id.soaldulu.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Palet Soaldulu (handoff Bagian 7.2).
 *
 * Dua aturan yang tidak boleh dilanggar:
 * 1. Kuningan (Accent) adalah SATU-SATUNYA bahasa interaktif. Kalau sebuah
 *    elemen bisa disentuh, warnanya kuningan.
 * 2. Crimson (Emphasis) HANYA penanda status salah. Tidak pernah untuk
 *    tombol, tidak pernah untuk elemen yang bisa disentuh.
 *
 * Tidak ada hitam murni, tidak ada putih murni. Tidak ada tema terang.
 */

/** Mahoni tua — latar halaman. */
val Background = Color(0xFF1C1714)

/** Kartu, panel, bilah. */
val Surface = Color(0xFF251E19)

/** Latar nonaktif, track progres. */
val Muted = Color(0xFF3D332B)

/** Garis pemisah. */
val Border = Color(0xFF4A3F35)

/** Teks utama. */
val OnBackground = Color(0xFFE8DFD4)

/** Teks sekunder, caption. */
val OnBackgroundDim = Color(0xFF9C8B7A)

/** Kuningan — SEMUA elemen interaktif. */
val Accent = Color(0xFFC9A962)

/** Strip kilau di tepi atas tombol primer. */
val AccentHighlight = Color(0xFFD4B872)

/** Crimson — HANYA penanda status salah. */
val Emphasis = Color(0xFF8B2635)

/** Teks di atas tombol kuningan. */
val OnAccent = Color(0xFF1C1714)
