package id.soaldulu.app.ui.layar

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import id.soaldulu.app.ui.TombolPrimer
import id.soaldulu.app.ui.theme.Background
import id.soaldulu.app.ui.theme.OnBackground
import id.soaldulu.app.ui.theme.OnBackgroundDim
import id.soaldulu.app.ui.theme.Teks
import id.soaldulu.app.ui.theme.Ukuran

/** Satu jawaban yang sudah diberikan dalam gerbang ini. */
data class JawabanGerbang(
    val soal: SoalLengkap,
    val opsiDipilih: OpsiEntity,
    val benar: Boolean,
    val durasiDetik: Int,
    val bonusDetik: Int,
)

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

    var indeks by remember { mutableIntStateOf(0) }
    var menampilkanUmpanBalik by remember { mutableStateOf(false) }
    // Dipakai menghitung durationSeconds tiap jawaban — kolom log yang
    // membedakan responden yang berpikir dari yang asal tekan.
    var mulaiSoalPada by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    val jawaban = remember { mutableStateListOf<JawabanGerbang>() }

    val soalSekarang = soal[indeks]
    val soalTerakhir = indeks == soal.lastIndex

    if (!menampilkanUmpanBalik) {
        LayarGerbang(
            soal = soalSekarang,
            nomorSoal = indeks + 1,
            totalSoal = soal.size,
            sisaKreditDetik = sisaKreditDetik,
            onJawab = { optionId ->
                val durasi =
                    ((SystemClock.elapsedRealtime() - mulaiSoalPada) / 1000L).toInt()
                val benar = optionId == soalSekarang.butir.correctOptionId
                jawaban += JawabanGerbang(
                    soal = soalSekarang,
                    opsiDipilih = soalSekarang.opsi.first { it.optionId == optionId },
                    benar = benar,
                    durasiDetik = durasi,
                    bonusDetik = Kredit.bonusSatuJawaban(benar, durasi),
                )
                menampilkanUmpanBalik = true
            },
            onLapor = { onLapor(soalSekarang.butir.id) },
            modifier = modifier,
        )
    } else {
        val terakhir = jawaban.last()
        LayarUmpanBalik(
            benar = terakhir.benar,
            opsiDipilih = terakhir.opsiDipilih,
            opsiBenar = soalSekarang.opsi.first {
                it.optionId == soalSekarang.butir.correctOptionId
            },
            pembahasan = soalSekarang.butir.explanation,
            kreditDidapatDetik = terakhir.bonusDetik,
            // Kredit yang akan diterima kalau gerbang ini diselesaikan:
            // dasar + seluruh bonus yang sudah terkumpul.
            totalKreditGerbangDetik = Kredit.totalGerbang(jawaban.sumOf { it.bonusDetik }),
            soalTerakhir = soalTerakhir,
            onLanjut = {
                if (soalTerakhir) {
                    onSelesai(jawaban.toList())
                } else {
                    indeks += 1
                    menampilkanUmpanBalik = false
                    mulaiSoalPada = SystemClock.elapsedRealtime()
                }
            },
            modifier = modifier,
        )
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
            .background(Background)
            .padding(Ukuran.marginLayar),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Bank soal belum siap", style = Teks.judulLayar, color = OnBackground)
        Spacer(Modifier.height(16.dp))
        Text(
            "Tidak ada butir aktif yang bisa ditampilkan, jadi gerbang " +
                "dilewati kali ini. Beri tahu peneliti.",
            style = Teks.isi,
            color = OnBackgroundDim,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Ukuran.antarBagian))
        TombolPrimer("Tutup", onClick = onTutup)
    }
}

