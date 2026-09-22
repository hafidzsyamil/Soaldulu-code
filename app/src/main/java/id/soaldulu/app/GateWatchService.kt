package id.soaldulu.app

import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import id.soaldulu.app.data.Preferensi
import id.soaldulu.app.data.SoalLengkap
import id.soaldulu.app.data.SoalduluRepository
import id.soaldulu.app.kirim.PengirimGitHub
import id.soaldulu.app.kirim.StatusKirim
import id.soaldulu.app.ui.layar.AlurGerbang
import id.soaldulu.app.ui.layar.JawabanGerbang
import id.soaldulu.app.ui.layar.formatSisaKredit
import id.soaldulu.app.ui.layar.namaAplikasi
import id.soaldulu.app.ui.theme.SoalduluTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Jantung aplikasi.
 *
 * Memantau aplikasi yang sedang di depan tiap detik. Kalau aplikasi terpantau
 * dibuka sementara kredit habis, gerbang soal muncul di atasnya. Menjawab
 * tiga soal menghasilkan kredit; selama kredit masih ada, aplikasi itu bebas
 * dipakai.
 *
 * Kredit dihitung dengan JAM DINDING sejak diberikan — bukan hanya saat
 * aplikasi terpantau dibuka. Alasannya: kolom creditStartedAt/creditEndedAt
 * di Bagian 6.2 adalah stempel waktu absolut dengan alasan berakhir EXPIRED,
 * yang hanya masuk akal untuk jendela waktu. Ini keputusan yang perlu kamu
 * tinjau — kalau maksudmu kredit hanya terpakai saat medsos dibuka,
 * bilang dan aku ubah.
 */
class GateWatchService : Service() {

    private lateinit var detector: ForegroundAppDetector
    private lateinit var repo: SoalduluRepository

    private lateinit var threadPantau: HandlerThread
    private lateinit var handlerPantau: Handler
    private val handlerUtama = Handler(Looper.getMainLooper())
    private val lingkup = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var overlay: OverlayCompose? = null

    /**
     * Soal untuk gerbang yang sedang tampil. null = masih dimuat.
     * Dibaca dari dalam composition overlay, jadi harus MutableState.
     */
    private val soalGerbang = mutableStateOf<List<SoalLengkap>?>(null)

    private var gerbangSedangTampil = false

    /**
     * Kapan gerbang terakhir ditutup, PER APLIKASI.
     *
     * Semula satu nilai untuk semua aplikasi, tapi itu berarti pindah dari
     * Instagram ke YouTube ikut tertahan cooldown — terukur 3,1 detik di HP
     * uji, jauh melewati target < 1 detik Bagian 3.5, dan meninggalkan
     * jendela 5 detik tanpa penjagaan. Cooldown hanya perlu mencegah gerbang
     * berulang pada aplikasi yang sama.
     */
    private val gerbangDitutupPada = mutableMapOf<String, Long>()

    /** Aplikasi yang memicu gerbang yang sedang tampil. */
    private var paketGerbangSekarang: String? = null

    /** Saldo pada polling sebelumnya masih ada; dipakai mendeteksi saat saldo habis. */
    private var adaKredit = false

    /** elapsedRealtime polling sebelumnya, untuk menghitung kredit yang terpakai. */
    private var pollingTerakhir = 0L

    private val power by lazy { getSystemService(Context.POWER_SERVICE) as PowerManager }
    private val keyguard by lazy { getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager }

    // Dibuat sekali: notifikasi dibangun ulang setiap detik, dan setiap
    // PendingIntent.getActivity adalah panggilan ke sistem.
    private val niatBuka by lazy {
        PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private val niatKerjakanSoal by lazy {
        PendingIntent.getActivity(
            this,
            1,
            Intent(this, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_KERJAKAN_SOAL, true)
                // SINGLE_TOP + CLEAR_TOP: kalau aplikasi sedang terbuka,
                // Activity yang sama menerimanya lewat onNewIntent — tidak
                // dibuat ulang dan tidak menumpuk dua salinan.
                .addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP,
                ),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    /** Teks notifikasi layanan yang terakhir dikirim. Hanya disentuh di thread utama. */
    private var teksNotifikasiTerakhir: String? = null

    /** Supaya peringatan kredit hampir habis hanya dikirim sekali per sesi. */
    private var peringatanSudahDikirim = false
    private var namaResponden = ""
    private var versiPaket = ""

    private var mulaiElapsed = 0L
    private var jumlahPolling = 0L
    private var heartbeatTerakhir = 0L
    private var jumlahStart = 1
    private var jumlahGerbang = 0

    /** waktuEvent yang sudah pernah ditangani, supaya satu event tidak dipakai dua kali. */
    private var eventTerakhirDitangani = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()

        detector = ForegroundAppDetector(this)
        repo = SoalduluRepository.ambil(this)

        val prefs = prefs()
        jumlahStart = prefs.getInt(KEY_JUMLAH_START, 0) + 1
        prefs.edit().putInt(KEY_JUMLAH_START, jumlahStart).apply()

        mulaiElapsed = SystemClock.elapsedRealtime()
        heartbeatTerakhir = mulaiElapsed
        pollingTerakhir = mulaiElapsed
        adaKredit = SaldoKredit.sisaMs(this) > 0

        buatChannelNotifikasi()
        teksNotifikasiTerakhir = teksNotifikasi()
        startForeground(ID_NOTIFIKASI, bangunNotifikasi(teksNotifikasiTerakhir.orEmpty()))

        SpikeLog.tulis(this, "SERVICE_START ke-$jumlahStart")

        lingkup.launch {
            namaResponden = Preferensi.nama(this@GateWatchService)
            versiPaket = repo.paketTerpasang()?.version.orEmpty()

            // Dengan model saldo, kredit tidak hangus saat service mati:
            // saldonya tetap tersimpan dan sesinya masih berjalan. Yang
            // ditandai SERVICE_KILLED hanya sesi yang tertinggal terbuka
            // padahal saldonya sudah nol.
            val saldo = SaldoKredit.sisaDetik(this@GateWatchService)
            if (saldo > 0) {
                repo.catatPeristiwa(
                    namaResponden,
                    "SERVICE_RESTARTED",
                    "saldo kredit ${saldo}s tetap tersimpan",
                )
                return@launch
            }
            val tergantung = repo.tandaiKreditTergantung()
            if (tergantung > 0) {
                SpikeLog.tulis(
                    this@GateWatchService,
                    "KREDIT_TERGANTUNG $tergantung sesi ditandai SERVICE_KILLED",
                )
                repo.catatPeristiwa(
                    namaResponden,
                    "SERVICE_RESTARTED",
                    "$tergantung sesi kredit ditutup sebagai SERVICE_KILLED",
                )
            }
        }

        threadPantau = HandlerThread("pantau-gerbang").apply { start() }
        handlerPantau = Handler(threadPantau.looper)
        handlerPantau.post(tugasPantau)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onTaskRemoved(rootIntent: Intent?) {
        SpikeLog.tulis(this, "TASK_REMOVED — aplikasi disapu dari recent apps, service jalan terus")
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        val uptime = SystemClock.elapsedRealtime() - mulaiElapsed
        SpikeLog.tulis(
            this,
            "SERVICE_DESTROY uptime=${formatDurasi(uptime)} polling=$jumlahPolling gerbang=$jumlahGerbang",
        )
        handlerPantau.removeCallbacksAndMessages(null)
        threadPantau.quitSafely()
        overlay?.tutup()
        overlay = null
        lingkup.cancel()
        super.onDestroy()
    }

    // ── Loop polling ────────────────────────────────────────────────────────

    private val tugasPantau = object : Runnable {
        override fun run() {
            jumlahPolling++
            try {
                periksaSekali()
            } catch (e: Exception) {
                SpikeLog.tulis(
                    this@GateWatchService,
                    "ERROR_POLLING ${e.javaClass.simpleName}: ${e.message}",
                )
            }
            heartbeatBilaWaktunya()
            // Sisa kredit di notifikasi ikut berjalan setiap detik saat
            // kredit sedang terpakai.
            perbaruiNotifikasi()
            handlerPantau.postDelayed(this, GateConfig.FOREGROUND_POLL_INTERVAL_SECONDS * 1000L)
        }
    }

    private fun periksaSekali() {
        val sekarang = SystemClock.elapsedRealtime()
        // Dibatasi supaya jeda panjang — thread yang sempat tertahan sistem —
        // tidak tiba-tiba memotong kredit banyak sekaligus.
        val selang = (sekarang - pollingTerakhir).coerceIn(0L, BATAS_SELANG_MS)
        pollingTerakhir = sekarang

        peringatkanBilaHampirHabis()
        tutupKreditBilaHabis()

        val depan = detector.cek() ?: return
        val eventBaru = depan.waktuEvent != eventTerakhirDitangani

        // Peramban sengaja TIDAK diblokir (handoff Bagian 3.6). Hanya dicatat,
        // karena perpindahan ke peramban adalah temuan penelitian.
        if (depan.paket in GateConfig.BROWSER_PACKAGES_FOR_LOGGING_ONLY) {
            if (eventBaru) {
                eventTerakhirDitangani = depan.waktuEvent
                lingkup.launch {
                    repo.catatPeristiwa(namaResponden, "BROWSER_OPENED", depan.paket)
                }
            }
            return
        }

        if (!DaftarAplikasi.dijaga(this, depan.paket)) {
            // Responden keluar dari aplikasi terpantau, atau aplikasinya
            // sedang dimatikan dari Settings; gerbang ikut dilepas.
            if (gerbangSedangTampil) tutupGerbang()
            return
        }

        // Ditahan sebelum penanda diperbarui: dipakai membedakan gerbang yang
        // muncul karena aplikasi baru dibuka dari gerbang yang muncul karena
        // kredit habis saat responden sudah berada di dalam aplikasi.
        val dipicuBukaanBaru = eventBaru
        if (eventBaru) {
            eventTerakhirDitangani = depan.waktuEvent
            // Berapa kali aplikasi dibuka adalah data penelitian tersendiri:
            // membuka dengan kredit tersisa tidak memunculkan gerbang, jadi
            // tanpa catatan ini pembukaan itu tidak terlihat di mana pun.
            lingkup.launch {
                repo.catatPeristiwa(namaResponden, "APP_OPENED", depan.paket)
            }
        }

        if (SaldoKredit.sisaMs(this) > 0) {
            // Masih punya kredit: biarkan, dan potong kredit sebanyak waktu
            // yang benar-benar dipakai. Detektor hanya tahu aplikasi terakhir
            // yang dibuka, jadi saat layar mati atau terkunci ia tetap
            // melaporkan aplikasi ini — waktu itu tidak dihitung. Begitu pula
            // selama gerbang masih tampil: kredit dari soal pertama tidak
            // boleh habis sementara responden mengerjakan soal kedua.
            if (!gerbangSedangTampil && power.isInteractive && !keyguard.isKeyguardLocked) {
                SaldoKredit.pakai(this, selang)
                Pemakaian.tambah(this, depan.paket, selang)
            }
            return
        }
        if (gerbangSedangTampil) return

        val ditutupPada = gerbangDitutupPada[depan.paket] ?: 0L
        if (SystemClock.elapsedRealtime() - ditutupPada <
            GateConfig.GATE_COOLDOWN_SECONDS * 1000L
        ) {
            return
        }

        bukaGerbang(depan.paket, depan.waktuEvent, dipicuBukaanBaru)
    }

    // ── Gerbang ─────────────────────────────────────────────────────────────

    /**
     * Overlay dipasang SEBELUM soal dimuat.
     *
     * Bagian 3.5 menuntut overlay muncul < 1 detik. Kalau kita menunggu
     * DataStore dan Room selesai dulu, waktunya terukur 945 ms di HP uji —
     * terlalu mepet. Yang dijanjikan Bagian 3.5 adalah gerbangnya muncul,
     * bukan soalnya sudah tergambar, jadi keduanya dipisah.
     */
    private fun bukaGerbang(
        paketPemicu: String,
        waktuEvent: Long,
        dipicuBukaanBaru: Boolean,
    ) {
        gerbangSedangTampil = true
        paketGerbangSekarang = paketPemicu
        jumlahGerbang++
        val idSesiGerbang = System.currentTimeMillis()
        val namaPemicu = labelAplikasi(paketPemicu)
        soalGerbang.value = null

        handlerUtama.post {
            overlay = OverlayCompose.tampilkan(this) {
                SoalduluTheme {
                    // Overlay memakai FLAG_LAYOUT_NO_LIMITS supaya menutup
                    // seluruh layar termasuk di balik status bar. Latarnya
                    // memenuhi semuanya, isinya digeser masuk.
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                            .safeDrawingPadding()
                    ) {
                        when (val soal = soalGerbang.value) {
                        null -> GerbangMemuat()
                        else -> AlurGerbang(
                            soal = soal,
                            sisaKreditDetik = { sisaKreditDetik() },
                            onJawab = { jawaban, urutan ->
                                catatJawabanGerbang(idSesiGerbang, paketPemicu, jawaban, urutan)
                            },
                            onSelesai = { dijawab ->
                                selesaikanGerbang(idSesiGerbang, paketPemicu, dijawab)
                            },
                            onLapor = { itemId ->
                                lingkup.launch {
                                    repo.catatPeristiwa(namaResponden, "ITEM_REPORTED", itemId)
                                }
                            },
                            labelKeluarAwal = "Buka $namaPemicu",
                        )
                        }
                    }
                }
            }
            if (overlay == null) {
                gerbangSedangTampil = false
                gerbangDitutupPada[paketPemicu] = SystemClock.elapsedRealtime()
                paketGerbangSekarang = null
                return@post
            }

            // Latensi hanya bermakna kalau gerbang muncul sebagai jawaban atas
            // aplikasi yang baru dibuka. Kalau gerbang muncul karena kredit
            // habis di tengah pemakaian, selisih terhadap waktuEvent bisa
            // menit-menitan dan akan merusak statistik Kriteria 2.
            SpikeLog.tulis(
                this,
                if (dipicuBukaanBaru) {
                    val latensi = System.currentTimeMillis() - waktuEvent
                    "GERBANG ke-$jumlahGerbang paket=$paketPemicu latensi=${latensi}ms"
                } else {
                    "GERBANG ke-$jumlahGerbang paket=$paketPemicu sebab=kredit_habis"
                },
            )

            lingkup.launch {
                // Disegarkan tiap gerbang supaya kode responden yang baru
                // dimasukkan langsung terpakai tanpa perlu restart service.
                namaResponden = Preferensi.nama(this@GateWatchService)
                versiPaket = repo.paketTerpasang()?.version.orEmpty()
                val soal = repo.soalUntukGerbang(namaResponden)
                withContext(Dispatchers.Main) {
                    soalGerbang.value = soal
                    SpikeLog.tulis(
                        this@GateWatchService,
                        if (dipicuBukaanBaru) {
                            "GERBANG_SOAL_SIAP jumlah=${soal.size} " +
                                "setelah=${System.currentTimeMillis() - waktuEvent}ms"
                        } else {
                            "GERBANG_SOAL_SIAP jumlah=${soal.size}"
                        },
                    )
                }
            }
        }
    }

    /** Lepas overlay tanpa memberi kredit — dipakai saat responden pindah aplikasi. */
    private fun tutupGerbang() {
        gerbangSedangTampil = false
        paketGerbangSekarang?.let { gerbangDitutupPada[it] = SystemClock.elapsedRealtime() }
        paketGerbangSekarang = null
        handlerUtama.post {
            overlay?.tutup()
            overlay = null
        }
    }

    /**
     * Satu jawaban di gerbang: dicatat dan kreditnya diberikan saat itu juga,
     * bukan menunggu gerbang selesai.
     */
    private fun catatJawabanGerbang(
        idSesiGerbang: Long,
        paketPemicu: String,
        j: JawabanGerbang,
        urutan: Int,
    ) {
        val masuk = if (j.bonusDetik > 0) SaldoKredit.tambah(this, j.bonusDetik) else 0
        if (masuk > 0) perbaruiNotifikasi()
        lingkup.launch {
            repo.catatJawaban(
                kode = namaResponden,
                versiPaket = versiPaket,
                gateSessionId = idSesiGerbang,
                urutanDalamGerbang = urutan,
                butir = j.soal.butir,
                opsiDipilih = j.opsiDipilih.optionId,
                durasiDetik = j.durasiDetik,
                kreditDidapat = j.bonusDetik,
                paketPemicu = paketPemicu,
            )
            // Satu sesi kredit per jawaban yang memberi kredit; totalnya per
            // gerbang bisa dijumlahkan lewat gateSessionId.
            if (masuk > 0) repo.mulaiSesiKredit(namaResponden, idSesiGerbang, masuk)
        }
    }

    /**
     * Gerbang ditutup: semua soal selesai, atau responden menekan tombol
     * "Buka ..." setelah mendapat kredit.
     */
    private fun selesaikanGerbang(
        idSesiGerbang: Long,
        paketPemicu: String,
        dijawab: Int,
    ) {
        gerbangSedangTampil = false
        gerbangDitutupPada[paketPemicu] = SystemClock.elapsedRealtime()
        paketGerbangSekarang = null
        overlay?.tutup()
        overlay = null

        // Katup pengaman gerbang tanpa soal: tidak ada jawaban, tidak ada kredit.
        if (dijawab == 0) return

        // Jawaban dan kreditnya sudah dicatat satu per satu. Yang tersisa hanya
        // kredit dasar per gerbang, kalau GateConfig memberinya (saat ini 0).
        val dasar = if (Kredit.dasarDetik > 0) SaldoKredit.tambah(this, Kredit.dasarDetik) else 0
        perbaruiNotifikasi()

        lingkup.launch {
            if (dasar > 0) repo.mulaiSesiKredit(namaResponden, idSesiGerbang, dasar)
        }
        SpikeLog.tulis(
            this,
            "GERBANG_SELESAI dijawab=$dijawab saldo=${sisaKreditDetik()}s pemicu=$paketPemicu",
        )
    }

    /**
     * Kirim laporan ke repo peneliti, paling sering sekali per
     * KIRIM_INTERVAL_SECONDS. Dijalankan dari service supaya responden yang
     * jarang membuka aplikasi tetap terkirim datanya.
     */
    private fun kirimLaporanBilaWaktunya() {
        if (!StatusKirim.disetel || !StatusKirim.aktif(this)) return
        val jeda = System.currentTimeMillis() - StatusKirim.terakhirCoba(this)
        if (jeda < GateConfig.KIRIM_INTERVAL_SECONDS * 1000L) return
        if (namaResponden.isBlank()) return
        lingkup.launch {
            PengirimGitHub.kirim(this@GateWatchService, repo, namaResponden, versiAplikasi())
        }
    }

    private fun versiAplikasi(): String = try {
        val info = packageManager.getPackageInfo(packageName, 0)
        "${info.versionName} (${info.longVersionCode})"
    } catch (e: PackageManager.NameNotFoundException) {
        "tidak diketahui"
    }

    /** Nama aplikasi untuk tombol "Buka ...". */
    private fun labelAplikasi(paket: String): String = try {
        @Suppress("DEPRECATION")
        packageManager.getApplicationLabel(packageManager.getApplicationInfo(paket, 0)).toString()
    } catch (e: PackageManager.NameNotFoundException) {
        namaAplikasi(paket)
    }

    // ── Kredit ──────────────────────────────────────────────────────────────

    private fun prefs() = getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /**
     * Sisa saldo kredit. Disimpan SaldoKredit di SharedPreferences, bukan
     * DataStore: nilai ini dibaca setiap detik di dalam loop polling, dan
     * DataStore yang berbasis Flow tidak cocok untuk pembacaan sinkron
     * sesering itu.
     */
    private fun sisaKreditDetik(): Int = SaldoKredit.sisaDetik(this)

    /**
     * Peringatan sebelum kredit habis (GateConfig.CREDIT_WARNING_BEFORE_EXPIRY_SECONDS).
     *
     * Dikirim sebagai notifikasi terpisah dengan tingkat kepentingan lebih
     * tinggi, bukan sekadar mengubah teks notifikasi layanan. Alasannya:
     * saat kredit hampir habis responden sedang berada di dalam media sosial,
     * dan notifikasi diam di baki tidak akan terlihat sama sekali.
     */
    private fun peringatkanBilaHampirHabis() {
        val sisa = sisaKreditDetik()
        if (sisa > Kredit.peringatanDetik) {
            // Saldo terisi lagi di atas batas peringatan, termasuk dari tombol
            // Kerjakan Soal: peringatan boleh dikirim lagi nanti.
            peringatanSudahDikirim = false
            return
        }
        if (sisa <= 0 || peringatanSudahDikirim) return
        peringatanSudahDikirim = true

        handlerUtama.post {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(
                ID_NOTIFIKASI_PERINGATAN,
                Notification.Builder(this, CHANNEL_PERINGATAN)
                    .setContentTitle("Kredit hampir habis")
                    .setContentText("Sisa ${formatSisaKredit(sisa)}. Gerbang akan menutup lagi.")
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setAutoCancel(true)
                    .build(),
            )
        }
        SpikeLog.tulis(this, "KREDIT_PERINGATAN sisa=${sisa}s")
    }

    private fun tutupKreditBilaHabis() {
        if (SaldoKredit.sisaMs(this) > 0) {
            adaKredit = true
            return
        }
        if (!adaKredit) return
        adaKredit = false

        lingkup.launch {
            val jumlah = repo.tutupSemuaKreditTerbuka("EXPIRED")
            SpikeLog.tulis(this@GateWatchService, "KREDIT_HABIS sesi_ditutup=$jumlah")
        }
        perbaruiNotifikasi()
    }

    // ── Notifikasi ──────────────────────────────────────────────────────────

    private fun heartbeatBilaWaktunya() {
        val sekarang = SystemClock.elapsedRealtime()
        if (sekarang - heartbeatTerakhir < JEDA_HEARTBEAT_MS) return
        heartbeatTerakhir = sekarang
        kirimLaporanBilaWaktunya()
        SpikeLog.tulis(
            this,
            "HEARTBEAT uptime=${formatDurasi(sekarang - mulaiElapsed)} " +
                "polling=$jumlahPolling gerbang=$jumlahGerbang kredit=${sisaKreditDetik()}s",
        )
        perbaruiNotifikasi()
    }

    /**
     * Dipanggil setiap polling, tapi notifikasi hanya dikirim ulang kalau
     * teksnya berubah. Saldo hanya bergerak saat media sosial terbuka, jadi
     * pembaruan per detik hanya terjadi saat itu — di luar itu tidak ada
     * yang dikirim. Sistem membatasi beberapa pembaruan per detik per
     * aplikasi; satu per detik masih jauh di bawahnya.
     */
    private fun perbaruiNotifikasi() {
        handlerUtama.post {
            val teks = teksNotifikasi()
            if (teks == teksNotifikasiTerakhir) return@post
            teksNotifikasiTerakhir = teks
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(ID_NOTIFIKASI, bangunNotifikasi(teks))
        }
    }

    private fun teksNotifikasi(): String {
        val sisa = sisaKreditDetik()
        return if (sisa > 0) {
            "Sisa kredit ${formatSisaKredit(sisa)}"
        } else {
            "Gerbang aktif — media sosial terkunci"
        }
    }

    private fun buatChannelNotifikasi() {
        // IMPORTANCE_DEFAULT, bukan LOW: channel LOW masuk kelompok "senyap"
        // di bagian bawah panel notifikasi. Suara dan getarnya dimatikan di
        // channel itu sendiri, jadi tetap tidak pernah berbunyi.
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Layanan gerbang",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Menampilkan sisa kredit dan status layanan Soaldulu."
            setSound(null, null)
            enableVibration(false)
            setShowBadge(false)
        }
        val peringatan = NotificationChannel(
            CHANNEL_PERINGATAN,
            "Peringatan kredit",
            // Lebih tinggi dari channel layanan supaya muncul sebagai banner
            // di atas media sosial yang sedang dibuka.
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Memberi tahu saat kredit waktu hampir habis."
        }

        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // Tingkat kepentingan channel tidak bisa dinaikkan setelah dibuat,
        // jadi channel LOW yang lama dihapus dan diganti channel baru.
        nm.deleteNotificationChannel(CHANNEL_LAMA)
        nm.createNotificationChannel(channel)
        nm.createNotificationChannel(peringatan)
    }

    private fun bangunNotifikasi(isi: String): Notification {
        val kerjakanSoal = Notification.Action.Builder(
            Icon.createWithResource(this, android.R.drawable.ic_menu_edit),
            "Kerjakan Soal",
            niatKerjakanSoal,
        ).build()
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Soaldulu")
            .setContentText(isi)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(niatBuka)
            .addAction(kerjakanSoal)
            .setOngoing(true)
            // Diperbarui tiap detik: jangan berbunyi atau bergetar ulang, dan
            // jangan tampilkan jam yang ikut berganti setiap pembaruan.
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            // Notifikasi foreground service yang diwarnai diletakkan sistem
            // di atas notifikasi lain. Warnanya biru tua aplikasi (BiruDalam).
            .setColor(WARNA_NOTIFIKASI)
            .setColorized(true)
            .build()
    }

    private fun formatDurasi(ms: Long): String {
        val totalMenit = ms / 60_000
        val jam = totalMenit / 60
        val menit = totalMenit % 60
        return if (jam > 0) "${jam}j ${menit}m" else "${menit}m"
    }

    companion object {
        const val PREFS = "spike"
        // (lanjut di bawah)
        const val KEY_JUMLAH_START = "jumlah_start"

        private const val CHANNEL_ID = "gerbang_utama"
        private const val CHANNEL_LAMA = "gerbang"
        private const val WARNA_NOTIFIKASI = 0xFF0F4C75.toInt()
        private const val CHANNEL_PERINGATAN = "peringatan_kredit"
        private const val ID_NOTIFIKASI = 1
        private const val ID_NOTIFIKASI_PERINGATAN = 2
        private const val JEDA_HEARTBEAT_MS = 60_000L
        private const val BATAS_SELANG_MS = GateConfig.FOREGROUND_POLL_INTERVAL_SECONDS * 2_000L
    }
}

/**
 * Tampilan sekejap antara overlay terpasang dan soal selesai dimuat.
 *
 * Sengaja latar polos tanpa spinner: yang penting media sosial di bawahnya
 * sudah tertutup pada milidetik pertama.
 */
@Composable
private fun GerbangMemuat() {
    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "Menyiapkan soal…",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
