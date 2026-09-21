package id.soaldulu.app.ui.layar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import id.soaldulu.app.GateConfig
import id.soaldulu.app.StatusIzin
import id.soaldulu.app.ui.theme.Ukuran

/**
 * Alat uji Fase 0. BUKAN layar aplikasi.
 *
 * Hidup hanya selama GateConfig.DEV_MODE true, dan dibuang setelah keempat
 * kriteria lulus.
 */
@Composable
fun LayarSpike(
    status: StatusIzin,
    jumlahStart: Int,
    ringkasanLog: String,
    isiLog: String,
    onMulaiService: () -> Unit,
    onHentikanService: () -> Unit,
    onMuatLog: () -> Unit,
    onKembali: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Ukuran.marginLayar, vertical = Ukuran.antarBagian),
    ) {
        Text(
            "Layar uji Fase 0",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            "Hilang sendiri saat DEV_MODE dikembalikan ke false.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(Ukuran.antarBagian))

        Text(
            "${status.jumlahAktif} dari 4 izin aktif",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = onMulaiService,
            enabled = status.semuaAktif,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Mulai service")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onHentikanService, modifier = Modifier.fillMaxWidth()) {
            Text("Hentikan service")
        }

        Spacer(Modifier.height(Ukuran.antarBagian))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(Ukuran.antarBagian))

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(Ukuran.paddingKartu)) {
                Text("Kriteria 3 — service hidup 6 jam", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Service pernah start: $jumlahStart kali. Kalau angka ini naik " +
                        "sendiri selama uji, service sempat mati lalu dihidupkan ulang.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Text("Aplikasi dipantau", style = MaterialTheme.typography.titleSmall)
        Text(
            GateConfig.MONITORED_PACKAGES.joinToString("\n"),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(Ukuran.antarBagian))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(Ukuran.antarBagian))

        Text("Berkas log", style = MaterialTheme.typography.titleSmall)
        Text(
            ringkasanLog,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(onClick = onMuatLog, modifier = Modifier.weight(1f)) {
                Text("Muat log")
            }
            OutlinedButton(onClick = onKembali, modifier = Modifier.weight(1f)) {
                Text("Kembali")
            }
        }
        if (isiLog.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text(
                isiLog,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(48.dp))
    }
}
