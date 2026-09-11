package id.soaldulu.app.ui.layar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import id.soaldulu.app.GateConfig
import id.soaldulu.app.ui.TombolSekunder
import id.soaldulu.app.ui.theme.Accent
import id.soaldulu.app.ui.theme.Background
import id.soaldulu.app.ui.theme.Border
import id.soaldulu.app.ui.theme.OnBackground
import id.soaldulu.app.ui.theme.OnBackgroundDim
import id.soaldulu.app.ui.theme.SoalduluTheme
import id.soaldulu.app.ui.theme.Teks
import id.soaldulu.app.ui.theme.Ukuran

/**
 * Layar 10 — Settings (handoff Bagian 8 nomor 10).
 *
 * Tidak ada toggle tema. Tema gelap hangat adalah keputusan desain, bukan
 * preferensi.
 */
@Composable
fun LayarSettings(
    kodeResponden: String,
    versiPaket: String,
    jumlahButirAktif: Int,
    versiAplikasi: String,
    statusEkspor: String,
    onEkspor: () -> Unit,
    onBukaLayarUji: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Ukuran.marginLayar, vertical = Ukuran.antarBagian),
    ) {
        Text("Pengaturan", style = Teks.judulLayar, color = OnBackground)

        Spacer(Modifier.height(Ukuran.antarBagian))

        Text("PENELITIAN", style = Teks.label, color = Accent)
        Spacer(Modifier.height(12.dp))
        BarisInfo("Kode responden", kodeResponden.ifBlank { "belum diisi" })
        BarisInfo("Versi paket soal", versiPaket.ifBlank { "belum terpasang" })
        BarisInfo("Butir aktif", "$jumlahButirAktif")
        BarisInfo(
            "Cek pembaruan",
            // UPDATE_MANIFEST_URL kosong = fitur mati (handoff Bagian 4).
            if (GateConfig.UPDATE_MANIFEST_URL.isBlank()) "tidak aktif" else "tersedia",
        )

        Spacer(Modifier.height(Ukuran.antarBagian))
        HorizontalDivider(color = Border)
        Spacer(Modifier.height(Ukuran.antarBagian))

        Text("DATA", style = Teks.label, color = Accent)
        Spacer(Modifier.height(12.dp))
        Text(
            "Ekspor seluruh log ke satu berkas CSV, lalu kirim ke peneliti.",
            style = Teks.isi,
            color = OnBackground,
        )
        Spacer(Modifier.height(16.dp))
        TombolSekunder("Ekspor & bagikan log", onClick = onEkspor)
        if (statusEkspor.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(statusEkspor, style = Teks.caption, color = OnBackgroundDim)
        }

        Spacer(Modifier.height(Ukuran.antarBagian))
        HorizontalDivider(color = Border)
        Spacer(Modifier.height(Ukuran.antarBagian))

        Text("TENTANG", style = Teks.label, color = Accent)
        Spacer(Modifier.height(12.dp))
        BarisInfo("Versi aplikasi", versiAplikasi)
        Spacer(Modifier.height(12.dp))
        Text(
            "Soaldulu dibuat untuk penelitian karya tulis ilmiah tentang " +
                "pengurangan distraksi digital pada siswa kelas XII. Aplikasi ini " +
                "tidak dipublikasikan dan tidak dimonetisasi.",
            style = Teks.isi,
            color = OnBackground,
        )

        Spacer(Modifier.height(Ukuran.antarBagian))

        Text(
            "Tema gelap hangat dipilih karena aplikasi paling sering muncul " +
                "pada penggunaan malam hari.",
            style = Teks.caption,
            color = OnBackgroundDim,
        )

        if (GateConfig.DEV_MODE) {
            Spacer(Modifier.height(Ukuran.antarBagian))
            HorizontalDivider(color = Border)
            Spacer(Modifier.height(Ukuran.antarBagian))
            Text("PENGEMBANGAN", style = Teks.label, color = Accent)
            Spacer(Modifier.height(8.dp))
            Text(
                "Bagian ini hilang sendiri saat GateConfig.DEV_MODE dikembalikan " +
                    "ke false — wajib sebelum APK dibagikan ke responden.",
                style = Teks.caption,
                color = OnBackgroundDim,
            )
            Spacer(Modifier.height(12.dp))
            TombolSekunder("Buka layar uji Fase 0", onClick = onBukaLayarUji)
        }

        Spacer(Modifier.height(Ukuran.antarBagian))
    }
}

@Composable
private fun BarisInfo(label: String, nilai: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = Teks.isi, color = OnBackgroundDim, modifier = Modifier.weight(1f))
        Text(nilai, style = Teks.isi, color = OnBackground)
    }
}

@Preview(name = "Layar 10 — Settings", heightDp = 900)
@Composable
private fun PratinjauSettings() {
    SoalduluTheme {
        LayarSettings(
            kodeResponden = "R3",
            versiPaket = "1.0.0",
            jumlahButirAktif = 148,
            versiAplikasi = "1.0 (1)",
            statusEkspor = "",
            onEkspor = {},
            onBukaLayarUji = {},
        )
    }
}
