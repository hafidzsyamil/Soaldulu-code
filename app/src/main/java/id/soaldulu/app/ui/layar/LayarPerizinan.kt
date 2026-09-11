package id.soaldulu.app.ui.layar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import id.soaldulu.app.StatusIzin
import id.soaldulu.app.ui.Kartu
import id.soaldulu.app.ui.TombolPrimer
import id.soaldulu.app.ui.theme.Accent
import id.soaldulu.app.ui.theme.Background
import id.soaldulu.app.ui.theme.Muted
import id.soaldulu.app.ui.theme.OnBackground
import id.soaldulu.app.ui.theme.OnBackgroundDim
import id.soaldulu.app.ui.theme.SoalduluTheme
import id.soaldulu.app.ui.theme.Teks
import id.soaldulu.app.ui.theme.Ukuran

/**
 * Layar 4 — Perizinan (handoff Bagian 8 nomor 4).
 *
 * Layar paling menentukan di seluruh aplikasi: kalau responden tersesat di
 * sini, data responden itu hilang.
 *
 * Tombol BERIKAN IZIN meminta izin yang BELUM aktif satu per satu, dalam
 * urutan wajib Bagian 3.3 — Akses Penggunaan lebih dulu, karena itu yang
 * paling mungkin membuat orang menyerah.
 */
@Composable
fun LayarPerizinan(
    status: StatusIzin,
    onBerikanIzin: () -> Unit,
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
        Text("LANGKAH 3 DARI 4", style = Teks.label, color = Accent)

        Spacer(Modifier.height(16.dp))

        Text("Perizinan", style = Teks.judulLayar, color = OnBackground)

        Spacer(Modifier.height(Ukuran.antarBagian))

        KartuIzin(
            nomor = 1,
            nama = "Akses Penggunaan",
            alasan = "Agar Soaldulu tahu aplikasi apa yang sedang kamu buka.",
            aktif = status.usageAccess,
            // BELUM DIVERIFIKASI di One UI. Sengaja tidak menyebut jalur menu
            // Samsung yang persis — handoff Bagian 3.3 melarang mengarang nama
            // menu. Tombol di bawah membuka halamannya langsung; teks ini hanya
            // jaring pengaman kalau intent-nya tidak mendarat di tempat benar.
            jalurSettings = "Kalau halamannya tidak terbuka sendiri, cari kata " +
                "\"penggunaan\" di pencarian Setelan.",
        )

        KartuIzin(
            nomor = 2,
            nama = "Tampil di Atas Aplikasi Lain",
            alasan = "Agar gerbang soal bisa muncul di atas media sosial.",
            aktif = status.overlay,
        )

        KartuIzin(
            nomor = 3,
            nama = "Notifikasi",
            alasan = "Agar layanan latar tetap berjalan dan statusnya terlihat.",
            aktif = status.notifikasi,
        )

        KartuIzin(
            nomor = 4,
            nama = "Abaikan Optimasi Baterai",
            alasan = "Agar sistem tidak mematikan Soaldulu diam-diam.",
            aktif = status.baterai,
        )

        Spacer(Modifier.height(16.dp))

        Text(
            "${status.jumlahAktif} dari 4 izin sudah aktif",
            style = Teks.isi,
            color = if (status.semuaAktif) Accent else OnBackgroundDim,
        )

        Spacer(Modifier.height(Ukuran.antarBagian))

        if (status.semuaAktif) {
            TombolPrimer("Lanjut", onClick = onLanjut)
        } else {
            TombolPrimer("Berikan izin", onClick = onBerikanIzin)
        }

        Spacer(Modifier.height(Ukuran.antarBagian))
    }
}

@Composable
private fun KartuIzin(
    nomor: Int,
    nama: String,
    alasan: String,
    aktif: Boolean,
    jalurSettings: String? = null,
) {
    Kartu(modifier = Modifier.padding(bottom = 12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text("$nomor · $nama", style = Teks.isi, color = OnBackground)
                Spacer(Modifier.height(6.dp))
                Text(alasan, style = Teks.caption, color = OnBackgroundDim)
                if (jalurSettings != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(jalurSettings, style = Teks.caption, color = Accent)
                }
            }
            Spacer(Modifier.width(12.dp))
            LingkaranStatus(aktif)
        }
    }
}

/** Terisi = izin aktif, kosong = belum. */
@Composable
private fun LingkaranStatus(aktif: Boolean) {
    Box(
        modifier = Modifier
            .size(Ukuran.lencanaOpsi)
            .clip(CircleShape)
            .background(if (aktif) Accent else Color.Transparent)
            .border(Ukuran.tebalGarisTombol, if (aktif) Accent else Muted, CircleShape),
    )
}

@Preview(name = "Layar 4 — Perizinan", heightDp = 900)
@Composable
private fun PratinjauPerizinan() {
    SoalduluTheme {
        LayarPerizinan(
            status = StatusIzin(usageAccess = true, overlay = true),
            onBerikanIzin = {},
            onLanjut = {},
        )
    }
}
