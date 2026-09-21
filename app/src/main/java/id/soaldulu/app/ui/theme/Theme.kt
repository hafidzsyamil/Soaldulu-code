package id.soaldulu.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/**
 * Tema Soaldulu, berbasis Material 3.
 *
 * Mengikuti tema sistem. `paksaGelap` hanya terisi kalau responden memilih
 * sendiri lewat Settings; selama null, sistem yang menentukan.
 *
 * Tidak ada dynamic color: warna biru ini adalah identitas aplikasi dan
 * tidak boleh berubah mengikuti wallpaper responden — kalau berubah, dua
 * responden melihat aplikasi yang berbeda dan pengamatan tidak sebanding.
 */
@Composable
fun SoalduluTheme(
    paksaGelap: Boolean? = null,
    content: @Composable () -> Unit,
) {
    val gelap = paksaGelap ?: isSystemInDarkTheme()

    CompositionLocalProvider(
        LocalWarnaTambahan provides if (gelap) TambahanGelap else TambahanTerang,
    ) {
        MaterialTheme(
            colorScheme = if (gelap) SkemaGelapM3 else SkemaTerangM3,
            typography = TipografiSoaldulu,
            content = content,
        )
    }
}
