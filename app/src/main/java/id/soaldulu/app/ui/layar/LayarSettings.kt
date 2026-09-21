package id.soaldulu.app.ui.layar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import id.soaldulu.app.GateConfig
import id.soaldulu.app.ui.theme.SoalduluTheme
import id.soaldulu.app.ui.theme.Ukuran

/**
 * Settings.
 *
 * Sakelar Light/Dark hanya mengambil alih tema sistem. Selama belum pernah
 * digeser, aplikasi mengikuti tema sistem.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LayarSettings(
    nama: String,
    avatar: Int,
    versiPaket: String,
    jumlahButirAktif: Int,
    versiAplikasi: String,
    statusEkspor: String,
    gelapEfektif: Boolean,
    ikutSistem: Boolean,
    onGantiAvatar: () -> Unit,
    onUbahTema: (Boolean) -> Unit,
    onIkutSistem: () -> Unit,
    onPerizinan: () -> Unit,
    onDataPrivasi: () -> Unit,
    onEkspor: () -> Unit,
    onLayarUji: () -> Unit,
    onKembali: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(Ukuran.antarBagian))

            Avatar(indeks = avatar, ukuran = Ukuran.avatarBesar, onEdit = onGantiAvatar)

            Spacer(Modifier.height(16.dp))

            Text(
                nama.ifBlank { "Tanpa nama" },
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Spacer(Modifier.height(Ukuran.antarBagian))

            Column(
                Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Ukuran.jarakKartu),
            ) {
                BarisPengaturan("Preference", "Paket $versiPaket · $jumlahButirAktif butir aktif")

                BarisPengaturan("Permission", "Periksa dan perbaiki izin", onKlik = onPerizinan)

                BarisPengaturan(
                    "Data and Privacy",
                    "Baca syarat penggunaan dan apa saja yang dicatat",
                    onKlik = onDataPrivasi,
                )

                BarisPengaturan(
                    "Ekspor catatan",
                    "Tulis seluruh log ke satu berkas CSV lalu kirim ke peneliti",
                    onKlik = onEkspor,
                )

                BarisPengaturan(
                    "Version",
                    versiAplikasi,
                    // UPDATE_MANIFEST_URL kosong = fitur pembaruan mati
                    // (handoff Bagian 4). Tidak ada yang bisa dicek.
                    jejak = if (GateConfig.UPDATE_MANIFEST_URL.isBlank()) {
                        "Tidak ada pembaruan"
                    } else {
                        "Update Available"
                    },
                )

                KartuBaris {
                    ListItem(
                        headlineContent = { Text("Light/Dark") },
                        supportingContent = {
                            Text(
                                if (ikutSistem) {
                                    "Mengikuti tema sistem"
                                } else if (gelapEfektif) {
                                    "Gelap · disetel sendiri"
                                } else {
                                    "Terang · disetel sendiri"
                                }
                            )
                        },
                        trailingContent = {
                            Switch(
                                checked = gelapEfektif,
                                onCheckedChange = onUbahTema,
                            )
                        },
                        colors = warnaListItem(),
                    )
                }

                if (!ikutSistem) {
                    BarisPengaturan(
                        "Kembali ikut tema sistem",
                        "Buang setelan tema manual",
                        onKlik = onIkutSistem,
                    )
                }
            }

            Spacer(Modifier.height(Ukuran.antarBagian))

            Text(
                "Ekspor terakhir: ${statusEkspor.ifBlank { "belum pernah" }}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            if (GateConfig.DEV_MODE) {
                Spacer(Modifier.height(Ukuran.antarBagian))
                Column(Modifier.fillMaxWidth()) {
                    BarisPengaturan(
                        "Layar uji Fase 0",
                        "Hilang saat GateConfig.DEV_MODE dikembalikan ke false",
                        onKlik = onLayarUji,
                    )
                }
            }

            Spacer(Modifier.height(48.dp))
        }
    }
}

@Composable
private fun BarisPengaturan(
    judul: String,
    penjelasan: String,
    jejak: String? = null,
    onKlik: (() -> Unit)? = null,
) {
    KartuBaris(onKlik = onKlik) {
        ListItem(
            headlineContent = { Text(judul) },
            supportingContent = { Text(penjelasan) },
            trailingContent = jejak?.let {
                { Text(it, style = MaterialTheme.typography.labelMedium) }
            },
            colors = warnaListItem(),
        )
    }
}

@Composable
private fun KartuBaris(
    onKlik: (() -> Unit)? = null,
    isi: @Composable () -> Unit,
) {
    val warna = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    )
    if (onKlik == null) {
        Card(colors = warna, modifier = Modifier.fillMaxWidth()) { isi() }
    } else {
        Card(onClick = onKlik, colors = warna, modifier = Modifier.fillMaxWidth()) { isi() }
    }
}

@Composable
private fun warnaListItem() = ListItemDefaults.colors(
    containerColor = MaterialTheme.colorScheme.primaryContainer,
    headlineColor = MaterialTheme.colorScheme.onPrimaryContainer,
    supportingColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
    trailingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
)

@Preview(name = "Settings", heightDp = 900)
@Composable
private fun PratinjauSettings() {
    SoalduluTheme(paksaGelap = true) {
        LayarSettings(
            nama = "Syamil",
            avatar = 1,
            versiPaket = "0.0.0-dummy",
            jumlahButirAktif = 9,
            versiAplikasi = "1.0 (1)",
            statusEkspor = "",
            gelapEfektif = true,
            ikutSistem = true,
            onGantiAvatar = {},
            onUbahTema = {},
            onIkutSistem = {},
            onPerizinan = {},
            onDataPrivasi = {},
            onEkspor = {},
            onLayarUji = {},
            onKembali = {},
        )
    }
}
