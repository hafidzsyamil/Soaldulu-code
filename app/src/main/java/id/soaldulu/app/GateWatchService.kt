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

/**
 * Foreground service Fase 0.
 *
 * Tugasnya tiga: polling aplikasi depan tiap 1 detik, memunculkan overlay
 * saat aplikasi terpantau dibuka, dan membuktikan dirinya masih hidup
 * setelah 6 jam.
 *
 * Uptime dan jumlah polling ditampilkan langsung di notifikasi supaya
 * kriteria 3 bisa diperiksa hanya dengan membuka panel notifikasi —
 * tanpa mencolok kabel ke laptop.
 */
class GateWatchService : Service() {

    private lateinit var detector: ForegroundAppDetector
    private lateinit var overlay: OverlayGate

    private lateinit var threadPantau: HandlerThread
    private lateinit var handlerPantau: Handler
    private val handlerUtama = Handler(Looper.getMainLooper())

    private var mulaiElapsed = 0L
    private var jumlahPolling = 0L
    private var heartbeatTerakhir = 0L
    private var jumlahStart = 1
    private var jumlahGerbang = 0

    /** waktuEvent yang sudah pernah memicu gerbang, supaya tidak memicu dua kali. */
    private var eventTerakhirDitangani = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()

        detector = ForegroundAppDetector(this)
        overlay = OverlayGate(this)

        // Kalau angka ini naik sendiri selama uji 6 jam, artinya service
        // sempat mati lalu dihidupkan ulang sistem. Itu temuan untuk kriteria 3.
        val prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        jumlahStart = prefs.getInt(KEY_JUMLAH_START, 0) + 1
        prefs.edit().putInt(KEY_JUMLAH_START, jumlahStart).apply()

        mulaiElapsed = SystemClock.elapsedRealtime()
        heartbeatTerakhir = mulaiElapsed

        buatChannelNotifikasi()
        // Versi 2 argumen sengaja dipakai: tipe service diambil dari
        // foregroundServiceType di manifest, jadi tidak perlu percabangan versi.
        startForeground(ID_NOTIFIKASI, bangunNotifikasi())

        SpikeLog.tulis(this, "SERVICE_START ke-$jumlahStart")

        threadPantau = HandlerThread("pantau-gerbang").apply { start() }
        handlerPantau = Handler(threadPantau.looper)
        handlerPantau.post(tugasPantau)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Kriteria 4: dipanggil saat aplikasi disapu dari layar recent apps.
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
        overlay.sembunyikan() // onDestroy berjalan di main thread
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
            handlerPantau.postDelayed(this, JEDA_POLL_MS)
        }
    }

    private fun periksaSekali() {
        val depan = detector.cek() ?: return

        if (depan.paket !in PAKET_DIPANTAU) {
            if (overlay.sedangTampil) {
                handlerUtama.post { overlay.sembunyikan() }
            }
            return
        }

        // Event yang sama tidak boleh memicu gerbang dua kali. Ini juga yang
        // membuat overlay tidak muncul lagi setelah ditutup manual, selama
        // pengguna belum berpindah aplikasi.
        if (depan.waktuEvent == eventTerakhirDitangani) return
        eventTerakhirDitangani = depan.waktuEvent

        val latensiDeteksi = System.currentTimeMillis() - depan.waktuEvent
        jumlahGerbang++

        handlerUtama.post {
            val latensiTotal = System.currentTimeMillis() - depan.waktuEvent
            val lulus = if (latensiTotal < 1000) "LULUS < 1 detik" else "LEWAT 1 detik"
            overlay.tampilkan(
                paketPemicu = depan.paket,
                latensiMs = latensiTotal,
                catatan = "$lulus  ·  deteksi ${latensiDeteksi}ms  ·  gerbang ke-$jumlahGerbang",
            ) {
                SpikeLog.tulis(this, "OVERLAY_DITUTUP manual")
            }
            SpikeLog.tulis(
                this,
                "GERBANG ke-$jumlahGerbang paket=${depan.paket} " +
                    "latensiDeteksi=${latensiDeteksi}ms latensiTotal=${latensiTotal}ms",
            )
        }
    }

    private fun heartbeatBilaWaktunya() {
        val sekarang = SystemClock.elapsedRealtime()
        if (sekarang - heartbeatTerakhir < JEDA_HEARTBEAT_MS) return
        heartbeatTerakhir = sekarang

        val uptime = sekarang - mulaiElapsed
        SpikeLog.tulis(
            this,
            "HEARTBEAT uptime=${formatDurasi(uptime)} polling=$jumlahPolling gerbang=$jumlahGerbang",
        )
        handlerUtama.post {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(ID_NOTIFIKASI, bangunNotifikasi())
        }
    }

    // ── Notifikasi ──────────────────────────────────────────────────────────

    private fun buatChannelNotifikasi() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Layanan gerbang",
            NotificationManager.IMPORTANCE_LOW, // tanpa suara
        ).apply {
            description = "Menampilkan status layanan pemantau Soaldulu."
            setShowBadge(false)
        }
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }

    private fun bangunNotifikasi(): Notification {
        val uptime = SystemClock.elapsedRealtime() - mulaiElapsed
        val buka = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Soaldulu — spike aktif · start ke-$jumlahStart")
            .setContentText("${formatDurasi(uptime)} · $jumlahPolling polling · $jumlahGerbang gerbang")
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
        const val KEY_JUMLAH_START = "jumlah_start"

        private const val CHANNEL_ID = "gerbang"
        private const val ID_NOTIFIKASI = 1
        private const val JEDA_POLL_MS = 1_000L
        private const val JEDA_HEARTBEAT_MS = 60_000L
    }
}
