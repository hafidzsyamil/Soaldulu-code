package id.soaldulu.app

import android.content.Context
import android.content.SharedPreferences

/**
 * Berapa lama tiap aplikasi yang dijaga benar-benar dipakai, dalam milidetik.
 *
 * Kredit yang terpakai sudah dihitung SaldoKredit, tapi saldo tidak menyimpan
 * ke mana perginya. Di sini dicatat per aplikasi, supaya laporan penelitian
 * bisa menjawab "berapa menit dipakai untuk TikTok" tanpa membaca ulang
 * seluruh log.
 *
 * Ditulis dari loop polling service setiap detik, jadi SharedPreferences,
 * bukan Room.
 */
object Pemakaian {

    private const val PREFS = "pemakaian"

    private val kunci = Any()

    /** Tambah waktu pakai satu aplikasi. */
    fun tambah(context: Context, paket: String, ms: Long) {
        if (ms <= 0L) return
        synchronized(kunci) {
            val p = prefs(context)
            p.edit().putLong(paket, p.getLong(paket, 0L) + ms).apply()
        }
    }

    /** Seluruh catatan pemakaian: nama paket ke total detik. */
    fun semuaDetik(context: Context): Map<String, Long> = synchronized(kunci) {
        prefs(context).all.mapNotNull { (paket, nilai) ->
            val ms = nilai as? Long ?: return@mapNotNull null
            paket to ms / 1000L
        }.toMap()
    }

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
