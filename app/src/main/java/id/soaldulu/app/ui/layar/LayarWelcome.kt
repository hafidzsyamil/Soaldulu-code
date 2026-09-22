package id.soaldulu.app.ui.layar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.soaldulu.app.ui.theme.Judul
import id.soaldulu.app.ui.theme.SoalduluTheme
import id.soaldulu.app.ui.theme.Ukuran

/**
 * Layar 1 — Welcome.
 *
 * Halaman pertama aplikasi: logo, nama, tagline. Seluruh layar adalah area
 * sentuh; tidak ada tombol, sesuai berkas desain.
 */
@Composable
fun LayarWelcome(
    onLanjut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onLanjut,
            )
            .padding(horizontal = Ukuran.marginLayar, vertical = Ukuran.antarBagian),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Spacer(Modifier.weight(1f))

        LogoSoaldulu()

        Spacer(Modifier.height(32.dp))

        Text(
            "Soaldulu",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "Tebus waktumu dengan soal.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.weight(1f))

        Text(
            "Tap Anywhere to Continue",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Tanda aplikasi: monogram S di dalam lencana bersudut tumpul, dengan satu
 * palang di bawahnya sebagai gerbang.
 *
 * Digambar dengan Compose, bukan berkas gambar, supaya ikut warna tema —
 * terang maupun gelap — tanpa perlu dua versi aset.
 */
@Composable
fun LogoSoaldulu(
    ukuran: androidx.compose.ui.unit.Dp = 128.dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(ukuran)
            .clip(RoundedCornerShape(percent = 26))
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "S",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontFamily = Judul,
                    fontSize = ukuran.value.times(0.52f).sp,
                ),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.height(ukuran * 0.04f))
            // Palang gerbang: garis yang harus dilewati sebelum masuk.
            Box(
                Modifier
                    .width(ukuran * 0.34f)
                    .height(ukuran * 0.035f)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
    }
}

@Preview(name = "Welcome — gelap", heightDp = 780)
@Composable
private fun PratinjauWelcomeGelap() {
    SoalduluTheme(paksaGelap = true) { LayarWelcome(onLanjut = {}) }
}

@Preview(name = "Welcome — terang", heightDp = 780)
@Composable
private fun PratinjauWelcomeTerang() {
    SoalduluTheme(paksaGelap = false) { LayarWelcome(onLanjut = {}) }
}
