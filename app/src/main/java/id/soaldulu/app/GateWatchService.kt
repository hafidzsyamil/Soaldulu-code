package id.soaldulu.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import id.soaldulu.app.data.Preferensi
import id.soaldulu.app.data.SoalLengkap
import id.soaldulu.app.data.SoalduluRepository
import id.soaldulu.app.ui.layar.AlurGerbang
import id.soaldulu.app.ui.layar.JawabanGerbang
import id.soaldulu.app.ui.layar.formatSisaKredit
import id.soaldulu.app.ui.theme.Background
import id.soaldulu.app.ui.theme.OnBackgroundDim
import id.soaldulu.app.ui.theme.SoalduluTheme
import id.soaldulu.app.ui.theme.Teks
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

    private var idSesiKreditBerjalan: Long? = null

    /** Supaya peringatan kredit hampir habis hanya dikirim sekali per sesi. */
    private var peringatanSudahDikirim = false
    private var kodeResponden = ""
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

        buatChannelNotifikasi()
        startForeground(ID_NOTIFIKASI, bangunNotifikasi())

        SpikeLog.tulis(this, "SERVICE_START ke-$jumlahStart")

        lingkup.launch {
            kodeResponden = Preferensi.kodeResponden(this@GateWatchService)
            versiPaket = repo.paketTerpasang()?.version.orEmpty()

            // Sesi kredit yang masih terbuka berarti service sempat mati saat
            // kredit sedang berjalan. Itu temuan penelitian, bukan kesalahan.
            val tergantung = repo.tandaiKreditTergantung()
            if (tergantung > 0) {
                SpikeLog.tulis(
                    this@GateWatchService,
                    "KREDIT_TERGANTUNG $tergantung sesi ditandai SERVICE_KILLED",
                )
                repo.catatPeristiwa(
                    kodeResponden,
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
            handlerPantau.postDelayed(this, GateConfig.FOREGROUND_POLL_INTERVAL_SECONDS * 1000L)
        }
    }

    private fun periksaSekali() {
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
                    repo.catatPeristiwa(kodeResponden, "BROWSER_OPENED", depan.paket)
                }
            }
            return
        }

        if (depan.paket !in GateConfig.MONITORED_PACKAGES) {
            // Responden keluar dari aplikasi terpantau; gerbang ikut dilepas.
            if (gerbangSedangTampil) tutupGerbang()
            return
        }

        // Ditahan sebelum penanda diperbarui: dipakai membedakan gerbang yang
        // muncul karena aplikasi baru dibuka dari gerbang yang muncul karena
        // kredit habis saat responden sudah berada di dalam aplikasi.
        val dipicuBukaanBaru = eventBaru
        if (eventBaru) eventTerakhirDitangani = depan.waktuEvent

        if (sisaKreditDetik() > 0) return // masih punya kredit, biarkan
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
                            .background(Background)
                            .safeDrawingPadding()
                    ) {
                        when (val soal = soalGerbang.value) {
                        null -> GerbangMemuat()
                        else -> AlurGerbang(
                            soal = soal,
                            sisaKreditDetik = 0,
                            onSelesai = { jawaban ->
                                selesaikanGerbang(idSesiGerbang, paketPemicu, jawaban)
                            },
                            onLapor = { itemId ->
                                lingkup.launch {
                                    repo.catatPeristiwa(kodeResponden, "ITEM_REPORTED", itemId)
                                }
                            },
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
                kodeResponden = Preferensi.kodeResponden(this@GateWatchService)
                versiPaket = repo.paketTerpasang()?.version.orEmpty()
                val soal = repo.soalUntukGerbang(kodeResponden)
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

    private fun selesaikanGerbang(
        idSesiGerbang: Long,
        paketPemicu: String,
        jawaban: List<JawabanGerbang>,
    ) {
        gerbangSedangTampil = false
        gerbangDitutupPada[paketPemicu] = SystemClock.elapsedRealtime()
        paketGerbangSekarang = null
        overlay?.tutup()
        overlay = null

        // Katup pengaman gerbang tanpa soal: tidak ada jawaban, tidak ada kredit.
        if (jawaban.isEmpty()) return

        val kreditDetik = Kredit.totalGerbang(jawaban.sumOf { it.bonusDetik })
        setKreditBerakhirPada(System.currentTimeMillis() + kreditDetik * 1000L)
        peringatanSudahDikirim = false
        perbaruiNotifikasi()

        lingkup.launch {
            jawaban.forEachIndexed { i, j ->
                repo.catatJawaban(
                    kode = kodeResponden,
                    versiPaket = versiPaket,
                    gateSessionId = idSesiGerbang,
                    urutanDalamGerbang = i + 1,
                    butir = j.soal.butir,
                    opsiDipilih = j.opsiDipilih.optionId,
                    durasiDetik = j.durasiDetik,
                    // Kredit dasar gerbang tidak dibagi ke tiap baris jawaban;
                    // yang tercatat di sini hanya bonus jawaban itu sendiri.
                    // Totalnya ada di log_kredit.creditGrantedSeconds.
                    kreditDidapat = j.bonusDetik,
                    paketPemicu = paketPemicu,
                )
            }
            idSesiKreditBerjalan =
                repo.mulaiSesiKredit(kodeResponden, idSesiGerbang, kreditDetik)

            SpikeLog.tulis(
                this@GateWatchService,
                "GERBANG_SELESAI benar=${jawaban.count { it.benar }}/${jawaban.size} " +
                    "kredit=${kreditDetik}s pemicu=$paketPemicu",
            )
        }
    }

    // ── Kredit ──────────────────────────────────────────────────────────────

    private fun prefs() = getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /**
     * Kapan kredit berakhir, dalam epoch ms. 0 = tidak punya kredit.
     *
     * Disimpan di SharedPreferences, bukan DataStore: nilai ini dibaca setiap
     * detik di dalam loop polling, dan DataStore yang berbasis Flow tidak
     * cocok untuk pembacaan sinkron sesering itu.
     */
    private fun kreditBerakhirPada(): Long = prefs().getLong(KEY_KREDIT_BERAKHIR, 0L)

    private fun setKreditBerakhirPada(waktu: Long) {
        prefs().edit().putLong(KEY_KREDIT_BERAKHIR, waktu).apply()
    }

    private fun sisaKreditDetik(): Int {
        val berakhir = kreditBerakhirPada()
        if (berakhir == 0L) return 0
        val sisa = (berakhir - System.currentTimeMillis()) / 1000L
        return sisa.coerceAtLeast(0L).toInt()
    }

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
        if (sisa <= 0 || sisa > Kredit.peringatanDetik) return
        if (peringatanSudahDikirim) return
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
        if (kreditBerakhirPada() == 0L) return
        if (sisaKreditDetik() > 0) return

        setKreditBerakhirPada(0L)
        val id = idSesiKreditBerjalan ?: return
        idSesiKreditBerjalan = null
        lingkup.launch { repo.tutupSesiKredit(id, "EXPIRED") }
        SpikeLog.tulis(this, "KREDIT_HABIS sesi=$id")
        perbaruiNotifikasi()
    }

    // ── Notifikasi ──────────────────────────────────────────────────────────

    private fun heartbeatBilaWaktunya() {
        val sekarang = SystemClock.elapsedRealtime()
        if (sekarang - heartbeatTerakhir < JEDA_HEARTBEAT_MS) return
        heartbeatTerakhir = sekarang
        SpikeLog.tulis(
            this,
            "HEARTBEAT uptime=${formatDurasi(sekarang - mulaiElapsed)} " +
                "polling=$jumlahPolling gerbang=$jumlahGerbang kredit=${sisaKreditDetik()}s",
        )
        perbaruiNotifikasi()
    }

    private fun perbaruiNotifikasi() {
        handlerUtama.post {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(ID_NOTIFIKASI, bangunNotifikasi())
        }
    }

    private fun buatChannelNotifikasi() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Layanan gerbang",
            NotificationManager.IMPORTANCE_LOW, // tanpa suara
        ).apply {
            description = "Menampilkan sisa kredit dan status layanan Soaldulu."
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
        nm.createNotificationChannel(channel)
        nm.createNotificationChannel(peringatan)
    }

    private fun bangunNotifikasi(): Notification {
        val sisa = sisaKreditDetik()
        val buka = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val isi = if (sisa > 0) {
            "Sisa kredit ${formatSisaKredit(sisa)}"
        } else {
            "Gerbang aktif — media sosial terkunci"
        }
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Soaldulu")
            .setContentText(isi)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(buka)
            .setOngoing(true)
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
        private const val KEY_KREDIT_BERAKHIR = "kredit_berakhir_pada"

        private const val CHANNEL_ID = "gerbang"
        private const val CHANNEL_PERINGATAN = "peringatan_kredit"
        private const val ID_NOTIFIKASI = 1
        private const val ID_NOTIFIKASI_PERINGATAN = 2
        private const val JEDA_HEARTBEAT_MS = 60_000L
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
        modifier = Modifier.fillMaxSize().background(Background),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Menyiapkan soal…", style = Teks.caption, color = OnBackgroundDim)
    }
}
