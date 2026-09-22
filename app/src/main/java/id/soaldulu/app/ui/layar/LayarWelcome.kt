package id.soaldulu.app.ui.layar

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import id.soaldulu.app.R
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
    /** null: layar pembuka saat aplikasi dibuka — tidak bisa disentuh, tanpa petunjuk. */
    onLanjut: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .then(
                if (onLanjut != null) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onLanjut,
                    )
                } else {
                    Modifier
                },
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
            // Disembunyikan, bukan dihapus: tata letaknya tetap sama persis,
            // jadi pembuka → Welcome hanya terlihat sebagai petunjuk yang muncul.
            modifier = Modifier.alpha(if (onLanjut != null) 1f else 0f),
        )
    }
}

/**
 * Logo aplikasi: dua lembar kertas bertumpuk, diambil apa adanya dari berkas
 * desain (Project Syamil RNI.svg). Latarnya transparan, jadi cocok di tema
 * terang maupun gelap. Logo yang sama menjadi ikon peluncur.
 */
@Composable
fun LogoSoaldulu(
    tinggi: Dp = 200.dp,
    modifier: Modifier = Modifier,
) {
    Image(
        painter = painterResource(R.drawable.logo_soaldulu),
        contentDescription = "Logo Soaldulu",
        modifier = modifier.height(tinggi),
    )
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
