package id.soaldulu.app.ui.layar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import id.soaldulu.app.GateConfig
import id.soaldulu.app.ui.theme.SoalduluTheme
import id.soaldulu.app.ui.theme.Ukuran
import id.soaldulu.app.ui.theme.WarnaTambah

/** Keadaan pemasangan paket soal. */
sealed interface StatusPaket {
    data object Belum : StatusPaket
    data object Memasang : StatusPaket
    data class Terpasang(val versi: String, val jumlahButir: Int, val jumlahAktif: Int) : StatusPaket
    data class Gagal(val kesalahan: List<String>) : StatusPaket
}

/**
 * Pemasangan paket soal.
 *
 * Paket dibaca dari assets, bukan diunduh, jadi layar ini menampilkan proses
 * seeding ke Room. Kata "unduh" sengaja tidak dipakai supaya tidak
 * menjanjikan sesuatu yang tidak terjadi.
 */
@Composable
fun LayarPaketSoal(
    status: StatusPaket,
    onPasang: () -> Unit,
    onLanjut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Ukuran.marginLayar, vertical = Ukuran.antarBagian),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "Paket Soal",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(Modifier.height(Ukuran.antarBagian))

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(Ukuran.paddingKartu)) {
                when (status) {
                    StatusPaket.Belum -> {
                        Baris("Berkas", GateConfig.BUNDLED_PACKAGE_ASSET)
                        Baris("Status", "Belum dipasang")
                    }

                    StatusPaket.Memasang -> {
                        Baris("Berkas", GateConfig.BUNDLED_PACKAGE_ASSET)
                        Baris("Status", "Sedang memasang…")
                    }

                    is StatusPaket.Terpasang -> {
                        Baris("Versi", status.versi)
                        Baris("Jumlah butir", "${status.jumlahButir}")
                        Baris("Butir aktif", "${status.jumlahAktif}")
                    }

                    is StatusPaket.Gagal -> {
                        Text(
                            "Paket ditolak — ${status.kesalahan.size} masalah:",
                            style = MaterialTheme.typography.titleSmall,
                            color = WarnaTambah.negatif,
                        )
                        Spacer(Modifier.height(8.dp))
                        status.kesalahan.forEach {
                            Text("· $it", style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.height(4.dp))
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        LinearProgressIndicator(
            progress = {
                when (status) {
                    StatusPaket.Belum -> 0f
                    StatusPaket.Memasang -> 0.5f
                    is StatusPaket.Terpasang -> 1f
                    is StatusPaket.Gagal -> 0f
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(Ukuran.antarBagian))

        Button(
            onClick = if (status is StatusPaket.Terpasang) onLanjut else onPasang,
            enabled = status !is StatusPaket.Memasang,
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) {
            Text(
                when (status) {
                    is StatusPaket.Terpasang -> "Selesai"
                    StatusPaket.Memasang -> "Memasang…"
                    else -> "Pasang paket soal"
                },
                style = MaterialTheme.typography.titleMedium,
            )
        }

        Spacer(Modifier.height(16.dp))

        Text(
            "Setelah terpasang, aplikasi tidak lagi memerlukan koneksi internet.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Baris(label: String, nilai: String) {
    Row(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
        )
        Text(nilai, style = MaterialTheme.typography.bodyMedium)
    }
}

@Preview(name = "Paket Soal", heightDp = 780)
@Composable
private fun PratinjauPaketSoal() {
    SoalduluTheme(paksaGelap = true) {
        LayarPaketSoal(
            status = StatusPaket.Terpasang(versi = "1.0.0", jumlahButir = 150, jumlahAktif = 148),
            onPasang = {},
            onLanjut = {},
        )
    }
}
