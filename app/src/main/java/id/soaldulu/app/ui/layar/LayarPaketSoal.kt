package id.soaldulu.app.ui.layar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import id.soaldulu.app.GateConfig
import id.soaldulu.app.ui.Kartu
import id.soaldulu.app.ui.TombolPrimer
import id.soaldulu.app.ui.theme.Accent
import id.soaldulu.app.ui.theme.Background
import id.soaldulu.app.ui.theme.Bentuk
import id.soaldulu.app.ui.theme.Emphasis
import id.soaldulu.app.ui.theme.Muted
import id.soaldulu.app.ui.theme.OnBackground
import id.soaldulu.app.ui.theme.OnBackgroundDim
import id.soaldulu.app.ui.theme.SoalduluTheme
import id.soaldulu.app.ui.theme.Teks
import id.soaldulu.app.ui.theme.Ukuran

/** Keadaan pemasangan paket soal. */
sealed interface StatusPaket {
    data object Belum : StatusPaket
    data object Memasang : StatusPaket
    data class Terpasang(val versi: String, val jumlahButir: Int, val jumlahAktif: Int) : StatusPaket
    data class Gagal(val kesalahan: List<String>) : StatusPaket
}

/**
 * Layar 5 — Unduh Paket Soal (handoff Bagian 8 nomor 5).
 *
 * Untuk Fase 1-4 paket dibaca dari assets, bukan diunduh, jadi layar ini
 * menampilkan proses seeding ke Room. Kata "unduh" sengaja tidak dipakai
 * supaya tidak menjanjikan sesuatu yang tidak terjadi.
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
            .background(Background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Ukuran.marginLayar, vertical = Ukuran.antarBagian),
    ) {
        Text("LANGKAH 4 DARI 4", style = Teks.label, color = Accent)

        Spacer(Modifier.height(16.dp))

        Text("Paket Soal", style = Teks.judulLayar, color = OnBackground)

        Spacer(Modifier.height(Ukuran.antarBagian))

        Kartu {
            Text("PAKET", style = Teks.label, color = Accent)
            Spacer(Modifier.height(12.dp))

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
                        style = Teks.isi,
                        color = Emphasis,
                    )
                    Spacer(Modifier.height(8.dp))
                    status.kesalahan.forEach {
                        Text("· $it", style = Teks.caption, color = OnBackgroundDim)
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        BilahProgres(
            terisi = when (status) {
                StatusPaket.Belum -> 0f
                StatusPaket.Memasang -> 0.5f
                is StatusPaket.Terpasang -> 1f
                is StatusPaket.Gagal -> 0f
            }
        )

        Spacer(Modifier.height(Ukuran.antarBagian))

        when (status) {
            is StatusPaket.Terpasang -> TombolPrimer("Selesai", onClick = onLanjut)
            StatusPaket.Memasang -> TombolPrimer("Memasang…", onClick = {}, aktif = false)
            else -> TombolPrimer("Pasang paket soal", onClick = onPasang)
        }

        Spacer(Modifier.height(Ukuran.antarBagian))

        Text(
            "Setelah terpasang, aplikasi tidak lagi memerlukan koneksi internet.",
            style = Teks.caption,
            color = OnBackgroundDim,
        )

        Spacer(Modifier.height(Ukuran.antarBagian))
    }
}

@Composable
private fun Baris(label: String, nilai: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
    ) {
        Text(label, style = Teks.caption, color = OnBackgroundDim, modifier = Modifier.weight(1f))
        Text(nilai, style = Teks.isi, color = OnBackground)
    }
}

@Composable
private fun BilahProgres(terisi: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(Bentuk.kartu)
            .background(Muted),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(terisi.coerceIn(0f, 1f))
                .height(6.dp)
                .clip(Bentuk.kartu)
                .background(Accent),
        )
    }
}

@Preview(name = "Layar 5 — Paket Soal", heightDp = 800)
@Composable
private fun PratinjauPaketSoal() {
    SoalduluTheme {
        LayarPaketSoal(
            status = StatusPaket.Terpasang(versi = "1.0.0", jumlahButir = 150, jumlahAktif = 148),
            onPasang = {},
            onLanjut = {},
        )
    }
}
