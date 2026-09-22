package id.soaldulu.app

import android.content.Context
import android.content.SharedPreferences

/**
 * Saldo kredit waktu, seperti kuota.
 *
 * Diputuskan 22 Sep 2026: kredit hanya berkurang selama aplikasi yang dijaga
 * benar-benar terbuka di layar. Sebelumnya kredit berupa jam berakhir —
 * begitu didapat ia terus berjalan, walaupun responden tidak membuka media
 * sosial sama sekali.
 *
 * Disimpan dalam milidetik di SharedPreferences milik service, karena
 * dibaca dan dikurangi setiap detik di loop polling. Service dan Activity
 * berjalan di proses yang sama, jadi satu kunci JVM cukup supaya penambahan
 * dari Activity dan pengurangan dari service tidak saling menimpa.
 */
object SaldoKredit {

    private const val KEY_SALDO_MS = "saldo_kredit_ms"

    /** Kunci model lama: epoch ms saat kredit berakhir. Hanya dibaca untuk migrasi. */
    private const val KEY_LAMA_BERAKHIR = "kredit_berakhir_pada"

    private val kunci = Any()

    fun sisaDetik(context: Context): Int = (sisaMs(context) / 1000L).toInt()

    fun sisaMs(context: Context): Long = synchronized(kunci) { baca(prefs(context)) }

    /** Tambah kredit hasil gerbang, dibatasi MAX_CREDIT_BALANCE_SECONDS. */
    fun tambah(context: Context, detik: Int) {
        synchronized(kunci) {
            val p = prefs(context)
            val batas = GateConfig.MAX_CREDIT_BALANCE_SECONDS * 1000L
            val baru = baca(p) + detik * 1000L
            p.edit().putLong(KEY_SALDO_MS, if (batas > 0) minOf(baru, batas) else baru).apply()
        }
    }

    /** Kurangi karena pemakaian aplikasi yang dijaga. Tidak pernah di bawah nol. */
    fun pakai(context: Context, ms: Long) {
        synchronized(kunci) {
            val p = prefs(context)
            p.edit().putLong(KEY_SALDO_MS, (baca(p) - ms).coerceAtLeast(0L)).apply()
        }
    }

    /** Bayar sesuatu dengan kredit. false kalau saldo kurang; saldo tidak diubah. */
    fun bayar(context: Context, detik: Int): Boolean = synchronized(kunci) {
        val p = prefs(context)
        val saldo = baca(p)
        val harga = detik * 1000L
        if (saldo < harga) {
            false
        } else {
            p.edit().putLong(KEY_SALDO_MS, saldo - harga).apply()
            true
        }
    }

    private fun baca(p: SharedPreferences): Long {
        // Migrasi sekali dari model jam berakhir: sisa waktunya menjadi saldo awal.
        if (p.contains(KEY_LAMA_BERAKHIR)) {
            val sisa = (p.getLong(KEY_LAMA_BERAKHIR, 0L) - System.currentTimeMillis())
                .coerceAtLeast(0L)
            p.edit().putLong(KEY_SALDO_MS, sisa).remove(KEY_LAMA_BERAKHIR).apply()
            return sisa
        }
        return p.getLong(KEY_SALDO_MS, 0L)
    }

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(GateWatchService.PREFS, Context.MODE_PRIVATE)
}
