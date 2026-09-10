package id.soaldulu.app.ui.layar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import id.soaldulu.app.data.BacaanEntity
import id.soaldulu.app.data.ButirEntity
import id.soaldulu.app.data.OpsiEntity
import id.soaldulu.app.data.SoalLengkap
import id.soaldulu.app.ui.LencanaOpsi
import id.soaldulu.app.ui.theme.Accent
import id.soaldulu.app.ui.theme.Background
import id.soaldulu.app.ui.theme.Bentuk
import id.soaldulu.app.ui.theme.Border
import id.soaldulu.app.ui.theme.OnBackground
import id.soaldulu.app.ui.theme.OnBackgroundDim
import id.soaldulu.app.ui.theme.SoalduluTheme
import id.soaldulu.app.ui.theme.Surface
import id.soaldulu.app.ui.theme.Teks
import id.soaldulu.app.ui.theme.Ukuran

/**
 * Layar 6 — Gerbang Soal (handoff Bagian 8 nomor 6).
 *
 * Layar yang paling sering dilihat responden, sekitar 200 kali. Karena itu
 * sengaja paling polos di seluruh aplikasi: tanpa ornamen, tanpa animasi,
 * dan TANPA tombol keluar — satu-satunya jalan maju adalah menjawab.
 *
 * Menyentuh opsi langsung mengirim jawaban; tidak ada tombol konfirmasi,
 * sesuai daftar elemen di Bagian 8 nomor 6.
 */
@Composable
fun LayarGerbang(
    soal: SoalLengkap,
    nomorSoal: Int,
    totalSoal: Int,
    sisaKreditDetik: Int,
    onJawab: (optionId: String) -> Unit,
    onLapor: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Ukuran.marginLayar, vertical = Ukuran.antarBagian),
    ) {
        BilahAtas(
            nomorSoal = nomorSoal,
            totalSoal = totalSoal,
            sisaKreditDetik = sisaKreditDetik,
            subtest = soal.butir.subtest,
        )

        Spacer(Modifier.height(Ukuran.antarBagian))

        soal.bacaan?.let { PanelBacaan(it) }

        Text(soal.butir.stem, style = Teks.batangSoal, color = OnBackground)

        Spacer(Modifier.height(Ukuran.antarBagian))

        soal.opsi.forEach { opsi ->
            BarisOpsi(opsi = opsi, terpilih = false, onKlik = { onJawab(opsi.optionId) })
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(Ukuran.antarBagian))

        Text(
            "Laporkan soal ini",
            style = Teks.caption.copy(textDecoration = TextDecoration.Underline),
            color = OnBackgroundDim,
            modifier = Modifier.clickable(onClick = onLapor),
        )

        Spacer(Modifier.height(Ukuran.antarBagian))
    }
}

@Composable
private fun BilahAtas(
    nomorSoal: Int,
    totalSoal: Int,
    sisaKreditDetik: Int,
    subtest: String,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text("SISA KREDIT", style = Teks.label, color = OnBackgroundDim)
                Spacer(Modifier.height(4.dp))
                Text(formatSisaKredit(sisaKreditDetik), style = Teks.isi, color = Accent)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "SOAL $nomorSoal DARI $totalSoal",
                    style = Teks.label,
                    color = OnBackgroundDim,
                )
                Spacer(Modifier.height(4.dp))
                Text(namaSubtes(subtest), style = Teks.caption, color = OnBackgroundDim)
            }
        }
    }
}

@Composable
private fun PanelBacaan(bacaan: BacaanEntity) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            // Bacaan bisa panjang. Dibatasi tingginya supaya batang soal dan
            // opsi tidak terdorong keluar layar; isinya digulir sendiri.
            .heightIn(max = 260.dp)
            .clip(Bentuk.kartu)
            .background(Surface)
            .border(Ukuran.tebalGaris, Border, Bentuk.kartu)
            .verticalScroll(rememberScrollState())
            .padding(Ukuran.paddingKartu),
    ) {
        Text(bacaan.title, style = Teks.label, color = Accent)
        Spacer(Modifier.height(12.dp))
        Text(bacaan.text, style = Teks.isi, color = OnBackground)
    }
}

@Composable
private fun BarisOpsi(
    opsi: OpsiEntity,
    terpilih: Boolean,
    onKlik: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Ukuran.tinggiBarisPilihan)
            .clip(Bentuk.kartu)
            .background(if (terpilih) Surface else Color.Transparent)
            .border(
                Ukuran.tebalGaris,
                if (terpilih) Accent else Border,
                Bentuk.kartu,
            )
            .clickable(onClick = onKlik)
            .padding(Ukuran.paddingKartu),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LencanaOpsi(huruf = opsi.optionId, terpilih = terpilih)
        Spacer(Modifier.width(12.dp))
        Text(opsi.text, style = Teks.opsi, color = OnBackground)
    }
}

/** mm:ss. Kredit tidak pernah ditampilkan sebagai angka negatif. */
fun formatSisaKredit(detik: Int): String {
    val aman = detik.coerceAtLeast(0)
    return "%02d:%02d".format(aman / 60, aman % 60)
}

/**
 * Ubah nilai enum subtes jadi teks yang bisa dibaca.
 *
 * LITERASI_BAHASA_INDONESIA -> Literasi Bahasa Indonesia
 *
 * Ini murni pemformatan tampilan. Nilai aslinya yang disimpan di log tetap
 * apa adanya dari JSON — daftar subtes belum diverifikasi ke kisi-kisi resmi
 * (handoff Bagian 10), jadi tidak ada nama yang dikarang di sini.
 */
fun namaSubtes(subtest: String): String =
    subtest.split("_")
        .filter { it.isNotBlank() }
        .joinToString(" ") { kata ->
            kata.lowercase().replaceFirstChar { it.uppercase() }
        }

@Preview(name = "Layar 6 — Gerbang Soal", heightDp = 900)
@Composable
private fun PratinjauGerbang() {
    val butir = ButirEntity(
        id = "itm-0001",
        active = true,
        exam = "BOTH",
        subtest = "LITERASI_BAHASA_INDONESIA",
        passageId = "psg-0001",
        stem = "Gagasan utama paragraf kedua adalah…",
        correctOptionId = "B",
        explanation = "",
        cognitiveLevel = "C4",
        estimatedSeconds = 75,
        sourceType = "PLACEHOLDER",
        sourceReference = "",
    )
    SoalduluTheme {
        LayarGerbang(
            soal = SoalLengkap(
                butir = butir,
                opsi = listOf("A", "B", "C", "D").mapIndexed { i, huruf ->
                    OpsiEntity(
                        itemId = butir.id,
                        optionId = huruf,
                        text = "Pilihan jawaban $huruf",
                        urutan = i,
                    )
                },
                bacaan = BacaanEntity(
                    id = "psg-0001",
                    title = "Notifikasi dan Konsentrasi Belajar",
                    text = "Paragraf pertama.\n\nParagraf kedua.",
                    sourceType = "PLACEHOLDER",
                    sourceReference = "",
                ),
            ),
            nomorSoal = 1,
            totalSoal = 3,
            sisaKreditDetik = 0,
            onJawab = {},
            onLapor = {},
        )
    }
}
