package id.soaldulu.app

import android.content.Context
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Pencatat hasil spike. Menulis ke Logcat DAN ke berkas teks.
 *
 * Alasan berkas: buffer Logcat berputar dan hampir pasti sudah membuang
 * entri paling awal setelah uji 6 jam. Kriteria 3 tidak bisa dibuktikan
 * dari Logcat saja.
 *
 * Ini alat ukur untuk Fase 0, bukan log penelitian Bagian 6. Log penelitian
 * yang sesungguhnya masuk ke Room di Fase 1.
 */
object SpikeLog {

    const val TAG = "SoalduluSpike"

    private val formatWaktu = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    /** Lokasi berkas: Android/data/id.soaldulu.app/files/spike_log.txt */
    fun berkas(context: Context): File =
        File(context.getExternalFilesDir(null), "spike_log.txt")

    fun tulis(context: Context, pesan: String) {
        Log.i(TAG, pesan)
        try {
            berkas(context).appendText("${formatWaktu.format(Date())}  $pesan\n")
        } catch (e: Exception) {
            Log.w(TAG, "Gagal menulis berkas log: ${e.message}")
        }
    }

    /** Untuk ditampilkan di layar supaya pengguna tahu berkasnya ada dan seberapa besar. */
    fun ringkasanBerkas(context: Context): String {
        val f = berkas(context)
        return if (f.exists()) "${f.absolutePath} (${f.length()} byte)" else "belum ada"
    }

    /**
     * Baca beberapa baris terakhir untuk ditampilkan di layar.
     *
     * Sejak Android 11 folder Android/data tidak bisa dibuka file manager,
     * jadi tanpa ini berkas log hanya terjangkau lewat adb. Kriteria 3 harus
     * bisa diperiksa tanpa laptop.
     */
    fun bacaBarisTerakhir(context: Context, jumlah: Int = 40): String {
        val f = berkas(context)
        if (!f.exists()) return "Berkas log belum ada. Nyalakan service dulu."
        return try {
            f.readLines().takeLast(jumlah).joinToString("\n")
        } catch (e: Exception) {
            "Gagal membaca berkas log: ${e.message}"
        }
    }

    fun hapusBerkas(context: Context) {
        try {
            berkas(context).delete()
        } catch (e: Exception) {
            Log.w(TAG, "Gagal menghapus berkas log: ${e.message}")
        }
    }
}
