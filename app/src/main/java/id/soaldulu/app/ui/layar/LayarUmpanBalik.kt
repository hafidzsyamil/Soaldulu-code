package id.soaldulu.app.ui.layar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import id.soaldulu.app.data.OpsiEntity
import id.soaldulu.app.ui.Kartu
import id.soaldulu.app.ui.TombolPrimer
import id.soaldulu.app.ui.theme.Accent
import id.soaldulu.app.ui.theme.Background
import id.soaldulu.app.ui.theme.Emphasis
import id.soaldulu.app.ui.theme.OnAccent
import id.soaldulu.app.ui.theme.OnBackground
import id.soaldulu.app.ui.theme.OnBackgroundDim
import id.soaldulu.app.ui.theme.SoalduluTheme
import id.soaldulu.app.ui.theme.Teks
import id.soaldulu.app.ui.theme.Ukuran

/**
 * Layar 7 dan 8 — Umpan Balik (handoff Bagian 8 nomor 7 dan 8).
 *
 * Satu composable untuk dua keadaan karena strukturnya sama; yang berbeda
 * hanya isi panel dan warna penandanya.
 *
 * Dua aturan yang mudah dilanggar dan sengaja ditulis di sini:
 * - Kata "Belum tepat" TIDAK berwarna crimson. Crimson di atas latar gelap
 *   kontrasnya buruk; crimson hanya dipakai untuk lingkaran penanda dan
 *   label "JAWABANMU".
 * - Tombolnya tetap kuningan, tidak pernah crimson. Salah bukan akhir.
 */
@Composable
fun LayarUmpanBalik(
    benar: Boolean,
    opsiDipilih: OpsiEntity,
    opsiBenar: OpsiEntity,
    pembahasan: String,
    kreditDidapatDetik: Int,
    totalKreditGerbangDetik: Int,
    soalTerakhir: Boolean,
    onLanjut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Ukuran.marginLayar, vertical = Ukuran.antarBagian),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PenandaHasil(benar)

        Spacer(Modifier.height(Ukuran.antarBagian))

        Text(
            if (benar) "Benar" else "Belum tepat",
            style = Teks.verdict,
            // Parchment, bukan crimson — lihat catatan di atas.
            color = if (benar) Accent else OnBackground,
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "+${kreditDidapatDetik / 60} menit kredit",
            style = Teks.isi,
            color = if (benar) Accent else OnBackgroundDim,
        )

        Spacer(Modifier.height(Ukuran.antarBagian))

        Kartu {
            if (!benar) {
                Text("JAWABANMU", style = Teks.label, color = Emphasis)
                Spacer(Modifier.height(6.dp))
                Text(
                    "${opsiDipilih.optionId}. ${opsiDipilih.text}",
                    style = Teks.opsi,
                    color = OnBackgroundDim,
                )

                Spacer(Modifier.height(Ukuran.paddingKartu))

                Text("JAWABAN TEPAT", style = Teks.label, color = Accent)
                Spacer(Modifier.height(6.dp))
                Text(
                    "${opsiBenar.optionId}. ${opsiBenar.text}",
                    style = Teks.opsi,
                    color = OnBackground,
                )

                Spacer(Modifier.height(Ukuran.paddingKartu))
            }

            Text("PEMBAHASAN", style = Teks.label, color = Accent)
            Spacer(Modifier.height(6.dp))
            Text(
                pembahasan.ifBlank { "Tidak ada pembahasan untuk butir ini." },
                style = Teks.isi,
                color = OnBackground,
            )
        }

        Spacer(Modifier.height(Ukuran.antarBagian))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Total kredit sekarang", style = Teks.isi, color = OnBackgroundDim)
            Text(formatSisaKredit(totalKreditGerbangDetik), style = Teks.isi, color = Accent)
        }

        Spacer(Modifier.height(Ukuran.antarBagian))

        TombolPrimer(
            teks = when {
                // "Soal berikutnya" akan bohong di soal terakhir — tidak ada
                // soal berikutnya, gerbang langsung terbuka.
                soalTerakhir -> "Selesai"
                benar -> "Lanjut"
                else -> "Soal berikutnya"
            },
            onClick = onLanjut,
        )

        Spacer(Modifier.height(Ukuran.antarBagian))
    }
}

@Composable
private fun PenandaHasil(benar: Boolean) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(if (benar) Accent else Emphasis)
            .border(Ukuran.tebalGarisTombol, if (benar) Accent else Emphasis, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            if (benar) "✓" else "✕",
            style = Teks.verdict,
            color = if (benar) OnAccent else OnBackground,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(name = "Layar 7 — Benar", heightDp = 800)
@Composable
private fun PratinjauBenar() {
    SoalduluTheme {
        LayarUmpanBalik(
            benar = true,
            opsiDipilih = OpsiEntity(itemId = "itm-0001", optionId = "B", text = "Pilihan B", urutan = 1),
            opsiBenar = OpsiEntity(itemId = "itm-0001", optionId = "B", text = "Pilihan B", urutan = 1),
            pembahasan = "Paragraf kedua membahas dampak notifikasi terhadap konsentrasi.",
            kreditDidapatDetik = 180,
            totalKreditGerbangDetik = 780,
            soalTerakhir = false,
            onLanjut = {},
        )
    }
}

@Preview(name = "Layar 8 — Belum tepat", heightDp = 800)
@Composable
private fun PratinjauSalah() {
    SoalduluTheme {
        LayarUmpanBalik(
            benar = false,
            opsiDipilih = OpsiEntity(itemId = "itm-0001", optionId = "C", text = "Pilihan C", urutan = 2),
            opsiBenar = OpsiEntity(itemId = "itm-0001", optionId = "B", text = "Pilihan B", urutan = 1),
            pembahasan = "Paragraf kedua membahas dampak notifikasi terhadap konsentrasi.",
            kreditDidapatDetik = 0,
            totalKreditGerbangDetik = 600,
            soalTerakhir = false,
            onLanjut = {},
        )
    }
}
