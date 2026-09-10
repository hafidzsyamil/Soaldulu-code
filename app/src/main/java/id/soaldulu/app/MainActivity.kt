package id.soaldulu.app

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import id.soaldulu.app.data.HasilSeed
import id.soaldulu.app.data.Preferensi
import id.soaldulu.app.data.SoalduluRepository
import id.soaldulu.app.ui.layar.LayarKodeResponden
import id.soaldulu.app.ui.layar.LayarWelcome
import id.soaldulu.app.ui.theme.SoalduluTheme
import kotlinx.coroutines.launch

/**
 * Layar yang sudah ada. Alurnya lurus, jadi navigasinya cukup satu enum
 * dan satu when — tanpa pustaka Navigation.
 *
 * SPIKE tetap jadi layar awal selama Fase 0 belum lulus semua kriteria.
 * Setelah itu, layar awal diganti WELCOME dan SPIKE dibuang.
 */
enum class Layar { SPIKE, WELCOME, KODE_RESPONDEN }

/**
 * Satu-satunya Activity aplikasi.
 *
 * LayarSpike di bawah BUKAN Layar 4 dari mockup — itu alat uji Fase 0 yang
 * akan dibuang. Layar sungguhan ada di paket ui.layar.
 */
class MainActivity : ComponentActivity() {

    private val status = mutableStateOf(StatusIzin())
    private val ringkasanLog = mutableStateOf("belum ada")
    private val jumlahStart = mutableStateOf(0)
    private val isiLog = mutableStateOf("")
    private val statusBank = mutableStateOf("belum diperiksa")
    private val statusOverlayCompose = mutableStateOf("belum diuji")
    private val layar = mutableStateOf(Layar.SPIKE)
    private val kodeResponden = mutableStateOf("")

    /** Overlay Compose percobaan — dilepas lagi lewat tombol di dalamnya. */
    private var overlayUji: OverlayCompose? = null

    private val mintaNotifikasi =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { segarkan() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SoalduluTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
                    when (layar.value) {
                        Layar.WELCOME -> LayarWelcome(
                            modifier = Modifier.padding(padding),
                            onMulai = { layar.value = Layar.KODE_RESPONDEN },
                        )

                        Layar.KODE_RESPONDEN -> LayarKodeResponden(
                            modifier = Modifier.padding(padding),
                            kodeAwal = kodeResponden.value,
                            onLanjut = { kode -> simpanKodeResponden(kode) },
                        )

                        Layar.SPIKE -> LayarSpike(
                        modifier = Modifier.padding(padding),
                        status = status.value,
                        ringkasanLog = ringkasanLog.value,
                        jumlahStart = jumlahStart.value,
                        isiLog = isiLog.value,
                        statusBank = statusBank.value,
                        onSeedBank = { seedBankSoal() },
                        statusOverlayCompose = statusOverlayCompose.value,
                        onUjiOverlayCompose = { ujiOverlayCompose() },
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
                        kodeResponden = kodeResponden.value,
                        onBukaOnboarding = { layar.value = Layar.WELCOME },
                        )
                    }
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
        lifecycleScope.launch {
            kodeResponden.value = Preferensi.kodeResponden(this@MainActivity)
        }
    }

    /**
     * Simpan kode ke DataStore, lalu kembali ke layar uji.
     *
     * Layar 3 (Persetujuan) belum dibangun, jadi untuk sementara alurnya
     * berhenti di sini — dan kode yang tersimpan tampil di layar uji sebagai
     * bukti DataStore bekerja.
     */
    private fun simpanKodeResponden(kode: String) {
        lifecycleScope.launch {
            Preferensi.simpanKodeResponden(this@MainActivity, kode)
            kodeResponden.value = kode
            layar.value = Layar.SPIKE
        }
    }

    private fun mintaIzinNotifikasi() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            mintaNotifikasi.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    /**
     * Membuktikan Compose bisa jalan di dalam jendela overlay.
     * Seluruh Layar Gerbang di Fase 2 bergantung pada ini.
     */
    private fun ujiOverlayCompose() {
        overlayUji?.tutup()
        overlayUji = OverlayCompose.tampilkan(this) {
            SoalduluTheme {
                Surface(color = Color(0xFF1C1714), modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            "Compose jalan di dalam overlay",
                            color = Color(0xFFC9A962),
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Kalau kamu melihat ini, ranjau terbesar Fase 2 sudah aman.",
                            color = Color(0xFF9C8B7A),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Spacer(Modifier.height(32.dp))
                        Button(onClick = {
                            overlayUji?.tutup()
                            overlayUji = null
                        }) {
                            Text("TUTUP")
                        }
                    }
                }
            }
        }
        statusOverlayCompose.value =
            if (overlayUji != null) {
                "Berhasil — jendela terpasang."
            } else {
                "GAGAL — cek izin 'tampil di atas aplikasi lain'. Rincian di berkas log."
            }
    }

    /** Baca bank_soal_v1.json dari assets dan tulis ke Room. */
    private fun seedBankSoal() {
        statusBank.value = "sedang membaca…"
        lifecycleScope.launch {
            val repo = SoalduluRepository.ambil(this@MainActivity)
            statusBank.value = when (val hasil = repo.seedDariAssets(this@MainActivity)) {
                is HasilSeed.Berhasil ->
                    "Berhasil · versi ${hasil.versi} · ${hasil.jumlahButir} butir " +
                        "(${hasil.jumlahAktif} aktif)"

                is HasilSeed.Gagal ->
                    "GAGAL — ${hasil.kesalahan.size} masalah:\n" +
                        hasil.kesalahan.joinToString("\n") { "· $it" }
            }
        }
    }
}

@Composable
private fun LayarSpike(
    status: StatusIzin,
    ringkasanLog: String,
    jumlahStart: Int,
    isiLog: String,
    statusBank: String,
    onSeedBank: () -> Unit,
    statusOverlayCompose: String,
    onUjiOverlayCompose: () -> Unit,
    kodeResponden: String,
    onBukaOnboarding: () -> Unit,
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
            GateConfig.MONITORED_PACKAGES.joinToString("\n"),
            style = MaterialTheme.typography.bodySmall,
        )

        Spacer(Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(Modifier.height(24.dp))

        Text("Layar sungguhan", style = MaterialTheme.typography.titleSmall)
        Text(
            "Layar 1 (Welcome) dan Layar 2 (Kode Responden). Alurnya berhenti " +
                "di Layar 2 karena Layar 3 belum dibangun.",
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onBukaOnboarding) { Text("BUKA ALUR ONBOARDING") }
        Spacer(Modifier.height(8.dp))
        Text(
            if (kodeResponden.isEmpty()) {
                "Kode responden di DataStore: belum ada"
            } else {
                "Kode responden di DataStore: $kodeResponden"
            },
            style = MaterialTheme.typography.bodySmall,
        )

        Spacer(Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(Modifier.height(24.dp))

        Text("Compose di dalam overlay", style = MaterialTheme.typography.titleSmall)
        Text(
            "Layar Gerbang harus muncul di dalam overlay, bukan sebagai Activity. " +
                "Tombol ini membuktikan itu mungkin.",
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onUjiOverlayCompose) { Text("UJI OVERLAY COMPOSE") }
        Spacer(Modifier.height(8.dp))
        Text(statusOverlayCompose, style = MaterialTheme.typography.bodySmall)

        Spacer(Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(Modifier.height(24.dp))

        Text("Bank soal (Fase 1)", style = MaterialTheme.typography.titleSmall)
        Text(
            "Membaca ${GateConfig.BUNDLED_PACKAGE_ASSET} dari assets, memvalidasi, " +
                "lalu menulis ke Room.",
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onSeedBank) { Text("BACA & SEED BANK SOAL") }
        Spacer(Modifier.height(8.dp))
        Text(
            statusBank,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
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
