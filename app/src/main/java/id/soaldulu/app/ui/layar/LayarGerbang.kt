package id.soaldulu.app.ui.layar

import androidx.compose.foundation.background
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import id.soaldulu.app.data.BacaanEntity
import id.soaldulu.app.data.ButirEntity
import id.soaldulu.app.data.OpsiEntity
import id.soaldulu.app.data.SoalLengkap
import id.soaldulu.app.ui.theme.GayaBatangSoal
import id.soaldulu.app.ui.theme.SoalduluTheme
import id.soaldulu.app.ui.theme.Ukuran

/**
 * Layar Gerbang.
 *
 * Layar yang paling sering dilihat responden. Tanpa tombol keluar — satu-satunya
 * jalan maju adalah menjawab. Menyentuh opsi langsung mengirim jawaban.
 *
 * Jumlah tombol opsi mengikuti isi bank soal, bukan angka tetap: berkas desain
 * menggambar lima, tapi butir sungguhan punya empat opsi A sampai D.
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
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = Ukuran.marginLayar),
    ) {
        Spacer(Modifier.height(Ukuran.antarBagian))

        Text(
            "Credit Left : ${formatKreditPanjang(sisaKreditDetik)}",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(16.dp))

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                Modifier.padding(horizontal = Ukuran.paddingKartu, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    namaSubtes(soal.butir.subtest),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "$nomorSoal / $totalSoal",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }

        // Bagian atas menggulir, blok opsi menempel di bawah supaya terjangkau
        // jempol tanpa memindahkan tangan.
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(top = 20.dp),
        ) {
            soal.bacaan?.let { bacaan ->
                Text(
                    bacaan.title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    bacaan.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(24.dp))
            }

            Text(
                soal.butir.stem,
                style = GayaBatangSoal,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Spacer(Modifier.height(20.dp))
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(bottom = 16.dp),
        ) {
            soal.opsi.forEach { opsi ->
                TombolOpsi(opsi = opsi, onKlik = { onJawab(opsi.optionId) })
            }

            Spacer(Modifier.height(4.dp))

            Text(
                "Laporkan soal ini",
                style = MaterialTheme.typography.bodySmall.copy(
                    textDecoration = TextDecoration.Underline,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clickable(onClick = onLapor)
                    .padding(vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun TombolOpsi(opsi: OpsiEntity, onKlik: () -> Unit) {
    Card(
        onClick = onKlik,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Ukuran.tinggiBarisPilihan),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = Ukuran.tinggiBarisPilihan)
                .padding(horizontal = Ukuran.paddingKartu, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                opsi.optionId,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(14.dp))
            Text(opsi.text, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

/** mm:ss. Kredit tidak pernah ditampilkan sebagai angka negatif. */
fun formatSisaKredit(detik: Int): String {
    val aman = detik.coerceAtLeast(0)
    return "%02d:%02d".format(aman / 60, aman % 60)
}

/**
 * Ubah nilai enum subtes jadi teks yang bisa dibaca.
 * LITERASI_BAHASA_INDONESIA menjadi Literasi Bahasa Indonesia.
 *
 * Murni pemformatan tampilan; nilai yang disimpan di log tetap apa adanya
 * dari JSON.
 */
fun namaSubtes(subtest: String): String =
    subtest.split("_")
        .filter { it.isNotBlank() }
        .joinToString(" ") { kata -> kata.lowercase().replaceFirstChar { it.uppercase() } }

@Preview(name = "Gerbang Soal", heightDp = 860)
@Composable
private fun PratinjauGerbang() {
    val butir = ButirEntity(
        id = "itm-0001",
        active = true,
        exam = "BOTH",
        subtest = "LITERASI_BAHASA_INDONESIA",
        passageId = "psg-0001",
        stem = "Gagasan utama paragraf kedua bacaan tersebut adalah…",
        correctOptionId = "B",
        explanation = "",
        cognitiveLevel = "C4",
        estimatedSeconds = 75,
        sourceType = "PLACEHOLDER",
        sourceReference = "",
    )
    SoalduluTheme(paksaGelap = true) {
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
