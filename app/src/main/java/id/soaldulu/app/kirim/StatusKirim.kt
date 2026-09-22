package id.soaldulu.app.kirim

import android.content.Context
import android.content.SharedPreferences
import id.soaldulu.app.BuildConfig
import java.util.UUID

/**
 * Keadaan pengiriman laporan ke peneliti.
 *
 * SharedPreferences, bukan DataStore: service membacanya dari loop pantau
 * yang bukan coroutine.
 */
object StatusKirim {

    private const val PREFS = "kirim"
    private const val KEY_AKTIF = "aktif"
    private const val KEY_TERAKHIR_BERHASIL = "terakhir_berhasil"
    private const val KEY_TERAKHIR_COBA = "terakhir_coba"
    private const val KEY_PESAN = "pesan_terakhir"
    private const val KEY_ID_PERANGKAT = "id_perangkat"

    /** Repo tujuan sudah disetel di local.properties saat APK dibangun. */
    val disetel: Boolean
        get() = BuildConfig.GITHUB_REPO.isNotBlank() && BuildConfig.GITHUB_TOKEN.isNotBlank()

    /**
     * Responden boleh menghentikan pengiriman kapan saja — persetujuan yang
     * tidak bisa ditarik bukan persetujuan. Menyala sejak awal, sesuai yang
     * disetujui di layar syarat.
     */
    fun aktif(context: Context): Boolean = prefs(context).getBoolean(KEY_AKTIF, true)

    fun setAktif(context: Context, aktif: Boolean) {
        prefs(context).edit().putBoolean(KEY_AKTIF, aktif).apply()
    }

    fun terakhirBerhasil(context: Context): Long = prefs(context).getLong(KEY_TERAKHIR_BERHASIL, 0L)

    fun terakhirCoba(context: Context): Long = prefs(context).getLong(KEY_TERAKHIR_COBA, 0L)

    fun pesanTerakhir(context: Context): String = prefs(context).getString(KEY_PESAN, "").orEmpty()

    fun catatHasil(context: Context, berhasil: Boolean, pesan: String) {
        val sekarang = System.currentTimeMillis()
        prefs(context).edit().apply {
            putLong(KEY_TERAKHIR_COBA, sekarang)
            if (berhasil) putLong(KEY_TERAKHIR_BERHASIL, sekarang)
            putString(KEY_PESAN, pesan)
        }.apply()
    }

    /**
     * Penanda tetap per pemasangan. Dipakai sebagai nama berkas di repo, supaya
     * dua responden bernama sama tidak saling menimpa laporan.
     */
    fun idPerangkat(context: Context): String {
        val p = prefs(context)
        p.getString(KEY_ID_PERANGKAT, null)?.let { return it }
        val baru = UUID.randomUUID().toString().take(8)
        p.edit().putString(KEY_ID_PERANGKAT, baru).apply()
        return baru
    }

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
