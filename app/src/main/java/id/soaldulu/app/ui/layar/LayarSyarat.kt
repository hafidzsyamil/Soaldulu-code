package id.soaldulu.app.ui.layar

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import id.soaldulu.app.kirim.StatusKirim
import id.soaldulu.app.ui.theme.SoalduluTheme
import id.soaldulu.app.ui.theme.Ukuran

/**
 * Terms and Condition.
 *
 * Isinya harus jujur. Sejak revisi 22 September aplikasi menyimpan nama
 * responden dan menyertakannya di setiap baris log, jadi teks ini TIDAK
 * boleh lagi menjanjikan anonimitas.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LayarSyarat(
    onKembali: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Terms and Condition") },
                navigationIcon = {
                    IconButton(onClick = onKembali) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Ukuran.marginLayar),
        ) {
            Bagian(
                "Apa ini",
                "Soaldulu adalah aplikasi penelitian karya tulis ilmiah tentang " +
                    "pengurangan distraksi digital pada siswa kelas XII. Aplikasi ini " +
                    "tidak dipublikasikan dan tidak dimonetisasi.",
            )
            Bagian(
                "Yang dicatat aplikasi",
                "Nama yang kamu masukkan di awal. Nama aplikasi yang kamu buka dan " +
                    "berapa lama. Jawaban soal beserta benar atau salahnya dan berapa " +
                    "lama kamu menjawab. Kredit waktu yang kamu peroleh dan kamu pakai.",
            )
            Bagian(
                "Nama kamu ikut tercatat",
                "Nama yang kamu masukkan disertakan di setiap baris catatan, dan ikut " +
                    "terbawa ketika catatan itu dikirim ke peneliti. Kalau kamu tidak " +
                    "ingin namamu tercatat, isi dengan nama samaran atau kode yang " +
                    "sudah disepakati dengan peneliti.",
            )
            Bagian(
                "Yang tidak dicatat",
                "Isi chat, foto, kontak, dan lokasi tidak pernah dibaca aplikasi ini. " +
                    "Tidak ada email atau nomor telepon yang diminta. Tidak ada foto " +
                    "yang diunggah — avatar hanya lingkaran berwarna.",
            )
            // Teksnya mengikuti APK yang benar-benar dipegang responden: kalau
            // tujuan pengiriman tidak disetel saat APK dibangun, aplikasi ini
            // memang tidak mengirim apa pun, dan janji sebaliknya akan bohong.
            if (StatusKirim.disetel) {
                Bagian(
                    "Catatan dikirim ke peneliti",
                    "Dengan menyetujui syarat ini, kamu setuju catatan di atas " +
                        "dikirim ke peneliti lewat internet, otomatis dan berkala, " +
                        "tanpa perlu kamu kirim sendiri. Yang terkirim adalah nama " +
                        "yang kamu isi, ringkasan pemakaian, dan seluruh catatan " +
                        "jawaban — sama persis dengan yang bisa kamu lihat sendiri " +
                        "lewat tombol ekspor di Settings. Isi chat, foto, kontak, " +
                        "dan lokasi tidak pernah ikut.",
                )
                Bagian(
                    "Di mana data disimpan",
                    "Catatan tersimpan di HP-mu sendiri, dan salinannya disimpan " +
                        "di penyimpanan tertutup milik peneliti yang hanya bisa " +
                        "dibuka peneliti. Gerbang soal, bank soal, dan kredit tetap " +
                        "berjalan penuh tanpa internet; internet hanya dipakai " +
                        "untuk mengirim catatan itu.",
                )
            } else {
                Bagian(
                    "Di mana data disimpan",
                    "Seluruh catatan tersimpan di HP-mu sendiri dan tidak dikirim " +
                        "ke mana pun secara otomatis. Aplikasi berjalan penuh tanpa " +
                        "internet. Catatan baru berpindah ke peneliti kalau kamu " +
                        "sendiri yang mengekspor dan mengirimkannya.",
                )
            }
            Bagian(
                "Kamu boleh berhenti",
                "Kamu boleh berhenti kapan saja tanpa konsekuensi apa pun. Cukup " +
                    "cabut izin lewat pengaturan HP atau hapus aplikasinya." +
                    if (StatusKirim.disetel) {
                        " Pengiriman catatan juga bisa kamu matikan sendiri kapan " +
                            "saja lewat Settings, dan catatan yang belum terkirim " +
                            "tidak akan dikirim setelah itu."
                    } else {
                        ""
                    },
            )
            Spacer(Modifier.height(48.dp))
        }
    }
}

@Composable
private fun Bagian(judul: String, isi: String) {
    Text(
        judul,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 24.dp, bottom = 6.dp),
    )
    Text(
        isi,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onBackground,
    )
}

@Preview(name = "Terms and Condition", heightDp = 900)
@Composable
private fun PratinjauSyarat() {
    SoalduluTheme(paksaGelap = true) { LayarSyarat(onKembali = {}) }
}
