package id.soaldulu.app.ui.layar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import id.soaldulu.app.ui.KolomIsian
import id.soaldulu.app.ui.TombolPrimer
import id.soaldulu.app.ui.theme.Accent
import id.soaldulu.app.ui.theme.Background
import id.soaldulu.app.ui.theme.OnBackground
import id.soaldulu.app.ui.theme.OnBackgroundDim
import id.soaldulu.app.ui.theme.SoalduluTheme
import id.soaldulu.app.ui.theme.Teks
import id.soaldulu.app.ui.theme.Ukuran

/**
 * Layar 2 — Kode Responden (handoff Bagian 8 nomor 2).
 *
 * Kode yang disimpan di sini ikut di SETIAP baris log. Kalau kosong atau
 * salah, seluruh data responden itu tidak bisa dihubungkan ke orangnya.
 */
@Composable
fun LayarKodeResponden(
    onLanjut: (kode: String) -> Unit,
    modifier: Modifier = Modifier,
    kodeAwal: String = "",
) {
    var kode by remember { mutableStateOf(kodeAwal) }
    val kodeBersih = kode.trim().uppercase()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Ukuran.marginLayar, vertical = Ukuran.antarBagian),
    ) {
        Text("LANGKAH 1 DARI 4", style = Teks.label, color = Accent)

        Spacer(Modifier.height(16.dp))

        Text("Kode Responden", style = Teks.judulLayar, color = OnBackground)

        Spacer(Modifier.height(16.dp))

        Text(
            "Masukkan kode yang diberikan peneliti. Kode ini dipakai untuk " +
                "menandai data yang aplikasi catat, tanpa mengaitkannya ke identitasmu.",
            style = Teks.isi,
            color = OnBackground,
        )

        Spacer(Modifier.height(Ukuran.antarBagian))

        KolomIsian(
            nilai = kode,
            onNilaiBerubah = { kode = it },
            petunjuk = "R3",
        )

        Spacer(Modifier.height(8.dp))

        Text("Contoh: R1, R2, R3", style = Teks.caption, color = OnBackgroundDim)

        Spacer(Modifier.height(Ukuran.antarBagian))

        TombolPrimer(
            teks = "Lanjut",
            onClick = { onLanjut(kodeBersih) },
            aktif = kodeBersih.isNotEmpty(),
        )

        Spacer(Modifier.height(Ukuran.antarBagian))

        Text(
            "Tidak ada nama, email, atau nomor telepon yang disimpan aplikasi.",
            style = Teks.caption,
            color = OnBackgroundDim,
        )

        Spacer(Modifier.height(Ukuran.antarBagian))
    }
}

@Preview(name = "Layar 2 — Kode Responden", heightDp = 800)
@Composable
private fun PratinjauKodeResponden() {
    SoalduluTheme { LayarKodeResponden(onLanjut = {}) }
}
