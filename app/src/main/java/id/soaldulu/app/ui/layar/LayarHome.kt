package id.soaldulu.app.ui.layar

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import id.soaldulu.app.ui.theme.SoalduluTheme
import id.soaldulu.app.ui.theme.Ukuran
import id.soaldulu.app.ui.theme.WarnaTambah

/** Angka-angka yang ditampilkan Dashboard. */
data class StatistikHome(
    val nama: String = "",
    val avatar: Int = 0,
    val sisaKreditDetik: Int = 0,
    val soalDikerjakan: Int = 0,
    val jumlahBenar: Int = 0,
    val jumlahSalah: Int = 0,
    /** Aplikasi yang paling sering memicu gerbang, beserta jumlahnya. */
    val paketPalingSering: String = "",
    val jumlahPemicu: Int = 0,
)

/**
 * Dashboard.
 *
 * Empat kartu statistik dan satu tombol. Tombol Kerjakan Soal membuka gerbang
 * atas kemauan sendiri, tanpa menunggu media sosial dibuka — ini perilaku baru
 * yang datang dari berkas desain, bukan dari dokumen handoff, dan perlu
 * dicatat sebagai variabel saat menganalisis data.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LayarHome(
    statistik: StatistikHome,
    onPengaturan: () -> Unit,
    menyegarkan: Boolean = false,
    onSegarkan: () -> Unit = {},
    onKerjakanSoal: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Dashboard") },
                actions = {
                    IconButton(onClick = onPengaturan) {
                        Icon(Icons.Default.Settings, contentDescription = "Pengaturan")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        // Tarik ke bawah untuk memuat ulang statistik. Isinya harus bisa
        // digulir supaya gesture tariknya sampai ke PullToRefreshBox.
        PullToRefreshBox(
            isRefreshing = menyegarkan,
            onRefresh = onSegarkan,
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Ukuran.marginLayar),
            ) {
                Row(
                    Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(Ukuran.jarakKartu),
                ) {
                    KartuStatistik("Total Soal", Modifier.weight(1f)) {
                        Text(
                            "${statistik.soalDikerjakan}",
                            style = MaterialTheme.typography.headlineMedium,
                        )
                        Text("soal dikerjakan", style = MaterialTheme.typography.bodySmall)
                    }
                    KartuStatistik("Credit Left", Modifier.weight(1f)) {
                        Text(
                            formatKreditPanjang(statistik.sisaKreditDetik),
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        Text(
                            if (statistik.sisaKreditDetik > 0) "media sosial terbuka" else "gerbang menutup",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }

                Spacer(Modifier.height(Ukuran.jarakKartu))

                Row(
                    Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(Ukuran.jarakKartu),
                ) {
                    KartuStatistik("False True", Modifier.weight(1f)) {
                        Text(
                            "True ${statistik.jumlahBenar}",
                            style = MaterialTheme.typography.titleMedium,
                            color = WarnaTambah.positif,
                        )
                        Text(
                            "False ${statistik.jumlahSalah}",
                            style = MaterialTheme.typography.titleMedium,
                            color = WarnaTambah.negatif,
                        )
                    }
                    KartuStatistik("Need Attention", Modifier.weight(1f)) {
                        if (statistik.paketPalingSering.isBlank()) {
                            Text("belum ada", style = MaterialTheme.typography.titleMedium)
                            Text("belum ada gerbang", style = MaterialTheme.typography.bodySmall)
                        } else {
                            Text(
                                namaAplikasi(statistik.paketPalingSering),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                "${statistik.jumlahPemicu} gerbang",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(Ukuran.antarBagian))

                Button(
                    onClick = onKerjakanSoal,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                ) {
                    Text("Kerjakan Soal", style = MaterialTheme.typography.titleMedium)
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    "Mengerjakan soal di sini menambah kredit tanpa menunggu media " +
                        "sosial dibuka.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(48.dp))
            }
        }
    }
}

@Composable
private fun KartuStatistik(
    judul: String,
    modifier: Modifier = Modifier,
    isi: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxHeight(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Column(Modifier.padding(Ukuran.paddingKartu)) {
            Text(judul, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(16.dp))
            isi()
        }
    }
}

/** 00h 00m 00s, seperti di berkas desain. */
fun formatKreditPanjang(detik: Int): String {
    val aman = detik.coerceAtLeast(0)
    return "%02dh %02dm %02ds".format(aman / 3600, (aman % 3600) / 60, aman % 60)
}

/**
 * Nama package jadi nama yang bisa dibaca.
 *
 * Dipetakan dari daftar yang dipantau saja. Package di luar daftar
 * dikembalikan apa adanya, bukan ditebak.
 */
fun namaAplikasi(paket: String): String = when (paket) {
    "com.zhiliaoapp.musically" -> "TikTok"
    "com.instagram.android" -> "Instagram"
    "com.twitter.android" -> "X"
    "com.google.android.youtube" -> "YouTube"
    "com.facebook.katana" -> "Facebook"
    "com.android.chrome" -> "Chrome"
    "org.mozilla.firefox" -> "Firefox"
    else -> paket.substringAfterLast('.')
}

@Preview(name = "Dashboard", heightDp = 800)
@Composable
private fun PratinjauHome() {
    SoalduluTheme(paksaGelap = true) {
        LayarHome(
            statistik = StatistikHome(
                nama = "Syamil",
                sisaKreditDetik = 742,
                soalDikerjakan = 24,
                jumlahBenar = 17,
                jumlahSalah = 7,
                paketPalingSering = "com.instagram.android",
                jumlahPemicu = 9,
            ),
            onPengaturan = {},
            onKerjakanSoal = {},
        )
    }
}
