package id.soaldulu.app

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import id.soaldulu.app.ui.theme.SoalduluTheme

/**
 * Layar uji Fase 0.
 *
 * Ini BUKAN Layar 4 dari mockup — tanpa warna kuningan, tanpa font serif,
 * tanpa kartu. Tujuannya hanya memberi jalan untuk memberikan empat izin,
 * menyalakan service, dan membaca angka hasil spike.
 */
class MainActivity : ComponentActivity() {

    private val status = mutableStateOf(StatusIzin())
    private val ringkasanLog = mutableStateOf("belum ada")
    private val jumlahStart = mutableStateOf(0)
    private val isiLog = mutableStateOf("")

    private val mintaNotifikasi =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { segarkan() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SoalduluTheme(darkTheme = true, dynamicColor = false) {
                Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
                    LayarSpike(
                        modifier = Modifier.padding(padding),
                        status = status.value,
                        ringkasanLog = ringkasanLog.value,
                        jumlahStart = jumlahStart.value,
                        isiLog = isiLog.value,
                        onMuatLog = { isiLog.value = SpikeLog.bacaBarisTerakhir(this) },
                        onHapusLog = {
                            SpikeLog.hapusBerkas(this)
                            isiLog.value = ""
                            segarkan()
                        },
                        onUsageAccess = { bukaSettingsUsageAccess(this) },
                        onOverlay = { bukaSettingsOverlay(this) },
                        onNotifikasi = { mintaIzinNotifikasi() },
                        onBaterai = { bukaSettingsBaterai(this) },
                        onMulaiService = {
                            startForegroundService(Intent(this, GateWatchService::class.java))
                        },
                        onHentikanService = {
                            stopService(Intent(this, GateWatchService::class.java))
                        },
                    )
                }
            }
        }
    }

    /**
     * Status izin diperbarui otomatis setiap kembali dari Settings.
     * Mekanisme yang sama nanti dipakai Layar 4 (handoff Bagian 8 nomor 4).
     */
    override fun onResume() {
        super.onResume()
        segarkan()
    }

    private fun segarkan() {
        status.value = bacaStatusIzin(this)
        ringkasanLog.value = SpikeLog.ringkasanBerkas(this)
        jumlahStart.value = getSharedPreferences(GateWatchService.PREFS, MODE_PRIVATE)
            .getInt(GateWatchService.KEY_JUMLAH_START, 0)
    }

    private fun mintaIzinNotifikasi() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            mintaNotifikasi.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

@Composable
private fun LayarSpike(
    status: StatusIzin,
    ringkasanLog: String,
    jumlahStart: Int,
    isiLog: String,
    onMuatLog: () -> Unit,
    onHapusLog: () -> Unit,
    onUsageAccess: () -> Unit,
    onOverlay: () -> Unit,
    onNotifikasi: () -> Unit,
    onBaterai: () -> Unit,
    onMulaiService: () -> Unit,
    onHentikanService: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Text("Soaldulu — Fase 0", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Layar uji teknis. Bukan Layar 4 dari mockup.",
            style = MaterialTheme.typography.bodySmall,
        )

        Spacer(Modifier.height(24.dp))

        BarisIzin(
            judul = "1 · Akses Penggunaan",
            alasan = "Agar Soaldulu tahu aplikasi apa yang sedang kamu buka.",
            aktif = status.usageAccess,
            onKlik = onUsageAccess,
        )
        BarisIzin(
            judul = "2 · Tampil di Atas Aplikasi Lain",
            alasan = "Agar gerbang soal bisa muncul di atas media sosial.",
            aktif = status.overlay,
            onKlik = onOverlay,
        )
        BarisIzin(
            judul = "3 · Notifikasi",
            alasan = "Agar layanan latar tetap berjalan dan statusnya terlihat.",
            aktif = status.notifikasi,
            onKlik = onNotifikasi,
        )
        BarisIzin(
            judul = "4 · Abaikan Optimasi Baterai",
            alasan = "Agar sistem tidak mematikan Soaldulu diam-diam.",
            aktif = status.baterai,
            onKlik = onBaterai,
        )

        Spacer(Modifier.height(8.dp))
        Text(
            "${status.jumlahAktif} dari 4 izin sudah aktif",
            style = MaterialTheme.typography.titleMedium,
        )

        Spacer(Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(Modifier.height(24.dp))

        Button(
            onClick = onMulaiService,
            enabled = status.semuaAktif,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("MULAI SERVICE")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onHentikanService, modifier = Modifier.fillMaxWidth()) {
            Text("HENTIKAN SERVICE")
        }
        if (!status.semuaAktif) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Lengkapi keempat izin dulu sebelum menyalakan service.",
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Spacer(Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(Modifier.height(24.dp))

        Text("Service pernah start: $jumlahStart kali", style = MaterialTheme.typography.titleSmall)
        Text(
            "Kalau angka ini naik sendiri selama uji 6 jam, berarti service " +
                "sempat mati lalu dihidupkan ulang sistem.",
            style = MaterialTheme.typography.bodySmall,
        )

        Spacer(Modifier.height(16.dp))
        Text("Aplikasi dipantau", style = MaterialTheme.typography.titleSmall)
        Text(
            PAKET_DIPANTAU.joinToString("\n"),
            style = MaterialTheme.typography.bodySmall,
        )

        Spacer(Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(Modifier.height(24.dp))

        Text("Berkas log", style = MaterialTheme.typography.titleSmall)
        Text(ringkasanLog, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(8.dp))
        Row {
            OutlinedButton(onClick = onMuatLog) { Text("MUAT LOG") }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(onClick = onHapusLog) { Text("HAPUS LOG") }
        }
        if (isiLog.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text(
                isiLog,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
            )
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun BarisIzin(
    judul: String,
    alasan: String,
    aktif: Boolean,
    onKlik: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(judul, style = MaterialTheme.typography.titleSmall)
                Text(alasan, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.width(12.dp))
            Text(
                if (aktif) "AKTIF" else "BELUM",
                style = MaterialTheme.typography.labelLarge,
            )
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onKlik, enabled = !aktif) {
            Text(if (aktif) "Sudah aktif" else "Buka pengaturan")
        }
    }
}
