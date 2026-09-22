package id.soaldulu.app.ui.layar

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import id.soaldulu.app.GateConfig
import id.soaldulu.app.data.OpsiEntity
import id.soaldulu.app.ui.theme.SoalduluTheme
import id.soaldulu.app.ui.theme.Ukuran
import id.soaldulu.app.ui.theme.WarnaTambah

/**
 * Umpan balik setelah satu jawaban.
 *
 * Satu composable untuk dua keadaan; yang berbeda hanya isi panel dan warna
 * penandanya. Tombolnya selalu warna utama, tidak pernah merah — salah bukan
 * akhir, dan tombol merah membuatnya terasa seperti hukuman.
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
    /** Teks tombol keluar sebelum soal habis, misalnya "Buka TikTok". null = tidak ada. */
    labelKeluar: String? = null,
    onKeluar: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Ukuran.marginLayar, vertical = Ukuran.antarBagian),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(if (benar) WarnaTambah.positif else WarnaTambah.negatif),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (benar) Icons.Default.Check else Icons.Default.Close,
                contentDescription = null,
                tint = Color(0xFF16222A),
                modifier = Modifier.size(44.dp),
            )
        }

        Spacer(Modifier.height(20.dp))

        Text(
            if (benar) "Benar" else "Belum tepat",
            style = MaterialTheme.typography.headlineMedium,
            // Sengaja bukan warna merah: merah di atas latar gelap kontrasnya
            // buruk, dan warnanya sudah dibawa lingkaran penanda di atas.
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(Modifier.height(6.dp))

        // Benar tapi tanpa kredit hanya punya satu sebab: dijawab lebih cepat
        // dari MIN_GENUINE_ANSWER_SECONDS. Tanpa penjelasan, "+0 menit"
        // setelah "Benar" terlihat seperti aplikasinya rusak.
        val terlaluCepat = benar && kreditDidapatDetik == 0

        Text(
            "+${kreditDidapatDetik / 60} menit kredit",
            style = MaterialTheme.typography.bodyLarge,
            color = if (benar && !terlaluCepat) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )

        if (terlaluCepat) {
            Spacer(Modifier.height(6.dp))
            Text(
                "Dijawab kurang dari ${GateConfig.MIN_GENUINE_ANSWER_SECONDS} detik, jadi " +
                    "dianggap menebak dan tidak mendapat kredit. Baca soalnya dulu.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(Modifier.height(Ukuran.antarBagian))

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(Ukuran.paddingKartu)) {
                if (!benar) {
                    Text(
                        "JAWABANMU",
                        style = MaterialTheme.typography.labelMedium,
                        color = WarnaTambah.negatif,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${opsiDipilih.optionId}. ${opsiDipilih.text}",
                        style = MaterialTheme.typography.bodyMedium,
                    )

                    Spacer(Modifier.height(16.dp))

                    Text(
                        "JAWABAN TEPAT",
                        style = MaterialTheme.typography.labelMedium,
                        color = WarnaTambah.positif,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${opsiBenar.optionId}. ${opsiBenar.text}",
                        style = MaterialTheme.typography.bodyLarge,
                    )

                    Spacer(Modifier.height(16.dp))
                }

                Text(
                    "PEMBAHASAN",
                    style = MaterialTheme.typography.labelMedium,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    pembahasan.ifBlank { "Tidak ada pembahasan untuk butir ini." },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        Spacer(Modifier.height(Ukuran.antarBagian))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "Total kredit sekarang",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                formatSisaKredit(totalKreditGerbangDetik),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Spacer(Modifier.height(Ukuran.antarBagian))

        Button(
            onClick = onLanjut,
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) {
            Text(
                when {
                    // "Soal berikutnya" akan bohong di soal terakhir.
                    soalTerakhir -> "Selesai"
                    benar -> "Lanjut"
                    else -> "Soal berikutnya"
                },
                style = MaterialTheme.typography.titleMedium,
            )
        }

        if (labelKeluar != null) {
            Spacer(Modifier.height(12.dp))
            // Sekunder: mengerjakan soal berikutnya tetap jalur utama.
            OutlinedButton(
                onClick = onKeluar,
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) {
                Text(labelKeluar, style = MaterialTheme.typography.titleMedium)
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Preview(name = "Umpan balik — benar", heightDp = 820)
@Composable
private fun PratinjauBenar() {
    SoalduluTheme(paksaGelap = true) {
        LayarUmpanBalik(
            benar = true,
            opsiDipilih = OpsiEntity(itemId = "i", optionId = "B", text = "Pilihan B", urutan = 1),
            opsiBenar = OpsiEntity(itemId = "i", optionId = "B", text = "Pilihan B", urutan = 1),
            pembahasan = "Paragraf kedua membahas dampak notifikasi terhadap konsentrasi.",
            kreditDidapatDetik = 180,
            totalKreditGerbangDetik = 780,
            soalTerakhir = false,
            onLanjut = {},
        )
    }
}

@Preview(name = "Umpan balik — salah", heightDp = 820)
@Composable
private fun PratinjauSalah() {
    SoalduluTheme(paksaGelap = false) {
        LayarUmpanBalik(
            benar = false,
            opsiDipilih = OpsiEntity(itemId = "i", optionId = "C", text = "Pilihan C", urutan = 2),
            opsiBenar = OpsiEntity(itemId = "i", optionId = "B", text = "Pilihan B", urutan = 1),
            pembahasan = "Paragraf kedua membahas dampak notifikasi terhadap konsentrasi.",
            kreditDidapatDetik = 0,
            totalKreditGerbangDetik = 600,
            soalTerakhir = false,
            onLanjut = {},
        )
    }
}
