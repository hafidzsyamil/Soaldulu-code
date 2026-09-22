package id.soaldulu.app.ui.theme

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput

/** Arah perpindahan layar, menentukan gerak transisinya. */
enum class Arah {
    /** Masuk lebih dalam: Welcome → Permission, Dashboard → Settings. */
    MAJU,

    /** Tombol Back, gesture Back, atau panah kembali. */
    MUNDUR,

    /** Pindah ke layar yang tidak bersaudara, misalnya onboarding selesai. */
    GANTI,

    /** Layar pembuka ke tujuan pertama: logo membesar dan memudar, tujuan muncul. */
    PEMBUKA,

    /** Pudar silang biasa, untuk dua layar yang hampir sama. */
    PUDAR,

    /** Tanpa gerak. */
    TANPA,
}

/**
 * Berapa lama logo di layar pembuka paling tidak tampil sebelum berpindah ke
 * Dashboard. Tanpa ini data selesai dimuat dalam sepersekian detik dan
 * perpindahannya terlihat sebagai kedipan, bukan animasi. 600 ms adalah
 * token durasi DurationLong4 Material 3.
 */
const val TAHAN_PEMBUKA_MS = 600L

/**
 * Pegas gerak standar Material 3.
 *
 * Nilainya disalin persis dari StandardMotionTokens milik material3 1.4.0 —
 * pegas yang sama dengan yang dipakai komponen M3 bawaan seperti lembar
 * bawah dan sakelar. Disalin, bukan dirujuk, karena MotionScheme di versi
 * ini masih internal; baru terbuka di versi material3 berikutnya.
 */
private object GerakM3 {
    fun <T> spasial(): FiniteAnimationSpec<T> = spring(dampingRatio = 0.9f, stiffness = 700f)
    fun <T> efek(): FiniteAnimationSpec<T> = spring(dampingRatio = 1f, stiffness = 1600f)
    fun <T> efekCepat(): FiniteAnimationSpec<T> = spring(dampingRatio = 1f, stiffness = 3800f)
    fun <T> spasialLambat(): FiniteAnimationSpec<T> = spring(dampingRatio = 0.9f, stiffness = 300f)
    fun <T> efekLambat(): FiniteAnimationSpec<T> = spring(dampingRatio = 1f, stiffness = 800f)
}

/** Transisi antarlayar dengan pola gerak Material 3. */
fun transisiLayar(arah: Arah): ContentTransform = when (arah) {
    // Shared axis X: layar baru datang dari kanan, yang lama bergeser ke kiri.
    Arah.MAJU -> ContentTransform(
        targetContentEnter = slideInHorizontally(GerakM3.spasial()) { it / 10 } +
            fadeIn(GerakM3.efek()),
        initialContentExit = slideOutHorizontally(GerakM3.spasial()) { -it / 10 } +
            fadeOut(GerakM3.efekCepat()),
        sizeTransform = null,
    )

    // Kebalikan MAJU. Layar yang ditinggalkan tetap di atas lalu mengecil,
    // mengikuti pola predictive back Material 3: saat gesture Back ditarik,
    // layar ini mengikuti jari dan layar sebelumnya terlihat di baliknya.
    Arah.MUNDUR -> ContentTransform(
        targetContentEnter = slideInHorizontally(GerakM3.spasial()) { -it / 10 } +
            fadeIn(GerakM3.efek()),
        initialContentExit = slideOutHorizontally(GerakM3.spasial()) { it / 10 } +
            scaleOut(GerakM3.spasial(), targetScale = 0.9f) +
            fadeOut(GerakM3.efek()),
        targetContentZIndex = -1f,
        sizeTransform = null,
    )

    // Fade through: yang lama memudar, yang baru muncul sambil membesar sedikit.
    Arah.GANTI -> ContentTransform(
        targetContentEnter = fadeIn(GerakM3.efek()) +
            scaleIn(GerakM3.spasial(), initialScale = 0.92f),
        initialContentExit = fadeOut(GerakM3.efekCepat()),
        sizeTransform = null,
    )

    // Sekali per pembukaan aplikasi, jadi memakai pegas lambat: logo sedikit
    // membesar sambil memudar, seolah-olah kita masuk ke dalamnya.
    Arah.PEMBUKA -> ContentTransform(
        targetContentEnter = fadeIn(GerakM3.efekLambat()) +
            scaleIn(GerakM3.spasialLambat(), initialScale = 0.92f),
        initialContentExit = fadeOut(GerakM3.efekLambat()) +
            scaleOut(GerakM3.spasialLambat(), targetScale = 1.08f),
        sizeTransform = null,
    )

    Arah.PUDAR -> ContentTransform(
        targetContentEnter = fadeIn(GerakM3.efekLambat()),
        initialContentExit = fadeOut(GerakM3.efekLambat()),
        sizeTransform = null,
    )

    Arah.TANPA -> ContentTransform(
        targetContentEnter = EnterTransition.None,
        initialContentExit = ExitTransition.None,
        sizeTransform = null,
    )
}

/**
 * Menelan seluruh sentuhan saat `tolak` bernilai true.
 *
 * Selama transisi, layar yang sedang pergi masih tergambar dan masih bisa
 * disentuh. Tanpa ini, ketukan ganda yang cepat bisa menekan tombol di
 * layar lama — misalnya membuka Settings dua kali.
 */
fun Modifier.tolakSentuhan(tolak: Boolean): Modifier =
    if (!tolak) {
        this
    } else {
        pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
                }
            }
        }
    }
