package id.soaldulu.app.ui.layar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import id.soaldulu.app.data.LogJawabanEntity
import id.soaldulu.app.ui.theme.Accent
import id.soaldulu.app.ui.theme.Background
import id.soaldulu.app.ui.theme.Bentuk
import id.soaldulu.app.ui.theme.Border
import id.soaldulu.app.ui.theme.Emphasis
import id.soaldulu.app.ui.theme.OnBackground
import id.soaldulu.app.ui.theme.OnBackgroundDim
import id.soaldulu.app.ui.theme.SoalduluTheme
import id.soaldulu.app.ui.theme.Surface
import id.soaldulu.app.ui.theme.Teks
import id.soaldulu.app.ui.theme.Ukuran

/** Angka-angka yang ditampilkan Home. */
data class StatistikHome(
    val kodeResponden: String = "",
    val sisaKreditDetik: Int = 0,
    val soalDikerjakan: Int = 0,
    val jumlahBenar: Int = 0,
    val hariBerjalan: Int = 0,
    val aktivitasTerakhir: List<LogJawabanEntity> = emptyList(),
)

/**
 * Ambang sebelum persentase benar ditampilkan.
 *
 * Handoff Bagian 8 nomor 9 menyarankan ini dan meminta ditanyakan dulu.
 * Diambil keputusan sementara: sembunyikan sampai 20 soal. Alasannya,
 * dengan 3 soal pertama satu kesalahan langsung membuat angkanya 67% dan
 * itu mematahkan semangat di hari pertama — persis kekhawatiran yang
 * ditulis di handoff. TINJAU INI.
 */
const val AMBANG_TAMPILKAN_PERSEN = 20

/** Layar 9 — Home (handoff Bagian 8 nomor 9). */
@Composable
fun LayarHome(
    statistik: StatistikHome,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Ukuran.marginLayar, vertical = Ukuran.antarBagian),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Soaldulu", style = Teks.judulLayar, color = OnBackground)
            Text(
                statistik.kodeResponden.ifBlank { "—" },
                style = Teks.label,
                color = Accent,
            )
        }

        Spacer(Modifier.height(Ukuran.antarBagian))

        KartuKredit(statistik.sisaKreditDetik)

        Spacer(Modifier.height(Ukuran.antarBagian))

        Row(
            // Tinggi disamakan supaya kotak BENAR yang punya baris catatan
            // tambahan tidak lebih jangkung dari dua kotak lainnya.
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            KotakStatistik(
                label = "SOAL",
                nilai = "${statistik.soalDikerjakan}",
                modifier = Modifier.weight(1f),
            )
            KotakStatistik(
                label = "BENAR",
                nilai = if (statistik.soalDikerjakan >= AMBANG_TAMPILKAN_PERSEN) {
                    "${statistik.jumlahBenar * 100 / statistik.soalDikerjakan}%"
                } else {
                    "—"
                },
                catatan = if (statistik.soalDikerjakan < AMBANG_TAMPILKAN_PERSEN) {
                    "setelah $AMBANG_TAMPILKAN_PERSEN soal"
                } else {
                    null
                },
                modifier = Modifier.weight(1f),
            )
            KotakStatistik(
                label = "HARI",
                nilai = "${statistik.hariBerjalan}",
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(Ukuran.antarBagian))

        Text("AKTIVITAS TERAKHIR", style = Teks.label, color = OnBackgroundDim)
        Spacer(Modifier.height(12.dp))

        if (statistik.aktivitasTerakhir.isEmpty()) {
            Text(
                "Belum ada. Aktivitas muncul di sini setelah kamu melewati " +
                    "gerbang pertama.",
                style = Teks.caption,
                color = OnBackgroundDim,
            )
        } else {
            statistik.aktivitasTerakhir.forEach { BarisAktivitas(it) }
        }

        Spacer(Modifier.height(Ukuran.antarBagian))
    }
}

@Composable
private fun KartuKredit(sisaDetik: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Bentuk.kartu)
            .background(Surface)
            // Berbingkai kuningan, bukan Border biasa — ini elemen terpenting
            // di layar ini.
            .border(Ukuran.tebalGarisTombol, Accent, Bentuk.kartu)
            .padding(Ukuran.antarBagian),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("SISA KREDIT", style = Teks.label, color = OnBackgroundDim)
        Spacer(Modifier.height(12.dp))
        Text(formatSisaKredit(sisaDetik), style = Teks.angkaKredit, color = Accent)
        Spacer(Modifier.height(8.dp))
        Text(
            if (sisaDetik > 0) {
                "Media sosial terbuka."
            } else {
                "Gerbang menutup. Buka media sosial untuk mengerjakan soal."
            },
            style = Teks.caption,
            color = OnBackgroundDim,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun KotakStatistik(
    label: String,
    nilai: String,
    modifier: Modifier = Modifier,
    catatan: String? = null,
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(Bentuk.kartu)
            .background(Surface)
            .border(Ukuran.tebalGaris, Border, Bentuk.kartu)
            .padding(Ukuran.paddingKartu),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, style = Teks.label, color = OnBackgroundDim)
        Spacer(Modifier.height(8.dp))
        Text(nilai, style = Teks.judulLayar, color = OnBackground)
        if (catatan != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                catatan,
                style = Teks.caption,
                color = OnBackgroundDim,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun BarisAktivitas(baris: LogJawabanEntity) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            if (baris.isCorrect) "✓" else "✕",
            style = Teks.isi,
            color = if (baris.isCorrect) Accent else Emphasis,
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(namaSubtes(baris.subtest), style = Teks.isi, color = OnBackground)
            Text(
                "${baris.durationSeconds} detik · ${baris.itemId}",
                style = Teks.caption,
                color = OnBackgroundDim,
            )
        }
        if (baris.creditEarnedSeconds > 0) {
            Text(
                "+${baris.creditEarnedSeconds / 60}m",
                style = Teks.isi,
                color = Accent,
            )
        }
    }
}

/** Navigasi bawah. Dua tujuan saja, jadi tidak perlu pustaka navigasi. */
@Composable
fun NavigasiBawah(
    diBeranda: Boolean,
    onBeranda: () -> Unit,
    onPengaturan: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Surface)
            .padding(vertical = 12.dp),
    ) {
        ItemNavigasi("BERANDA", diBeranda, onBeranda, Modifier.weight(1f))
        ItemNavigasi("PENGATURAN", !diBeranda, onPengaturan, Modifier.weight(1f))
    }
}

@Composable
private fun ItemNavigasi(
    teks: String,
    aktif: Boolean,
    onKlik: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.clickable(onClick = onKlik).padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(teks, style = Teks.label, color = if (aktif) Accent else OnBackgroundDim)
        Spacer(Modifier.height(6.dp))
        Column(
            Modifier
                .width(32.dp)
                .height(Ukuran.stripKilau)
                .background(if (aktif) Accent else Surface),
        ) {}
    }
}

@Preview(name = "Layar 9 — Home", heightDp = 900)
@Composable
private fun PratinjauHome() {
    SoalduluTheme {
        LayarHome(
            statistik = StatistikHome(
                kodeResponden = "R3",
                sisaKreditDetik = 742,
                soalDikerjakan = 24,
                jumlahBenar = 17,
                hariBerjalan = 3,
                aktivitasTerakhir = listOf(
                    LogJawabanEntity(
                        id = 1, respondentCode = "R3", packageVersion = "1.0.0",
                        gateSessionId = 1L, urutanDalamGerbang = 1, itemId = "itm-0004",
                        subtest = "LITERASI_BAHASA_INDONESIA", selectedOptionId = "B",
                        isCorrect = true, durationSeconds = 41, creditEarnedSeconds = 180,
                        triggeredByPackage = "com.instagram.android",
                        timestamp = System.currentTimeMillis(),
                    ),
                ),
            ),
        )
    }
}
