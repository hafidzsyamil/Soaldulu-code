package id.soaldulu.app

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import id.soaldulu.app.data.EksporCsv
import id.soaldulu.app.data.HasilSeed
import id.soaldulu.app.data.Preferensi
import id.soaldulu.app.data.SoalLengkap
import id.soaldulu.app.data.SoalduluRepository
import id.soaldulu.app.ui.layar.AlurGerbang
import id.soaldulu.app.ui.layar.JawabanGerbang
import id.soaldulu.app.ui.layar.LayarHome
import id.soaldulu.app.ui.layar.LayarNama
import id.soaldulu.app.ui.layar.LayarPaketSoal
import id.soaldulu.app.ui.layar.LayarPerizinan
import id.soaldulu.app.ui.layar.LayarSettings
import id.soaldulu.app.ui.layar.LayarSpike
import id.soaldulu.app.ui.layar.LayarSyarat
import id.soaldulu.app.ui.layar.LayarWelcome
import id.soaldulu.app.ui.layar.PemilihAvatar
import id.soaldulu.app.ui.layar.StatistikHome
import id.soaldulu.app.ui.layar.StatusPaket
import id.soaldulu.app.ui.theme.Arah
import id.soaldulu.app.ui.theme.SoalduluTheme
import id.soaldulu.app.ui.theme.tolakSentuhan
import id.soaldulu.app.ui.theme.transisiLayar
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.launch

/**
 * Layar-layar aplikasi.
 *
 * Urutannya mengikuti berkas desain: Welcome, Permission, lalu Enter your Name.
 * Persetujuan tidak lagi berupa layar tersendiri — menekan tombol centang di
 * layar nama berarti menyetujui syarat, yang bisa dibaca di LayarSyarat.
 */
enum class Layar {
    WELCOME,
    PERMISSION,
    NAMA,
    PAKET_SOAL,
    DASHBOARD,
    SETTINGS,
    SYARAT,
    KERJAKAN_SOAL,
    SPIKE,
}

/**
 * Satu-satunya Activity aplikasi.
 *
 * Layar Gerbang yang dipicu media sosial TIDAK ada di sini — itu hidup di
 * dalam overlay milik GateWatchService. Yang ada di sini adalah gerbang yang
 * dibuka sendiri lewat tombol Kerjakan Soal.
 */
class MainActivity : ComponentActivity() {

    private val layar = mutableStateOf(Layar.WELCOME)

    /**
     * Riwayat layar untuk tombol Back.
     *
     * Perlu tumpukan sungguhan, bukan tabel tujuan tetap: Permission dan
     * Terms masing-masing bisa dicapai dari dua arah, dan tabel tetap akan
     * memulangkan ke tempat yang salah pada salah satunya.
     */
    private val tumpukan = mutableStateListOf<Layar>()

    /**
     * Arah perpindahan terakhir, menentukan animasinya.
     *
     * Variabel biasa, bukan state: nilainya hanya dibaca saat layar tujuan
     * berubah, dan selalu diisi tepat sebelum perubahan itu.
     */
    private var arah = Arah.TANPA
    private val status = mutableStateOf(StatusIzin())
    private val nama = mutableStateOf("")
    private val avatar = mutableIntStateOf(0)
    private val pilihAvatarTerbuka = mutableStateOf(false)
    private val temaGelap = mutableStateOf<Boolean?>(null)
    private val statistik = mutableStateOf(StatistikHome())
    private val statusPaket = mutableStateOf<StatusPaket>(StatusPaket.Belum)
    private val statusEkspor = mutableStateOf("")
    private val versiPaket = mutableStateOf("")
    private val jumlahButirAktif = mutableIntStateOf(0)
    private val soalManual = mutableStateOf<List<SoalLengkap>?>(null)

    // Hanya dipakai layar uji Fase 0.
    private val jumlahStart = mutableIntStateOf(0)
    private val ringkasanLog = mutableStateOf("belum ada")
    private val isiLog = mutableStateOf("")

    private var rutePertamaSudahDitentukan = false
    private var paketBawaanSudahDicek = false

    private val mintaNotifikasi =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { segarkan() }

    private val repo by lazy { SoalduluRepository.ambil(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // enableEdgeToEdge() di atas memilih warna ikon status bar dari
            // tema SISTEM. Begitu responden memaksa tema lewat Settings —
            // misalnya HP gelap tapi aplikasi terang — ikon jam, baterai, dan
            // sinyal tetap putih di atas latar terang dan tak terlihat. Jadi
            // gayanya dipasang ulang mengikuti tema yang benar-benar tampil.
            val gelap = temaGelap.value ?: isSystemInDarkTheme()
            LaunchedEffect(gelap) {
                val transparan = android.graphics.Color.TRANSPARENT
                val gaya = if (gelap) {
                    SystemBarStyle.dark(transparan)
                } else {
                    SystemBarStyle.light(transparan, transparan)
                }
                enableEdgeToEdge(statusBarStyle = gaya, navigationBarStyle = gaya)
            }

            SoalduluTheme(paksaGelap = temaGelap.value) {
                // enableEdgeToEdge membuat konten menggambar di bawah status bar
                // dan navigation bar. Latar memenuhi layar, isinya digeser masuk.
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .safeDrawingPadding()
                ) {
                    IsiLayar()
                }

                if (pilihAvatarTerbuka.value) {
                    PemilihAvatar(
                        terpilih = avatar.intValue,
                        // Lembarnya menutup sendiri lewat animasinya, lalu
                        // memanggil onTutup — di sini cukup menyimpan pilihan.
                        onPilih = { i ->
                            avatar.intValue = i
                            lifecycleScope.launch {
                                Preferensi.simpanAvatar(this@MainActivity, i)
                            }
                        },
                        onTutup = { pilihAvatarTerbuka.value = false },
                    )
                }
            }
        }
    }

    /** Maju satu layar, menyimpan yang sekarang untuk tombol Back. */
    private fun buka(tujuan: Layar) {
        // Ketukan ganda saat animasi berjalan tidak boleh menumpuk layar
        // yang sama dua kali — Back akan terasa tidak bekerja.
        if (tujuan == layar.value) return
        arah = Arah.MAJU
        tumpukan.add(layar.value)
        layar.value = tujuan
    }

    /** Mundur satu layar. false berarti tidak ada tujuan mundur. */
    private fun kembali(): Boolean {
        val sebelumnya = tumpukan.removeLastOrNull() ?: return false
        arah = Arah.MUNDUR
        layar.value = sebelumnya
        return true
    }

    /**
     * Pindah ke layar akar dan buang riwayatnya.
     *
     * Dipakai saat onboarding selesai: Back dari Dashboard harus keluar dari
     * aplikasi, bukan kembali ke layar pengisian nama.
     */
    private fun gantiAkar(tujuan: Layar, arahBaru: Arah = Arah.GANTI) {
        arah = arahBaru
        tumpukan.clear()
        layar.value = tujuan
    }

    /**
     * Wadah seluruh layar, dengan animasi perpindahan dan predictive back.
     *
     * Polanya sama dengan NavHost milik Navigation Compose: selama gesture
     * Back ditarik, transisi digeser mengikuti jari; kalau dilepas, animasinya
     * dituntaskan; kalau dibatalkan, diputar mundur ke posisi awal.
     */
    @Composable
    private fun IsiLayar() {
        val keadaan = remember { SeekableTransitionState(layar.value) }
        val transisi = rememberTransition(keadaan, label = "layar")
        var sedangGestureBack by remember { mutableStateOf(false) }
        var progresBack by remember { mutableFloatStateOf(0f) }

        PredictiveBackHandler(
            // Dimatikan selama lembar avatar terbuka supaya Back menutup
            // lembarnya lebih dulu, bukan melompati satu layar. Tumpukan
            // kosong berarti Back memang seharusnya menutup aplikasi — itu
            // ditangani sistem, lengkap dengan animasi kembali ke home.
            enabled = tumpukan.isNotEmpty() && !pilihAvatarTerbuka.value,
        ) { progres ->
            arah = Arah.MUNDUR
            progresBack = 0f
            try {
                progres.collect {
                    sedangGestureBack = true
                    progresBack = it.progress
                }
                sedangGestureBack = false
                // Meninggalkan gerbang yang dibuka sendiri membuang jawabannya:
                // gerbang yang tidak selesai bukan data, jadi tidak dicatat.
                kembali()
            } catch (_: CancellationException) {
                sedangGestureBack = false
            }
        }

        if (sedangGestureBack) {
            LaunchedEffect(progresBack) {
                val tujuan = tumpukan.lastOrNull() ?: return@LaunchedEffect
                keadaan.seekTo(progresBack, targetState = tujuan)
            }
        } else {
            LaunchedEffect(layar.value) {
                when {
                    arah == Arah.TANPA -> keadaan.snapTo(layar.value)
                    keadaan.currentState != layar.value -> keadaan.animateTo(layar.value)
                    else -> {
                        // Gesture Back dibatalkan: layar masih di tempatnya,
                        // tapi transisinya sudah tergeser. Putar mundur dari
                        // posisi terakhir, lalu kunci di layar semula.
                        val totalMs = transisi.totalDurationNanos / 1_000_000
                        animate(
                            initialValue = keadaan.fraction,
                            targetValue = 0f,
                            animationSpec = tween((keadaan.fraction * totalMs).toInt()),
                        ) { nilai, _ ->
                            launch {
                                if (nilai > 0f) keadaan.seekTo(nilai)
                                if (nilai == 0f) keadaan.snapTo(layar.value)
                            }
                        }
                    }
                }
            }
        }

        transisi.AnimatedContent(
            transitionSpec = { transisiLayar(arah) },
        ) { tujuan ->
            Box(Modifier.fillMaxSize().tolakSentuhan(tujuan != layar.value)) {
                LayarUntuk(tujuan)
            }
        }
    }

    @Composable
    private fun LayarUntuk(tujuan: Layar) {
        when (tujuan) {
            Layar.WELCOME -> LayarWelcome(
                onLanjut = { buka(Layar.PERMISSION) },
            )

            Layar.PERMISSION -> LayarPerizinan(
                status = status.value,
                onMinta = { mintaIzin(it) },
                onLanjut = {
                    // Permission juga bisa dibuka dari Settings. Kalau datang
                    // dari sana, lanjut berarti kembali ke sana.
                    if (tumpukan.lastOrNull() == Layar.SETTINGS) kembali()
                    else buka(Layar.NAMA)
                },
            )

            Layar.NAMA -> LayarNama(
                namaAwal = nama.value,
                avatar = avatar.intValue,
                onGantiAvatar = { pilihAvatarTerbuka.value = true },
                onSelesai = { diisi -> simpanNama(diisi) },
                onBukaSyarat = { buka(Layar.SYARAT) },
            )

            Layar.PAKET_SOAL -> LayarPaketSoal(
                status = statusPaket.value,
                onPasang = { pasangPaketSoal() },
                onLanjut = {
                    startForegroundService(Intent(this, GateWatchService::class.java))
                    gantiAkar(Layar.DASHBOARD)
                },
            )

            Layar.DASHBOARD -> LayarHome(
                statistik = statistik.value,
                onPengaturan = { buka(Layar.SETTINGS) },
                onKerjakanSoal = { mulaiGerbangManual() },
            )

            Layar.SETTINGS -> LayarSettings(
                nama = nama.value,
                avatar = avatar.intValue,
                versiPaket = versiPaket.value.ifBlank { "belum terpasang" },
                jumlahButirAktif = jumlahButirAktif.intValue,
                versiAplikasi = versiAplikasi(),
                statusEkspor = statusEkspor.value,
                gelapEfektif = temaGelap.value ?: isSystemInDarkTheme(),
                ikutSistem = temaGelap.value == null,
                onGantiAvatar = { pilihAvatarTerbuka.value = true },
                onUbahTema = { gelap -> setTema(gelap) },
                onIkutSistem = { setTema(null) },
                onPerizinan = { buka(Layar.PERMISSION) },
                onDataPrivasi = { buka(Layar.SYARAT) },
                onEkspor = { eksporLog() },
                onLayarUji = { buka(Layar.SPIKE) },
                onKembali = { kembali() },
            )

            Layar.SYARAT -> LayarSyarat(
                onKembali = { kembali() },
            )

            // Selama soal dimuat, layar dibiarkan kosong. Daftar kosong
            // berarti "bank soal belum siap", jadi tidak boleh dipakai
            // sebagai tanda sedang memuat — pesan itu akan berkedip.
            Layar.KERJAKAN_SOAL -> soalManual.value?.let { soal -> AlurGerbang(
                soal = soal,
                sisaKreditDetik = sisaKreditDetik(),
                onSelesai = { jawaban -> selesaikanGerbangManual(jawaban) },
                onLapor = { itemId ->
                    lifecycleScope.launch {
                        repo.catatPeristiwa(nama.value, "ITEM_REPORTED", itemId)
                    }
                },
            ) }

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
                onKembali = { kembali() },
            )
        }
    }

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
            val namaTersimpan = Preferensi.nama(this@MainActivity)
            nama.value = namaTersimpan
            avatar.intValue = Preferensi.avatar(this@MainActivity)
            temaGelap.value = Preferensi.temaGelap(this@MainActivity)

            // Sekali per pembukaan aplikasi: kalau APK membawa bank soal
            // versi baru, bank itu menggantikan yang terpasang.
            if (!paketBawaanSudahDicek) {
                paketBawaanSudahDicek = true
                repo.perbaruiPaketBawaan(this@MainActivity, namaTersimpan)
            }

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

            val dijawab = repo.jumlahDijawab(namaTersimpan)
            val benar = repo.jumlahBenar(namaTersimpan)
            val (paketSering, jumlahPemicu) = repo.pemicuTerbanyak(namaTersimpan)
            statistik.value = StatistikHome(
                nama = namaTersimpan,
                avatar = avatar.intValue,
                sisaKreditDetik = sisaKreditDetik(),
                soalDikerjakan = dijawab,
                jumlahBenar = benar,
                jumlahSalah = dijawab - benar,
                paketPalingSering = paketSering,
                jumlahPemicu = jumlahPemicu,
            )

            val onboardingSelesai = namaTersimpan.isNotBlank() &&
                status.value.semuaAktif &&
                paket != null

            if (!rutePertamaSudahDitentukan) {
                rutePertamaSudahDitentukan = true
                gantiAkar(ruteAwal(namaTersimpan, paket != null), Arah.TANPA)
            }

            // Jaring pengaman: kalau service pernah mati — dibunuh sistem, HP
            // di-restart, atau aplikasi dipasang ulang — membuka aplikasi
            // menghidupkannya lagi. startForegroundService aman dipanggil berulang.
            if (onboardingSelesai) {
                startForegroundService(Intent(this@MainActivity, GateWatchService::class.java))
            }
        }
    }

    /**
     * Onboarding bisa dilanjutkan dari tempat terakhir.
     *
     * Nama kosong berarti responden belum pernah menyelesaikan onboarding,
     * jadi dia SELALU mulai dari Welcome — apa pun keadaan izinnya. Aturan
     * sebelumnya memeriksa izin lebih dulu, sehingga HP yang izinnya sudah
     * aktif melompati Welcome dan langsung mendarat di pengisian nama.
     */
    private fun ruteAwal(namaTersimpan: String, paketAda: Boolean): Layar = when {
        namaTersimpan.isBlank() -> Layar.WELCOME
        !bacaStatusIzin(this).semuaAktif -> Layar.PERMISSION
        !paketAda -> Layar.PAKET_SOAL
        else -> Layar.DASHBOARD
    }

    private fun simpanNama(diisi: String) {
        lifecycleScope.launch {
            Preferensi.simpanNama(this@MainActivity, diisi)
            // Menekan centang berarti menyetujui syarat penggunaan.
            Preferensi.simpanPersetujuan(this@MainActivity, true)
            nama.value = diisi
            if (repo.paketTerpasang() == null) {
                buka(Layar.PAKET_SOAL)
            } else {
                gantiAkar(Layar.DASHBOARD)
            }
        }
    }

    private fun setTema(gelap: Boolean?) {
        temaGelap.value = gelap
        lifecycleScope.launch { Preferensi.simpanTema(this@MainActivity, gelap) }
    }

    /** Sisa kredit dibaca dari tempat yang sama dengan yang ditulis service. */
    private fun sisaKreditDetik(): Int {
        val berakhir = getSharedPreferences(GateWatchService.PREFS, MODE_PRIVATE)
            .getLong(GateWatchService.KEY_KREDIT_BERAKHIR, 0L)
        if (berakhir == 0L) return 0
        return ((berakhir - System.currentTimeMillis()) / 1000L).coerceAtLeast(0L).toInt()
    }

    private fun setKreditBerakhirPada(waktu: Long) {
        getSharedPreferences(GateWatchService.PREFS, MODE_PRIVATE)
            .edit()
            .putLong(GateWatchService.KEY_KREDIT_BERAKHIR, waktu)
            .apply()
    }

    // ── Gerbang atas kemauan sendiri ────────────────────────────────────────

    private fun mulaiGerbangManual() {
        soalManual.value = null
        buka(Layar.KERJAKAN_SOAL)
        lifecycleScope.launch {
            soalManual.value = repo.soalUntukGerbang(nama.value)
        }
    }

    /**
     * Kredit dari tombol Kerjakan Soal.
     *
     * Dicatat dengan triggeredByPackage "MANUAL" supaya saat analisis bisa
     * dipisahkan dari gerbang yang dipicu media sosial — keduanya perilaku
     * yang sangat berbeda.
     */
    private fun selesaikanGerbangManual(jawaban: List<JawabanGerbang>) {
        // Hanya sekali per gerbang. Layar gerbang masih tergambar selama
        // animasi keluar; ketukan kedua pada tombolnya tidak boleh
        // mencatat jawaban dan memberi kredit untuk kedua kalinya.
        if (layar.value != Layar.KERJAKAN_SOAL) return
        gantiAkar(Layar.DASHBOARD)
        if (jawaban.isEmpty()) return

        val idSesi = System.currentTimeMillis()
        val kreditDetik = Kredit.totalGerbang(jawaban.sumOf { it.bonusDetik })
        val sisaLama = sisaKreditDetik()
        setKreditBerakhirPada(System.currentTimeMillis() + (sisaLama + kreditDetik) * 1000L)

        lifecycleScope.launch {
            jawaban.forEachIndexed { i, j ->
                repo.catatJawaban(
                    kode = nama.value,
                    versiPaket = versiPaket.value,
                    gateSessionId = idSesi,
                    urutanDalamGerbang = i + 1,
                    butir = j.soal.butir,
                    opsiDipilih = j.opsiDipilih.optionId,
                    durasiDetik = j.durasiDetik,
                    kreditDidapat = j.bonusDetik,
                    paketPemicu = "MANUAL",
                )
            }
            repo.mulaiSesiKredit(nama.value, idSesi, kreditDetik)
            segarkan()
        }
    }

    // ── Izin, paket, ekspor ─────────────────────────────────────────────────

    /** nomor mengikuti urutan wajib handoff Bagian 3.3. */
    private fun mintaIzin(nomor: Int) {
        when (nomor) {
            1 -> bukaSettingsUsageAccess(this)
            2 -> bukaSettingsOverlay(this)
            3 -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                mintaNotifikasi.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            4 -> bukaSettingsBaterai(this)
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
                val berkas = EksporCsv.tulis(this@MainActivity, repo, nama.value)
                statusEkspor.value = "${berkas.name} · ${berkas.length()} byte"
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
