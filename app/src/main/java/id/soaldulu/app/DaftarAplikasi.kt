package id.soaldulu.app

import android.content.Context
import android.content.SharedPreferences

/**
 * Aplikasi yang dijaga gerbang, beserta sakelarnya.
 *
 * Diputuskan 22 Sep 2026:
 * - Responden bisa menambah aplikasi sendiri, karena nama paket berbeda antar
 *   HP — TikTok di HP uji ternyata com.ss.android.ugc.trill.
 * - Aplikasi "terbatas" (TikTok dan Instagram) hanya bisa dimatikan lewat
 *   mode darurat: paling lama EMERGENCY_PAUSE_SECONDS, lalu terkunci selama
 *   EMERGENCY_COOLDOWN_SECONDS. Satu jatah darurat dipakai bersama.
 * - Aplikasi lain bebas dimatikan dan dinyalakan.
 * - Aplikasi tambahan bisa dihapus (syaratnya diurus MainActivity); aplikasi
 *   bawaan dari GateConfig tidak bisa dihapus.
 *
 * Disimpan di SharedPreferences karena service membacanya setiap detik.
 * Semua waktu memakai jam dinding supaya cooldown 6 jam tetap berlaku
 * setelah HP dinyalakan ulang.
 */
object DaftarAplikasi {

    private const val PREFS = "aplikasi_dipantau"
    private const val KEY_TAMBAHAN = "tambahan"
    private const val KEY_TAMBAHAN_TERBATAS = "tambahan_terbatas"
    private const val KEY_NONAKTIF = "nonaktif"
    private const val KEY_DARURAT_PAKET = "darurat_paket"

    /** Epoch ms saat mode darurat berakhir — terjadwal, atau lebih awal kalau dinyalakan lagi. */
    private const val KEY_DARURAT_BERAKHIR = "darurat_berakhir"

    /** Keadaan mode darurat pada satu saat. */
    data class Darurat(
        /** Aplikasi yang sedang dimatikan, null kalau tidak ada. */
        val paket: String?,
        /** Sisa waktu mode darurat yang sedang berjalan. */
        val sisaDetik: Int,
        /** Sisa waktu terkunci setelah mode darurat berakhir. */
        val cooldownDetik: Int,
    ) {
        val bisaDipakai: Boolean get() = paket == null && cooldownDetik == 0
    }

    fun semua(context: Context): Set<String> =
        GateConfig.MONITORED_PACKAGES + set(prefs(context), KEY_TAMBAHAN)

    fun bawaan(paket: String): Boolean = paket in GateConfig.MONITORED_PACKAGES

    fun terbatas(context: Context, paket: String): Boolean =
        paket in GateConfig.EMERGENCY_LIMITED_PACKAGES ||
            paket in set(prefs(context), KEY_TAMBAHAN_TERBATAS)

    /**
     * Apakah gerbang berlaku untuk aplikasi ini sekarang. Dipanggil service
     * setiap detik, jadi hanya membaca SharedPreferences yang sudah di memori.
     */
    fun dijaga(
        context: Context,
        paket: String,
        sekarang: Long = System.currentTimeMillis(),
    ): Boolean {
        if (paket !in semua(context)) return false
        if (terbatas(context, paket)) return darurat(context, sekarang).paket != paket
        return paket !in set(prefs(context), KEY_NONAKTIF)
    }

    /**
     * Tambah aplikasi. Aplikasi yang namanya memuat kata di
     * EMERGENCY_LIMITED_NAME_KEYWORDS ikut terbatas, supaya varian TikTok
     * dengan nama paket lain tidak lolos dari aturan darurat.
     * Mengembalikan true kalau aplikasi itu terbatas.
     */
    fun tambah(context: Context, paket: String, nama: String): Boolean {
        val p = prefs(context)
        val terbatas = GateConfig.EMERGENCY_LIMITED_NAME_KEYWORDS.any {
            nama.contains(it, ignoreCase = true)
        }
        p.edit()
            .putStringSet(KEY_TAMBAHAN, set(p, KEY_TAMBAHAN) + paket)
            .putStringSet(
                KEY_TAMBAHAN_TERBATAS,
                if (terbatas) set(p, KEY_TAMBAHAN_TERBATAS) + paket else set(p, KEY_TAMBAHAN_TERBATAS),
            )
            .apply()
        return terbatas
    }

    /** Hapus aplikasi tambahan. Aplikasi bawaan diabaikan. */
    fun hapus(context: Context, paket: String) {
        if (bawaan(paket)) return
        val p = prefs(context)
        p.edit()
            .putStringSet(KEY_TAMBAHAN, set(p, KEY_TAMBAHAN) - paket)
            .putStringSet(KEY_TAMBAHAN_TERBATAS, set(p, KEY_TAMBAHAN_TERBATAS) - paket)
            .putStringSet(KEY_NONAKTIF, set(p, KEY_NONAKTIF) - paket)
            .apply()
    }

    /** Sakelar untuk aplikasi yang tidak terbatas. Aplikasi terbatas diabaikan. */
    fun setAktif(context: Context, paket: String, aktif: Boolean) {
        if (terbatas(context, paket)) return
        val p = prefs(context)
        val nonaktif = set(p, KEY_NONAKTIF)
        p.edit().putStringSet(KEY_NONAKTIF, if (aktif) nonaktif - paket else nonaktif + paket).apply()
    }

    fun darurat(context: Context, sekarang: Long = System.currentTimeMillis()): Darurat {
        val p = prefs(context)
        val berakhir = p.getLong(KEY_DARURAT_BERAKHIR, 0L)
        if (berakhir == 0L) return Darurat(paket = null, sisaDetik = 0, cooldownDetik = 0)
        if (sekarang < berakhir) {
            return Darurat(
                paket = p.getString(KEY_DARURAT_PAKET, null),
                sisaDetik = keDetik(berakhir - sekarang),
                cooldownDetik = 0,
            )
        }
        val bebas = berakhir + GateConfig.EMERGENCY_COOLDOWN_SECONDS * 1000L
        return Darurat(
            paket = null,
            sisaDetik = 0,
            cooldownDetik = if (sekarang < bebas) keDetik(bebas - sekarang) else 0,
        )
    }

    /** Matikan aplikasi terbatas selama EMERGENCY_PAUSE_SECONDS. false kalau belum boleh. */
    fun mulaiDarurat(context: Context, paket: String, sekarang: Long = System.currentTimeMillis()): Boolean {
        if (!terbatas(context, paket) || !darurat(context, sekarang).bisaDipakai) return false
        prefs(context).edit()
            .putString(KEY_DARURAT_PAKET, paket)
            .putLong(KEY_DARURAT_BERAKHIR, sekarang + GateConfig.EMERGENCY_PAUSE_SECONDS * 1000L)
            .apply()
        return true
    }

    /**
     * Nyalakan lagi sebelum waktunya habis. Cooldown dihitung sejak saat ini.
     * Mengembalikan aplikasi yang dinyalakan, atau null kalau tidak ada.
     */
    fun akhiriDarurat(context: Context, sekarang: Long = System.currentTimeMillis()): String? {
        val paket = darurat(context, sekarang).paket ?: return null
        prefs(context).edit().putLong(KEY_DARURAT_BERAKHIR, sekarang).apply()
        return paket
    }

    // Dibulatkan ke atas: sisa 0,4 detik masih ditampilkan sebagai 1 detik.
    private fun keDetik(ms: Long): Int = ((ms + 999L) / 1000L).toInt()

    // Salinan: set dari getStringSet tidak boleh diubah langsung.
    private fun set(p: SharedPreferences, key: String): Set<String> =
        p.getStringSet(key, emptySet()).orEmpty().toSet()

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
