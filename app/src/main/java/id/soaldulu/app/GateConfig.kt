package id.soaldulu.app

/**
 * Semua parameter perilaku aplikasi, dalam satu objek.
 *
 * Aturan keras (handoff Bagian 4): tidak boleh ada angka waktu ditulis
 * langsung di UI, service, atau repository. Selalu rujuk ke sini.
 * Semua durasi dalam DETIK.
 */
object GateConfig {

    // ── KURS KREDIT ──
    // Diputuskan 22 Sep 2026: satu jawaban benar = 10 menit. Tidak ada kredit
    // dasar lagi — sebelumnya 10 menit dasar + 3 menit per jawaban benar.
    const val QUESTIONS_PER_GATE = 3
    const val GATE_REWARD_SECONDS = 0 // tanpa kredit dasar
    const val CORRECT_BONUS_SECONDS = 600 // 10 menit per jawaban benar
    const val MAX_CREDIT_BALANCE_SECONDS = 3600 // 0 = tanpa batas

    // Kurs versi pendek untuk DEV_MODE sudah dihapus (22 Sep 2026): yang diuji
    // di HP pengembang harus sama persis dengan yang diterima responden.

    // 3 soal, 0 benar → 0 detik    (gerbang muncul lagi setelah cooldown)
    // 3 soal, 1 benar → 600 detik  (10 menit)
    // 3 soal, 3 benar → 1800 detik (30 menit)
    // Jawaban benar yang lebih cepat dari MIN_GENUINE_ANSWER_SECONDS tetap
    // bernilai 0 — dianggap asal tekan.

    // ── PERILAKU GERBANG ──
    const val FOREGROUND_POLL_INTERVAL_SECONDS = 1

    // Handoff Bagian 4 semula menetapkan 2 detik, tapi itu bertabrakan dengan
    // Bagian 3.5 yang minta overlay muncul < 1 detik. Diputuskan 11 Sep 2026:
    // jeda dibuang, target < 1 detik yang dipertahankan — supaya gerbang
    // menangkap dorongan membuka medsos sebelum scroll pertama terjadi.
    // Pengukuran di HP Samsung A55 Android 16: 327-744 ms tanpa jeda.
    const val GATE_TRIGGER_DELAY_SECONDS = 0
    const val CREDIT_WARNING_BEFORE_EXPIRY_SECONDS = 60
    const val MIN_GENUINE_ANSWER_SECONDS = 2 // lebih cepat = tak dapat bonus
    const val GATE_COOLDOWN_SECONDS = 5

    // Seberapa sering angka sisa kredit di Dashboard diperbarui.
    const val DASHBOARD_REFRESH_SECONDS = 1

    // ── APLIKASI DIPANTAU ──
    // BELUM DIVERIFIKASI di HP responden (handoff Bagian 10). Daftar ini
    // disalin apa adanya dari handoff; nama package harus dicek ulang
    // sebelum APK dibagikan.
    val MONITORED_PACKAGES = setOf(
        "com.zhiliaoapp.musically", // TikTok (versi global)
        // TikTok versi Asia Tenggara. Diverifikasi 22 Sep 2026 di HP uji
        // (Samsung, Android 16): inilah paket TikTok yang terpasang, bukan
        // com.zhiliaoapp.musically — sebabnya TikTok tidak pernah memicu gerbang.
        "com.ss.android.ugc.trill",
        "com.instagram.android",
        "com.twitter.android",
        "com.google.android.youtube",
        "com.facebook.katana",
    )

    // ── MODE DARURAT DAN DAFTAR APLIKASI (diputuskan 22 Sep 2026) ──
    // TikTok dan Instagram hanya bisa dimatikan lewat mode darurat. Aplikasi
    // lain di daftar bebas dimatikan. Jatah darurat dipakai bersama.
    const val EMERGENCY_PAUSE_SECONDS = 600 // 10 menit
    const val EMERGENCY_COOLDOWN_SECONDS = 21_600 // 6 jam, sejak mode darurat berakhir

    // Harga menghapus aplikasi tambahan dari daftar, kalau tidak mengerjakan soal.
    const val REMOVE_APP_CREDIT_COST_SECONDS = 3600 // 1 jam

    val EMERGENCY_LIMITED_PACKAGES = setOf(
        "com.zhiliaoapp.musically",
        "com.ss.android.ugc.trill",
        "com.instagram.android",
    )

    // Aplikasi tambahan yang namanya memuat kata ini ikut dibatasi, supaya
    // varian TikTok dengan nama paket lain tidak lolos dari aturan darurat.
    val EMERGENCY_LIMITED_NAME_KEYWORDS = setOf("tiktok", "instagram")

    // Dicatat, TIDAK diblokir (handoff Bagian 3.6)
    val BROWSER_PACKAGES_FOR_LOGGING_ONLY = setOf(
        "com.android.chrome",
        "org.mozilla.firefox",
    )

    // ── PAKET SOAL ──
    const val BUNDLED_PACKAGE_ASSET = "bank_soal_v1.json"

    /**
     * Bank soal contoh untuk menguji aplikasi sebelum bank sungguhan ada.
     *
     * HANYA dipakai kalau BUNDLED_PACKAGE_ASSET tidak ditemukan DAN
     * DEV_MODE true. Karena DEV_MODE wajib false di APK responden, butir
     * dummy tidak mungkin sampai ke responden.
     */
    const val DEV_PACKAGE_ASSET = "bank_soal_dummy.json"
    const val UPDATE_MANIFEST_URL = "" // kosong = fitur mati
    const val UPDATE_CHECK_INTERVAL_SECONDS = 86_400
    const val UPDATE_TIMEOUT_SECONDS = 15
    const val ACCEPT_PATCH_UPDATES_ONLY = true

    // ── PENELITIAN ──
    const val EXPORT_FILENAME_TEMPLATE = "soaldulu_log_%s.csv"

    // SEDANG true UNTUK PENGEMBANGAN.
    // WAJIB dikembalikan ke false sebelum APK dibagikan ke responden — ini
    // yang membuka layar uji Fase 0 dari Settings.
    const val DEV_MODE = true
}

/**
 * Kurs kredit.
 *
 * Dipisah dari GateConfig karena ini aturan, bukan angka — tapi seluruh
 * angkanya tetap berasal dari GateConfig. Satu tempat saja, supaya UI dan
 * repository tidak pernah menghitung kredit dengan cara yang berbeda.
 */
object Kredit {

    /** Kredit dasar per gerbang. */
    val dasarDetik: Int
        get() = GateConfig.GATE_REWARD_SECONDS

    /** Bonus per jawaban benar. */
    val bonusDetik: Int
        get() = GateConfig.CORRECT_BONUS_SECONDS

    /** Berapa detik sebelum kredit habis peringatan ditampilkan. */
    val peringatanDetik: Int
        get() = GateConfig.CREDIT_WARNING_BEFORE_EXPIRY_SECONDS

    /**
     * Jawaban benar hanya berbuah bonus kalau tidak dijawab terlalu cepat.
     * Lebih cepat dari MIN_GENUINE_ANSWER_SECONDS dianggap asal tekan.
     */
    fun bonusSah(benar: Boolean, durasiDetik: Int): Boolean =
        benar && durasiDetik >= GateConfig.MIN_GENUINE_ANSWER_SECONDS

    /** Bonus untuk satu jawaban, dalam detik. */
    fun bonusSatuJawaban(benar: Boolean, durasiDetik: Int): Int =
        if (bonusSah(benar, durasiDetik)) bonusDetik else 0

    /** Total kredit satu gerbang: dasar + seluruh bonus, dibatasi plafon. */
    fun totalGerbang(totalBonusDetik: Int): Int {
        val mentah = dasarDetik + totalBonusDetik
        val batas = GateConfig.MAX_CREDIT_BALANCE_SECONDS
        return if (batas > 0) minOf(mentah, batas) else mentah
    }
}
