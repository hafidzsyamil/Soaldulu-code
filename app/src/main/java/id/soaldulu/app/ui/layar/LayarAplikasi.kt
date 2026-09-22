package id.soaldulu.app.ui.layar

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import id.soaldulu.app.DaftarAplikasi
import id.soaldulu.app.GateConfig
import id.soaldulu.app.ui.theme.Ukuran

/** Satu aplikasi di daftar yang dijaga gerbang. */
data class AplikasiDipantau(
    val paket: String,
    val nama: String,
    val ikon: ImageBitmap?,
    /** Dari GateConfig; tidak bisa dihapus. */
    val bawaan: Boolean,
    /** TikTok dan Instagram: hanya bisa dimatikan lewat mode darurat. */
    val terbatas: Boolean,
    /** Gerbang sedang berlaku untuk aplikasi ini. */
    val dijaga: Boolean,
)

/** Aplikasi terpasang yang bisa ditambahkan ke daftar. */
data class KandidatAplikasi(val paket: String, val nama: String, val ikon: ImageBitmap?)

/**
 * Aplikasi Dipantau.
 *
 * Aturan sakelarnya ada di DaftarAplikasi; layar ini hanya menampilkannya
 * dan meminta konfirmasi sebelum langkah yang tidak bisa dibatalkan —
 * memakai jatah darurat, atau menghapus aplikasi.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LayarAplikasi(
    daftar: List<AplikasiDipantau>,
    darurat: DaftarAplikasi.Darurat,
    saldoKreditDetik: Int,
    pemilihTerbuka: Boolean,
    /** null = daftar aplikasi terpasang masih dimuat. */
    kandidat: List<KandidatAplikasi>?,
    onUbahAktif: (paket: String, aktif: Boolean) -> Unit,
    onMulaiDarurat: (paket: String) -> Unit,
    onAkhiriDarurat: () -> Unit,
    onBukaPemilih: () -> Unit,
    onTutupPemilih: () -> Unit,
    onTambah: (KandidatAplikasi) -> Unit,
    onHapusDenganSoal: (paket: String) -> Unit,
    onHapusDenganKredit: (paket: String) -> Unit,
    onKembali: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var konfirmasiDarurat by remember { mutableStateOf<AplikasiDipantau?>(null) }
    var konfirmasiHapus by remember { mutableStateOf<AplikasiDipantau?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Aplikasi Dipantau") },
                navigationIcon = {
                    IconButton(onClick = onKembali) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
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
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = Ukuran.marginLayar, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(Ukuran.jarakKartu),
        ) {
            item {
                Text(
                    "Gerbang soal muncul saat aplikasi di bawah dibuka tanpa kredit. " +
                        "TikTok dan Instagram hanya bisa dimatikan lewat mode darurat: paling lama " +
                        "${GateConfig.EMERGENCY_PAUSE_SECONDS / 60} menit, lalu terkunci " +
                        "${GateConfig.EMERGENCY_COOLDOWN_SECONDS / 3600} jam. Aplikasi lain bebas " +
                        "dimatikan.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            items(daftar, key = { it.paket }) { app ->
                BarisAplikasi(
                    app = app,
                    darurat = darurat,
                    onSakelar = { nyala ->
                        when {
                            !app.terbatas -> onUbahAktif(app.paket, nyala)
                            nyala -> onAkhiriDarurat()
                            else -> konfirmasiDarurat = app
                        }
                    },
                    onHapus = { konfirmasiHapus = app },
                )
            }

            item {
                FilledTonalButton(
                    onClick = onBukaPemilih,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text("Tambah aplikasi", modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }

    konfirmasiDarurat?.let { app ->
        AlertDialog(
            onDismissRequest = { konfirmasiDarurat = null },
            title = { Text("Matikan ${app.nama}?") },
            text = {
                Text(
                    "Gerbang ${app.nama} mati selama ${GateConfig.EMERGENCY_PAUSE_SECONDS / 60} menit " +
                        "dan kredit tidak terpakai. Setelah itu gerbang aktif lagi, dan mode darurat " +
                        "terkunci ${GateConfig.EMERGENCY_COOLDOWN_SECONDS / 3600} jam untuk TikTok " +
                        "maupun Instagram."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onMulaiDarurat(app.paket)
                    konfirmasiDarurat = null
                }) { Text("Matikan") }
            },
            dismissButton = {
                TextButton(onClick = { konfirmasiDarurat = null }) { Text("Batal") }
            },
        )
    }

    konfirmasiHapus?.let { app ->
        val biaya = GateConfig.REMOVE_APP_CREDIT_COST_SECONDS
        val kreditCukup = saldoKreditDetik >= biaya
        AlertDialog(
            onDismissRequest = { konfirmasiHapus = null },
            title = { Text("Hapus ${app.nama}?") },
            text = {
                Text(
                    "Untuk menghapus aplikasi dari daftar, kerjakan ${GateConfig.QUESTIONS_PER_GATE} " +
                        "soal atau bayar ${biaya / 3600} jam kredit." +
                        if (kreditCukup) "" else "\n\nKreditmu belum cukup untuk membayar."
                )
            },
            confirmButton = {
                Column(horizontalAlignment = Alignment.End) {
                    TextButton(onClick = {
                        onHapusDenganSoal(app.paket)
                        konfirmasiHapus = null
                    }) { Text("Kerjakan ${GateConfig.QUESTIONS_PER_GATE} soal") }
                    TextButton(
                        enabled = kreditCukup,
                        onClick = {
                            onHapusDenganKredit(app.paket)
                            konfirmasiHapus = null
                        },
                    ) { Text("Bayar ${biaya / 3600} jam kredit") }
                    TextButton(onClick = { konfirmasiHapus = null }) { Text("Batal") }
                }
            },
        )
    }

    if (pemilihTerbuka) {
        PemilihAplikasi(kandidat = kandidat, onPilih = onTambah, onTutup = onTutupPemilih)
    }
}

@Composable
private fun BarisAplikasi(
    app: AplikasiDipantau,
    darurat: DaftarAplikasi.Darurat,
    onSakelar: (Boolean) -> Unit,
    onHapus: () -> Unit,
) {
    val sedangDarurat = darurat.paket == app.paket
    val keterangan: String
    val sakelarAktif: Boolean
    when {
        !app.terbatas -> {
            keterangan = if (app.dijaga) "Dijaga gerbang" else "Dimatikan"
            sakelarAktif = true
        }
        sedangDarurat -> {
            keterangan = "Mode darurat · aktif lagi dalam ${formatMenitDetik(darurat.sisaDetik)}"
            sakelarAktif = true
        }
        darurat.paket != null -> {
            keterangan = "Dijaga · mode darurat sedang dipakai aplikasi lain"
            sakelarAktif = false
        }
        darurat.cooldownDetik > 0 -> {
            keterangan = "Dijaga · darurat terkunci ${formatJamMenit(darurat.cooldownDetik)} lagi"
            sakelarAktif = false
        }
        else -> {
            keterangan = "Dijaga · bisa dimatikan ${GateConfig.EMERGENCY_PAUSE_SECONDS / 60} menit"
            sakelarAktif = true
        }
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        ListItem(
            leadingContent = { IkonAplikasi(app.ikon) },
            headlineContent = { Text(app.nama) },
            supportingContent = { Text(keterangan) },
            trailingContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!app.bawaan) {
                        IconButton(onClick = onHapus) {
                            Icon(Icons.Default.Delete, contentDescription = "Hapus ${app.nama}")
                        }
                    }
                    Switch(checked = app.dijaga, onCheckedChange = onSakelar, enabled = sakelarAktif)
                }
            },
            colors = ListItemDefaults.colors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                headlineColor = MaterialTheme.colorScheme.onPrimaryContainer,
                supportingColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PemilihAplikasi(
    kandidat: List<KandidatAplikasi>?,
    onPilih: (KandidatAplikasi) -> Unit,
    onTutup: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onTutup,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Text(
            "Tambah aplikasi",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = Ukuran.marginLayar),
        )
        Text(
            "Gerbang soal akan muncul saat aplikasi ini dibuka tanpa kredit.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Ukuran.marginLayar, vertical = 4.dp),
        )
        when {
            kandidat == null -> Box(
                Modifier.fillMaxWidth().heightIn(min = 160.dp),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            kandidat.isEmpty() -> Text(
                "Semua aplikasi yang terpasang sudah ada di daftar.",
                modifier = Modifier.padding(Ukuran.marginLayar),
            )

            else -> LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
                items(kandidat, key = { it.paket }) { k ->
                    ListItem(
                        leadingContent = { IkonAplikasi(k.ikon) },
                        headlineContent = { Text(k.nama) },
                        supportingContent = { Text(k.paket) },
                        modifier = Modifier.clickable { onPilih(k) },
                    )
                }
            }
        }
    }
}

@Composable
private fun IkonAplikasi(ikon: ImageBitmap?) {
    if (ikon != null) {
        Image(bitmap = ikon, contentDescription = null, modifier = Modifier.size(40.dp))
    } else {
        Box(Modifier.size(40.dp))
    }
}

/** 09:05 — untuk sisa mode darurat yang paling lama 10 menit. */
private fun formatMenitDetik(detik: Int): String = "%02d:%02d".format(detik / 60, detik % 60)

/** 5j 12m — untuk cooldown yang berjam-jam. */
private fun formatJamMenit(detik: Int): String {
    val menit = (detik + 59) / 60
    return if (menit >= 60) "${menit / 60}j ${menit % 60}m" else "${menit}m"
}
