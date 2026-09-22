package id.soaldulu.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.lifecycleScope
import id.soaldulu.app.data.EksporCsv
import id.soaldulu.app.data.HasilSeed
import id.soaldulu.app.data.Preferensi
import id.soaldulu.app.data.SoalLengkap
import id.soaldulu.app.data.SoalduluRepository
import id.soaldulu.app.ui.layar.AlurGerbang
import id.soaldulu.app.ui.layar.AplikasiDipantau
import id.soaldulu.app.ui.layar.KandidatAplikasi
import id.soaldulu.app.ui.layar.LayarAplikasi
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    APLIKASI,
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
    private val menyegarkan = mutableStateOf(false)
    private val daftarAplikasi = mutableStateOf<List<AplikasiDipantau>>(emptyList())
    private val pemilihAplikasiTerbuka = mutableStateOf(false)
    private val kandidatAplikasi = mutableStateOf<List<KandidatAplikasi>?>(null)

    /**
     * Aplikasi yang dihapus dari daftar begitu gerbang Kerjakan Soal selesai.
     * null berarti gerbang itu dibuka biasa dari Dashboard.
     */
    private var hapusSetelahGerbang: String? = null

    /** triggeredByPackage untuk gerbang yang sedang dibuka lewat Kerjakan Soal. */
    private var pemicuGerbangManual = PEMICU_MANUAL

    /** gateSessionId gerbang Kerjakan Soal yang sedang berjalan. */
    private var idSesiManual = 0L

    /**
     * Tombol Kerjakan Soal di notifikasi ditekan. Dijalankan begitu rute awal
     * sudah ditentukan — saat aplikasi baru dibuka, itu belum tentu sudah.
     */
    private var mintaKerjakanSoal = false

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
        // Hanya saat benar-benar baru dibuat. Saat dibuat ulang (rotasi, ganti
        // tema), intent lama tidak boleh membuka gerbang lagi.
        if (savedInstanceState == null) terimaIntent(intent)
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
    private fun buka(tujuan: Layar, arahBaru: Arah = Arah.MAJU) {
        // Ketukan ganda saat animasi berjalan tidak boleh menumpuk layar
        // yang sama dua kali — Back akan terasa tidak bekerja.
        if (tujuan == layar.value) return
        arah = arahBaru
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
                // Meninggalkan gerbang di tengah jalan: jawaban yang sudah
                // diberikan sudah tercatat beserta kreditnya, sisanya dibuang.
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
                // Tanpa tujuan mundur — aplikasi dibuka langsung di sini karena
                // ada izin yang dicabut — tombol kembali akan menutup aplikasi.
                onKembali = if (tumpukan.isNotEmpty()) ({ kembali() }) else null,
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

            Layar.DASHBOARD -> {
                // Sisa kredit dibaca ulang dari sumbernya setiap detik selama
                // Dashboard tampil, jadi angkanya berjalan tanpa perlu disegarkan.
                var sisaKredit by remember { mutableIntStateOf(sisaKreditDetik()) }
                LaunchedEffect(Unit) {
                    while (true) {
                        sisaKredit = sisaKreditDetik()
                        delay(GateConfig.DASHBOARD_REFRESH_SECONDS * 1000L)
                    }
                }
                LayarHome(
                    statistik = statistik.value.copy(sisaKreditDetik = sisaKredit),
                    menyegarkan = menyegarkan.value,
                    onSegarkan = { tarikSegarkan() },
                    onPengaturan = { buka(Layar.SETTINGS) },
                    onKerjakanSoal = { mulaiGerbangManual() },
                )
            }

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
                onAplikasi = {
                    muatDaftarAplikasi()
                    buka(Layar.APLIKASI)
                },
                onDataPrivasi = { buka(Layar.SYARAT) },
                onEkspor = { eksporLog() },
                onLayarUji = { buka(Layar.SPIKE) },
                onKembali = { kembali() },
            )

            Layar.APLIKASI -> {
                // Jam untuk hitung mundur mode darurat dan cooldown. Status
                // "dijaga" dihitung ulang tiap detik supaya sakelar menyala
                // sendiri begitu 10 menit darurat habis.
                var sekarang by remember { mutableLongStateOf(System.currentTimeMillis()) }
                LaunchedEffect(Unit) {
                    while (true) {
                        sekarang = System.currentTimeMillis()
                        delay(GateConfig.DASHBOARD_REFRESH_SECONDS * 1000L)
                    }
                }
                LayarAplikasi(
                    daftar = daftarAplikasi.value.map {
                        it.copy(dijaga = DaftarAplikasi.dijaga(this, it.paket, sekarang))
                    },
                    darurat = DaftarAplikasi.darurat(this, sekarang),
                    saldoKreditDetik = sisaKreditDetik(),
                    pemilihTerbuka = pemilihAplikasiTerbuka.value,
                    kandidat = kandidatAplikasi.value,
                    onUbahAktif = { paket, aktif -> ubahAktifAplikasi(paket, aktif) },
                    onMulaiDarurat = { paket -> mulaiDarurat(paket) },
                    onAkhiriDarurat = { akhiriDarurat() },
                    onBukaPemilih = { bukaPemilihAplikasi() },
                    onTutupPemilih = { pemilihAplikasiTerbuka.value = false },
                    onTambah = { tambahAplikasi(it) },
                    onHapusDenganSoal = { paket -> mulaiGerbangManual(untukHapus = paket) },
                    onHapusDenganKredit = { paket -> hapusDenganKredit(paket) },
                    onKembali = { kembali() },
                )
            }

            Layar.SYARAT -> LayarSyarat(
                onKembali = { kembali() },
            )

            // Selama soal dimuat, layar dibiarkan kosong. Daftar kosong
            // berarti "bank soal belum siap", jadi tidak boleh dipakai
            // sebagai tanda sedang memuat — pesan itu akan berkedip.
            Layar.KERJAKAN_SOAL -> soalManual.value?.let { soal -> AlurGerbang(
                soal = soal,
                sisaKreditDetik = { sisaKreditDetik() },
                onJawab = { jawaban, urutan -> catatJawabanManual(jawaban, urutan) },
                onSelesai = { dijawab -> selesaikanGerbangManual(dijawab) },
                onLapor = { itemId ->
                    lifecycleScope.launch {
                        repo.catatPeristiwa(nama.value, "ITEM_REPORTED", itemId)
                    }
                },
                // Menghapus aplikasi mensyaratkan semua soal dikerjakan.
                labelKeluarAwal = if (hapusSetelahGerbang == null) "Selesai" else null,
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        terimaIntent(intent)
        if (rutePertamaSudahDitentukan) jalankanPermintaanKerjakanSoal()
    }

    private fun terimaIntent(intent: Intent?) {
        if (intent == null) return
        // Dibuka ulang dari daftar aplikasi terbaru membawa intent lama;
        // itu bukan tekanan tombol yang baru.
        if (intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0) return
        if (intent.getBooleanExtra(EXTRA_KERJAKAN_SOAL, false)) mintaKerjakanSoal = true
    }

    /**
     * Buka gerbang Kerjakan Soal di atas Dashboard, sehingga Back kembali ke
     * Dashboard. Langsung tampil tanpa animasi masuk: saat aplikasi baru
     * dibuka, animasi justru akan memperlihatkan layar Welcome sekilas.
     */
    private fun jalankanPermintaanKerjakanSoal() {
        if (!mintaKerjakanSoal) return
        mintaKerjakanSoal = false
        // Hanya setelah onboarding selesai: perlu nama dan bank soal.
        if (nama.value.isBlank() || versiPaket.value.isBlank()) return
        if (layar.value == Layar.KERJAKAN_SOAL) return
        gantiAkar(Layar.DASHBOARD, Arah.TANPA)
        mulaiGerbangManual(pemicu = PEMICU_NOTIFIKASI, arahMasuk = Arah.TANPA)
    }

    private fun segarkan() {
        status.value = bacaStatusIzin(this)
        ringkasanLog.value = SpikeLog.ringkasanBerkas(this)
        jumlahStart.intValue = getSharedPreferences(GateWatchService.PREFS, MODE_PRIVATE)
            .getInt(GateWatchService.KEY_JUMLAH_START, 0)

        lifecycleScope.launch { muatUlang() }
    }

    /** Tarik ke bawah di Dashboard: muat ulang semuanya, indikator tampil sampai selesai. */
    private fun tarikSegarkan() {
        if (menyegarkan.value) return
        menyegarkan.value = true
        status.value = bacaStatusIzin(this)
        lifecycleScope.launch {
            try {
                muatUlang()
            } finally {
                menyegarkan.value = false
            }
        }
    }

    /** Baca ulang semua yang ditampilkan dari DataStore dan Room. */
    private suspend fun muatUlang() {
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
        jalankanPermintaanKerjakanSoal()

        // Jaring pengaman: kalau service pernah mati — dibunuh sistem, HP
        // di-restart, atau aplikasi dipasang ulang — membuka aplikasi
        // menghidupkannya lagi. startForegroundService aman dipanggil berulang.
        if (onboardingSelesai) {
            startForegroundService(Intent(this@MainActivity, GateWatchService::class.java))
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
    private fun sisaKreditDetik(): Int = SaldoKredit.sisaDetik(this)

    // ── Gerbang atas kemauan sendiri ────────────────────────────────────────

    /** `untukHapus` terisi: gerbang ini syarat menghapus aplikasi tambahan. */
    private fun mulaiGerbangManual(
        untukHapus: String? = null,
        pemicu: String = PEMICU_MANUAL,
        arahMasuk: Arah = Arah.MAJU,
    ) {
        hapusSetelahGerbang = untukHapus
        pemicuGerbangManual = if (untukHapus != null) PEMICU_HAPUS_APLIKASI else pemicu
        idSesiManual = System.currentTimeMillis()
        soalManual.value = null
        buka(Layar.KERJAKAN_SOAL, arahMasuk)
        lifecycleScope.launch {
            soalManual.value = repo.soalUntukGerbang(nama.value)
        }
    }

    /**
     * Kredit dari tombol Kerjakan Soal.
     *
     * Dicatat dengan triggeredByPackage "MANUAL" supaya saat analisis bisa
     * dipisahkan dari gerbang yang dipicu media sosial — keduanya perilaku
     * yang sangat berbeda. Yang dibuka dari tombol notifikasi dicatat sebagai
     * "NOTIFICATION". Gerbang yang menjadi syarat menghapus aplikasi dicatat
     * sebagai "REMOVE_APP"; kreditnya tetap diberikan seperti biasa.
     */
    /** Satu jawaban di Kerjakan Soal: dicatat dan kreditnya diberikan saat itu juga. */
    private fun catatJawabanManual(j: JawabanGerbang, urutan: Int) {
        val masuk = if (j.bonusDetik > 0) SaldoKredit.tambah(this, j.bonusDetik) else 0
        val idSesi = idSesiManual
        val pemicu = pemicuGerbangManual
        lifecycleScope.launch {
            repo.catatJawaban(
                kode = nama.value,
                versiPaket = versiPaket.value,
                gateSessionId = idSesi,
                urutanDalamGerbang = urutan,
                butir = j.soal.butir,
                opsiDipilih = j.opsiDipilih.optionId,
                durasiDetik = j.durasiDetik,
                kreditDidapat = j.bonusDetik,
                paketPemicu = pemicu,
            )
            if (masuk > 0) repo.mulaiSesiKredit(nama.value, idSesi, masuk)
            segarkan()
        }
    }

    private fun selesaikanGerbangManual(dijawab: Int) {
        // Hanya sekali per gerbang. Layar gerbang masih tergambar selama
        // animasi keluar; ketukan kedua pada tombolnya tidak boleh
        // menutup gerbang dan memberi kredit dasar untuk kedua kalinya.
        if (layar.value != Layar.KERJAKAN_SOAL) return
        val paketDihapus = hapusSetelahGerbang
        hapusSetelahGerbang = null
        // Dari daftar aplikasi, kembali ke daftar itu; dari Dashboard, ke Dashboard.
        if (paketDihapus != null) kembali() else gantiAkar(Layar.DASHBOARD)
        // Tanpa jawaban (bank kosong) tidak ada kredit, dan aplikasi tidak dihapus.
        if (dijawab == 0) return

        // Jawaban dan kreditnya sudah dicatat satu per satu. Yang tersisa hanya
        // kredit dasar per gerbang, kalau GateConfig memberinya (saat ini 0).
        val dasar = if (Kredit.dasarDetik > 0) SaldoKredit.tambah(this, Kredit.dasarDetik) else 0
        // Gerbang hapus-aplikasi tidak punya tombol keluar lebih awal, jadi
        // sampai di sini berarti semua soalnya sudah dikerjakan.
        if (paketDihapus != null) {
            DaftarAplikasi.hapus(this, paketDihapus)
            muatDaftarAplikasi()
        }

        val idSesi = idSesiManual
        lifecycleScope.launch {
            if (dasar > 0) repo.mulaiSesiKredit(nama.value, idSesi, dasar)
            if (paketDihapus != null) {
                repo.catatPeristiwa(nama.value, "APP_REMOVED", "$paketDihapus cara=SOAL")
            }
            segarkan()
        }
    }

    // ── Aplikasi dipantau ───────────────────────────────────────────────────
    // Setiap perubahan dicatat di log_peristiwa: responden yang mematikan atau
    // menghapus aplikasi adalah temuan penelitian, bukan kesalahan.

    private fun muatDaftarAplikasi() {
        lifecycleScope.launch {
            daftarAplikasi.value = withContext(Dispatchers.Default) {
                DaftarAplikasi.semua(this@MainActivity)
                    .mapNotNull { paket ->
                        // Aplikasi bawaan yang tidak terpasang di HP ini tidak ditampilkan.
                        val (namaApp, ikon) = infoAplikasi(paket) ?: return@mapNotNull null
                        AplikasiDipantau(
                            paket = paket,
                            nama = namaApp,
                            ikon = ikon,
                            bawaan = DaftarAplikasi.bawaan(paket),
                            terbatas = DaftarAplikasi.terbatas(this@MainActivity, paket),
                            dijaga = DaftarAplikasi.dijaga(this@MainActivity, paket),
                        )
                    }
                    .sortedWith(compareBy({ !it.terbatas }, { it.nama.lowercase() }))
            }
        }
    }

    private fun bukaPemilihAplikasi() {
        pemilihAplikasiTerbuka.value = true
        kandidatAplikasi.value = null
        lifecycleScope.launch {
            kandidatAplikasi.value = withContext(Dispatchers.Default) {
                val sudahAda = DaftarAplikasi.semua(this@MainActivity)
                val peluncur = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                @Suppress("DEPRECATION")
                packageManager.queryIntentActivities(peluncur, 0)
                    .map { it.activityInfo.packageName }
                    .distinct()
                    .filter { it != packageName && it !in sudahAda }
                    .mapNotNull { paket ->
                        val (namaApp, ikon) = infoAplikasi(paket) ?: return@mapNotNull null
                        KandidatAplikasi(paket, namaApp, ikon)
                    }
                    .sortedBy { it.nama.lowercase() }
            }
        }
    }

    /** Nama dan ikon aplikasi, atau null kalau tidak terpasang. */
    private fun infoAplikasi(paket: String): Pair<String, androidx.compose.ui.graphics.ImageBitmap>? =
        try {
            @Suppress("DEPRECATION")
            val info = packageManager.getApplicationInfo(paket, 0)
            val ikon = packageManager.getApplicationIcon(info).toBitmap(UKURAN_IKON_PX, UKURAN_IKON_PX)
            packageManager.getApplicationLabel(info).toString() to ikon.asImageBitmap()
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }

    private fun tambahAplikasi(k: KandidatAplikasi) {
        pemilihAplikasiTerbuka.value = false
        val terbatas = DaftarAplikasi.tambah(this, k.paket, k.nama)
        muatDaftarAplikasi()
        lifecycleScope.launch {
            repo.catatPeristiwa(
                nama.value,
                "APP_ADDED",
                "${k.paket} (${k.nama})" + if (terbatas) " terbatas" else "",
            )
        }
    }

    private fun ubahAktifAplikasi(paket: String, aktif: Boolean) {
        DaftarAplikasi.setAktif(this, paket, aktif)
        muatDaftarAplikasi()
        lifecycleScope.launch {
            repo.catatPeristiwa(nama.value, if (aktif) "APP_ENABLED" else "APP_DISABLED", paket)
        }
    }

    private fun mulaiDarurat(paket: String) {
        if (!DaftarAplikasi.mulaiDarurat(this, paket)) return
        lifecycleScope.launch {
            repo.catatPeristiwa(
                nama.value,
                "EMERGENCY_STARTED",
                "$paket ${GateConfig.EMERGENCY_PAUSE_SECONDS}s",
            )
        }
    }

    private fun akhiriDarurat() {
        val paket = DaftarAplikasi.akhiriDarurat(this) ?: return
        lifecycleScope.launch { repo.catatPeristiwa(nama.value, "EMERGENCY_ENDED_EARLY", paket) }
    }

    private fun hapusDenganKredit(paket: String) {
        val biaya = GateConfig.REMOVE_APP_CREDIT_COST_SECONDS
        if (!SaldoKredit.bayar(this, biaya)) return
        DaftarAplikasi.hapus(this, paket)
        muatDaftarAplikasi()
        lifecycleScope.launch {
            repo.catatPeristiwa(nama.value, "CREDIT_SPENT", "REMOVE_APP $paket ${biaya}s")
            repo.catatPeristiwa(nama.value, "APP_REMOVED", "$paket cara=KREDIT")
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

    companion object {
        /** Extra dari tombol Kerjakan Soal di notifikasi layanan. */
        const val EXTRA_KERJAKAN_SOAL = "kerjakan_soal"

        private const val PEMICU_MANUAL = "MANUAL"
        private const val PEMICU_NOTIFIKASI = "NOTIFICATION"
        private const val PEMICU_HAPUS_APLIKASI = "REMOVE_APP"
        private const val UKURAN_IKON_PX = 96
    }

    private fun versiAplikasi(): String = try {
        val info = packageManager.getPackageInfo(packageName, 0)
        "${info.versionName} (${info.longVersionCode})"
    } catch (e: Exception) {
        "tidak diketahui"
    }
}
