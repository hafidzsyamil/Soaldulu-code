package id.soaldulu.app.ui.layar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import id.soaldulu.app.GateConfig
import id.soaldulu.app.StatusIzin
import id.soaldulu.app.ui.TombolPrimer
import id.soaldulu.app.ui.TombolSekunder
import id.soaldulu.app.ui.theme.Accent
import id.soaldulu.app.ui.theme.Background
import id.soaldulu.app.ui.theme.Border
import id.soaldulu.app.ui.theme.OnBackground
import id.soaldulu.app.ui.theme.OnBackgroundDim
import id.soaldulu.app.ui.theme.Teks
import id.soaldulu.app.ui.theme.Ukuran

/**
 * Alat uji Fase 0. BUKAN layar aplikasi.
 *
 * Hidup hanya selama GateConfig.DEV_MODE true, dan dibuang setelah keempat
 * kriteria Bagian 9 lulus. Bagian yang sudah punya layar sungguhan
 * (perizinan, bank soal, onboarding) sudah dikeluarkan dari sini.
 */
@Composable
fun LayarSpike(
    status: StatusIzin,
    jumlahStart: Int,
    ringkasanLog: String,
    isiLog: String,
    onMulaiService: () -> Unit,
    onHentikanService: () -> Unit,
    onMuatLog: () -> Unit,
    onKembali: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Ukuran.marginLayar, vertical = Ukuran.antarBagian),
    ) {
        Text("LAYAR UJI FASE 0", style = Teks.label, color = Accent)
        Spacer(Modifier.height(8.dp))
        Text(
            "Hilang sendiri saat DEV_MODE dikembalikan ke false.",
            style = Teks.caption,
            color = OnBackgroundDim,
        )

        Spacer(Modifier.height(Ukuran.antarBagian))

        Text(
            "${status.jumlahAktif} dari 4 izin aktif",
            style = Teks.isi,
            color = if (status.semuaAktif) Accent else OnBackgroundDim,
        )

        Spacer(Modifier.height(16.dp))

        TombolPrimer("Mulai service", onClick = onMulaiService, aktif = status.semuaAktif)
        Spacer(Modifier.height(8.dp))
        TombolSekunder("Hentikan service", onClick = onHentikanService)

        Spacer(Modifier.height(Ukuran.antarBagian))
        HorizontalDivider(color = Border)
        Spacer(Modifier.height(Ukuran.antarBagian))

        Text("Kriteria 3 — service hidup 6 jam", style = Teks.isi, color = OnBackground)
        Spacer(Modifier.height(8.dp))
        Text(
            "Service pernah start: $jumlahStart kali. Kalau angka ini naik " +
                "sendiri selama uji, service sempat mati lalu dihidupkan ulang sistem.",
            style = Teks.caption,
            color = OnBackgroundDim,
        )

        Spacer(Modifier.height(Ukuran.antarBagian))

        Text("Aplikasi dipantau", style = Teks.isi, color = OnBackground)
        Spacer(Modifier.height(8.dp))
        Text(
            GateConfig.MONITORED_PACKAGES.joinToString("\n"),
            style = Teks.caption,
            color = OnBackgroundDim,
        )

        Spacer(Modifier.height(Ukuran.antarBagian))
        HorizontalDivider(color = Border)
        Spacer(Modifier.height(Ukuran.antarBagian))

        Text("Berkas log", style = Teks.isi, color = OnBackground)
        Spacer(Modifier.height(4.dp))
        Text(ringkasanLog, style = Teks.caption, color = OnBackgroundDim)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth()) {
            TombolSekunder("Muat log", onClick = onMuatLog, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(12.dp))
            TombolSekunder("Kembali", onClick = onKembali, modifier = Modifier.weight(1f))
        }
        if (isiLog.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text(
                isiLog,
                style = Teks.caption.copy(fontFamily = FontFamily.Monospace),
                color = OnBackgroundDim,
            )
        }

        Spacer(Modifier.height(Ukuran.antarBagian))
    }
}
