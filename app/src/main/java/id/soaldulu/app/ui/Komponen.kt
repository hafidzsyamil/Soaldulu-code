package id.soaldulu.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.soaldulu.app.ui.theme.Accent
import id.soaldulu.app.ui.theme.AccentHighlight
import id.soaldulu.app.ui.theme.Background
import id.soaldulu.app.ui.theme.Bentuk
import id.soaldulu.app.ui.theme.Border
import id.soaldulu.app.ui.theme.Muted
import id.soaldulu.app.ui.theme.OnAccent
import id.soaldulu.app.ui.theme.OnBackground
import id.soaldulu.app.ui.theme.OnBackgroundDim
import id.soaldulu.app.ui.theme.SoalduluTheme
import id.soaldulu.app.ui.theme.Surface
import id.soaldulu.app.ui.theme.Teks
import id.soaldulu.app.ui.theme.Ukuran

/**
 * Lima komponen dasar dari handoff Bagian 7.5.
 *
 * Dibangun sendiri, bukan memakai komponen Material, karena spesifikasinya
 * menuntut bentuk yang tidak disediakan Material — terutama strip kilau
 * kuningan di tepi atas tombol primer.
 *
 * Tidak ada animasi, transisi, atau ripple kustom (handoff Bagian 0.3).
 * Elemen khas Bagian 7.6 (arch-top, corner flourish, ornate divider)
 * sengaja BELUM dibuat — dikerjakan terakhir, setelah semua fungsi jalan.
 */

/**
 * Tombol primer — latar kuningan dengan strip kilau di tepi atas.
 * Teks otomatis dikapitalkan sesuai Bagian 7.3.
 */
@Composable
fun TombolPrimer(
    teks: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    aktif: Boolean = true,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(Ukuran.tinggiTombol)
            .clip(Bentuk.tombol)
            .background(if (aktif) Accent else Muted)
            .clickable(enabled = aktif, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (aktif) {
            // Imitasi kilau kuningan.
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(Ukuran.stripKilau)
                    .background(AccentHighlight),
            )
        }
        Text(
            text = teks.uppercase(),
            style = Teks.tombol,
            color = if (aktif) OnAccent else OnBackgroundDim,
        )
    }
}

/** Tombol sekunder — transparan dengan garis kuningan. */
@Composable
fun TombolSekunder(
    teks: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    aktif: Boolean = true,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(Ukuran.tinggiTombol)
            .clip(Bentuk.tombol)
            .border(Ukuran.tebalGarisTombol, if (aktif) Accent else Muted, Bentuk.tombol)
            .clickable(enabled = aktif, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = teks.uppercase(),
            style = Teks.tombol,
            color = if (aktif) Accent else OnBackgroundDim,
        )
    }
}

/** Kartu / panel — latar Surface dengan garis tipis. */
@Composable
fun Kartu(
    modifier: Modifier = Modifier,
    isi: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(Bentuk.kartu)
            .background(Surface)
            .border(Ukuran.tebalGaris, Border, Bentuk.kartu)
            .padding(Ukuran.paddingKartu),
        content = isi,
    )
}

/** Kolom isian — dipakai layar Kode Responden. */
@Composable
fun KolomIsian(
    nilai: String,
    onNilaiBerubah: (String) -> Unit,
    modifier: Modifier = Modifier,
    petunjuk: String = "",
) {
    BasicTextField(
        value = nilai,
        onValueChange = onNilaiBerubah,
        singleLine = true,
        textStyle = Teks.isi.copy(color = OnBackground),
        cursorBrush = SolidColor(Accent),
        modifier = modifier
            .fillMaxWidth()
            .height(Ukuran.tinggiKolomIsian)
            .clip(Bentuk.kartu)
            .background(Surface)
            .border(Ukuran.tebalGaris, Accent, Bentuk.kartu),
        decorationBox = { isian ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = Ukuran.paddingKartu),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (nilai.isEmpty()) {
                    Text(text = petunjuk, style = Teks.isi, color = OnBackgroundDim)
                }
                isian()
            }
        },
    )
}

/**
 * Lencana huruf opsi (A/B/C/D).
 *
 * Keadaan terpilih: lingkaran terisi kuningan, huruf jadi gelap. Tampilan
 * terpilih ini belum ada di mockup tapi diminta Bagian 8 nomor 6.
 */
@Composable
fun LencanaOpsi(
    huruf: String,
    terpilih: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(Ukuran.lencanaOpsi)
            .clip(CircleShape)
            .background(if (terpilih) Accent else Color.Transparent)
            .border(Ukuran.tebalGarisTombol, Accent, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = huruf,
            // letterSpacing dibuang: di dalam lingkaran 24dp, jarak antar huruf
            // menggeser huruf tunggal keluar dari titik tengah.
            style = Teks.label.copy(letterSpacing = 0.sp),
            color = if (terpilih) OnAccent else Accent,
        )
    }
}

// ─── Pratinjau untuk Android Studio ──────────────────────────────────────────
// Bukan layar aplikasi. Handoff Bagian 8 nomor 11 menyuruh mengabaikan halaman
// style sheet sebagai layar, jadi acuan visualnya hidup sebagai @Preview saja.

@Preview(name = "Komponen Soaldulu", showBackground = true, heightDp = 900)
@Composable
private fun PratinjauKomponen() {
    SoalduluTheme {
        var isian by remember { mutableStateOf("") }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Background)
                .padding(Ukuran.marginLayar),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("LANGKAH 1 DARI 4", style = Teks.label, color = Accent)
            Text("Kode Responden", style = Teks.judulLayar, color = OnBackground)
            Text(
                "Tebus waktumu dengan soal. Akses media sosial dibuka dengan " +
                    "mengerjakan soal TKA/SNBT.",
                style = Teks.isi,
                color = OnBackground,
            )
            Text("Contoh: R1, R2, R3", style = Teks.caption, color = OnBackgroundDim)

            KolomIsian(isian, { isian = it }, petunjuk = "R3")

            Kartu {
                Text("YANG DICATAT", style = Teks.label, color = Accent)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Nama aplikasi yang dibuka dan berapa lama.",
                    style = Teks.isi,
                    color = OnBackground,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LencanaOpsi("A", terpilih = false)
                LencanaOpsi("B", terpilih = true)
            }

            TombolPrimer("Lanjut", onClick = {})
            TombolSekunder("Soal berikutnya", onClick = {})
            TombolPrimer("Belum bisa", onClick = {}, aktif = false)
        }
    }
}
