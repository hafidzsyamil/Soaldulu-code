package id.soaldulu.app

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context

/**
 * Aplikasi yang dipantau selama spike.
 *
 * Sengaja hardcode di sini, BUKAN di GateConfig. Handoff Bagian 4 melarang
 * membuat GateConfig sebelum struktur folder dikonfirmasi, dan daftar
 * package ini masih harus diverifikasi di HP responden.
 */
val PAKET_DIPANTAU = setOf(
    "com.zhiliaoapp.musically",   // TikTok
    "com.instagram.android",      // Instagram
    "com.google.android.youtube", // YouTube
)

/** Hasil satu kali pengecekan aplikasi depan. */
data class AplikasiDepan(
    val paket: String,
    /**
     * Kapan sistem mencatat aplikasi ini naik ke depan, dalam epoch ms.
     * Sejam dengan System.currentTimeMillis(), jadi selisihnya bisa dipakai
     * langsung untuk mengukur latensi.
     */
    val waktuEvent: Long,
)

/**
 * Membaca aplikasi yang sedang di depan lewat UsageStatsManager.
 *
 * Catatan penting: sistem mengirim usage event secara batch, bukan seketika.
 * Seberapa besar jedanya berbeda antar vendor dan versi Android. Justru
 * itulah yang diukur spike ini — angkanya datang dari pengukuran di HP,
 * bukan dari asumsi.
 */
class ForegroundAppDetector(context: Context) {

    private val usm =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    /** Batas akhir jendela query sebelumnya. */
    private var batasTerakhir = System.currentTimeMillis()

    private var terakhirDiketahui: AplikasiDepan? = null

    /**
     * Baca usage event sejak pengecekan sebelumnya, kembalikan aplikasi yang
     * sekarang di depan. Mengembalikan null hanya sampai event pertama masuk.
     *
     * Jendela sengaja dimundurkan JEDA_AMAN_MS supaya event yang datang
     * terlambat tetap terbaca. Membaca event yang sama dua kali tidak masalah:
     * kita hanya mengambil yang timestamp-nya paling baru.
     */
    fun cek(): AplikasiDepan? {
        val sekarang = System.currentTimeMillis()
        val mulai = batasTerakhir - JEDA_AMAN_MS

        val events = usm.queryEvents(mulai, sekarang)
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType != UsageEvents.Event.ACTIVITY_RESUMED) continue

            val sebelumnya = terakhirDiketahui
            if (sebelumnya == null || event.timeStamp >= sebelumnya.waktuEvent) {
                terakhirDiketahui = AplikasiDepan(event.packageName, event.timeStamp)
            }
        }

        batasTerakhir = sekarang
        return terakhirDiketahui
    }

    private companion object {
        const val JEDA_AMAN_MS = 10_000L
    }
}
