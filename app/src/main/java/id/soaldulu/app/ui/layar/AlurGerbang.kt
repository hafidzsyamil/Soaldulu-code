package id.soaldulu.app.ui.layar

import android.os.SystemClock
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import id.soaldulu.app.Kredit
import id.soaldulu.app.data.OpsiEntity
import id.soaldulu.app.data.SoalLengkap
import id.soaldulu.app.ui.theme.Arah
import id.soaldulu.app.ui.theme.Ukuran
import id.soaldulu.app.ui.theme.transisiLayar

/** Satu jawaban yang sudah diberikan dalam gerbang ini. */
data class JawabanGerbang(
    val soal: SoalLengkap,
    val opsiDipilih: OpsiEntity,
    val benar: Boolean,
    val durasiDetik: Int,
    val bonusDetik: Int,
)

/** Posisi di dalam gerbang: soal ke-berapa, dan apakah umpan baliknya yang tampil. */
private data class Langkah(val indeks: Int, val umpanBalik: Boolean)

/**
 * Menggerakkan satu gerbang: soal → umpan balik → soal berikutnya, sampai
 * QUESTIONS_PER_GATE terpenuhi.
 *
 * Composable ini yang berjalan di dalam overlay, jadi ia tidak punya
 * Activity dan tidak boleh bergantung pada apa pun milik Activity.
 */
@Composable
fun AlurGerbang(
    soal: List<SoalLengkap>,
    sisaKreditDetik: Int,
    onSelesai: (List<JawabanGerbang>) -> Unit,
    onLapor: (itemId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Katup pengaman. Overlay gerbang tidak punya tombol keluar, jadi kalau
    // daftar soal kosong (bank belum di-seed, atau seluruh butir nonaktif)
    // responden akan terkunci di layar kosong. Ini tambahan di luar
    // spesifikasi dan sengaja ada.
    if (soal.isEmpty()) {
        GerbangTanpaSoal(onTutup = { onSelesai(emptyList()) }, modifier = modifier)
        return
    }

    var langkah by remember { mutableStateOf(Langkah(indeks = 0, umpanBalik = false)) }
    // Dipakai menghitung durationSeconds tiap jawaban — kolom log yang
    // membedakan responden yang berpikir dari yang asal tekan.
    var mulaiSoalPada by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    val jawaban = remember { mutableStateListOf<JawabanGerbang>() }
    var sudahSelesai by remember { mutableStateOf(false) }

    AnimatedContent(
        targetState = langkah,
        transitionSpec = { transisiLayar(Arah.MAJU) },
        modifier = modifier,
        label = "gerbang",
    ) { ini ->
        // Selama transisi, langkah yang sedang pergi masih tergambar dan
        // tombolnya masih bisa disentuh. Karena itu setiap aksi di bawah
        // memeriksa `ini == langkah` pada saat ditekan: ketukan ganda yang
        // cepat tidak boleh mencatat satu soal dua kali atau melompati soal
        // — keduanya merusak data penelitian.
        val soalIni = soal[ini.indeks]
        val soalTerakhir = ini.indeks == soal.lastIndex

        if (!ini.umpanBalik) {
            LayarGerbang(
                soal = soalIni,
                nomorSoal = ini.indeks + 1,
                totalSoal = soal.size,
                sisaKreditDetik = sisaKreditDetik,
                onJawab = { optionId ->
                    if (ini == langkah) {
                        val durasi =
                            ((SystemClock.elapsedRealtime() - mulaiSoalPada) / 1000L).toInt()
                        val benar = optionId == soalIni.butir.correctOptionId
                        jawaban += JawabanGerbang(
                            soal = soalIni,
                            opsiDipilih = soalIni.opsi.first { it.optionId == optionId },
                            benar = benar,
                            durasiDetik = durasi,
                            bonusDetik = Kredit.bonusSatuJawaban(benar, durasi),
                        )
                        langkah = ini.copy(umpanBalik = true)
                    }
                },
                onLapor = { onLapor(soalIni.butir.id) },
            )
        } else {
            // Diambil menurut nomor soal, bukan jawaban terakhir: selama
            // transisi, umpan balik soal sebelumnya masih tergambar.
            val jawabanIni = jawaban[ini.indeks]
            LayarUmpanBalik(
                benar = jawabanIni.benar,
                opsiDipilih = jawabanIni.opsiDipilih,
                opsiBenar = soalIni.opsi.first {
                    it.optionId == soalIni.butir.correctOptionId
                },
                pembahasan = soalIni.butir.explanation,
                kreditDidapatDetik = jawabanIni.bonusDetik,
                // Kredit yang akan diterima kalau gerbang ini diselesaikan:
                // dasar + seluruh bonus yang sudah terkumpul sampai soal ini.
                totalKreditGerbangDetik = Kredit.totalGerbang(
                    jawaban.take(ini.indeks + 1).sumOf { it.bonusDetik },
                ),
                soalTerakhir = soalTerakhir,
                onLanjut = {
                    if (ini == langkah && !sudahSelesai) {
                        if (soalTerakhir) {
                            sudahSelesai = true
                            onSelesai(jawaban.toList())
                        } else {
                            langkah = Langkah(indeks = ini.indeks + 1, umpanBalik = false)
                            mulaiSoalPada = SystemClock.elapsedRealtime()
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun GerbangTanpaSoal(
    onTutup: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(Ukuran.marginLayar),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "Bank soal belum siap",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "Tidak ada butir aktif yang bisa ditampilkan, jadi gerbang " +
                "dilewati kali ini. Beri tahu peneliti.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Ukuran.antarBagian))
        Button(onClick = onTutup) { Text("Tutup") }
    }
}

