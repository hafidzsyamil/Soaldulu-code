package id.soaldulu.app

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import id.soaldulu.app.data.EksporCsv
import id.soaldulu.app.data.HasilSeed
import id.soaldulu.app.data.Preferensi
import id.soaldulu.app.data.SoalduluRepository
import id.soaldulu.app.ui.layar.LayarHome
import id.soaldulu.app.ui.layar.LayarKodeResponden
import id.soaldulu.app.ui.layar.LayarPaketSoal
import id.soaldulu.app.ui.layar.LayarPerizinan
import id.soaldulu.app.ui.layar.LayarPersetujuan
import id.soaldulu.app.ui.layar.LayarSettings
import id.soaldulu.app.ui.layar.LayarSpike
import id.soaldulu.app.ui.layar.LayarWelcome
import id.soaldulu.app.ui.layar.NavigasiBawah
import id.soaldulu.app.ui.layar.StatistikHome
import id.soaldulu.app.ui.layar.StatusPaket
import id.soaldulu.app.ui.theme.Background
import id.soaldulu.app.ui.theme.SoalduluTheme
import kotlinx.coroutines.launch

/**
 * Layar-layar aplikasi.
 *
 * Alurnya lurus, jadi navigasinya cukup satu enum dan satu when — tanpa
 * pustaka Navigation. Tujuh layar berurutan tidak sepadan dengan satu
 * dependency tambahan dan satu konsep baru.
 */
enum class Layar {
    WELCOME,
    KODE_RESPONDEN,
    PERSETUJUAN,
    PERIZINAN,
    PAKET_SOAL,
    HOME,
    PENGATURAN,
    SPIKE,
}

/**
 * Satu-satunya Activity aplikasi.
 *
 * Menyimpan state layar dan menjembatani UI ke DataStore, Room, dan service.
 * Layar Gerbang TIDAK ada di sini — itu hidup di dalam overlay milik
 * GateWatchService, karena harus bisa muncul di atas aplikasi lain.
 */
class MainActivity : ComponentActivity() {

    private val layar = mutableStateOf(Layar.WELCOME)
    private val status = mutableStateOf(StatusIzin())
    private val kodeResponden = mutableStateOf("")
    private val statistik = mutableStateOf(StatistikHome())
    private val statusPaket = mutableStateOf<StatusPaket>(StatusPaket.Belum)
    private val statusEkspor = mutableStateOf("")
    private val versiPaket = mutableStateOf("")
    private val jumlahButirAktif = mutableIntStateOf(0)

    // Hanya dipakai layar uji Fase 0.
    private val jumlahStart = mutableIntStateOf(0)
    private val ringkasanLog = mutableStateOf("belum ada")
    private val isiLog = mutableStateOf("")

    private var rutePertamaSudahDitentukan = false

    private val mintaNotifikasi =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { segarkan() }

    private val repo by lazy { SoalduluRepository.ambil(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SoalduluTheme {
                // enableEdgeToEdge membuat konten menggambar di bawah status bar
                // dan navigation bar. Latar tetap memenuhi layar, tapi isinya
                // digeser masuk supaya judul tidak tertutup jam.
                Column(
                    Modifier
                        .fillMaxSize()
                        .background(Background)
                        .safeDrawingPadding()
                ) {
                    Column(Modifier.weight(1f)) { IsiLayar() }
                    if (layar.value == Layar.HOME || layar.value == Layar.PENGATURAN) {
                        NavigasiBawah(
                            diBeranda = layar.value == Layar.HOME,
                            onBeranda = { layar.value = Layar.HOME },
                            onPengaturan = { layar.value = Layar.PENGATURAN },
                        )
                    }
                }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun IsiLayar() {
        when (layar.value) {
            Layar.WELCOME -> LayarWelcome(
                onMulai = { layar.value = Layar.KODE_RESPONDEN },
            )

            Layar.KODE_RESPONDEN -> LayarKodeResponden(
                kodeAwal = kodeResponden.value,
                onLanjut = { kode ->
                    lifecycleScope.launch {
                        Preferensi.simpanKodeResponden(this@MainActivity, kode)
                        kodeResponden.value = kode
                        layar.value = Layar.PERSETUJUAN
                    }
                },
            )

            Layar.PERSETUJUAN -> LayarPersetujuan(
                onSetuju = {
                    lifecycleScope.launch {
                        Preferensi.simpanPersetujuan(this@MainActivity, true)
                        layar.value = Layar.PERIZINAN
                    }
                },
                // Menolak berarti keluar. Tidak ada bujukan kedua — responden
                // berhak berhenti tanpa konsekuensi (Bagian 8 nomor 3).
                onTidakBersedia = { finish() },
            )

            Layar.PERIZINAN -> LayarPerizinan(
                status = status.value,
                onBerikanIzin = { mintaIzinBerikutnya() },
                onLanjut = { layar.value = Layar.PAKET_SOAL },
            )

            Layar.PAKET_SOAL -> LayarPaketSoal(
                status = statusPaket.value,
                onPasang = { pasangPaketSoal() },
                onLanjut = {
                    startForegroundService(Intent(this, GateWatchService::class.java))
                    layar.value = Layar.HOME
                },
            )

            Layar.HOME -> LayarHome(statistik = statistik.value)

            Layar.PENGATURAN -> LayarSettings(
                kodeResponden = kodeResponden.value,
                versiPaket = versiPaket.value,
                jumlahButirAktif = jumlahButirAktif.intValue,
                versiAplikasi = versiAplikasi(),
                statusEkspor = statusEkspor.value,
                onEkspor = { eksporLog() },
                onBukaLayarUji = { layar.value = Layar.SPIKE },
            )

            Layar.SPIKE -> LayarSpike(
                status = status.value,
                jumlahStart = jumlahStart.intValue,
                ringkasanLog = ringkasanLog.value,
                isiLog = isiLog.value,
                onMulaiService = {
                    startForegroundService(Intent(this, GateWatchService::class.java))
                },
                onHentikanService = {
                    stopService(Intent(this, GateWatchService::class.java))
                },
                onMuatLog = { isiLog.value = SpikeLog.bacaBarisTerakhir(this) },
                onKembali = { layar.value = Layar.PENGATURAN },
            )
        }
    }

    /**
     * Status izin dan angka Home disegarkan tiap kali layar kembali ke depan.
     * Ini juga yang membuat Layar 4 memperbarui dirinya sendiri saat responden
     * kembali dari Settings (Bagian 8 nomor 4).
     */
    override fun onResume() {
        super.onResume()
        segarkan()
    }

    private fun segarkan() {
        status.value = bacaStatusIzin(this)
        ringkasanLog.value = SpikeLog.ringkasanBerkas(this)
        jumlahStart.intValue = getSharedPreferences(GateWatchService.PREFS, MODE_PRIVATE)
            .getInt(GateWatchService.KEY_JUMLAH_START, 0)

        lifecycleScope.launch {
            val kode = Preferensi.kodeResponden(this@MainActivity)
            kodeResponden.value = kode

            val paket = repo.paketTerpasang()
            versiPaket.value = paket?.version.orEmpty()
            jumlahButirAktif.intValue = repo.jumlahButirAktif()
            if (paket != null && statusPaket.value !is StatusPaket.Gagal) {
                statusPaket.value = StatusPaket.Terpasang(
                    versi = paket.version,
                    jumlahButir = paket.itemCount,
                    jumlahAktif = paket.activeItemCount,
                )
            }

            statistik.value = StatistikHome(
                kodeResponden = kode,
                sisaKreditDetik = sisaKreditDetik(),
                soalDikerjakan = repo.jumlahDijawab(kode),
                jumlahBenar = repo.jumlahBenar(kode),
                hariBerjalan = repo.hariBerjalan(kode),
                aktivitasTerakhir = repo.jawabanTerakhir(kode, 5),
            )

            val onboardingSelesai = kode.isNotBlank() &&
                Preferensi.sudahSetuju(this@MainActivity) &&
                status.value.semuaAktif &&
                paket != null

            if (!rutePertamaSudahDitentukan) {
                rutePertamaSudahDitentukan = true
                layar.value = ruteAwal(kode, Preferensi.sudahSetuju(this@MainActivity), paket != null)
            }

            // Jaring pengaman: kalau service pernah mati — dibunuh sistem,
            // HP di-restart, atau aplikasi dipasang ulang — membuka aplikasi
            // menghidupkannya lagi. Tanpa ini gerbang bisa diam-diam mati
            // selama berhari-hari dan uji coba responden itu hangus.
            // startForegroundService aman dipanggil berulang.
            if (onboardingSelesai) {
                startForegroundService(Intent(this@MainActivity, GateWatchService::class.java))
            }
        }
    }

    /**
     * Onboarding bisa dilanjutkan dari tempat terakhir. Kalau responden
     * menutup aplikasi di tengah jalan, dia tidak diminta mengulang semuanya.
     */
    private fun ruteAwal(kode: String, sudahSetuju: Boolean, paketAda: Boolean): Layar = when {
        kode.isBlank() -> Layar.WELCOME
        !sudahSetuju -> Layar.PERSETUJUAN
        !bacaStatusIzin(this).semuaAktif -> Layar.PERIZINAN
        !paketAda -> Layar.PAKET_SOAL
        else -> Layar.HOME
    }

    /** Sisa kredit dibaca dari tempat yang sama dengan yang ditulis service. */
    private fun sisaKreditDetik(): Int {
        val berakhir = getSharedPreferences(GateWatchService.PREFS, MODE_PRIVATE)
            .getLong("kredit_berakhir_pada", 0L)
        if (berakhir == 0L) return 0
        return ((berakhir - System.currentTimeMillis()) / 1000L).coerceAtLeast(0L).toInt()
    }

    /**
     * Minta izin yang belum aktif, satu per satu, dalam urutan wajib
     * Bagian 3.3. Akses Penggunaan lebih dulu karena itu yang paling
     * mungkin membuat orang menyerah.
     */
    private fun mintaIzinBerikutnya() {
        val s = status.value
        when {
            !s.usageAccess -> bukaSettingsUsageAccess(this)
            !s.overlay -> bukaSettingsOverlay(this)
            !s.notifikasi -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    mintaNotifikasi.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            !s.baterai -> bukaSettingsBaterai(this)
        }
    }

    private fun pasangPaketSoal() {
        statusPaket.value = StatusPaket.Memasang
        lifecycleScope.launch {
            statusPaket.value = when (val hasil = repo.seedDariAssets(this@MainActivity)) {
                is HasilSeed.Berhasil -> StatusPaket.Terpasang(
                    versi = hasil.versi,
                    jumlahButir = hasil.jumlahButir,
                    jumlahAktif = hasil.jumlahAktif,
                )

                is HasilSeed.Gagal -> StatusPaket.Gagal(hasil.kesalahan)
            }
            segarkan()
        }
    }

    private fun eksporLog() {
        statusEkspor.value = "Menulis berkas…"
        lifecycleScope.launch {
            try {
                val berkas = EksporCsv.tulis(this@MainActivity, repo, kodeResponden.value)
                statusEkspor.value = "${berkas.name} · ${berkas.length()} byte"
                // Lembar berbagi sistem; respondenlah yang memilih tujuannya.
                startActivity(EksporCsv.intentBagikan(this@MainActivity, berkas))
            } catch (e: Exception) {
                statusEkspor.value = "Gagal mengekspor: ${e.message}"
            }
        }
    }

    private fun versiAplikasi(): String = try {
        val info = packageManager.getPackageInfo(packageName, 0)
        "${info.versionName} (${info.longVersionCode})"
    } catch (e: Exception) {
        "tidak diketahui"
    }
}
